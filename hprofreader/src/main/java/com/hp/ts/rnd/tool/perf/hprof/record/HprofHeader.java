package com.hp.ts.rnd.tool.perf.hprof.record;

import java.util.Date;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x00, name = "HEADER")
public class HprofHeader extends HprofRecord {

	private String format;

	private int identifierSize;

	private long dumpTime;

	private int dataLength;

	public String getFormat() {
		return format;
	}

	public int getIdentifierSize() {
		return identifierSize;
	}

	public long getDumpTime() {
		return dumpTime;
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader) {
		// no super call
		reader.beginTag();
		byte[] bytes = reader.readNullTerminatedBytes();
		char[] ca = new char[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			ca[i] = (char) (0x0FF & bytes[i]);
		}
		format = new String(ca);
		identifierSize = reader.readU4AsInt();
		long hTime = reader.readU4AsLong() << 32;
		long lTime = reader.readU4AsLong();
		dumpTime = hTime | lTime;
		dataLength = reader.endTag();
	}

	@Override
	public long getDataLength() {
		return dataLength;
	}

	@Override
	public long getLength() {
		return getDataLength();
	}

	@Override
	public String toString() {
		return String.format("%s[identifierSize=%s, dumpTime=%s]", format,
				identifierSize, new Date(dumpTime));
	}

}
