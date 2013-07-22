package com.hp.ts.rnd.tool.perf.hprof;

public abstract class HprofRecord {

	static int getTagTypeByClass(Class<? extends HprofRecord> recordClass) {
		HprofRecordTag tag = getTagByClass(recordClass);
		int subValue = tag.subValue();
		return subValue == 0 ? tag.value().tagValue() : subValue;
	}

	static HprofRecordTag getTagByClass(Class<? extends HprofRecord> recordClass) {
		HprofRecordTag tag = recordClass.getAnnotation(HprofRecordTag.class);
		if (tag == null) {
			throw new IllegalArgumentException(
					"Invalid record with no annotation "
							+ HprofRecordTag.class.getSimpleName() + ": "
							+ recordClass.getName());
		}
		return tag;
	}

	public int getTagValue() {
		HprofRecordTag tag = getTag();
		int subValue = tag.subValue();
		return subValue == 0 ? tag.value().tagValue() : subValue;
	}

	public String getTagName() {
		HprofRecordTag tag = getTag();
		return tag.alias().length() == 0 ? tag.value().name() : tag.alias();
	}

	private HprofRecordTag getTag() {
		return getTagByClass(getClass());
	}

	// Internal body length
	public abstract long getDataLength();

	// Overall length
	public abstract long getLength();

	protected abstract void readFields(HprofRecordReader reader)
			throws HprofException;

	protected abstract void readHeaders(int tagValue, HprofRecordReader reader)
			throws HprofException;

	protected boolean isSkip(long skipMask) {
		return (getTag().value().mask() & skipMask) != 0;
	}
}
