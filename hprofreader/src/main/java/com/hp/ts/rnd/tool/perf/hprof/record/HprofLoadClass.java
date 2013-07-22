package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(HprofRecordType.LOAD_CLASS)
public class HprofLoadClass extends HprofRootRecord {

	private int classNo;

	private long classID;

	private int stacktraceNo;

	private long classNameID;

	protected void readRecord(HprofRecordReader reader) {
		classNo = reader.readU4AsInt();
		classID = reader.readID();
		stacktraceNo = reader.readU4AsInt();
		classNameID = reader.readID();
	}

	public int getClassNo() {
		return classNo;
	}

	public long getClassID() {
		return classID;
	}

	public int getStacktraceNo() {
		return stacktraceNo;
	}

	public long getClassNameID() {
		return classNameID;
	}

	@Override
	public String toString() {
		return String
				.format("LoadClass [classNo=%s, classID=0x%08x, stacktraceNo=%s, classNameID=0x%08x]",
						classNo, classID, stacktraceNo, classNameID);
	}

}
