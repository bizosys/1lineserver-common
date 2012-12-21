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
package com.bizosys.onelineserver.service.scheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.BaseService;
import com.oneline.util.Configuration;

public class SchedulerService extends BaseService implements ISchedulerService {

	private final static Logger LOG = Logger.getLogger(SchedulerService.class);
	static SchedulerService instance = null;
	public static SchedulerService getInstance() {
		 if ( null != instance) return instance;
		 synchronized (SchedulerService.class) {
			 if ( null != instance) return instance;
			 instance = new SchedulerService();
		 }
		 return instance;
	}
	 
	private Timer timer = null;
	public Map<String, ScheduleTask > scheduledTasks = 
		new HashMap<String, ScheduleTask >();

	private SchedulerService() {
		
	}

	@Override
	public boolean serviceStart(Configuration conf) {
		LOG.info("Starting scheduler service");
		super.serviceStart(conf);
		timer = new Timer(true);
		SchedulerLog.l.info("Scheduler Service has Started Sucessfully.");
		LOG.info("Scheduler service started");
		return true;
	}

    /**
     * Stopping the service
     */
	@Override
	public boolean serviceStop() {
		LOG.info("Stopping scheduler service");
		timer.cancel();
		LOG.info("Scheduler service stopped");
		return true;
	}
    
	/**
	 * A task is added to the timer.
	 */
	public boolean putTask(ScheduleTask task) {
		String taskId = task.getJobName();
		if ( scheduledTasks.containsKey(taskId) ) {
			ScheduleTask existingTask = scheduledTasks.get(taskId);
			if ( null != existingTask ) existingTask.refresh(task);
			return false;
		} else {
			scheduledTasks.put(taskId, task);
			try {
				task.schedule(); 
			} catch (Exception ex) {
				SchedulerLog.l.fatal(ex);
			}
			return true;
		}
	}
	
	/**
	 * Once executed, again it is configured for next window
	 * Not to conflict the parallel processing/ always clone and add.
	 * @param aTask
	 */
	public void putTaskNextTime(String jobId) {
		
		if ( SchedulerLog.l.isDebugEnabled() ) SchedulerLog.l.debug("scheduler.SchedulerService >> Putting Task For Next Time");

		ScheduleTask aTask =  scheduledTasks.get(jobId);
		if ( null == aTask) {
			if ( SchedulerLog.l.isDebugEnabled() ) SchedulerLog.l.debug(
				"scheduler.SchedulerService >> The job might have been discontinued");
			return;
		}
		
		try {
			Date startTime = aTask.getNextWindow();
			if ( null == startTime) {
				aTask.purge();
				scheduledTasks.remove(jobId);
				return;
			}

			if ( (null != aTask.endDate) && startTime.after(aTask.endDate) ) {
				if ( SchedulerLog.l.isDebugEnabled() ) SchedulerLog.l.debug(
					"scheduler.SchedulerService >> " + jobId + " : Job is expired.");
				aTask.purge();
				scheduledTasks.remove(jobId);
				return;
			} else {
				if ( SchedulerLog.l.isDebugEnabled() ) SchedulerLog.l.debug(
					"scheduler.SchedulerService >> " + jobId + " >> Scheduled at : " + startTime);
				this.timer.schedule(aTask.clone(), startTime);
			}
		} catch (Exception ex) {
			SchedulerLog.l.fatal("scheduler.SchedulerService >> ", ex);
		}
	}
	
}