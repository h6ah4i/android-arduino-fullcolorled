// includes
#include <stdio.h>
#include "SerialStdoutAdapter.h"
#include "ReceiveBuffer.h"
#include "Led.h"
#include "Command.h"

// variables
static ReceiveBuffer<32> buff;
static Led RED(9), GREEN(10), BLUE(11);


void setup()
{ 
  RED.setup();
  GREEN.setup();
  BLUE.setup();

  Serial.begin(115200);
  SerialStdoutAdapter::setup();
} 

void loop()  { 
  doSerialRxProcess();
}

static void processCommand(const Command &cmd)
{
  switch (cmd.type())
  {
  case CMD_R:
    RED.set(cmd.arg_i(0));
    printf("CMD_R %d\n", cmd.arg_i(0));
    break;
  case CMD_G:
    GREEN.set(cmd.arg_i(0));
    printf("CMD_G %d\n", cmd.arg_i(0));
    break;
  case CMD_B:
    BLUE.set(cmd.arg_i(0));
    printf("CMD_B %d\n", cmd.arg_i(0));
    break;
  case CMD_UNKNOWN:
  default:
    break;
  }
}

static void doSerialRxProcess() 
{
  if (!Serial.available())
    return;

  int c = Serial.read();

  if (c == '\n' || c == '\r')
  {
    if (buff.count() > 0)
    {
      Command cmd(CommandParser::parse(buff.getString()));
      processCommand(cmd);
      buff.clear();
    }
  }
  else
  {
    buff.put(c);
  }
}

