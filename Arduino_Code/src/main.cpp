#include <Arduino.h>
#include <string.h>


// prototypes
void receiveData();
void publishData();

boolean newData = false;

// buffer for reading data
const byte NUMBYTES = 32;
byte buffer[NUMBYTES];
byte numReceived = 0;

void setup() {
  // begin serial communication at 9600 bps
  Serial.begin(9600);
  
}

void loop() {
  receiveData();
  publishData();
}

// function to receive data
void receiveData()
{

  // indicates if data is currently being received
  static boolean recvInProgress = false;

  // index of byte received
  static byte index = 0;

  // start marker
  const byte STARTMARKER = 0x3C;

  // end marker
  const byte ENDMARKER = 0x3E;

  // the current read byte
  byte rb;

  while(Serial.available() > 0 && newData == false)
  { 
    // read one byte at a time 
    rb = Serial.read();
  
    // check if we are receiving
    if (recvInProgress == true)
    {
      // have not yet received end marker
      if (rb != ENDMARKER)
      {
        buffer[index] = rb;
        index++;

        // buffer is too small
        if (index >= NUMBYTES)
        {
          // TODO send message that buffer is too small
          index = NUMBYTES - 1;
        }

      }
      // received the end marker
      else if (rb == ENDMARKER)
      {
        recvInProgress = false;
        newData = true;
        index = 0;
      }

    }
    else if (rb == STARTMARKER)
    {
      // received start marker, start reading data
      recvInProgress = true;   

    }
  }
}

// function to show the data
void publishData()
{
  if (newData == true)
  {
    for (byte i = 0; i < NUMBYTES; i++)
    {
      Serial.print(buffer[i], INT);
      Serial.print(' ');
      newData = false;
    }
  }
  
}
