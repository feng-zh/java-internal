package com.hp.ts.rnd.tool.perf.hprof;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HprofRecordTag {

	String name();

	int value();

}
