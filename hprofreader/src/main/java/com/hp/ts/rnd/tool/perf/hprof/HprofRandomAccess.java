package com.hp.ts.rnd.tool.perf.hprof;

import java.io.Closeable;

public interface HprofRandomAccess extends Closeable {

	public long read(long position, HprofRecordVisitor visitor)
			throws HprofException;

}
