package com.ericwen229.example;

import org.ros.internal.message.Message;

/**
 * An example of custom message definition.
 */
public interface CustomMsg extends Message {
	/**
	 * Current class name. Will be used by classloader.
	 */
	String _TYPE = "com.ericwen229.sample/CustomMsg";

	/**
	 * Fields that will be parsed and generated automatically.
	 */
	String _DEFINITION = "# foo bar\nstring foo\nint32 bar\n"; // fields

	/**
	 * Getter of field foo.
	 *
	 * @return value of field foo
	 */
	String getFoo();

	/**
	 * Setter of field foo.
	 *
	 * @param val new value of field foo
	 */
	void setFoo(String val);

	/**
	 * Getter of field bar.
	 *
	 * @return value of field bar
	 */
	int getBar();

	/**
	 * Setter of field bar.
	 *
	 * @param val new value of field bar
	 */
	void setBar(int val);
}