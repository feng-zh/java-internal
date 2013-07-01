package com.hp.ts.rnd.tool.perf.hprof;

import java.io.IOException;

public class HprofIOException extends HprofException {

	private static final long serialVersionUID = 1228583657791258910L;

	public HprofIOException(IOException cause) {
		super(cause);
	}

	public HprofIOException(String msg, IOException cause) {
		super(msg, cause);
	}

	public IOException getIOException() {
		return (IOException) getCause();
	}

}
