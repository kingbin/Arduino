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

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.internal.ContentHistory;
import com.rapplogic.droplet.framework.internal.ContentHistoryMap;
import com.rapplogic.droplet.framework.internal.ContentHistory.PageNotFoundException;
import com.rapplogic.droplet.framework.service.Alert;
import com.rapplogic.droplet.framework.service.DelayedAlert;
import com.rapplogic.droplet.framework.service.DelayedAlertPushService;
import com.rapplogic.droplet.framework.service.OneTimeService;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.service.PushService;
import com.rapplogic.droplet.framework.service.RealtimeAlertPushService;
import com.rapplogic.droplet.framework.service.RecurringService;
import com.rapplogic.droplet.framework.service.RunnablePushService;
import com.rapplogic.droplet.framework.text.ContentFormatter;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import com.rapplogic.xbee.util.ByteUtils;

/**
 * Receives XBee RX Packets from the remote XBee/Arduino/LCD for a service and responds with a string
 * to be displayed on the LCD.  The Droplet implementation must register a service to handle the request or
 * a error message will be displayed on the LCD.  The request is processed in a separate thread in order 
 * to guarantee a response to the remote XBee in a timely manner, in the event a service takes longer than 
 * expected to respond (e.g. network latency/timeout when fetching content over the internet).
 * <p/>
 * The service processRequest method will need to respond within getServiceTimeoutMillis() milliseconds 
 * (defaults to 6000 or 6 seconds).  
 * If the service does not respond in that time, it will timeout and the LCD will display "Application Timeout".  
 * You can change this timeout by calling setServiceTimeoutMillis method.  If you change this timeout, 
 * you should also change the Arduino timeout to the same value, plus a buffer of 1 or 2 seconds.  
 * The buffer is necessary to account for the round trip packet transmission time.  
 * Packets are usually delivered in under 100ms but packet size, distance, interference, and all sorts of factors 
 * can result in delays.
 * <p/>
 * This is coded for Series 2 XBee but will also work with Series 1 radios with some modifications.
 * All you need to do is replace the ZB request/responses with the Series 1 equivalents.
 * And perform the same modifications in the Arduino Sketch.
 * <p/>
 * When using Series 2 XBees make sure you are using the ZB Pro firmware since we need to send a 
 * packet of 80 characters, which it supports but ZNet does not.  Series 1 radios support 100 bytes per
 * packet.
 * <p/>
 * This isn't designed for services that require frequent updates (e.g. monitoring linux "top" command).
 * For that type of setup, consider connecting the LCD directly to the host via USB, like LCD Smartie
 * and other similar projects
 * <p/>
 * Here are some other LCD projects but they are different in that they rely on a USB/Parallel connections to the LCD:
 * <p/>
 * <ul>
 * <li>LCD Smartie http://lcdsmartie.sourceforge.net/</li>
 * <li>LCD4Linux http://ssl.bulix.org/projects/lcd4linux/</li>
 * <li>LCDProc http://lcdproc.omnipotent.net/</li>
 * <li>Pertelian X2040 http://www.pertelian.com/joomla/index.php?option=com_content&task=view&id=43&Itemid=48</li>
 * </ul>
 * <p/>
 * This application uses XBee ACK as an indicator that a message was successfully delivered to a remote.
 * This is quite reliable but not 100% correct as the ACK only indicates that the packet was delivered to the
 * remote XBee, not that the packet was consumed by the Arduino.  However it is highly unlikely 
 * that the XBee would need to be powered on and Arduino powered off, or for some reason not reading bytes off
 * the serial line.  To remedy this situation it would be necessary 
 * for the Arduino to send a reply packet.  I'm not going to do this.  Just wanted to share.
 * <p/>
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 * <p/>
 * <!--
 * FIXME history (up) should move marker to beginning of last content.  problem: pull services, like twitter bundle all tweets into a single content, 
 * so we can't index to first page of previous tweet.  need something like book->chapter->pages model or
 * envelope -> list<content>  or content -> content group -> pages
 * FIXME need to advance history marker to last page before adding new content.  problem is new content is inserted in middle
 * FIXME history should only include what was send to lcd, not everything that was paginated. diff comes when user cancels out curr. content
 * 
 * TODO database for recording events/jetty server for access event history, configuration etc.
 * TODO arduino pin to control backlight
 * TODO jmx instrumentation
 * TODO stats pull service (how long droplet has been running, #pull/push requests, delivery errors, timeouts etc.)
 * TODO ideally the remote Arduino should request the menu/service ids from this application.  this would greatly ease updates
 * TODO missed queue: alerts that are undeliverable should queue until the radio comes back online, then get merged and sent
 * TODO support delivery of an alert to multiple radios (e.g. google calendar goes to bedroom and office remotes)
 * TODO google talk service
 * TODO broadcast alerts/messages from one end device to another
 * TODO asynchronous pull service.  this is a long running pull service, like kick off build.  user receives immediate reply then alert is sent when task completes
 * TODO capability to send parameters to service (user would use up/down buttons to specify value alpha or numeric to send to service
 *      we have a lot of payload space for request packets (arduino to service)
 * TODO generic rss service
 * TODO set quiet period (e.g. 10pm-6am) where sound alarm is ignored
 * TODO node discover on startup and keep track of what remote device have connected
 * TODO support series 1 XBee
 * TODO backlight on/off and XBee sleep
 * TODO decided this is not worthwhile ->compress characters (limit char space to 64) into 5 bytes to fit 60% more chars into a packet.  this is only useful if the Arduino can support a large enough buffer
 *
 * Notes: 
 * 
 * 8/5/09: Noticing a lot of ADDRESS_NOT_FOUND errors. t'storms in the area and downloading windows 7 RC2.  gonna blame ms on this one
 * 
 * Delivery failure and I know the remote radio was on.  I expect better, XBee.
 * [2009-07-19 00:21:29,361] [Alert Thread] [WARN] [com.rapplogic.droplet.framework.Droplet] Failed to send alert. remote may be offline: address=0x00 0x13 0xa2 0x00 0x40 0x0a 0x3e 0x02,content=
Page1
twtr:[ToddBailey]|Got a haircut and am|going to bed early|and alone on a|
Page2
Saturday.  A solipsi|stic study in cause|and effect. @12:20AM|,flashLed=true,alarm=false 
com.rapplogic.droplet.framework.DeliveryException: Unexpected error while transmitting message to radio
	at com.rapplogic.droplet.framework.Droplet.sendToXBee(Droplet.java:691)
	at com.rapplogic.droplet.framework.Droplet.sendAlertToXBee(Droplet.java:603)
	at com.rapplogic.droplet.framework.Droplet.run(Droplet.java:351)
	at java.lang.Thread.run(Thread.java:613)
Caused by: com.rapplogic.droplet.framework.DeliveryException: Packet delivery failed due to error: ADDRESS_NOT_FOUND
	at com.rapplogic.droplet.framework.Droplet.sendToXBee(Droplet.java:682)
	... 3 more
	
 *	5/23 - noticing some yahoo api requests are timing out.  may switch to google rss
	 -->
	 
 * @author Andrew Rapp
 *
 * </pre>
 */
