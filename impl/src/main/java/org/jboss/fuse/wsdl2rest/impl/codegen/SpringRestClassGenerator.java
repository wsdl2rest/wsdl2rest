package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;
import org.jboss.fuse.wsdl2rest.ParamInfo;

public class SpringRestClassGenerator extends ClassGeneratorImpl {

    public SpringRestClassGenerator(Path outpath) {
        super(outpath);
    }
    
    public SpringRestClassGenerator(Path inpath, String sourceType, Path outpath) {
    	super(inpath, sourceType, outpath);
    }
    
    @Override
    protected String getClassFileName(EndpointInfo clazzDef) {
    	return super.getClassFileName(clazzDef) + "Controller";
    }

    @Override
    protected void writeImports(PrintWriter writer, EndpointInfo clazzDef) {
    	writer.println("import org.springframework.web.bind.annotation.RestController;");
        writer.println("import org.springframework.web.bind.annotation.GetMapping;");
        writer.println("import org.springframework.web.bind.annotation.PutMapping;");
        writer.println("import org.springframework.web.bind.annotation.DeleteMapping;");
        writer.println("import org.springframework.web.bind.annotation.PostMapping;");
        writer.println("import org.springframework.web.bind.annotation.PathVariable;");
        super.writeImports(writer, clazzDef);
    }

    @Override
    protected void writeServiceClass(PrintWriter writer, EndpointInfo clazzDef) {
        String pathName = clazzDef.getClassName().toLowerCase();
        writer.println("@RestController(\"/" + pathName + "/\")");
        super.writeServiceClass(writer, clazzDef);
    }

    @Override
    protected void writeMethods(PrintWriter writer, List<? extends MethodInfo> methods) {
        for (MethodInfo minfo : methods) {
            List<String> resources = minfo.getResources();
            if (minfo.getPreferredResource() != null) {
                resources = new ArrayList<String>();
                resources.add(minfo.getPreferredResource());
            }
            if (resources != null) {
                String httpMethod =  minfo.getHttpMethod().substring(0,1).toUpperCase() +  minfo.getHttpMethod().substring(1, minfo.getHttpMethod().length()).toLowerCase();
                writer.print("\t@" + httpMethod);
                StringBuilder path = new StringBuilder();
                //int loc = resources.size() >= 2 ? 1 : 0;
                //for (int i = loc; i < resources.size(); i++) {
                path.append(resources.get(0));
                //}
                writer.print("Mapping(\"" + path.toString().toLowerCase());

                // Add path param
                String[] sourceParams = getSourceMethodParams(minfo.getMethodName());
                if (minfo.getParams().size() > 0) {
                    ParamInfo pinfo = minfo.getParams().get(0);
                    if (hasPathParam(minfo, pinfo)) {
                    	writer.print("/{" + getParamName(pinfo.getParamName(), 0, sourceParams)  + "}");
                    }
                }
                writer.println("\")");
            }
            writeMethod(writer, minfo);
        }
    }

    protected void writeParams(PrintWriter writer, MethodInfo minfo) {
    	List<ParamInfo> params = minfo.getParams();
    	String[] sourceParams = getSourceMethodParams(minfo.getMethodName());
        for (int i = 0; i < params.size(); i++) {
            ParamInfo pinfo = params.get(i);
            String type = pinfo.getParamType();
            String name = getParamName(pinfo.getParamName(), i, sourceParams);
            if (i == 0 && hasPathParam(minfo, pinfo)) {
                writer.print("@PathVariable(\"" + name + "\") ");
                writer.print(getNestedParameterType(pinfo) + " " + name);
            } else if (getNestedParameterType(pinfo) != null) {
                writer.print(i == 0 ? "" : ", ");
                writer.print(type + " " + name);
            }
        }
    }

    private boolean hasPathParam(MethodInfo minfo, ParamInfo pinfo) {
        String httpMethod = minfo.getHttpMethod();
        boolean pathParam = httpMethod.equals("GET") || httpMethod.equals("DELETE");
        return pathParam && getNestedParameterType(pinfo) != null;
    }

}