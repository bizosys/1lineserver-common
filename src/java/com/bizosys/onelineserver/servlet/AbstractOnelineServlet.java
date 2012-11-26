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

package com.bizosys.onelineserver.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.IConfiguration;
import com.bizosys.onelineserver.service.ServiceFactory;
import com.bizosys.onelineserver.user.UserProfile;
import com.oneline.util.Hash;
import com.oneline.util.StringUtils;
import com.oneline.web.sensor.Request;
import com.oneline.web.sensor.Response;
import com.oneline.web.sensor.Sensor;

public abstract class AbstractOnelineServlet extends HttpServlet {

	protected static final long serialVersionUID = 4L;
	protected final static Logger LOG = Logger.getLogger(AbstractOnelineServlet.class);
	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	private static final boolean INFO_ENABLED = LOG.isInfoEnabled();

	private Map<String, Sensor> sensorM = new HashMap<String, Sensor>();
	private String key = Hash.KEY_VALUE_DEFAULT;
	private Set<String> captchaUrls = null;
	
	@Override
	public void init(ServletConfig config) throws ServletException 
	{
		super.init(config);
    	IConfiguration conf = ServiceFactory.getInstance().getAppConfig();
		this.key = conf.get(Hash.KEY_NAME,Hash.KEY_VALUE_DEFAULT);
		
		/**
		 * Parse all the Urls which require the captcha verification.
		 */
		String captchUrlLine = conf.get("captchaurls");
		if ( ! StringUtils.isEmpty(captchUrlLine)) {
			List<String> sensorActionPairs = StringUtils.fastSplit(captchUrlLine, ',');
			if ( null != sensorActionPairs) {
				captchaUrls = new HashSet<String>();
				for (String sensorAction : sensorActionPairs) {
					LOG.info("Captcha Enable Url :" + sensorAction);
					captchaUrls.add(sensorAction);
				}
			}
		}
	}

	protected void setupSensor(Sensor sensor, String sensorId)
	{
		sensor.init();
		this.sensorM.put(sensorId, sensor);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		this.doProcess(req, res);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		this.doProcess(req, res);
	}
	
	private void doProcess(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {

		res.setContentType("text/html");
		res.setCharacterEncoding (req.getCharacterEncoding() );
		
		if ( INFO_ENABLED ) LOG.info("\n\n\n A web request has entered server.\n");

		/**
		 * Store all the parameters in the Sensor request object
		 */
		Enumeration reqKeys = req.getParameterNames();
		Map<String, String> data = new HashMap<String, String>();

		while (reqKeys.hasMoreElements()) {
			String key = (String) reqKeys.nextElement();
			String value = req.getParameter(key);
			data.put(key, value);
		}

		String sensorId = req.getParameter("service");
		if ( StringUtils.isEmpty(sensorId)) {
			sensorId = req.getParameter("sensor");
		}
		String action = req.getParameter("action");
		sensorId = (null == sensorId) ? StringUtils.Empty : sensorId.trim();
		action = (null == action) ? StringUtils.Empty : action.trim();
		
		if ( INFO_ENABLED ) LOG.info(sensorId + " : " + action);
		
		if ( (sensorId.length() == 0) || ( action.length() == 0 )) {
			String errorMsg =  StringUtils.FatalPrefix + "Sensor [" + sensorId + "] or action [" + action + "] are missing." ; 
			LOG.warn(errorMsg);
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
		}

		Request sensorReq = new Request(sensorId, action, data);
		sensorReq.clientIp = req.getRemoteAddr();
		sensorReq.serverName = req.getServerName();
		
		if ( INFO_ENABLED) {
			StringBuilder sb = new StringBuilder(100);
			sb.append("Service name=").append(sensorId).append(": action=").append(action);
			LOG.info(sb.toString());
			sb = null;
		}

		setUser(req, sensorReq);
		
		/**
		 * Initiate the sensor response, putting the stamp on it and xsl. 
		 */
		PrintWriter out = res.getWriter();
		Response sensorRes = new Response(out);

		String callback = req.getParameter("callback");
		sensorRes.callback = (null == callback) ? StringUtils.Empty : callback;
		String format = req.getParameter("format");
		
		if ( StringUtils.isEmpty(format)) {
			res.setContentType("text/html");
			sensorRes.format = StringUtils.Empty;
		} else if ( "csv".equals(format) ){
			res.setContentType("application/CSV");
			sensorRes.format = "csv";
		} else {
			res.setContentType("text/xml");
			sensorRes.format = format;
		}
		
		try 
		{
			/**
			 * Check if this request need a captcha checkup. 
			 */
			boolean captchaVerified = verifyCaptcha(req, sensorReq, sensorRes, out);
			if ( !captchaVerified) return;
			
			if ( DEBUG_ENABLED ) LOG.debug("Sensor processing START");
			Sensor sensor = this.getSensor(sensorId);
			if (sensor != null) sensor.processRequest(sensorReq, sensorRes);
			else sensorRes.error("UNAVAILABLE" , (sensorId + " service is not initialized.") );
			if ( DEBUG_ENABLED ) LOG.debug("Sensor processing END");
			
		} catch (Exception ex) {
			res.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "CONTACT_ADMIN");
			LOG.error("Error in processing request", ex);
		} finally {
			if ( sensorRes.isError) {
				sensorRes.writeHeader();
				out.write("<error>" +  sensorRes.getError() + "</error>");
				sensorRes.writeFooter();
			}
			out.flush();
			out.close();
		}
	}

