#include <Boards.h>
#include <Firmata.h>

#include <DHT.h>

#define DHTPIN 6
#define DHTTYPE DHT11

DHT dht(DHTPIN, DHTTYPE);

void stringCallback(char *myString) {
  Firmata.sendString(myString);
}

void setup() {
  dht.begin();
  Firmata.setFirmwareVersion(0, 1);
  Firmata.attach(STRING_DATA, customCommandCallbackFunction);
  Firmata.begin(57600);
}

void loop() {
  while(Firmata.available()) {
    Firmata.processInput();
  }
}

void customCommandCallbackFunction(char *str) {
  float h = dht.readHumidity();
  float t = dht.readTemperature();

  char buf[100] = {0};
  append(buf, h);
  append(buf, ";");
  append(buf, t);
  Firmata.sendString(buf);
}

void append(char* buf, char* toAppend) {
  sprintf(buf, "%s%s", buf, toAppend);
}

void append(char* buf, float f) {
  int integralPart = (int)f;
  int decimalPart = (f - integralPart) * 100;
  sprintf(buf, "%s%d.%d", buf, integralPart, decimalPart);
}

//For debugging purpose
void echoService() {
  char val;
  val = Serial.read();
  if (val != -1) {
    Serial.print(val);
  }
}

//For debugging purpose
void printTemperatureAndHumidity() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();

  if (isnan(t) || isnan(h)) {
    Serial.println("Failed to read input from DHT.");
  } 
  else {
    Serial.print("Humidity: ");
    Serial.print(h);
    Serial.print(" %\t");
    Serial.print("Temperature: ");
    Serial.print(t);
    Serial.print(" *C");
    Serial.println();
  }
  delay(1000);
}