public class Droplet implements ServiceContext, Runnable {

	private final static Logger log = Logger.getLogger(Droplet.class);
	private HashMap<Integer, PullService> serviceMap = new HashMap<Integer, PullService>();
	
	// stat variables
	private final long appStartupTime = System.currentTimeMillis();
	private int deliveryFailures;
	private int pullTimeouts;
	private int pullMessages;
	private int pushAlerts;
	private int pullAppErrors;
	private int nextPrevPageHits;

	private final XBee xbee = new XBee();
	
	private BlockingQueue<Alert> alertQueue = new LinkedBlockingQueue<Alert>();
	private DelayQueue<DelayedAlert> delayedAlertQueue = new DelayQueue<DelayedAlert>();
	
	private ContentHistoryMap contentMap = new ContentHistoryMap();
	
	private long serviceTimeoutMillis = 6000;
	
	private int sendTimeout = 10000;

	private final ContentFormatter formatter = new ContentFormatter();

	private int pullServiceThreads = 3;
	private int pushServiceThreads = 2;
	
	/**
	 * Thread pool for servicing pull requests.  Handles timeout if service exceeds serviceTimeoutMillis
	 * TODO move creation after constructor and allow for num threads to be user-defined
	 */
	private ExecutorService pullServiceThreadPool = Executors.newFixedThreadPool(pullServiceThreads);
	
