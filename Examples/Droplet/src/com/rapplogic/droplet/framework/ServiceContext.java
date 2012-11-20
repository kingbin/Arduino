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

package com.rapplogic.droplet.framework;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import com.rapplogic.droplet.framework.service.Alert;
import com.rapplogic.droplet.framework.service.DelayedAlert;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.service.PushService;
import com.rapplogic.droplet.framework.text.ContentFormatter;

/**
 * Exposes the Droplet API functionality to services, allowing
 * services to send messages, alerts, format content etc.
 * 
 * @author andrew
 *
 */
public interface ServiceContext {

	public void registerPullService(Integer serviceId, PullService service);
	public void unRegisterPullService(Integer serviceId);
	public Runnable registerPushService(final PushService service);
	public boolean unRegisterPushService(Runnable runnable);
	public void sendAlert(Alert alert);
	public void sendDelayedAlert(DelayedAlert alert);
	public long getPullServiceTimeoutMillis();
	public void setPullServiceTimeoutMillis(long serviceTimeoutMillis);
	public int getXBeeSendTimeout();
	public void setXBeeSendTimeout(int sendTimeout);
	public ContentFormatter getFormatter();
	public BlockingQueue<Alert> getAlertQueue();
	public DelayQueue<DelayedAlert> getDelayedAlertQueue();
}
