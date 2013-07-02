package com.hp.ts.rnd.tool.perf.hprof.visitor;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordVisitor;

public class HprofMultipleVisitors implements HprofRecordVisitor {

	private HprofRecordVisitor[] visitors;

	public HprofMultipleVisitors(HprofRecordVisitor... visitors) {
		this.visitors = visitors;
	}

	public void visitRecord(HprofRecord record, HprofRecord parent,
			long position) {
		for (HprofRecordVisitor visitor : visitors) {
			visitor.visitRecord(record, parent, position);
		}
	}

}
