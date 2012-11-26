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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * @author karan
 */
public class QueueProcessingService extends BaseService implements Runnable {

	private final static Logger LOG = Logger.getLogger(QueueProcessingService.class);
	private static final boolean INFO_ENABLED = LOG.isInfoEnabled();
	private static QueueProcessingService instance = null;
	
	public static QueueProcessingService getInstance() {
		if ( null != instance) return instance;
		synchronized (QueueProcessingService.class) {
			if ( null != instance ) return instance;
			BlockingQueue<Task> msgQueue = new LinkedBlockingQueue<Task>();
			instance = new QueueProcessingService(msgQueue);
			Thread offlineThread = new Thread(instance);
			offlineThread.setDaemon(true);
			offlineThread.start();
		}
		return instance;
	}
	
	BlockingQueue<Task> blockingQueue = null; 
	
	private QueueProcessingService () {}
	
	private QueueProcessingService ( BlockingQueue<Task> blockingQueue){
		this.blockingQueue = blockingQueue;
	}

	public void addTask(Task task) {
		if ( null == task ) return;
		if ( LOG.isDebugEnabled() ) LOG.debug("QueueProcessingService >  A new task is lunched > " + task.getJobName());
		
		blockingQueue.add(task); 
	}
	
	public int getQueueSize() {
		if ( null == blockingQueue) return 0;
		else return blockingQueue.size();
	}
	
	/**
	 * Takes a transaction from the queue and apply this in the database.
	 */
	public void run() {
		LOG.info("QueueProcessingService > Batch processor is ready to take jobs.");
		while (true) {
			Task offlineTask = null;
			try {
				offlineTask = this.blockingQueue.take(); //Request blocks here 
				if ( INFO_ENABLED ) LOG.info( "QueueProcessingService > Taken from the Queue for processing - " + offlineTask.getJobName());
				offlineTask.process();
				
			} catch (InterruptedException ex) {
				LOG.warn("Batch Interrupted", ex);
				Iterator<Task> queueItr = this.blockingQueue.iterator();
				while ( queueItr.hasNext() ) LOG.fatal("QueueProcessingService > " + queueItr.next());
				break;
			} catch (Exception ex) {
				LOG.fatal("QueueProcessingService > ",  ex);
				if ( null != offlineTask) LOG.fatal("QueueProcessingService > " + offlineTask.toString());
			}
		}
	}

	@Override
	public boolean serviceStart(IConfiguration conf) 
	{
		super.serviceStart(conf);
		QueueProcessingService.getInstance();
		return true;
	}

	@Override
	public boolean serviceStop() {
		return true;
	}

}