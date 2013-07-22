package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x02, alias = "ROOT JNI LOCAL", value = HprofRecordType.HEAP_DUMP)
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
	protected void readRecord(HprofRecordReader reader) {
		objectID = reader.readID();
		threadNo = reader.readU4AsInt();
		frameNo = reader.readU4AsInt();
	}

	@Override
	public String toString() {
		return String.format(
				"HeapRootJniLocal [objectID=0x%08x, threadNo=%s, frameNo=%s]",
				objectID, threadNo, frameNo);
	}

}
