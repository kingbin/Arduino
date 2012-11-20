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

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.rapplogic.droplet.framework.LcdProperties;

// TODO don't use hyphen for break last character on line is a hyphen!
// TODO consider handling html entities: &#8217; they are huge waste of lcd realestate

/**
 * Contains logic for structuring a blob of text into one or
 * more Pages for display on a LCD.
 */
public class ContentFormatter {

	private final static Logger log = Logger.getLogger(ContentFormatter.class);
	
	public final static ContentFormatter instance = new ContentFormatter();

	public final static char LF_CHAR = (char)10;
	public final static char SPACE_CHAR = (char)32;
	
	public final static String CR = String.valueOf((char)13);
	public final static String LF = String.valueOf(LF_CHAR);
	public final static String SPACE = String.valueOf(SPACE_CHAR);
	
	// number of chars that should be reserved for the continuation marker on line 4 (1 is good)
	private int continuationCharSize = 1;
	
	private LcdProperties lcdProperties = new LcdProperties();
	
	public LcdProperties getLcdProperties() {
		return lcdProperties;
	}

	public void setLcdProperties(LcdProperties lcdProperties) {
		this.lcdProperties = lcdProperties;
	}

	// default pagination rules
	PaginationRules pr = new PaginationRules();
	
	public PaginationRules getPaginationRules() {
		return pr;
	}

	public void setPaginationRules(PaginationRules paginationRules) {
		this.pr = paginationRules;
	}

	public static ContentFormatter getInstance() {
		return instance;
	}	
	
	public int getContinuationCharSize() {
		return continuationCharSize;
	}

	public void setContinuationCharSize(int continuationCharSize) {
		this.continuationCharSize = continuationCharSize;
	}

	public int getEffectivePageSize() {
		return lcdProperties.getLineSize() * lcdProperties.getNumLines() - this.continuationCharSize;
	}
	
	/**
	 * Return only the first page
	 * @param text
	 * @return
	 */
	public Page getFirstPage(String text) {
		return this.format(text).getFirstPage();
	}
	
