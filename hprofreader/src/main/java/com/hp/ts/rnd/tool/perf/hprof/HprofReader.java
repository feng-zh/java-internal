package com.hp.ts.rnd.tool.perf.hprof;

public interface HprofReader {

	public HprofRecord read() throws HprofException;

	public long getPosition() throws HprofException;

}