	private ScheduledThreadPoolExecutor pushServiceThreadPool = new ScheduledThreadPoolExecutor(pushServiceThreads);
	
	private Thread alertThread;
	private Thread delayedAlertThread;
	
	private boolean disableXBee;
	
	public Droplet() {
		alertThread = new Thread(this);
		alertThread.setName("Alert Thread");
		alertThread.start();		
		
		delayedAlertThread = new Thread(this);
		delayedAlertThread.setName("Delayed Alert Thread");
		delayedAlertThread.start();		
		
		formatter.setContinuationCharSize(1);
	}
	
	public void registerPullService(Integer serviceId, PullService service) {
		if (serviceMap.get(serviceId) != null) {
			throw new IllegalArgumentException("Service already exists for id " + serviceId + ".  Use unregisterService first");
		}
		
		serviceMap.put(serviceId, service);
	}
	
	public void unRegisterPullService(Integer serviceId) {
		if (serviceMap.get(serviceId) == null) {
			throw new IllegalArgumentException("Service does not exist for id " + serviceId);
		}
		
		serviceMap.remove(serviceId);
	}
	
	static abstract class PushServiceRunnable implements Runnable {
		
		PushService service;
		ServiceContext sc;
		long lastCall;
		int minTimeBetweenCalls = 100;
		
		PushServiceRunnable(ServiceContext sc, PushService service) {
			this.sc = sc;
			this.service = service;
		}

		protected abstract void executeService(ServiceContext sc, PushService service);
		
		public void run() {
			
			if (System.currentTimeMillis() - lastCall < minTimeBetweenCalls) {
				log.warn("Service [" + service + "] is attempting to run too soon after last execution.  Typically this occurs if the computer has been put in sleep/suspend mode, where the thread pool attempts to make up missed calls");
			} else {
				executeService(sc, service);
			}
			
			lastCall = System.currentTimeMillis();
		}
	}
	
	static class RealTimeAlertRunnable extends PushServiceRunnable {

		RealTimeAlertRunnable(ServiceContext sc, PushService service) {
			super(sc, service);
		}

		@Override
		protected void executeService(ServiceContext sc, PushService service) {
			try {
				log.debug("executing RealtimeAlertPushService: " + service);
				Alert alert = ((RealtimeAlertPushService)service).execute(sc);

				if (alert != null) {
					log.info("Adding alert to alert queue: " + alert);
					sc.sendAlert(alert);
				}
			} catch (Exception ex) {
				log.error("Error in RealtimeAlertPushService", ex);
			}
		}		
	}

	static class DelayedAlertRunnable extends PushServiceRunnable {

		DelayedAlertRunnable(ServiceContext sc, PushService service) {
			super(sc, service);
		}

		@Override
		protected void executeService(ServiceContext sc, PushService service) {
			
			try {
				log.debug("executing DelayedAlertPushService: " + service);
				((DelayedAlertPushService)service).execute(sc);
				
				// print queue
//				for (DelayedAlert alert : delayedAlertQueue) {
//					log.debug("delayedAlertQueue: " + alert);
//				}
			} catch (Exception ex) {
				log.error("Error in DelayedAlertPushService", ex);
			}
		}		
	}
	
	/**
	 * Registers a push service and returns the runnable wrapper object that 
	 * was added to the queue.
	 */
	public Runnable registerPushService(final PushService service) {
		
		Runnable runnable;
		
		if (service instanceof RealtimeAlertPushService) {
			// package service execution in a Runnable and schedule
			runnable = new RealTimeAlertRunnable(this, service);
		} else if (service instanceof DelayedAlertPushService) {
			runnable = new DelayedAlertRunnable(this, service);
		} else if (service instanceof RunnablePushService) {
			runnable = (Runnable) service;
		} else {
			throw new IllegalArgumentException("Unsupported Push Service: " + service.getClass().getName());
		}

		Future<?> sf;
		
		if (service instanceof RecurringService) {
			RecurringService rs = (RecurringService) service;
			
			if (rs.getType() == RecurringService.RecurringType.FIXED_DELAY) {
				 sf = pushServiceThreadPool.scheduleAtFixedRate(runnable, rs.getInitialDelay(), rs.getDelay(), TimeUnit.MILLISECONDS);
			} else {
				sf = pushServiceThreadPool.scheduleAtFixedRate(runnable, rs.getInitialDelay(), rs.getDelay(), TimeUnit.MILLISECONDS);			
			}
		} else if (service instanceof OneTimeService) {
			sf = pushServiceThreadPool.schedule(runnable, ((OneTimeService)service).getInitialDelay(),TimeUnit.MILLISECONDS);
		} else if (service instanceof RunnablePushService) {
			// increment threadpool size for this service
			pushServiceThreadPool.setCorePoolSize(pushServiceThreadPool.getPoolSize() + 1);
			((RunnablePushService) service).setServiceContext(this);
			sf = pushServiceThreadPool.submit((Runnable)service);
		} else {
			throw new IllegalArgumentException("Unsupported service: " + service.getClass().getName());
		}
		
		// if process runs for > delay, service will not execute second until first finishes but executes second as soon as first finishes
		
		log.info("Registering push service: " + service);
		
		return runnable;
	}
	
