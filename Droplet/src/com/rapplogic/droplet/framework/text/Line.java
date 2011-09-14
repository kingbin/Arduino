package com.rapplogic.droplet.framework.text;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Represents a line of the LCD
 * A line may contain characters, whitespace and the LF char, up to the lineSize length
 * A line may have any characters follow a LF char
 * 
 * @author andrew
 *
 */
public class Line {

	private final static Logger log = Logger.getLogger(Line.class);
	
	private List<Word> line = new ArrayList<Word>();
	
	public int position;
	public int length;
	public boolean closed;
	
//	private StringBuilder line = new StringBuilder();
	
	public Line(int length) {
		this.length = length;
	}
	
	public int getRemainingCharacters() {
		return this.length - position;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void addWord(Word word) {
		if (this.isClosed()) {
			throw new RuntimeException("line is closed");
		}
		
		
		line.add(word);
//		line.append(word.toString());
		
		position+= word.toString().length();
		
		if (word.isCr()) {
			this.close();
		} else if (this.getRemainingCharacters() == 0) {
			this.close();
		}
	}
	
	public Word getCurrentWord() {
		if (line.size() > 0) {
			return line.get(line.size() - 1);
		}
		
		throw new RuntimeException("line is empty");
	}
	
	public void close() {
		log.debug("line close: " + line.toString());
		
		if (!closed) {
			closed = true;
		} else {
			throw new RuntimeException("already closed");
		}
	}
	
	public boolean isEffectivelyEmpty() {
		
		boolean foundNonWhitespace = false;
		
//		log.debug("isEffectivelyEmpty(): line is " + line.toString());
		
//		for (int i = 0; i < line.toString().length(); i++) {
//			if (line.charAt(i) != ' ' && line.charAt(i) != '\n') {
//				foundNonWhitespace = true;
//				break;
//			}
//		}

		for (Word word : line) {
			if (!word.isCr() && !word.isSpace()) {
				foundNonWhitespace = true;
				break;
			}
		}
		
		return !foundNonWhitespace;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(position);
		
		for (Word word : line) {
			sb.append(word.toString());
		}		
		
		return sb.toString();
	}
}
