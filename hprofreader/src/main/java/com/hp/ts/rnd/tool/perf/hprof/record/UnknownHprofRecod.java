package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofDataException;
import com.hp.ts.rnd.tool.perf.hprof.HprofException;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

public class UnknownHprofRecod extends HprofRootRecord {

	private int tagValue;

	protected void readHeaders(int tagValue, HprofRecordReader reader) {
		super.readHeaders(tagValue, reader);
		this.tagValue = tagValue;
	}

	protected void readRecord(HprofRecordReader reader) throws HprofException {
		long length = getDataLength();
		long position = reader.getPosition();
		try {
			reader.skip(length);
		} catch (Exception e) {
			throw new HprofDataException(
					String.format(
							"skip data on unknown tag error: 0x%02x, position: %s, length: %s",
							tagValue, position, length), e);
		}
	}

	public int getTagValue() {
		return tagValue;
	}

	@Override
	public String getTagName() {
		return "UNKNOWN_TAG";
	}

	@Override
	protected boolean isSkip(long skipMask) {
		return HprofRecordType.isSkip(tagValue, skipMask);
	}

}
