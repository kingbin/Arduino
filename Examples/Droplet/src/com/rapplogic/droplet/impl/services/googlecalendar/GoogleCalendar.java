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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.extensions.Reminder;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.util.ServiceException;
import com.rapplogic.droplet.framework.Credentials;

/**
 * A basic API for retrieving calendar events and reminders from Google Calendar.
 * Uses the Google Gdata API.
 * <p/>
 *  TODO repeat events do not have reminders if they are unmodified (clone of original event), so must extrapolate base event reminders to the repeats
 *  if repeat events was modified (instance only, not series), google will return it as an calendar entry and generate reminders, but it will also 
 *  include the same event as a repeat in the getTimes(), so I have duplicates now, boooooooo
 *  
 * @author andrew
 *
 */
public class GoogleCalendar {

	private final static Logger log = Logger.getLogger(GoogleCalendar.class);

	private String username;
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public GoogleCalendar() {

	}

	public List<EventAlert> getEventAlertsFourWeeksOut(boolean getReminders) throws IOException, ServiceException, CloneNotSupportedException {
		
		Calendar fourWeeksOut = Calendar.getInstance();
		fourWeeksOut.add(Calendar.DAY_OF_YEAR, 7 * 4 + 1);
		log.debug("fours weeks + 1 day is " + fourWeeksOut.getTime());
		
		CalendarEventFeed resultFeed = this.eventQuery(new Date(), fourWeeksOut.getTime());
		
		// get all cal events
		// CalendarEventFeed resultFeed = service.getFeed(feedUrl,
		// CalendarEventFeed.class);

		// alerts are notices about an event (reminder) or the actual event or a repeat of the event
		List<EventAlert> alerts = new ArrayList<EventAlert>();

		for (CalendarEventEntry entry : resultFeed.getEntries()) {

			log.debug("cal event entry: title " + entry.getTitle().getPlainText() + ", id is " + entry.getIcalUID());

			Event event = new Event();
			
			event.setId(entry.getIcalUID());
			event.setTitle(entry.getTitle().getPlainText());
			event.setDescription(entry.getTextContent().getContent().getPlainText());
	
			// not useful
//			if (entry.getRecurrence() != null) {
//				log.debug("event recurrence is " + entry.getRecurrence().getValue());	
//			}
			
			Where firstWhere = null;
			
			for (Where where : entry.getLocations()) {
				if (firstWhere == null) {
					firstWhere = where;
					
					if (firstWhere.getValueString() != null) {
						event.setLocation(firstWhere.getValueString());
					}
					
					log.debug("event location: " + where.getValueString());
				} else {
					log.warn("calendar event has multiple locations.. ignoring: " + where);
				}
			}
			
			When firstTime = null;

			// repeating events will have multiple times
			for (When when : entry.getTimes()) {
				if (firstTime == null) {

					firstTime = when;
					
					event.setStart(new Date(firstTime.getStartTime().getValue()));
					event.setEnd(new Date(firstTime.getEndTime().getValue()));
				} else {
					log.debug("handling repeat event: " + when.getStartTime() + ", end " + when.getEndTime());
					
					Event repeat = event.getEvent();
					
					repeat.setStart(new Date(when.getStartTime().getValue()));
					repeat.setEnd(new Date(when.getEndTime().getValue()));
					repeat.setRepeat(true);
					
					EventAlert alert = new EventAlert();
					alert.setDate(repeat.getStart());
					alert.setEvent(repeat);
					// this tells us it's not a reminder but the real thing
					alert.setMinutesBeforeEvent(0);	
					
					log.debug("adding repeat event time as a reminder " + alert);
					alerts.add(alert);
					
				}
			}			

			//add the actual event date as an alert.. this is a bit hacky
			EventAlert eventAlert = new EventAlert();
			eventAlert.setDate(event.getStart());
			eventAlert.setEvent(event);
			// this tells us it's not a reminder but the real thing
			eventAlert.setMinutesBeforeEvent(0);				

			log.debug("adding event time as alert " + eventAlert);
			alerts.add(eventAlert);
			
			if (getReminders) {
				// minutes is only time field that is populated.  weird
				// returns all reminders, incl. past reminders
				for (Reminder reminder : entry.getReminder()) {

					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(firstTime.getStartTime().getValue());

					// now back up by the number of mins
					cal.add(Calendar.MINUTE, -1 * reminder.getMinutes());
					log.debug("reminder date is " + cal.getTime());

					EventAlert er = new EventAlert();
					er.setDate(cal.getTime());
					er.setEvent(event);
					er.setMinutesBeforeEvent(reminder.getMinutes());
					alerts.add(er);
					
					log.debug("handling reminder: " + er);
				}				
			}
		}

//		for (EventAlert alert : alerts) {
//			log.debug("event alert is " + alert);
//		}
		
		return alerts;
		
	}

	public CalendarEventFeed eventQuery(Date start, Date end) throws IOException, ServiceException, CloneNotSupportedException {

		CalendarService service = new CalendarService("rapplogic-arduinoxbeeservice-1");
		service.setUserCredentials(this.getUsername(), this.getPassword());
		//		
		URL feedUrl = new URL("http://www.google.com/calendar/feeds/default/private/full");

		// get future cal events

		CalendarQuery query = new CalendarQuery(feedUrl);
		query.setMinimumStartTime(new DateTime(start.getTime()));
		// max time should be out as far as the maximum reminder (4 weeks)

		query.setMaximumStartTime(new DateTime(end.getTime()));

		// Send the request and receive the response:
		CalendarEventFeed feed = service.query(query, CalendarEventFeed.class);
		
		return feed;
	}
		
	public static void main(String[] args) throws FileNotFoundException, IOException, ServiceException, CloneNotSupportedException {
		PropertyConfigurator.configure("log4j.properties");
		Credentials cred = Credentials.read("/Users/andrew/Documents/private/gcal.credentials");
		GoogleCalendar gcal = new GoogleCalendar();
		gcal.setUsername(cred.getUsername());
		gcal.setPassword(cred.getPassword());
		
		gcal.getEventAlertsFourWeeksOut(false);
	}
}
