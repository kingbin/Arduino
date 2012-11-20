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

package com.rapplogic.droplet.framework.text;

/**
 * A collection of rules for describing how a blob of text should be
 * formated for display on a LCD
 * 
 * @author andrew
 *
 */
public class PaginationRules {
	private boolean dontStartLineWithSpaceChar = true;
	private boolean dontEndNewLineWithSpaceChar = true;
	private boolean dontBreakWordsAcrossPages = true;
	private boolean dontStartPageWithEmptyLine = true;
	private boolean hyphenateWordBreaks = true;
	// TODO remove occurrences of two or more adjacent space chars, replacing with just one
	private boolean removeContiguousSpaces = true;
	
	private int dontBreakWordsSmallerThan = 6;
	private int dontLeaveThisManyCharsOfAWordAtTheEndOfTheLine = 3;
	
//	private int dontLeaveThisManyCharsOfAWordAtBeginningOfNextLine = 1;

	public boolean isDontStartLineWithWhiteSpace() {
		return dontStartLineWithSpaceChar;
	}
	public void setDontStartLineWithWhiteSpace(boolean dontStartNewLineWithWhiteSpace) {
		this.dontStartLineWithSpaceChar = dontStartNewLineWithWhiteSpace;
	}
	public boolean isDontEndNewLineWithWhiteSpace() {
		return dontEndNewLineWithSpaceChar;
	}
	public void setDontEndNewLineWithWhiteSpace(boolean dontEndNewLineWithWhiteSpace) {
		this.dontEndNewLineWithSpaceChar = dontEndNewLineWithWhiteSpace;
	}

	public boolean isDontBreakWordsAcrossPages() {
		return dontBreakWordsAcrossPages;
	}
	public void setDontBreakWordsAcrossPages(boolean dontBreakWordsAcrossPages) {
		this.dontBreakWordsAcrossPages = dontBreakWordsAcrossPages;
	}
	public boolean isHyphenateWordBreaks() {
		return hyphenateWordBreaks;
	}
	public void setHyphenateWordBreaks(boolean hyphenateWordBreaks) {
		this.hyphenateWordBreaks = hyphenateWordBreaks;
	}
	public boolean isDontStartPageWithEmptyLine() {
		return dontStartPageWithEmptyLine;
	}
	public void setDontStartPageWithEmptyLine(boolean dontStartPageWithEmptyLine) {
		this.dontStartPageWithEmptyLine = dontStartPageWithEmptyLine;
	}
	/**
	 * Words smaller than this many chars will be moved to the next line if they don't
	 * fit on the current line of LCD
	 * 
	 * @return
	 */
	public int getDontBreakWordsSmallerThan() {
		return dontBreakWordsSmallerThan;
	}
	public void setDontBreakWordsSmallerThan(int dontBreakWordsSmallerThan) {
		this.dontBreakWordsSmallerThan = dontBreakWordsSmallerThan;
	}
	public int getDontLeaveThisManyCharsOfAWordAtTheEndOfTheLine() {
		return dontLeaveThisManyCharsOfAWordAtTheEndOfTheLine;
	}
	public void setDontLeaveThisManyCharsOfAWordAtTheEndOfTheLine(int dontLeaveThisManyCharsOfAWordAtTheEndOfTheLine) {
		this.dontLeaveThisManyCharsOfAWordAtTheEndOfTheLine = dontLeaveThisManyCharsOfAWordAtTheEndOfTheLine;
	}
	public boolean isRemoveContiguousSpaces() {
		return removeContiguousSpaces;
	}
	public void setRemoveContiguousSpaces(boolean removeContiguousSpaces) {
		this.removeContiguousSpaces = removeContiguousSpaces;
	}
}
