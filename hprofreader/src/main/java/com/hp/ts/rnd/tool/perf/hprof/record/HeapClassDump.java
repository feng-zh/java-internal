package com.hp.ts.rnd.tool.perf.hprof.record;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecordReader;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordTag;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordType;

@HprofRecordTag(subValue = 0x20, alias = "CLASS DUMP", value = HprofRecordType.HEAP_DUMP)
public class HeapClassDump extends HprofHeapRecord {

	private long classID;

	private int stacktraceNo;

	private long superClassID;

	private long classLoaderID;

	private long signersID;

	private long protectionDomainID;

	private int instanceSize;

	public static class ConstantPool {
		private int constantPoolIndex;
		private byte entryType;
		private long entryValue;

		public int getConstantPoolIndex() {
			return constantPoolIndex;
		}

		public byte getEntryType() {
			return entryType;
		}

		public long getEntryValue() {
			return entryValue;
		}

		static ConstantPool readFields(HprofRecordReader reader) {
			ConstantPool instance = new ConstantPool();
			instance.constantPoolIndex = reader.readU2AsInt();
			instance.entryType = (byte) reader.readU1AsInt();
			instance.entryValue = readEntryType(instance.entryType, reader);
			return instance;
		}
	}

	private ConstantPool[] constantPools;

	public static class StaticField {
		private long fieldNameID;
		private byte entryType;
		private long entryValue;

		public long getFieldNameID() {
			return fieldNameID;
		}

		public byte getEntryType() {
			return entryType;
		}

		public long getEntryValue() {
			return entryValue;
		}

		static StaticField readFields(HprofRecordReader reader) {
			StaticField instance = new StaticField();
			instance.fieldNameID = reader.readID();
			instance.entryType = (byte) reader.readU1AsInt();
			instance.entryValue = readEntryType(instance.entryType, reader);
			return instance;
		}
	}

	private StaticField[] staticFields;

	public static class InstanceField {
		private long fieldNameID;
		private byte entryType;

		public long getFieldNameID() {
			return fieldNameID;
		}

		public byte getEntryType() {
			return entryType;
		}

		static InstanceField readFields(HprofRecordReader reader) {
			InstanceField instance = new InstanceField();
			instance.fieldNameID = reader.readID();
			instance.entryType = (byte) reader.readU1AsInt();
			return instance;
		}
	}

	private InstanceField[] instanceFields;

	@Override
	protected void readRecord(HprofRecordReader reader) {
		classID = reader.readID();
		stacktraceNo = reader.readU4AsInt();
		superClassID = reader.readID();
		classLoaderID = reader.readID();
		signersID = reader.readID();
		protectionDomainID = reader.readID();
		// reserved
		reader.readID();
		reader.readID();
		instanceSize = reader.readU4AsInt();
		int count;
		count = reader.readU2AsInt();
		constantPools = new ConstantPool[count];
		for (int i = 0; i < count; i++) {
			constantPools[i] = ConstantPool.readFields(reader);
		}
		count = reader.readU2AsInt();
		staticFields = new StaticField[count];
		for (int i = 0; i < count; i++) {
			staticFields[i] = StaticField.readFields(reader);
		}
		count = reader.readU2AsInt();
		instanceFields = new InstanceField[count];
		for (int i = 0; i < count; i++) {
			instanceFields[i] = InstanceField.readFields(reader);
		}
	}

	static long readEntryType(int entryType, HprofRecordReader reader) {
		switch (entryType) {
		case 2:// object
			return reader.readID();
		case 4:// boolean
			return reader.readU1AsInt();
		case 5:// char
			return reader.readU2AsInt();
		case 6:// float
			return reader.readU4AsInt();
		case 7:// double
			return reader.readU8AsLong();
		case 8:// byte
			return reader.readU1AsInt();
		case 9:// short
			return reader.readU2AsInt();
		case 10:// int
			return reader.readU4AsInt();
		case 11:// long
			return reader.readU8AsLong();
		default:
			throw new IllegalArgumentException("invalid entry type: "
					+ entryType);
		}
	}

	public long getClassID() {
		return classID;
	}

	public int getStacktraceNo() {
		return stacktraceNo;
	}

	public long getSuperClassID() {
		return superClassID;
	}

	public long getClassLoaderID() {
		return classLoaderID;
	}

	public long getSignersID() {
		return signersID;
	}

	public long getProtectionDomainID() {
		return protectionDomainID;
	}

	public int getInstanceSize() {
		return instanceSize;
	}

	public ConstantPool[] getConstantPools() {
		return constantPools;
	}

	public StaticField[] getStaticFields() {
		return staticFields;
	}

	public InstanceField[] getInstanceFields() {
		return instanceFields;
	}

	@Override
	public String toString() {
		return String.format("HeapClassDump [classID=0x%08x, classSize=%s]",
				classID, instanceSize);
	}

}
