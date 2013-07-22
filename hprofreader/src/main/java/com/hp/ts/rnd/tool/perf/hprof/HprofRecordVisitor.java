package com.hp.ts.rnd.tool.perf.hprof;

public interface HprofRecordVisitor {

	public void visitSingleRecord(HprofRecord record, long position);

	public HprofRecordVisitor visitCompositeRecord(HprofRecord record,
			long position);

}
