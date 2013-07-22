package com.hp.ts.rnd.tool.perf.hprof;

public enum HprofRecordType {

	HEADER(0x00), STRINGS_IN_UTF8(0x01), LOAD_CLASS(0x02), UNLOAD_CLASS(0x03), STACK_FRAME(
			0x04), STACK_TRACE(0x05), ALLOC_SITES(0x06), HEAP_SUMMARY(0x07), START_THREAD(
			0x0A), END_THREAD(0x0B), HEAP_DUMP(0x0C), HEAP_DUMP_SEGMENT(0x1C), HEAP_DUMP_END(
			0x2C), CPU_SAMPLES(0x0D), CONTROL_SETTINGS(0x0E);

	private int tagValue;
	private long maskValue;

	private HprofRecordType(int tagValue) {
		this.tagValue = tagValue;
		this.maskValue = 1L << (ordinal() - 1);
	}

	public int tagValue() {
		return tagValue;
	}

	public long mask() {
		return maskValue;
	}

	public static boolean isSkip(int tagValue, long skipMask) {
		for (HprofRecordType type : values()) {
			if (tagValue == type.tagValue()) {
				return (type.mask() & skipMask) != 0;
			}
		}
		return false;
	}

}
