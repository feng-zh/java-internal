package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x22, alias = "OBJECT ARRAY DUMP", value = HprofRecordType.HEAP_DUMP)
public class HeapObjectArrayDump extends HprofHeapRecord {

	private long arrayID;

	private int stacktraceNo;

	private long[] elementIDs;

	private long arrayClassID;

	public long getArrayID() {
		return arrayID;
	}

	public long getStacktraceNo() {
		return stacktraceNo;
	}

	public long[] getElementIDs() {
		return elementIDs;
	}

	public long getArrayClassID() {
		return arrayClassID;
	}

	@Override
	protected void readRecord(HprofRecordReader reader) {
		arrayID = reader.readID();
		stacktraceNo = reader.readU4AsInt();
		int count = reader.readU4AsInt();
		arrayClassID = reader.readID();
		elementIDs = new long[count];
		for (int i = 0; i < count; i++) {
			elementIDs[i] = reader.readID();
		}
	}

	@Override
	public String toString() {
		return String
				.format("HeapObjectArrayDump [arrayID=0x%08x, arrayClassID=0x%08x, elementCount=%s]",
						arrayID, arrayClassID, elementIDs.length);
	}

}
