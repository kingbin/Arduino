/**
Chatduino is Licensed under Attribution-Noncommercial-Share Alike 3.0 http://creativecommons.org/licenses/by-nc-sa/3.0/us/
Copyright (c) 2009 Andrew Rapp. All rights reserved.
*/

#include <Ethernet.h>

// set to 1 for serial debug
#define CHATDUINO_DEBUG 0
// set to 1 if using wiznet812
#define WIZNET812MJ 0
// ignores html in messages
#define SKIP_HTML 1

// wiznet reset
int resetPin = 9;

long start = millis();

//long start = 0;
uint16_t cmdsRecvd = 0;
uint16_t msgsRecvd = 0;
long lastReconnect = 0;
uint16_t reconnects = 0;

uint16_t seq = 0;

static byte mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
static byte ip[] = { 192, 168, 1, 99 };

// I found two IPs for toc.oscar.aol.com.  these may change over time
//static byte server[] = { 64, 12, 202, 14 }; // toc
static byte server[] = {  64, 12, 202, 7 }; // toc
static const uint16_t port = 5190;

Client client(server, port);

// define TOC strings
static const char cmd_flapon[] = "FLAPON\r\n\r\n";
static const char cmd_init_done[] = "toc_init_done";
static const char cmd_login1[] = "toc2_signon login.oscar.aol.com 29999 ";
static const char cmd_login2[] = " English \"TIC:TOC2\" 160";
static const char info[] = "No info";
static const char tocSetInfo[] = "toc_set_info \"";
static const char special[] = "${}[]()\"\\";
static const char cmd_send[] = "toc2_send_im ";
static const char digits[] = "0123456789abcdef";
// roast pass
static const char roast[] = "Tic/Toc";

// toc commands
const char nick[] = "NICK";
const char signon[] = "SIGN_ON";
const char im_in2[] = "IM_IN2";

// arbitrary length
const uint16_t commandLen = 14;
char command[commandLen];

const uint8_t cmdStart = 42; // *
const uint8_t space = 32;
const uint8_t dquote = 34;
const uint8_t zero = 0;

void write(uint8_t b) {
  
  if (CHATDUINO_DEBUG) {
    Serial.print("write:");

    if (b >= space && b <= 126) {
      Serial.println(b);
    } else {
      Serial.println(b, DEC);
    }    
  }
  
  client.write(b);
}

void write(const char *str) {
  if (CHATDUINO_DEBUG) {
    Serial.print("write:");
    Serial.println(str);
  }
  
  client.write(str);
}

void write(const prog_uchar *str) {
  while (pgm_read_byte(str++) != 0) {
    client.write(pgm_read_byte(str - 1));
  }
}

void flush() {
  client.flush();
}

int available() {
  int val = client.available();

  return val;
}

bool wait(int numBytes, long timeout) {
  long wstart = millis();

  while (available() < numBytes) {
    if (millis() - wstart > timeout) {
      return false;
    }

    if (CHATDUINO_DEBUG) Serial.print("waiting for: ");
    if (CHATDUINO_DEBUG) Serial.print(numBytes, DEC);
    if (CHATDUINO_DEBUG) Serial.println(" bytes");

    delay(20);
  }

  return true;
}

int read() {
  wait(1, 5000);

  int val = client.read();

//  if (CHATDUINO_DEBUG) {
//    if (CHATDUINO_DEBUG) Serial.print("read:");
//    
//    if (val >= space && val <= 126) {
//      if (CHATDUINO_DEBUG) Serial.println((char)val);
//    } else {
//      if (CHATDUINO_DEBUG) Serial.println(val, DEC);
//    }
//  }

  return val;
}

uint8_t skip(uint8_t n) {
  uint8_t rem = n;

  if (CHATDUINO_DEBUG) Serial.print("skip: ");
  if (CHATDUINO_DEBUG) Serial.println(n, DEC);

  while (rem > 0) {
    read();
    rem--;
  }

  return n;
}

