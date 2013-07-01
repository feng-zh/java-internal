package com.hp.ts.rnd.tool.perf.hprof;

public interface HprofRecordVisitor {

	public void visitRecord(HprofRecord record, HprofRecord parent,
			long position);

}
