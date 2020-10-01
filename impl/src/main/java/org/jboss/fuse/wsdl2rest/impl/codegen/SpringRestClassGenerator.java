package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;
import org.jboss.fuse.wsdl2rest.ParamInfo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class SpringRestClassGenerator extends ClassGeneratorImpl {

    public SpringRestClassGenerator(Path outpath) {
        super(outpath);
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
                int loc = resources.size() >= 2 ? 1 : 0;
                for (int i = loc; i < resources.size(); i++) {
                    path.append(resources.get(i));
                }
                writer.print("Mapping(\"" + path.toString().toLowerCase());

                // Add path param
                if (minfo.getParams().size() > 0) {
                    ParamInfo pinfo = minfo.getParams().get(0);
                    if (hasPathParam(minfo, pinfo)) {
                        writer.print("/{" + pinfo.getParamName() + "}");
                    }
                }
                writer.println("\")");
            }
            writeMethod(writer, minfo);
        }
    }

    @Override
    protected void writeParams(PrintWriter writer, MethodInfo minfo) {
        for (int i = 0; i < minfo.getParams().size(); i++) {
            ParamInfo pinfo = minfo.getParams().get(i);
            String name = pinfo.getParamName();
            String type = pinfo.getParamType();
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

    private String getNestedParameterType(ParamInfo pinfo) {
        String javaType = pinfo.getParamType();
        File javaFile = outpath.resolve(javaType.replace('.', '/') + ".java").toFile();
        if (javaFile.exists()) {
            try (InputStream in = new FileInputStream(javaFile)) {
                final StringBuffer result = new StringBuffer();
                CompilationUnit cu = JavaParser.parse(in);
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(MethodDeclaration decl, Object obj) {
                        if (result.length() == 0 && decl.getName().startsWith("get")) {
                            result.append(decl.getType().toStringWithoutComments());
                        }
                        super.visit(decl, obj);
                    }
                }.visit(cu, null);
                javaType = result.length() > 0 ? result.toString() : null;
            } catch (ParseException | IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return javaType;
    }
}