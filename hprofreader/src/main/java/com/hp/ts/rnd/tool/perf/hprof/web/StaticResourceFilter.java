package com.hp.ts.rnd.tool.perf.hprof.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class StaticResourceFilter extends com.sun.net.httpserver.Filter {

	private static final Map<String, String> mimeMap = new HashMap<String, String>();

	static {
		mimeMap.put("js", "text/javascript");
		mimeMap.put("css", "text/css");
	}

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		// try static resource
		String fileName = exchange.getRequestURI().toString()
				.substring(exchange.getHttpContext().getPath().length());
		int paramIndex = fileName.indexOf('?');
		if (paramIndex >= 0) {
			fileName = fileName.substring(0, paramIndex);
		}
		if (!fileName.endsWith("/")) {
			InputStream resource = loadResource(fileName);
			if (resource != null) {
				int extIndex = fileName.lastIndexOf('.');
				if (extIndex > 0) {
					String ext = fileName.substring(extIndex + 1,
							fileName.length());
					String contentTypeFromName = HttpURLConnection
							.guessContentTypeFromName(ext);
					if (contentTypeFromName == null) {
						contentTypeFromName = mimeMap.get(ext);
					}
					if (contentTypeFromName == null) {
						contentTypeFromName = HttpURLConnection
								.guessContentTypeFromStream(resource);
					}
					if (contentTypeFromName != null) {
						exchange.getResponseHeaders().set("Content-Type",
								contentTypeFromName);
					}
				}
				exchange.sendResponseHeaders(200, 0);
				OutputStream outputStream = exchange.getResponseBody();
				byte[] buf = new byte[1024];
				int len;
				while ((len = resource.read(buf)) != -1) {
					outputStream.write(buf, 0, len);
				}
				exchange.close();
				return;
			}
		}
		chain.doFilter(exchange);
	}

	protected InputStream loadResource(String fileName) {
		return getClass().getResourceAsStream(fileName);
	}

	@Override
	public String description() {
		return "Static Resource Filter";
	}

}
