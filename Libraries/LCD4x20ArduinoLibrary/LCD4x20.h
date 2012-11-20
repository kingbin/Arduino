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

#ifndef LCD4x20_h
#define LCD4x20_h

#include <inttypes.h>

#define MODE_SHIFT_UP 1
#define MODE_SHIFT_LEFT 2
// overflows to line 1
//#define MODE_SHIFT_OVERFLOW 3

// TODO Java class that sends each keystroke to LCD immediately
// TODO reorder public/private methods
// TODO observe busy flag for better performance.  This requires the R/W pin.
// TODO try enabling R/W to read the contents of LCD instead of buffer

/**
 * An Arduino library for interfacing with a 4x20 HD44780 LCD in 4-bit mode (Adapted
 * from LCD4Bit http://www.arduino.cc/playground/Code/LCD4BitLibrary)
 *
 * This library maintains a 80 char buffer so that it can shift contents up when the LCD is full.
 * Supports shifting content up (line feed) or left, depending on mode, once the display is full.
 * Ideally the LCD would handle this but unfortunately with this particular LCD,
 * and maybe all 4x20 HD44780 LCDs, line 1 continues to line 3, line 3 to line 2 and line 2 to line 4.
 *
 * Andrew Rapp
 *
 */
class LCD4x20 {
public:
	/**
	 * Creates a instance of LCD4x20 and Specifies the how the Arduino pins that are connected to the LCD
	 */
	LCD4x20 (int rsPin, int enablePin, int d4, int d5, int d6, int d7);
	/**
	 * Initializes LCD
	 */
	void init();
	void setMode(uint8_t mode);
	void print(int line, char value[]);
	void print(char value);
	void print(char value[]);
	void print(char msg[], int offset, int length);
	/**
	 * Same as println(char msg[]), but doesn't move the cursor to the end of the line
	 */
	void print(uint8_t* msg, int offset, int length);
	/**
	 * Same as println(char msg[]), but allows a subset of the string to be printed, from offset to offset + length
	 */
	void println(uint8_t* msg, int offset, int length);
	/**
	 * Prints the string and moves the cursor to the end of display so the next print will occur on the next line
	 */
	void println(char msg[]);
	// TODO void print(int val);
	void clear();
	void setCursor(int line_num, int x);
	void end();
	int getLine();
	void lineFeed();
	void clearLine(int line);
	// Writes contents of display buffer to LCD
	//void flush();
	//void setAutoFlush(bool autoFlush);
	void commandWriteNibble(int nibble);
	void commandWrite(int value);

	// is it possible to turn off backlight with command?
	//  void displayOff();
	//  void displayOn();
  // keyboard operations
//  void tab();
//  void home();
//  void del();
//  void backSpace();
//  void upArrow();
//  void downArrow();
//  void leftArrow();
//  void rightArrow();
//  void ins();
private:
	void handleCursorEvent(int remainingChars);
	void shiftLeft(int x);
	void shiftUpBuffer();
	void shiftUp(int remainingChars);
	void setCursor();
	void setCursorHome();
	void incrementCursor();
	int getCursorPos(int pos);
	int getBufferPos();
	void initBuffer();
	void printToDataBus(char value);
	void printInternal(char value);
	void printDisplayBuffer(int length);
	void printDisplayBuffer();
	void pulseEnablePin();
	void pushNibble(int nibble);
	void pushByte(int value);
	// position of cursor 0-79
	uint8_t _cursorPos;
	int _rsPin;
	int _enablePin;
	int _numLines;
	bool _usingRw;
	int _rwPin;
	int _db[4];
	// state of lcd. wasteful I know
	char _displayBuffer[80];
	int row_offsets[4];
	// indicates cursor is at end of the display and a shift up/left must occur before next print command
	bool _endOfDisplay;
	// indicates the cursor should be moved to next line prior to printing again
	bool _endOfLine;
	// either shift up or left
	uint8_t _mode;
	// if true, print* is immediately sent to LCD; if false, user must call flush() to sync contents of buffer w/ display
	bool _autoFlush;
};

#endif
