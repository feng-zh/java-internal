package com.hp.ts.rnd.tool.perf.hprof.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.hp.ts.rnd.tool.perf.hprof.rest.InstanceHistogramController;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class WebServer implements com.sun.net.httpserver.HttpHandler {

	private HttpServer httpServer;
	private int port;
	private String context;
	private static RestMethodProxy restMethodProxy = new RestMethodProxy();

	public WebServer(String context, int port) {
		this.context = context;
		this.port = port;
	}

	public void start() throws IOException {
		httpServer = com.sun.net.httpserver.HttpServer.create();
		HttpContext httpContext = httpServer.createContext(context);
		httpContext.setHandler(this);
		httpContext.getFilters().add(new LogRequestFilter());
		httpContext.getFilters().add(new ResponseHeaderFilter());
		httpContext.getFilters().add(new GZIPEncodingFilter());
		httpContext.getFilters().add(new StaticResourceFilter());
		httpServer.bind(
				new InetSocketAddress(InetAddress.getLocalHost(), port), 10);
		httpServer.start();
		System.out.println("Server started on port " + port
				+ ", please access http:/" + httpServer.getAddress() + context);
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

	public static void main(String[] args) throws IOException {
		WebServer server = new WebServer("/heap", 7001);
		restMethodProxy.addRootResources(new InstanceHistogramController());
		server.start();
	}
}
