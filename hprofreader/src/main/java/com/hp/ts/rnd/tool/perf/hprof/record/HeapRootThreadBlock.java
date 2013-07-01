package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x06, name = "ROOT THREAD BLOCK")
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
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		objectID = reader.readID();
		threadNo = reader.readU4AsInt();
		super.calcuateDataLength(reader);
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootThreadBlock [objectID=0x%08x, threadNo=%s]", objectID,
				threadNo);
	}

}
