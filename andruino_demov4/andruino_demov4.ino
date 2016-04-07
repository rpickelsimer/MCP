#include <SoftwareSerial.h>
#include <TinyGPS.h>
#include <SPI.h>
#include <SD.h>
 
/* Bluetooth is on dedicated serial pins (0,1)
 Make sure to unplug when uploading sketches!
 or using serial monitor.
 UBLOX NEO-6M GPS module is 9600 3.3v but has onboard converter
*/

SoftwareSerial gpsSerial(2, 3); // (rx,tx) create gps sensor connection
TinyGPS gps; // create gps object

const int CS = 10; // chip/slave select
const int LED1 = 7;  // green led
const int LED2 = 9;  // red led  ~

long lat,lon; // location

bool record = false;        // save location stream to file
bool display_gps = false;  // send location stream to display

File dataFile;  // "log.csv" SD card

void setup() {
  Serial.begin(9600); // connect serial
  gpsSerial.begin(9600); // connect gps sensor 
  
  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);   
  
  Serial.print("Initializing SD card...");  
  pinMode(CS, OUTPUT);

  // if card is present, try to initialize
  if (!SD.begin(CS)) 
    Serial.println("Card failed, or not present");
  else   
    Serial.println("card initialized.");
  }

void loop() {
  
  while(gpsSerial.available()){ // check for gps data
    if(gps.encode(gpsSerial.read())) { // encode gps data
      gps.get_position(&lat,&lon); // get latitude and longitude
      
      if (record) {
        dataFile = SD.open("log.csv", FILE_WRITE);
        if (dataFile) {
          String dataString = String(lat) + "," + String(lon);
          dataFile.println(dataString);
          dataFile.close();          
        }
        // if the file isn't open, pop up an error:
        else {
          Serial.println("error opening log.csv");  
        } 
      }
      if (display_gps) {
        // display position
        Serial.print("lat: ");Serial.print(lat);Serial.print(" ");// print latitude
        Serial.print("lon: ");Serial.println(lon); // print longitude 
      } 
    }
  }
  
  
  /* State machine */
  /*****************/
  String text; // arduino is weird, that is all. may just have too many locals  
  if (Serial.available() > 0) {  // if there is data to be read 
 
  char c = Serial.read();
  int pin, val, bin;
  //if (Serial.available() > 0) 
    pin = Serial.parseInt();
  //if (Serial.available() > 0) {
    Serial.read();
    val = Serial.parseInt();
  //} 
  bin = pin;
    
    switch (c) {
      case 'p':                  // set pinMode      
        pinMode(pin, val);
        break; 
        
      case 'a':                 // PWM        %256 if you change seekbar       
        analogWrite(pin, val);  // accepts values 0 - 255
        break;
        
      case 'w':                  // digitalWrite      
        digitalWrite(pin, val);
        break;
        
      case 'c':                  //
        text = String(lat) + "," + String(lon);
        Serial.println(text);  
        break; 
        
      case 's':                 // toggle ave file        
        if (bin > 0)
          record = true;
        else record = false;
        break;
         
      case 'g':                 // togglesend GPS to serial        
        if (bin > 0)
          display_gps = true;
        else display_gps = false;
        break; 
        
      case 'f':                // file to serial
        // re-open the file for reading:
        dataFile = SD.open("log.csv", FILE_READ);
        if (dataFile) {
          Serial.println("log.csv:");   
          // read from the file until there's nothing else in it:
          while (dataFile.available()) {
            Serial.write(dataFile.read());  
          }          
          // close the file:
          dataFile.close();
        } else {
        // if the file didn't open, print an error:
        Serial.println("error opening test.txt");
        }
        break;
        
      case 'd':
        //delay = pin; not implemented  
      break;
        
      case 'i':                    // digitalWrite        
        if (bin == 0) {
          digitalWrite(LED1, LOW);  // if 1, switch LED Off
          Serial.println("LED OFF");  // print message
        } 
        else if (bin == 1) {
          digitalWrite(LED1, HIGH); // if 0, switch LED on
          Serial.println("LED ON");
        }
        break; 
        
      case 'j':                    // digitalWrite  
        if (bin == 0) {
          digitalWrite(LED2, LOW);  // if 1, switch LED Off
          Serial.println("LED OFF");  // print message
        } 
        else if (bin == 1) {
          digitalWrite(LED2, HIGH); // if 0, switch LED on
          Serial.println("LED ON");
        }
        break; 
      }  
  }
}

// delay function from Italian TinyGPS guy
// that does not disrupt gps feed
static void smartdelay(unsigned long ms)
{
  unsigned long start = millis();
  do {
    while (gpsSerial.available())
      gps.encode(gpsSerial.read());
  } while (millis() - start < ms);
}
