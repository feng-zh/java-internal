package com.hp.ts.rnd.tool.perf.web;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

class ServerSentEventOutput extends OutputStream {

	private static byte[] Byte_Header = "data: ".getBytes();

	private OutputStream output;
	private boolean wasLF = false;
	private boolean wasNewLine = true;
	private boolean wasFlush = false;
	private Closeable closeable;

	public ServerSentEventOutput(OutputStream output, Closeable closeable) {
		this.output = output;
		this.closeable = closeable;
	}

	@Override
	public void write(int b) throws IOException {
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
		} else {
			if (b == '\r') {
				wasLF = true;
			} else {
				wasNewLine = b == '\n';
				output.write(b);
			}
		}
	}

	@Override
	public void flush() throws IOException {
		if (!wasFlush) {
			wasFlush = true;
			if (wasNewLine) {
				output.write('\n');
			} else {
				// last end of line is not new line
				System.err
						.println("End of line is not New Line, append new line");
				output.write('\n');
				output.write('\n');
				wasNewLine = true;
			}
			super.flush();
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		closeable.close();
	}

}
