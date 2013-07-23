package com.hp.ts.rnd.tool.perf.hprof;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.hp.ts.rnd.tool.perf.hprof.record.HprofHeader;

final public class HprofRecordReader {

	private DataInput input;
	private RandomAccessFile randomAccess;
	private HprofHeader header;
	private long position;
	private long tagPos;
	private long limit = 0;

	public HprofRecordReader(DataInput input) {
		this.input = input;
		if (input instanceof RandomAccessFile) {
			randomAccess = (RandomAccessFile) input;
		}
	}

	boolean isRandomAccess() {
		return randomAccess != null;
	}

	public long readID() {
		switch (header.getIdentifierSize()) {
		case 1:
			return readU1AsInt();
		case 2:
			return readU2AsInt();
		case 4:
			return readU4AsLong();
		case 8:
			return readU8AsLong();
		default:
			throw new IllegalArgumentException("invalid identifier size");
		}
	}

	public byte[] readBytes(int len) {
		byte[] bytes = new byte[len];
		try {
			input.readFully(bytes);
			position += len;
			checkLimit();
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
		return bytes;
	}

	private void checkLimit() {
		if (limit > 0 && position > limit) {
			throw new HprofDataException("limit exceed: positition(" + position
					+ ") > limit(" + limit + ")");
		}
	}

	public byte[] readNullTerminatedBytes() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int b;
		while ((b = read()) != 0) {
			if (b == -1) {
				throw new HprofDataException(
						"read EOF for null terminated data", new EOFException());
			}
			bytes.write(b);
			checkLimit();
		}
		return bytes.toByteArray();
	}

	public int readU1AsInt() {
		try {
			int i = input.readUnsignedByte();
			position++;
			checkLimit();
			return i;
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
	}

	public int read() {
		int i;
		try {
			if (limit > 0 && position >= limit) {
				return -1;
			}
			try {
				i = input.readUnsignedByte();
				position++;
			} catch (EOFException e) {
				i = -1;
			}
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
		return i;
	}

	public int readU2AsInt() {
		try {
			int i = input.readUnsignedShort();
			position += 2;
			checkLimit();
			return i;
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
	}

	public int readU4AsInt() {
		try {
			int i = input.readInt();
			position += 4;
			checkLimit();
			return i;
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
	}

	public long readU4AsLong() {
		long i;
		try {
			i = input.readInt();
			position += 4;
			checkLimit();
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
		return 0x0FFFFFFFFl & i;
	}

	public long readU8AsLong() {
		try {
			long l = input.readLong();
			position += 8;
			checkLimit();
			return l;
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
	}

	public void setHeader(HprofHeader header) {
		this.header = header;
	}

	public long convertTime(long time) {
		return header.getRecordTime() + time;
	}

	public int endTag() {
		if (tagPos == -1) {
			throw new IllegalStateException("no begin tag called");
		}
		int tagLength = (int) (position - tagPos);
		tagPos = -1;
		return tagLength;
	}

	public void beginTag() {
		tagPos = position;
	}

	public int getIdentifierSize() {
		return header.getIdentifierSize();
	}

	public void skip(long len) {
		try {
			long count = len;
			while (true) {
				long skiped = input.skipBytes((int) Math.min(count,
						Integer.MAX_VALUE));
				if (skiped == 0) {
					break;
				}
				count -= skiped;
				position += skiped;
				checkLimit();
			}
			if (count > 0) {
				throw new HprofIOException("read EOF before skip " + count,
						new EOFException());
			}
		} catch (IOException e) {
			throw new HprofIOException(e);
		}
	}

	void limit(long len) {
		if (len <= 0) {
			this.limit = 0;
		} else {
			this.limit = position + len;
		}
	}

	public long getPosition() {
		return position;
	}

	public void close() throws IOException {
		if (input instanceof Closeable) {
			((Closeable) input).close();
		}
	}

	void setPosition(long pos) {
		try {
			randomAccess.seek(pos);
			position = randomAccess.getFilePointer();
		} catch (IOException e) {
			throw new HprofIOException("set position error", e);
		}
	}

}
