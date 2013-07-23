package com.hp.ts.rnd.tool.perf.hprof;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

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
public class HprofParser implements HprofRandomAccess, HprofReader, Closeable {

	private HprofRecordReader reader;
	private HprofHeader header;
	private RootRecordDef<?>[] rootRecordDefs = new RootRecordDef<?>[256];

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
		this.reader = new HprofRecordReader(input);
		initialRecordDefs();
	}

	public HprofParser(RandomAccessFile input) {
		this.reader = new HprofRecordReader(input);
		initialRecordDefs();
		readHeader();
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

	private HprofRootRecord readRootRecordHeader() throws HprofException {
		if (header == null) {
			readHeader();
			return header;
		}
		int tagValue = reader.read();
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

	private void readHeader() {
		if (header == null) {
			HprofRecord h = new HprofHeader();
			// header
			h.readHeaders(0, reader);
			h.readFields(reader);
			header = (HprofHeader) h;
			reader.setHeader(header);
		}
	}

	public void accept(HprofRecordVisitor visitor, long skipMask) {
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
					if (anotherVisitor != null) {
						subReader.accept(anotherVisitor, skipMask);
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
		reader.close();
	}

	public static void main(String[] args) throws Exception {
		File file = new File("heap.bin");
		DataInputStream input = new DataInputStream(new BufferedInputStream(
				new FileInputStream(file), 1024 * 1024));
		HprofParser parser = new HprofParser(input);
		final AtomicLong pos = new AtomicLong();
		try {
			parser.accept(
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
							pos.set(position);
							return null;
						}
					},
					~(HprofRecordType.HEAP_DUMP.mask()
							| HprofRecordType.HEAP_DUMP_SEGMENT.mask() | HprofRecordType.HEAP_DUMP_END
							.mask()));
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			parser.close();
		}
		// try random access file
		HprofParser reader = new HprofParser(new RandomAccessFile(file, "r"));
		try {
			long count = reader.read(pos.get(), new HprofRecordVisitor() {

				public void visitSingleRecord(HprofRecord record, long position) {
					System.out.println("" + record.getTagName() + "("
							+ record.getLength() + ")@" + position + ": "
							+ record);
				}

				public HprofRecordVisitor visitCompositeRecord(
						HprofRecord record, long position) {
					System.out.println("" + record.getTagName() + "("
							+ record.getLength() + ")@" + position + ": "
							+ record);
					return null;
				}
			});
			System.out.println("Read count: " + count);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			reader.close();
		}
	}

	public long read(long position, HprofRecordVisitor visitor)
			throws HprofException {
		if (reader.isRandomAccess()) {
			reader.setPosition(position);
		}
		HprofRootRecord record = readRootRecordHeader();
		long startField = reader.getPosition();
		((HprofRecord) record).readFields(reader);
		long loaded = reader.getPosition() - startField;
		if (record instanceof HprofReader) {
			HprofRecordVisitor anotherVisitor = visitor.visitCompositeRecord(
					record, position);
			HprofReader subReader = (HprofReader) record;
			if (anotherVisitor != null) {
				subReader.accept(anotherVisitor, 0);
			} else {
				reader.skip(record.getDataLength() - loaded);
			}
		} else {
			visitor.visitSingleRecord(record, position);
		}
		return reader.getPosition() - position;
	}

}
