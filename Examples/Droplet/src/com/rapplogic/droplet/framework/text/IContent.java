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

import java.util.List;

public interface IContent {
	public void setLastPage();
	public void setFirstPage();
	public List<Page> getPages();
	public Page getFirstPage();
	public Page getCurrentPage();
	public void incrementPageNumber();
	public boolean isFirstPage();
	public boolean isLastPage();
	public boolean isNextPage();
	public Page getNextPage();
	public Page getPreviousPage();
	public boolean isErrorMessage();
	public void setIndexToPreviousPage();
	public void setIndexToNextPage();
	public void setErrorMessage(boolean errorMessage);
}
