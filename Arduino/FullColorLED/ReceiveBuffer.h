
#pragma once

template<size_t SIZE>
class ReceiveBuffer {
public:
  ReceiveBuffer() : pos_(0) {
  }
  
  bool put(char c)
  {
    if (pos_ < SIZE)
    {
      buff_[pos_] = c;
      pos_ += 1;
      buff_[pos_] = '\0';  // add termination
      return true;
    }
    else
    {
      return false;
    }
  }
  
  bool isEmpty() const {
    return (pos_ == 0);
  }
  
  bool isFull() const {
    return (pos_ == SIZE);
  }
  
  int count() const {
    return pos_;
  }
  
  const char *getString() const {
    return buff_;
  }

  void clear() {
    pos_ = 0;
    buff_[pos_] = '\0';
  }

private:
  char buff_[SIZE + 1];
  unsigned int pos_;
};

