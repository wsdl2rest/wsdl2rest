package org.jboss.fuse.wsdl2rest.impl.service;

import org.jboss.fuse.wsdl2rest.ElementInfo;

public class ElementInfoImpl implements ElementInfo {

	private final String elementType;
	private final String elementName;
	
	private final boolean complex;

	public ElementInfoImpl(String elementType, String elementName, boolean complex) {
		this.elementType = elementType;
		this.elementName = elementName;
		this.complex = complex;
	}

	@Override
	public String getElementType() {
		return this.elementType;
	}

	@Override
	public String getElementName() {
		return this.elementName;
	}
	
	@Override
	public boolean isComplex() {
		return this.complex;
	}

	public String toString() {
		return "[name=" + elementName + ", type=" + elementType + ", complex=" + complex +"]";
	}
}