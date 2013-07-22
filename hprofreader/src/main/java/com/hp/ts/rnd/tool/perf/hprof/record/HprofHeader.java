package com.hp.ts.rnd.tool.perf.hprof.record;

import java.util.Date;

import com.hp.ts.rnd.tool.perf.hprof.HprofException;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(HprofRecordType.HEADER)
public class HprofHeader extends HprofRootRecord {

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

	@Override
	public long getRecordTime() {
		return dumpTime;
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

	@Override
	protected void readHeaders(int tagValue, HprofRecordReader reader)
			throws HprofException {
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
	protected void readRecord(HprofRecordReader reader) {
		// no-op;
	}

	// always not skippable
	@Override
	protected boolean isSkip(long skipMask) {
		return false;
	}

}
