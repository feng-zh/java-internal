package com.hp.ts.rnd.tool.perf.hprof.visitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.ts.rnd.tool.perf.hprof.HprofRecord;
import com.hp.ts.rnd.tool.perf.hprof.HprofRecordVisitor;

public class HprofProcessProxy implements HprofRecordVisitor {

	private Map<Class<?>, BitSet> recordClassMatches = new HashMap<Class<?>, BitSet>();

	private Object delegator;

	private List<MethodProxy> proxyMethodList = new ArrayList<MethodProxy>();

	abstract class MethodProxy implements HprofRecordVisitor {

		private Class<?>[] targets;

		private Method method;

		public MethodProxy(Class<?>[] targets, Method method) {
			this.targets = targets;
			this.method = method;
		}

		public Method getMethod() {
			return method;
		}

		public Class<?>[] getTargets() {
			return targets;
		}

	}

	private static Class<?>[][] MatchParameters = {
			{ HprofRecord.class, HprofRecord.class, Long.TYPE },
			{ HprofRecord.class, null, Long.TYPE },
			{ HprofRecord.class, HprofRecord.class, null },
			{ HprofRecord.class, null, null } };

	protected HprofProcessProxy() {
		delegator = this;
	}

	public HprofProcessProxy(Object delegator) {
		this.delegator = delegator;
		for (Method m : delegator.getClass().getMethods()) {
			if (m.isAnnotationPresent(HprofProcessTarget.class)) {
				Class<?>[] paramTypes = m.getParameterTypes();
				int matched = -1;
				LOOP: for (int i = 0; i < MatchParameters.length; i++) {
					Class<?>[] checkingTypes = MatchParameters[i];
					int k = 0;
					for (int j = 0; j < checkingTypes.length; j++) {
						Class<?> checkingParameter = checkingTypes[j];
						if (checkingParameter == null) {
							continue;
						}
						if (k >= paramTypes.length
								|| !checkingParameter
										.isAssignableFrom(paramTypes[k++])) {
							continue LOOP;
						}
					}
					if (k == paramTypes.length) {
						matched = i;
						break;
					}
				}
				if (matched == -1) {
					throw new IllegalArgumentException(
							"invalid method with incompatiable parameters: "
									+ m);
				}
				Class<?>[] targets = m.getAnnotation(HprofProcessTarget.class)
						.value();
				for (Class<?> target : targets) {
					if (!paramTypes[0].isAssignableFrom(target)) {
						throw new IllegalArgumentException(
								"incompatiable parameter type (1st parameter) with annotation target type "
										+ target.getName() + " on method " + m);
					}
				}
				if (targets.length == 0) {
					// default to parameter
					if (!HprofRecord.class.isAssignableFrom(paramTypes[0])) {
						throw new IllegalArgumentException(
								"incompatiable parameter type (1st parameter) with annotation target type "
										+ HprofRecord.class.getName()
										+ " on method " + m);
					}
					targets = new Class<?>[] { paramTypes[0] };
				}
				final Class<?>[] parameters = MatchParameters[matched];
				proxyMethodList.add(new MethodProxy(targets, m) {
					public void visitRecord(HprofRecord record,
							HprofRecord parent, long position) {
						Object[] args = new Object[getMethod()
								.getParameterTypes().length];
						int i = 0;
						if (parameters[0] != null) {
							args[i++] = record;
						}
						if (parameters[1] != null) {
							args[i++] = parent;
						}
						if (parameters[2] != null) {
							args[i++] = position;
						}
						try {
							getMethod().invoke(
									HprofProcessProxy.this.delegator, args);
						} catch (InvocationTargetException e) {
							if (e.getCause() instanceof RuntimeException) {
								throw (RuntimeException) e.getCause();
							}
							if (e.getCause() instanceof Error) {
								throw (Error) e.getCause();
							}
							throw new UndeclaredThrowableException(e.getCause());
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
		}
		if (proxyMethodList.isEmpty()) {
			throw new IllegalArgumentException(
					"invalid delegator without annotation "
							+ HprofProcessTarget.class.getName() + " on "
							+ delegator.getClass());
		}
	}

	final public void visitRecord(HprofRecord record, HprofRecord parent,
			long position) {
		Class<? extends HprofRecord> clz = record.getClass();
		BitSet bs = recordClassMatches.get(clz);
		if (bs == null) {
			bs = new BitSet();
			recordClassMatches.put(clz, bs);
			for (int i = 0; i < proxyMethodList.size(); i++) {
				MethodProxy mp = proxyMethodList.get(i);
				for (Class<?> target : mp.getTargets()) {
					if (target.isAssignableFrom(clz)) {
						bs.set(i);
						break;
					}
				}
			}
		}
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			proxyMethodList.get(i).visitRecord(record, parent, position);
		}
	}
}
