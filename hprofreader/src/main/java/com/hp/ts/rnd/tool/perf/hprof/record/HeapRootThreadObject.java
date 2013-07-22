package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x08, alias = "ROOT THREAD OBJECT", value = HprofRecordType.HEAP_DUMP)
public class HeapRootThreadObject extends HprofHeapRecord {

	private long objectID;

	private int threadNo;

	private int stacktraceNo;

	public long getObjectID() {
		return objectID;
	}

	public int getThreadNo() {
		return threadNo;
	}

	public int getStacktraceNo() {
		return stacktraceNo;
	}

	@Override
	protected void readRecord(HprofRecordReader reader) {
		objectID = reader.readID();
		threadNo = reader.readU4AsInt();
		stacktraceNo = reader.readU4AsInt();
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootThreadObject [objectID=0x%08x, threadNo=%s]",
				objectID, threadNo);
	}

}
