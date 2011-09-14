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

package com.rapplogic.droplet.impl;

import java.io.IOException;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.rapplogic.droplet.framework.Credentials;
import com.rapplogic.droplet.framework.Droplet;
import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.droplet.impl.services.email.ImapPushService;
import com.rapplogic.droplet.impl.services.googlecalendar.GoogleCalendarPullService;
import com.rapplogic.droplet.impl.services.googlecalendar.GoogleCalendarPushService;
import com.rapplogic.droplet.impl.services.news.TopStoriesService;
import com.rapplogic.droplet.impl.services.twitter.TwitterFriendsTimelinePullService;
import com.rapplogic.droplet.impl.services.twitter.TwitterPushService;
import com.rapplogic.droplet.impl.services.twitter.TwitterSearchPullService;
import com.rapplogic.droplet.impl.services.twitter.TwitterUserTimelinePullService;
import com.rapplogic.droplet.impl.services.weather.SevereWeatherPushService;
import com.rapplogic.droplet.impl.services.weather.WeatherPullService;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * A Droplet implementation with support for twitter, google calendar, gmail, current weather/forecast, 
 * time, news etc.
 * <p/>
 * Other services can be easily added by registering an implementation with Droplet
 * <p/>
 * You will need to modify a few things in this class:
 * <ul>
 * <li>XBee COM port and baud rate for the local XBee (coordinator)</li>
 * <li>XBee 64-bit address for push services</li>
 * <li>Specify your Twitter and Google credentials in twitter.credentials and 
 * google.credentials</li>
 * <li>Specify your zipcode for weather</li>
 * <li>Specify a different Twitter search keyword (optional)</li>
 * </ul>
 * 
 * @author Andrew Rapp
 */
public class DropletDemo {

	// TODO traffic alert, instant messenger (receive) gtalk/aol
	
	private final static Logger log = Logger.getLogger(DropletDemo.class);
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		
		String comPort = null;
		
		if (args.length == 1) {
			comPort = args[0];
		} else {
			// Replace with your local XBee's COM port.  On windows this will be something like COM1
			comPort = "/dev/tty.usbserial-A6005v5M";
		}

		//default XBee baud rate.
		int baudRate = 9600;

		// this is the address of the remote LCD XBee
		//Replace with the SH + SL address of the remote (LCD connected) XBee.
		XBeeAddress64 remoteXBee = new XBeeAddress64(0x00,0x13,0xa2,0x00,0x40,0x0a,0x3e,0x02);
		
		Droplet droplet = new Droplet();
	
