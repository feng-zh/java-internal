package com.hp.ts.rnd.tool.perf.hprof;

public abstract class HprofRecord {

	static int getTagTypeByClass(Class<? extends HprofRecord> recordClass) {
		HprofRecordTag tag = recordClass.getAnnotation(HprofRecordTag.class);
		if (tag == null) {
			throw new IllegalArgumentException(
					"Invalid record with no annotation "
							+ HprofRecordTag.class.getSimpleName() + ": "
							+ recordClass.getName());
		}
		return tag.value();
	}

	static String getTagNameByClass(Class<? extends HprofRecord> recordClass) {
		HprofRecordTag tag = recordClass.getAnnotation(HprofRecordTag.class);
		if (tag == null) {
			throw new IllegalArgumentException(
					"Invalid record with no annotation "
							+ HprofRecordTag.class.getSimpleName() + ": "
							+ recordClass.getName());
		}
		return tag.name();
	}

	public int getTagValue() {
		return getTagTypeByClass(getClass());
	}

	public String getTagName() {
		return getTagNameByClass(getClass());
	}

	// Internal body length
	public abstract long getDataLength();

	// Overall length
	public abstract long getLength();

	// skip read data from current record, this depends on record type
	public void skip() {
		// no-op
	}

	protected abstract void readFields(int tagValue, HprofRecordReader reader)
			throws HprofException;

}
