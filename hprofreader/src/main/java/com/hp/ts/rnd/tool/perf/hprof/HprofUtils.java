package com.hp.ts.rnd.tool.perf.hprof;

import java.io.UnsupportedEncodingException;

public class HprofUtils {

	public static Class<?> toPrimitiveClass(char elementType,
			boolean errorIfNotMatch) {
		switch (elementType) {
		case 'B':
			return Byte.TYPE;
		case 'C':
			return Character.TYPE;
		case 'D':
			return Double.TYPE;
		case 'F':
			return Float.TYPE;
		case 'I':
			return Integer.TYPE;
		case 'J':
			return Long.TYPE;
		case 'S':
			return Short.TYPE;
		case 'Z':
			return Boolean.TYPE;
		default:
			if (errorIfNotMatch) {
				throw new IllegalArgumentException("invalid primitive type: "
						+ elementType);
			} else {
				return null;
			}
		}
	}

	public static String toJavaClassName(String className) {
		String name = className.replace('/', '.');
		while (true) {
			if (name.startsWith("[L") && name.endsWith(";")) {
				name = name.substring(2, name.length() - 1) + "[]";
			} else if (name.startsWith("[")) {
				if (name.endsWith(";")) {
					name = name.substring(1, name.length() - 1) + "[];";
				} else {
					Class<?> clz = toPrimitiveClass(
							name.charAt(name.length() - 1), false);
					if (clz != null) {
						name = name.substring(0, name.length() - 1)
								+ clz.getName();
					}
					name = name.substring(1, name.length()) + "[]";
				}
			} else {
				break;
			}
		}
		return name;
	}

	public static String utf8ToString(byte[] bytes) {
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new HprofDataException(e);
		}
	}

	public static boolean isPrimitiveArrayName(String name) {
		if (name.startsWith("[") && !name.startsWith("[[")
				&& !name.endsWith(";")) {
			Class<?> clz = toPrimitiveClass(name.charAt(name.length() - 1),
					false);
			return clz != null;
		} else {
			return false;
		}
	}

	public static char toPrimitiveCode(int primitiveType,
			boolean errorIfNotMatch) {
		switch (primitiveType) {
		case 4:// boolean
			return 'Z';
		case 8:// byte
			return 'B';
		case 5:// char
			return 'C';
		case 9:// short
			return 'S';
		case 6:// float
			return 'F';
		case 10:// int
			return 'I';
		case 7:// double
			return 'D';
		case 11:// long
			return 'J';
		default:
			if (errorIfNotMatch) {
				throw new IllegalArgumentException("invalid element type: "
						+ primitiveType);
			} else {
				return '\0';
			}
		}
	}

	public static int sizeOfPrimitiveOrObject(int primitiveType, int sizeOfID) {
		switch (primitiveType) {
		case 2://object
			return sizeOfID;
		case 4:// boolean
		case 8:// byte
			return 1;
		case 5:// char
		case 9:// short
			return 2;
		case 6:// float
		case 10:// int
			return 4;
		case 7:// double
		case 11:// long
			return 8;
		default:
			return 0;
		}
	}

}
