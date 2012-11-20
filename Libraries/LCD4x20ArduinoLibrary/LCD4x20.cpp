/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *
 * This file is part of LCD4x20.
 *
 * LCD4x20 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LCD4x20 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCD4x20.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "LCD4x20.h"
extern "C" {
  #include <stdio.h>  //not needed yet
  #include <string.h>
  #include <inttypes.h>
  #include "WConstants.h"
}

//command bytes for LCD
// spec p.24
#define CMD_CLR 0x01
#define CMD_HOME 0x02

#define CMD_RIGHT 0x1C
#define CMD_LEFT 0x18

#define CMD_DISPLAY B00001000
#define CMD_DISPLAY_ON B00000100
#define CMD_DISPLAY_CURSOR_ON B00000010
#define CMD_DISPLAY_CURSOR_BLINKING_ON B00000001

#define CMD_ENTRY_MODE_SET B00000100
#define CMD_ENTRY_MODE_SET_INCREMENT B00000010
#define CMD_ENTRY_MODE_SET_DISP_SHIFT B00000001

// NOTES:
// DDRAM address is position of last written or read char inc/dec auto when set to

LCD4x20::LCD4x20 (int rsPin, int enablePin, int d4, int d5, int d6, int d7) {

	_rsPin = rsPin;
	// we are only writing to LCD so we can save an extra pin
	_usingRw = false;
	// this pin is currently grounded and not used since _usingRw is set to false
	_rwPin = -1;
	_enablePin = enablePin;

	_cursorPos = 0;

	_db[0] = d4;
	_db[1] = d5;
	_db[2] = d6;
	_db[3] = d7;

	// since this is specifically for a 4x20 lcd
	_numLines = 4;

	//row_offsets = { 0x00, 0x40, 0x14, 0x54 };
	row_offsets[0] = 0x00;
	row_offsets[1] = 0x40;
	row_offsets[2] = 0x14;
	row_offsets[3] = 0x54;

	// default mode
	//_mode = MODE_SHIFT_UP;
	_mode = MODE_SHIFT_LEFT;

	_autoFlush = true;

	initBuffer();
}

void LCD4x20::setMode(uint8_t mode) {
	if (mode == MODE_SHIFT_UP || MODE_SHIFT_LEFT) {
		_mode = mode;
	}
}

// TODO If autoFlush = false, value is only written to the buffer, then sync'd later
//void LCD4x20::setAutoFlush(bool autoFlush) {
//	_autoFlush = autoFlush;
//}

//pulse the Enable pin high (for a microsecond).
//This clocks whatever command or data is in DB4~7 into the LCD controller.
void LCD4x20::pulseEnablePin(){
  digitalWrite(_enablePin,LOW);
  delayMicroseconds(1);
  // send a pulse to enable
  digitalWrite(_enablePin,HIGH);
  delayMicroseconds(1);
  digitalWrite(_enablePin,LOW);
  delay(1);  // pause 1 ms.  TODO: what delay, if any, is necessary here?
}

// Writes bits 0-3 to d4-d7
void LCD4x20::pushNibble(int value){
  int val_nibble= value & 0x0F;  //clean the value.  (unnecessary)

  //for (int i=DB[0]; i <= DB[3]; i++) {

  // TODO turn on ports in true parallel fashion.  needs to observe db array
  // currently hardcoded to 7,8,9,10
  // TODO put all on one port to do in one operation
  // turns on port 7
  //PORTD = PORTD | B10000000;
  // turn on 8-10
  //PORTB = PORTB | B00000111;

  // turn off
  //PORTD = PORTD & B01111111;
  //PORTB = PORTB & B11111000;

  // move bit 1 to 8
  //PORTD = PORTD | (val_nibble << 7)
  // move bits 2-4 to 1-3
  //PORTB = PORTB | (val_nibble >> 1);

  // could use
  //int size = sizeof(_db);

  for (int i = 0; i < 4; i++) {
    digitalWrite(_db[i], val_nibble & 01);
    val_nibble >>= 1;
  }

  pulseEnablePin();
}