uint16_t read16bit() {
  uint16_t temp = read();
  return (temp << 8) + read();
}

void sendHeader(bool data, uint16_t len) {
  // ignoring send limit

  write(cmdStart); // *

  if (data) {
    write(2); // DATA
  } 
  else {
    write(1); // signin
  }

  write((seq >> 8) & 0xff); // seq high
  write(seq & 0xff); // seq low

  seq++;

  write((len >> 8) & 0xff);
  write(len & 0xff);
}

void sendFrame(bool data, const char *charPtr, bool nullTerm) {
  if (nullTerm) {
    // special handling for cammands that end with null terminator.  the write command will not write the term char so we must explicitly write it and add it to the length
    sendHeader(data, strlen(charPtr) + 1);
    write(charPtr);
    write(zero);
  } 
  else {
    sendHeader(data, strlen(charPtr));
    write(charPtr);
  }

  flush();
}

uint32_t magicNumber(char *username, char *password) {
  uint32_t a = (uint32_t) (username[0] - 96) * 7696 + 738816;
  uint32_t b = (uint32_t) (username[0] - 96) * 746512;
  uint32_t c = (uint32_t) (password[0] - 96) * a;

  return c - a + b + 71665152;
}

uint8_t readPart(uint16_t len, uint16_t &bytesRead, char *dest, uint16_t destLen) {

  int in = -1;
  uint8_t pos = 0;

  bool skip = false;

  while (true) {
    in = read();

    if (in == -1) {
      return 1;
    }
    
    // skip html.  far from correct
    if (in == '<' && SKIP_HTML) {
      skip = true;
    }

    // TODO unescape html entities: &lt;, &gt; etc.
    
    if (!skip) {
      // read up to destLen into char array
      if (pos < destLen) {
        dest[pos++] = in;
      }
    }

    if (in == '>' && skip) {
      skip = false;
    }

    bytesRead++;

    if (in == ':' || bytesRead == len) {
      // terminate string
      if (in == ':') {
        // don't include colon
        dest[pos - 1] = 0;
      } 
      else {
        dest[pos] = 0;
      }

      return 0;
    }
  }
}

/**
 * Attempts to read a frame from the toc server.  waits up to n-secs for the start of data.
 * Unfortunatley this must block (wait for data) because we are not storing any of the incoming data or state
 */
uint8_t readMessage(char *from, uint16_t fromLen, char *msg, uint16_t msgLen) {

  // all packets seem to start with: * <2>
  // guess it would have killed them to use something reliable, such as a start byte

  wait(5, 10000);

  uint8_t in = read();
  uint8_t lastIn = 0;

  // TODO check for -1 from read()

  while (true) {
    if (in == 2 && lastIn == cmdStart) {

      // start of command, maybe
      // next two bytes should be sequence. who cares
      skip(2);

      // next 2 bytes are length
      uint16_t len = read16bit();

      // now read len bytes
      uint16_t bytesRead = 0;

      uint8_t retcode = readPart(len, bytesRead, command, commandLen);

      if (retcode != 0) {
        return retcode;
      }

      if (strcmp(command, im_in2) == 0) {
        // proceed with the message

        // read from users
        uint8_t retcode = readPart(len, bytesRead, from, fromLen);

        if (retcode != 0) {
          return retcode;
        }

        // skip this junk: F:F:
        skip(4);
        bytesRead += 4;

        retcode = readPart(len, bytesRead, msg, msgLen);

        if (retcode != 0) {
          return retcode;
        }

        // success
        return 0;
      } 
      else {
        // something else
        // consume remaining bytes
        skip(len - bytesRead);

        return 1;
      }
    }

    lastIn = in;
    in = read();
  }

  return 1;
}

