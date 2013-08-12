package com.hp.ts.rnd.tool.perf.web;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class RestMethodProxy {
	private Map<String, MethodProxy> methodProxys = new HashMap<String, MethodProxy>();

	private static class MethodProxy {
		Method method;
		Object proxy;

		MethodProxy(Method method, Object proxy) {
			this.method = method;
			this.proxy = proxy;
			this.method.setAccessible(true);
		}

		public void invoke(String queryURI, OutputStream output,
				HttpExchange exchange) throws IllegalArgumentException,
				IllegalAccessException, InvocationTargetException, IOException {
			Object ret = method.invoke(proxy);
			if (ret != null) {
				String json = toJson(ret);
				output.write(json.getBytes("UTF-8"));
			}
		}
	}

	static String toJson(Object object) {
		if (object instanceof Iterable) {
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
		} else if (object instanceof Number) {
			return String.valueOf(object);
		} else if (object instanceof String) {
			return "\"" + escape((String) object) + "\"";
		} else if (object != null) {
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
		} else {
			return String.valueOf(object);
		}
	}

	static String escape(String str) {
		if (str.indexOf('"') >= 0) {
			return str.replace("\"", "\\\"");
		} else {
			return str;
		}
	}

	public void addRootResources(Object root) {
		for (Method m : root.getClass().getMethods()) {
			if (m.isAnnotationPresent(RestPath.class)) {
				String path = m.getAnnotation(RestPath.class).value();
				MethodProxy proxy = new MethodProxy(m, root);
				methodProxys.put(path, proxy);
				System.out.println("register on path " + path + " for method "
						+ m);
			}
		}
	}

	public void handle(String fileName, HttpExchange exchange)
			throws IOException {
		int paramIndex = fileName.indexOf('?');
		if (paramIndex >= 0) {
			fileName = fileName.substring(0, paramIndex);
		}
		MethodProxy methodProxy = methodProxys.get(fileName);
		if (methodProxy != null) {
			exchange.getResponseHeaders().set("Content-Type",
					"application/json");
			exchange.sendResponseHeaders(200, 0);
			OutputStream outputStream = exchange.getResponseBody();
			try {
				methodProxy.invoke(fileName, outputStream, exchange);
			} catch (Exception e) {
				e.printStackTrace();
				exchange.sendResponseHeaders(500, 0);
			}
		} else {
			exchange.sendResponseHeaders(404, 0);
		}
	}
}
