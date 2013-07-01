package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x21, name = "INSTANCE DUMP")
public class HeapInstanceDump extends HprofHeapRecord {

	private long objectID;

	private int stacktraceNo;

	private long classID;

	private byte[] fieldValues;

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		objectID = reader.readID();
		stacktraceNo = reader.readU4AsInt();
		classID = reader.readID();
		int fieldLength = reader.readU4AsInt();
		fieldValues = reader.readBytes(fieldLength);
		super.calcuateDataLength(reader);
	}

	public long getObjectID() {
		return objectID;
	}

	public int getStacktraceNo() {
		return stacktraceNo;
	}

	public long getClassID() {
		return classID;
	}

	public byte[] getFieldValues() {
		return fieldValues;
	}

	@Override
	public String toString() {
		return String
				.format("HeapInstanceDump [objectID=0x%08x, classID=0x%08x, fieldLength=%s]",
						objectID, classID, fieldValues.length);
	}

}
