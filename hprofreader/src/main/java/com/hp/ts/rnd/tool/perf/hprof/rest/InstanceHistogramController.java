package com.hp.ts.rnd.tool.perf.hprof.rest;

import java.util.List;

import com.hp.ts.rnd.tool.perf.hprof.historm.InstanceHistogram;
import com.hp.ts.rnd.tool.perf.hprof.historm.InstanceHistogram.InstanceHistogramEntry;
import com.hp.ts.rnd.tool.perf.web.annotation.RestPath;

class InstanceHistogramController {

	private List<InstanceHistogramEntry> list;

	@RestPath("/histogram")
	public List<InstanceHistogramEntry> instanceHistogram() throws Exception {
		if (list == null) {
			list = InstanceHistogram.list("heap.bin");
		}
		return list;
	}

}
