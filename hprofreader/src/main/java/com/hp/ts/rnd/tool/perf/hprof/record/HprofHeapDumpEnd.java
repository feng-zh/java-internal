package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofException;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(HprofRecordType.HEAP_DUMP_END)
public class HprofHeapDumpEnd extends HprofRootRecord {

	@Override
	protected void readRecord(HprofRecordReader reader) throws HprofException {
		// no-op;
	}
}
