package com.hp.ts.rnd.tool.perf.web;

import java.io.IOException;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class WebContext implements HttpHandler {

	private HttpContext httpContext;
	private RestMethodProxy restMethodProxy = new RestMethodProxy();
	static final String RESOURCE_ERROR = "RESOURCE_ERROR";
	static final String CONNECTION_ASYNC = "CONNECTION_ASYNC";

	public WebContext(WebResourceApplication application, HttpServer httpServer) {
		String contextPath = application.getContextPath();
		httpContext = httpServer.createContext(contextPath
				+ (contextPath.endsWith("/") ? "" : "/"));
		httpContext.setHandler(this);
		setupFilters(application);
		addResources(application);
	}

	private void setupFilters(WebResourceApplication application) {
		httpContext.getFilters().add(new LogRequestFilter());
		httpContext.getFilters().add(new ResponseHeaderFilter());
		httpContext.getFilters().add(new GZIPEncodingFilter());
		httpContext.getFilters().add(
				new StaticResourceFilter(application.getClass()));
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
		if ("GET".equals(method) || "POST".equals(method)
				|| "DELETE".equals(method)) {
			service(fileName, exchange);
		} else {
			exchange.sendResponseHeaders(500, 0);
			exchange.close();
		}
	}

	private void service(String fileName, HttpExchange exchange)
			throws IOException {
		restMethodProxy.handle(fileName, exchange);
	}

}
