package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x21, alias = "INSTANCE DUMP", value = HprofRecordType.HEAP_DUMP)
public class HeapInstanceDump extends HprofHeapRecord {

	private long objectID;

	private int stacktraceNo;

	private long classID;

	private byte[] fieldValues;

	@Override
	protected void readRecord(HprofRecordReader reader) {
		objectID = reader.readID();
		stacktraceNo = reader.readU4AsInt();
		classID = reader.readID();
		int fieldLength = reader.readU4AsInt();
		fieldValues = reader.readBytes(fieldLength);
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
