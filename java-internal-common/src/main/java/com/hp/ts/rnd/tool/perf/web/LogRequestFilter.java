package com.hp.ts.rnd.tool.perf.web;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

@SuppressWarnings("restriction")
class LogRequestFilter extends com.sun.net.httpserver.Filter {

	@Override
	public void doFilter(final HttpExchange exchange, Chain chain)
			throws IOException {
		final AtomicInteger size = new AtomicInteger();
		final AtomicBoolean exchangeClosed = new AtomicBoolean();
		final AtomicBoolean exitFilter = new AtomicBoolean();
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
		HttpExchange wrapExchange = new HttpExchange() {

			private long requestedOn;
			private long startedOn;

			{
				requestedOn = System.currentTimeMillis();
				startedOn = System.nanoTime();

			}

			@Override
			public Headers getRequestHeaders() {
				return exchange.getRequestHeaders();
			}

			@Override
			public Headers getResponseHeaders() {
				return exchange.getResponseHeaders();
			}

			@Override
			public URI getRequestURI() {
				return exchange.getRequestURI();
			}

			@Override
			public String getRequestMethod() {
				return exchange.getRequestMethod();
			}

			@Override
			public HttpContext getHttpContext() {
				return exchange.getHttpContext();
			}

			@Override
			public void close() {
				exchangeClosed.set(true);
				exchange.close();
				logRequestEnd(exchange, requestedOn, startedOn, exitFilter,
						size);
			}

			@Override
			public InputStream getRequestBody() {
				return exchange.getRequestBody();
			}

			@Override
			public OutputStream getResponseBody() {
				return exchange.getResponseBody();
			}

			@Override
			public void sendResponseHeaders(int i, long l) throws IOException {
				exchange.sendResponseHeaders(i, l);
			}

			@Override
			public InetSocketAddress getRemoteAddress() {
				return exchange.getRemoteAddress();
			}

			@Override
			public int getResponseCode() {
				return exchange.getResponseCode();
			}

			@Override
			public InetSocketAddress getLocalAddress() {
				return exchange.getLocalAddress();
			}

			@Override
			public String getProtocol() {
				return exchange.getProtocol();
			}

			@Override
			public Object getAttribute(String s) {
				return exchange.getAttribute(s);
			}

			@Override
			public void setAttribute(String s, Object obj) {
				exchange.setAttribute(s, obj);
			}

			@Override
			public void setStreams(InputStream inputstream,
					OutputStream outputstream) {
				exchange.setStreams(inputstream, outputstream);
			}

			@Override
			public HttpPrincipal getPrincipal() {
				return exchange.getPrincipal();
			}
		};
		wrapExchange.setStreams(exchange.getRequestBody(), sizeStream);
		wrapExchange.setAttribute(WebContext.RESOURCE_ERROR, null);
		try {
			chain.doFilter(wrapExchange);
			checkLogError(wrapExchange);
		} catch (IOException e) {
			logError(exchange, e);
			throw e;
		} catch (RuntimeException e) {
			logError(exchange, e);
			throw e;
		} finally {
			exitFilter.set(true);
			Object asyncConnection = exchange
					.getAttribute(WebContext.CONNECTION_ASYNC);
			exchange.setAttribute(WebContext.CONNECTION_ASYNC, null);
			if (!Boolean.TRUE.equals(asyncConnection) && !exchangeClosed.get()) {
				logLeakConnection(wrapExchange, size);
			}

		}
	}

	private void logLeakConnection(HttpExchange exchange, AtomicInteger size) {
		logError(
				exchange,
				String.format(
						"Detect connection leak from %1$s on \"%2$s %3$s %4$s\", response code: %5$d, response size: %6$d",
						exchange.getRemoteAddress().getAddress()
								.getHostAddress(), exchange.getRequestMethod(),
						exchange.getRequestURI(), exchange.getProtocol(),
						exchange.getResponseCode(), size.intValue()));
	}

	protected void logRequestEnd(HttpExchange exchange, long requestedOn,
			long startedOn, AtomicBoolean exitFilter, AtomicInteger size) {
		long endedOn = System.nanoTime();
		String useragent = exchange.getRequestHeaders().getFirst("User-Agent");
		String referer = exchange.getRequestHeaders().getFirst("Referer");
		String accessLog = String
				.format("[%1$s] "
						+ (exitFilter.get() ? "+" : "-")
						+ " %2$s [%3$tF:%3$tT.%3$tL %3$tz] \"%4$s %5$s %6$s\" %7$d %8$d \"%9$s\" \"%10$s\" %11$d",
						exchange.getRemoteAddress().getAddress()
								.getHostAddress(),
						exchange.getPrincipal() == null ? "-" : exchange
								.getPrincipal().getName(), requestedOn,
						exchange.getRequestMethod(), exchange.getRequestURI(),
						exchange.getProtocol(), exchange.getResponseCode(),
						size.intValue(), referer == null ? "-" : referer,
						useragent == null ? "-" : useragent,
						TimeUnit.NANOSECONDS.toMicros(endedOn - startedOn));
		System.out.println(accessLog);
	}

	private void checkLogError(HttpExchange exchange) {
		Throwable e = (Throwable) exchange
				.getAttribute(WebContext.RESOURCE_ERROR);
		if (e == null) {
			return;
		}
		logError(exchange, e);
	}

	private void logError(HttpExchange exchange, Throwable e) {
		e.printStackTrace();
		logError(exchange, getMessageChain(e));
	}

	private void logError(HttpExchange exchange, String msg) {
		String errorLog = String.format(
				"[%1$tF:%1$tT %1$tz] [error] [client %2$s] %3$s (%4$s)",
				System.currentTimeMillis(), exchange.getRemoteAddress()
						.getAddress().getHostAddress(), msg,
				exchange.getRequestURI());
		System.err.println(errorLog);
	}

	private String getMessageChain(Throwable e) {
		StringBuilder buf = new StringBuilder();
		while (true) {
			buf.append(e.getMessage());
			e = e.getCause();
			if (e == null) {
				break;
			} else {
				buf.append(" -> Caused by: ").append(e.getClass().getName())
						.append(": ");
			}
		}
		return buf.toString();
	}

	@Override
	public String description() {
		return "Audit Request and Log Error";
	}

}
