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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	protected String source;

	protected Path outpath;

	private List<CompilationUnit> sourceClasses;
	
	private Map<String, MethodDeclaration> sourceMethodMap;

	private boolean domainSplit;
	private DomainClassGeneratorDelegate delegate;

	public ClassGeneratorImpl(Path outpath) {
		this.outpath = outpath;
	}

	public ClassGeneratorImpl(Path inpath, String source, Path outpath) {
		this.inpath = inpath;
		this.source = source;
		this.outpath = outpath;
		loadSourceClass();
	}

	public ClassGeneratorImpl(Path inpath, String source, Path outpath, boolean domainSplit) {
		this(inpath, source, outpath);
		this.domainSplit = domainSplit;
		if (domainSplit) {
			delegate = new DomainClassGeneratorDelegate(this);
		}
	}

	@Override
	public void generateClasses(List<EndpointInfo> clazzDefs) throws IOException {
		for (EndpointInfo clazzDef : clazzDefs) {

			String packageName = clazzDef.getPackageName();
			packageName = packageName.replace('.', File.separatorChar);

			File packageDir = outpath.resolve(packageName).toFile();
			packageDir.mkdirs();

			File clazzFile = new File(packageDir, getClassFileName(clazzDef.getClassName()) + ".java");
			try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(clazzFile)))) {
				writePackageName(writer, clazzDef);
				writeImports(writer, clazzDef);
				writeServiceClass(writer, clazzDef);
			}
		}
		if (this.domainSplit) {
			delegate.writeDomianServiceClassEnd();
		}
	}

	private void loadSourceClass() {
		if (inpath == null || source == null) {
			return;
		}
		File sourePath = inpath.resolve(source.replace('.', '/')).toFile();
		if (sourePath.isDirectory()) {
			sourceClasses = new LinkedList<>();
			File[] sourceTypes = sourePath.listFiles();
			for (File sourceType : sourceTypes) {
				if (sourceType.isFile()) {
					sourceClasses.add(parse(sourceType));
				}
			}

		} else {
			File sourceType = inpath.resolve(source.replace('.', '/') + ".java").toFile();
			if (sourceType.exists()) {
				sourceClasses = Collections.singletonList(parse(sourceType));
			}
		}
		
		mapSourceMethods();
	}

	private void mapSourceMethods() {
		sourceMethodMap = new LinkedHashMap<>();
		for (CompilationUnit sourceClass : sourceClasses) {
			new VoidVisitorAdapter<Object>() {
				@Override
				public void visit(MethodDeclaration decl, Object obj) {
					sourceMethodMap.put(decl.getName(), decl);
				}
			}.visit(sourceClass, null);
		}		
	}

	private CompilationUnit parse(File javaFile) {
		CompilationUnit sourceClass;
		try (InputStream in = new FileInputStream(javaFile)) {
			sourceClass = JavaParser.parse(in);
		} catch (ParseException | IOException ex) {
			throw new IllegalStateException(ex);
		}
		return sourceClass;
	}

	protected String getClassFileName(String className) {
		return className + "Template";
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

	protected void writeServiceClass(PrintWriter writer, EndpointInfo clazzDef) throws IOException {
		if (clazzDef.getClassName() != null) {
			writer.println("public class " + getClassFileName(clazzDef.getClassName()) + " {\n");
			writeFields(writer);
			writeMethods(writer, clazzDef);
			writer.println("}");
			writer.println();
		}
	}

	protected void writeFields(PrintWriter writer) {
		String sourceFields = getSourceFields();
		if (sourceFields.length() > 0) {
			writer.println(sourceFields);
		}
	}

	private String getSourceFields() {
		if (!isSourceAvailable()) {
			return "";
		}
		final StringBuilder result = new StringBuilder();
		for (CompilationUnit sourceClass : sourceClasses) {
			new VoidVisitorAdapter<Object>() {
				@Override
				public void visit(FieldDeclaration decl, Object obj) {
					result.append("\t").append(decl).append("\n");
				}
			}.visit(sourceClass, null);
		}
		return result.toString();
	}

	protected void writeMethods(PrintWriter writer, EndpointInfo clazzDef) throws IOException {
		List<? extends MethodInfo> methods = clazzDef.getMethods();
		if (methods != null) {
			for (MethodInfo minfo : methods) {
				writeMethod(writer, clazzDef, minfo);
				if (this.domainSplit) {
					delegate.writeMethod(clazzDef, minfo);
				}
			}
		}
	}

	protected void writeMethod(PrintWriter writer, EndpointInfo clazzDef, MethodInfo minfo) throws IOException {
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
			ParamInfo param = params.get(i);
			String type = param.getParamType();
			String name = getParamName(param.getParamName(), i, sourceParams);
			writer.print(i == 0 ? "" : ", ");
			writer.print(type + " " + name);
		}
	}

	protected String getParamName(String name, int i, String[] sourceParams) {
		if (sourceParams != null && sourceParams.length > i) {
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
		if(!isSourceAvailable()) {
			return "{ }";
		}
		final StringBuilder result = new StringBuilder();
		MethodDeclaration decl = sourceMethodMap.get(methodName);
		if (decl != null) {
			result.append(decl.getBody().toStringWithoutComments());
			return result.toString().replace("\n", "\n\t");
		} else {
			return "{ }";			
		}

	}

	protected String[] getSourceMethodParams(String methodName) {
		if(!isSourceAvailable()) {
			return null;
		}
		final List<String> result = new ArrayList<String>();
		MethodDeclaration decl = sourceMethodMap.get(methodName);
		if (decl != null) {
			for (Parameter p : decl.getParameters()) {
				result.add(p.getId().getName());
			}
			return result.toArray(new String[] {});
		} else {
			return null;
		}
	}
	
	public boolean isSourceMethodAvailable(String methodName) {
		MethodDeclaration decl = sourceMethodMap.get(methodName);
		return decl != null;
	}


	public boolean isSourceAvailable() {
		return sourceClasses != null && sourceClasses.size() > 0;
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
						if (result.length() == 0
								&& (decl.getName().startsWith("get") || decl.getName().startsWith("Get"))) {
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
