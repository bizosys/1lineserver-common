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

import java.io.PrintWriter;
import java.util.List;

import org.apache.log4j.Logger;

import com.oneline.util.StringUtils;
import com.oneline.util.XmlUtils;
 

public class Response { 

	private PrintWriter out = null;
    public String callback = StringUtils.Empty;
    public String format = StringUtils.Empty; //JSONP, XML, XSL, CSV

	static final int FORMAT_TEXT_OR_HTML = -1;
	static final int FORMAT_XML = 0;
	static final int FORMAT_JSONP = 1;
	static final int FORMAT_XSL = 2;
	static final int FORMAT_CSV = 3;
    int formatIndex = FORMAT_TEXT_OR_HTML;
	
    public Object data = null;
    private String errorMessage = null;
    private String errorCode = null;
    public Boolean isError = false;

    private static final Logger LOG = Logger.getLogger(Response.class);

	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
    
    public static void register (String name, Class value) {
    	XmlUtils.xstream.alias(name, value);
    }

    private Response() 
    {
    }
    
    public PrintWriter getWriter() {
    	return this.out;
    }
    
    public Response(PrintWriter out) {
    	this.out = out;
    }

    public boolean hasNoErrors()
    {
    	return !(this.isError);
    }
    
    public void writeTextWithNoHeaderAndFooter(String result) {
    	if ( DEBUG_ENABLED ) LOG.debug("The result:" + result );
    	out.println(result);
    }

    public void writeTextWithHeaderAndFooter(String xmlText) {
    	if ( DEBUG_ENABLED ) LOG.debug("The result:" + xmlText );
    	writeHeader();
    	out.println(xmlText);
    	writeFooter();
    }

    public void writeTextListWithHeaderAndFooter(List serializeL) {
    	writeHeader();
    	if ( null != serializeL ){ 
	    	int serializeT = serializeL.size();
	    	for ( int i=0; i<serializeT; i++ ) {
	        	out.println(serializeL.get(i).toString());
	        	if ( DEBUG_ENABLED ) LOG.debug("\n" + serializeL.get(i) + "\n");
	    	}
    	}
    	writeFooter();
    }

    /**
     * Writes Header, Object and Footer 
     * @param aObject
     */
    public void writeObjectWithHeaderAndFooter(Object aObject) {
    	writeHeader();
    	String body =  ( formatIndex == FORMAT_XML ) ? 
    			XmlUtils.xstream.toXML(aObject) : XmlUtils.jstream.toXML(aObject);
    	out.println(body);
    	writeFooter();
    	
    	if ( DEBUG_ENABLED ) { LOG.debug("\n" + body + "\n");}
    }

    public void writeObjectListWithHeaderAndFooter(List serializeL) {
    	writeHeader();
    	if ( null != serializeL ){ 
	    	int serializeT = serializeL.size();
	    	for ( int i=0; i<serializeT; i++ ) {
	        	String body =  ( formatIndex == FORMAT_XML ) ? 
	        			XmlUtils.xstream.toXML(serializeL.get(i)) : XmlUtils.jstream.toXML(serializeL.get(i));
				out.println(body);
	        	if ( DEBUG_ENABLED) LOG.debug("\n" + body + "\n");
	    	}
    	}
    	writeFooter();
    }
    
    public void writeXMLArray(String[] xmlStrings, String tag) {
    	writeHeader();
    	StringBuilder sb = new StringBuilder(100);
    	if ( null != xmlStrings ){ 
	    	int serializeT = xmlStrings.length;
	    	
	    	for ( int i=0; i<serializeT; i++ ) {
	        	sb.append('<').append(tag).append('>');
	        	sb.append(xmlStrings[i]);
	        	sb.append("</").append(tag).append('>');
	        	out.println('<');
	    		out.println(sb.toString());
        		LOG.debug(sb.toString());
        		sb.delete(0, sb.capacity());
	    	}
    	}
    	writeFooter();
    }

    public void error(String errorCode, String message) 
    {
   		this.isError = true;
		this.errorMessage = message;
		this.errorCode = errorCode;
    }

    public void error(String errorCode, String message, Exception ex) 
    {
    	this.error(message, errorCode);
		LOG.error(message, ex);
	}
    
    public String getError() 
    {
    	if (this.isError)
    	{
			StringBuffer errors = new StringBuffer();
			errors.append("<code>").append(this.errorCode).append("</code>");
			errors.append("<message>").append(this.errorMessage).append("</message>");
			return errors.toString();
    	}
    	return "<code>0</code><message>No Error</message>";
    }

    /**
     * We will support JSONP, XML and XSL type headers.
     * XSL will render the output using a XSL file.
     * JSONP and XML calls appropriate callback function.
     * In JSONP whole array is passed to the callback function.
     * XML it is stamped as message id.
     */
    public void writeHeader() {
    	StringBuilder sb = new StringBuilder(100);

    	if ( ! StringUtils.isEmpty(format) ) {
    		if ( format.length() == 5 ) formatIndex = FORMAT_JSONP;
    		else if ( format.equals("xml") )  formatIndex = FORMAT_XML;
    		else formatIndex = FORMAT_XSL;
    	}
    	
    	switch ( formatIndex ) {
    		case FORMAT_XML:
    		   	if ( StringUtils.isEmpty(this.callback) ) {
    	    		sb.append("<result>");
    	    	} else {
    	    		sb.append("<result callback=\"").append(this.callback).append("\" >");
    	    	}
    		   	break;
    		
    		case FORMAT_JSONP:
    		   	if ( ! StringUtils.isEmpty(this.callback) )
    		   		sb.append(this.callback).append('(');
    		   	break;
    		
    		case FORMAT_XSL:
        		sb.append("<?xml version=\"1.0\" ?>");
        		sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"");
        		sb.append(this.callback); //This is the XSL file name
        		sb.append("\" ?>");
        		sb.append("<result>");
    		   	break;
    		
    		default: //HTML and CSV formats.
    		   	break;
    	}
    	
    	out.print(sb.toString());
    	sb.delete(0, sb.capacity());
    }
    
    /**
     * Footer at the end of the result
     */
    public void writeFooter() {
    	switch ( formatIndex ) {
			case FORMAT_XML:
		    	out.print("</result>");
			   	break;
			
			case FORMAT_JSONP:
			   	if ( ! StringUtils.isEmpty(this.callback) ) out.print(");");
			   	break;
			   	
			case FORMAT_XSL:
		    	out.print("</result>");
			   	break;

			default: //HTML and CSV formats.
    		   	break;
	    }
    }
}
	