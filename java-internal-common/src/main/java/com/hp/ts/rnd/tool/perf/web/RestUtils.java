package com.hp.ts.rnd.tool.perf.web;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.hp.ts.rnd.tool.perf.com.eclipsesource.json.JsonArray;
import com.hp.ts.rnd.tool.perf.com.eclipsesource.json.JsonObject;
import com.hp.ts.rnd.tool.perf.com.eclipsesource.json.JsonValue;

class RestUtils {

	private static class RestObjectOutputStream extends DataOutputStream
			implements ObjectOutput {

		public RestObjectOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void writeObject(Object obj) throws IOException {
			// TODO workaround chrome JSON.parse
			write(toJson(obj).getBytes("UTF-8"));
			writeByte('\n');
		}

	}

	public static ObjectOutput createRestOutputStream(OutputStream out) {
		return new RestObjectOutputStream(out);
	}

	public static String toJson(Object object) {
		if (object == null) {
			return "null";
		} else if (object instanceof Iterable) {
			Iterable<?> it = (Iterable<?>) object;
			StringBuffer buf = new StringBuffer();
			buf.append('[');
			for (Object obj : it) {
				buf.append(toJson(obj)).append(',');
			}
			if (buf.length() > 1) {
				buf.setCharAt(buf.length() - 1, ']');
			} else {
				buf.append(']');
			}
			return buf.toString();
		} else if (object instanceof Map) {
			StringBuffer buf = new StringBuffer();
			buf.append('{');
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
				buf.append(toJson(entry.getKey())).append(':')
						.append(toJson(entry.getValue())).append(',');
			}
			if (buf.length() > 1) {
				buf.setCharAt(buf.length() - 1, '}');
			} else {
				buf.append('}');
			}
			return buf.toString();
		} else if (object instanceof Boolean) {
			return String.valueOf(object);
		} else if (object instanceof Number) {
			return String.valueOf(object);
		} else if (object instanceof String) {
			return "\"" + escape((String) object) + "\"";
		} else if (object instanceof Date) {
			SimpleDateFormat format = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			return '"' + format.format((Date) object) + '"';
		} else if (object instanceof Throwable) {
			StringWriter sw = new StringWriter();
			((Throwable) object).printStackTrace(new PrintWriter(sw));
			return toJson(sw.toString());
		} else {
			BeanInfo beanInfo;
			try {
				beanInfo = Introspector.getBeanInfo(object.getClass());
			} catch (IntrospectionException e) {
				throw new RuntimeException(e);
			}
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
				if (pd.getReadMethod() != null
						&& pd.getReadMethod().getDeclaringClass() != Object.class) {
					Object value;
					pd.getReadMethod().setAccessible(true);
					try {
						value = pd.getReadMethod().invoke(object);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					map.put(pd.getName(), value);
				}
			}
			return toJson(map);
		}
	}

	private static String escape(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if ((ch >= '\u0000' && ch <= '\u001F')
						|| (ch >= '\u007F' && ch <= '\u009F')
						|| (ch >= '\u2000' && ch <= '\u20FF')) {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int j = 0; j < 4 - ss.length(); j++) {
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}// for
		return sb.toString();
	}

	private static Object fromJson(JsonValue jsonValue, Type jType) {
		if (jType instanceof Class) {
			Class<?> type = (Class<?>) jType;
			if (jsonValue.isArray()) {
				JsonArray jsonArray = ((JsonArray) jsonValue);
				if (type.isArray()) {
					Object array = Array.newInstance(type.getComponentType(),
							jsonArray.size());
					for (int i = 0; i < jsonArray.size(); i++) {
						Array.set(
								array,
								i,
								fromJson(jsonArray.get(i),
										type.getComponentType()));
					}
					return array;
				} else {
					throw new UnsupportedOperationException(
							"not support conver json array to " + type);
				}
			} else if (jsonValue.isBoolean()) {
				if (type == Boolean.TYPE || type == Boolean.class) {
					return jsonValue.asBoolean();
				} else {
					throw new UnsupportedOperationException(
							"not support conver json boolean to " + type);
				}
			} else if (jsonValue.isNull()) {
				if (type.isPrimitive()) {
					throw new NullPointerException(
							"null value cannot set as primitive type");
				} else {
					return null;
				}
			} else if (jsonValue.isNumber()) {
				if (type == Integer.TYPE || type == Integer.class) {
					return jsonValue.asInt();
				} else if (type == Long.TYPE || type == Long.class) {
					return jsonValue.asLong();
				} else if (type == Double.TYPE || type == Double.class) {
					return jsonValue.asDouble();
				} else if (type == Float.TYPE || type == Float.class) {
					return jsonValue.asFloat();
				} else {
					throw new UnsupportedOperationException(
							"not support conver json number to " + type);
				}
			} else if (jsonValue.isObject()) {
				JsonObject jsonObject = (JsonObject) jsonValue;
				Object object;
				try {
					Constructor<?> constructor = type.getDeclaredConstructor();
					constructor.setAccessible(true);
					object = constructor.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				BeanInfo beanInfo;
				try {
					beanInfo = Introspector.getBeanInfo(type);
				} catch (IntrospectionException e) {
					throw new RuntimeException(e);
				}
				for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
					if (pd.getWriteMethod() != null) {
						JsonValue fieldValue = jsonObject.get(pd.getName());
						if (fieldValue != null) {
							try {
								pd.getWriteMethod().setAccessible(true);
								pd.getWriteMethod().invoke(
										object,
										fromJson(fieldValue,
												pd.getPropertyType()));
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					}
				}
				return object;
			} else if (jsonValue.isString()) {
				if (type == String.class) {
					return jsonValue.asString();
				} else {
					throw new UnsupportedOperationException(
							"not support conver json string to " + type);
				}
			}
		} else if (jType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) jType;
			if (jsonValue.isArray()) {
				JsonArray jsonArray = ((JsonArray) jsonValue);
				if (type.getRawType() instanceof Class
						&& ((Class<?>) type.getRawType())
								.isAssignableFrom(List.class)) {
					ArrayList<Object> list = new ArrayList<Object>();
					for (int i = 0; i < jsonArray.size(); i++) {
						list.add(fromJson(jsonArray.get(i),
								type.getActualTypeArguments()[0]));
					}
					return list;
				}
			}
		}
		throw new UnsupportedOperationException("not support conver "
				+ jsonValue.getClass().getSimpleName() + " to " + jType);
	}

	public static Object fromJson(String text, Type type) {
		JsonValue jsonValue = JsonValue.readFrom(text);
		return fromJson(jsonValue, type);
	}

	public static Object convert(Object obj, Type type) {
		if (type instanceof Class) {
			return convert(obj, (Class<?>) type);
		} else {
			if (obj == null) {
				throw new NullPointerException(
						"expect non-null value with type " + type);
			} else {
				return fromJson(String.valueOf(obj), type);
			}
		}
	}

	public static Object convert(Object obj, Class<?> type) {
		if (type.isInstance(obj)) {
			return obj;
		}
		if (obj == null) {
			throw new NullPointerException("expect non-null value with type "
					+ type);
		}
		if (type.isPrimitive()) {
			String s = String.valueOf(obj);
			try {
				if (type == Integer.TYPE) {
					return Integer.parseInt(s);
				} else if (type == Double.TYPE) {
					return Double.parseDouble(s);
				} else if (type == Long.TYPE) {
					return Long.parseLong(s);
				} else if (type == Boolean.TYPE) {
					return Boolean.parseBoolean(s);
				} else {
					throw new UnsupportedOperationException(
							"unsupport primitive convert for " + type);
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"convert primitive value fail: " + obj, e);
			}
		} else {
			// try POJO JSON reverse mapping
			return fromJson(String.valueOf(obj), type);
		}
	}

}
