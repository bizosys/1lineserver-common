/*
* Copyright 2010 Bizosys Technologies Limited
*
* Licensed to the Bizosys Technologies Limited (Bizosys) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The Bizosys licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.oneline.web.sensor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.user.UserProfile;
import com.oneline.util.StringUtils;
import com.oneline.util.XmlUtils;
import com.thoughtworks.xstream.converters.extended.SqlTimestampConverter;

/**
 * Starts from client
 * @author Abinash
 *
 */
public class Request {

	private final static Logger LOG = Logger.getLogger(Request.class);
	
	/** Request business feature information */
	public String sensorId = StringUtils.Empty; 
	public String action = StringUtils.Empty;
	public Map<String, String> mapData;

	/** Request Authentication information */
	public String hexDigest = StringUtils.Empty;
	public Boolean isAuthenticated = false;
	private UserProfile user = null; 
	public String clientIp = StringUtils.Empty;
	public String serverName = StringUtils.Empty;

	public Request(String sensorId, String action, Map<String, String> data) {
		this.sensorId = sensorId;
		this.action = action;
		this.mapData = data;
	}

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return
	 * @throws InvalidRequestException
	 */
	private String getMapValue(String key, boolean strict )  throws InvalidRequestException {
		
		String value = null;
		if ( (null != this.mapData) && (null != key) ) {
			if ( mapData.containsKey(key)) 
				value = this.mapData.get(key);
		}
		if ( (null == value) && strict ) {
			throw new InvalidRequestException(key);
		}
		return value;
	}

	public void setUser(UserProfile user)
	{
		if (user != null) this.user = user;
		else this.user = UserProfile.getAnonymous();
	}

	public UserProfile getUser() {
		return this.user;
	}
	/**
	 * @deprecated
	 * Returns the action requested by the client. This method is deprecated, classes can access
	 * this value by a direct public variable
	 * @return
	 */
	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	/**
	 * From the incoming xml data, creates an object directly. 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Object
	 * @throws InvalidRequestException
	 */
	public Object getObject (String key, boolean strict ) throws InvalidRequestException {
		String value = this.getString(key,strict, true, false);
		if ( null == value ) return null;
	
		try {
			return XmlUtils.xstream.fromXML(value);
		} catch (Exception ex) {
			LOG.error("Error in creating object from xml : " + value, ex);
			throw new InvalidRequestException(value, ex);
		}
	}	

	/**
	 * From the incoming xml data, creates an object directly. 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Object
	 * @throws InvalidRequestException
	 */
	public List getList(String key, boolean strict, Class classToFill) throws InvalidRequestException {
		
		Integer recordCount = this.getInteger(key + "T", strict);
		
		if (recordCount == null || recordCount == 0)
		{
			return Collections.EMPTY_LIST;
		}
		
		List<Object> records = new ArrayList<Object>(recordCount);
		
		for(int index = 1; index <= recordCount; index++ )
		{
			records.add(this.getObject(key + index, strict, classToFill));
		}
		
		return records;
	}	

	/**
	 * From the incoming xml data, creates an object directly. 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Object
	 * @throws InvalidRequestException
	 */
	public List getList(String key, boolean strict) throws InvalidRequestException {
		return this.getList(key, strict, null);
	}
		
