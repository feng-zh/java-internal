package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x03, name = "UNLOAD CLASS")
public class HprofUnloadClass extends HprofRootRecord {

	private long classNo;

	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		classNo = reader.readU4AsLong();
	}

	public long getClassNo() {
		return classNo;
	}

	@Override
	public String toString() {
		return String.format("HprofUnloadClass [classNo=%s]", classNo);
	}

}
