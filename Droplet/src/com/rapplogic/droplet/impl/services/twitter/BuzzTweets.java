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

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.Alert;
import com.rapplogic.droplet.framework.service.RealtimeAlertPushService;
import com.rapplogic.droplet.framework.service.RecurringService;

/**
 * TODO this has not yet been implemented!!
 * <p/>
 * Queries the public timeline for the specified search terms and delivers
 * a steady stream of tweets to the remote.
 * <p/>
 * Tweets will be delivered every tweetDelay*pages, unless turned off
 * via the remote
 * 
 * @author andrew
 *
 */
public class BuzzTweets extends RealtimeAlertPushService implements RecurringService {

	@Override
	public Alert execute(ServiceContext serviceContext) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getDelay() {
		// TODO Auto-generated method stub
		return 0;
	}

	public RecurringType getType() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDelay(long delay) {
		// TODO Auto-generated method stub
		
	}

	public long getInitialDelay() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setInitialDelay(long initialDelay) {
		// TODO Auto-generated method stub
		
	}

}
