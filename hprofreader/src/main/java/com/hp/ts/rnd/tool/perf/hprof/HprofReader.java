package com.hp.ts.rnd.tool.perf.hprof;

public interface HprofReader {

	public void accept(HprofRecordVisitor visitor, long skipMask)
			throws HprofException;

}
