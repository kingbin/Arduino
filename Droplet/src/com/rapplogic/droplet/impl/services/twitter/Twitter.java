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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.ConnectException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A Basic Twitter API for searching and accessing the friends_timeline.
 * <p/>
 * dependencies: httpclient 1.3 which requires commons-logging and commons-codec
 * 
 * @author andrew
 *
 */
public class Twitter {

	private final static Logger log = Logger.getLogger(Twitter.class);
	
	final static String friendsTimelineUrl = "/statuses/friends_timeline.xml";
	final static String userTimelineUrl = "/statuses/user_timeline.xml";
	
	// twitter date format
	private final DateFormat twitterXmlDateFormat = new SimpleDateFormat("EEE MMM d H:m:s Z yyyy");
	//Fri, 19 Jun 2009 18:51:20 +0000
	private final DateFormat twitterJsonDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	
	private final String twitterHost = "www.twitter.com";

	private String username;
	private String password;

	private DocumentBuilder builder;
	private XPath xPath;
	
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

	public Twitter() throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		builder = dbFactory.newDocumentBuilder();		
		
		XPathFactory factory = XPathFactory.newInstance();
		xPath = factory.newXPath();
	}

	private String fetchTwitterXml(String twitterRestUrl, boolean authenticate) {
		
		GetMethod get = null;
		
		try {
			get = new GetMethod(twitterRestUrl);

			HttpClient client = new HttpClient();

			if (authenticate) {
				Credentials credentials = new UsernamePasswordCredentials(this.getUsername(), this.getPassword());
				client.getState().setCredentials(new AuthScope(twitterHost, 80, AuthScope.ANY_REALM), credentials);				
			}

			HostConfiguration host = client.getHostConfiguration();
			host.setHost(new URI("http://" + twitterHost, true));

		    int statusCode = 0;
		    
		    try {
		    	statusCode = client.executeMethod(get); 
		    } catch (ConnectException ce) {
		    	throw new TwitterException("Connect Exception: " + ce.getMessage());
		    }

//		    log.debug("twitter return status code " + statusCode);
		    
		    if (statusCode != HttpStatus.SC_OK) {
		    	// when you exceed your rate limit
//		    	Status code: 400, repsonse: <?xml version="1.0" encoding="UTF-8"?>
//			    <hash>
//			      <request>/statuses/friends_timeline.xml</request>
//			      <error>Rate limit exceeded. Clients may not make more than 100 requests per hour.</error>
//			    </hash>
		    	throw new TwitterException("Request was unsuccessful.  Status code: " + statusCode + ", repsonse: " + get.getResponseBodyAsString());
		    }
		    
		    String body = get.getResponseBodyAsString();
		    
		    // twitter sends a html response when over capacity. like this:
//	          <span style="font-size:1.8em; font-weight:bold">Twitter is over capacity.</span><br />
//	          <div style="font-size:1.2em;margin-top:2px;color:#b6b6a3">Too many tweets! Please wait a moment and try again.</div><br />
		    if (body.indexOf("<html") > -1 && body.indexOf("Twitter is over capacity") > -1) {
		    	throw new TwitterException("Twitter over capacity");
		    }
		      
			String s = get.getResponseBodyAsString();
			
			// save for test/debug
//			FileWriter writer = new FileWriter("friends_timeline.xml");
//			writer.write(s);
//			writer.close();
			
			return s;
		} catch (TwitterException e) {
			throw e;
		} catch (Exception e) {
			throw new TwitterException("Error retrieving XML from twitter: " + e.getMessage());
		} finally {
			try {
				get.releaseConnection();
			} catch (Exception e) {} // don't particularly care
		}
	}
	
	private Document getFriendsTimeLineDom(String body) throws SAXException, IOException {
//		log.debug("twitter body: " + body);
		
		ByteArrayInputStream bin = new ByteArrayInputStream(body.getBytes("UTF-8"));

		InputSource inputSource = new InputSource(bin);
		
		// put results in Document node so we can run queries on it
		Document doc = builder.parse(inputSource);
		
		return doc;		
	}

	private String readFileAsString(String file) throws IOException {
	
		//BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		
		FileInputStream fin = new FileInputStream(file);
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		byte[] buf = new byte[1024];
		
		int len;
		
		while ((len = fin.read(buf)) > 0) {
			bout.write(buf, 0, len);
		}
		
		String s = new String(bout.toByteArray(), "UTF-8");

		return s;
	}
	
	/**
	 * Returns tweets in the friends timeline
	 * 
	 * @return
	 */
	public List<Tweet> parseTweets(String body) {
		try {
//			FileInputStream bin = new FileInputStream("friends_timeline");
			
//			String body = this.getFriendsTimeLineXml();
//			String body = this.readFileAsString("friends_timeline.xml");
			
			log.debug("body is " + body);
			
			Document doc = getFriendsTimeLineDom(body);
			
			// query all statuses. isn't xpath wonderful?
			NodeList nodeList = (NodeList) xPath.evaluate("//status", doc, XPathConstants.NODESET);
			
			List<Tweet> tweets = new ArrayList<Tweet>();
			
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				
				Tweet tweet = new Tweet();
				
				tweet.setCreatedAt(twitterXmlDateFormat.parse(xPath.evaluate("created_at", node)));
				tweet.setMessage(xPath.evaluate("text", node));
				tweet.setUser(xPath.evaluate("user/screen_name", node));
				
				log.debug("tweet is " + tweet);
				
				tweets.add(tweet);
			}
			
			return tweets;
		} catch (TwitterException e) {
			throw e;
		} catch (Exception e) {
			throw new TwitterException("Unexpected twitter service failure", e);
		}		
	}
	
	public List<Tweet> getUserTimeLineXml(String screenName) {
		String body = this.fetchTwitterXml(userTimelineUrl + "?screen_name=" + screenName, true);
		return this.parseTweets(body);
	}
	

	public List<Tweet> getFriendsTimeline() {
		return this.parseTweets(this.fetchTwitterXml(friendsTimelineUrl, true));
	}
	
	/**
	 * Returns the most recent Tweet from a users's friends_timeline
	 * 
	 * @return
	 */
	public Tweet getFriendsTimelineLastTweet() {
		
		try {		
			String body = this.fetchTwitterXml(friendsTimelineUrl, true);
			
			Document doc = getFriendsTimeLineDom(body);

			// TODO if count(//status) == 0, return null
			
			// get the first status. isn't xpath wonderful?
			String createdAt = (String) xPath.evaluate("//status[1]/created_at", doc);
			
			if (createdAt.length() == 0) {
				throw new TwitterException("//status[1]/created_at missing from twitter response: " + body);
			}
			
			String text = (String) xPath.evaluate("//status[1]/text", doc);
			String user = (String) xPath.evaluate("//status[1]/user/screen_name", doc);
			
			Tweet tweet = new Tweet();
			
			tweet.setCreatedAt(twitterXmlDateFormat.parse(createdAt));
			tweet.setUser(user);
			tweet.setMessage(text);			

			return tweet;
		} catch (TwitterException e) {
			throw e;
		} catch (Exception e) {
			throw new TwitterException("booo failed", e);
		}
	}
	
	/**
	 * Searches the public timeline for the specified search term
	 * Uses the Jackson JSON parser
	 * 
	 * @param searchTerm
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws ParseException
	 */
	public List<Tweet> search(String searchTerm) throws JsonParseException, IOException, ParseException {
		String json = this.fetchTwitterXml("http://search.twitter.com/search.json?q=" + searchTerm, false);
		log.debug("search returned " + json);

		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(json);

		List<Tweet> tweets = new ArrayList<Tweet>();

		JsonToken token = jp.nextToken();
		Tweet currentTweet = null;
		int level = 0;
		
		do {
			String currentName = jp.getCurrentName();

			//log.debug("token is " + token + ", currentName is " + currentName + ", level is " + level);

			if (token == JsonToken.START_OBJECT) {
				level++;
				
				if (level == 2) {
					currentTweet = new Tweet();	
				}
			} else if (token == JsonToken.END_OBJECT) {

				if (level == 2) {
					log.debug("parsed tweet" + currentTweet);
					tweets.add(currentTweet);					
				}
				
				level--;
			}

			if (level == 2 && jp.getCurrentToken() == JsonToken.VALUE_STRING) {
				if (currentName.equals("text")) {
					currentTweet.setMessage(jp.getText());
				} else if (currentName.equals("from_user")) {
					currentTweet.setUser(jp.getText());
				} else if (currentName.equals("created_at")) {
					currentTweet.setCreatedAt(twitterJsonDateFormat.parse(jp.getText()));
				} else {
//					log.debug("ignoring " + currentName);
				}
			}

		} while ((token = jp.nextToken()) != null);

		return tweets;
	}
	
	public static class TwitterException extends RuntimeException {
		public TwitterException(String s, Exception e) {
			super(s, e);
		}
		
		public TwitterException(String s) {
			super(s);
		}
	}
	
	public static void main(String args[]) throws ParserConfigurationException, JsonParseException, IOException, ParseException {
		PropertyConfigurator.configure("log4j.properties");
		
		Twitter twitter = new Twitter();
		twitter.search("woot");
	}
}
