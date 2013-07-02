package com.hp.ts.rnd.tool.perf.hprof.visitor;

import com.hp.ts.rnd.tool.perf.hprof.HprofHeapRecordReader;

public class HprofHeapSkipAccess {

	@HprofProcessTarget
	public void skipHeapRecord(HprofHeapRecordReader heapRecord) {
		heapRecord.skip();
	}
}
