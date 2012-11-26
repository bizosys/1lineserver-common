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

import com.oneline.util.StringUtils;

public class UserProfile 
{
	public static final String ANY = "*";
	public static final String GUEST = "GUEST";

	public String loginid = StringUtils.Empty;
	public String id = StringUtils.Empty;
	public String hexdigest = StringUtils.Empty;
	
	private boolean isGuest = false;
	
	public static UserProfile GUEST_PROFILE;
	
	static
	{
		GUEST_PROFILE = new UserProfile(GUEST, GUEST);
		GUEST_PROFILE.isGuest = true;
	}
	
	public UserProfile() 
	{
	}

	public UserProfile(String loginid, String id) 
	{
		this.loginid = loginid;
		this.id = id;
	}

	public boolean isGuest()
	{
		return this.isGuest;
	}
	
	public static UserProfile getAnonymous()
	{
		return GUEST_PROFILE;
	}
	
}
