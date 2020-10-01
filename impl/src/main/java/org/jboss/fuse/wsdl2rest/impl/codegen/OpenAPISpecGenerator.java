package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.nio.file.Path;

public class OpenAPISpecGenerator extends RestSpecGenerator {

    public OpenAPISpecGenerator(Path contextPath) {
        super(contextPath);
    }

    protected String getTemplatePath() {
        return "templates/wsdl2rest-openapi-spec.vm";
    }
}