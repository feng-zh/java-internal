package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofException;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;

public abstract class HprofRootRecord extends HprofRecord {

	private long dataLength;
	private long recordTime;

	@Override
	public long getDataLength() {
		return dataLength;
	}

	@Override
	public long getLength() {
		return dataLength + 1 + 4 + 4;
	}

	public long getRecordTime() {
		return recordTime;
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader)
			throws HprofException {
		long rtime = reader.readU4AsLong();
		recordTime = reader.convertTime(rtime);
		dataLength = reader.readU4AsLong();
	}
	
	@Override
	public String toString() {
		return String.format("%s [tagValue=0x%02x, dataLength=%s]", getClass()
				.getSimpleName(), getTagValue(), getDataLength());
	}

}
