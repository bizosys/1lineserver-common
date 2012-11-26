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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.IConfiguration;
import com.bizosys.onelineserver.service.ServiceFactory;
import com.oneline.util.Hash;
import com.oneline.util.StringUtils;

public class CaptchaServlet extends HttpServlet {
	
	/**
	 * Default version uid
	 */
	private static final long serialVersionUID = 1L;
	protected final static Logger LOG = Logger.getLogger(CaptchaServlet.class);
	
	private int height = 200;  
	private int width = 70;      
	Random r = new Random();
	String key = Hash.KEY_VALUE_DEFAULT;
	String subDomain = null;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);   
    	IConfiguration conf = ServiceFactory.getInstance().getAppConfig();
		this.key = conf.get(Hash.KEY_NAME,Hash.KEY_VALUE_DEFAULT);
		this.subDomain = conf.get("subdomain", "");
	}  
	
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {  
		
		//Expire response    
		response.setHeader("Cache-Control", "no-cache");   
		response.setDateHeader("Expires", 0);   
		response.setHeader("Pragma", "no-cache");   
		response.setDateHeader("Max-Age", 0);

		OutputStream os = null;
		try {
			String captchaText = Long.toString(Math.abs(r.nextLong()), 36);
			captchaText = cleanseCaptchaText(captchaText);
			
			String secureCaptchaText = req.getRemoteAddr() + captchaText;
			String captchaTextEncoded = Hash.createHex(this.key, secureCaptchaText);
			
			response.setHeader("encodedcaptcha", captchaTextEncoded);
			Cookie cookie = new Cookie("encodedcaptcha", captchaTextEncoded);
			if ( ! StringUtils.isEmpty(this.subDomain)) cookie.setDomain(this.subDomain);
			cookie.setMaxAge(-1);
			response.addCookie(cookie);
			
			os = response.getOutputStream();
			build(captchaText, os, height, width);
		} catch (Exception ex) {
			response.sendError(
				HttpServletResponse.SC_EXPECTATION_FAILED, "CONTACT_ADMIN");
			LOG.error("Error in processing request", ex);
		} finally {
			if ( null != os ) {
				try {
					os.flush();
					os.close();
				} catch (Exception ex) {
				}
			}
		}
	}
	
	public static void build( String captchText, 
			OutputStream os, int width, int height) throws IOException {  
			
			BufferedImage image = new BufferedImage(
				width, height, BufferedImage.TYPE_INT_RGB);   
			Graphics2D graphics2D = image.createGraphics();   
			graphics2D.setPaint(Color.white);   
			float[] dashPattern = { 30, 10, 10, 10 };
			graphics2D.setStroke(new BasicStroke(8, BasicStroke.CAP_BUTT,
	                BasicStroke.JOIN_MITER, 10,
	                dashPattern, 0));
			
			Font font = new Font(Font.SERIF, Font.ITALIC, 36);
			graphics2D.setFont(font);

			graphics2D.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			double theta = Math.PI * 5 / 180;
			double centerx = (double) width / 2;
			double centery = (double) height / 2;
			AffineTransform transform = AffineTransform.getRotateInstance(theta, centerx, centery); 
			graphics2D.setTransform(transform); 		
			
			
			graphics2D.drawString(captchText,50,50);
			graphics2D.setStroke(new BasicStroke(1));
			graphics2D.drawLine(30,30, 130, 50);
			
			graphics2D.dispose();
			ImageIO.write(image, "jpeg", os);
		}

	private static String cleanseCaptchaText(String captchaText) {
		if ( captchaText.length() > 6)
			captchaText = captchaText.substring(0,6);   
		
		//Replace Confusing Characters
		captchaText = captchaText.replace("1", "2");
		captchaText = captchaText.replace("l", "L");
		
		captchaText = captchaText.replace("0", "Z");
		captchaText = captchaText.replace("o", "p");
		captchaText = captchaText.replace("O", "P");
		return captchaText;
	}	
}