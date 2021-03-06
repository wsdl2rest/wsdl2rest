package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;
import org.jboss.fuse.wsdl2rest.ParamInfo;

public class JavaRestClassGenerator extends ClassGeneratorImpl {

    public JavaRestClassGenerator(Path outpath) {
        super(outpath);
    }
    
    public JavaRestClassGenerator(Path inpath, String source, Path outpath) {
    	super(inpath, source, outpath);
    }
    
    public JavaRestClassGenerator(Path inpath, String source, Path outpath, boolean domainSplit) {
    	super(inpath, source, outpath, domainSplit);
    }
    
    @Override
    public String getClassFileName(String className) {
    	return className + "Resource";
    }

    @Override
    protected void writeImports(PrintWriter writer, EndpointInfo clazzDef) {
        writer.println("import javax.ws.rs.Consumes;");
        writer.println("import javax.ws.rs.Produces;");
        writer.println("import javax.ws.rs.DELETE;");
        writer.println("import javax.ws.rs.GET;");
        writer.println("import javax.ws.rs.POST;");
        writer.println("import javax.ws.rs.PUT;");
        writer.println("import javax.ws.rs.Path;");
        writer.println("import javax.ws.rs.PathParam;");
        writer.println("import javax.ws.rs.core.MediaType;");
        super.writeImports(writer, clazzDef);
    }

    @Override
    protected void writeServiceClass(PrintWriter writer, EndpointInfo clazzDef) throws IOException {
        String pathName = clazzDef.getClassName().toLowerCase();
        writer.println("@Path(\"/" + pathName + "/\")");
        super.writeServiceClass(writer, clazzDef);
    }

    @Override
    protected void writeMethod(PrintWriter writer, EndpointInfo clazzDef, MethodInfo minfo) throws IOException {
            List<String> resources = minfo.getResources();
            if (minfo.getPreferredResource() != null) {
                resources = new ArrayList<String>();
                resources.add(minfo.getPreferredResource());
            }
            if (resources != null) {
                String httpMethod = minfo.getHttpMethod();
                writer.println("\t@" + httpMethod);
                StringBuilder path = new StringBuilder();
                // int loc = resources.size() >= 2 ? 1 : 0;
                //for (int i = loc; i < resources.size(); i++) {
                path.append(resources.get(0));
                //}
                
            	writer.print("\t@Path(\"" + path.toString().toLowerCase());

                // Add path param
            	String[] sourceParams = getSourceMethodParams(minfo.getMethodName());
                if (minfo.getParams().size() > 0) {
                    ParamInfo pinfo = minfo.getParams().get(0);
                    if (hasPathParam(minfo, pinfo)) {
                        writer.print("/{" + getParamName(pinfo.getParamName(), 0, sourceParams)  + "}");
                    }
                }
                writer.println("\")");

                // Add @Consumes for PUT,POST 
                if (httpMethod.equals("PUT") || httpMethod.equals("POST")) {
                    writer.println("\t@Consumes(MediaType.APPLICATION_JSON)");
                }

                // Add @Produces for all methods 
                writer.println("\t@Produces(MediaType.APPLICATION_JSON)");
            }
            super.writeMethod(writer, clazzDef, minfo);
    }

	@Override
    protected void writeParams(PrintWriter writer, MethodInfo minfo) {
    	List<ParamInfo> params = minfo.getParams();
    	String[] sourceParams = getSourceMethodParams(minfo.getMethodName());
        for (int i = 0; i < params.size(); i++) {
            ParamInfo pinfo = params.get(i);
            String type = pinfo.getParamType();
            String name = getParamName(pinfo.getParamName(), i, sourceParams);
            if (i == 0 && hasPathParam(minfo, pinfo)) {
                writer.print("@PathParam(\"" + name + "\") ");
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