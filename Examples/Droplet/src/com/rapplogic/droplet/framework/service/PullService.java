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

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * Describes a user-initiated service.
 * More specifically this service runs in response to a button press
 * on a remote LCD.
 * <p/>
 * An object of type IContent is sent to the remote for display on the LCD
 * Content larger than LCD size will automatically be paginated.
 */
public interface PullService {

	/**
	 * Called by the framework when a user request is received.  Returns
	 * the content to be displayed on the remote LCD.
	 * <p/>
	 * You may call serviceContext.getFormatter().format(String) to format
	 * content into an <code>IContent</code>, to be returned.
	 * <p/>
	 * Null may be returned if service performs an action that does not require 
	 * a response, however, if null is returned, the user will receive a message indicating the
	 * service did not provide a response.
	 * 
	 * @param serviceId
	 * @param response
	 * @param serviceContext
	 * @return
	 * @throws Exception
	 */
	public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception;
}
