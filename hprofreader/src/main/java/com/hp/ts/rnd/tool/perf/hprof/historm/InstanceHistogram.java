package com.hp.ts.rnd.tool.perf.hprof.historm;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.ts.rnd.tool.perf.hprof.HprofParser;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordVisitor;
import com.hp.ts.rnd.tool.perf.hprof.HprofUtils;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapClassDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapClassDump.StaticField;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapInstanceDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapObjectArrayDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapPrimitiveArrayDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofHeader;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofLoadClass;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofMultipleVisitors;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofProcessProxy;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofProcessTarget;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofStringAccess;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofStringAccess.StringSetter;

public class InstanceHistogram implements StringSetter {

	public static class InstanceHistogramEntry {
		String className;
		int instanceCount;
		long instanceTotalSize;
		int instanceSize;

		public String getClassName() {
			return className;
		}

		public int getInstanceCount() {
			return instanceCount;
		}

		public long getInstanceTotalSize() {
			return instanceTotalSize;
		}

		public int getInstanceSize() {
			return instanceSize;
		}

		@Override
		public String toString() {
			return String.format("%s\t%s\t%s", instanceCount,
					instanceTotalSize, className);
		}

	}

	private Map<Long, List<InstanceHistogramEntry>> nameEntries = new HashMap<Long, List<InstanceHistogramEntry>>();

	private Map<Long, InstanceHistogramEntry> classEntries = new HashMap<Long, InstanceHistogramEntry>();

	private Map<String, InstanceHistogramEntry> arrayEntries = new HashMap<String, InstanceHistogramEntry>();

	private InstanceHistogramEntry classEntry = new InstanceHistogramEntry();

	private int idSize;

	@HprofProcessTarget
	public void visitHeader(HprofHeader header) {
		this.idSize = header.getIdentifierSize();
	}

	@HprofProcessTarget
	public void visitLoadClass(HprofLoadClass loadClass) {
		long classNameID = loadClass.getClassNameID();
		long classID = loadClass.getClassID();
		InstanceHistogramEntry entry = new InstanceHistogramEntry();
		List<InstanceHistogramEntry> list = nameEntries.get(classNameID);
		if (list == null) {
			list = new ArrayList<InstanceHistogramEntry>(1);
			nameEntries.put(classNameID, list);
		}
		list.add(entry);
		classEntries.put(classID, entry);
	}

	@HprofProcessTarget
	public void visitClassDump(HeapClassDump classDump) {
		long classID = classDump.getClassID();
		InstanceHistogramEntry entry = classEntries.get(classID);
		entry.instanceSize = classDump.getInstanceSize();
		classEntry.instanceCount++;
		for (StaticField staticField : classDump.getStaticFields()) {
			classEntry.instanceTotalSize += HprofUtils.sizeOfPrimitiveOrObject(
					staticField.getEntryType(), idSize);
		}
		classEntry.instanceTotalSize += idSize * 2;
	}

	@HprofProcessTarget
	public void visitInstanceDump(HeapInstanceDump instanceDump) {
		long classID = instanceDump.getClassID();
		InstanceHistogramEntry entry = classEntries.get(classID);
		entry.instanceCount++;
		entry.instanceTotalSize += instanceDump.getFieldValues().length;
	}

	@HprofProcessTarget
	public void visitPrimitiveArrayDump(HeapPrimitiveArrayDump arrayDump) {
		byte arrayType = arrayDump.getElementType();
		String arrayName = "[" + HprofUtils.toPrimitiveCode(arrayType, true);
		InstanceHistogramEntry entry = arrayEntries.get(arrayName);
		if (entry == null) {
			entry = new InstanceHistogramEntry();
			arrayEntries.put(arrayName, entry);
		}
		entry.instanceCount++;
		entry.instanceTotalSize += arrayDump.getElementByteSize() + idSize * 2;
	}

	@HprofProcessTarget
	public void visitObjectArrayDump(HeapObjectArrayDump arrayDump) {
		long classID = arrayDump.getArrayClassID();
		InstanceHistogramEntry entry = classEntries.get(classID);
		entry.instanceCount++;
		// TODO -- refer to jhat result
		entry.instanceTotalSize += arrayDump.getElementIDs().length * idSize
				+ idSize * 2;
	}

	public void setUTF8Bytes(long id, byte[] utf8Bytes) {
		List<InstanceHistogramEntry> list = nameEntries.get(id);
		if (list != null) {
			String className = HprofUtils.utf8ToString(utf8Bytes);
			String javaClassName = HprofUtils.toJavaClassName(className);
			if (HprofUtils.isPrimitiveArrayName(className)) {
				InstanceHistogramEntry arrayEntry = arrayEntries.get(className);
				for (InstanceHistogramEntry entry : list) {
					entry.className = javaClassName;
					entry.instanceCount = arrayEntry.instanceCount;
					entry.instanceTotalSize = arrayEntry.instanceTotalSize;
					entry.instanceSize = arrayEntry.instanceSize;
				}
			} else {
				for (InstanceHistogramEntry entry : list) {
					entry.className = javaClassName;
				}
				if (javaClassName.equals("java.lang.Class")) {
					for (InstanceHistogramEntry entry : list) {
						entry.className = javaClassName;
						entry.instanceCount += classEntry.instanceCount;
						entry.instanceTotalSize += classEntry.instanceTotalSize;
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		List<InstanceHistogramEntry> entries = list("heap.bin");
		for (InstanceHistogramEntry entry : entries) {
			if (entry.instanceCount > 0) {
				System.out.println(entry);
			}
		}
	}

	private static void parse(String fileName, HprofRecordVisitor visitor,
			long skipMask) throws Exception {
		DataInputStream input = new DataInputStream(new BufferedInputStream(
				new FileInputStream(fileName), 1024 * 1024));
		HprofParser parser = new HprofParser(input);
		try {
			parser.parse(visitor, skipMask);
		} finally {
			parser.close();
		}
	}

	public static List<InstanceHistogramEntry> list(String fileName)
			throws Exception {
		InstanceHistogram histogram = new InstanceHistogram();
		parse(fileName, new HprofMultipleVisitors(new HprofProcessProxy(
				histogram)), HprofRecordType.STRINGS_IN_UTF8.mask());
		parse(fileName, new HprofMultipleVisitors(new HprofProcessProxy(
				new HprofStringAccess(histogram))),
				~HprofRecordType.STRINGS_IN_UTF8.mask());
		List<InstanceHistogramEntry> entries = new ArrayList<InstanceHistogramEntry>();
		for (List<InstanceHistogramEntry> list : histogram.nameEntries.values()) {
			entries.addAll(list);
		}
		Collections.sort(entries, new Comparator<InstanceHistogramEntry>() {

			public int compare(InstanceHistogramEntry p1,
					InstanceHistogramEntry p2) {
				long x = p1.instanceTotalSize;
				long y = p2.instanceTotalSize;
				return (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		Collections.reverse(entries);
		return entries;
	}

}