void LCD4x20::commandWriteNibble(int nibble) {
  digitalWrite(_rsPin, LOW);

  if (_usingRw) {
	  digitalWrite(_rwPin, LOW);
  }

  pushNibble(nibble);
}


void LCD4x20::commandWrite(int value) {
  digitalWrite(_rsPin, LOW);

  if (_usingRw) {
	  digitalWrite(_rwPin, LOW);
  }

  pushByte(value);
  //TODO: perhaps better to add a delay after EVERY command, here.  many need a delay, apparently.
}


//push a byte of data through the LCD's DB4~7 pins, in two steps, clocking each with the enable pin.
void LCD4x20::pushByte(int value){
  int val_lower = value & 0x0F;  // bits 1-4
  int val_upper = value >> 4; // bits 5-8
  // send bits 1-4
  pushNibble(val_upper);
  // send bits 5-8
  pushNibble(val_lower);
}

/**
 * Increments cursor to next position and calls setCursor on LCD if at end of line;
 * sets end_of_display flag when cursor is at the end of display
 *
 * LCD cursor to line mapping:
 *
 * line 1: 0-19
 * line 2: 64-83
 * line 3: 20-39
 * line 4: 84-103
 *
 */
void LCD4x20::incrementCursor() {

	if (_cursorPos == (row_offsets[0] + 19)) {
		// end of line 1. move to line 2
		_cursorPos = row_offsets[1];
		setCursor();
	} else if (_cursorPos == (row_offsets[1] + 19)) {
		// end of line 2
		_cursorPos = row_offsets[2];
		setCursor();
	} else if (_cursorPos == (row_offsets[2] + 19)) {
		// end of line 3
		_cursorPos = row_offsets[3];
		setCursor();
	} else if (_cursorPos == (row_offsets[3] + 19)) {
		// end of line 4
		// cursor will sit at end of display until next print event
		_endOfDisplay = true;
	} else {
		_cursorPos++;
	}
}

/**
 * Sends the character to the LCD at current cursor position
 */
void LCD4x20::printToDataBus(char value) {

	  //set the RS and RW pins to show we're writing data
	  digitalWrite(_rsPin, HIGH);

	  if (_usingRw) {
		  digitalWrite(_rwPin, LOW);
	  }

	  //let pushByte worry about the intricacies of Enable, nibble order.
	  pushByte(value);
}

/**
 * Prints a single char at current cursor position, stores char in buffer
 * and increments internal cursor to next position.
 *
 * Now handles the line feed character (10)!
 *
 * Callers must check _endOfDisplay after each call
 *
 */
void LCD4x20::printInternal(char value) {

	if (uint8_t(value) == 10) {
		// print spaces until the end of the current line
		int spaces = (getLine() * 20) - getBufferPos();

		for (int i = 0; i < spaces; i++) {
			// space char
			value = 32;
			_displayBuffer[getBufferPos()] = value;
			printToDataBus(value);
			incrementCursor();
		}
	} else {
		_displayBuffer[getBufferPos()] = value;
		printToDataBus(value);
		incrementCursor();
	}
}

/**
 * Print entire buffer to LCD.  Only relevant if autoFlush is false
 */
//void LCD4x20::flush() {
//	if (!_autoFlush) {
//		// set to true to print buffer
//		_autoFlush = true;
//		printDisplayBuffer();
//		// restore setting
//		_autoFlush = false;
//	}
//}

/**
 * Prints contents of buffer to display, from 0 to length.
 * Cursor is left at length position
 */
void LCD4x20::printDisplayBuffer(int length) {
  uint8_t i;

  setCursorHome();

  for (i = 0; i < length; i++){
	  printInternal(_displayBuffer[i]);
  }
}

/**
 * Prints entire buffer to display, from 0-80
 */
