package com.hp.ts.rnd.tool.perf.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class WebContext implements HttpHandler {

	private HttpContext httpContext;
	private RestMethodProxy restMethodProxy = new RestMethodProxy();
	static final String RESOURCE_ROOT = "RESOURCE_ROOT";
	static final String RESOURCE_ERROR = "RESOURCE_ERROR";

	public WebContext(WebResourceApplication application, HttpServer httpServer) {
		httpContext = httpServer.createContext(application.getContextPath());
		httpContext.setHandler(this);
		setupFilters(httpContext.getFilters());
		addResources(application);
		Class<?> clz = application.getClass();
		URL clzUrl = clz.getResource("/" + clz.getName().replace('.', '/')
				+ ".class");
		try {
			httpContext.getAttributes().put(
					RESOURCE_ROOT,
					new URL(clzUrl, clz.getPackage().getName()
							.replace('.', '/').replaceAll("[^/]+", "..")));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid resource root url", e);
		}
	}

	private void setupFilters(List<Filter> filters) {
		filters.add(new LogRequestFilter());
		filters.add(new ResponseHeaderFilter());
		filters.add(new GZIPEncodingFilter());
		filters.add(new StaticResourceFilter());
	}

	private void addResources(WebResourceApplication application) {
		for (Object obj : application.getSingletons()) {
			restMethodProxy.addRootResources(obj);
		}
	}

	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		String fileName = exchange.getRequestURI().toString()
				.substring(exchange.getHttpContext().getPath().length());
		if ("GET".equals(method)) {
			service(fileName, exchange);
		} else {
			exchange.sendResponseHeaders(500, 0);
		}
		exchange.close();
	}

	private void service(String fileName, HttpExchange exchange)
			throws IOException {
		restMethodProxy.handle(fileName, exchange);
	}

}
