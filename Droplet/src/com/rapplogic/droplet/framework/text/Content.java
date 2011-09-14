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


/**
 * Represents one or more pages of related content.
 * Keeps track of the current page and provides methods
 * for navigating back and forth.
 * 
 * @author andrew
 *
 */
public class Content implements IContent {
	
	private final static Logger log = Logger.getLogger(Content.class);
	
	private final List<Page> pages = new ArrayList<Page>();
	
	private int currentPageNumberIndex;
	private boolean errorMessage;

	public boolean isErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(boolean errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<Page> getPages() {
		return pages;
	}

	public void setLastPage() {
		this.currentPageNumberIndex = pages.size() - 1;
	}
	
	public void setFirstPage() {
		this.currentPageNumberIndex = 0;
	}
	
	public Page getFirstPage() {
		return pages.get(0);
	}
	
	public Page getCurrentPage() {
		return pages.get(currentPageNumberIndex);
	}
	
	public boolean isFirstPage() {
		return this.currentPageNumberIndex == 0;
	}
	
	public boolean isLastPage() {
		return this.currentPageNumberIndex >= (pages.size() - 1);
	}
	
	public boolean isNextPage() {
		return !this.isLastPage();
	}
	
	public void setIndexToNextPage() {

		if (this.currentPageNumberIndex >= (this.getPages().size() - 1)) {
			throw new RuntimeException("Page " + this.currentPageNumberIndex + " does not exist");
		}
		
		this.currentPageNumberIndex++;
	}
	
	public Page getNextPage() {
		this.setIndexToNextPage();
		return this.getCurrentPage();
	}

	public void setIndexToPreviousPage() {
		if (this.currentPageNumberIndex == 0) {
			throw new RuntimeException("Already at first page");
		}
		
		this.currentPageNumberIndex--;
	}
	
	public Page getPreviousPage() {
		this.setIndexToPreviousPage();
		return this.getCurrentPage();		
	}

	public void incrementPageNumber() {
		if (this.isLastPage()) {
			throw new RuntimeException("Already at last page");
		}
		
		this.currentPageNumberIndex++;
	}
		
	/**
	 * Add pages to existing set/book
	 * 
	 * @param pages
	 */
	public void addPages(List<Page> pages) {
		this.getPages().addAll(pages);
	}

	/**
	 * Prints all pages of content
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		
		for (Page page : pages) {
			sb.append(System.getProperty("line.separator"));
			
			sb.append("Page" + (i + 1) + "\n");
			sb.append(page);
			
			i++;
		}
		
		return sb.toString();		
	}
}