	/**
	 * From the incoming xml data, creates an object directly. 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Object
	 * @throws InvalidRequestException
	 */
	public Object getObject (String key, boolean strict, Class classToFill) throws InvalidRequestException {
		boolean emptyAllowed = true;
		if (strict) emptyAllowed = false;
		
		String value = this.getString(key,strict, true, emptyAllowed);
		if ( null == value ) return null;
		if (classToFill == null) return value;
		
		try {
			return XmlUtils.xstream.fromXML(value, classToFill.newInstance());
		} catch (Exception ex) {
			LOG.error("Error in creating object from xml : " + value, ex);
			throw new InvalidRequestException(value, ex);
		}
	}	

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Short
	 * @throws InvalidRequestException
	 */
	public Short getShort(String key, boolean strict ) throws InvalidRequestException {
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return Short.parseShort(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Boolen
	 * @throws InvalidRequestException
	 */
	public Boolean getBoolean(String key, boolean strict ) throws InvalidRequestException {
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return Boolean.parseBoolean(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}	
	
	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Integer
	 * @throws InvalidRequestException
	 */
	public Integer getInteger(String key, boolean strict ) throws InvalidRequestException {
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return Integer.parseInt(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}

	public int getInteger(String key, int defaultVal ) throws InvalidRequestException {
		String value = getMapValue(key,false);
		if ( null == value ) return defaultVal;
	
		try {
			return Integer.parseInt(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}
	

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Timestamp
	 * @throws InvalidRequestException
	 */
	public Timestamp getDate(String key, boolean strict ) throws InvalidRequestException {
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return (Timestamp) new SqlTimestampConverter().fromString(value);
		} catch (Exception ex) {
			throw new InvalidRequestException(key, ex);
		}
	}

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Timestamp
	 * @throws InvalidRequestException
	 */
	public Date getDateTime(String key, boolean strict ) throws InvalidRequestException {
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return new Date(new Long(value));
		} catch (Exception ex) {
			throw new InvalidRequestException(key, ex);
		}
	}
	
	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Long
	 * @throws InvalidRequestException
	 */
	public Long getLong(String key, boolean strict ) throws InvalidRequestException {
		
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return Long.parseLong(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Double
	 * @throws InvalidRequestException
	 */
	public Double getDouble(String key, boolean strict ) throws InvalidRequestException {
		
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return Double.parseDouble(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return Float
	 * @throws InvalidRequestException
	 */
	public Float getFloat(String key, boolean strict ) throws InvalidRequestException {
		
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
	
		try {
			return Float.parseFloat(value);
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}    

	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @return BigDecimal
	 * @throws InvalidRequestException
	 */
	public BigDecimal getBigDecimal(String key, boolean strict ) throws InvalidRequestException {
	
		String value = getMapValue(key,strict);
		if ( null == value ) return null;
		try {
			BigDecimal decimal = new BigDecimal(value.toString());
			return decimal;
		} catch ( NumberFormatException ex) {
			throw new InvalidRequestException(key, ex);
		}
	}        

	public String getStringKeyStrict (String key, boolean strict) throws InvalidRequestException {
		return getString(key, strict, false, true);
	}

	public String getStringKeyStrictTrim (String key, boolean strict, boolean trim) throws InvalidRequestException {
		return getString(key, strict, trim, true);
	}
	
	public String getStringKeyStrictTrimEmptyallowed (String key, boolean strict, boolean trim,
			boolean emptyAllowed ) throws InvalidRequestException {
		return getString(key, strict, trim, emptyAllowed);
	}
	
	/**
	 * 
	 * @param key The map key in which this is passed.
	 * @param strict Is null allowed
	 * @param trim Should it be trimmed.
	 * @param emptyAllowed Is empty expected.
	 * @return String
	 * @throws InvalidRequestException
	 */
	public String getString (String key, boolean strict, boolean trim,
		boolean emptyAllowed ) throws InvalidRequestException {
	
		String value = getMapValue(key,strict);
		if ( !emptyAllowed) {
			if ( null == value ) throw new InvalidRequestException(key);
			if ( StringUtils.Empty.equals(value) ) throw new InvalidRequestException(key);
		}
			
		if ( trim && null != value) value = value.trim();
		return value;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(100);
		sb.append("\n\n\n");
		sb.append("[sensorId=" ).append(this.sensorId);
		sb.append("][action=").append(this.action).append("]\n" + "[data=");
		sb.append(mapData).append(']');
		return sb.toString();
	}    	
	
	public static void main(String[] args) throws Exception {
		Map<String, String> data = new HashMap<String, String>();
		data.put("name", "");
		Request req = new Request("service", "action", data);
		System.out.println( req.getStringKeyStrict("name", true) );
		
	}
}