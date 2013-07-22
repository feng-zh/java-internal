package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(HprofRecordType.UNLOAD_CLASS)
public class HprofUnloadClass extends HprofRootRecord {

	private long classNo;

	protected void readRecord(HprofRecordReader reader) {
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
