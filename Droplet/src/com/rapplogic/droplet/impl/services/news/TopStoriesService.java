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

package com.rapplogic.droplet.impl.services.news;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.text.Content;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * Retrieves top stories from Yahoo RSS for display on a remote XBee LCD
 * 
 * @author andrew
 *
 */
public class TopStoriesService implements PullService {

	private final static Logger log = Logger.getLogger(TopStoriesService.class);
	
	public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();

		// top stories
		// google news is returning a 403 booooo.  we'll try yahoo instead
		//InputSource inputSource = new InputSource(this.getRss("http://news.google.com/news?pz=1&ned=us&hl=en&output=rss"));
		// yahoo news 
		InputSource inputSource = new InputSource(this.getRss("http://rss.news.yahoo.com/rss/topstories"));
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder builder = dbFactory.newDocumentBuilder();	
		
		Document doc = builder.parse(inputSource);
		
		NodeList nodeList;
		
		StringBuilder sb = new StringBuilder();
		
		// get item titles
		nodeList = (NodeList) xPath.evaluate("//item/title", doc, XPathConstants.NODESET);
		
		Content news = new Content();
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String title = node.getFirstChild().getTextContent().replaceAll("\n", "").replaceAll("\r", "");
			log.debug("adding title is " + title);
			news.getPages().addAll(serviceContext.getFormatter().format(title).getPages());
			//sb.append(title);
		}
		
		return news;
	}
	
	private InputStream getRss(String urlStr) throws FileNotFoundException {
		
		try {
			URL url = new URL(urlStr);
	        URLConnection conn = url.openConnection();
	        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
	      
	        return in;
		} catch (Exception e) {
			throw new RuntimeException("Rss fetch failed", e);
		}
	}
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		TopStoriesService service = new TopStoriesService();
	}
}
