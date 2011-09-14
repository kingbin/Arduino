/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *  
 * This file is part of Droplet.
 *  
 * Droplet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * Droplet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with Droplet.  If not, see <http://www.gnu.org/licenses/>.
 */
 
/**
 * This sketch uses the XBee-Arduino library to display messages (twitter/gcalendar etc.) on a LCD.
 * Content may be user-initiated (pull) or system-initiated (push).  
 * User-initiated content is delivered when a menu item is selected (e.g. weather).  
 * System-initiated content is sent by the Java service, upon an external event (e.g. new twitter update).
 * <p/>
 * Supports four navigation buttons (escape, select, down/next and up/back)
 * <p/>
 * Error messages are printed to the LCD for a variety of situations, such as,
 * coordinator offline or disassociated, Java service not running, or Java service could not fulfill the request
 * in a timely manner. 
 * <p/>
 * Requires at least version 0.1.2 of the XBee-Arduino library
 * <p/>
 * Andrew Rapp (andrew.rapp@gmail.com)
 */
 
 // TODO all lcd print calls need to go through one function to set state correctly
 // TODO clock
 // TODO submenus
 // TODO allow user to select parameters to send to service (e.g. y/n, numbers, alphanumeric)
 // TODO support local services e.g. read and display local sensors/info
 // TODO check if XBee is associated at startup.  requires AtCommandRequest
 
#include <XBee.h>
#include <LCD4x20.h>

#define BASE_10 10
// a timeout occurs if a response is not received within this time
#define APPLICATION_TIMEOUT 7500

// milliseconds to wait for a XBee packet ACK
// response is usually very fast (within 500ms) but if radio is not associated it can take upwards of 5 seconds
#define STATUS_RESPONSE_TIMEOUT 500

#define LCD_LINES 4

// Specify the Arduino pins that are connected to the LCD
int rsPin = 6;
int enablePin = 7;
int db4 = 8;
int db5 = 9;
int db6 = 10;
int db7 = 11;

// lcd control
LCD4x20 lcd = LCD4x20(rsPin, enablePin, db4, db5, db6, db7);

// navigation buttons
// down arrow or next page
int downButtonPin = 2;
// up arrow or previous page (analog 1)
 // Analog pins can be re-purposed as digital pins 14-19, woot! http://www.arduino.cc/en/Tutorial/AnalogInputPins
int upButtonPin = 15;
// call service, execute, enter etc.
int selectButtonPin = 3;
// go back.. exit etc.
int escButtonPin = 4;

// time of last select button press
long lastSelectButton;
             
int alertLedPin = 5;
// repurpose analog pin 0 as digital
int buzzerPin = 14;

// would be nice if we could query the pin instead
// true if led is flashing
bool flashAlertLed = false;
// true if led is currently on
bool alertLedPinState = false;
// time between flashes
int alertFlashDelay = 1000;
// time of last flash
long lastAlertFlash;

bool soundBuzzer = false;
bool buzzerState = false;
long lastBuzzerMillis;
int buzzerDelay = 1000;

// true if a next page exists
bool nextPage = false;
// true if a previous page exists
bool previousPage = false;

// Service id must match menu index
//     "build software"
//     "iTunes Next Track",
//        "Clock",
        
// TODO need menu hashmap for decoupling menu array index and service id.
// need reserved service ids for default services

char* menu[] = {
        "Google Calendar",
        "Twitter",
	"News",
	"Current Weather",
        "Weather Forecast",
        "Twitter Woot Search",
        "Get Date/Time",
	"Feed Cat",
        "Make Sandwich"
};

// size of menu[] array
int menuSize = 9;
bool menuIsDisplayed = false;
// index of current menu item
int currentMenuSelection = 0;
// last menu item selection
int lastMenuSelection = 0;
// position of the top of window
int menuWindowPosition = 0;

char rightArrow = (char) 126;

XBee xbee = XBee();
XBeeResponse response = XBeeResponse();
// create reusable response objects for responses we expect to handle 
ZBRxResponse rx = ZBRxResponse();
// not necessary
ModemStatusResponse msr = ModemStatusResponse();

// two bytes for menu selection (service) # and prev/next page request
uint8_t payload[] = { 0, 0 };

// SH + SL Address of coordinator XBee
XBeeAddress64 addr64 = XBeeAddress64(0x0013a200, 0x403e0f30);
ZBTxRequest zbTx = ZBTxRequest(addr64, payload, sizeof(payload));
ZBTxStatusResponse txStatus = ZBTxStatusResponse();

// we use this to convert the api id to a char array for lcd
char buffer[4];
bool associated = false;

//char timeChar[6];
//long start;

