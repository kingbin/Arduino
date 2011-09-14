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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.droplet.framework.service.PullService;
import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.droplet.impl.services.weather.YahooWeather.Weather;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * Provides current weather by zipcode via the Yahoo Weather RSS
 * 
 * @author Andrew Rapp
 *
 */
public class WeatherPullService implements PullService {

	private final static Logger log = Logger.getLogger(WeatherPullService.class);

	private int currentWeatherServiceId;
	private int weatherForecastServiceId;
	
	public int getCurrentWeatherServiceId() {
		return currentWeatherServiceId;
	}

	public void setCurrentWeatherServiceId(int currentWeatherServiceId) {
		this.currentWeatherServiceId = currentWeatherServiceId;
	}

	public int getWeatherForecastServiceId() {
		return weatherForecastServiceId;
	}

	public void setWeatherForecastServiceId(int weatherForecastServiceId) {
		this.weatherForecastServiceId = weatherForecastServiceId;
	}

	private YahooWeather yahooWeather;
	
	public WeatherPullService(String zip) throws ParserConfigurationException {
		yahooWeather = new YahooWeather();
		yahooWeather.setZipcode(zip);
	}

	public IContent execute(Integer serviceId, XBeeResponse response, ServiceContext serviceContext) throws Exception {
		// a more flexible approach would be to have arduino send the weather parameters, but whateva
		Weather weather = yahooWeather.getWeather(true);
		//log.debug(weather);
		
		if (serviceId == this.currentWeatherServiceId) {
			return serviceContext.getFormatter().format(weather.current());	
		} else if (serviceId == this.weatherForecastServiceId) {
			return serviceContext.getFormatter().format(weather.forecast());
		}
	
		throw new RuntimeException("wasn't expecting that");
	}	
}






