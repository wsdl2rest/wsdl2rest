package org.jboss.fuse.wsdl2rest.impl.service;

import org.jboss.fuse.wsdl2rest.ElementInfo;

public class ElementInfoImpl implements ElementInfo {

	private final String elementType;
	private final String elementName;

	public ElementInfoImpl(String elementType, String elementName) {
		this.elementType = elementType;
		this.elementName = elementName;
	}

	@Override
	public String getElementType() {
		return this.elementType;
	}

	@Override
	public String getElementName() {
		return this.elementName;
	}

	public String toString() {
		return "[name=" + elementName + ",type=" + elementType + "]";
	}
}