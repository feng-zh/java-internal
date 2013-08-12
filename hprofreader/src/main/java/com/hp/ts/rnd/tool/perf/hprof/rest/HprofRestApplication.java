package com.hp.ts.rnd.tool.perf.hprof.rest;

import java.util.HashSet;
import java.util.Set;

import com.hp.ts.rnd.tool.perf.web.WebResourceApplication;

public class HprofRestApplication implements WebResourceApplication {

	private Set<Object> instances = new HashSet<Object>();

	public HprofRestApplication() {
		instances.add(new InstanceHistogramController());
	}

	public Set<Object> getSingletons() {
		return instances;
	}

	public String getContextPath() {
		return "/heap";
	}

}
