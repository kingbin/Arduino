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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;

import org.apache.log4j.Logger;

import com.sun.mail.imap.IMAPFolder;

/**
 * Connects to an IMAP server to receive new messages as the arrive via IMAP Idle.
 * Only acts on messages that contain the text/plain mime type.
 * <p/>
 * Service attempts to reconnect to IMAP server on folder close exceptions.
 * Uses Javamail MessageCountAdapter to get receive mail events.
 * <p/>
 * Note: Most gmail messages are multipart/ALTERNATIVE with a text/plain and text/html. 
 * obviously text/plain is more suitable for an LCD so we'll use that.  if text/plain
 * is not present, only the from/subject fields are captured.
 * <p/>
 * The iphone doesn't have gmail push but Droplet does, woot!
 * <p/>
 * exception in thread "main" javax.mail.FolderClosedException: * BYE JavaMail Exception: java.io.IOException: Connection dropped by server?
	at com.sun.mail.imap.IMAPFolder.throwClosedException(IMAPFolder.java:2447)
	at com.sun.mail.imap.IMAPFolder.idle(IMAPFolder.java:2249)
	at com.rapplogic.droplet.framework.framework.service.GmailService.main(JavamailImapMonitor.java:94)
<p/>
Prior to revamping the reconnect code:
// Getting folder disconnect after a few hours and it blocks on reconnect.  now closing store on folder exception
[2009-07-16 00:40:33,321] [pool-2-thread-3] [WARN] [com.rapplogic.droplet.impl.services.email.JavamailImapMonitor] folder close exception 
[2009-07-16 00:40:33,321] [pool-2-thread-3] [INFO] [com.rapplogic.droplet.impl.services.email.JavamailImapMonitor] connecting to imap server 
 * 
 * @author andrew
 *
 */
public class JavamailImapMonitor {
	
	private final static Logger log = Logger.getLogger(JavamailImapMonitor.class);
	
	private String username;
	private String password;
	private String imapServer;
	
	private Store store;
	private Folder folder;
	
	private MessageHandler messageHandler;
	
//	public static void main(String[] args) throws Exception {
//		PropertyConfigurator.configure("log4j.properties");
//		new JavamailImapMonitor();
//	}
	
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	public JavamailImapMonitor(String username, String password, String imapServer) throws NoSuchProviderException {
				
		this.username = username;
		this.password = password;
		this.imapServer = imapServer;
	}
	
	public void run() {
		
		try {
			while (true) {
				IMAPFolder f = (IMAPFolder) folder;
				log.debug("calling idle");
				f.idle();
				log.debug("after idle");
			}			
		} catch (FolderClosedException fce) {
			// try reconnect. untested
			log.warn("folder close exception");
			
			log.info("Closing store");
			
			try {
				store.close();
			} catch (MessagingException e) {
				log.warn("Store close failed", e);
			}
			
			while (true) {
				try {
					this.connect();
					
					log.info("successfully connected!");
					break;
				} catch (MessagingException me) {
					log.warn("Reconnect failed. sleeping for a min then trying again");
					
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {

					}
				}				
			}
		} catch (Exception e) {
			log.error("error in imap idle: ", e);
		}
	}
	
	public void connect() throws MessagingException {
		
		Properties props = System.getProperties();

		// Get a Session object
		Session session = Session.getInstance(props, null);
		// session.setDebug(true);

		// Get a Store object
		store = session.getStore("imaps");
		
		log.info("connecting to imap server");
				
		// Connect		
		store.connect(imapServer, this.username, this.password);

		log.debug("opening folder");
		
		// Open this inbox
		folder = store.getFolder("Inbox");

		folder.open(Folder.READ_ONLY);

		log.debug("adding message listener");
		
		// Add messageCountListener to listen for new messages
		folder.addMessageCountListener(new MessageCountAdapter() {
			
			public void messagesAdded(MessageCountEvent ev) {
				processMessages(ev.getMessages());
			}
			
			public void messagesRemoved(MessageCountEvent ev) {
				log.debug("message removed");
			}
		});	
	}

	private void processMessages(Message[] messages) {

		List<EmailMessage> emails = new ArrayList<EmailMessage>();
		
		try {
			log.debug("Got " + messages.length + " new messages");
			
			// Just dump out the new messages
			for (int i = 0; i < messages.length; i++) {
				log.debug("Message " + messages[i].getMessageNumber() + ":");
				
				//msgs[i].writeTo(System.out);

				try {
					emails.add(extractMessage(messages[i]));	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}		
			
			for (EmailMessage email : emails) {
				log.debug("email: " + email);
			}
			
			if (emails.size() > 0 && this.getMessageHandler() != null) {
				this.getMessageHandler().processMessages(emails);
			}
		} catch (Exception e) {
			log.error("process message failure", e);
		}
	}
	
	private EmailMessage extractMessage(Message message) throws Exception {
		log.debug("content type is " + message.getContentType());
		
		EmailMessage email = null;
		
		if (message.isMimeType("text/plain")) {
			email = createEmailMessage(message, (String)message.getContent());
		} else if (message.isMimeType("multipart/*")) {
			log.debug("itsa multipart");
			
		    Multipart mp = (Multipart) message.getContent();

		    int count = mp.getCount();
		    
		    log.debug("multipart count is " + mp.getCount());
		    
		    for (int i = 0; i < count; i++) {
		    	Part multipart = mp.getBodyPart(i);
		    	
		    	log.debug("part " + i + " is " + multipart.getContentType());
		    	
		    	if (multipart.isMimeType("text/plain")) {
		    		email = createEmailMessage(message, (String)multipart.getContent());
		    	} else {
		    		log.debug("ignoring multipart part: " + multipart.getContentType());
		    	}
		    }
		    
		    if (email == null) {
		    	email = createEmailMessage(message, "multipart does not contain text/plain");
		    }
		} else {
			// message does not contain text/plain!
			// text/html etc.
			
			email = createEmailMessage(message, "can't display message, content type: " + message.getContentType());
			
			log.warn("message does not contain text/plain.. can't display body. content type: " + message.getContentType() + ", content class is " + message.getContent().getClass().getName());
		}
		
		return email;
	}
	
	private EmailMessage createEmailMessage(Message message, String body) throws MessagingException {
		EmailMessage email = new EmailMessage();
		
		email.setFrom(message.getFrom()[0].toString());
		email.setSubject(message.getSubject());
		
		email.setBody(body);
		
		return email;
	}
	
	public static class EmailMessage {
		private String from;
		private String subject;
		private String body;
		
		public String getFrom() {
			return from;
		}
		public void setFrom(String from) {
			this.from = from;
		}
		public String getSubject() {
			return subject;
		}
		public void setSubject(String subject) {
			this.subject = subject;
		}
		public String getBody() {
			return body;
		}
		public void setBody(String body) {
			this.body = body;
		}
		
		public String toString() {
			return "from=" + this.getFrom() + ",subject=" + this.getSubject() + ",body=" + this.getBody();
		}
	}
	
	public static interface MessageHandler {
		void processMessages(List<EmailMessage> messages);
	}
}