	/**
	 * Parses a arbitrary, unbounded string of text, into a sequence of Pages, represented
	 * by an instance of IContent. 
	 * <p/>
	 * This method preserves line feed (LF) characters but strips all carriage returns and
	 * Multibyte (unicode) characters.
	 * <p/>
	 * See PaginationRules for info on how to affect the formatting of content. 
	 * 
	 * @param text
	 * @param maxSize
	 * @return
	 */
	public IContent format(String text) {
		
		WordList wordList = this.chop(text);
		
//		for (Word word : wordList.getList()) {
//			log.debug("[" + word.getString() + "]");
//		}
		
		// TODO remove leading whitespace
		
		Content content = new Content();
	    
	    Page currentPage = new Page(lcdProperties);
	    content.getPages().add(currentPage);

	    Line currentLine = new Line(lcdProperties.getLineSize());
	    
	    Word word = null;
	    
	    while (wordList.hasNext()) {

	    	if (currentPage == null) {
	    		log.debug("creating new page");
	    		
    			currentPage = new Page(lcdProperties);
    			content.getPages().add(currentPage);
	    	}
	    	
	    	if (currentLine == null) {
	    		log.debug("creating new line");
	    		
	    		if (currentPage.isLineBeforeLastLine()) {
	    			// allow one char for continuation marker
	    			currentLine = new Line(lcdProperties.getLineSize() - this.getContinuationCharSize());
	    		} else {
	    			currentLine = new Line(lcdProperties.getLineSize());
	    		}
	    	}

	    	if (word == null) {
	    		word = wordList.first();
	    	} else {
	    		word = wordList.next();	
	    	}
	    	 
	    	log.debug("word is [" + word.toString() + "], line number is " + currentPage.getCurrentLine());
	    	
    		if (pr.isDontStartLineWithWhiteSpace() && currentLine.getPosition() == 0 && word.isSpace()) {
				// TODO catch index out of bounds
				// advance word to next non-whitespace
    			
    			log.debug("whitespace found at beginning of line");
    			
    			if (wordList.isLastWord()) {
    				//nothing but spaces on this line.  discard this line
    				log.debug("nothing but spaces on this line.  discarding this line");
    				// TODO
    			} else {
    				while (wordList.hasNext()) {
        				word = wordList.next();
        				
        				log.debug("trimming whitespace from beginning of line");
        				
        				if (!word.isSpace()) {
        					break;
        				}    					
    				}
    				        			
        			if (word.isSpace()) {
        				// TODO nothing but spaces on this line.  discard this line
        			}
    			}
    		}
    		
    		if (word.length() <= currentLine.getRemainingCharacters()) {
				// it fits, great
    			
    			// first check if this is a superfluous space
    			if (pr.isRemoveContiguousSpaces() && word.isSpace() && currentLine.getPosition() > 0 && currentLine.getCurrentWord().isSpace()) {
    				// don't add additional space char
    				log.debug("removing contiguous space from line at position " + currentLine.getPosition());
    			} else {
        			log.debug("word fits on line: [" + word.toString() + "]");
    				currentLine.addWord(word);    				
    			}
    			

			} else {
				//it doesn't fit, must break word or more to next line
				
				log.debug("word does not fit: [" + word.toString() + "], remaining chars " + currentLine.getRemainingCharacters() + ", current page is " + currentPage.getCurrentLine());
				
				boolean breakWord = true;
				
				if (word.length() < pr.getDontBreakWordsSmallerThan()) {
					// close out line
					breakWord = false;
				} else if (currentLine.getRemainingCharacters() < pr.getDontLeaveThisManyCharsOfAWordAtTheEndOfTheLine()) {
					// hyphen counts as a char
					breakWord = false;
				} else if (currentLine.getRemainingCharacters() == 1 && pr.isHyphenateWordBreaks()) {
					// not enough room to hyphenate.. need 2 chars or more
					breakWord = false;
				} else if (currentPage.isLineBeforeLastLine() && pr.isDontBreakWordsAcrossPages()) {
					log.debug("last line and break words is true");
					breakWord = false;
				} else if (currentLine.getRemainingCharacters() < pr.getDontLeaveThisManyCharsOfAWordAtTheEndOfTheLine()) {
					breakWord = false;
				}
				
				// word is larger than display.. must break
				// TODO linesize depends on line number
				if (word.length() > lcdProperties.getLineSize()) {
					breakWord = true;
				}
				
				if (breakWord) {
					log.debug("breaking word: " + word.toString());
					
					// break word

					int remainingChars = 0;
					
					if (pr.isHyphenateWordBreaks()) {
						remainingChars = currentLine.getRemainingCharacters() - 1;
					} else {
						remainingChars = currentLine.getRemainingCharacters();
					}
					
					Word partial = new Word(word.toString().substring(0, remainingChars));
					
					log.debug("partial word is " + partial.toString());		
					
					currentLine.addWord(partial);
					
					if (pr.isHyphenateWordBreaks()) {
						currentLine.addWord(new Word("-"));
					}
					
					// put word remainder into list
					Word remainder = new Word(word.toString().substring(remainingChars));
					
					log.debug("returning remainer of word to list: " + remainder.toString());					
					
					wordList.put(remainder);							
				} else {
					// put word back in list
					log.debug("returning word to list: " + word.toString());
					wordList.put(word);
				}
				
				if (!currentLine.isClosed()) {
					currentLine.close();	
				}
			}
    		
    		if (currentLine.isClosed()) {
    	    	// don't add empty line to first line of page
    	    	if (pr.isDontStartPageWithEmptyLine() && currentPage.isEmpty() && currentLine.isEffectivelyEmpty()) {
    	    		// don't start page with CR or whitespace
    	    		log.debug("ignoring eff. empty line");
    	    	} else {
        			log.debug("adding line to page: " + currentLine.toString());
    	    		currentPage.addLine(currentLine.toString());
    	    	}
    			
    			currentLine = null;
    			
        		if (currentPage.isLastLine()) {
        			// page is full.. set null
        			currentPage = null;
        		}    			
    		}
	    }
	    
	    // after loop
	    
	    if (currentLine != null && currentLine.toString().length() > 0) {
		    
	    	if (currentPage == null) {
	    		log.debug("creating new page");
    			currentPage = new Page(lcdProperties);
    			content.getPages().add(currentPage);
	    	}
	    	
	    	// don't add empty line to first line of page
	    	if (!(pr.isDontStartPageWithEmptyLine() && currentPage.isEmpty() && currentLine.isEffectivelyEmpty())) {
		    	// add last line
			    currentPage.addLine(currentLine.toString());	    		
	    	}
	    }
    	
    	for (Page page : content.getPages()) {
    		log.debug("\n" + page.toStringAsLcdFormat());
    	}
	    
	    return content;
	}

	private static String readFileAsString(String path) throws IOException {
		FileReader reader = new FileReader(path);	
		StringWriter sw = new StringWriter();
		
		char[] buf = new char[1024];
		int len = 0;
		
		while ((len = reader.read(buf)) > -1) {
			sw.write(buf, 0, len);
		}
		
		return sw.toString();
	}
	
	public static void main(String[] args) throws IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		ContentFormatter p = new ContentFormatter();

		p.setContinuationCharSize(1);
		
//		IContent c = p.format("In court documents filed this morning before U.S. Bankruptcy Court in New York, Chrysler asked Judge Arthur\n Gonzalez for approval to reject dealership agreements it has with 789 dealers across the country. Local Chrysler dealers on the chopping block include Laurel Dodge, Montrose Motors in Germantown, Reed Brothers Dodge in Rockville, Westside Dodge in Potomac, Darcars of Fairfax, Dulles Motor Cars in Leesburg and Gunning Motors in Manassas. Chrysler, which has received $4 billion in federal loans, has been operating in bankruptcy protection since April 30. The automaker is rushing to pare down its business and cut costs");
//		IContent c = p.format("                                                   f        ");
//		IContent c = p.format("  The\n     quick\nbrown fox jumped over the\n \nlazy\ndog. this is another test bignonbreakingwordasdfasdfasdfasdff asdf");
		
//		String test = readFileAsString("/Users/andrew/Documents/dev/physical-computing/droplet/pagination-test-content");	
		String test = readFileAsString("/Users/andrew/Documents/droplet-test");
		IContent c = p.format(test);
		
