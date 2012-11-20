#include <LCD4x20.h>

#define BASE 10

//LCD4x20 (int rsPin, int enablePin, int d4, int d5, int d6, int d7);
LCD4x20 lcd = LCD4x20(6, 7, 8, 9, 10, 11);

// for itoa int conversion. since int is signed 16-bit, max number 32767  we need 5 chars, plus one sign and one string terminator, so 7
// since we are reading 10-bit ADC (max 1023), we should only every need 5 total.
char buffer[5];

char teststr[] = { "ABCDEFGHIJKLMOPQRSTUVWXYZabcdefghijklmopqrstuvwxyz01234567890ABCDEFGHIJKLMOPQRST" };

char twolines[] = { "this is more then twenty characters "};

void setup() {
pinMode(13, OUTPUT); //we'll use the debug LED to output a heartbeat
Serial.begin(57600);

lcd.init();
lcd.setMode(MODE_SHIFT_UP);
lcd.print(teststr);
}

void loop() {
  //itoa(analogRead(5), buffer, BASE);
  //Serial.println(buffer);  
  
  while (Serial.available()) {
    lcd.print(Serial.read());  
  }
}
