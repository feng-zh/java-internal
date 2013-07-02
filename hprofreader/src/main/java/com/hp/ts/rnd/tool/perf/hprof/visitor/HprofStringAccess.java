package com.hp.ts.rnd.tool.perf.hprof.visitor;

import com.hp.ts.rnd.tool.perf.hprof.record.HprofUTF8;

public class HprofStringAccess {

	public interface StringSetter {

		public void setUTF8Bytes(long id, byte[] utf8Bytes);

	}

	private StringSetter stringSetter;

	public HprofStringAccess(StringSetter setter) {
		this.stringSetter = setter;
	}

	@HprofProcessTarget
	public void visitUTF8(HprofUTF8 utf8, long position) {
		stringSetter.setUTF8Bytes(utf8.getID(), utf8.getUTF8Bytes());
	}
}
