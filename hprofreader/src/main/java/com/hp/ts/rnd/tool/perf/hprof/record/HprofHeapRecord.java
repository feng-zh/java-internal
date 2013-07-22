package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofException;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;

public abstract class HprofHeapRecord extends HprofRecord {

	private int dataLength;

	@Override
	public long getLength() {
		return dataLength + 1;
	}

	@Override
	public long getDataLength() {
		return dataLength;
	}

	final protected void readHeaders(int tagValue, HprofRecordReader reader)
			throws HprofException {
		reader.beginTag();
	}

	final protected void readFields(HprofRecordReader reader) {
		readRecord(reader);
		dataLength = reader.endTag();
	}

	protected abstract void readRecord(HprofRecordReader reader);

	// not support skip
	protected boolean isSkip(long skipMask) {
		return false;
	}

}
