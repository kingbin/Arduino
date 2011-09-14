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

/**
 * Thrown if a message to a remote is undeliverable.  
 * Now why didn't I call it UndeliverableException??
 * 
 * @author andrew
 *
 */
public class DeliveryException extends RuntimeException {

	public DeliveryException(String s, Exception e) {
		super(s, e);
	}
	
	public DeliveryException(String s) {
		super(s);
	}
}
