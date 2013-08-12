package com.hp.ts.rnd.tool.perf.web;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class GZIPEncodingFilter extends com.sun.net.httpserver.Filter {

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		Headers headers = exchange.getRequestHeaders();
		if (headers.containsKey("Accept-Encoding")
				&& headers.getFirst("Accept-Encoding").contains("gzip")) {
			// gzip required
			exchange.getResponseHeaders().add("Content-Encoding", "gzip");
			exchange.setStreams(exchange.getRequestBody(),
					new FilterOutputStream(exchange.getResponseBody()) {

						@Override
						public void write(int paramInt) throws IOException {
							setGzipStream();
							super.write(paramInt);
						}

						@Override
						public void write(byte[] paramArrayOfByte,
								int paramInt1, int paramInt2)
								throws IOException {
							if (paramInt2 > 0) {
								setGzipStream();
							}
							super.write(paramArrayOfByte, paramInt1, paramInt2);
						}

						private void setGzipStream() throws IOException {
							if (!(out instanceof GZIPOutputStream)) {
								out = new GZIPOutputStream(out);
							}
						}

					});
		}
		chain.doFilter(exchange);
	}

	@Override
	public String description() {
		return "GZIP Encoding Filter";
	}

}