uint8_t login(char *screenName, char *pass) {
  if (!client.connected()) {
    return 1;
  }

  write(cmd_flapon);
  flush();

  // read signon.. next 10 bytes
  skip(10);

  sendHeader(false, strlen(screenName) + 8);

  // send FLAP VERSION (1) as big endian 32-bit.. weird
  write(zero);
  write(zero);
  write(zero);
  write((uint8_t) 1);

  // send TLF TAGS as 16-bit
  write(zero);
  write((uint8_t) 1);

  // send screenname length as 16-bit
  write(zero);
  write(strlen(screenName));

  // send screenname
  write(screenName);
  flush();

  // compute magic number of screen name and write to command buffer 
  long magic = magicNumber(screenName, pass);
  // max unsigned long is 4294967296, so 10 + 1 (null char) digits
  // we're using the command buffer to save memory
  ltoa(magic, command, 10);

  // now we send the login info
  // but before we send any data, we have to send the length of the data

  // the 3 is for 2 spaces and the null term char: '\0'
  // roast length is strlen(pass)*2 + 2
  sendHeader(true, strlen(cmd_login1) + strlen(screenName) + strlen(pass) * 2
    + 2 + strlen(cmd_login2) + strlen(command) + 3);

  // now we can send the actual data
  write(cmd_login1);
  write(screenName);
  write(space); // space

    // ascii hex start chars
  write((uint8_t) '0');
  write((uint8_t) 'x');

  // for each character in password, send exclusive OR of roast (two hex: high + low) of each value
  for (uint8_t i = 0; i < strlen(pass); i++) {
    // exclusive bitwise or
    uint8_t r1 = pass[i] ^ roast[i % 7];

    write(digits[(r1 >> 4) & 0xf]);
    write(digits[r1 & 0xf]);
  }

  write(cmd_login2);
  write(space); // space
  write(command);
  write(zero); // '\0'
  flush();

  // read SIGN_ON:TOC2.0
  readMessage(NULL, 0, NULL, 0);

  if (strcmp(command, signon) != 0) {
    // bad
    return 1;
  }

  // read NICK:screen-name
  readMessage(NULL, 0, NULL, 0);

  if (strcmp(command, nick) != 0) {
    // bad
    return 2;
  }

  sendFrame(true, cmd_init_done, true);
  flush();

  // almost there
  sendHeader(true, strlen(tocSetInfo) + strlen(info) + 2);

  write(tocSetInfo);
  write(info);
  write(dquote);
  write(zero);
  flush();

  // login complete

  // drain buffer: read CONFIG2, NICK commands

  while (available() > 0) {
    if (readMessage(NULL, 0, NULL, 0) == 0) {
      // successful read
      if (CHATDUINO_DEBUG) Serial.print("recv cmd");
      if (CHATDUINO_DEBUG) Serial.println(command);
    }
  }

  return 0;
}

/**
 * Send an instant message.  Assumes the recipient is online.
 * Message must be less than 1024 chars
 */
uint8_t sendMessage(char *to, char *msg) {
  
  // we don't need to check for max length on Arduino because it doesn't have enough memory for a string this big
//  if (strlen(msg) >= 1024) {
//    // message too long
//    return 1;
//  }

  // count special chars
  uint16_t scount = 0;

  for (uint16_t i = 0; i < strlen(msg); i++) {
    for (uint8_t k = 0; k < sizeof(special); k++) {
      if (msg[i] == special[k]) {
        scount++;
        break;
      }
    }
  }

  sendHeader(true, strlen(cmd_send) + strlen(to) + 2 + strlen(msg) + scount
    + 2);

  write(cmd_send);
  write(to);
  write(space);
  write(dquote);

  for (uint16_t i = 0; i < strlen(msg); i++) {

    for (uint8_t k = 0; k < sizeof(special); k++) {
      if (msg[i] == special[k]) {
        write((uint8_t) 92); // backslash
        break;
      }
    }

    write((uint8_t) msg[i]);
  }

  write(dquote);
  write(zero);

  flush();

  if (client.connected()) {
    return 0;
  }

  return 2;
}

