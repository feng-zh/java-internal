package com.hp.ts.rnd.tool.perf.web;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class WebRequest {

	private String filePath;
	private HttpExchange exchange;
	private Object attachement;

	public WebRequest(HttpExchange exchange, String fileName) {
		int paramIndex = fileName.indexOf('?');
		if (paramIndex >= 0) {
			filePath = fileName.substring(0, paramIndex);
		} else {
			filePath = fileName;
		}
		this.exchange = exchange;
	}

	public String getFilePath() {
		return filePath;
	}

	public HttpExchange getHttpExchange() {
		return exchange;
	}

	public void setAttachement(Object attachement) {
		this.attachement = attachement;
	}

	public Object getAttachement() {
		return attachement;
	}

	public String getHttpMethod() {
		return exchange.getRequestMethod();
	}
}
