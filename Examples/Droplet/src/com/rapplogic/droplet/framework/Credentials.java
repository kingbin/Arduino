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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads a file containing a username/password pair
 * 
 * @author andrew
 *
 */
public class Credentials {
	private String username;
	private String password;
	
	public Credentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

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

	public String toString() {
		return "username=" + this.getUsername() + ",password=" + this.getPassword();
	}
	
	/**
	 * File must contain username and password at top of file as follows:
	 * 
	 * username=xxxx
	 * password=xxxx
	 * 
	 * @param path
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Credentials read(String path) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(path));
		
		return new Credentials(props.getProperty("username"), props.getProperty("password"));
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Credentials cred = read("/Users/andrew/Documents/private/gcal.credentials");
		System.out.println(cred);
	}
}
