package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;
import com.hp.ts.rnd.tool.perf.hprof.HprofUtils;

@HprofRecordTag(HprofRecordType.STRINGS_IN_UTF8)
public class HprofUTF8 extends HprofRootRecord {

	private byte[] bytes;
	private long id;

	public long getID() {
		return id;
	}

	public byte[] getUTF8Bytes() {
		return bytes;
	}

	public String getString() {
		return HprofUtils.utf8ToString(bytes);
	}

	protected void readRecord(HprofRecordReader reader) {
		id = reader.readID();
		bytes = reader.readBytes((int) getDataLength()
				- reader.getIdentifierSize());
	}

	@Override
	public String toString() {
		return String.format("%s[@0x%08x]", getString(), id);
	}

}
