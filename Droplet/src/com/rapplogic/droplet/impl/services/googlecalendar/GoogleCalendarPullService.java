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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.text.Content;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * Retrieves calendar events, up to four weeks out.
 * 
 * @author andrew
 *
 */
public class GoogleCalendarPullService implements PullService {

	private final static Logger log = Logger.getLogger(GoogleCalendarPushService.class);
	
	private GoogleCalendar calendar = new GoogleCalendar();
	
	// TODO share with push service
	private DateFormat timeDf = new SimpleDateFormat("h:mm a");
	private DateFormat dayDf = new SimpleDateFormat("MMM d");
	
	
	public GoogleCalendarPullService(String username, String password) {
		calendar.setUsername(username);
		calendar.setPassword(password);
	}
	
	public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception {
		List<EventAlert> events = calendar.getEventAlertsFourWeeksOut(false);

		Date now = new Date();
		
		List<EventAlert> newList = new ArrayList<EventAlert>();
		
		for (EventAlert alert: events) {
			if (alert.getDate().before(now)) {
				log.debug("ignoring past event: " + alert);
			} else {
				newList.add(alert);
			}
		}
		
		if (newList.size() == 0) {
			return serviceContext.getFormatter().format("No events in calendar");
		} else {
			// sort by date
			Collections.sort(newList);
			
			Content c = new Content();
			
			for (EventAlert alert : newList) {
				c.getPages().addAll(serviceContext.getFormatter().format(this.formatAlertForLcd(alert)).getPages());
			}
			
			return c;			
		}

	}
	
	// TODO move to EventAlert
	public String formatAlertForLcd(EventAlert alert) {
		StringBuilder sb = new StringBuilder();
		
		// TODO add "today" if event is today, or in 20 minutes
//		Calendar today = Calendar.getInstance();
//		today.get(Calendar.DAY_OF_YEAR);
//		
//		Calendar alertCal = Calendar.getInstance();
//		alertCal.setTime(alert.getDate().getTime());
//		
//		if (alertCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))

		sb.append(dayDf.format(alert.getDate()));
		
		if (alert.getEvent().getStart().getTime() != alert.getEvent().getEnd().getTime()) {
			sb.append(" ");
			sb.append(timeDf.format(alert.getEvent().getStart()));
			sb.append("-");
			sb.append(timeDf.format(alert.getEvent().getEnd()));
		} else {
			// zero minute event
			sb.append(" ");
			sb.append(timeDf.format(alert.getEvent().getStart()));
		}
		
		sb.append(" ");
		sb.append(alert.getEvent().getTitle());
		
		if (alert.getEvent().getDescription() != null && alert.getEvent().getDescription().length() > 0) {
			sb.append("-");
			// remove alarmIndicator from description
			sb.append(alert.getEvent().getDescription());			
		}
		
		if (alert.getEvent().getLocation() != null && alert.getEvent().getLocation().length() > 0) {
			sb.append(",Location:");
			sb.append(alert.getEvent().getLocation());
		}
		
		return sb.toString();
	}

}
