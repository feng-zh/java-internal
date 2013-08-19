package com.hp.ts.rnd.tool.perf.web;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
class ServerSentEventOutput extends OutputStream {

	private static byte[] Byte_Header = "data: ".getBytes();

	private OutputStream output;
	private boolean wasLF = false;
	private boolean wasNewLine = true;
	private boolean wasFlush = false;
	private boolean hasContent = false;
	private HttpExchange exchange;
	private boolean headerSent;

	public ServerSentEventOutput(OutputStream output, HttpExchange exchange) {
		this.output = output;
		this.exchange = exchange;
	}

	@Override
	public void write(int b) throws IOException {
		sendHeader(true);
		wasFlush = false;
		if (wasNewLine) {
			output.write(Byte_Header);
		}
		if (wasLF) {
			wasLF = false;
			if (b != '\n') {
				output.write('\r');
			}
			wasNewLine = b == '\n';
			output.write(b);
			hasContent = true;
		} else {
			if (b == '\r') {
				wasLF = true;
			} else {
				wasNewLine = b == '\n';
				output.write(b);
				hasContent = true;
			}
		}
	}

	private void sendHeader(boolean inWrite) throws IOException {
		if (!headerSent) {
			headerSent = true;
			exchange.getResponseHeaders().remove("Connection");
			if (hasContent || inWrite) {
				exchange.sendResponseHeaders(200, 0);
			} else {
				exchange.sendResponseHeaders(404, -1);
			}
		}
	}

	@Override
	public void flush() throws IOException {
		if (!wasFlush && hasContent) {
			sendHeader(true);
			wasFlush = true;
			if (wasNewLine) {
				output.write('\n');
				hasContent = true;
			} else {
				// last end of line is not new line
				System.err
						.println("End of line is not New Line, append new line");
				output.write('\n');
				output.write('\n');
				hasContent = true;
				wasNewLine = true;
			}
			output.flush();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			sendHeader(false);
			super.close();
		} finally {
			exchange.close();
		}
	}

}
