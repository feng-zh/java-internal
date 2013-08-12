package com.hp.ts.rnd.tool.perf.web;

import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class ResponseHeaderFilter extends com.sun.net.httpserver.Filter {

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		Headers respHeaders = exchange.getResponseHeaders();
		respHeaders.add("Server", "HProf Web Server");
		respHeaders.add("Accept-Ranges", "bytes");
		respHeaders.add("Vary", "Accept-Encoding");
		respHeaders.add("Connection", "close");
		chain.doFilter(exchange);
	}

	@Override
	public String description() {
		return "Response Header Filter";
	}

}
