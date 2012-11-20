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

package com.rapplogic.droplet.impl.services.weather;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

/**
 * A basic <code>NamespaceContext</code> implementation for performing 
 * XPath queries on XMLs that use namespace
 * 
 * @author andrew
 *
 */
public class Namespace implements NamespaceContext {
	
	public String uri;
	public String prefix;


	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getNamespaceURI(String prefix) {
		return uri;
	}

	public String getPrefix(String namespaceURI) {
		return prefix;
	}

	public void setNamespaceUri(String uri) {
		this.uri = uri;
	}

	public Iterator getPrefixes(String namespaceURI) {
		return null;
	}
}