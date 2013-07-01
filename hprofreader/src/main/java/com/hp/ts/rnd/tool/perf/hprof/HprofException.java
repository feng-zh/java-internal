package com.hp.ts.rnd.tool.perf.hprof;

public class HprofException extends RuntimeException {

	private static final long serialVersionUID = 9137170795462211688L;

	public HprofException(Throwable cause) {
		super(cause);
	}

	public HprofException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public HprofException(String message) {
		super(message);
	}

}