void LCD4x20::printDisplayBuffer() {
  uint8_t i;

  setCursorHome();

  for (i = 0; i < 80; i++){
	  printInternal(_displayBuffer[i]);
  }
}

/**
 * If cursor is at end of display, buffer will be shifted according to mode,
 * to make room
 */
void LCD4x20::handleCursorEvent(int remainingChars) {
	if (_endOfDisplay) {

		if (_mode == MODE_SHIFT_UP) {
			// shift up
			shiftUp(remainingChars);
		} else {
			// left shift
			shiftLeft(remainingChars);
		}

		_endOfDisplay = false;
	} else if (_endOfLine) {
		// lines 1-3.  move cursor to next line
		int line = getLine();
		// move cursor to beginning of next line
		setCursor(line + 1, 0);

		_endOfLine = false;
	}
}

/**
 * Clears buffer
 */
void LCD4x20::initBuffer() {
	for (int i = 0; i < 80; i++) {
		// set display char to space
		_displayBuffer[i] = 32;
	}
}

// PUBLIC METHODS

/**
 * Moves cursor to end of line.  Next print will occur on next line
 */
void LCD4x20::end() {

	if (_endOfLine || _endOfDisplay) {
		// nothing to do. already at end of line
	} else {
		int line = getLine();
		// this is not really necessary
		setCursor(line, 19);

		if (line == 4) {
			_endOfDisplay = true;
		} else {
			_endOfLine = true;
		}
	}
}

/**
 * If current line is 4, shifts up and moves cursor to 4,0;
 * otherwise simply moves cursor to next line
 *
 * @untested
 */
void LCD4x20::lineFeed() {
	int currentLine = getLine();

	if (currentLine < 4) {
		setCursor(currentLine + 1, 0);
	} else {
		shiftUpBuffer();

		// print lines 1 through 4.  we cleared line 4
		// this will set overflow true again but setCursor clears
		printDisplayBuffer();

		setCursor(4, 0);
	}
}

/**
 * Clears the current line and leaves cursor on line ?
 */
void LCD4x20::clearLine(int line) {
	int i = 0;

	if (line < 1 || line > 4) {
		line = 1;
	}

	for (i = (line - 1)*20; i < (line -1)*20 + 20; i++) {
		_displayBuffer[i] = 32;
	}

	// TODO printDisplayBuffer on line
}


/**
 * Prints a single char to display, shifting contents to the left if
 * at end of display
 */
void LCD4x20::print(char c) {
	handleCursorEvent(1);
	printInternal(c);
}

/**
 * Prints char array at current cursor position, wrapping across lines 1-4
 * If the char array exceeds the size of the display, the contents of the display
 * are shifted up or to the left, according to shift mode
 *
 * Note: The length of char array should not exceed the lcd display size;
 *
 * TODO use pointer
 * TODO don't print space char at beginning of line!!!
 */
void LCD4x20::print(char msg[]) {
  uint8_t i;

  // sizeof should work same, right?  char is a byte
  int len = strlen(msg);

  for (i = 0; i < len; i++){
	  // if overflowed and there are more chars to write, make room!
	  handleCursorEvent(len - i);
	  printInternal(msg[i]);
  }
}

/**
 * Prints the char array to display, starting at offset to
 * (offset + length)
 *
 */
void LCD4x20::print(char msg[], int offset, int length) {
  uint8_t i;

  for (i = offset; i < offset + length; i++) {
	  // TODO count should not include any LF chars
	  handleCursorEvent(length - i);
	  printInternal(msg[i]);
  }
}

void LCD4x20::println(uint8_t* msg, int offset, int length) {
	print(msg, offset, length);
	end();
}

void LCD4x20::print(uint8_t* msg, int offset, int length) {
	uint8_t i;

  for (i = offset; i < offset + length; i++) {
	  handleCursorEvent(length - i);
	  printInternal((char)msg[i]);
  }
}

/**
 * Prints string to display then moves cursor to end of line
 * so next print will occur on next line
 *
 */
