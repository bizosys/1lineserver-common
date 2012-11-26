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

public interface IConfiguration
{

	/**
	 * Returns the value of the <code>name</code> property, or null if no such
	 * property exists.
	 */
	public abstract Object getObject(String name);

	/** Sets the value of the <code>name</code> property. */
	public abstract void setObject(String name, Object value);

	/** Returns the value of the <code>name</code> property.  If no such property
	 * exists, then <code>defaultValue</code> is returned.
	 */
	public abstract Object get(String name, Object defaultValue);

	/** Returns the value of the <code>name</code> property, or null if no
	 * such property exists. */
	public abstract String get(String name);

	/** Sets the value of the <code>name</code> property. */
	public abstract void set(String name, Object value);

	/** Returns the value of the <code>name</code> property.  If no such property
	 * exists, then <code>defaultValue</code> is returned.
	 */
	public abstract String get(String name, String defaultValue);

	/** Returns the value of the <code>name</code> property as an integer.  If no
	 * such property is specified, or if the specified value is not a valid
	 * integer, then <code>defaultValue</code> is returned.
	 */
	public abstract int getInt(String name, int defaultValue);

	/** Sets the value of the <code>name</code> property to an integer. */
	public abstract void setInt(String name, int value);

	/** Returns the value of the <code>name</code> property as a long.  If no
	 * such property is specified, or if the specified value is not a valid
	 * long, then <code>defaultValue</code> is returned.
	 */
	public abstract long getLong(String name, long defaultValue);

	/** Sets the value of the <code>name</code> property to a long. */
	public abstract void setLong(String name, long value);

	/** Returns the value of the <code>name</code> property as a float.  If no
	 * such property is specified, or if the specified value is not a valid
	 * float, then <code>defaultValue</code> is returned.
	 */
	public abstract float getFloat(String name, float defaultValue);

	/** Returns the value of the <code>name</code> property as an boolean.  If no
	 * such property is specified, or if the specified value is not a valid
	 * boolean, then <code>defaultValue</code> is returned.  Valid boolean values
	 * are "true" and "false".
	 */
	public abstract boolean getBoolean(String name, boolean defaultValue);

	/** Sets the value of the <code>name</code> property to an integer. */
	public abstract void setBoolean(String name, boolean value);

	/** Returns the value of the <code>name</code> property as an array of
	 * strings.  If no such property is specified, then <code>null</code>
	 * is returned.  Values are comma delimited.
	 */
	public abstract String[] getStrings(String name);

	/**
	 * Load a class by name.
	 * @param name the class name
	 * @return the class object
	 * @throws ClassNotFoundException if the class is not found
	 */
	public abstract Class getClassByName(String name)
			throws ClassNotFoundException;

	/** Returns the value of the <code>name</code> property as a Class.  If no
	 * such property is specified, then <code>defaultValue</code> is returned.
	 */
	public abstract Class getClass(String name, Class defaultValue);

	/** Returns the value of the <code>name</code> property as a Class.  If no
	 * such property is specified, then <code>defaultValue</code> is returned.
	 * An error is thrown if the returned class does not implement the named
	 * interface. 
	 */
	public abstract Class getClass(String propertyName, Class defaultValue,
			Class xface);

	/** Sets the value of the <code>name</code> property to the name of a class.
	 * First checks that the class implements the named interface. 
	 */
	public abstract void setClass(String propertyName, Class theClass,
			Class xface);

}