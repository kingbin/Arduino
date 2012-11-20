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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.LcdProperties;

/**
 * Represents content to be displayed on a remote LCD.  
 * The character maximum is specified by lineSize*numLines.
 *  
 * @author andrew
 *
 */
public class Page {
	
	private final static Logger log = Logger.getLogger(Page.class);
	
	private List<String> lines = new ArrayList<String>();
	
	private LcdProperties props;
	
	// has this been sent to the LCD yet?  I'm aiming for the the past tense of read
	private boolean read;
	
	public Page(LcdProperties props) {
		this.props = props;
	}
	
	public boolean isEmpty() {
		return this.lines.size() == 0;
	}
	
	public boolean isLineBeforeLastLine() {
		return this.lines.size() == (props.getNumLines() - 1);
	}
	
	public boolean isLastLine() {
		return this.lines.size() == props.getNumLines();
	}
	
	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
	
	public int getCurrentLine() {
		return lines.size();
	}
	
	/**
	 * Adds a line of text to the page.
	 * The text must not contain line feeds and be between 0
	 * and lineSize in size, inclusive.
	 * A null line will be treated as a line break
	 * 
	 * @param s
	 */
	public void addLine(String s) {
		// TODO can't contain more than one LF

		// line must either be 20 chars or end with a CR
		// can't contain more than 1 CR
		// can't have any chars follow a CR
		
		// safety net checks..
		
		if (s == null) {
			throw new IllegalArgumentException("Line cannot be null");
		}
		
		if (s.length() > props.getLineSize()) {
			throw new IllegalArgumentException("Line exceeds " + props.getLineSize() + " chars: " + s);
		}
		
		if (this.isLastLine()) {
			throw new IllegalArgumentException("Page is already full");
		}
		
		lines.add(s);
	}
	
	public static int countCharInString(String s, char ch) {
		int count = 0;
		
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == ch) {
				count++;
			}
		}
		
		return count;
	}
	
	public int getLineCount() {
		return lines.size();
	}
	
	/**
	 * Returns a string suitable for display on a LCD
	 * To maintain formatting on LCD the line must either end with a LF or be exactly "lineSize" chars in length
	 * 
	 * @return
	 */
	public String getLcdFormattedText() {
		StringBuilder sb = new StringBuilder();
		
		for (String line : lines) {
//			log.debug("appending line [" + line + "]");
			
			sb.append(line);
			
			if (!line.endsWith(ContentFormatter.LF) && line.length() < props.getLineSize()) {
				// line must be props.getLineSize() chars in length or end in LF
//				log.debug("appending LF");
				sb.append(ContentFormatter.LF);
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns a visual depiction of content to be displayed on the LCD.
	 * <p/>
	 * For example:
	 * 
	 <pre>
	  		**********************
	  		*[slashdot] Company  *
	  		*Claims Potential Ma-*
	  		*gnification In Bio  *
	  		*Fuel Production     *
	  		**********************
	 </pre>
	 * @return
	 */
	public String toStringAsLcdFormat() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		
		for (String line : lines) {
			
			line = line.replaceAll(ContentFormatter.LF, "");
			
			if (i > 0) {
				sb.append(System.getProperty("line.separator"));
			}
			
			if (i == 0) {
				for (int j = 0; j < props.getLineSize() + 2; j++) {
					sb.append("*");
				}
				sb.append(System.getProperty("line.separator"));
			}
			
			sb.append("*" + line);
			
			for (int j = line.length(); j < props.getLineSize(); j++) {
				sb.append(" ");
			}
			
			sb.append("*");
		
			i++;
		}
		
		sb.append(System.getProperty("line.separator"));
		
		//TODO add lines if < 4
		
		while (i < props.getNumLines()) {
			sb.append("*                    *");
			i++;
			sb.append(System.getProperty("line.separator"));
		}
		
		for (int j = 0; j < props.getLineSize() + 2; j++) {
			sb.append("*");
		}
		
		return sb.toString();		
	}
	
	/**
	 * Condensed representation of LCD text.  Pipe chars
	 * represent line breaks
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (String line : lines) {
			sb.append(line + "|");
		}
		
		return sb.toString();
	}
}