	public boolean unRegisterPushService(Runnable runnable) {
		return pushServiceThreadPool.remove(runnable);
	}
	
	/**
	 * Removes Alerts from the alert queue and sends to LCD
	 */
	public void run() {
		//TODO if request was processed within last 30 seconds, wait then send to give user time to read request, unless this is an emergency!!
		
		if (!(Thread.currentThread() == alertThread || Thread.currentThread() == delayedAlertThread)) {
			throw new RuntimeException("Only alert threads can call this method");
		}
		
		while (true) {
			log.info("Thread in loop ");
			
			try {
				if (Thread.currentThread() == alertThread) {
					// Thread blocks here until an alert is available
					Alert alert = alertQueue.take();	
					log.info("Alert retrieved from queue " + alert);
					
					this.sendAlertToXBee(alert);
				} else if (Thread.currentThread() == delayedAlertThread) {
					// Thread blocks here until an alert is available
					log.debug("delay thread waiting for alert");
					DelayedAlert alert = delayedAlertQueue.take();
					log.info("Delay Alert has emerged from queue " + alert);
					
					// check if there are other alerts with the same time and destination address; if so merge content
					int count = 0;
					
					synchronized (delayedAlertQueue) {
						for (DelayedAlert inQueue : delayedAlertQueue) {
							if (inQueue.getExpectedFireDate().getTime() == alert.getExpectedFireDate().getTime()) {
								log.info("found delayAlert with same time.. will check xbee address after take: " + inQueue);
								count++;
							}
						}
						
						// TODO TESTME alerts with same time but different XBee address must not get merged!
						for (int i = 0; i < count; i++) {
							// this should NOT block since it has the same targetDate
							log.info("Expecting a immediate poll from delay queue to merge another alert that fires at the same time");
							DelayedAlert simultaneousAlert = delayedAlertQueue.poll();
							
							if (simultaneousAlert == null) {
								log.error("queue.poll returned null.  Was expecting a immediate poll from delay queue to merge another alert that fires at the same time");
								continue;
							}
							
							if (alert.getRemoteXBeeAddress().equals(simultaneousAlert.getRemoteXBeeAddress())) {
								log.info("Merging content with first alert: " + simultaneousAlert.getContent().getPages());	
								
								// merge content
								alert.getContent().getPages().addAll(simultaneousAlert.getContent().getPages());
							} else {
								log.debug("Found simultaneous alert but with XBee address, stuffing back in queue");
								// TODO verify this will not be retrieved in next poll call in this loop.  if so may need to add one second to the delay
								this.delayedAlertQueue.offer(simultaneousAlert);
							}
						}						
					}

					this.sendAlertToXBee(alert);
				}
			} catch (InterruptedException e) {
				log.warn("Thread interrupted.. exiting");
				return;
			} catch (Exception e) {
				log.error("exception in alert thread", e);
			}
		}
	}	
	
	/**
	 * Initiates the connection to the local XBee radio
	 * 
	 * @param comPort
	 * @param baudRate
	 * @throws XBeeException
	 */
	final public void initXBee(String comPort, int baudRate) throws XBeeException {		
		if (!this.disableXBee) {
			xbee.open(comPort, baudRate);	
		} else {
			log.warn("XBee is disabled");
		}
	}
	
