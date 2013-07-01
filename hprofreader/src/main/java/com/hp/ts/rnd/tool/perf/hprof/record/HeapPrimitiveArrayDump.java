package com.hp.ts.rnd.tool.perf.hprof.record;

import java.lang.reflect.Array;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;

@HprofRecordTag(value = 0x23, name = "PRIMITIVE ARRAY DUMP")
public class HeapPrimitiveArrayDump extends HprofHeapRecord {

	private long arrayID;

	private int stacktraceNo;

	private Object elements;

	private byte elementType;

	// used for lazy parsing
	private int elementCount;

	public long getArrayID() {
		return arrayID;
	}

	public int getStacktraceNo() {
		return stacktraceNo;
	}

	public Object getElements() {
		if (elementCount >= 0) {
			elements = reserveElements((byte[]) elements, elementCount,
					elementType);
			elementCount = -1;
		}
		return elements;
	}

	static int readU1AsInt(byte[] data, int pos) {
		return 0x0FF & data[pos++];
	}

	static int readU2AsInt(byte[] data, int pos) {
		int ch1 = 0x0FF & data[pos++];
		int ch2 = 0x0FF & data[pos++];
		return ((ch1 << 8) + (ch2 << 0));
	}

	static int readU4AsInt(byte[] data, int pos) {
		int ch1 = 0x0FF & data[pos++];
		int ch2 = 0x0FF & data[pos++];
		int ch3 = 0x0FF & data[pos++];
		int ch4 = 0x0FF & data[pos++];
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	static long readU8AsLong(byte[] data, int pos) {
		return (((long) data[0] << 56) + ((long) (data[1] & 255) << 48)
				+ ((long) (data[2] & 255) << 40)
				+ ((long) (data[3] & 255) << 32)
				+ ((long) (data[4] & 255) << 24) + ((data[5] & 255) << 16)
				+ ((data[6] & 255) << 8) + ((data[7] & 255) << 0));
	}

	private static Object reserveElements(byte[] source, int count, int type) {
		switch (type) {
		case 4:// boolean
		{
			boolean[] data = new boolean[count];
			for (int i = 0; i < count; i++) {
				data[i] = readU1AsInt(source, i) != 0;
			}
			return data;
		}
		case 5:// char
		{
			char[] data = new char[count];
			for (int i = 0; i < count; i++) {
				data[i] = (char) readU2AsInt(source, i << 1);
			}
			return data;
		}
		case 6:// float
		{
			float[] data = new float[count];
			for (int i = 0; i < count; i++) {
				data[i] = Float.intBitsToFloat(readU4AsInt(source, i << 2));
			}
			return data;
		}
		case 7:// double
		{
			double[] data = new double[count];
			for (int i = 0; i < count; i++) {
				data[i] = Double.longBitsToDouble(readU8AsLong(source, i << 3));
			}
			return data;
		}
		case 8:// byte
		{
			byte[] data = new byte[count];
			for (int i = 0; i < count; i++) {
				data[i] = (byte) readU1AsInt(source, i);
			}
			return data;
		}
		case 9:// short
		{
			short[] data = new short[count];
			for (int i = 0; i < count; i++) {
				data[i] = (short) readU2AsInt(source, i << 1);
			}
			return data;
		}
		case 10:// int
		{
			int[] data = new int[count];
			for (int i = 0; i < count; i++) {
				data[i] = readU4AsInt(source, i << 2);
			}
			return data;
		}
		case 11:// long
		{
			long[] data = new long[count];
			for (int i = 0; i < count; i++) {
				data[i] = readU8AsLong(source, i << 3);
			}
			return data;
		}
		default:
			throw new IllegalArgumentException("invalid element type: " + type);
		}
	}

	public byte getElementType() {
		return elementType;
	}

	@Override
	protected void readFields(int tagValue, HprofRecordReader reader) {
		super.readFields(tagValue, reader);
		arrayID = reader.readID();
		stacktraceNo = reader.readU4AsInt();
		elementCount = reader.readU4AsInt();
		elementType = (byte) reader.readU1AsInt();
		elements = readElements(reader, elementCount, elementType);
		super.calcuateDataLength(reader);
	}

	private static Object readElements(HprofRecordReader reader, int count,
			int type) {
		switch (type) {
		case 4:// boolean
		case 8:// byte
			return reader.readBytes(count);
		case 5:// char
		case 9:// short
			return reader.readBytes(count << 1);
		case 6:// float
		case 10:// int
			return reader.readBytes(count << 2);
		case 7:// double
		case 11:// long
			return reader.readBytes(count << 3);
		default:
			throw new IllegalArgumentException("invalid element type: " + type);
		}
	}

	@Override
	public String toString() {
		return String
				.format("HeapPrimitiveArrayDump [arrayID=0x%08x, elementType=%s, elementCount=%s]",
						arrayID, elementType, Array.getLength(getElements()));
	}

}