void LCD4x20::println(char msg[]) {
	print(msg);
	end();
}

/**
 * Sets cursor to line and prints.
 */
void LCD4x20::print(int line, char msg[]) {
	setCursor(line, 0);
	print(msg);
}

//send the clear screen command to the LCD
void LCD4x20::clear() {

	// keep buffer in sync w/ display
	initBuffer();

	commandWrite(CMD_CLR);
	delay(1);

	setCursorHome();
}


/**
 *  4-bit init - spec p.46
 */
void LCD4x20::init () {
  pinMode(_enablePin,OUTPUT);
  pinMode(_rsPin,OUTPUT);

  if (_usingRw) {
	  pinMode(_rwPin,OUTPUT);
  }

  pinMode(_db[0],OUTPUT);
  pinMode(_db[1],OUTPUT);
  pinMode(_db[2],OUTPUT);
  pinMode(_db[3],OUTPUT);

  delay(50);

  //The first 4 nibbles and timings are not in my DEM16217 SYH datasheet, but apparently are HD44780 standard...

  // function set ??
  commandWriteNibble(0x03);
  delay(5);
  commandWriteNibble(0x03);
  delayMicroseconds(100);

  commandWriteNibble(0x03);
  delay(5);
  // needed by the LCDs controller
  //this being 2 sets up 4-bit mode.
  commandWriteNibble(0x02);
  commandWriteNibble(0x02);

  // WTF - this sets DL to 1 (8 bit mode) when num lines is 4
  int num_lines_ptn = (_numLines - 1) << 3;
  int dot_format_ptn = 0x00;      //5x7 dots.  0x04 is 5x10
  // function set (but only supports 1 or 2 lines??)
  commandWriteNibble(num_lines_ptn | dot_format_ptn);
  delayMicroseconds(60);

  //The rest of the init is not specific to 4-bit mode.
  //NOTE: we're writing full bytes now, not nibbles.

  // display control:
  // turn display on, cursor off, no blinking
  //  + CMD_DISPLAY_CURSOR_ON + CMD_DISPLAY_CURSOR_BLINKING_ON
  //commandWrite(CMD_DISPLAY + CMD_DISPLAY_ON + CMD_DISPLAY_CURSOR_ON + CMD_DISPLAY_CURSOR_BLINKING_ON);
  commandWrite(CMD_DISPLAY + CMD_DISPLAY_ON);
  delayMicroseconds(60);

  //clear display
  commandWrite(CMD_CLR);
  delay(3);

  // entry mode set: 06
  // increment automatically, display shift, entire shift off
  // TODO what does entire shift off do?
  commandWrite(CMD_ENTRY_MODE_SET + CMD_ENTRY_MODE_SET_INCREMENT);

  delay(1);//TODO: remove unnecessary delays
}

void LCD4x20::setCursorHome() {
	// set cursor to 0
	setCursor(1, 0);
}

/**
 * Sets cursor to line, position
 */
void LCD4x20::setCursor(int line_num, int x) {

	if (x > 19) {
		x = 19;
	}

	// line 1: 0-19
	// line 2: 64-83
	// line 3: 20-39
	// line 4: 84-103

	// weird gap between 39 and 64

	_cursorPos = row_offsets[line_num - 1] + x;

	setCursor();
}

/**
 * Sets cursor to _cursorPos and clears events
 */
void LCD4x20::setCursor() {

	// clear cursor events
	_endOfDisplay = false;
	_endOfLine = false;

	if (_autoFlush) {
		//first, put cursor home
		commandWrite(CMD_HOME);
		commandWrite(0x80 + _cursorPos);
	}
}

/**
 * Returns the LCD cursor position based on logical 0-79 position, so 21 (line 2 pos 2) returns 66
 * not used
 */
int LCD4x20::getCursorPos(int pos) {
  return row_offsets[(pos / 20)] + pos % 20;
}

/**
 * Returns current line of cursor
 */