	/**
	 * <pre>
	 * Waits for requests from remote XBee, delegates the request to a Pull Service implementation
	 * and returns a response.
	 * The first byte of the RX packet determines the service id (e.g. weather, news)
	 * This is the integer that the service was registered with (registerPullService(Integer serviceId..)
	 * The second byte contains status information.  The first bit indicates if this is a next page 
	 * request (1 = next page, 0 = n/a) 
	 * 
	 * The payload of the outgoing request is structured as follows:
	 * byte [0]: bit 1 if a next page is available, bit 2 if the led should flash, bit 3 if the buzzer should activate
	 * byte [1-n] character data for display on LCD
	 * </pre>
	 */
	final public void start() {
		
		if (this.isDisableXBee()) {
			return;
		}
		
		try {			
			while (true) {

				try {
					// we wait here until a packet is received.
					final XBeeResponse remoteRequest = xbee.getResponse();
					
					if (remoteRequest.getApiId() == ApiId.ZNET_RX_RESPONSE) {
						
						log.info("Received RX packet from Arduino: " + remoteRequest);
						
						// we received a packet from ZNetSenderTest.java
						ZNetRxResponse rx = (ZNetRxResponse) remoteRequest;
						
						final int serviceId = rx.getData()[0];
						
						// check if pagination request
						final boolean nextPageRequest = ByteUtils.getBit(rx.getData()[1], 1);
						final boolean previousPageRequest = ByteUtils.getBit(rx.getData()[1], 2);

						log.debug("Service id is " + serviceId + ", nextPageRequest is " + nextPageRequest + ", previousPageRequest is " + previousPageRequest + ", pagination byte is " + rx.getData()[1]);
													
						// container for the response
						Message message = new Message();
						message.setRemoteXBeeAddress(rx.getRemoteAddress64());
						
						if (nextPageRequest || previousPageRequest) {
							nextPrevPageHits++;
							paginationRequest(message, nextPageRequest);
						} else {
							pullMessages++;
							// exec service
							executeService(serviceId, remoteRequest, message);									
						}
						
						try {
							this.sendToXBee(message);	
						} catch (DeliveryException e) {
							// FAIL!
							deliveryFailures++;
							// this is warn not error because well wireless is not 100% reliable
							log.warn("Failed to return pull-service response to radio", e);
						}
					} else if (remoteRequest.getApiId() == ApiId.ZNET_TX_STATUS_RESPONSE) {
						// ignore
						log.debug("ignoring tx status from synchronous send " + remoteRequest);
					} else {
						// might you have other radios on same PAN ID/channel?
						log.warn("Received unexpected packet " + remoteRequest);
					}
				} catch (Exception e) {
					log.error("Error in main-thread top-level loop!", e);
				}
			}			
		} finally {
			xbee.close();	
		}
	}

	private void paginationRequest(Message message, boolean nextPageRequest) {
		// pagination request
		
		ContentHistory history = contentMap.findByAddress(message.getRemoteXBeeAddress());
		
		// already in history.. don't store again
		message.setStoreInHistory(false);
		
		if (history.isEmpty()) {
			// we probably restarted the app after the first request
			message.setContent(formatter.format("Sorry, but I lost your content.  Please request service from menu"));
		} else {
			
			try {
				if (nextPageRequest) {
					if (history.nextPageExists()) {
						history.setIndexToNextPage();
						message.setContent(history.getCurrent());
					}
				} else {
					if (history.previousPageExists()) {
						history.setIndexToPreviousPage();
						message.setContent(history.getCurrent());
					}
				}				
			} catch (PageNotFoundException e) {
				message.setContent(formatter.format(e.getMessage()));
			}

			
			if (message.getContent() == null) {
				message.setContent(formatter.format("Page does not exist"));
			}
		}
	}
	