		try {
			// connect to the XBee
			droplet.initXBee(comPort, baudRate);
				
			Credentials twitterCreds = null;
			
			// the app needs your twitter account (username/password) to access your tweets
			try {
				// enter your twitter username/password in this file
				twitterCreds = Credentials.read("twitter.credentials");
//				twitterCreds = Credentials.read("/Users/andrew/Documents/private/twitter.credentials");
			} catch (IOException e) {
				throw new RuntimeException("Couldn't find twitter credentials");
			}

			Credentials googleCreds;
			// the app needs your google account (username/password) to access your calendar and mail
			try {
				// enter your gmail username/password in this file
				googleCreds = Credentials.read("google.credentials");
//				googleCreds = Credentials.read("/Users/andrew/Documents/private/google.credentials");
			} catch (IOException e) {
				throw new RuntimeException("Couldn't find google.credentials");
			}
			
			/////////////////////////
			// Register Push Services
			/////////////////////////
			
			// register google calendar service
			GoogleCalendarPushService gcal = new GoogleCalendarPushService(googleCreds.getUsername(), googleCreds.getPassword(), remoteXBee);
			droplet.registerPushService(gcal);		
			
			// head for the bomb shelter
			droplet.registerPushService(new SevereWeatherPushService("20175", remoteXBee));
				
			// register twitter push service
			TwitterPushService twitter = new TwitterPushService(twitterCreds.getUsername(), twitterCreds.getPassword(), remoteXBee);
			twitter.setTweetOnStartup(false);
			// adjust delay if you are getting rate limited.  uncomment the following to run every five minutes
//			twitter.setDelay(60*1000*5);
			droplet.registerPushService(twitter);	
			
			// register an imap service for gmail
			ImapPushService gmail = new ImapPushService(googleCreds.getUsername(), googleCreds.getPassword(), "imap.gmail.com", remoteXBee);
			droplet.registerPushService(gmail);
			
			/////////////////////////
			// Register Pull Services
			/////////////////////////
			
			// google calendar.  this will return a listing of your next events
			GoogleCalendarPullService gcalEvents = new GoogleCalendarPullService(googleCreds.getUsername(), googleCreds.getPassword());
			droplet.registerPullService(0, gcalEvents);
			
			// returns your twitter timeline
			droplet.registerPullService(1, new TwitterFriendsTimelinePullService(twitterCreds.getUsername(), twitterCreds.getPassword()));
			
			// yahoo top stories
			droplet.registerPullService(2, new TopStoriesService());
			
			// enter your zip code to get current weather and forecast
			WeatherPullService weather = new WeatherPullService("20175");
			// note: the service id must correspond to the menu position, starting at 0
			weather.setCurrentWeatherServiceId(3);
			weather.setWeatherForecastServiceId(4);
			
			// current weather
			droplet.registerPullService(weather.getCurrentWeatherServiceId(), weather);
			droplet.registerPullService(weather.getWeatherForecastServiceId(), weather);			

			// twitter search.  you can enter any search term. no twitter account required
			droplet.registerPullService(5, new TwitterSearchPullService("woot"));
			
			// Simple time service
			PullService time = new PullService() {
				public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception {
					Calendar cal = Calendar.getInstance();
					return serviceContext.getFormatter().format(cal.getTime().toString());
				}
			};
			
			droplet.registerPullService(6, time);
			
			// Stephen Colbert tweets
			droplet.registerPullService(7, new TwitterUserTimelinePullService(twitterCreds.getUsername(), twitterCreds.getPassword(), "StephenAtHome"));
			
			// start services
			droplet.start();
			
			
			
			// advanced (optional)
			
			// if you want to get fancy, you can add a alert handler to act on the tweet
//			TwitterPushService twitter = new TwitterPushService(twitterCreds.getUsername(), twitterCreds.getPassword(), officeXBee)  {
//				public Alert handleAlert(Alert alert, Tweet tweet) {
//					// here I'm going to sound the alarm if it's cnn breaking news that contains "obama"
//					// but before you start following cnnbrk, you should know what they consider breaking news: 
//					// Ashton Kutcher is the first to reach 1 million followers in Twitter (seriously http://twitter.com/cnnbrk/status/1540144302)
//					if (tweet.getUser().equals("cnnbrk") && tweet.getMessage().toLowerCase().indexOf("obama") > -1) {
//						alert.setSoundAlarm(true);
//						return alert;
//					} else if (tweet.getUser().indexOf("andrewr") > -1) {
//						// ignore tweets from this user.  this guy is annoying.
//						return null;
//					} else {
//						// default
//						return alert;
//					}
//				}
//			};
			
			
			// blog example
//			PullService time = new PullService() {
//				public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception {		
//					return serviceContext.getFormatter().format("The current time is " + new Date() + ". You will see this response on your LCD after the service executes. You can even use linefeed characters \n to format the response");
//				}
//			};
//
//			droplet.registerPullService(1, time);
			
			
			// blog example test it
			//XBeeAddress64 remoteXBee = new XBeeAddress64(0x00,0x13,0xa2,0x00,0x40,0x0a,0x3e,0x02);
			//
//			SimpleRealtimeRecurringPushService wakeUp = new SimpleRealtimeRecurringPushService(remoteXBee) {
//
//				@Override
//				public Alert execute(ServiceContext serviceContext) throws Exception {
//					Calendar cal = Calendar.getInstance();
//					
//					int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
//					
//					if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
//						// it's the weekend.  you can sleep in
//						return null;
//					}
//					
//					int minute = cal.get(Calendar.MINUTE);
//					int hour = cal.get(Calendar.HOUR_OF_DAY);
//					
//					if (hour == 6 && minute == 0) {
//						// wake up!
//						Alert alert = new Alert();
//						alert.setContent(serviceContext.getFormatter().format("It's Wake Up Time !!!!!!"));
//						alert.setRemoteXBeeAddress(this.getRemoteXBeeAddress());
//						// flash the LED
//						alert.setFlashLed(true);
//						// sound the alarm!
//						alert.setSoundAlarm(true);
//						
//						return alert;
//					}
//					
//					// not time yet
//					return null;				}	
//			};
//
//			// run once a minute (delay is in milliseconds, so multiple by 1000)
//			wakeUp.setDelay(60000);
//			wakeUp.setName("WakeUp");
//
//			// register the service with the framework
//			droplet.registerPushService(wakeUp);
			
		} finally {
			droplet.shutdown();
		}
	}
}