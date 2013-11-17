package com.h6ah4i.android.app;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hasegawa on 11/16/13.
 */
class AsyncUsbSerial {
    private static final String TAG = AsyncUsbSerial.class.getSimpleName();

    private Context mContext;
    private EventListener mListener;

    private UsbManager mUsbManager;
    private UsbSerialDriver mUsbSerial;
    private TxWorker mTxWorker;
    private RxWorker mRxWorker;
    private Thread mTxThread;
    private Thread mRxThread;

    /**
     * Circular buffer
     */
    private static final class CircDataBuffer {
        private int mCount;
        private int mReadPos;
        private int mWritePos;
        private byte[] mBuff;

        public CircDataBuffer(int size) {
            mBuff = new byte[size];
        }

        public synchronized void clear() {
            mReadPos = 0;
            mWritePos = 0;
            mCount = 0;
        }

        public int size() {
            return mBuff.length;
        }

        public synchronized int count() {
            return mCount;
        }

        public synchronized int read(byte[] dest, int offset, int n) {
            final int n_ = Math.min(n, mCount);
            final int n1, n2;

            if ((mReadPos + n_) < mBuff.length) {
                n1 = n_;
                n2 = 0;
            } else {
                n1 = mBuff.length - mReadPos;
                n2 = n_ - n1;
            }

            if (n1 > 0) {
                System.arraycopy(mBuff, mReadPos, dest, offset, n1);
            }
            if (n2 > 0) {
                System.arraycopy(mBuff, 0, dest, offset + n1, n2);
            }

            mReadPos = (mReadPos + n_) % mBuff.length;
            mCount -= n_;

            return n_;
        }

        public synchronized int write(byte[] src, int offset, int n) {
            final int n_ = Math.min(n, mBuff.length - mCount);
            final int n1, n2;

            if ((mWritePos + n_) < mBuff.length) {
                n1 = n_;
                n2 = 0;
            } else {
                n1 = mBuff.length - mWritePos;
                n2 = n_ - n1;
            }

            if (n1 > 0) {
                System.arraycopy(src, offset, mBuff, mWritePos, n1);
            }
            if (n2 > 0) {
                System.arraycopy(src, offset + n1, mBuff, 0, n2);
            }

            mWritePos = (mWritePos + n_) % mBuff.length;
            mCount += n_;

            return n_;
        }
    }

    /**
     * Tx. worker process
     */
    private static class TxWorker implements Runnable {
        private static final String TAG = AsyncUsbSerial.TAG + "." + TxWorker.class.getSimpleName();
        private static final int TIMEOUT = 50;

        private UsbSerialDriver mDriver;
        private WeakReference<AsyncUsbSerial> mHolder;
        private CircDataBuffer mWriteDataBuff;

        public TxWorker(AsyncUsbSerial holder, UsbSerialDriver driver) {
            mHolder = new WeakReference<AsyncUsbSerial>(holder);
            mDriver = driver;
            mWriteDataBuff = new CircDataBuffer(1024);
        }

