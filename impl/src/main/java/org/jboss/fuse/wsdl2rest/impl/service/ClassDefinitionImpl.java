package org.jboss.fuse.wsdl2rest.impl.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.fuse.wsdl2rest.EndpointInfo;
import org.jboss.fuse.wsdl2rest.MethodInfo;
import org.jboss.fuse.wsdl2rest.TypeInfo;

public class ClassDefinitionImpl extends MetaInfoImpl implements EndpointInfo {

    private String packageName;
    private List<String> imports;
    private String className;
    private Map<String, MethodInfo> methods = new LinkedHashMap<>();
    private Map<String, TypeInfo> types = new LinkedHashMap<>();

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public List<MethodInfo> getMethods() {
        List<MethodInfo> result = new ArrayList<>(methods.values());
        result.sort(Comparator.comparing(MethodInfo::getMethodName));
        return Collections.unmodifiableList(result);
    }

    @Override
   public MethodInfo getMethod(String methodName) {
        return methods.get(methodName);
    }

    public void addMethod(MethodInfo method) {
        methods.put(method.getMethodName(), method);
    }
    
    @Override
    public List<TypeInfo> getTypes() {
        List<TypeInfo> result = new ArrayList<>(types.values());
        result.sort(Comparator.comparing(TypeInfo::getTypeName));
        return Collections.unmodifiableList(result);
    }
    
    @Override
    public TypeInfo getType(String type) {
    	return types.get(type);
    }
    
	public void addType(TypeInfo type) {
		types.put(type.getTypeName(), type);
    }
    
    @Override
    public String getFQN() {
        return packageName + "." + className;
    }

    @Override
    public String toString() {
        return getFQN();
    }
}