	/**
	 * Set the user object in the sensor request.
	 * @param req
	 * @param sensorReq
	 */
	private void setUser(HttpServletRequest req, Request sensorReq) {
		Object userObject = req.getAttribute("__user");
		if ( null == userObject) 
		{
			sensorReq.setUser(UserProfile.getAnonymous());
		} 
		else 
		{
			sensorReq.setUser((UserProfile) userObject);
		}
	}

	/**
	 * Verify the captch for the enabled urls
	 * @param req
	 * @param res
	 * @param out
	 * @return
	 */
	public boolean verifyCaptcha(HttpServletRequest servletReq, Request req, Response res, PrintWriter out)
	{
		if ( null == captchaUrls) return true;
		StringBuilder sb = new StringBuilder(24);
		sb.append(req.sensorId).append('.').append(req.action);
		if ( ! captchaUrls.contains(sb.toString())) return true;
		
		Map<String, String> params = req.mapData;

		String readcaptcha = null;
		String encodedCaptcha = null;
		if ( params.containsKey("readcaptcha") ) 
			readcaptcha = req.getString("readcaptcha", true, true, false);

		if ( params.containsKey("encodedcaptcha") ) 
			encodedCaptcha = req.getString("encodedcaptcha", true, true, false);
		else {
			if ( StringUtils.isEmpty(encodedCaptcha)) {
				Cookie[] cookies = servletReq.getCookies();
				if (cookies != null && cookies.length > 0) {
					for (Cookie cookie : cookies) {
						if ( cookie.getName().equals("encodedcaptcha")) {
							encodedCaptcha = cookie.getValue(); 
							break;
						}
					}
				}
			}
		}
		
		String captchaTextEncoded = null;
		if ( null != readcaptcha && null != encodedCaptcha) {
			String secureCaptchaText = servletReq.getRemoteAddr() + readcaptcha;
			captchaTextEncoded = Hash.createHex(this.key, secureCaptchaText);
			if (captchaTextEncoded.equals(encodedCaptcha)) return true;
		}
		
		res.error("INVALID_CAPTCHA", "Retry Captcha");
		return false;
	}
	
	private Sensor getSensor(String sensorId)
	{
		if (this.sensorM == null || this.sensorM.isEmpty()) 
		{
			return null;
		}
		return this.sensorM.get(sensorId);
	}

}
