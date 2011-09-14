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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides current weather by zipcode via the Yahoo Weather RSS
 * 
 * @author Andrew Rapp
 *
 */
public class YahooWeather {

	private final static Logger log = Logger.getLogger(YahooWeather.class);
		
	private DocumentBuilder builder;
	
	// date format classes for parsing/formatting text dates
	private static DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy h:mm aaa z");
	private static DateFormat timeDf = new SimpleDateFormat("h:mm a");
	private static DateFormat dayDf = new SimpleDateFormat("EEE");

	private String zipcode = "94301";
	
	public YahooWeather() throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		builder = dbFactory.newDocumentBuilder();		
	}	
	
	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}
	
	/**
	 * Parses Yahoo weather RSS with XPath and packages into a Weather object
	 * 
	 * @return
	 * @throws XPathExpressionException
	 * @throws RssFetchException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParseException
	 * Apr 10, 2009
	 */
	public Weather getWeather(boolean fahrenheit) throws XPathExpressionException, RssFetchException, SAXException, IOException, ParseException {

		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		
//		log.debug("getting weather");
		InputSource inputSource = new InputSource(this.getWeatherRss(this.getZipcode(), fahrenheit));
//		log.debug("got weather");
		
		// put results in Document node so we can run queries on it
		Document doc = builder.parse(inputSource);
		
//		log.debug("parsed weather into document");
		
		Namespace ns = new Namespace();
		// we need to set the namespace for XPath to work
		ns.setNamespaceUri("http://xml.weather.yahoo.com/ns/rss/1.0");
		ns.setPrefix("yweather");
		// must set namespace for xpath to function
		xPath.setNamespaceContext(ns);
		
		NodeList nodeList;
		
		//<yweather:units temperature="F" distance="mi" pressure="in" speed="mph"/>
		nodeList = (NodeList) xPath.evaluate("//yweather:units", doc, XPathConstants.NODESET);
		
		Weather weather = new Weather();

//		<yweather:units temperature="F" distance="mi" pressure="in" speed="mph"/>
		weather.setTemperatureUnit(this.getAttribute("temperature", nodeList.item(0)));
		weather.setSpeedUnit(this.getAttribute("speed", nodeList.item(0)));
		weather.setPressureUnit(this.getAttribute("pressure", nodeList.item(0)));
		weather.setDistanceUnit(this.getAttribute("distance", nodeList.item(0)));
		
		// there should only be one of these ever
		//<yweather:condition  text="Fair"  code="34"  temp="70"  date="Sun, 05 Apr 2009 7:20 pm EDT" />
		// codes: http://developer.yahoo.com/weather/#codes
		nodeList = (NodeList) xPath.evaluate("//yweather:condition", doc, XPathConstants.NODESET);
		weather.setText(this.getAttribute("text", nodeList.item(0)));
		weather.setTemperature(Integer.parseInt(this.getAttribute("temp", nodeList.item(0))));
		// parse the date
		Date date = df.parse(this.getAttribute("date", nodeList.item(0)));
		weather.setDate(date);
		weather.setCode(Integer.parseInt(this.getAttribute("code", nodeList.item(0))));
	
		// get humidity
//		<yweather:atmosphere humidity="37"  visibility="10"  pressure="29.65"  rising="0" />
		nodeList = (NodeList) xPath.evaluate("//yweather:atmosphere", doc, XPathConstants.NODESET);		
		weather.setHumidity(Integer.parseInt(getAttribute("humidity", nodeList.item(0))));
		weather.setPressure(getAttribute("pressure", nodeList.item(0)));
		
		// get wind
//		<yweather:wind chill="63"   direction="110"   speed="5" />
		nodeList = (NodeList) xPath.evaluate("//yweather:wind", doc, XPathConstants.NODESET);
		weather.setWindSpeed(Integer.parseInt(getAttribute("speed", nodeList.item(0))));
		weather.setWindChill(Integer.parseInt(getAttribute("chill", nodeList.item(0))));
		weather.setWindDirection(Integer.parseInt(getAttribute("direction", nodeList.item(0))));
		// still more info we could display
//		<yweather:astronomy sunrise="6:48 am"   sunset="7:38 pm"/>

		// TODO abbreviations for codes e.g. thunderstorm with tstorm
		//<yweather:forecast day="Sun" date="5 Apr 2009" low="52" high="68" text="Thunderstorms/Wind Late" code="4" />
		nodeList = (NodeList) xPath.evaluate("//yweather:forecast", doc, XPathConstants.NODESET);
		
		// yahoo seems to send two forecasts, with the first being todays
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
	
			Forecast forecast = new Forecast();
			
			forecast.setDay(getAttribute("day", node));
			forecast.setHigh(getAttribute("high", node));
			forecast.setLow(getAttribute("low", node));
			forecast.setText(getAttribute("text", node));
			forecast.setCode(Integer.parseInt(getAttribute("code", node)));
			
//			forecast.setCode(Integer.parseInt(getAttribute("code", node)));
			
			if (dayDf.format(weather.getDate()).equals(forecast.getDay())) {
				weather.setTodaysForecast(forecast);
			}
			
			//weather.getForecastList().put(forecast.getDay(), forecast);
			weather.getForecastList().add(forecast);
		}
		
		return weather;

	}
	
	private String getAttribute(String attr, Node node) {
		return node.getAttributes().getNamedItem(attr).getNodeValue();
	}
	
	/**
	 * Returns an input stream to Yahoo weather RSS
	 * 
	 * @return
	 * @throws RssFetchException
	 * @throws FileNotFoundException
	 */
	private InputStream getWeatherRss(String zip, boolean fahrenheit) throws RssFetchException, FileNotFoundException {

		// for testing
		//return new FileInputStream("weather.rss");
		
		try {
			URL url = new URL("http://weather.yahooapis.com/forecastrss?p=" + zip + "&u=" + (fahrenheit ? "f" : "c"));
	        URLConnection conn = url.openConnection();
	        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
	      
	        return in;
		} catch (Exception e) {
			throw new RssFetchException("Rss fetch failed", e);
		}
	}
	
	/**
	 * Contains current weather conditions and all available Forecasts
	 * 
	 * @author andrew
	 *
	 */
	static class Weather {
		String temperatureUnit;
		String speedUnit;
		String pressureUnit;
		String distanceUnit;
		
		int code;
		int temperature;
		String text;
		Date date;
		int humidity;
		int windSpeed;
		int windChill;
		int windDirection;
		String pressure;
		final List<Forecast> forecastList = new ArrayList<Forecast>();
		
		Forecast todaysForecast;

		public int getWindDirection() {
			return windDirection;
		}

		public void setWindDirection(int windDirection) {
			this.windDirection = windDirection;
		}
		
		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		/**
		 * Returns a compact string representation of current weather and forecast
		 * for display on an 80 char LCD
		 * 
		 * TODO remove wind info if size exceeds 80 
		 */
		public String current() {
			StringBuilder sb = new StringBuilder();
			
			sb.append(this.getText());
			sb.append(",");
			sb.append(this.getTemperature());
			sb.append(this.getTemperatureUnit());

			sb.append(",RH:");
			sb.append(this.getHumidity());		
			sb.append("%");
			
			// TODO add wind if windy or low wind chill
//			if (Integer.parseInt(this.getWindSpeed()) >= 15 || (Integer.parseInt(this.getTemperature()) - Integer.parseInt(this.getWindChill())) > 12) {
			
			if (this.getWindSpeed() == 0) {
				sb.append("Calm");
			} else {
				sb.append(",wind:");
				sb.append(this.getWindSpeed());		
				sb.append(this.getSpeedUnit());
				sb.append(",direction:");
				sb.append(this.getWindDirection());
				
				// somewhat arbitrary
				if (Math.abs(this.getWindChill() - this.getTemperature()) > 10) {
					sb.append(",chill:");
					sb.append(this.getWindChill());
					sb.append(this.getTemperatureUnit());	
				}
			}

			sb.append(",pressure:");
			sb.append(this.getPressure());
			sb.append("(");
			sb.append(this.getPressureUnit());
			sb.append(")");
			
			sb.append("@");
			sb.append(timeDf.format(this.getDate()).replaceAll("\\s", ""));
			
			return sb.toString();
		}
		
		public String forecast() {
			StringBuilder sb = new StringBuilder();
			
			int i = 0;
			
			for (Forecast forecast : this.getForecastList()) {
				if (i++ > 0) {
					sb.append(";");
				}
				
				sb.append(forecast.getDay());
				sb.append(":");
				// save some lcd real estate
				sb.append(forecast.getText().replaceAll("Thunderstorms", "T'Storms"));
				sb.append(",");
				sb.append(forecast.getLow());
				sb.append(this.getTemperatureUnit());
				sb.append("-");
				sb.append(forecast.getHigh());
				sb.append(this.getTemperatureUnit());
			}
			
			sb.append("@");
			sb.append(timeDf.format(this.getDate()).replaceAll("\\s", ""));
			
			return sb.toString();
		}
		
		public boolean isFahrenheit() {
			return "F".equals(this.temperatureUnit);
		}
		
		public String getTemperatureUnit() {
			return temperatureUnit;
		}
		public void setTemperatureUnit(String temperatureUnit) {
			this.temperatureUnit = temperatureUnit;
		}
		public String getSpeedUnit() {
			return speedUnit;
		}
		public void setSpeedUnit(String speedUnit) {
			this.speedUnit = speedUnit;
		}
		public String getPressureUnit() {
			return pressureUnit;
		}
		public void setPressureUnit(String pressureUnit) {
			this.pressureUnit = pressureUnit;
		}
		public String getDistanceUnit() {
			return distanceUnit;
		}
		public void setDistanceUnit(String distanceUnit) {
			this.distanceUnit = distanceUnit;
		}

		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		public List<Forecast> getForecastList() {
			return forecastList;
		}

		/**
		 * Returns the forecast for today, if it exists.
		 * Where "today" is defined by the date sent with the RSS.  
		 * This date usually lags 10-30 minutes behind current time,
		 * so "today" could be yesterday, but just briefly
		 */
		public Forecast getTodaysForecast() {
			return todaysForecast;
		}

		public void setTodaysForecast(Forecast todaysForecast) {
			this.todaysForecast = todaysForecast;
		}

		public int getTemperature() {
			return temperature;
		}

		public void setTemperature(int temperature) {
			this.temperature = temperature;
		}

		public int getHumidity() {
			return humidity;
		}

		public void setHumidity(int humidity) {
			this.humidity = humidity;
		}

		public int getWindSpeed() {
			return windSpeed;
		}

		public void setWindSpeed(int windSpeed) {
			this.windSpeed = windSpeed;
		}

		public int getWindChill() {
			return windChill;
		}

		public void setWindChill(int windChill) {
			this.windChill = windChill;
		}
		
		public String getPressure() {
			return pressure;
		}

		public void setPressure(String pressure) {
			this.pressure = pressure;
		}
	}
	
	static class Forecast {
		int code;
		String day;
		String text;
		String low;
		String high;
		
		// TODO
//		WeatherCode code;
		
		public String getDay() {
			return day;
		}
		public void setDay(String day) {
			this.day = day;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public String getLow() {
			return low;
		}
		public void setLow(String low) {
			this.low = low;
		}
		public String getHigh() {
			return high;
		}
		public void setHigh(String high) {
			this.high = high;
		}
		public int getCode() {
			return code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		
	}
	
	static enum WeatherCode {		
		TORNADO (0),
		TROPICAL_STORM (1),
		HURRICANE (2),
		SEVERE_THUNDERSTORM(3),
		SHOWERS(11),
		FAIR(33),
		SNOW(16); // ok I skipped a bunch
		
		// TODO if forecast code is in severeWeather list, flash and led or something
		// of course this would make more sense if we were actively polling the weather
		// service for these situations
		final static List<WeatherCode> severeWeather = new ArrayList<WeatherCode>();
		
		static {
			severeWeather.add(TORNADO);
			severeWeather.add(TROPICAL_STORM);
			severeWeather.add(HURRICANE);
			severeWeather.add(SEVERE_THUNDERSTORM);
		}
		
		int code;

		WeatherCode(int code) {
			this.code = code; 
		}

		public int getCode() {
			return code;
		}

		public static List<WeatherCode> getSevereWeather() {
			return severeWeather;
		}
	}
	
	static class RssFetchException extends RuntimeException {
		
		private static final long serialVersionUID = 5945447524428558010L;
		
		RssFetchException(String s, Exception e) {
			super(s, e);
		}
	}
}






