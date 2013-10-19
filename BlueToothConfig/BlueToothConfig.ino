#define AT 2

void setup() {
  pinMode(AT, OUTPUT);
  digitalWrite(AT, HIGH);

  Serial.begin(38400);
  delay(100);
  Serial.println("AT");
  delay(100);
  Serial.println("AT+NAME=Bluetooth-Slave");
  delay(100);
  Serial.println("AT+ROLE=0");
  delay(100);
  Serial.println("AT+CMODE=1");
  delay(100);  
  Serial.println("AT+PSWD=1234");  
  delay(100);
  Serial.println("AT+UART=57600,1,0");  
  delay(100);
  Serial.println("AT+RMAAD");
}

void loop() {
  delay(1000);
}


