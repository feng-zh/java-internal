package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x08, name = "ROOT THREAD OBJECT")
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
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		objectID = reader.readID();
		threadNo = reader.readU4AsInt();
		stacktraceNo = reader.readU4AsInt();
		super.calcuateDataLength(reader);
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootThreadObject [objectID=0x%08x, threadNo=%s]",
				objectID, threadNo);
	}

}
