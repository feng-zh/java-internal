package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x02, name = "ROOT JNI LOCAL")
public class HeapRootJniLocal extends HprofHeapRecord {
	private long objectID;

	private int threadNo;

	private int frameNo;

	public long getObjectID() {
		return objectID;
	}

	public int getThreadNo() {
		return threadNo;
	}

	public int getFrameNo() {
		return frameNo;
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		objectID = reader.readID();
		threadNo = reader.readU4AsInt();
		frameNo = reader.readU4AsInt();
		super.calcuateDataLength(reader);
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootJniLocal [objectID=0x%08x, threadNo=%s, frameNo=%s]",
				objectID, threadNo, frameNo);
	}

}