        @Override
        public void run() {
            final UsbSerialDriver driver = mDriver;
            final AsyncUsbSerial holder = mHolder.get();

            if (driver == null || holder == null) {
                Log.w(TAG, "!!!");
                return;
            }

            byte[] buff = new byte[1024];

            try {
                while (!Thread.interrupted()) {
                    final int numWrite;

                    // wait for data
                    synchronized (mWriteDataBuff)
                    {
                        mWriteDataBuff.wait(50);
                        numWrite = mWriteDataBuff.read(buff, 0, buff.length);
                    }

                    // write
                    if (numWrite > 0) {
                        try {
//                            Log.d("TX-DATA", new String(buff, 0, numWrite, "US-ASCII"));
                            byte[] buff2 = Arrays.copyOfRange(buff, 0, numWrite);
                            driver.write(buff2, TIMEOUT);
                        } catch (IOException e) {
                            Log.e(TAG, "AsyncUsbSerial.Worker.run() - write", e);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "AsyncUsbSerial.Worker.run() interrupted");
            }
        }

        public int putWriteData(byte[] data, int offset, int n) {
            final int numActuallyWrote;

            synchronized (mWriteDataBuff) {
                numActuallyWrote =
                        mWriteDataBuff.write(data, offset, n);
                mWriteDataBuff.notify();
            }

            return numActuallyWrote;
        }
    }

    /**
     * Rx. worker process
     */
    private static class RxWorker implements Runnable {
        private static final String TAG = AsyncUsbSerial.TAG + "." + RxWorker.class.getSimpleName();
        private static final int TIMEOUT = 50;

        private UsbSerialDriver mDriver;
        private WeakReference<AsyncUsbSerial> mHolder;

        public RxWorker(AsyncUsbSerial holder, UsbSerialDriver driver) {
            mHolder = new WeakReference<AsyncUsbSerial>(holder);
            mDriver = driver;
        }

        @Override
        public void run() {
            final UsbSerialDriver driver = mDriver;
            final AsyncUsbSerial holder = mHolder.get();

            if (driver == null || holder == null) {
                Log.w(TAG, "!!!");
                return;
            }

            byte[] buff = new byte[1024];

            while (!Thread.interrupted()) {
                // read
                int numRead = 0;

                try {
                    numRead = driver.read(buff, TIMEOUT);
                } catch (IOException e) {
                    Log.e(TAG, "AsyncUsbSerial.Worker.run() - read", e);
                }

                if (numRead > 0) {
                    holder.onReceived(buff, numRead);
                }

                if (numRead >= buff.length) {
                    continue;
                }
            }
        }
    }

    private void onReceived(byte[] data, int n) {
        if (mListener != null) {
            mListener.onReceived(data, n);
        }
    }

    public interface EventListener {
        void onReceived(byte[] data, int n);
    }

    /**
     * AsyncUsbSerial constructor
     *
     * @param context Context object
     * @param listener AsyncUsbSerial.EventListener instance
     */
    public AsyncUsbSerial(Context context, EventListener listener) {
        if (context == null)
            throw new IllegalArgumentException("context cannot be null");
        if (listener == null)
            throw new IllegalArgumentException("listener cannot be null");

        mContext = context;
        mListener = listener;
    }

    /**
     * Open USB-Serial driver
     */
    public  synchronized void open() {
        if (setupUSB(mContext)) {
            Log.d(TAG, "USB-Serial driver is now opened");
        }
    }

    /**
     * Gets opened state
     *
     * @return Whether USB-Serial driver is opened
     */
    public synchronized boolean isOpened() {
        return (mUsbSerial != null);
    }

    /**
     * Start Tx./Rx. operation
     */
    public synchronized void start() {
        if (!isOpened())
            throw new IllegalStateException("USB-Serial driver is not opened yet");

        if (mTxWorker != null || mRxThread != null)
            throw new IllegalStateException("Already started");

        TxWorker tx = new TxWorker(this, mUsbSerial);
        RxWorker rx = new RxWorker(this, mUsbSerial);
        Thread txThread = null;
        Thread rxThread = null;
        try {
            txThread = new Thread(tx);
            rxThread = new Thread(rx);
            txThread.start();
            rxThread.start();
        } catch (Exception e) {
            safeInterruptJoinThread(txThread);
            safeInterruptJoinThread(rxThread);
            txThread = null;
            rxThread = null;
        }

        // update fields
        if (tx != null && rx != null && txThread != null && rxThread != null) {
            mTxWorker = tx;
            mRxWorker = rx;
            mTxThread = txThread;
            mRxThread = rxThread;
        }
    }

    /**
     * Stop Tx./Rx. operation
     */
    public synchronized void stop() {
        stopInternal();
    }

    /**
     * Close the USB-Serial driver
     */
    public synchronized void close() {
        stopInternal();

        safeCloseUsbSerial(mUsbSerial);
        mUsbSerial = null;

        mContext = null;
        mListener = null;
        mUsbManager = null;
    }

    public synchronized void writeAsync(byte[] data, int offset, int n) {
        if (mTxWorker == null)
            throw new IllegalStateException();

        mTxWorker.putWriteData(data, offset, n);
    }

    private void stopInternal() {
        if (mTxThread != null) {
            safeInterruptJoinThread(mTxThread);
            mTxThread = null;
        }
        if (mRxThread != null) {
            safeInterruptJoinThread(mRxThread);
            mRxThread = null;
        }
        mTxWorker = null;
        mRxWorker = null;
    }

    private boolean setupUSB(Context context) {
        // Get UsbManager from Android.
        final UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        // Find the first available driver.
        UsbSerialDriver driver = UsbSerialProber.findFirstDevice(manager);

        if (driver != null) {
            try {
                driver.open();
                driver.setParameters(
                        115200, UsbSerialDriver.DATABITS_8,
                        UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                // Deal with error.
                Log.e(TAG, "IOException occurred in setupUSB()", e);
                safeCloseUsbSerial(driver);
                driver = null;
            } finally {
            }
        }

        if (driver != null) {
            mUsbManager = manager;
            mUsbSerial = driver;
        }

        return (driver != null);
    }

    private void cleanupUSB() {
        if (mUsbSerial != null) {
            safeCloseUsbSerial(mUsbSerial);
            mUsbSerial = null;
        }
        if (mUsbManager != null) {
            mUsbManager = null;
        }
    }

    private static void safeCloseUsbSerial(UsbSerialDriver driver)    {
        if (driver == null) {
            return;
        }

        try {
            driver.close();
        } catch (IOException e2) {
            // eat all exceptions
        }
    }

    private static void safeInterruptJoinThread(Thread thread) {
        if (thread == null)
            return;

        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted - safeInterruptJoinThread()", e);
        }
    }
}