package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0xFF, alias = "ROOT UNKNOWN", value = HprofRecordType.HEAP_DUMP)
public class HeapRootUnknown extends HprofHeapRecord {

	private long objectID;

	public long getObjectID() {
		return objectID;
	}

	@Override
	protected void readRecord(HprofRecordReader reader) {
		objectID = reader.readID();
	}

	@Override
	public String toString() {
		return String.format("HeapRootUnknown [objectID=0x%08x]", objectID);
	}

}
