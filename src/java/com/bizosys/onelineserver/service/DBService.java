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

import org.apache.log4j.Logger;

import com.oneline.dao.PoolFactory;
import com.oneline.util.FileReaderUtil;

public class DBService extends BaseService
{

    private final static Logger LOG = Logger.getLogger(DBService.class);
    
	public void refresh() 
	{
	}

	@Override
	public boolean serviceStart(IConfiguration conf) 
	{
		super.serviceStart(conf);
		try
		{
			String dbConfFilename = conf.get("db.conf", "db.conf");
			String configXml = FileReaderUtil.toString(dbConfFilename);
			PoolFactory.getInstance().setup(configXml);
		}
		catch (Exception e)
		{
			LOG.error("Error in starting database service.", e);
			return false;
		}
		return true;
	}

	@Override
	public boolean serviceStop()
	{
		return PoolFactory.getInstance().stop();
	}
}