void connect(char *screenName, char *pass) {
  // generate 16-bit initial sequence
  seq = random(0, 0xffff) & 0xffff;

  if (CHATDUINO_DEBUG) {
    Serial.print("seq:");
    Serial.println(seq, DEC);
  }

  if (WIZNET812MJ) {
    // wire reset pin to arduino (use a resistor!)
    if (CHATDUINO_DEBUG) Serial.println("Reset");
      digitalWrite(resetPin, LOW);
      pinMode(resetPin, OUTPUT);
      delayMicroseconds(5);
      // set floating
      pinMode(resetPin, INPUT);
  }
  
  if (CHATDUINO_DEBUG) Serial.println("Begin eth");

  Ethernet.begin(mac, ip);
  delay(1000);

  if (client.connect()) {
    if (CHATDUINO_DEBUG) Serial.println("connected");

    if (login(screenName, pass) == 0) {
      if (CHATDUINO_DEBUG) Serial.println("login ok");
    } 
    else {
      if (CHATDUINO_DEBUG) Serial.println("login fail");
    }
  } 
  else {
    if (CHATDUINO_DEBUG) Serial.println("conn. fail");
  }
}

// Paul (pololu) avail mem function
extern int __bss_end;
extern void *__brkval;

int get_free_memory() {
  int free_memory;

  if((int)__brkval == 0)
    free_memory = ((int)&free_memory) - ((int)&__bss_end);
  else
    free_memory = ((int)&free_memory) - ((int)__brkval);

  return free_memory;
}

// if you don't have a aim account, go to http://www.aim.com/ and get one!

// replace with your AIM screenname
char screenName[] = "yourscreenname";
// replace with your password
char pass[] = "yourpassword";

// authorized user.  this user is authorized to send signoff, pinWrite commands
char authUser[] = "";

// usually we can rely on strlen to get the string size but all bets are off after the null term char is moved, so we save it in a var
// BTW, this is very important to prevent buffer overflows
const uint16_t msgLen = 50;
// holds message.  make sure size is sufficient
// add 1 for null character
char msg[msgLen + 1];

// always make length larger than your screen name.  this way you can use strcmp to check if a message is from you with some amount of security
const uint16_t fromLen = 18;
// holds from user when a message is received
char from[fromLen + 1];

// NOTE: always signoff from aim before uploading a new sketch, or powering off Arduino/Wiznet.  
// Otherwise AIM still thinks you are connected and will not allow a new connection.
// If this occurs it may take a few attempts to re-establish the connection.

void setup() {
  // use analog 0 to seed random.  you don't need to connect anything to this pin
  pinMode(0, INPUT);
  randomSeed(analogRead(0));

  if (CHATDUINO_DEBUG) Serial.begin(9600);

  // short delay so you can open the serial console after upload
  if (CHATDUINO_DEBUG) delay(3000);
  client.stop();
  connect(screenName, pass);
}

/**
 * Supports requests to read/write from/to pins.
 * Format:
 * analog read: ar[pin]
 * digital read: dr[pin]
 * analog write (pwm): aw[pin]=[val] (where val: 0-255)
 * digital write: dw[pin]=[val] (where val: 0 or 1)
 * 
 * For security, write requests are only allowed from the authUser user.  Feel free to change
 * Read requests are allowed from any screen name
 * Returns true if it successfully processes a request
 */
