package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jboss.fuse.wsdl2rest.ClassGenerator;
import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;
import org.jboss.fuse.wsdl2rest.ParamInfo;
import org.jboss.fuse.wsdl2rest.impl.writer.MessageWriter;
import org.jboss.fuse.wsdl2rest.impl.writer.MessageWriterFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ClassGeneratorImpl implements ClassGenerator {

	protected MessageWriter msgWriter = MessageWriterFactory.getMessageWriter();

	protected Path inpath;
	protected String sourceType;
	protected Path outpath;
	
	private CompilationUnit sourceClass;

	public ClassGeneratorImpl(Path outpath) {
		this.outpath = outpath;
	}

	public ClassGeneratorImpl(Path inpath, String sourceType, Path outpath) {
		this.inpath = inpath;
		this.sourceType = sourceType;
		this.outpath = outpath;
		loadSourceClass();
	}

	@Override
	public void generateClasses(List<EndpointInfo> clazzDefs) throws IOException {
		for (EndpointInfo clazzDef : clazzDefs) {
			
			String packageName = clazzDef.getPackageName();
			packageName = packageName.replace('.', File.separatorChar);

			File packageDir = outpath.resolve(packageName).toFile();
			packageDir.mkdirs();

			File clazzFile = new File(packageDir, getClassFileName(clazzDef) + ".java");
			try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(clazzFile)))) {
				writePackageName(writer, clazzDef);
				writeImports(writer, clazzDef);
				writeServiceClass(writer, clazzDef);
			}
		}
	}

	private void loadSourceClass() {
		if (inpath == null || sourceType == null) {
			return;
		}
		File javaFile = inpath.resolve(sourceType.replace('.', '/') + ".java").toFile();
		if (javaFile.exists()) {
			try (InputStream in = new FileInputStream(javaFile)) {
				sourceClass = JavaParser.parse(in);
			} catch (ParseException | IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	protected String getClassFileName(EndpointInfo clazzDef) {
		return clazzDef.getClassName();
	}

	protected void writePackageName(PrintWriter writer, EndpointInfo clazzDef) {
		final String packName = clazzDef.getPackageName();
		if (packName != null && packName.length() != 0) {
			writer.println("package " + packName + ";");
		}
		writer.println();
	}

	protected void writeImports(PrintWriter writer, EndpointInfo clazzDef) {
		if (clazzDef.getImports() != null) {
			for (String impo : clazzDef.getImports()) {
				writer.println("import " + impo + ";");
			}
		}
		writer.println();
	}

	protected void writeServiceClass(PrintWriter writer, EndpointInfo clazzDef) {
		if (clazzDef.getClassName() != null) {
			writer.println("public class " + getClassFileName(clazzDef) + " {\n");
			writeFields(writer);
			writeMethods(writer, clazzDef.getMethods());
			writer.println("}");
			writer.println();
		}
	}

	protected void writeFields(PrintWriter writer) {
		writer.println(getSourceFields());
	}
	
	private String getSourceFields() {
		if(sourceClass == null) {
			return "";
		}
		
		final StringBuilder result = new StringBuilder();
		new VoidVisitorAdapter<Object>() {
			@Override
			public void visit(FieldDeclaration decl, Object obj) {
				result.append("\t").append(decl).append("\n");
			}
		}.visit(sourceClass, null);
		return result.toString();
	}

	protected void writeMethods(PrintWriter writer, List<? extends MethodInfo> methods) {
		if (methods != null) {
			for (MethodInfo minfo : methods) {
				String retType = minfo.getReturnType();
				writer.print("\tpublic " + (retType != null ? retType : "void") + " ");
				writer.print(minfo.getMethodName() + "(");
				writeParams(writer, minfo);
				String excep = minfo.getExceptionType() != null ? (" throws " + minfo.getExceptionType()) : "";
				writer.println(")" + excep + ";");
				writer.println();
			}
		}
	}

	protected void writeMethod(PrintWriter writer, MethodInfo minfo) {
		if (minfo != null) {
			String retType = minfo.getReturnType();
			writer.print("\tpublic " + (retType != null ? retType : "void") + " ");
			writer.print(minfo.getMethodName() + "(");
			writeParams(writer, minfo);
			String excep = minfo.getExceptionType() != null ? (" throws " + minfo.getExceptionType()) : "";
			writer.print(")" + excep + " ");
			writeBody(writer, minfo);
			writer.println();
		}
	}

	protected void writeParams(PrintWriter writer, MethodInfo minfo) {
		List<ParamInfo> params = minfo.getParams();
		String[] sourceParams = getSourceMethodParams(minfo.getMethodName());
		for (int i = 0; i < params.size(); i++) {
			ParamInfo param = params.get(0);
			String type = param.getParamType();
			String name = getParamName(param.getParamName(), i, sourceParams);
			writer.print(i == 0 ? "" : ", ");
			writer.print(type + " " + name);
		}
	}

	protected String getParamName(String name, int i, String[] sourceParams) {
		if(sourceParams != null) {
			return sourceParams[i];	
		} else {
			return name;
		}
	}
	
	protected void writeBody(PrintWriter writer, MethodInfo minfo) {
		String body = getSourceMethodBody(minfo.getMethodName());
		writer.println(body);
		writer.println();
	}

	private String getSourceMethodBody(String methodName) {
		if(sourceClass == null) {
			return "{ }";
		}
		final StringBuilder result = new StringBuilder();
		new VoidVisitorAdapter<Object>() {
			@Override
			public void visit(MethodDeclaration decl, Object obj) {
				if(decl.getName().equals(methodName)) {
					result.append(decl.getBody().toStringWithoutComments());
				}
			}
		}.visit(sourceClass, null);
		return result.toString().replace("\n", "\n\t");
	}
	
	protected String[] getSourceMethodParams(String methodName) {
		if(sourceClass == null) {
			return null;
		}
		
		final List<String> result = new ArrayList<String>();
		new VoidVisitorAdapter<Object>() {
			@Override
			public void visit(MethodDeclaration decl, Object obj) {
				if(decl.getName().equals(methodName)) {
					for(Parameter p : decl.getParameters()) {
						result.add(p.getId().getName());
					}
				}
			}
		}.visit(sourceClass, null);
		return result.toArray(new String[] {});
	}
	
    protected String getNestedParameterType(ParamInfo pinfo) {
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
