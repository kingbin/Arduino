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

package com.rapplogic.droplet.impl.services.googlecalendar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.rapplogic.droplet.framework.Credentials;
import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.DelayedAlert;
import com.rapplogic.droplet.framework.service.DelayedAlertPushService;
import com.rapplogic.droplet.framework.service.RecurringService;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Runs every minute and checks for new/modified/deleted calendar events to schedule
 * Place #buzzer# in the event description to activate the buzzer when event fires
 * <p/>
 * Currently does not handle reminders for repeat events very well due to some errr
 * inconsistencies in the google calendar api.
 * 
 * @author andrew
 *
 */
public class GoogleCalendarPushService extends DelayedAlertPushService implements RecurringService {
	
	private final static Logger log = Logger.getLogger(GoogleCalendarPushService.class);
	
	private GoogleCalendar calendar = new GoogleCalendar();
	
	private HashMap<Integer, EventAlert> alertMap = new HashMap<Integer, EventAlert>();
	
	private DateFormat timeDf = new SimpleDateFormat("h:mm a");
	private DateFormat dayDf = new SimpleDateFormat("M/d");
	
	private long initialDelay = 0L;
	private long delay = 60000L;
	
	private final String alarmIndicator = "#buzzer#";
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		Credentials cred = Credentials.read("/Users/andrew/Documents/private/gcal.credentials");
		GoogleCalendarPushService gcs = new GoogleCalendarPushService(cred.getUsername(), cred.getPassword(), new XBeeAddress64(0x00,0x13,0xa2,0x00,0x40,0x0a,0x3e,0x02));
	}
	
	public GoogleCalendarPushService(String username, String password, XBeeAddress64 remoteXBeeAddress) {
		this.calendar.setUsername(username);
		this.calendar.setPassword(password);
		this.setRemoteXBeeAddress(remoteXBeeAddress);
	}
	
	public void execute(ServiceContext serviceContext) throws Exception {
		
		List<EventAlert> alerts = calendar.getEventAlertsFourWeeksOut(true);
		
		Date now = new Date();
		
		for (EventAlert eventAlert: alerts) {
			
			if (eventAlert.getDate().before(now)) {
				log.debug("ignoring past calendar alert " + eventAlert);
				continue;
			}
			
			log.debug("evaluating alert " + eventAlert);
			
			// could use map.get(Object) which depends on equals
			if (alertMap.containsKey(eventAlert.hashCode())) {
//				log.debug("reminder is already in map");
				
				// won't work until equals is implemented
				if (alertMap.get(eventAlert.hashCode()).getEvent().uniqueId() != eventAlert.getEvent().uniqueId()) {
					log.info("reminder has been updated.  need to remove and re-add new event to delay queue: " + eventAlert);
					// update new one
					alertMap.put(eventAlert.hashCode(), eventAlert);
					
					DelayedAlert da = this.createDelayedAlert(eventAlert, serviceContext);
					
					boolean success = serviceContext.getDelayedAlertQueue().remove(da);
					
					if (success) {
						// add updated
						serviceContext.getDelayedAlertQueue().offer(da);
					} else {
						log.error("remove was unsuccesful");
					}
				} else {
//					log.debug("alert has not changed.. ignoring");
				}
			} else {
				log.debug("new alert.. adding to map: " + eventAlert);
				alertMap.put(eventAlert.hashCode(), eventAlert);
				
				// add to queue
				serviceContext.getDelayedAlertQueue().offer(this.createDelayedAlert(eventAlert, serviceContext));
			}
		}
		
		List<EventAlert> deleteList = new ArrayList<EventAlert>();
		
		// now find deleted.. set operations would be nice here
		// we are looking for reminders that have been deleted (in hash but not in the latest results).
		// this could mean it was deleted or has been processed already
		for (EventAlert alertHash: alertMap.values()) {
			
//			log.debug("looking for hash reminder in latest results: " + hashReminder);
			
			boolean found = false;
			
			for (EventAlert reminder: alerts) {
				
//				log.debug("current reminder is: " + reminder);
				
				if (alertHash.equals(reminder)) {
					found = true;
//					log.debug("current reminder is equal!");
					break;
				}
			}
			
			if (!found) {
				deleteList.add(alertHash);
			}
		}
		
		for (EventAlert delAlert : deleteList) {
			alertMap.remove(delAlert.hashCode());
			log.info("reminder was deleted.  removing from delay queue " + delAlert);
			
			boolean success = serviceContext.getDelayedAlertQueue().remove(this.createDelayedAlert(delAlert, serviceContext));
			
			if (!success) {
				log.error("failed to remove reminder from queue");
			} else {
//				log.info("successfully removed reminder from delayQueue");
			}
		}
		
		// print queue
//		for (DelayedAlert alert : delayQueue) {
//			log.debug("delayedAlertQueue: " + alert);
//		}
	}

	private DelayedAlert createDelayedAlert(final EventAlert alert, ServiceContext sc) {
		
//		if (this.getExpectedFireDate().equals(da.getExpectedFireDate())) {
//			return true;
//		}
		
		DelayedAlert da = new DelayedAlert();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("gcal:");
		
		if (alert.isReminder()) {
			sb.append("(reminder):");
		}
		
		sb.append(dayDf.format(alert.getDate()));
		
		if (!alert.getEvent().isAllDay()) {
			sb.append(" ");
			sb.append(timeDf.format(alert.getEvent().getStart()));
			sb.append("-");
			sb.append(timeDf.format(alert.getEvent().getEnd()));
		}
		
		sb.append(" ");
		sb.append(alert.getEvent().getTitle());
		
		if (alert.getEvent().getDescription() != null && alert.getEvent().getDescription().length() > 0) {
			sb.append("-");
			// remove alarmIndicator from description
			sb.append(alert.getEvent().getDescription().replaceAll(alarmIndicator, ""));			
		}
		
		if (alert.getEvent().getLocation() != null && alert.getEvent().getLocation().length() > 0) {
			sb.append(",Location:");
			sb.append(alert.getEvent().getLocation());
		}
		
//		log.debug("formatted event is " + sb.toString());

		da.setContent(sc.getFormatter().format(sb.toString()));
		// set the delay from now until when the reminder should be sent to the remote
		da.setExpectedFireDate(alert.getDate());
		da.setRemoteXBeeAddress(this.getRemoteXBeeAddress());
		
		if (alert.getEvent().getDescription().indexOf(alarmIndicator) > -1) {
			log.debug("#buzzer# token found in event description");
			da.setSoundAlarm(true);
		}
		
		da.setId(alert.getEvent().getId());
		
		return da;
	}
	
	public long getExecutionDelay() {
		// run once a minute
		return 60000L;
	}

	public RecurringType getType() {
		return RecurringType.FIXED_RATE;
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

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}
	
	@Override
	public String getName() {
		return "google-calendar";
	}
}