	private void executeService(final int serviceId, final XBeeResponse response, Message message) {
		// find service
		final PullService service = serviceMap.get(serviceId);
		final ServiceContext sc = this;
		
		if (service == null) {
			message.setContent(formatter.format("Service not defined for id: " + serviceId));
			message.getContent().setErrorMessage(true);
		} else {
			// The service is handled in a separate thread so that we can support a timeout
			// if the request takes too long to process (i.e. network delay)
			Future<IContent> future = pullServiceThreadPool.submit(new Callable<IContent>() {
				public IContent call() {
					try {
						return service.execute(serviceId, response, sc);
					} catch (Exception e) {
						pullAppErrors++;
						log.error("Exception in service request", e);
						// display app error on LCD
						IContent content = formatter.format("Application error: " + e.getMessage());
						content.setErrorMessage(true);
						return content;
					}		
				}
			});
			
			try {
				//for testing only: result = future.get(5, TimeUnit.SECONDS);
				// this will throw a timeout exception if the request takes longer than the service timeout
				IContent content = future.get(this.getPullServiceTimeoutMillis(), TimeUnit.MILLISECONDS);
				
				if (content == null) {
					content = formatter.format("Service did not return anything");
				}
				
				message.setContent(content);
				
				log.debug("Service returned " + content);
				
				// return first page
//				result = content.getFirstPage().getLcdFormattedText();
				
//				if (content.getPages().size() > 1) {
//					// set continuation bit
//					multiplePages = true;
//				}											
			} catch (ExecutionException e) {
				log.error("Exception in callable", e);
//				result = formatter.getFirstPage("Application Failure").getLcdFormattedText();
				message.setContent(formatter.format("Application Failure"));
				message.getContent().setErrorMessage(true);
			} catch (TimeoutException e) {
				pullTimeouts++;
				log.warn("service type [" + serviceId + "] timeout.. cancelling request ");
				future.cancel(true);
//				result = formatter.getFirstPage("Application Timeout").getLcdFormattedText();
				message.setContent(formatter.format("Application Timeout"));
				message.getContent().setErrorMessage(true);
			} catch (Exception e) {
//				result = formatter.getFirstPage("Unexpected error: " + e.getMessage()).getLcdFormattedText();
				message.setContent(formatter.format("Unexpected error: " + e.getMessage()));
				message.getContent().setErrorMessage(true);
			}
		}
	}
	
	private void sendAlertToXBee(Alert alert) {
		try {
			pushAlerts++;
			this.sendToXBee(alert);
		} catch (DeliveryException e) {
			deliveryFailures++;
			log.warn("Failed to send alert. remote may be offline: " + alert, e);
			
			try {
				alert.handleError(e);	
			} catch (Exception f) {}						
		}
	}
	
	/**
	 * Sends a string to the XBee for display on LCD
	 * 
	 * TODO return delivery status
	 * 
	 * @param message
	 * @throws XBeeException
	 */
	private synchronized void sendToXBee(Message message) {
		try {
			//log.debug("sendToXBee(): multiplePage is " + multiplePages + ", alertLed is " + alertLed + ", buzzer is " + buzzer + ", message is \n" + message);
			log.debug("sendToXBee(): " + message.getContent().getCurrentPage().getLcdFormattedText());
			
			// kind of hacky but need to sneak an extra byte in there for the continuation/alert led bitset
			int[] payload = ByteUtils.stringToIntArray("0" + message.getContent().getCurrentPage().getLcdFormattedText());
			
			// reset status byte
			payload[0] = 0;
			
			if (message.isStoreInHistory() && !message.getContent().isErrorMessage()) {
				// store in hash with remote address and service id as key
				// next page won't work if content is not stored!
				contentMap.store(message.getRemoteXBeeAddress(), message.getContent());				
			}

			ContentHistory history = contentMap.findByAddress(message.getRemoteXBeeAddress());
			
			// TODO problem is if message isn't stored in history, next page will always be false.  next page should always depend
			// on current content only!
			if (history.nextPageExists()) {
				// set next page bit
				payload[0] = payload[0] | 1;
			}
			
			if (message instanceof Alert && ((Alert)message).isFlashLed()) {
				// set flash alert led bit
				payload[0] = payload[0] | (1 << 1);
			}
			
			if (message instanceof Alert && ((Alert)message).isSoundAlarm()) {
				// turn on buzzer bit
				payload[0] = payload[0] | (1 << 2);
			}
			
			if (history.previousPageExists()) {
				// set previous page bit
				payload[0] = payload[0] | (1 << 3);
			}
			
			// build a request with the response data
			ZNetTxRequest request = new ZNetTxRequest(message.getRemoteXBeeAddress(), payload);
			
			log.info("Sending packet to Arduino: " + request);

			if (!this.isDisableXBee()) {
				ZNetTxStatusResponse response = (ZNetTxStatusResponse) xbee.sendSynchronous(request, sendTimeout);
				// FIXME problem here is the main thread is already waiting on getResponse so it gets it first 
				// and this thread is is left waiting, maybe for ever.
				//xbee.getResponse();
				
				// update frame id for next request
				request.setFrameId(xbee.getNextFrameId());
				
				log.info("Received TX Status Response " + response);

				if (!response.isSuccess()) {
					// packet failed.  log error
					// it's easy to create this error by unplugging/powering off your remote xbee.  when doing so I get: packet failed due to error: ADDRESS_NOT_FOUND  
					throw new DeliveryException("Packet delivery failed due to error: " + response.getDeliveryStatus());
				}				
			} else {
				log.warn("XBee is disabled.  Message was not sent");
			}
		} catch (XBeeTimeoutException e) {
			// should never timeout since we allowed 10 seconds to send and local XBee delivers the response packet
			throw new DeliveryException("Unable to send response to LCD.  Timeout after " + sendTimeout + " milliseconds");
		} catch (Exception e) {
			throw new DeliveryException("Unexpected error while transmitting message to radio", e);
		}
	}
	