		log.debug("content is " + c);
		
		for (Page page : c.getPages()) {
			log.debug("Page text is:\n" + page.toStringAsLcdFormat());
		}

	          //print each word in order
//	          BreakIterator boundary = BreakIterator.getWordInstance();
//	          boundary.setText(testStr);
//	          printEachForward(boundary, testStr);
	      
//		Pagination.breakText();
	}
	
	public static WordList chop(String text) {
		text = removeMultiByteCharacters(text);
		
		text = cleanseInput(text);
		
		// getClass() reports java.text.RuleBasedBreakIterator, not in javadoc
        BreakIterator breakIterator = BreakIterator.getWordInstance();
        
        // we don't want to break on colons, which breakIterator does by default so  we're going to trick
        // it by replacing colons with a non-breaking character. we don't need to replace it back since we refer
        // to the unmodified text string
        final String replaceStr = "a";
        breakIterator.setText(text.replaceAll(":", replaceStr));
    	
	    log.debug("input text is " + text);
	  
	    // start of current word boundary
	    int start = breakIterator.first();
	    // end of current word boundary	    
	    int end = breakIterator.next();
	    
	    List<Word> wordList = new ArrayList<Word>();
	    
	    do {	    	
	    	String word = text.substring(start, end);
	    	
	    	if ((word.indexOf(' ') > -1 || word.indexOf('\n') > -1) && word.length() > 1) {
	    		for (int i = 0; i < word.length(); i++) {
	    			wordList.add(new Word(new String(new char[] {word.charAt(i)})));
	    		}
	    	} else {
	    		wordList.add(new Word(word));
	    	}
	    	
	    	start = end;
	    	end = breakIterator.next();
	    } while (end != BreakIterator.DONE);
		
	    if (wordList.size() == 0) {
	    	throw new RuntimeException("no content!");
	    }
	    
		// remove trailing whitespace/LF chars
		while (wordList.get(wordList.size() - 1).isCr() || wordList.get(wordList.size() - 1).isSpace()) {
			// discard cr/space
			wordList.remove(wordList.size() - 1);
		}

	    if (wordList.size() == 0) {
	    	throw new RuntimeException("no content!");
	    }
	    
//	    for (Word word : wordList) {
//	    	log.debug(word.toString());
//	    }
	    
	    return new WordList(wordList);
	}
	
	/**
	 * Removes characters that are not supported on a standard LCD
	 * For example: multibyte characters
	 *  
	 * @param text
	 * @return
	 */
	public static String cleanseInput(String text) {		
		text = removeCarriageReturns(text);		
		return removeMultiByteCharacters(text);
	}
	
	public static String removeCarriageReturns(String text) {
		// remove CRs
		return text.replaceAll(CR, "");		
	}
	
	public static String removeLineFeeds(String text) {
		return text.replaceAll(LF, "");
	}
	
	/**
	 * LCDs generally only support ASCII and a limited number of extended characters
	 * More importantly however, the XBee can only single byte characters.
	 * This method will cleanse the string of multibyte characters, such as fancy quotes
	 * that would otherwise cause an error during send.
	 * Multibyte chars are replace with a ?
	 * <p/>
	 * This does not imply that the LCD can supports chars between 128 and 255, and
	 * it probably can't.  A more sophisticated approach would be to find the ASCII
	 * equivalent, if it exists.  
	 * <p/>
	 * This method will make some reasonable replacements of unicode characters,
	 * such as fancy quotes, dashes etc, but is not nearly comprehensive.
	 * 
	 * TODO first detect if text contains multibyte before creating StringBuilder
	 * 
	 * @param s
	 * @return
	 */
	public static String removeMultiByteCharacters(String s) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) > 255) {
				if (s.charAt(i) == 8220 || s.charAt(i) == 8221) {
					// it's a fancy quote open/close
					sb.append("\"");
				} else if (s.charAt(i) == 8216 || s.charAt(i) == 8217) {
					// fancy open/close single quote
					sb.append("'");
				} else if (s.charAt(i) == 8208 || s.charAt(i) == 8209 || s.charAt(i) == 8259) {
					// fancy hyphen
					sb.append("-");
				} else if (s.charAt(i) == 8211 || s.charAt(i) == 8212) {
					// fancy dash
					sb.append("-");
				} else if (s.charAt(i) == 8194 || s.charAt(i) == 8195) {
					// fancy space
					sb.append(" ");
				} else if (s.charAt(i) == 8230) {
					// ellipsis
					sb.append("...");
				} else {
					log.warn("Removing multibyte character : (int)" + (int)s.charAt(i) + ", (char): " + s.charAt(i));
					sb.append("?");					
				}

			} else {
				sb.append(s.charAt(i));
			}
		}
		
		return sb.toString();
	}
}
