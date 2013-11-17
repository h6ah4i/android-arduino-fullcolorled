package com.h6ah4i.android.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.SeekBar;

import java.io.UnsupportedEncodingException;

public class LedControlActivity extends Activity implements  SeekBar.OnSeekBarChangeListener {
    private static final String TAG = LedControlActivity.class.getSimpleName();

    private static final String TAG_UART_RX = "UART-RX";
    private StringBuilder mUartRxStringBuilder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        final SeekBar seekBarR = (SeekBar) findViewById(R.id.seekbar_red_brightness);
        final SeekBar seekBarG = (SeekBar) findViewById(R.id.seekbar_green_brightness);
        final SeekBar seekBarB = (SeekBar) findViewById(R.id.seekbar_blue_brightness);

        seekBarR.setOnSeekBarChangeListener(this);
        seekBarG.setOnSeekBarChangeListener(this);
        seekBarB.setOnSeekBarChangeListener(this);
    }

    private AsyncUsbSerial mAsyncUsbSerial;

    @Override
    protected void onResume() {
        super.onResume();
        mAsyncUsbSerial = new AsyncUsbSerial(this, new AsyncUsbSerial.EventListener() {

            @Override
            public void onReceived(byte[] data, int n) {
                LedControlActivity.this.onDataReceivedFromUsbSerial(data, n);
            }
        });

        mUartRxStringBuilder = new StringBuilder();

        mAsyncUsbSerial.open();
        if (mAsyncUsbSerial.isOpened()) {
            mAsyncUsbSerial.start();
        }
    }

    private void onDataReceivedFromUsbSerial(byte[] data, int n) {
//        Log.d(TAG, "Data received: " + n + " [bytes]");
        final StringBuilder sb = mUartRxStringBuilder;

        try {
            final String decodedStr = new String(data, 0, n, "UTF-8");
            sb.append(decodedStr);
        } catch (UnsupportedEncodingException e) {
        }

        while (true) {
            // find newline
            int i = sb.indexOf("\n");
            if (i < 0) {
                i = sb.indexOf("\r");
            }
            if (i < 0) {
                break;
            }

            String line = sb.substring(0, i + 1);
            sb.delete(0, i + 1);

//            Log.i(TAG_UART_RX, line);
        }
    }

    @Override
    protected void onPause() {
        if (mAsyncUsbSerial != null) {
            mAsyncUsbSerial.stop();
            mAsyncUsbSerial.close();
            mAsyncUsbSerial = null;
        }
        mUartRxStringBuilder = null;
        super.onPause();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int brightness = seekBar.getProgress();
        String command;

        switch (seekBar.getId()) {
            case R.id.seekbar_red_brightness:
                command = "R";
                break;
            case R.id.seekbar_green_brightness:
                command = "G";
                break;
            case R.id.seekbar_blue_brightness:
                command = "B";
                break;
            default:
                return;
        }

        if (mAsyncUsbSerial != null) {
            try {
                byte[] data = (command + " " + brightness + "\n").getBytes("US-ASCII");
                mAsyncUsbSerial.writeAsync(data, 0, data.length);
            } catch (UnsupportedEncodingException e) {
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
