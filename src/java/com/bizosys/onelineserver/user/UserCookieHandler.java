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

package com.bizosys.onelineserver.user;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.ServiceFactory;
import com.oneline.util.Configuration;
import com.oneline.util.Hash;
import com.oneline.util.StringUtils;

public class UserCookieHandler
{
	protected static final String COOKIE_LOGINID = "lid";
	protected static final String COOKIE_USERID = "uid";
	protected static final String COOKIE_HEXDIGEST = "hex";

	private static final Logger LOG = Logger.getLogger(UserCookieHandler.class);

	private String key = Hash.KEY_VALUE_DEFAULT;
	private String subDomain = StringUtils.Empty;
	private int credentialsExpireInSeconds = 0;

	private Map<String, String> cachedEncodings = new HashMap<String, String>();    

	private static UserCookieHandler handler = null;
	
	public ThreadLocal<HttpServletRequest> request;
	public ThreadLocal<HttpServletResponse> response;

	public static UserCookieHandler getInstance()
	{
		if (handler != null) return handler;
		handler = new UserCookieHandler();
		return handler;
	}
	
	private UserCookieHandler()
	{
		this.init();
    	this.request = new ThreadLocal<HttpServletRequest>();
    	this.response = new ThreadLocal<HttpServletResponse>();
	}
	
    private void init()
	{
    	Configuration conf = ServiceFactory.getInstance().getAppConfig();
		this.key = conf.get(Hash.KEY_NAME,Hash.KEY_VALUE_DEFAULT);
		this.subDomain = conf.get("subdomain", "");
		this.credentialsExpireInSeconds = conf.getInt("credential_expire_seconds", (8 * 60 * 60)); // 8 hours in seconds

		if ( LOG.isDebugEnabled()) 
		{
			StringBuilder sb = new StringBuilder(100);
			sb.append("this.subDomain:").append(this.subDomain);
			sb.append(". this.credentialsExpireInSeconds:").append(this.credentialsExpireInSeconds);
			LOG.debug(sb.toString());
			sb.delete(0, sb.capacity());
		}
	}

    public void store(HttpServletRequest request, HttpServletResponse response)
    {
    	this.request.set(request);
    	this.response.set(response);
    }
    
    public void clear()
    {
    	this.request.remove();
    	this.response.remove();
    }
    
	public UserProfile getUser( HttpServletRequest request, HttpServletResponse response)
	{
		UserProfile user = this.getUser(request);
		if (user == null) return UserProfile.getAnonymous();

		String browserKey = this.buildBrowserKey(request, user);
		return (this.isDigestValid(browserKey, user.hexdigest)) ? user: UserProfile.getAnonymous();
	}
	
	public void setUser(UserProfile user, HttpServletRequest request,	HttpServletResponse response ) 
	{
		String browserKey = this.buildBrowserKey(request, user);
		user.hexdigest = Hash.createHex(this.key, browserKey);
		LOG.debug("Setting user:" + user.loginid+ " with browserKey: + " + browserKey + " and digest:" + user.hexdigest);
		this.storeInCookie(user, response);
	}
	
	public void setUser(UserProfile user) 
	{
		if (this.request == null || this.response == null) return;
		HttpServletRequest request = this.request.get();
		HttpServletResponse response = this.response.get();
		if (request == null || response == null) return;
		this.setUser(user, request, response);
	}
	
	public void removeUser(UserProfile user)
	{
		String browserKey = this.buildBrowserKey(this.request.get(), user);
		user.hexdigest = Hash.createHex(this.key, browserKey);
		this.removeCookies(user, this.response.get());
	}
	
    /**
     * Private Methods
     */
    
	private String buildBrowserKey(HttpServletRequest request, UserProfile user) {
		StringBuilder sb = new StringBuilder(100);
		sb.append(user.getProfile()).append(':').append(user.loginid).append(':').append(request.getRemoteAddr());
		return sb.toString();
	}

