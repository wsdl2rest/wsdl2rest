package org.jboss.fuse.wsdl2rest.impl.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.fuse.wsdl2rest.ElementInfo;
import org.jboss.fuse.wsdl2rest.TypeInfo;

public class TypeInfoImpl implements TypeInfo {

	private String typeName;
	private List<ElementInfo> elements = new LinkedList<>();;

	public TypeInfoImpl(String typeName) {
		this.typeName = typeName;
	}

	@Override
	public List<ElementInfo> getElements() {
		return Collections.unmodifiableList(elements);
	}

	public void addElement(ElementInfo element) {
		elements.add(element);
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

}