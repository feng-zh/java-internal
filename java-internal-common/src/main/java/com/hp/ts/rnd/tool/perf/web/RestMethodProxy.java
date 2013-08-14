package com.hp.ts.rnd.tool.perf.web;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.ts.rnd.tool.perf.com.eclipsesource.json.JsonArray;
import com.hp.ts.rnd.tool.perf.com.eclipsesource.json.JsonObject;
import com.hp.ts.rnd.tool.perf.com.eclipsesource.json.JsonValue;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class RestMethodProxy {
	private Map<String, MethodProxy> methodProxys = new HashMap<String, MethodProxy>();
	private Map<String, ParameterMethodProxy> parameterMethodProxys = new HashMap<String, ParameterMethodProxy>();

	private static class MethodProxy {
		Method method;
		Object proxy;
		String httpMethod;

		MethodProxy(Method method, Object proxy) {
			this.method = method;
			this.proxy = proxy;
			this.method.setAccessible(true);
		}

		public void invoke(String queryURI, OutputStream output,
				HttpExchange exchange, Object handback)
				throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException, IOException {
			Object ret = method.invoke(proxy);
			sendResult(ret, exchange, output);
		}

		protected void sendResult(Object ret, HttpExchange exchange,
				OutputStream output) throws IOException {
			if (ret != null) {
				String json = toJson(ret);
				byte[] bytes = json.getBytes("UTF-8");
				exchange.sendResponseHeaders(200, 0);
				output.write(bytes);
			} else {
				exchange.sendResponseHeaders(204, -1);
			}
		}
	}

	private static class ParameterMethodProxy extends MethodProxy {

		private static Pattern PathParameterPattern = Pattern
				.compile("\\{([^/]+)\\}");

		private boolean[] parameterFragements;

		private String[] pathFragements;

		private Pattern pathPattern;

		private String[] methodParameterMap;

		private ParameterMethodProxy(Method method, Object proxy,
				String[] pathFragements, boolean[] parameterFragements,
				String[] methodParameterMap) {
			super(method, proxy);
			this.pathFragements = pathFragements;
			this.parameterFragements = parameterFragements;
			StringBuilder patternBuf = new StringBuilder();
			for (int i = 0; i < pathFragements.length; i++) {
				if (parameterFragements[i]) {
					patternBuf.append("([^/]+)");
				} else {
					patternBuf.append(Pattern.quote(pathFragements[i]));
				}
			}
			pathPattern = Pattern.compile(patternBuf.toString());
			this.methodParameterMap = methodParameterMap;
		}

		public static ParameterMethodProxy creatProxy(String path,
				Method method, Object proxy, String httpMethod) {
			Matcher matcher = PathParameterPattern.matcher(path);
			List<String> pathFragements = new ArrayList<String>();
			Set<String> parameterSet = new HashSet<String>();
			List<Boolean> parameterFragementList = new ArrayList<Boolean>();
			while (matcher.find()) {
				StringBuffer pathFragement = new StringBuffer();
				matcher.appendReplacement(pathFragement, "");
				if (pathFragement.length() > 0) {
					pathFragements.add(pathFragement.toString());
					parameterFragementList.add(false);
				}
				String group = matcher.group(1);
				pathFragements.add(group);
				parameterFragementList.add(true);
				parameterSet.add(group);
			}
			StringBuffer pathFragement = new StringBuffer();
			matcher.appendTail(pathFragement);
			if (pathFragement.length() > 0) {
				pathFragements.add(pathFragement.toString());
				parameterFragementList.add(false);
			}
			boolean[] parameterFragements = new boolean[parameterFragementList
					.size()];
			int index = 0;
			boolean hasParameter = false;
			for (Boolean b : parameterFragementList) {
				parameterFragements[index++] = b;
				hasParameter |= b;
			}
			if (!hasParameter) {
				return null;
			}
			Annotation[][] annotations = method.getParameterAnnotations();
			String[] methodParameterMap = new String[annotations.length];
			for (int i = 0; i < annotations.length; i++) {
				Annotation[] paramAnnot = annotations[i];
				boolean paramHasAnnot = false;
				for (int j = 0; j < paramAnnot.length; j++) {
					if (paramAnnot[j].annotationType() == RestParameter.class) {
						RestParameter restParameter = (RestParameter) paramAnnot[j];
						// check if parameter cover path
						if (!parameterSet.contains(restParameter.value())) {
							throw new IllegalArgumentException(
									"no path parameter '"
											+ restParameter.value()
											+ "' found in path '" + path + "'");
						}
						methodParameterMap[i] = restParameter.value();
						paramHasAnnot = true;
						break;
					} else if (paramAnnot[j].annotationType() == RestEntity.class) {
						if (!httpMethod.equals("POST")
								&& !httpMethod.equals("GET")) {
							throw new IllegalArgumentException(
									"The HTTP method "
											+ httpMethod
											+ " not support RestEntity annotation on parameter index "
											+ (i + 1) + " for method: "
											+ method);
						}
						paramHasAnnot = true;
					}
				}
				if (!paramHasAnnot) {
					throw new IllegalArgumentException(
							"no path parameter found in method parameter on index "
									+ (i + 1) + " for method: " + method);
				}
			}
			return new ParameterMethodProxy(method, proxy,
					pathFragements.toArray(new String[pathFragements.size()]),
					parameterFragements, methodParameterMap);
		}

		public String getPathTemplate() {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < pathFragements.length; i++) {
				if (parameterFragements[i]) {
					buf.append("*");
				} else {
					buf.append(pathFragements[i]);
				}
			}
			return buf.toString();
		}

		public Object match(String httpMethod, String fileName) {
			if (!httpMethod.equals(this.httpMethod)) {
				return null;
			}
			Matcher matcher = pathPattern.matcher(fileName);
			if (matcher.matches()) {
				Map<String, String> map = new HashMap<String, String>();
				int index = 1;
				for (int i = 0; i < pathFragements.length; i++) {
					if (parameterFragements[i]) {
						String parameter = pathFragements[i];
						map.put(parameter, matcher.group(index++));
					}
				}
				return map;
			} else {
				return null;
			}
		}

		@SuppressWarnings("rawtypes")
		public void invoke(String queryURI, OutputStream output,
				HttpExchange exchange, Object handback)
				throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException, IOException {
			Map map = (Map) handback;
			Object[] args = new Object[methodParameterMap.length];
			Class<?>[] pt = method.getParameterTypes();
			for (int i = 0; i < args.length; i++) {
				try {
					if (methodParameterMap[i] == null) {
						// entity annotation
						args[i] = convert(readAsString(exchange), pt[i]);
					} else {
						args[i] = convert(map.get(methodParameterMap[i]), pt[i]);
					}
				} catch (Exception e) {
					throw new IllegalArgumentException(
							"invalid parameter value on index: " + (i + 1), e);
				}
			}
			Object ret = method.invoke(proxy, args);
			sendResult(ret, exchange, output);
		}

		private String readAsString(HttpExchange exchange) throws IOException {
			String contentType = exchange.getRequestHeaders().getFirst(
					"Content-Type");
			int charsetIndex = contentType.indexOf("charset=");
			String charset = "UTF-8";
			if (charsetIndex >= 0) {
				try {
					charset = Charset.forName(
							contentType.substring(charsetIndex
									+ ("charset=".length()))).name();
				} catch (Exception ignored) {
				}
			}
			StringWriter strings = new StringWriter();
			InputStreamReader input = new InputStreamReader(
					exchange.getRequestBody(), charset);
			char[] buf = new char[1024];
			int len;
			try {
				while ((len = input.read(buf)) != -1) {
					strings.write(buf, 0, len);
				}
			} finally {
				try {
					input.close();
				} catch (IOException ignored) {
				}
				strings.close();
			}
			return strings.toString();
		}

		private Object convert(Object obj, Class<?> type) {
			if (type.isInstance(obj)) {
				return obj;
			}
			if (obj == null) {
				throw new NullPointerException(
						"expect non-null value with type " + type);
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
				// throw new UnsupportedOperationException(
				// "unsupport object convert for " + type);
			}
		}
	}

	static Object fromJson(String text, Class<?> type) {
		JsonValue jsonValue = JsonValue.readFrom(text);
		return fromJson(jsonValue, type);
	}

	static Object fromJson(JsonValue jsonValue, Class<?> type) {
		if (jsonValue.isArray()) {
			JsonArray jsonArray = ((JsonArray) jsonValue);
			if (type.isArray()) {
				Object array = Array.newInstance(type.getComponentType(),
						jsonArray.size());
				for (int i = 0; i < jsonArray.size(); i++) {
					Array.set(array, i,
							fromJson(jsonArray.get(i), type.getComponentType()));
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
				object = type.newInstance();
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
							pd.getWriteMethod().invoke(object,
									fromJson(fieldValue, pd.getPropertyType()));
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
		} else {
			throw new UnsupportedOperationException("not support conver "
					+ jsonValue.getClass().getSimpleName() + " to " + type);
		}
	}

	static String toJson(Object object) {
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

	static String escape(String s) {
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

	public void addRootResources(Object root) {
		for (Method m : root.getClass().getMethods()) {
			if (m.isAnnotationPresent(RestPath.class)) {
				String path = m.getAnnotation(RestPath.class).value();
				if (path.startsWith("/")) {
					path = path.substring(1, path.length());
				}
				RestMethod restMethod = m.getAnnotation(RestMethod.class);
				String restMethodValue;
				if (restMethod != null) {
					restMethodValue = restMethod.value();
				} else {
					restMethodValue = "GET";
				}
				ParameterMethodProxy pProxy = ParameterMethodProxy.creatProxy(
						path, m, root, restMethodValue);
				MethodProxy proxy;
				if (pProxy != null) {
					parameterMethodProxys.put(
							restMethodValue + "|" + pProxy.getPathTemplate(),
							pProxy);
					path = pProxy.getPathTemplate();
					proxy = pProxy;
				} else {
					proxy = new MethodProxy(m, root);
					methodProxys.put(restMethodValue + "|" + path, proxy);
				}
				proxy.httpMethod = restMethodValue;
				System.out.println("register " + restMethodValue + " on path "
						+ path + " for method " + m);
			}
		}
	}

	public void handle(String fileName, HttpExchange exchange)
			throws IOException {
		int paramIndex = fileName.indexOf('?');
		if (paramIndex >= 0) {
			fileName = fileName.substring(0, paramIndex);
		}
		Object handback = null;
		String httpMethod = exchange.getRequestMethod();
		// quick check first
		MethodProxy methodProxy = methodProxys.get(httpMethod + "|" + fileName);
		if (methodProxy == null) {
			// check path parameter match
			for (ParameterMethodProxy proxy : parameterMethodProxys.values()) {
				handback = proxy.match(httpMethod, fileName);
				if (handback != null) {
					methodProxy = proxy;
					break;
				}
			}
		}
		if (methodProxy != null) {
			exchange.getResponseHeaders().set("Content-Type",
					"application/json");
			OutputStream outputStream = exchange.getResponseBody();
			try {
				methodProxy.invoke(fileName, outputStream, exchange, handback);
			} catch (IllegalArgumentException e) {
				exchange.setAttribute(WebContext.RESOURCE_ERROR, e);
				// invalid input
				exchange.sendResponseHeaders(400, 0);
				exchange.getResponseBody().write(
						"<h1>400 Bad Request</h1>The request cannot be fulfilled due to bad syntax."
								.getBytes());
			} catch (Exception e) {
				exchange.setAttribute(WebContext.RESOURCE_ERROR, e);
				exchange.sendResponseHeaders(500, 0);
				exchange.getResponseBody().write(
						"<h1>500 Internal Server Error</h1>The server encountered an unexpected error."
								.getBytes());
			}
		} else {
			exchange.sendResponseHeaders(404, 0);
			exchange.getResponseBody().write(
					"<h1>404 Not Found</h1>The requested URL was not found on this server."
							.getBytes());
		}
	}
}
