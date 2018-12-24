package com.ericwen229.util;

import lombok.Getter;
import lombok.Setter;

public class Pair<U, V> {

	@Getter
	@Setter
	private U car;

	@Getter
	@Setter
	private V cdr;

	public Pair(U car, V cdr) {
		this.car = car;
		this.cdr = cdr;
	}

}
