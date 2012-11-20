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

package com.rapplogic.droplet.framework.service;

import com.rapplogic.droplet.framework.DeliveryException;
import com.rapplogic.droplet.framework.Message;

/**
 * Describes a <code>Message</code> sent by a Push Service to a remote XBee.
 * It specifies if the flashLed or soundAlarm actions should occur
 * 
 * @author andrew
 *
 */
public class Alert extends Message {

	private boolean flashLed = true;
	private boolean soundAlarm = false;
	
	public boolean isFlashLed() {
		return flashLed;
	}
	public void setFlashLed(boolean flashLed) {
		this.flashLed = flashLed;
	}
	
	public boolean isSoundAlarm() {
		return soundAlarm;
	}
	
	public void setSoundAlarm(boolean soundAlarm) {
		this.soundAlarm = soundAlarm;
	}
	
	/**
	 * The framework calls method if delivery of the alert fails
	 * 
	 * @param message
	 */
	public void handleError(DeliveryException e) {
		
	}
	
	public String toString() {
		return super.toString() + ",flashLed=" + this.flashLed + ",alarm=" + this.soundAlarm;
	}
}
