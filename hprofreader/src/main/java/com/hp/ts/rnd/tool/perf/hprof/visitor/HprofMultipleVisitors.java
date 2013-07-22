package com.hp.ts.rnd.tool.perf.hprof.visitor;

import java.util.ArrayList;
import java.util.List;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordVisitor;

public class HprofMultipleVisitors implements HprofRecordVisitor {

	private HprofRecordVisitor[] visitors;

	public HprofMultipleVisitors(HprofRecordVisitor... visitors) {
		this.visitors = visitors;
	}

	public void visitSingleRecord(HprofRecord record, long position) {
		for (HprofRecordVisitor visitor : visitors) {
			visitor.visitSingleRecord(record, position);
		}
	}

	public HprofRecordVisitor visitCompositeRecord(HprofRecord record,
			long position) {
		List<HprofRecordVisitor> subVisitors = new ArrayList<HprofRecordVisitor>();
		for (HprofRecordVisitor visitor : visitors) {
			HprofRecordVisitor sub = visitor.visitCompositeRecord(record,
					position);
			if (sub != null) {
				subVisitors.add(sub);
			}
		}
		if (subVisitors.isEmpty()) {
			return null;
		} else {
			return new HprofMultipleVisitors(
					subVisitors.toArray(new HprofRecordVisitor[subVisitors
							.size()]));
		}
	}

}
