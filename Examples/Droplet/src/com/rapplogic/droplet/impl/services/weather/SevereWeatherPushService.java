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

import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.Alert;
import com.rapplogic.droplet.framework.service.RealtimeAlertPushService;
import com.rapplogic.droplet.framework.service.RecurringService;
import com.rapplogic.droplet.impl.services.weather.YahooWeather.Weather;
import com.rapplogic.droplet.impl.services.weather.YahooWeather.WeatherCode;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Sends alerts based on severe weather for a zipcode.
 * note: this has not been tested
 * 
 * @author andrew
 *
 */
public class SevereWeatherPushService extends RealtimeAlertPushService implements RecurringService {

	private final static Logger log = Logger.getLogger(SevereWeatherPushService.class);
	
	// startup delay
	private long initialDelay = 45000;
	private long delay = 60*1000*10;

	private YahooWeather yahooWeather;
	
	private HashMap<Integer,WeatherCode> severeWeather = new HashMap<Integer,WeatherCode>();
	
	public SevereWeatherPushService(String zip, XBeeAddress64 remoteXBeeAddress) throws ParserConfigurationException {
		yahooWeather = new YahooWeather();
		yahooWeather.setZipcode(zip);
		
		this.setRemoteXBeeAddress(remoteXBeeAddress);
		
		severeWeather.put(WeatherCode.HURRICANE.getCode(), WeatherCode.HURRICANE);
		severeWeather.put(WeatherCode.SEVERE_THUNDERSTORM.getCode(), WeatherCode.SEVERE_THUNDERSTORM);
		severeWeather.put(WeatherCode.TORNADO.getCode(), WeatherCode.TORNADO);
		severeWeather.put(WeatherCode.TROPICAL_STORM.getCode(), WeatherCode.TROPICAL_STORM);
	}
	
	@Override
	public Alert execute(ServiceContext serviceContext) throws Exception {
		Weather weather = yahooWeather.getWeather(true);
		
		log.debug("weather code is " + weather.getCode());
		
		if (severeWeather.get(weather.getCode()) != null) {
			// uh-oh
			Alert alert = new Alert();
			alert.setContent(serviceContext.getFormatter().format("Severe weather: " + weather.getText()));
			alert.setRemoteXBeeAddress(this.getRemoteXBeeAddress());
			alert.setFlashLed(true);
			alert.setSoundAlarm(true);
		
			return alert;
		}
		
		return null;
	}

	@Override
	public String getName() {
		return "severe-weather";
	}

	/**
	 * Start immediately
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

	/**
	 * Run every minute
	 */
	public long getDelay() {
		return this.delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	public RecurringType getType() {
		return RecurringType.FIXED_RATE;
	}
}
