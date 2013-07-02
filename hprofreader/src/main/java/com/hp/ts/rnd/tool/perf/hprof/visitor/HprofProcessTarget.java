package com.hp.ts.rnd.tool.perf.hprof.visitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HprofProcessTarget {

	Class<? extends HprofRecord>[] value() default {};

}
