package com.hp.ts.rnd.tool.perf.web;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class StaticResourceFilter extends com.sun.net.httpserver.Filter {

	private static final Map<String, String> mimeMap = new HashMap<String, String>();

	private boolean supportDefaultFile = true;

	private URL resourceRootUrl;

	protected InputStream NOT_MODIFIED_STREAM = new ByteArrayInputStream(
			new byte[0]);

	static {
		mimeMap.put("js", "text/javascript");
		mimeMap.put("css", "text/css");
		mimeMap.put("html", "text/html");
		mimeMap.put("appcache", "text/cache-manifest");
	}

	public StaticResourceFilter() {
	}

	public StaticResourceFilter(Class<?> clz) {
		URL clzUrl = clz.getResource("/" + clz.getName().replace('.', '/')
				+ ".class");
		try {
			resourceRootUrl = new URL(clzUrl, clz.getPackage().getName()
					.replace('.', '/').replaceAll("[^/]+", ".."));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid resource root url", e);
		}
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
		if (!fileName.endsWith("/") && fileName.length() > 0) {
			InputStream resource = loadResource(exchange, fileName);
			if (resource != null) {
				if (resource == NOT_MODIFIED_STREAM) {
					// not modified
					exchange.sendResponseHeaders(304, -1);
				} else {
					sendFile(exchange, fileName, resource);
				}
				return;
			}
		} else if (supportDefaultFile) {
			String indexFile = fileName + "index.html";
			InputStream resource = loadResource(exchange, indexFile);
			if (resource != null) {
				close(resource);
				exchange.getResponseHeaders().set("Location",
						exchange.getHttpContext().getPath() + indexFile);
				exchange.sendResponseHeaders(302, -1);
				return;
			}
		}
		chain.doFilter(exchange);
	}

	protected void sendFile(HttpExchange exchange, String fileName,
			InputStream resource) throws IOException {
		try {
			int extIndex = fileName.lastIndexOf('.');
			if (extIndex > 0) {
				String ext = fileName
						.substring(extIndex + 1, fileName.length());
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
			return;
		} finally {
			close(resource);
			exchange.close();
		}
	}

	protected void close(Closeable resource) {
		try {
			resource.close();
		} catch (IOException ignored) {
		}
	}

	protected InputStream loadResource(HttpExchange exchange, String fileName)
			throws IOException {
		while (fileName.startsWith("/")) {
			fileName = fileName.substring(1);
		}
		if (resourceRootUrl == null) {
			return getClass().getClassLoader().getResourceAsStream(fileName);
		} else {
			URL resourceUrl = new URL(resourceRootUrl, fileName);
			if (resourceUrl.getPath().startsWith(resourceRootUrl.getPath())) {
				if (resourceUrl.getProtocol().equals("file")) {
					File resourceFile;
					try {
						resourceFile = new File(resourceUrl.toURI());
						if (!resourceFile.exists()) {
							return null;
						}
						SimpleDateFormat dateFormat = new SimpleDateFormat(
								"EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
						dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
						Date fileModified = new Date(
								resourceFile.lastModified());
						// send last modified
						exchange.getResponseHeaders().set("Last-Modified",
								dateFormat.format(fileModified));
						// check if last modified header
						String ifModifiedSince = exchange.getRequestHeaders()
								.getFirst("If-Modified-Since");
						if (ifModifiedSince != null) {
							try {
								Date dateCheck = dateFormat
										.parse(ifModifiedSince);
								if (fileModified.equals(dateCheck)) {
									// no change
									return NOT_MODIFIED_STREAM;
								}
							} catch (ParseException ignored) {
							}
						}
					} catch (URISyntaxException ignored) {
					}
				}
				try {
					return resourceUrl.openStream();
				} catch (FileNotFoundException e) {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	@Override
	public String description() {
		return "Static Resource Filter";
	}

}
