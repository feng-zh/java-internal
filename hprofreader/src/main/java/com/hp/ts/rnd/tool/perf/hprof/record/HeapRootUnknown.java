package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0xFF, name = "ROOT UNKNOWN")
public class HeapRootUnknown extends HprofHeapRecord {

	private long objectID;

	public long getObjectID() {
		return objectID;
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		objectID = reader.readID();
		super.calcuateDataLength(reader);
	}

	@Override
	public String toString() {
		return String.format("HeapRootUnknown [objectID=0x%08x]", objectID);
	}

}
