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

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader)
			throws HprofException {
		reader.beginTag();
	}

	protected void calcuateDataLength(HprofRecordReader reader) {
		dataLength = reader.endTag();
	}

}
