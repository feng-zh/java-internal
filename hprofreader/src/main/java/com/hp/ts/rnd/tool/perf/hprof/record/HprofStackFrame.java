package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(HprofRecordType.STACK_FRAME)
public class HprofStackFrame extends HprofRootRecord {

	private long stackframeID;

	private long methodNameID;

	private long methodSignatureID;

	private long sourceFileID;

	private int classNo;

	private int lineNo;

	protected void readRecord(HprofRecordReader reader) {
		stackframeID = reader.readID();
		methodNameID = reader.readID();
		methodSignatureID = reader.readID();
		sourceFileID = reader.readID();
		classNo = reader.readU4AsInt();
		lineNo = reader.readU4AsInt();
	}

	public String getLineNumberText() {
		if (lineNo > 0) {
			return String.valueOf(lineNo);
		} else if (lineNo == -1) {
			return "unknown location";
		} else if (lineNo == -2) {
			return "compiled method";
		} else if (lineNo == -3) {
			return "native method";
		} else {
			return "no line information available";
		}
	}

	public long getStackframeID() {
		return stackframeID;
	}

	public long getMethodNameID() {
		return methodNameID;
	}

	public long getMethodSignatureID() {
		return methodSignatureID;
	}

	public long getSourceFileID() {
		return sourceFileID;
	}

	public int getClassNo() {
		return classNo;
	}

	public int getLineNo() {
		return lineNo;
	}

	@Override
	public String toString() {
		return String
				.format("HprofStackFrame [stackframeID=0x%08x, methodNameID=0x%08x, methodSignatureID=0x%08x, sourceFileID=0x%08x, classNo=%s, lineNo=%s]",
						stackframeID, methodNameID, methodSignatureID,
						sourceFileID, classNo, lineNo);
	}

}
