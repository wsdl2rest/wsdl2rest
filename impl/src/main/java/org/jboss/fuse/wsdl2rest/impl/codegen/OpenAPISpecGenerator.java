package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAPISpecGenerator extends RestSpecGenerator {

	private static Map<String, String> elementTypeMapping = new LinkedHashMap<>();
	static {

		// Primitive
		elementTypeMapping.put("boolean", "boolean");
		elementTypeMapping.put("Boolean", "boolean");

		elementTypeMapping.put("char", "string");
		elementTypeMapping.put("Character", "string");

		elementTypeMapping.put("byte", "string");
		elementTypeMapping.put("Byte", "string");

		elementTypeMapping.put("short", "integer");
		elementTypeMapping.put("Short", "integer");

		elementTypeMapping.put("int", "integer");
		elementTypeMapping.put("Integer", "integer");

		elementTypeMapping.put("long", "integer");
		elementTypeMapping.put("Long", "integer");

		elementTypeMapping.put("float", "number");
		elementTypeMapping.put("Float", "number");

		elementTypeMapping.put("double", "number");
		elementTypeMapping.put("Double", "number");

		// Date/Time
		elementTypeMapping.put("String", "string");
		elementTypeMapping.put("Calendar", "string");
		elementTypeMapping.put("XMLGregorianCalendar", "string");
		elementTypeMapping.put("Duration", "string");
	}

	public OpenAPISpecGenerator(Path javaPath, Path specPath) {
		super(javaPath, specPath);
	}

	protected String getTemplatePath() {
		return "templates/wsdl2rest-openapi-spec.vm";
	}

	protected String getElementTypeMapping(String xmlType) {
		return elementTypeMapping.get(xmlType);
	}

}