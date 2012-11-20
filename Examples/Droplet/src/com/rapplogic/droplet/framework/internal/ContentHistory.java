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

package com.rapplogic.droplet.framework.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.text.IContent;

public class ContentHistory {
	
	private final static Logger log = Logger.getLogger(ContentHistory.class);
	
	private List<IContent> contentList = new ArrayList<IContent>();
	private int contentIndex;
	private int maxSize = 100;
	
	public IContent getCurrent() {
		return this.contentList.get(this.contentIndex);
	}
	
	public boolean isEmpty() {
		return contentList.isEmpty();
	}
	
	public void store(IContent content) {
//		log.debug("storing content " + content + ", by address " + address);
		
		while (contentList.size() >= maxSize) {
			// remove old
			log.debug("history is full.. removing content to make room");
			contentList.remove(0);
		}
		
		contentList.add(content);
		
		this.contentIndex = contentList.size() - 1;
	}
	
	/**
	 * True if there is a next page in current content or a next content
	 * 
	 * @return
	 */
	public boolean nextPageExists() {
		return !this.isEmpty() && (!this.getCurrent().isLastPage() || this.contentIndex < (contentList.size() - 1));
	}

	/**
	 * True if there is a previous page in current content or a previous content
	 * 
	 * @return
	 */
	public boolean previousPageExists() {
		return !this.isEmpty() && (!this.getCurrent().isFirstPage() || this.contentIndex > 0);
	}
	
	/**
	 * Returns the previous page or null at beginning
	 * 
	 * @return
	 * @throws PageNotFoundException 
	 */
	public void setIndexToPreviousPage() throws PageNotFoundException {
		IContent content = contentList.get(this.contentIndex);
		
		if (content.isFirstPage()) {
			if (this.contentIndex > 0) {
				// index to previous content and first page
				this.contentIndex--;
				this.getCurrent().setFirstPage();
//				return this.getCurrent().getCurrentPage();
			} else {
				// at first content/first page
				throw new PageNotFoundException("Already at first page");
			}
			
		} else {
			content.setIndexToPreviousPage();
		}		
	}
	
	/**
	 * Returns the next page or null if at end
	 * @throws PageNotFoundException 
	 */
	public void setIndexToNextPage() throws PageNotFoundException {
		
		IContent content = contentList.get(this.contentIndex);
		
		if (content.isLastPage()) {
			if (this.contentIndex < (contentList.size() - 1)) {
				// index to next content and return first page
				this.contentIndex++;
				this.getCurrent().setFirstPage();
			} else {
				// last page of last content
				throw new PageNotFoundException("Already at last page!");
			}
		} else {
			content.setIndexToNextPage();
		}
	}
	
	public static class PageNotFoundException extends Exception {
		PageNotFoundException(String s) {
			super(s);
		}
	}
}
