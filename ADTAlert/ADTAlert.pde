/*
* ********* ADT Alerts ********
* Alerts sent by XBee Radios
* Chris Blazek http://chrisblazek.me
*/
#define VERSION "1.002a"
int PA = 2;
int PB = 3;

void setup() {
  pinMode(PA, INPUT);
  pinMode(PB, INPUT);
  Serial.begin(9600);
}

void loop() {
  if (digitalRead(PA) == HIGH) {
    Serial.print("A");
  }
  if (digitalRead(PB) == HIGH) {
    Serial.print("B");
  }

  delay(500);
}
