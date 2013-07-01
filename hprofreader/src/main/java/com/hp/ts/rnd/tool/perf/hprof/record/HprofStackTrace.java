package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x05, name = "STACK TRACE")
public class HprofStackTrace extends HprofRootRecord {

	private int stacktraceNo;

	private int threadNo;

	private int framesCount;

	private long[] framesIDs;

	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		stacktraceNo = reader.readU4AsInt();
		threadNo = reader.readU4AsInt();
		framesCount = reader.readU4AsInt();
		framesIDs = new long[framesCount];
		for (int i = 0; i < framesCount; i++) {
			framesIDs[i] = reader.readID();
		}
	}

	public int getStacktraceNo() {
		return stacktraceNo;
	}

	public int getThreadNo() {
		return threadNo;
	}

	public int getFramesCount() {
		return framesCount;
	}

	public long[] getFramesIDs() {
		return framesIDs;
	}

	@Override
	public String toString() {
		return String
				.format("HprofStackTrace [stacktraceNo=%s, threadNo=%s, framesCount=%s]",
						stacktraceNo, threadNo, framesCount);
	}

}
