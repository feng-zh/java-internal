package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x05, name = "ROOT STICKY CLASS")
// System class
public class HeapRootStickyClass extends HprofHeapRecord {

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
		return String.format("HeapRootStickyClass [objectID=0x%08x]", objectID);
	}

}
