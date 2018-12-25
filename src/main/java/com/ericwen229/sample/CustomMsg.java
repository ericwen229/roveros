package com.ericwen229.sample;

import org.ros.internal.message.Message;

public interface CustomMsg extends Message {
	String _TYPE = "com.ericwen229.sample/CustomMsg";
	String _DEFINITION = "# foo bar\nstring foo\nint32 bar\n";

	String getFoo();
	void setFoo(String val);
	int getBar();
	void setBar(int val);
}