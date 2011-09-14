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

package com.rapplogic.droplet.impl.services.twitter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.rapplogic.droplet.framework.text.ContentFormatter;

/**
 * Just the basics of a twitter tweet: user, message and time
 * 
 * @author andrew
 *
 */
public class Tweet {
	private static DateFormat timeDf = new SimpleDateFormat("h:mm a");
	
	private String user;
	private Date createdAt;
	private String message;
	
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = ContentFormatter.removeLineFeeds(message);
		this.message = ContentFormatter.removeCarriageReturns(this.message);
	}
	
	public String formatForLcd() {
		return "[" + this.getUser() + "] " + this.getMessage() + " @" + timeDf.format(this.getCreatedAt());
	}
	
	public String toString() {
		return "user=" + user + ",createdAt=" + this.createdAt.toString() + ",tweet=" + this.getMessage();
	}
}
