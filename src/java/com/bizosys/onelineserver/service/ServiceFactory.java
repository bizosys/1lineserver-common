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

package com.bizosys.onelineserver.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.scheduler.SchedulerService;
import com.oneline.util.StringUtils;

public class ServiceFactory extends BaseService {

	private static Logger LOG = Logger.getLogger(ServiceFactory.class);
	private static ServiceFactory thisInstance = new ServiceFactory();
	public IConfiguration conf = null;

	private Map<String, IService> services = new HashMap<String, IService>(8); 

	private int lvl = IService.LVL_OPTIMAL;
	
	private ServiceFactory () {
	}

	/**
	 * Only once this gets initialized from the web application start up.
	 * @return
	 */
	public static ServiceFactory getInstance() {
		return ServiceFactory.thisInstance;
	}
	
	public boolean serviceStart(IConfiguration conf) {
		
		this.conf = conf;
		String[] startL = this.conf.getStrings("services.to.start");
		
		boolean allServiceStarted = true;
		try 
		{
			BaseService dbService = new DBService();
			boolean sqlStat = startAService(dbService, "sql", startL);

			BaseService schedulerService = SchedulerService.getInstance();
			boolean schedulerStat = startAService(schedulerService, "scheduler", startL);

			BaseService asyncService = AsyncProcessingService.getInstance();
			boolean asyncStat = startAService(asyncService, "async", startL);

			BaseService queueService = QueueProcessingService.getInstance();
			boolean queueStat = startAService(queueService, "queue", startL);
			
			allServiceStarted = sqlStat && schedulerStat && asyncStat && queueStat;

			LOG.info("> All services started with Status : " + allServiceStarted);
/*			Runtime.getRuntime().addShutdownHook(
				new Thread( new Runnable() { 
					public void run() {
						LOG.debug("System is Shutting Down...");
						ServiceFactory.getInstance().serviceStop();
					}
				} )
			);
			LOG.info("Shutdown thread is regisered.");
*/			
			boolean allDelayedServiceStarted = this.delayedStart();
			LOG.info("> All delayed services started with Status : " + allDelayedServiceStarted);

			LOG.info(StringUtils.FatalPrefix + "> All services started with Status : " + (allServiceStarted && allDelayedServiceStarted) );
			return allServiceStarted && allDelayedServiceStarted;
		}
		catch (RuntimeException e) 
		{
			LOG.error("Error in starting services", e);
			return false;
		}
	}
	
	private boolean startAService(BaseService service, String serviceName, String[] startL) {
		
		if (this.shouldStartService(serviceName, startL)) 
			LOG.info("> Starting the " + serviceName + " service.");
		else return true;
			
		this.services.put(serviceName, service);
		boolean status = service.serviceStart(conf);
		if ( status ) {
			LOG.info("> " + serviceName + " service started sucessfully.");
		} else {
			LOG.fatal("> Failed on starting " + serviceName + " service.");
		}
		return status;
	}

	private boolean shouldStartService(String serviceName, String[] startL)
	{
		if (startL == null || startL.length == 0) return true;
		for (String name : startL)
		{
			if (name.equals(serviceName)) return true;
		}
		LOG.info("> Not starting " + serviceName + " service.");
		return false;
	}

	public boolean delayedStart() {
		boolean isSucess = true;
		for (IService aService: services.values()) {
			if ( null != aService ) {
				if ( ! aService.delayedStart() ) isSucess = false;
			}
		}
		return isSucess;
		
	}

	public IConfiguration getAppConfig() {
		return this.conf;
	}
	
	
	public IService getService(String service) {
		return services.get(service);
	}
	
	/**
	 * This is going to stop all the processing units. 
	 */
	public boolean serviceStop() {
		LOG.trace("Stopping all services ____");
		LOG.trace("Local Job runner shutdown.");
		if ( null != services) {
		}
		return true;
	}
	
/*	public void stopOneService(String name) {
		LOG.trace("Stopping service :" + name);
		System.out.println("Stopping service :" + name);
		IService aService = services.get(name);
		if ( null != aService ) aService.serviceStop(); 
		
		try { Thread.sleep(100); } catch (InterruptedException ex) {
			LOG.trace("Interrupted service stop:" + name);
		}
	}*/
	
	/**
	 * No request will be taken now.. Existing requests will be served. 
	 */
	public boolean serviceSuspend() {
		boolean isSucess = true;
		for (IService aService: services.values()) {
			if ( null != aService ) {
				if ( ! aService.serviceSuspend() ) isSucess = false;
			}
		}
		return isSucess;
	}
	
	/**
	 * Things will perform smoothly as usual. Here the suspension state is withdrawn. 
	 */
	public boolean serviceResume() {
		boolean isSucess = true;
		for (IService aService: services.values()) {
			if ( null != aService ) {
				if ( ! aService.serviceResume() ) isSucess = false;
			}
		}
		return isSucess;
	}

	/**
	 * Refresh this service. This will help us to restrain from app booting.
	 */
	public boolean serviceRefresh() {
		boolean isSucess = true;
		for (IService aService: services.values()) {
			if ( null != aService ) {
				if ( ! aService.serviceRefresh() ) isSucess = false;
			}
		}
		return isSucess;	
	}
	
	/**
	 * Notify all other services about the current working level.
	 */
	public void setWorkingLevel(int lvl) {
		this.lvl = lvl;
		for (IService aService: services.values()) {
			if ( null != aService ) {
				try {
					aService.setWorkingLevel(lvl);
				} catch (Exception ex) {
					LOG.fatal("ServiceFactory.setWorkingLevel ? " + 
						aService.getClass() , ex);
				}
			}
		}
	}
	
	public int getWorkingLevel() {
		return this.lvl;
	}
	
	public static void main(String[] args) {
	}
	
}