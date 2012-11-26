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

import java.util.HashMap;
import java.util.Map;

import com.oneline.util.StringUtils;

public class UserProfile 
{
	public static final String ANY = "*";
	public static final String GUEST = "GUEST";

	public String loginid = StringUtils.Empty;
	public String hexdigest = StringUtils.Empty;
	
	private Map<String, String> profile = new HashMap<String, String>();
	private boolean isGuest = false;
	
	public static UserProfile GUEST_PROFILE;
	
	static
	{
		GUEST_PROFILE = new UserProfile(GUEST, "role,"+ GUEST);
		GUEST_PROFILE.isGuest = true;
	}
	
	public UserProfile() 
	{
	}

	public UserProfile(String loginid, String profile) 
	{
		this.loginid = loginid;
		setProfile(profile);
	}

	public boolean isGuest()
	{
		return this.isGuest;
	}
	
	public static UserProfile getAnonymous()
	{
		return GUEST_PROFILE;
	}
	
	public String getProfile() {
		boolean isFirstTime = true;
		StringBuilder aclSB = new StringBuilder();
		for (String key : profile.keySet()) {
			if ( isFirstTime ) {
				isFirstTime = false;
			} else {
				aclSB.append('|');
			}
			aclSB.append(key).append(',').append(profile.get(key));
		}
		return aclSB.toString();
	}
	
	public String getProfile(String key) {
		return this.profile.get(key);
	}	

	public void setProfile(String strProfile) {
		
		if ( null == strProfile) return;
		if ( strProfile.length() == 0 ) return;
		
		int startIndex = 0;
		int endIndex = 0;
		while ( true ) {
			endIndex = strProfile.indexOf(',', startIndex);
			if ( endIndex == -1 ) break;
			String key = strProfile.substring(startIndex, endIndex);
			startIndex = endIndex + 1;
			
			endIndex = strProfile.indexOf('|', startIndex);
			String value = ( endIndex == -1 )  ? strProfile.substring(startIndex) :
					strProfile.substring(startIndex, endIndex);
			
			this.profile.put(key, value);

			if ( endIndex  == -1 ) break;
			startIndex = endIndex + 1;

		
		}
	}
	
	public static void main(String[] args) {
		UserProfile profile = new UserProfile("abhinashak@gmail.com", "ou,si|outlet,2345|role,34");
		System.out.println(profile.getProfile("outlet"));
		System.out.println(profile.getProfile("role"));
		System.out.println(profile.getProfile("ou"));
		
		System.out.println(profile.getProfile() );
	}
}
