package com.rapplogic.droplet.framework.text;

import java.util.List;

import org.apache.log4j.Logger;

/**
 * Provides access to <code>List</code> of <code>Word</code>s
 * 
 * @author andrew
 *
 */
public class WordList {

	private final static Logger log = Logger.getLogger(WordList.class);
	
	private List<Word> wordList;
	private int position;
	
	public WordList(List<Word> wordList) {
		this.wordList = wordList;
	}
	
	public boolean isLastWord() {
		return position == wordList.size() - 1;
	}
	
	public boolean isFirstWord() {
		return position == 0;
	}
	
	public Word first() {
		return wordList.get(0);
	}
	
	/**
	 * Inserts word into the list
	 *  
	 * @param word
	 */
	public void put(Word word) {
		wordList.remove(position);
		wordList.add(position, word);
		// list has grown by one, so increment position
		position--;
	}
	
	public boolean hasNext() {
		return !this.isLastWord();
	}
	
	public Word next() {
		
		if (!this.isLastWord()) {
			Word word = wordList.get(++position);
			
			return word;
		}
		
		throw new RuntimeException("at end of list");
	}
	
	public List<Word> getList() {
		return wordList;
	}
}
