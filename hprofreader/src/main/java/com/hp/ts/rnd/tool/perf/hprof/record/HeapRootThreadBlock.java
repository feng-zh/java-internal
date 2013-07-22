package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x06, alias = "ROOT THREAD BLOCK", value = HprofRecordType.HEAP_DUMP)
public class HeapRootThreadBlock extends HprofHeapRecord {

	private long objectID;

	private int threadNo;

	public long getObjectID() {
		return objectID;
	}

	public int getThreadNo() {
		return threadNo;
	}

	@Override
	protected void readRecord(HprofRecordReader reader) {
		objectID = reader.readID();
		threadNo = reader.readU4AsInt();
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootThreadBlock [objectID=0x%08x, threadNo=%s]", objectID,
				threadNo);
	}

}
