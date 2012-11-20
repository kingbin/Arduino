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

import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.Alert;
import com.rapplogic.droplet.framework.service.RealtimeAlertPushService;
import com.rapplogic.droplet.framework.service.RecurringService;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Runs every minute and checks the friends_timeline for new tweets.
 * New tweets are delivered to the LCD
 * 
 * @author andrew
 *
 */
public class TwitterPushService extends RealtimeAlertPushService implements RecurringService {
	
	private final static Logger log = Logger.getLogger(TwitterPushService.class);
	
	private Twitter twitter;
	private Date lastTweet;
	private boolean tweetOnStartup;

	private long initialDelay = 0L;
	private long delay = 60000L;
	
	public TwitterPushService(String username, String password, XBeeAddress64 remoteXBeeAddress) throws ParserConfigurationException {
		twitter = new Twitter();
		this.twitter.setUsername(username);
		this.twitter.setPassword(password);
		this.setRemoteXBeeAddress(remoteXBeeAddress);
	}
	
	public Alert execute(ServiceContext serviceContext) throws Exception {
		
//		log.debug("waiting on delay");
//		// very bad.. don't do this!
//		Thread.sleep(70000);
//		log.debug("after delay");
		
		Tweet tweet = this.twitter.getFriendsTimelineLastTweet();
		
		if (tweet == null) {
			// no tweets
			return null;
		}
		
		Alert alert = null;
		
		boolean sendTweet = false;
		
		if (this.tweetOnStartup && lastTweet == null) {
			sendTweet = true;
		} else if (lastTweet != null && lastTweet.before(tweet.getCreatedAt())) {
			sendTweet = true;
		}
		 
		if (sendTweet) {
			// it's new.  send to LCD
			alert = new Alert();
			// TODO source app (i.e. twtr) should be added in less of a hacky way
			alert.setContent(serviceContext.getFormatter().format("twtr:" + tweet.formatForLcd()));
			alert.setRemoteXBeeAddress(this.getRemoteXBeeAddress());
		} else {
			// no updates
		}

		if (tweet != null) {
			lastTweet = tweet.getCreatedAt();
//			log.debug("tweet is " + tweet.toString());
		}
		
		if (alert != null) {
			return this.handleAlert(alert, tweet);
		} 
		
		// no updates
		return null;
	}

	/**
	 * Called when new tweet is detected.  Subclass to decorate/filter/block the alert
	 * Return null if alert should not be sent
	 * 
	 * @param alert
	 * @param tweet
	 * @return
	 */
	public Alert handleAlert(Alert alert, Tweet tweet) {
		return alert;
	}
	
	public RecurringType getType() {
		return RecurringType.FIXED_DELAY;
	}

	/**
	 * Start immediately
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

	/**
	 * Run every minute
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * If you are using other tools for twitter updates you may need to set a 
	 * longer delay -- default is 60000 (1 minute) -- or you may hit the rate
	 * limit of 100 requests per hour.
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	@Override
	public String getName() {
		return "twitter";
	}
	
	/**
	 * If true sends tweet on startup
	 *  
	 * @return
	 */
	public boolean isTweetOnStartup() {
		return tweetOnStartup;
	}

	/**
	 * Set to true for a tweet on startup
	 * 
	 * @param tweetOnStartup
	 */
	public void setTweetOnStartup(boolean tweetOnStartup) {
		this.tweetOnStartup = tweetOnStartup;
	}
}
