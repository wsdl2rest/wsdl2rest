package org.jboss.fuse.wsdl2rest;

import java.util.List;

public interface TypeInfo {

	String getTypeName();

	List<ElementInfo> getElements();

}