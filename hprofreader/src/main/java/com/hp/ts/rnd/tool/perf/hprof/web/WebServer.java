package com.hp.ts.rnd.tool.perf.hprof.web;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.hp.ts.rnd.tool.perf.hprof.rest.InstanceHistogramController;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class WebServer implements com.sun.net.httpserver.HttpHandler {

	private HttpServer httpServer;
	private int port;
	private String context;
	private Map<String, MethodProxy> methodProxys = new HashMap<String, WebServer.MethodProxy>();

	private static class MethodProxy {
		Method method;
		Object proxy;

		MethodProxy(Method method, Object proxy) {
			this.method = method;
			this.proxy = proxy;
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

	public WebServer(String context, int port) {
		this.context = context;
		this.port = port;
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

	public void start() throws IOException {
		httpServer = com.sun.net.httpserver.HttpServer.create();
		HttpContext httpContext = httpServer.createContext(context);
		httpContext.setHandler(this);
		httpServer.bind(
				new InetSocketAddress(InetAddress.getLocalHost(), port), 10);
		httpServer.start();
		System.out.println("Server started on port " + port
				+ ", please access http:/" + httpServer.getAddress() + context);
	}

	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		System.out.println("access on " + method + " "
				+ exchange.getRequestURI());
		if ("GET".equals(method)) {
			try {
				handleGet(exchange);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			exchange.sendResponseHeaders(500, 0);
		}
		exchange.close();
	}

	protected void handleGet(HttpExchange exchange) throws IOException {
		writeDefaultRespHeaders(exchange);
		String fileName = exchange.getRequestURI().toString()
				.substring(context.length());
		service(fileName, exchange);
	}

	private void writeDefaultRespHeaders(HttpExchange exchange) {
		Headers respHeaders = exchange.getResponseHeaders();
		respHeaders.add("Server", "HProf Web Server");
		respHeaders.add("Accept-Ranges", "bytes");
		respHeaders.add("Vary", "Accept-Encoding");
		respHeaders.add("Connection", "close");
	}

	protected void service(String queryURI, HttpExchange exchange)
			throws IOException {
		System.out.println(queryURI);
		MethodProxy methodProxy = methodProxys.get("/" + queryURI);
		if (methodProxy == null) {
			// try static resource
			InputStream resource = getClass().getClassLoader()
					.getResourceAsStream(queryURI);
			if (resource == null) {
				System.err.println("404 - File Not Found: " + queryURI);
				exchange.sendResponseHeaders(404, 0);
			} else {
				System.out.println("200 - File Found: " + queryURI);
				OutputStream outputStream = setGziped(exchange);
				byte[] buf = new byte[1024];
				int len;
				while ((len = resource.read(buf)) != -1) {
					outputStream.write(buf, 0, len);
				}
				outputStream.close();
			}
		} else {
			System.out.println("200 - Invoke method: " + methodProxy.method);
			OutputStream outputStream = setGziped(exchange);
			try {
				methodProxy.invoke(queryURI, outputStream, exchange);
			} catch (Exception e) {
				e.printStackTrace();
				exchange.sendResponseHeaders(500, 0);
			}
			outputStream.close();
		}
	}

	private OutputStream setGziped(HttpExchange exchange) throws IOException {
		Headers headers = exchange.getRequestHeaders();
		if (headers.containsKey("Accept-Encoding")
				&& headers.getFirst("Accept-Encoding").contains("gzip")) {
			// gzip required
			exchange.getResponseHeaders().add("Content-Encoding", "gzip");
			exchange.sendResponseHeaders(200, 0);
			return new GZIPOutputStream(exchange.getResponseBody());
		} else {
			exchange.sendResponseHeaders(200, 0);
			return exchange.getResponseBody();
		}
	}

	public static void main(String[] args) throws IOException {
		WebServer server = new WebServer("/heap/", 7001);
		server.addRootResources(new InstanceHistogramController());
		server.start();
	}
}