void setup() {  
  lcd.init();
  // does a line feed when buffer is full
  lcd.setMode(MODE_SHIFT_UP);
  lcd.println("Droplet v0.1");
  
  // iterate through chars
 // for (int i = 1; i < 256; i++) {
 //   lcd.print((char)i);
 //   
 //   if (((i + 1) % 80) == 0) {
 //     delay(10000);
 //   }
 // }
  
  delay(1000);
  
  lcd.clear();
  
  // TODO check that each menu listing doesn't exceed 19 chars
  printMenu(false, false);
  
  // Turn on internal pull up resistors for navigation buttons so we can connect the pins directly to ground.  
  // Internal pull ups will drive the pin to 5V when left floating.
  // The internal pull ups are configured by setting the pin to "input" (the default) and turning it on high (digitalWrite(button, HIGH))
  // Note: Do not set the pinMode to OUTPUT -- this will cause a short when the button is pressed and likely fry the atmega
  pinMode(escButtonPin, INPUT);
  digitalWrite(escButtonPin, HIGH);
  
  pinMode(downButtonPin, INPUT);
  digitalWrite(downButtonPin, HIGH);

  pinMode(upButtonPin, INPUT);
  digitalWrite(upButtonPin, HIGH);
  
  pinMode(selectButtonPin, INPUT);
  digitalWrite(selectButtonPin, HIGH);
  
  // configure alert LED
  pinMode(alertLedPin, OUTPUT);
  digitalWrite(alertLedPin, LOW);

  // configure alarm
  pinMode(buzzerPin, OUTPUT);
  digitalWrite(buzzerPin, LOW);
   
  // chirp buzzer
  // apparently this wakes my wife and makes her very angry
  //activateBuzzer(250);
  
  // start serial
  xbee.begin(9600);
  
  // flash led
  digitalWrite(alertLedPin, HIGH);
  delay(100);
  digitalWrite(alertLedPin, LOW);
}

void requestService(bool nextPageRequest, bool previousPageRequest) {

  menuIsDisplayed = false;
  
  lcd.clear();  
  lcd.println("Requesting Service");

  // service id indicator
  payload[0] = currentMenuSelection;
  // next/prev indicator byte
  payload[1] = 0;
  
  if (nextPageRequest && nextPage) {
    // set next page request bit
    payload[1] = payload[1] | 1;

  } else if (previousPageRequest && previousPage) {
    // set previous page request bit
    payload[1] = payload[1] | (1 << 1);
    /*
                  itoa(payload[1], buffer, 10);
                  lcd.print("payload[1]=");
                  lcd.print(buffer);
                  lcd.print("previous page");
                  delay(2000);
    */
  }

  // reset
  reset();
  
  long packetSendTime = millis();
  
  xbee.send(zbTx);
  
    // after sending a tx request, we expect a status response
    // wait up to half second for the status response
    if (xbee.readPacket(STATUS_RESPONSE_TIMEOUT)) {
        // got a response!

        // should be a znet tx status            	
    	if (xbee.getResponse().getApiId() == ZB_TX_STATUS_RESPONSE) {
    	   xbee.getResponse().getZBTxStatusResponse(txStatus);
    		
    	   // get the delivery status, the fifth byte
           if (txStatus.getDeliveryStatus() == SUCCESS) {
             	lcd.println("Waiting... ");
             
                 // we wait up to APPLICATION_TIMEOUT milliseconds for the response before giving up
                 xbee.readPacket(APPLICATION_TIMEOUT);
                 
                 if (xbee.getResponse().isAvailable()) {
                   if (xbee.getResponse().getApiId() == ZB_RX_RESPONSE) {
                   // now fill our zb rx class
                    xbee.getResponse().getZBRxResponse(rx);
                    lcd.clear();
                    // print char data directly from the packet payload, starting at the second byte
                    lcd.println(rx.getFrameData(), rx.getDataOffset() + 1, rx.getDataLength() - 1);
                    
                    if (rx.getData(0) & 1) {
                      // next page is available
                      nextPage = true;
                      
                      // print continuation marker
                      lcd.setCursor(4, 19);
                      lcd.print(rightArrow);
                    } else {
                      nextPage = false;
                    }
                    
                    // 4th bit is the prev. page indicator
                    if (rx.getData(0) & (1 << 3)) {
                      previousPage = true; 
                    } else {
                      previousPage = false;
                    }
            		
                    //lcd.print("roundtrip in");
                    //itoa(millis() - start, timeChar, 10);
                    //lcd.print(timeChar);        
                   } else {
                     itoa(xbee.getResponse().getApiId(), buffer, 10);
                     lcd.print("Unexpected packet");                     
                     lcd.println(buffer);       
                   }
                 } else {
                   // the XBee network is up but the Java app is not running or did not respond in time
                   lcd.clear();
                   lcd.println("No response from\napplication");
                 }
           } else {
             // offline, we assume
             // TODO print delivery status

             lcd.clear();
             lcd.println("No Network");
             
             // if this comes back too quickly, the button may still be pressed.. delay for button depress
             while (millis() - packetSendTime < 750) {
               delay(1);
             }
           }
        }      
    } else {
      // local XBee did not provide a timely TX Status Response -- should not happen
      // did you switch the TX/RX jumpers back to XBee?
       lcd.clear();
       lcd.println("Request timeout");
    } 
}