	/**
	 * HexDigest is checked against the local cache as follows.
	 * 1. If browser did not send hexdigest then this request is not authenticated.
	 * 2. If browser has sent and matches the digest against local cache it is good.
	 * 3. If browser has sent it but local cache does not have it, regenerate and match. 
	 * @param browserDigest
	 * @return
	 */
	
	protected boolean isDigestValid(String browserKey, String browserDigest) 
	{
		LOG.debug("Checking digest is good:" + browserKey + "-" + browserDigest);
		if (StringUtils.isEmpty(browserDigest))	return false; //Browser did not send the digest
		
		if (this.cachedEncodings.containsKey(browserKey) && browserDigest.equals(this.cachedEncodings.get(browserKey))) return true; //Digests match

		String localDigest = Hash.createHex(this.key, browserKey);	
		if (browserDigest.equals(localDigest)) {
			this.cachedEncodings.put(browserKey, localDigest);
			return true;
		}
		
        LOG.warn("CookieSession > Authentication digest seem to be corrupted or compromised. Browser sent a digest and did not match with local digest for key:" + browserKey);
		return false;
	}

	/**
	 * This is constructed from bizosys stamped cookies
	 * This is valid for 8 hours
	 * 
	 * If we don't find in our cookie, check it with SSO service
	 * 
	 * @param request
	 * @return
	 */
	private UserProfile getUser(HttpServletRequest request)
	{
		Cookie[] cookies = request.getCookies();
		if (cookies != null && cookies.length > 0)
		{
			LOG.debug("Got cookies count :" + cookies.length);
			UserProfile user = new UserProfile();
			for (Cookie cookie : cookies)
			{
				LOG.debug("Cookie - " + cookie.getName() + " : " + cookie.getValue());
				if (COOKIE_LOGINID.equals(cookie.getName()))
				{
					user.loginid = this.decodeCookieValue(cookie);
				}
				else if (COOKIE_USERID.equals(cookie.getName()))
				{
					user.setProfile(this.decodeCookieValue(cookie));
				}
				else if (COOKIE_HEXDIGEST.equals(cookie.getName()))
				{
					user.hexdigest = this.decodeCookieValue(cookie);
				}
			}
			return user;
		}
		LOG.debug("Did not get cookies.");
		return null;
	}

	private String decodeCookieValue(Cookie cookie) 
	{
		try 
		{
			return URLDecoder.decode(cookie.getValue(), "UTF-8");
		} 
		catch (UnsupportedEncodingException e) 
		{
			return cookie.getValue();
		}
	}

	private void storeInCookie(UserProfile user, HttpServletResponse response)
	{
		this.addCookie(COOKIE_LOGINID, user.loginid, response, this.credentialsExpireInSeconds);
		this.addCookie(COOKIE_USERID, user.getProfile(), response, this.credentialsExpireInSeconds);
		this.addCookie(COOKIE_HEXDIGEST, user.hexdigest, response, this.credentialsExpireInSeconds);
	}

	private void removeCookies(UserProfile user, HttpServletResponse response)
	{
		this.addCookie(COOKIE_LOGINID, user.loginid, response, 1);
		this.addCookie(COOKIE_USERID, user.getProfile(), response, 1);
		this.addCookie(COOKIE_HEXDIGEST, user.hexdigest, response, 1);
	}

	private void addCookie(String key, String value, HttpServletResponse response, int expiryInSeconds)
	{
		if (LOG.isDebugEnabled()) LOG.debug("Storing cookie: "+ key + ":" + value + " for " + expiryInSeconds  + " seconds");
		String encodedValue = value;
		try 
		{
			encodedValue = URLEncoder.encode(value, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) 
		{
			encodedValue = value;
		}
		Cookie cookie = new Cookie(key, encodedValue);
		if ( ! StringUtils.isEmpty(this.subDomain)) cookie.setDomain(this.subDomain);
		cookie.setMaxAge(expiryInSeconds);
		response.addCookie(cookie);
	}
	
	/**
	private void resetCookie(HttpServletResponse res, Request sensorReq) {
		Cookie hexCookie = new Cookie("HEXDIGEST", "");
		res.addCookie(hexCookie);
	}
	*/

}