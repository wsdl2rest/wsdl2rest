package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;

public class DomainClassGeneratorDelegate {

	private Pattern[] httpWords = new Pattern[] { //
			Pattern.compile("[Gg]et|[Rr]ead|[Ff]etch|[Ll]ist"), //
			Pattern.compile("[Pp]ost|[Aa]dd|[Cc]reate"), //
			Pattern.compile("[Pp]ut|[Ss]et|[Uu]pd|[Mm]od"), //
			Pattern.compile("[Dd]el|[Rr]em") };

	private Map<String, PrintWriter> domainWriters = new LinkedHashMap<>();

	private ClassGeneratorImpl codeGen;
	
	public DomainClassGeneratorDelegate(ClassGeneratorImpl codeGen) {
		this.codeGen = codeGen;
	}

	private String getDomain(MethodInfo minfo) {
		String domain;
		for (Pattern httpWord : httpWords) {
			Matcher matcher = httpWord.matcher(minfo.getMethodName());
			if (matcher.find()) {
				String group = matcher.group();
				domain = minfo.getMethodName().replaceFirst(group, "");
				return domain;
			}
		}
		return minfo.getMethodName();
	}

	private PrintWriter getDomainWriter(EndpointInfo clazzDef, MethodInfo minfo) throws IOException {
		if (codeGen.isSourceAvailable()) {
			if (!codeGen.isSourceMethodAvailable(minfo.getMethodName())) {
				return null;
			}
		}
		String domain = getDomain(minfo);
		PrintWriter writer = domainWriters.get(domain);
		if (writer == null) {

			String packageName = clazzDef.getPackageName();
			packageName = packageName.replace('.', File.separatorChar);

			File packageDir = codeGen.outpath.resolve(packageName).toFile();
			packageDir.mkdirs();

			File clazzFile = new File(packageDir, codeGen.getClassFileName(domain) + ".java");
			writer = new PrintWriter(new FileWriter(clazzFile));
			domainWriters.put(domain, writer);

			codeGen.writePackageName(writer, clazzDef);
			codeGen.writeImports(writer, clazzDef);
			writeDomianServiceClassStart(writer, domain);
		}
		return writer;
	}

	protected void writeDomianServiceClassStart(PrintWriter writer, String domain) {
		writer.println("public class " + codeGen.getClassFileName(domain) + " {\n");
		codeGen.writeFields(writer);
	}

	protected void writeDomianServiceClassEnd() {
		for (PrintWriter writer : domainWriters.values()) {
			writer.println("}");
			writer.close();
		}
	}
	
	protected void writeMethod(EndpointInfo clazzDef, MethodInfo minfo) throws IOException {
		PrintWriter domainWriter = getDomainWriter(clazzDef, minfo);
		if(domainWriter != null) {
			codeGen.writeMethod(domainWriter, clazzDef, minfo);
		}
	}
	
}
