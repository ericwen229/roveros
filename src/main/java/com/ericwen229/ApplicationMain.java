package com.ericwen229;

import com.ericwen229.util.Config;

public class ApplicationMain {

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new RuntimeException(String.format("Incorrect argument number. %d received but 1 expected.", args.length
			));
		}

		Config.loadConfig(args[0]);
	}

}
