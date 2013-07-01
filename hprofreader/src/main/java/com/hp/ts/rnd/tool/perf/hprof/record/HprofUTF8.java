package com.hp.ts.rnd.tool.perf.hprof.record;

import java.io.UnsupportedEncodingException;

import com.hp.ts.rnd.tool.perf.hprof.HprofDataException;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x01, name = "UTF8")
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
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new HprofDataException(e);
		}
	}

	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		id = reader.readID();
		bytes = reader.readBytes((int) getDataLength()
				- reader.getIdentifierSize());
	}

	@Override
	public String toString() {
		return String.format("%s[@0x%08x]", getString(), id);
	}

}
