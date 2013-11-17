#pragma once

class Led {
public:
  Led(int port)
    : port_(port), brightnessCoeff_(1.0f)
  {
  }

  Led(int port, float brightnessCoeff)
    : port_(port), brightnessCoeff_(brightnessCoeff)
  {
  }

  void setup() {
    pinMode(port_, OUTPUT);
    analogWrite(port_, 255);  // OFF
  }

  void set(int brightness)
  {
    brightness = constrain(255 - (int) (brightness * brightnessCoeff_ + 0.5f), 0, 255);
    analogWrite(port_, brightness);
  }
private:
  const int port_;
  const float brightnessCoeff_;
};