int LCD4x20::getLine() {
	return getBufferPos() / 20 + 1;
}
/**
 * Gets buffer array index based on cursor position
 */
int LCD4x20::getBufferPos() {

	if (_cursorPos >= row_offsets[0] && _cursorPos < row_offsets[2]) {
		// line 1: cursor pos: 0-19; buffer pos: 0-19
		return _cursorPos;
	} else if (_cursorPos >= row_offsets[1] && _cursorPos < row_offsets[3]) {
		// line 2: cursor pos: 64-83; buffer pos: 20-39
		return 20 + _cursorPos - row_offsets[1];
	} else if (_cursorPos >= row_offsets[2] && _cursorPos < row_offsets[1]) {
		// line 3: cursor pos: 20-39; buffer pos: 40-59
		return 20*2 + _cursorPos - row_offsets[2];
	} else if (_cursorPos >= row_offsets[3]) {
		// line 4: cursor pos: 84-103; buffer pos: 60-79
		return 20*3 + _cursorPos - row_offsets[3];
	}

	return -1;
}

/**
 * Shifts the contents of the display buffer by x
 * x must be between 1 and 79, inclusive
 */
void LCD4x20::shiftLeft(int x) {
	if (x <= 0 || x >= 80) {
		return;
	}

	for (int i = 0; i < (80 - x); i++) {
		_displayBuffer[i] = _displayBuffer[i + x];
	}

	// now print shifted contents up to remaining characters
	printDisplayBuffer(80 - x);
}

/**
 * Copies lines 2-4 to 1-3 and clears line 4
 */
void LCD4x20::shiftUpBuffer() {
	// shift buffer up
	int i = 20;

	// copy line 2 to 1
	for (i = 20; i < 40; i++) {
		_displayBuffer[i - 20] = _displayBuffer[i];
	}

	// copy line 3 to 2
	for (i = 40; i < 60; i++) {
		_displayBuffer[i - 20] = _displayBuffer[i];
	}

	// copy line 4 to 3
	for (i = 60; i < 80; i++) {
		_displayBuffer[i - 20] = _displayBuffer[i];
	}

	// clear line 4 of buffer
	for (i = 60; i < 80; i++) {
		_displayBuffer[i] = 32;
	}
}

/**
 * Shifts buffer up by enough lines to make room for remaining characters
 * Should only be called when at endOfDisplay
 */
void LCD4x20::shiftUp(int remainingChars) {

	int lines = (remainingChars - 1) / 20 + 1;

	for (int i = 0; i < lines; i++) {
		shiftUpBuffer();
	}

	// print lines 1 through 4.  we cleared line 4
	// this will set overflow true again but setCursor clears
	printDisplayBuffer();

	// TODO modify to support shiftup when cursor not on line4
	// move to next line so we are ready to print the next line
	setCursor(4 - lines + 1, 0);
}


//
///**
// * When the busy flag is 1, the HD44780U is in the internal operation mode, and the next instruction will not
//be accepted. When RS = 0 and R/W = 1 (Table 1), the busy flag is output to DB7. The next instruction
//must be written after ensuring that the busy flag is 0.
//
//requires R/W pin enabled
// */
//void whileBusyFlag() {
//
//	if (!_usingRw) {
//		return;
//	}
//
//	// cmd mode
//	digitalWrite(_rsPin, LOW);
//	// turn on read
//	digitalWrite(_rwPin, HIGH);
//
//	// set d7 as input
//	pinMode(_db[3], INPUT);
//
//	while (digitalRead(_db[3]) == HIGH) {
//		// address counter 7-bit can be read now 3 bits then next cycle remaining 4 bits
////		pulseEnablePin(); //??
//
//	}
//
//	// return to output
//	pinMode(_db[3], OUTPUT);
//}
//
//// return true if cursor at end of line
//bool LCD4x20::endOfLine() {
//	return (_cursorPos == 19 || _cursorPos == 83 || _cursorPos == 39 || _cursorPos == 103);
//}

