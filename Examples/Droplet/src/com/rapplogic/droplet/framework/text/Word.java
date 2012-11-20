package com.rapplogic.droplet.framework.text;

import org.apache.log4j.Logger;

/**
 * A word is a contiguous sequence of non white/LF characters or a single whitespace or single LF
 * 
 * @author andrew
 *
 */
public class Word {

	private final static Logger log = Logger.getLogger(Word.class);
	
	public String word;
	
	public Word(String word) {
		this.word = word;
	}
	
	public boolean isSpace() {
		return ContentFormatter.SPACE.equals(word);
	}
	
	public boolean isCr() {
		return ContentFormatter.LF.equals(word);
	}
	
	public int length() {
		return word.length();
	}
	
	public String toString() {
		return word;
	}
}
