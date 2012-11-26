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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * SecurityFilter is the gate keeper for security in 10screens. 
 * It makes sure users are authenticated before they are given access 
 * to the site. Guest access is provided for not yet logged in users 
 * @author Sunil
 *
 */
public class SecurityFilter implements Filter
{
	private static final Logger LOG = Logger.getLogger(SecurityFilter.class);
	
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) 
		throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		
		UserCookieHandler.getInstance().store(request, response);
		UserProfile user = UserCookieHandler.getInstance().getUser(request, response);
		LOG.debug("login Before:" + user.loginid);
		request.setAttribute("__user", user);
		chain.doFilter(request, response);
		UserCookieHandler.getInstance().clear();
		LOG.debug("login After:" + user.loginid);
	}

	public void init(FilterConfig config) throws ServletException 
	{
		LOG.info("SecurityFilter is on and working.");
	}

	public void destroy() 
	{
		LOG.info("SecurityFilter is going down.");
	}
}
