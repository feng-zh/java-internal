package com.hp.ts.rnd.tool.perf.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.ts.rnd.tool.perf.web.annotation.RestEntity;
import com.hp.ts.rnd.tool.perf.web.annotation.RestMethod;
import com.hp.ts.rnd.tool.perf.web.annotation.RestPath;
import com.hp.ts.rnd.tool.perf.web.annotation.RestPathParameter;
import com.hp.ts.rnd.tool.perf.web.annotation.RestServerSentEventStream;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class RestAnnotatedMethod {

	public static interface RestParameterHandler {
		public Object handleParameter(WebRequest request) throws Exception;
	}

	public static interface RestParametersProcessor {

		public RestParameterHandler[] createParameterHandler(Method method);

	}

	public static interface RestPathResolver {

		public String getPathTemplate();

		public String getPathMethod();

		public boolean match(WebRequest request);

	}

	public static interface RestResultProcessor {

		public void processResult(Object ret, HttpExchange exchange)
				throws Exception;

	}

	private static class RestPathParameterProcessor implements
			RestParametersProcessor, RestPathResolver {

		static Pattern PathParameterPattern = Pattern.compile("\\{([^/]+)\\}");

		private Pattern pathPattern;
		private String[] pathParameterNames;
		private String pathTemplate;
		private String pathHttpMethod;

		@Override
		public RestParameterHandler[] createParameterHandler(Method method) {
			String path = method.getAnnotation(RestPath.class).value();
			if (path.startsWith("/")) {
				path = path.substring(1, path.length());
			}
			RestMethod restMethod = method.getAnnotation(RestMethod.class);
			if (restMethod != null) {
				pathHttpMethod = restMethod.value();
			} else {
				pathHttpMethod = "GET";
			}
			Matcher matcher = PathParameterPattern.matcher(path);
			List<String> pathParameterList = new ArrayList<String>();
			StringBuilder patternBuf = new StringBuilder();
			StringBuilder pathTemplateBuf = new StringBuilder();
			boolean hasParameter = false;
			while (matcher.find()) {
				StringBuffer pathFragement = new StringBuffer();
				matcher.appendReplacement(pathFragement, "");
				if (pathFragement.length() > 0) {
					patternBuf.append(Pattern.quote(pathFragement.toString()));
					pathTemplateBuf.append(pathFragement);
				}
				String group = matcher.group(1);
				pathParameterList.add(group);
				patternBuf.append("([^/]+)");
				pathTemplateBuf.append("*");
				hasParameter = true;
			}
			this.pathParameterNames = pathParameterList
					.toArray(new String[pathParameterList.size()]);
			StringBuffer pathFragement = new StringBuffer();
			matcher.appendTail(pathFragement);
			if (pathFragement.length() > 0) {
				patternBuf.append(Pattern.quote(pathFragement.toString()));
				pathTemplateBuf.append(pathFragement);
			}
			this.pathPattern = Pattern.compile(patternBuf.toString());
			this.pathTemplate = pathTemplateBuf.toString();
			RestParameterHandler[] parameterHandlers = new RestParameterHandler[method
					.getParameterTypes().length];
			// no path parameter and no method parameter
			if (!hasParameter && method.getParameterTypes().length == 0) {
				return parameterHandlers;
			}
			Annotation[][] annotations = method.getParameterAnnotations();
			for (int i = 0; i < annotations.length; i++) {
				Annotation[] paramAnnot = annotations[i];
				for (int j = 0; j < paramAnnot.length; j++) {
					if (paramAnnot[j].annotationType() == RestPathParameter.class) {
						RestPathParameter restParameter = (RestPathParameter) paramAnnot[j];
						// check if parameter cover path
						if (!pathParameterList.contains(restParameter.value())) {
							throw new IllegalArgumentException(
									"no path parameter '"
											+ restParameter.value()
											+ "' found in path '" + path + "'");
						}
						parameterHandlers[i] = new RestPathParameterHandler(
								restParameter.value(),
								method.getParameterTypes()[i]);
						break;
					}
				}
			}
			return parameterHandlers;
		}

		private Object matchPath0(WebRequest request) {
			String filePath = request.getFilePath();
			Matcher matcher = pathPattern.matcher(filePath);
			if (matcher.matches()) {
				Map<String, String> map = new HashMap<String, String>();
				for (int i = 0; i < pathParameterNames.length; i++) {
					String parameter = pathParameterNames[i];
					map.put(parameter, matcher.group(i + 1));
				}
				return map;
			} else {
				return null;
			}
		}

		public String getPathTemplate() {
			return pathTemplate;
		}

		@Override
		public String getPathMethod() {
			return pathHttpMethod;
		}

		@Override
		public boolean match(WebRequest request) {
			if (!pathHttpMethod.equals(request.getHttpMethod())) {
				return false;
			}
			Object matchedData = matchPath0(request);
			if (matchedData != null) {
				request.setAttachement(matchedData);
			}
			return matchedData != null;
		}
	}

	private static class RestPathParameterHandler implements
			RestParameterHandler {

		private final Class<?> parameterType;

		private final String pathParameterName;

		public RestPathParameterHandler(String pathParameterName,
				Class<?> parameterType) {
			this.pathParameterName = pathParameterName;
			this.parameterType = parameterType;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object handleParameter(WebRequest requet) {
			Map map = (Map) requet.getAttachement();
			return RestUtils.convert(map.get(pathParameterName), parameterType);
		}

	}

	private static class RestEntityParameterProcessor implements
			RestParametersProcessor {

		@Override
		public RestParameterHandler[] createParameterHandler(Method method) {
			RestMethod restMethod = method.getAnnotation(RestMethod.class);
			String httpMethod;
			if (restMethod != null) {
				httpMethod = restMethod.value();
			} else {
				httpMethod = "GET";
			}
			Annotation[][] annotations = method.getParameterAnnotations();
			RestParameterHandler[] parameterHandlers = new RestParameterHandler[annotations.length];
			for (int i = 0; i < annotations.length; i++) {
				Annotation[] paramAnnot = annotations[i];
				for (int j = 0; j < paramAnnot.length; j++) {
					if (paramAnnot[j].annotationType() == RestEntity.class) {
						if (httpMethod.equals("DELETE")) {
							throw new IllegalArgumentException(
									"The HTTP method "
											+ httpMethod
											+ " not support RestEntity annotation on parameter index "
											+ (i + 1) + " for method: "
											+ method);
						}
						parameterHandlers[i] = new RestEntityParameterHandler(
								method.getGenericParameterTypes()[i]);
						break;
					}
				}
			}
			return parameterHandlers;
		}

	}

	private static class RestEntityParameterHandler implements
			RestParameterHandler {

		private final Type parameterType;

		public RestEntityParameterHandler(Type parameterType) {
			this.parameterType = parameterType;
		}

		@Override
		public Object handleParameter(WebRequest request) throws Exception {
			HttpExchange exchange = request.getHttpExchange();
			return RestUtils.convert(readAsString(exchange), parameterType);
		}

		private static String readAsString(HttpExchange exchange)
				throws IOException {
			String contentType = exchange.getRequestHeaders().getFirst(
					"Content-Type");
			int charsetIndex = contentType.indexOf("charset=");
			String charset = "UTF-8";
			if (charsetIndex >= 0) {
				try {
					charset = Charset.forName(
							contentType.substring(charsetIndex
									+ ("charset=".length()))).name();
				} catch (Exception ignored) {
				}
			}
			StringWriter strings = new StringWriter();
			InputStreamReader input = new InputStreamReader(
					exchange.getRequestBody(), charset);
			char[] buf = new char[1024];
			int len;
			try {
				while ((len = input.read(buf)) != -1) {
					strings.write(buf, 0, len);
				}
			} finally {
				try {
					input.close();
				} catch (IOException ignored) {
				}
				strings.close();
			}
			return strings.toString();
		}
	}

	private static class RestSSEParameterProcessor implements
			RestParametersProcessor, RestResultProcessor {

		@Override
		public void processResult(Object ret, HttpExchange exchange)
				throws Exception {
			exchange.setAttribute(WebContext.CONNECTION_ASYNC, Boolean.TRUE);
		}

		@Override
		public RestParameterHandler[] createParameterHandler(Method method) {
			Annotation[][] annotations = method.getParameterAnnotations();
			RestParameterHandler[] parameterHandlers = new RestParameterHandler[annotations.length];
			for (int i = 0; i < annotations.length; i++) {
				Annotation[] paramAnnot = annotations[i];
				for (int j = 0; j < paramAnnot.length; j++) {
					if (paramAnnot[j].annotationType() == RestServerSentEventStream.class) {
						Class<?> pt = method.getParameterTypes()[i];
						if (ObjectOutput.class != pt) {
							throw new IllegalArgumentException(
									"invalid parameter type, expect "
											+ ObjectOutput.class + ", but is "
											+ pt);
						}
						parameterHandlers[i] = new RestSSEParameterHandler();
						break;
					}
				}
			}
			return parameterHandlers;
		}

	}

	private static class RestSSEParameterHandler implements
			RestParameterHandler {

		@Override
		public Object handleParameter(WebRequest request) throws Exception {
			HttpExchange exchange = request.getHttpExchange();
			return createSSEWriter(exchange);
		}

		private static ObjectOutput createSSEWriter(final HttpExchange exchange) {
			exchange.getResponseHeaders().set("Content-Type",
					"text/event-stream");
			return RestUtils.createRestOutputStream(new ServerSentEventOutput(
					exchange.getResponseBody(), exchange));
		}

	}

	private static class PlainPathResolver implements RestPathResolver {

		private String path;
		private String pathMethod;

		public PlainPathResolver(String path, RestMethod restMethod) {
			if (path.startsWith("/")) {
				path = path.substring(1, path.length());
			}
			this.path = path;
			this.pathMethod = restMethod == null ? "GET" : restMethod.value();
		}

		public String getPathTemplate() {
			return path;
		}

		@Override
		public String getPathMethod() {
			return pathMethod;
		}

		@Override
		public boolean match(WebRequest request) {
			return path.equals(request.getFilePath())
					&& pathMethod.equals(request.getHttpMethod());
		}

	}

	private static class NoContentResultProcessor implements
			RestResultProcessor {

		@Override
		public void processResult(Object ret, HttpExchange exchange)
				throws Exception {
			exchange.sendResponseHeaders(204, -1);
			exchange.close();
		}

	}

	private static class ReturnResultProcessor implements RestResultProcessor {

		@Override
		public void processResult(Object ret, HttpExchange exchange)
				throws Exception {
			exchange.getResponseHeaders().set("Content-Type",
					"application/json");
			String json = RestUtils.toJson(ret);
			byte[] bytes = json.getBytes("UTF-8");
			exchange.sendResponseHeaders(200, 0);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		}

	}

	private static RestResultProcessor NO_CONTENT = new NoContentResultProcessor();
	private static RestResultProcessor RETRUN_CONTENT = new ReturnResultProcessor();

	private RestPathResolver pathResolver;
	private RestResultProcessor resultProcessor;
	private Method method;
	private RestParameterHandler[] handlers;
	private Object proxy;

	public static RestAnnotatedMethod create(Method method, Object proxy) {
		RestAnnotatedMethod restMethod = new RestAnnotatedMethod();
		RestParametersProcessor[] processors = new RestParametersProcessor[] {
				new RestPathParameterProcessor(),
				new RestEntityParameterProcessor(),
				new RestSSEParameterProcessor() };
		Set<RestParametersProcessor> processorList = new HashSet<RestParametersProcessor>();
		RestParameterHandler[] allHandlers = new RestParameterHandler[method
				.getParameterTypes().length];
		for (int i = 0; i < processors.length; i++) {
			RestParameterHandler[] handlers = processors[i]
					.createParameterHandler(method);
			for (int j = 0; j < handlers.length; j++) {
				RestParameterHandler handler = handlers[j];
				if (handler != null) {
					if (allHandlers[j] == null) {
						allHandlers[j] = handler;
						processorList.add(processors[i]);
					} else {
						throw new IllegalArgumentException(
								"duplicate rest annotation on parameter index at "
										+ (j + 1) + "for method " + method);
					}
				}
			}
		}
		// check all parameter are processed
		for (int i = 0; i < allHandlers.length; i++) {
			if (allHandlers[i] == null) {
				throw new IllegalArgumentException(
						"no rest parameter annotation found in method parameter on index "
								+ (i + 1) + " for method: " + method);
			}
		}
		// process default path resolver
		for (RestParametersProcessor processor : processorList) {
			if (processor instanceof RestPathResolver) {
				restMethod.pathResolver = (RestPathResolver) processor;
				break;
			}
		}
		if (restMethod.pathResolver == null) {
			restMethod.pathResolver = new PlainPathResolver(method
					.getAnnotation(RestPath.class).value(),
					method.getAnnotation(RestMethod.class));
		}
		// process default result handler
		for (RestParametersProcessor processor : processorList) {
			if (processor instanceof RestResultProcessor) {
				restMethod.resultProcessor = (RestResultProcessor) processor;
				break;
			}
		}
		if (restMethod.resultProcessor == null) {
			if (method.getReturnType() == Void.TYPE) {
				restMethod.resultProcessor = NO_CONTENT;
			} else {
				restMethod.resultProcessor = RETRUN_CONTENT;
			}
		} else {
			if (method.getReturnType() != Void.TYPE) {
				throw new IllegalArgumentException(
						"SSE supported method should not have no return");
			}
		}
		restMethod.handlers = allHandlers;
		method.setAccessible(true);
		restMethod.method = method;
		restMethod.proxy = proxy;
		return restMethod;
	}

	public String getMethodKey() {
		return pathResolver.getPathMethod() + " "
				+ pathResolver.getPathTemplate();
	}

	public Method getMethod() {
		return method;
	}

	public boolean requestMatch(WebRequest request) {
		return pathResolver.match(request);
	}

	public void invoke(WebRequest request) throws Exception {
		Object[] args = new Object[handlers.length];
		for (int i = 0; i < args.length; i++) {
			try {
				args[i] = handlers[i].handleParameter(request);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"invalid parameter value on index: " + (i + 1), e);
			}
		}
		Object ret = method.invoke(proxy, args);
		resultProcessor.processResult(ret, request.getHttpExchange());
	}

}
