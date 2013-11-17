#pragma ocne

// includes
#include "Arduino.h"

class SerialStdoutAdapter {
public:
	static void setup() {
		static FILE uartout;
		// set Serial as stdout
		fdev_setup_stream(
			&uartout, serial_write, NULL, _FDEV_SETUP_WRITE);
		stdout = &uartout;
	}

private:
	static int serial_write(char c, FILE *stream)
	{
		(void) stream;

		if (Serial.write(c) > 0) {
		  return 0;
		} else {
		  return -1;
		}
	}
};