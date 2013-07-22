package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x01, alias = "ROOT JNI GLOBAL", value = HprofRecordType.HEAP_DUMP)
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
	protected void readRecord(HprofRecordReader reader) {
		objectID = reader.readID();
		jniGlobalRefID = reader.readID();
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootJniGlobal [objectID=0x%08x, jniGlobalRefID=0x%08x]",
				objectID, jniGlobalRefID);
	}

}
