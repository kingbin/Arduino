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

package com.rapplogic.droplet.impl.services.email;

import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.service.Alert;
import com.rapplogic.droplet.framework.service.RunnablePushService;
import com.rapplogic.droplet.impl.services.email.JavamailImapMonitor.EmailMessage;
import com.rapplogic.droplet.impl.services.email.JavamailImapMonitor.MessageHandler;
import com.rapplogic.xbee.api.XBeeAddress64;


/**
 * Registers a message handler with JavamailImapMontitor to send alerts when new messages
 * are received. Only emails that contain the text/plain mime type are supported.  If an
 * email is text/html or something else, you will get the from/subject only.
 * <p/>
 * Displays a maximum of 640 characters of an email message (approximately 8 pages on an LCD).
 * Any content beyond 640 characters is discarded.
 * 
 * @author andrew
 *
 */
public class ImapPushService extends RunnablePushService {
	
	private final static Logger log = Logger.getLogger(ImapPushService.class);
	
	/**
	 * Maximum size of an email (in characters). 
	 */
	public final static int MAX_EMAIL_SIZE = 640;
	
	private JavamailImapMonitor imap;
	
	public ImapPushService(String username, String password, String imapServer, XBeeAddress64 remoteXBeeAddress) throws MessagingException {
		super();
		
		this.setRemoteXBeeAddress(remoteXBeeAddress);
		
		imap = new JavamailImapMonitor(username, password, imapServer);
		
		log.debug("connecting..");
		
		imap.setMessageHandler(new MessageHandler() {

			public void processMessages(List<EmailMessage> messages) {
				
				for (EmailMessage message : messages) {	
					log.debug("sending alert for message " + message);
					
					Alert alert = new Alert();
					// TODO source app (i.e. twtr) should be added in less of a hacky way
					// TODO Create StringAlert class that has setText method.  framework should paginate content!!!
					alert.setContent(getServiceContext().getFormatter().format("gmail:" + formatMessage(message)));
					alert.setRemoteXBeeAddress(getRemoteXBeeAddress());
					
					// send it!
					getServiceContext().sendAlert(alert);						

				}
			}
		});
		
		imap.connect();
	}
	
	public String formatMessage(EmailMessage message) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("from-> " + message.getFrom());
		
		if (message.getSubject() != null && message.getSubject().trim().length() > 0) {
			sb.append(",subject-> " + message.getSubject());	
		}
		
		if (message.getBody() != null && message.getBody().trim().length() > 0) {
			sb.append(", body->" + this.truncate(message.getBody()));	
		}
		
		return sb.toString();
	}
	
	private String truncate(String s) {
		if (s.length() <= MAX_EMAIL_SIZE) {
			return s;
		} else {
			return s.substring(0, MAX_EMAIL_SIZE - 3) + "...";
		}
	}
	
	public void run() {
		imap.run();
	}


	@Override
	public String getName() {
		return "gmail";
	}
}
