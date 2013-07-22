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
import java.util.Map.Entry;

import com.hp.ts.rnd.tool.perf.hprof.HprofParser;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordVisitor;
import com.hp.ts.rnd.tool.perf.hprof.HprofUtils;
import com.hp.ts.rnd.tool.perf.hprof.record.HeapClassDump;
import com.hp.ts.rnd.tool.perf.hprof.record.HprofLoadClass;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofMultipleVisitors;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofProcessProxy;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofProcessTarget;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofStringAccess;
import com.hp.ts.rnd.tool.perf.hprof.visitor.HprofStringAccess.StringSetter;

public class DuplicatClass implements StringSetter {

	private Map<Object, List<Long>> classNameIdMap = new HashMap<Object, List<Long>>();
	private static int count;

	public static class DuplicatClassEntry {
		String className;
		int classCount;

		public String getClassName() {
			return className;
		}

		@Override
		public String toString() {
			return String.format("%d\t%s", classCount, className);
		}

	}

	@HprofProcessTarget
	public void visitLoadClass(HprofLoadClass loadClass) {
		// graphDB.addEdge(String, loadClass.getClassNameID(), ClassID,
		// loadClass.getClassID());
		List<Long> classIDList = classNameIdMap.get(loadClass.getClassNameID());
		if (classIDList == null) {
			classIDList = new ArrayList<Long>(1);
			classNameIdMap.put(loadClass.getClassNameID(), classIDList);
		}
		classIDList.add(loadClass.getClassID());
	}

	@HprofProcessTarget
	public void visitClassDump(HeapClassDump classDump) {
		long classID = classDump.getClassID();
		count++;
	}

	public void setUTF8Bytes(long id, byte[] utf8Bytes) {
		// graphDB.retrieveNode(String, id).setValue(name);
		List<Long> classIDList = classNameIdMap.remove(id);
		if (classIDList != null) {
			DuplicatClassEntry entry = new DuplicatClassEntry();
			entry.className = HprofUtils.toJavaClassName(HprofUtils
					.utf8ToString(utf8Bytes));
			entry.classCount = classIDList.size();
			classNameIdMap.put(entry, classIDList);
		}
	}

	public static void main(String[] args) throws Exception {
		List<DuplicatClassEntry> entries = list("heap.bin");
		for (DuplicatClassEntry entry : entries) {
			System.out.println(entry);
		}
		System.out.println(count + " <=> " + entries.size());
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

	public static List<DuplicatClassEntry> list(String fileName)
			throws Exception {
		DuplicatClass histogram = new DuplicatClass();
		parse(fileName, new HprofMultipleVisitors(new HprofProcessProxy(
				histogram)), HprofRecordType.STRINGS_IN_UTF8.mask());
		parse(fileName, new HprofMultipleVisitors(new HprofProcessProxy(
				new HprofStringAccess(histogram))),
				~HprofRecordType.STRINGS_IN_UTF8.mask());
		// graphDB.listPath("?? statement ??");
		List<DuplicatClassEntry> entries = new ArrayList<DuplicatClassEntry>();
		for (Entry<Object, List<Long>> entry : histogram.classNameIdMap
				.entrySet()) {
			DuplicatClassEntry dce = (entry.getKey() instanceof DuplicatClassEntry) ? (DuplicatClassEntry) entry
					.getKey() : null;
			if (dce.classCount >= 1) {
				entries.add(dce);
			}
		}
		Collections.sort(entries, new Comparator<DuplicatClassEntry>() {

			public int compare(DuplicatClassEntry p1, DuplicatClassEntry p2) {
				long x = p1.classCount;
				long y = p2.classCount;
				return (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		Collections.reverse(entries);
		return entries;
	}

}