bool processPinRequest() {
  int pin = 0;
  
  if (strlen(msg) == 3 || strlen(msg) == 4) {
    if ((msg[0] == 'a' || msg[0] == 'd') && msg[1] == 'r') {
      // get pin number. unfortunately it returns a zero for error
      pin = atoi(msg + 2);
      
      bool invalid = false;
      
      if (msg[0] == 'a') {
        // analog read
        if (pin >= 0 && pin <= 5) {
          // place analog val in message
          itoa(analogRead(pin), msg, 10);
          sendMessage(from, msg);
          return true;
        }
      } else {
        // digital read
        if (pin >= 0 && pin <= 19) {
          // place digital val in message
          itoa(digitalRead(pin), msg, 10);
          sendMessage(from, msg);
          return true;
        }               
      }
    }
  } else if ((msg[0] == 'a' || msg[0] == 'd') && msg[1] == 'w' && strchr(msg, '=') != NULL && strlen(msg) >= 5 && strlen(msg) <= 8) {
    
    // deny write request if message from unauthorized screen name
    if (strcmp(from, authUser) != 0) {
      // denied
      return false;
    }
    
    // get location of equals char
    char *eq = strchr(msg, '=');
    // set equals to null char so atoi might work    
    *eq = '\0';
    // get pin.  starts at array position 2
    pin = atoi(msg + 2);
    // get set val.  starts 1 position after equals char
    int setVal = atoi(eq + 1);
          
    if (msg[0] == 'a') {
      // PWM request
      if ((pin == 3 || pin == 5 || pin == 6 || pin == 9 || pin == 10 || pin == 11) && (setVal >= 0 && setVal <= 255)) {
        // set pwm value
        analogWrite(pin, setVal);
        sendMessage(from, "ok");
        return true;
      }
    } else {
      // digital write.  pins 0-19    
      if (pin >= 0 && pin <= 19 && (setVal == 1 || setVal == 0)) {
        digitalWrite(pin, setVal);
        sendMessage(from, "ok");
        return true;
      }
    }
  }  
  
  // did not process a pin request
  return false;
}

void loop() {
  if (client.connected()) {
    if (client.available() > 4) {
      
      if (readMessage(from, fromLen, msg, msgLen) == 0) {
        // got a message
        
        msgsRecvd++;
        cmdsRecvd++;
        
        if (CHATDUINO_DEBUG) Serial.print("recv'd frm:");
        if (CHATDUINO_DEBUG) Serial.println(from);
        if (CHATDUINO_DEBUG) Serial.print("msg: ");
        if (CHATDUINO_DEBUG) Serial.println(msg);

        // default reply
        char helloMsg[] = "Chatduino says Hello";
        
        // example:
//        if (strcmp(msg, "turn on led") == 0) {
//          digitalWrite(0, HIGH);
//        }
        
        if (processPinRequest()) {
          // awesome.. we handled a pin request
        } else if (strcmp(msg, "reconnects") == 0) {
          itoa(reconnects, msg, 10);
          sendMessage(from, msg);
        } else if (strcmp(msg, "nummsgs") == 0) {
          itoa(msgsRecvd, msg, 10);
          sendMessage(from, msg);
        } else if (strcmp(msg, "numcmds") == 0) {
          itoa(cmdsRecvd, msg, 10);
          sendMessage(from, msg);
        } else if (strcmp(msg, "freemem") == 0) {
          itoa(get_free_memory(), msg, 10);
          sendMessage(from, msg);
        } else if (strcmp(msg, "uptime") == 0) {
          // uptime in hours
          // rolls over every 49 days
          ltoa((millis() - start)/3600000, msg, 10);
          sendMessage(from, msg);
        } else if (strcmp(msg, "signoff") == 0) {
          // always signoff before uploading a new sketch or aim gets confused
          // only allow from specified screen name.  default is login user
          if (strcmp(from, authUser) == 0) {
            client.stop();
            while (1) {};            
          }
        } else if (strcmp(msg, "reconn") == 0) {
          client.stop();
        } else {         
          sendMessage(from, helloMsg);
        }
      } 
      else {
        // this is an aim command.  ignore
        cmdsRecvd++;
        if (CHATDUINO_DEBUG) Serial.print("cmd:");
        if (CHATDUINO_DEBUG) Serial.println(command);
      }
    }
  } 
  else {
    if (CHATDUINO_DEBUG) Serial.println("lost conn.");
    
    if (millis() - lastReconnect < 60000) {
      // wait 5 minutes
      delay(300000);      
    } else {
      delay(3000);      
    }
    
//  if (CHATDUINO_DEBUG) Serial.println("reconn conn.");
    
    client.stop();
    connect(screenName, pass);
    lastReconnect = millis();
    reconnects++;
  }
}
