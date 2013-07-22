package com.hp.ts.rnd.tool.perf.hprof;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.hp.ts.rnd.tool.perf.hprof.record.HprofHeader;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofHeapDumpEnd;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofLoadClass;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofRootRecord;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofStackFrame;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofStackTrace;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofUTF8;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofUnloadClass;
import com.hp.ts.rnd.tool.perf.hprof.record.UnknownHprofRecod;

/* Based on https://java.net/downloads/heap-snapshot/hprof-binary-format.html */
public class HprofParser implements HprofReader, Closeable {

	private HprofRecordReader reader;
	private HprofHeader header;
	private RootRecordDef<?>[] rootRecordDefs = new RootRecordDef<?>[256];
	private DataInputStream input;

	private static class RootRecordDef<T extends HprofRootRecord> {
		Class<T> recordDefClass;
		T recordInstance;

		public RootRecordDef(Class<T> clasz) {
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
		rootRecordDefs[HprofRecordType.HEAP_DUMP.tagValue()] = new RootRecordDef<HprofHeapReader>(
				HprofHeapReader.class);
		rootRecordDefs[HprofRecordType.HEAP_DUMP_SEGMENT.tagValue()] = new RootRecordDef<HprofHeapReader>(
				HprofHeapReader.class);
		addRecordDef(HprofHeader.class);
		addRecordDef(HprofUTF8.class);
		addRecordDef(HprofLoadClass.class);
		addRecordDef(HprofUnloadClass.class);
		addRecordDef(HprofStackTrace.class);
		addRecordDef(HprofStackFrame.class);
		addRecordDef(HprofHeapDumpEnd.class);
	}

	private <T extends HprofRootRecord> void addRecordDef(Class<T> recordDef) {
		int tagType = HprofRecord.getTagTypeByClass(recordDef);
		if (rootRecordDefs[tagType] != null) {
			throw new IllegalArgumentException("duplicate record type "
					+ tagType + " on "
					+ rootRecordDefs[tagType].getRecordClass() + " and "
					+ recordDef);
		}
		rootRecordDefs[tagType] = new RootRecordDef<T>(recordDef);
	}

	public HprofRecord read() throws HprofException {
		HprofRecord record = readRootRecordHeader();
		if (record != null) {
			record.readFields(reader);
		}
		return record;
	}

	private HprofRootRecord readRootRecordHeader() throws HprofException {
		if (header == null) {
			HprofRecord h = new HprofHeader();
			// header
			h.readHeaders(0, reader);
			h.readFields(reader);
			header = (HprofHeader) h;
			reader.setHeader(header);
			return header;
		}
		int tagValue = reader.readByte();
		if (tagValue == -1) {
			return null;
		}
		RootRecordDef<?> recordClass = rootRecordDefs[tagValue];
		if (recordClass == null) {
			HprofRootRecord unknown = new UnknownHprofRecod();
			((HprofRecord) unknown).readHeaders(tagValue, reader);
			return unknown;
		}
		HprofRootRecord record = recordClass.getInstance();
		((HprofRecord) record).readHeaders(tagValue, reader);
		return record;
	}

	public void parse(HprofRecordVisitor visitor, long skipMask) {
		HprofRootRecord record;
		long position = reader.getPosition();
		while ((record = readRootRecordHeader()) != null) {
			if (record.isSkip(skipMask)) {
				reader.skip(record.getDataLength());
			} else {
				long startField = reader.getPosition();
				((HprofRecord) record).readFields(reader);
				long loaded = reader.getPosition() - startField;
				if (record instanceof HprofReader) {
					HprofRecordVisitor anotherVisitor = visitor
							.visitCompositeRecord(record, position);
					HprofReader subReader = (HprofReader) record;
					position = reader.getPosition();
					HprofRecord subRecord;
					if (anotherVisitor != null) {
						while ((subRecord = subReader.read()) != null) {
							anotherVisitor.visitSingleRecord(subRecord,
									position);
							position = reader.getPosition();
						}
					} else {
						reader.skip(record.getDataLength() - loaded);
					}
				} else {
					visitor.visitSingleRecord(record, position);
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
			parser.parse(
					new HprofRecordVisitor() {

						public void visitSingleRecord(HprofRecord record,
								long position) {
							System.out.println("" + record.getTagName() + "("
									+ record.getLength() + ")@" + position
									+ ": " + record);
						}

						public HprofRecordVisitor visitCompositeRecord(
								HprofRecord record, long position) {
							System.out.println("" + record.getTagName() + "("
									+ record.getLength() + ")@" + position
									+ ": " + record);
							return null;
						}
					},
					(HprofRecordType.HEAP_DUMP.mask()
							| HprofRecordType.HEAP_DUMP_SEGMENT.mask() | HprofRecordType.HEAP_DUMP_END
							.mask()));
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
