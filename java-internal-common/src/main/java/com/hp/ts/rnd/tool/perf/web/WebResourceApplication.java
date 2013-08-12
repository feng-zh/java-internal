package com.hp.ts.rnd.tool.perf.web;

import java.util.Set;

public interface WebResourceApplication {

	public Set<Object> getSingletons();

	public String getContextPath();

}