void reset() {
  nextPage = false;
  previousPage = false;  
}

void printMenu(bool down, bool up) {
          
  // not working
  bool redrawWindow = false;
  
  if (!menuIsDisplayed) {
    redrawWindow = true;
    /*
    lcd.println("lcd not disp");
    delay(1000);
    */
  }
  
       if (down) {
            
           // move menu down by 1
	  if ((currentMenuSelection + 1) == menuSize) {
            // end of menu. return to start
            currentMenuSelection = 0;
            menuWindowPosition = 0;           
            redrawWindow = true;            
          } else {
            currentMenuSelection++;
                         
            if (currentMenuSelection > (menuWindowPosition + LCD_LINES - 1)) {
                  /* debug stuff
                  lcd.println("moving window");
                  itoa(currentMenuSelection, buffer, 10);
                  lcd.print("cm=");
                  lcd.print(buffer);
                  itoa(menuWindowPosition, buffer, 10);
                  lcd.print(",win=");
                  lcd.print(buffer);                  
                  delay(3000);
                  */
                                
              // move top of window down one.  current selection is still line 4
              menuWindowPosition = currentMenuSelection - LCD_LINES + 1;
              redrawWindow = true;             
            }
          }  
        }
        
        if (up) {
            
          if (currentMenuSelection == 0) {
            // at first, move to last
            currentMenuSelection = menuSize - 1;
            menuWindowPosition = menuSize - LCD_LINES;
            redrawWindow = true;
          } else {
            currentMenuSelection--;
            
            if (currentMenuSelection < menuWindowPosition) {
                // move top of window up to current selection.  current selection is still line 0
                menuWindowPosition = currentMenuSelection;
                redrawWindow = true;
            }
          } 
        }
   
  reset();
  
  // how many lines to output
  int numMenuLinesToDisplay = LCD_LINES;
  
  if (menuSize < LCD_LINES) {
    numMenuLinesToDisplay = menuSize;
  }
  
  int menuIndexPos = menuWindowPosition;
  
  if (redrawWindow) {
    lcd.clear();
    
    /* debug stuff
    lcd.println("redraw");
    delay(1000);
    lcd.clear();
    */
    
    // redraw entire menu
    for (int i = 0; i < numMenuLinesToDisplay; i++) {
      
      if (menuIndexPos == currentMenuSelection) {
        // set first as selected
        lcd.setCursor(i + 1, 0);
        // TODO use arrow character
        lcd.print(rightArrow);
      }

      lcd.setCursor(i + 1, 1);
      lcd.print(menu[menuIndexPos]);

      menuIndexPos++;    
    }    
  } else {
    // only move the selection arrow
    if (lastMenuSelection != currentMenuSelection) {
      lcd.setCursor(currentMenuSelection - menuWindowPosition + 1, 0);
      lcd.print(rightArrow);
      lcd.setCursor(lastMenuSelection - menuWindowPosition + 1, 0);
      lcd.print(" ");
      
      // this is so quick we need to debounce
      //debounce();
      delay(250);
    } 
  }
  
  // save lastMenuSelection
  lastMenuSelection = currentMenuSelection;
  
  menuIsDisplayed = true;
  
  /*
                  itoa(currentMenuSelection, buffer, 10);
                  lcd.print("curmensel=");
                  lcd.print(buffer);
  */
  
}

void debounce() {
  delay(10);
}

void activateBuzzer(int howLong) {
  long now = millis();
  
  while (millis() - now < howLong) {
    buzz10Millis();
  }
}


// activates the buzzer with 533hz tone (approximately) for about 10 milliseconds
void buzz10Millis() {  
  for (int i = 0; i < 5; i++) {
    digitalWrite(buzzerPin, HIGH);
    delayMicroseconds(938);
    digitalWrite(buzzerPin, LOW);
    delayMicroseconds(938);
  }
}

