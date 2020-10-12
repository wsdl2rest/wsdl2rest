package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.tools.common.model.JavaModel;
import org.jboss.fuse.wsdl2rest.EndpointInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

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
	
	@Override
	public void process(List<EndpointInfo> clazzDefs, JavaModel javaModel) throws IOException {
		super.process(clazzDefs, javaModel);
		
		File inFile = specPath.toFile();
		File outFile = new File(inFile.getParent(), inFile.getName().substring(0, inFile.getName().length() - 5) + ".yaml");

		
		JsonNode json = new ObjectMapper().reader().with(JsonParser.Feature.ALLOW_TRAILING_COMMA).readTree(new FileReader(inFile));
        String yaml = new YAMLMapper().writeValueAsString(json);

        try (FileWriter writer = new FileWriter(outFile)) {
        	writer.write(yaml);
        }
	}

}