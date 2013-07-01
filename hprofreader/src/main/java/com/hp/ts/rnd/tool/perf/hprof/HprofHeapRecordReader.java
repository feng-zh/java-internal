package com.hp.ts.rnd.tool.perf.hprof;

import com.hp.ts.rnd.tool.perf.hprof.record.HeapClassDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapInstanceDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapObjectArrayDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapPrimitiveArrayDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootJavaFrame;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootJniGlobal;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootJniLocal;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootMonitorUsed;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootNativeStack;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootStickyClass;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootThreadBlock;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootThreadObject;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapRootUnknown;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofRootRecord;

public class HprofHeapRecordReader extends HprofRootRecord implements
		HprofReader {

	private HprofRecordReader reader;

	private RecordDef<?>[] recordDefs = new RecordDef<?>[256];

	private boolean eof;

	private long loaded;

	private static class RecordDef<T extends HprofRecord> {
		Class<T> recordDefClass;
		T recordInstance;

		public RecordDef(Class<T> clasz) {
			this.recordDefClass = clasz;
		}

		public T getInstance() {
			if (recordInstance == null) {
				try {
					recordInstance = recordDefClass.newInstance();
				} catch (Exception e) {
					throw new HprofException(e);
				}
			}
			return recordInstance;
		}

		public Class<T> getRecordClass() {
			return recordDefClass;
		}
	}

	private int tagValue;

	public static final int TAG_HEAP_DUMP = 0x0C;
	public static final int TAG_HEAP_DUMP_SEGMENT = 0x1C;

	public HprofHeapRecordReader() {
		initialRecordDefs();
	}

	private void initialRecordDefs() {
		addRecordDef(HeapRootUnknown.class);
		addRecordDef(HeapClassDump.class);
		addRecordDef(HeapObjectArrayDump.class);
		addRecordDef(HeapInstanceDump.class);
		addRecordDef(HeapPrimitiveArrayDump.class);
		addRecordDef(HeapRootThreadObject.class);
		addRecordDef(HeapRootJavaFrame.class);
		addRecordDef(HeapRootMonitorUsed.class);
		addRecordDef(HeapRootJniGlobal.class);
		addRecordDef(HeapRootJniLocal.class);
		addRecordDef(HeapRootNativeStack.class);
		addRecordDef(HeapRootThreadBlock.class);
		addRecordDef(HeapRootStickyClass.class);
	}

	private <T extends HprofRecord> void addRecordDef(Class<T> recordDef) {
		int tagType = HprofRecord.getTagTypeByClass(recordDef);
		if (recordDefs[tagType] != null) {
			throw new IllegalArgumentException("duplicate record type "
					+ tagType + " on " + recordDefs[tagType].getRecordClass()
					+ " and " + recordDef);
		}
		recordDefs[tagType] = new RecordDef<T>(recordDef);
	}

	@Override
	public int getTagValue() {
		return tagValue;
	}

	@Override
	public String getTagName() {
		int tag = getTagValue();
		if (tag == TAG_HEAP_DUMP) {
			return "HEAP DUMP";
		} else if (tag == TAG_HEAP_DUMP_SEGMENT) {
			return "HEAP DUMP SEGMENT";
		} else {
			throw new IllegalArgumentException("invalid tag: " + tag);
		}
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader)
			throws HprofException {
		super.readFields(tagValue, reader);
		this.tagValue = tagValue;
		this.reader = reader;
		this.eof = false;
		this.loaded = 0;
		reader.limit(getDataLength());
	}

	public HprofRecord read() throws HprofException {
		if (eof) {
			return null;
		}
		int tagValue = reader.readByte();
		if (tagValue == -1) {
			// reset limit
			reader.limit(0);
			eof = true;
			return null;
		}
		RecordDef<?> recordDef = recordDefs[tagValue];
		if (recordDef == null) {
			throw new IllegalArgumentException(String.format(
					"unknown heap sub tag 0x%02x, at position %s", tagValue,
					reader.getPosition() - 1));
		}
		HprofRecord instance = recordDef.getInstance();
		instance.readFields(tagValue, reader);
		loaded += instance.getLength();
		return instance;
	}

	public long getPosition() throws HprofException {
		return reader.getPosition();
	}

	@Override
	public void skip() {
		long len = getDataLength() - loaded;
		reader.skip(len);
		System.out.println("skip: " + len);
	}
}