void loop() {
    // reads a packet from Serial, if data is available; otherwise continues on
    xbee.readPacket();
    
    if (xbee.getResponse().isAvailable()) {
      // got something
      
      // TODO consolidate code with read w/ timeout
      if (xbee.getResponse().getApiId() == ZB_RX_RESPONSE) {
        menuIsDisplayed = false;
        
        lcd.clear();
                
        // This is a push service packet (i.e. no button was pressed)
        // we will display the contents of the packet to the LCD
        xbee.getResponse().getZBRxResponse(rx);
        
        // print char data directly from the packet payload, starting at the second byte
        lcd.println(rx.getFrameData(), rx.getDataOffset() + 1, rx.getDataLength() - 1);
        
         if (rx.getData(0) & 1) {
          // next page is available
          nextPage = true;
                      
          // print continuation marker
          lcd.setCursor(4, 19);
          lcd.print(rightArrow);
        } else {
          nextPage = false;
        }
        
        // check if led alert bit is on (2nd bit)
        if (rx.getData(0) & (1 << 1)) {
          // turn on alert LED
          digitalWrite(alertLedPin, HIGH);
          flashAlertLed = true;
        }

        // check if buzzer bit is on
        if (rx.getData(0) & (1 << 2)) {
          // turn on buzzer
          soundBuzzer = true;
        }
        
      } else if (xbee.getResponse().getApiId() == MODEM_STATUS_RESPONSE) {
     	// we don't care about this too much
        // the local XBee sends our modem status packets on certain events, like assoc./disassoc.
        xbee.getResponse().getModemStatusResponse(msr);
        
        //if (msr.getStatus() == ASSOCIATED) {
        //  associated = true;
        //  lcd.println("Associated");
        //} else if (msr.getStatus() == DISASSOCIATED) {
        //  lcd.println("Disassociated");
        //  associated = false;
        //} else {
        //  lcd.print("msr status: ");
        //  itoa(msr.getStatus(), buffer, 10);
        //  lcd.println(buffer);          
        //}
      } else {
        // 8/5 got a ZB_TX_STATUS_RESPONSE here??
        lcd.print("unexpected response:");
        itoa(xbee.getResponse().getApiId(), buffer, BASE_10);
        lcd.println(buffer);
      }
    }
    
    bool anyButtonPress = false;
    bool downButtonPress = false;
    bool upButtonPress = false;
                
    if (digitalRead(upButtonPin) == LOW) {
      upButtonPress = true;
    } else if (digitalRead(downButtonPin) == LOW) {
      downButtonPress = true;
    }
    
    if (downButtonPress || upButtonPress) {
      anyButtonPress = true;
    
      // TODO if nextPage flag is set, we issue next page request
      
      if (nextPage && downButtonPress) {
      	requestService(true, false);
      } else if (previousPage && upButtonPress) {
        requestService(false, true);
      } else if (menuIsDisplayed) {
        
        if (downButtonPress) {
          printMenu(true, false);
        } else {
          printMenu(false, true);
        }
        
        // debounce delay
        debounce();
      } else {
        // return to menu
        printMenu(false, false);
      }
    } else if (digitalRead(escButtonPin) == LOW) {
      anyButtonPress = true;
      // back to menu
      if (!menuIsDisplayed) {
        printMenu(false, false);
      } else {
        // nothing to do
      }
    } else if (digitalRead(selectButtonPin) == LOW) {
      anyButtonPress = true;
      
      // only makes sense if menu is active
      if (menuIsDisplayed) {
        // call service
        //lastSelectButton = millis();
        requestService(false, false);
      } else {
        //if (millis() - lastSelectButton > 500) {
          lastSelectButton = millis();
          lcd.clear();
          lcd.print("Nothing to Select! Hit Esc button");
        //} else {
          // too quick. user hasn't depressed button yet after last request
        //}
      }
    } 
    
    // any button to reset alert
    if (anyButtonPress) {
      
      if (flashAlertLed) {
        // reset alert led
        digitalWrite(alertLedPin, LOW);
        flashAlertLed = false;
      }
      
      if (soundBuzzer) {
        soundBuzzer = false; 
      }
    } else {
      if (flashAlertLed) {
        // flash led
        if (millis() - lastAlertFlash > alertFlashDelay) {
          // flash alert
          lastAlertFlash = millis();
        
          if (alertLedPinState) {
            digitalWrite(alertLedPin, LOW);
          } else {
            digitalWrite(alertLedPin, HIGH);
          }
        
          // toggle
          alertLedPinState = !alertLedPinState;
        }
                
        if (soundBuzzer) {
          
          if (millis() - lastBuzzerMillis > buzzerDelay) {
            // gets called about every buzzerDelay milliseconds and toggles buzzerState
            buzzerState = !buzzerState;
            lastBuzzerMillis = millis();
          }
          
          if (buzzerState) {
            // TODO type of buzzer is not optimal since we might miss button events while we are in the buzz method
            // but this risk should be nill now that it only blocks for 10 milliseconds at a time
            buzz10Millis();
          }
        }
      }
    }
}

