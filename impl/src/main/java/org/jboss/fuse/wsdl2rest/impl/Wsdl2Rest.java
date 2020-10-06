package org.jboss.fuse.wsdl2rest.impl;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.tools.common.model.JavaModel;
import org.jboss.fuse.wsdl2rest.ClassGenerator;
import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.ResourceMapper;
import org.jboss.fuse.wsdl2rest.WSDLProcessor;
import org.jboss.fuse.wsdl2rest.impl.codegen.BlueprintContextGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.CamelContextGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.JavaRestClassGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.JavaTypeGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.OpenAPISpecGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.RestSpecGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.SpringContextGenerator;
import org.jboss.fuse.wsdl2rest.impl.codegen.SpringRestClassGenerator;
import org.jboss.fuse.wsdl2rest.util.IllegalArgumentAssertion;

public class Wsdl2Rest {

    private final URL wsdlUrl;
    private final Path outpath;

    private URL jaxrsAddress;
    private URL jaxwsAddress;
    private Path blueprintContext;
    private Path camelContext;
    private Path openAPISpec;
    private Path javaOut;
    private boolean noVelocityLog = false;
    
    public Wsdl2Rest(URL wsdlUrl, Path outpath) {
        IllegalArgumentAssertion.assertNotNull(wsdlUrl, "wsdlUrl");
        IllegalArgumentAssertion.assertNotNull(outpath, "outpath");
        this.wsdlUrl = wsdlUrl;
        this.outpath = outpath;
    }

    public void setJaxrsAddress(URL jaxrsAddress) {
        this.jaxrsAddress = jaxrsAddress;
    }

    public void setJaxwsAddress(URL jaxwsAddress) {
        this.jaxwsAddress = jaxwsAddress;
    }

    /**
     * Defaults to [outpath]/camel/wsdl2rest-camel-context.xml
     */
    public void setBlueprintContext(Path blueprintContext) {
        this.blueprintContext = blueprintContext;
    }

    /**
     * Defaults to [outpath]/camel/wsdl2rest-camel-context.xml
     */
    public void setCamelContext(Path camelContext) {
        this.camelContext = camelContext;
    }
    
    /**
     * Defaults to [outpath]/camel/wsdl2rest-openapi-spec.json
     */
    public void setOpenAPISpec(Path openAPISpec) {
        this.openAPISpec = openAPISpec;
    }

    /**
     * Defaults to [outpath]/java
     */
    public void setJavaOut(Path javaOut) {
        this.javaOut = javaOut;
    }
    
    /**
     * Defaults to false
     * @param flag
     */
    public void setNoVelocityLog(boolean flag) {
    	this.noVelocityLog = flag;
    }

    public List<EndpointInfo> process() throws Exception {
        
        WSDLProcessor wsdlProcessor = new WSDLProcessorImpl();
        wsdlProcessor.process(wsdlUrl);

        // if there was a SOAP address in the WSDL and the JAX-WS address
        // has not been overridden via the command-line, use the one from the WSDL
        if (wsdlProcessor.getJaxWsServiceLocation() != null && jaxwsAddress == null) {
            jaxwsAddress = wsdlProcessor.getJaxWsServiceLocation();
        }
        
        List<EndpointInfo> clazzDefs = wsdlProcessor.getClassDefinitions();
        ResourceMapper resMapper = new ResourceMapperImpl();
        resMapper.assignResources(clazzDefs);

        Path javaPath = effectiveJavaOut();
        JavaTypeGenerator typeGen = new JavaTypeGenerator(javaPath, wsdlUrl);
        JavaModel javaModel = typeGen.execute();
        
        ClassGenerator classGen = new JavaRestClassGenerator(javaPath);
        classGen.generateClasses(clazzDefs);
        
        classGen = new SpringRestClassGenerator(javaPath);
        classGen.generateClasses(clazzDefs);


        if (blueprintContext != null) {
            Path contextPath = effectiveCamelContext(blueprintContext, Paths.get("wsdl2rest-blueprint-context.xml"));
            CamelContextGenerator camelGen = new BlueprintContextGenerator(contextPath);
            camelGen.setJaxrsAddress(jaxrsAddress);
            camelGen.setJaxwsAddress(jaxwsAddress);
            camelGen.setNoVelocityLog(noVelocityLog);
            camelGen.process(clazzDefs, javaModel);
        }
        
        if (camelContext != null) {
            Path contextPath = effectiveCamelContext(camelContext, Paths.get("wsdl2rest-camel-context.xml"));
            CamelContextGenerator camelGen = new SpringContextGenerator(contextPath);
            camelGen.setJaxrsAddress(jaxrsAddress);
            camelGen.setJaxwsAddress(jaxwsAddress);
            camelGen.setNoVelocityLog(noVelocityLog);
            camelGen.process(clazzDefs, javaModel);
        }
        
        if (openAPISpec != null) {
            Path specPath = effectiveSpec(openAPISpec, Paths.get("wsdl2rest-openapi-spec.json"));
            RestSpecGenerator specGen = new OpenAPISpecGenerator(javaPath, specPath);
            specGen.setJaxrsAddress(jaxrsAddress);
            specGen.setNoVelocityLog(noVelocityLog);
            specGen.process(clazzDefs, javaModel);
        }
        
        return Collections.unmodifiableList(clazzDefs);
    }

    private Path effectiveJavaOut() {
        Path resultPath = javaOut;
        if (resultPath == null) {
            resultPath = Paths.get("java");
        }
        return resultPath.isAbsolute() ? resultPath : outpath.resolve(resultPath); 
    }

    private Path effectiveCamelContext(Path givenPath, Path defaultPath) {
        Path resultPath = givenPath;
        if (resultPath == null) {
            resultPath = defaultPath;
        }
        List<Path> pathElements = new ArrayList<>();
        resultPath.iterator().forEachRemaining(pathElements::add);
        if (pathElements.size() < 2) {
            resultPath = Paths.get("camel", resultPath.toString());
        }
        return resultPath.isAbsolute() ? resultPath : outpath.resolve(resultPath); 
    }
    
    private Path effectiveSpec(Path givenPath, Path defaultPath) {
        Path resultPath = givenPath;
        if (resultPath == null) {
            resultPath = defaultPath;
        }
        List<Path> pathElements = new ArrayList<>();
        resultPath.iterator().forEachRemaining(pathElements::add);
        if (pathElements.size() < 2) {
            resultPath = Paths.get("spec", resultPath.toString());
        }
        return resultPath.isAbsolute() ? resultPath : outpath.resolve(resultPath); 
    }
    
}