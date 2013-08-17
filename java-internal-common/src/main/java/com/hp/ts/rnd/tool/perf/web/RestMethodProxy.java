package com.hp.ts.rnd.tool.perf.web;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.hp.ts.rnd.tool.perf.web.annotation.RestPath;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class RestMethodProxy {

	private Map<String, RestAnnotatedMethod> methods = new HashMap<String, RestAnnotatedMethod>();

	public void addRootResources(Object root) {
		for (Method method : root.getClass().getMethods()) {
			if (method.isAnnotationPresent(RestPath.class)) {
				try {
					RestAnnotatedMethod restAnnotatedMethod = RestAnnotatedMethod
							.create(method, root);
					RestAnnotatedMethod old = methods.put(
							restAnnotatedMethod.getMethodKey(),
							restAnnotatedMethod);
					if (old != null) {
						throw new IllegalArgumentException(
								"duplicate method with same path key: "
										+ old.getMethodKey() + ", method: "
										+ restAnnotatedMethod.getMethod());
					}
					System.out.println("register on path "
							+ restAnnotatedMethod.getMethodKey()
							+ " for method " + method);
				} catch (Exception e) {
					throw new IllegalArgumentException(
							"process rest annotated method fail: " + method, e);
				}
			}
		}
	}

	public void handle(String fileName, HttpExchange exchange)
			throws IOException {
		WebRequest request = new WebRequest(exchange, fileName);
		// check path parameter match
		RestAnnotatedMethod method = null;
		for (RestAnnotatedMethod restMethod : methods.values()) {
			if (restMethod.requestMatch(request)) {
				method = restMethod;
				break;
			}
		}
		if (method != null) {
			try {
				method.invoke(request);
			} catch (IllegalArgumentException e) {
				rejectBadRequest(exchange, e);
			} catch (Exception e) {
				rejectServerError(exchange, e);
			}
		} else {
			rejectNotFound(exchange);
		}
	}

	private void rejectBadRequest(HttpExchange exchange, Exception e)
			throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "text/html");
		exchange.setAttribute(WebContext.RESOURCE_ERROR, e);
		// invalid input
		exchange.sendResponseHeaders(400, 0);
		exchange.getResponseBody().write(
				"<h1>400 Bad Request</h1>The request cannot be fulfilled due to bad syntax."
						.getBytes());
		exchange.close();
	}

	private void rejectServerError(HttpExchange exchange, Exception e)
			throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "text/html");
		exchange.setAttribute(WebContext.RESOURCE_ERROR, e);
		exchange.sendResponseHeaders(500, 0);
		exchange.getResponseBody().write(
				"<h1>500 Internal Server Error</h1>The server encountered an unexpected error."
						.getBytes());
		exchange.close();
	}

	private void rejectNotFound(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "text/html");
		exchange.sendResponseHeaders(404, 0);
		exchange.getResponseBody().write(
				"<h1>404 Not Found</h1>The requested URL was not found on this server."
						.getBytes());
		exchange.close();
	}
}
