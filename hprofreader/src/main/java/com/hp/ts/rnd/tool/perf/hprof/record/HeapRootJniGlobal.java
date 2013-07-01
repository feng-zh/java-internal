package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x01, name = "ROOT JNI GLOBAL")
public class HeapRootJniGlobal extends HprofHeapRecord {

	private long objectID;

	private long jniGlobalRefID;

	public long getObjectID() {
		return objectID;
	}

	public long getJniGlobalRefID() {
		return jniGlobalRefID;
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		objectID = reader.readID();
		jniGlobalRefID = reader.readID();
		super.calcuateDataLength(reader);
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootJniGlobal [objectID=0x%08x, jniGlobalRefID=0x%08x]",
				objectID, jniGlobalRefID);
	}

}
