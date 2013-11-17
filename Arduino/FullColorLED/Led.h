#pragma once

class Led {
public:
  Led(int port)
    : port_(port)
  {
  }

  void setup() {
    pinMode(port_, OUTPUT);
    analogWrite(port_, 255);  // OFF
  }

  void set(int brightness)
  {
    brightness = constrain(255 - brightness, 0, 255);
    analogWrite(port_, brightness);
  }
private:
  const int port_;
};
