package com.hp.ts.rnd.tool.perf.web;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class LogRequestFilter extends com.sun.net.httpserver.Filter {

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		long requestedOn = System.currentTimeMillis();
		long startedOn = System.nanoTime();
		String useragent = exchange.getRequestHeaders().getFirst("User-Agent");
		String referer = exchange.getRequestHeaders().getFirst("Referer");
		final AtomicInteger size = new AtomicInteger();
		OutputStream sizeStream = new FilterOutputStream(
				exchange.getResponseBody()) {

			@Override
			public void write(int b) throws IOException {
				super.write(b);
				size.incrementAndGet();
			}

			@Override
			public void write(byte[] bytes, int offset, int len)
					throws IOException {
				super.write(bytes, offset, len);
				size.addAndGet(len);
			}

		};
		exchange.setStreams(exchange.getRequestBody(), sizeStream);
		try {
			chain.doFilter(exchange);
		} catch (IOException e) {
			logError(exchange, e);
			throw e;
		} catch (RuntimeException e) {
			logError(exchange, e);
			throw e;
		} finally {
			long endedOn = System.nanoTime();
			String accessLog = String
					.format("[%1$s] - %2$s [%3$tF:%3$tT.%3$tL %3$tz] \"%4$s %5$s %6$s\" %7$d %8$d \"%9$s\" \"%10$s\" %11$d",
							exchange.getRemoteAddress().getAddress()
									.getHostAddress(),
							exchange.getPrincipal() == null ? "-" : exchange
									.getPrincipal().getName(), requestedOn,
							exchange.getRequestMethod(), exchange
									.getRequestURI(), exchange.getProtocol(),
							exchange.getResponseCode(), size.intValue(),
							referer == null ? "-" : referer,
							useragent == null ? "-" : useragent,
							TimeUnit.NANOSECONDS.toMicros(endedOn - startedOn));
			System.out.println(accessLog);
		}
	}

	private void logError(HttpExchange exchange, Exception e) {
		String errorLog = String.format(
				"[%1$tF:%1$tT %1$tz] [error] [client %2$s] %3$s (%4$s)",
				System.currentTimeMillis(), exchange.getRemoteAddress()
						.getAddress().getHostAddress(), e.getMessage(),
				exchange.getRequestURI());
		System.err.println(errorLog);
	}

	@Override
	public String description() {
		return "Audit Request and Log Error";
	}

}
