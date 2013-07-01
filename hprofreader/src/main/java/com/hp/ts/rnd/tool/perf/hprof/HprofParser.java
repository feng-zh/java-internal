package com.hp.ts.rnd.tool.perf.hprof;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.hp.ts.rnd.tool.perf.hprof.record.HprofHeader;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofHeapDumpEnd;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofLoadClass;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofStackFrame;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofStackTrace;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofUTF8;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofUnloadClass;
import com.hp.ts.rnd.tool.perf.hprof.record.UnknownHprofRecod;

public class HprofParser implements HprofReader, Closeable {

	private HprofRecordReader reader;
	private HprofHeader header;
	private RecordDef<?>[] recordDefs = new RecordDef<?>[256];
	private DataInputStream input;

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

	public HprofParser(DataInputStream input) {
		this.input = input;
		this.reader = new HprofRecordReader(input);
		initialRecordDefs();
	}

	private void initialRecordDefs() {
		recordDefs[HprofHeapRecordReader.TAG_HEAP_DUMP] = new RecordDef<HprofHeapRecordReader>(
				HprofHeapRecordReader.class);
		recordDefs[HprofHeapRecordReader.TAG_HEAP_DUMP_SEGMENT] = new RecordDef<HprofHeapRecordReader>(
				HprofHeapRecordReader.class);
		addRecordDef(HprofHeader.class);
		addRecordDef(HprofUTF8.class);
		addRecordDef(HprofLoadClass.class);
		addRecordDef(HprofUnloadClass.class);
		addRecordDef(HprofStackTrace.class);
		addRecordDef(HprofStackFrame.class);
		addRecordDef(HprofHeapDumpEnd.class);
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

	public HprofRecord read() throws HprofException {
		if (header == null) {
			HprofRecord h = new HprofHeader();
			// header
			h.readFields(0, reader);
			header = (HprofHeader) h;
			reader.setHeader(header);
			return header;
		}
		int tagValue = reader.readByte();
		if (tagValue == -1) {
			return null;
		}
		RecordDef<?> recordClass = recordDefs[tagValue];
		if (recordClass == null) {
			HprofRecord unknown = new UnknownHprofRecod();
			unknown.readFields(tagValue, reader);
			return unknown;
		}
		HprofRecord record = recordClass.getInstance();
		record.readFields(tagValue, reader);
		return record;
	}

	public void parse(HprofRecordVisitor visitor) {
		HprofRecord record;
		long position = reader.getPosition();
		while ((record = read()) != null) {
			visitor.visitRecord(record, null, position);
			if (record instanceof HprofReader) {
				HprofRecord parent = record;
				HprofReader subReader = (HprofReader) record;
				position = reader.getPosition();
				while ((record = subReader.read()) != null) {
					visitor.visitRecord(record, parent, position);
					position = reader.getPosition();
				}
			}
			position = reader.getPosition();
		}
	}

	public void close() throws IOException {
		input.close();
	}

	public static void main(String[] args) throws Exception {
		DataInputStream input = new DataInputStream(new BufferedInputStream(
				new FileInputStream("heap.bin"), 1024 * 1024));
		HprofParser parser = new HprofParser(input);
		try {
			parser.parse(new HprofRecordVisitor() {

				public void visitRecord(HprofRecord record, HprofRecord parent,
						long position) {
					if (parent == null
							&& (record instanceof HprofHeader
									|| record instanceof HprofHeapRecordReader
									|| record instanceof HprofHeapDumpEnd || record instanceof UnknownHprofRecod)) {
						System.out.println((parent != null ? "\t" : "")
								+ record.getTagName() + "("
								+ record.getLength() + ")@" + position + ": "
								+ record);
						record.skip();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			parser.close();
		}
	}

	public long getPosition() throws HprofException {
		return reader.getPosition();
	}

}
