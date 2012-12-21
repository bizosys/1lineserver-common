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

import com.oneline.util.Configuration;


public class BaseService implements IService {

   protected Configuration conf;
   protected int workingLvl = IService.LVL_OPTIMAL;

	/**
	 * Override on need basis
	 */
    public boolean serviceRefresh() {
		return true;
	}

	/**
	 * Call this
	 */
	public boolean serviceStart(Configuration conf) {
		this.conf = conf;
		return true;
	}

	/**
	 * This will be called after all services are started to give a chance to additional startup
	 */
	public boolean delayedStart() {
		return true;
	}

	/**
	 * Override on need basis
	 */
	public boolean serviceStop() {
		return true;
	}

	/**
	 * Override on need basis
	 */
	public boolean serviceSuspend() {
		return true;
	}

	/**
	 * Again from suspended state to the normal state.
	 */
	public boolean serviceResume() {
		return true;
	}
	
	public void setWorkingLevel(int lvl) {
		this.workingLvl = lvl;
	}
	
	public int getWorkingLevel() {
		return this.workingLvl;
	}
}