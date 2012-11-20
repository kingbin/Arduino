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

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.text.Content;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * Returns the friends_timeline for display.
 * 
 * @author andrew
 *
 */
public class TwitterUserTimelinePullService implements PullService {

	private Twitter twitter;
	private String screenName;
	
	public TwitterUserTimelinePullService(String username, String password, String screenName) throws ParserConfigurationException {
		this.twitter = new Twitter();
		this.twitter.setUsername(username);
		this.twitter.setPassword(password);
		this.screenName = screenName;
	}

	public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception {
		List<Tweet> results = twitter.getUserTimeLineXml(screenName);
		
		if (results.size() == 0) {
			return serviceContext.getFormatter().format("no tweets!!");
		} else {
			Content content = new Content();
			
			for (Tweet tweet : results) {
				content.getPages().addAll(serviceContext.getFormatter().format(tweet.formatForLcd()).getPages());
			}
			
			return content;
		}
	}
}
