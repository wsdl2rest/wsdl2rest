package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.jboss.fuse.wsdl2rest.ElementInfo;
import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;
import org.jboss.fuse.wsdl2rest.ParamInfo;
import org.jboss.fuse.wsdl2rest.impl.service.ClassDefinitionImpl;
import org.jboss.fuse.wsdl2rest.impl.service.ElementInfoImpl;
import org.jboss.fuse.wsdl2rest.impl.service.MethodInfoImpl;
import org.jboss.fuse.wsdl2rest.impl.service.ParamImpl;
import org.jboss.fuse.wsdl2rest.impl.service.TypeInfoImpl;
import org.jboss.fuse.wsdl2rest.util.IllegalArgumentAssertion;
import org.jboss.fuse.wsdl2rest.util.IllegalStateAssertion;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public abstract class RestSpecGenerator {

	protected Path javaPath;
    protected Path specPath;
    protected URL jaxrsAddress;
    
    private boolean noVelocityLog = false;

    RestSpecGenerator(Path javaPath, Path specPath) {
    	this.javaPath = javaPath;
        this.specPath = specPath;
    }
    
    protected String getElementTypeMapping(String xmlType) {
    	return xmlType;
    }

    public void setJaxrsAddress(URL jaxrsAddress) {
        this.jaxrsAddress = jaxrsAddress;
    }

    public void setNoVelocityLog(boolean flag) {
    	this.noVelocityLog = flag;
    }
    
    public void process(List<EndpointInfo> clazzDefs, JavaModel javaModel) throws IOException {
        IllegalArgumentAssertion.assertNotNull(clazzDefs, "clazzDefs");
        IllegalArgumentAssertion.assertNotNull(javaModel, "javaModel");
        IllegalStateAssertion.assertNotNull(specPath, "Spec file name not set");
        IllegalArgumentAssertion.assertTrue(clazzDefs.size() == 1, "Multiple endpoints not supported");
        
        EndpointInfo epinfo = clazzDefs.get(0);
        
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        if (this.noVelocityLog) {
        	ve.setProperty("runtime.log.logsystem.class", org.apache.velocity.runtime.log.NullLogChute.class.getName());
        }
        ve.init();

        String tmplPath = getTemplatePath();
        try (InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(tmplPath))) {

            jaxrsAddress = jaxrsAddress != null ? jaxrsAddress : new URL("http://localhost:8081/jaxrs");
            String jaxrsPath = jaxrsAddress.getPath();
            
            VelocityContext context = new VelocityContext();
            context.put("jaxrsAddress", jaxrsAddress);
            context.put("jaxrsPath", jaxrsPath);
            context.put("serviceClass", epinfo.getFQN());
            context.put("serviceClassName", epinfo.getClassName());
            context.put("allMethods", epinfo.getMethods());
            context.put("id", new IDGenerator());
            
            addTypesMapping(epinfo, javaModel);
            context.put("allTypes", epinfo.getTypes());

            File outfile = specPath.toFile();
            outfile.getParentFile().mkdirs();
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
                ve.evaluate(context, writer, tmplPath, reader);
            }
        }
    }

    protected abstract String getTemplatePath();

    private void addTypesMapping(EndpointInfo epinfo, JavaModel javaModel) {
        IllegalArgumentAssertion.assertTrue(javaModel.getInterfaces().size() == 1, "Multiple interfaces not supported");
        // the keys used have removed the underscores
        JavaInterface javaIntrf = javaModel.getInterfaces().get(epinfo.getClassName().replaceAll("_", ""));

        for (MethodInfo method : epinfo.getMethods()) {
        	List<ParamInfo> params  = method.getParams();
			for (ParamInfo pinfo : params) {
				addTypeMapping(epinfo, pinfo.getParamType());
			}
			if(params.size() > 0) {
				String returnType = method.getReturnType();
				addTypeMapping(epinfo, returnType);
			}
			
        	if (method.getStyle() == null || "document".equals(method.getStyle())) {
        		JavaMethod javaMethod = getJavaMethod(javaIntrf, method.getMethodName());
                List<ParamInfo> wrappedParams = new ArrayList<>();
                for (JavaParameter javaParam : javaMethod.getParameters()) {
                    String paramName = javaParam.getName();
                    wrappedParams.add(new ParamImpl(paramName, normalize(javaParam.getClassName())));
                }
                ((MethodInfoImpl) method).setWrappedParams(wrappedParams);
                ((MethodInfoImpl) method).setWrappedReturnType(normalize(javaMethod.getReturnValue()));
            }
        }
    }
    
    private boolean addTypeMapping(EndpointInfo epinfo, String javaType) {
        if(epinfo.getType(javaType) != null) {
        	return true;
        }
        File javaFile = javaPath.resolve(javaType.replace('.', '/') + ".java").toFile();
        if (javaFile.exists()) {
			final TypeInfoImpl type = new TypeInfoImpl(javaType);
			((ClassDefinitionImpl) epinfo).addType(type);
            try (InputStream in = new FileInputStream(javaFile)) {
                CompilationUnit cu = JavaParser.parse(in);
                new VoidVisitorAdapter<Object>() {
                	public void visit(FieldDeclaration field, Object arg) {
                		boolean complex = false;
                		String elementType = field.getType().toString();
                		if(elementType.contains("<")) {
                			elementType = elementType.substring(0, elementType.indexOf('<'));
                		}
						String elementTypeMapping = getElementTypeMapping(elementType);
						if(elementTypeMapping == null) {
							elementTypeMapping = elementType = cu.getPackage().getName() + "." + elementType;
							complex = addTypeMapping(epinfo, elementType);
						}
						ElementInfo element = new ElementInfoImpl(elementTypeMapping, field.getVariables().get(0).toString(), complex);
                		type.addElement(element);
                		super.visit(field, arg);
                	}
                }.visit(cu, null);
            } catch (ParseException | IOException ex) {
                throw new IllegalStateException(ex);
            }
            return true;
        }
        return false;
    }


    private String normalize(String typeName) {
        if (typeName.contains("List<") && typeName.endsWith(">")) {
            typeName = typeName.substring(typeName.indexOf('<') + 1);
            typeName = typeName.substring(0, typeName.indexOf('>'));
            typeName = typeName + "[]";
        }
        return typeName;
    }
    
    private JavaMethod getJavaMethod(JavaInterface intrf, String methodName) {
        JavaMethod result = null;
        for (JavaMethod method : intrf.getMethods()) {
            if (method.getOperationName().equalsIgnoreCase(methodName))
                result = method;
        }
        IllegalStateAssertion.assertNotNull(result, "Cannot obtain java method for: " + methodName);
        return result;
    }
    
}