	/**
	 * Sends content directly to LCD.
	 * If a response is in progress, it will be sent after a delay;
	 * otherwise it will go immediately.
	 * 
	 * This alert is processed asynchronously, so you must override handleError to receive errors
	 * 
	 * TODO create priorites (DEFAULT, EMERGENCY), where emergency is sent immediately.
	 */
	public void sendAlert(Alert alert) {
		alertQueue.offer(alert);
	}

	/**
	 * Schedules an alert to be sent to the LCD at a specified time
	 * in the future, or now if delay is 0
	 * 
	 * @param da
	 */
	public void sendDelayedAlert(DelayedAlert alert) {
		this.delayedAlertQueue.offer(alert);
	}
	
	/**
	 * The pull service must respond within this amount of time or a timeout will occur 
	 */
	public long getPullServiceTimeoutMillis() {
		return serviceTimeoutMillis;
	}

	public void setPullServiceTimeoutMillis(long serviceTimeoutMillis) {
		if (serviceTimeoutMillis <= 0) {
			throw new IllegalArgumentException("must be > 0");
		}
		
		this.serviceTimeoutMillis = serviceTimeoutMillis;
	}
	
	/**
	 * How long in milliseconds we wait after sending a packet to get an ACK before timing out
	 */
	public int getXBeeSendTimeout() {
		return sendTimeout;
	}

	public void setXBeeSendTimeout(int sendTimeout) {
		if (sendTimeout <= 0) {
			throw new IllegalArgumentException("must be > 0");
		}
		
		this.sendTimeout = sendTimeout;
	}
	
	public ContentFormatter getFormatter() {
		return formatter;
	}

	public boolean isDisableXBee() {
		return disableXBee;
	}

	public void setDisableXBee(boolean disableXBee) {
		this.disableXBee = disableXBee;
	}
	
	public BlockingQueue<Alert> getAlertQueue() {
		return alertQueue;
	}

	public DelayQueue<DelayedAlert> getDelayedAlertQueue() {
		return delayedAlertQueue;
	}

	public void shutdown() {
		
		log.info("this is shutdown");
		
		try {
			xbee.close();
		} catch (Exception e) {
			log.warn("XBee service failed to shutdown");
		}
		
		try {
			this.pullServiceThreadPool.shutdown();
		} catch (Exception e) {
			log.warn("Pull service failed to shutdown");
		}
		
		try {
			this.pushServiceThreadPool.shutdown();
		} catch (Exception e) {
			log.warn("Push service failed to shutdown");
		}
		
		try {
			this.alertThread.interrupt();
		} catch (Exception e) {
			log.warn("Alert thread failed to shutdown");
		}
		
		try {
			this.delayedAlertThread.interrupt();
		} catch (Exception e) {
			log.warn("Delayed alert thread failed to shutdown");
		}
	}
	
	// stats
	
	public long getAppStartupTime() {
		return appStartupTime;
	}

	public int getPullMessages() {
		return pullMessages;
	}

	public int getPushAlerts() {
		return pushAlerts;
	}

	public int getPullAppErrors() {
		return pullAppErrors;
	}

	public int getDeliveryFailures() {
		return deliveryFailures;
	}

	public int getPullTimeouts() {
		return pullTimeouts;
	}

	public int getNextPrevPageHits() {
		return nextPrevPageHits;
	}
}