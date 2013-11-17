#pragma once

// includes
#include <WString.h>

typedef enum {
  CMD_UNKNOWN = -1,
  CMD_R,
  CMD_G,
  CMD_B,
} CommandType;

class Command {
public:

  union Argument_t {
  public:
    int integer;

    Argument_t()
     : integer(0) 
    {
    }

    Argument_t(const Argument_t &s)
     : integer(s.integer)
    {
    }

    Argument_t(int x)
     : integer(x) 
    {
    }
  };


  Command(CommandType type)
    : type_(type), arg0_()
  {
  }

  Command(CommandType type, int arg0) 
    : type_(type), arg0_(arg0) 
  {
  }

  Command(CommandType type, const Argument_t &arg0) 
    : type_(type), arg0_(arg0) 
  {
  }

  CommandType type() const {
    return type_;
  }

  int arg_i(int no) const {
    switch (no) {
      case 0: return arg0_.integer;
    }
    return -1;
  }


private:
  CommandType type_;
  Argument_t arg0_;
};


class CommandParser {
private:
  static CommandType parseType(const String &stype)
  {
    if (stype.equalsIgnoreCase("R"))  return CMD_R;
    if (stype.equalsIgnoreCase("G"))  return CMD_G;
    if (stype.equalsIgnoreCase("B"))  return CMD_B;

    return CMD_UNKNOWN;
  }

  static Command::Argument_t parseIntArgument(const String &sarg)
  {
    const int len = sarg.length();
    char c;
    int value = 0;

    for (int i = 0; i < len; ++i)
    {
      c = sarg[i];

      if (c >= '0' && c <= '9') {
        value *= 10;
        value += (c - '0');
      } else {
        break;
      }
    }

    return Command::Argument_t(value);
  }

public:
  static Command parse(const char *cmd)
  {
    String scmd(cmd);
    CommandType type = CMD_UNKNOWN;
    Command::Argument_t args[1];
    
    int cnt = 0;
    int spos, epos;

    spos = 0;
    for (int i = 0; i < (1 + 1) && spos < (int) scmd.length(); ++i)
    {
      epos = scmd.indexOf(" ", spos);
      if (epos < 0) {
        epos = scmd.length();
      }
      if (epos < 0)
        break;

      if (cnt == 0)
      {
        // command type
        type = parseType(scmd.substring(spos, epos));
      }
      else
      {
        // arguments
        args[cnt - 1] = parseIntArgument(scmd.substring(spos, epos));
      }

      cnt += 1;
      spos = epos + 1;
    }

    if (cnt <= 1) {
      return Command(type);
    } else {
      return Command(type, args[0]);
    }
  }
};