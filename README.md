Android + Arduino test program (Full color LED controls)
===

![Nexus 5 + Arduino UNO + Full color LED](https://raw.github.com/h6ah4i/android-arduino-fullcolorled/master/doc/images/pic1.jpg)

Requirements (Hardware)
---
- Arduino UNO board
- Android device (Android 3.2+)
- USB host cable (micro B plug - standard A receptacle)
- USB 2.0 cable (standard A plug - standard B plug)
- Full color LED (common anode), Resister (1 kOhm) x 3, Bread board, Jumper wires
- (+5V AC adapter)

Setup
---

### Android

1. Install 'Android Studio (v0.3.6 or later)'
2. Place usb-serial-for-android library in Android/UsbSerialLibrary/src/main/java directory  (see. [Android/UsbSerialLibrary/src/main/java/NOTE.txt](../master/Android/UsbSerialLibrary/src/main/java/NOTE.txt))
3. Import Android project (Android/)
4. Build & Run


### Arduino

1. Install 'Arduino IDE (v1.0.5 or later)'
2. Open Arduino project (Arduino/FullColorLED)
3. Build & Upload to target


### Hardware

Make the following curcuit on bread boead.

- LED (Anode): +5V
- LED (RED) -> Register -> DIGITAL ~9
- LED (GREEN) -> Register -> DIGITAL ~10
- LED (BLUE) -> Register -> DIGITAL ~11

Connect Android device and Arduino board with USB cables.

- {Android} - {USB host cable} - {USB standard cable} - {Arduino UNO}


Command protocol
---

### Common format
"[command] [parameter]\n"

### Commands
- "R brightness(0-255)"
    - ex.) "R 100"
- "G brightness(0-255)"
    - ex.) "G 100"
- "B brightness(0-255)"
    - ex.) "B 100"


Depenedency libraries
---
- usb-serial-for-android
    - (https://github.com/mik3y/usb-serial-for-android)

