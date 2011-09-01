/*
* ********* Doorbell Basic BUTTON ********
* requires pre-paired XBee Radios
* and the BELL program on the receiving end
* by Rob Faludi http://faludi.com
*/
#define VERSION "1.00a0"
int BUTTON = 2;
int LED = 11;

void setup() {
  pinMode(BUTTON, INPUT);
  pinMode(LED, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  // send a capital D over the serial port if the button is pressed
  if (digitalRead(BUTTON) == HIGH) {
    Serial.print('D');
    delay(500); // prevents overwhelming the serial port

    if(Serial.available() > 0){
      if(Serial.read() == 'K'){
        digitalWrite(LED, HIGH);
      }
    }
  } else {
    digitalWrite(LED, LOW);
  }
}
