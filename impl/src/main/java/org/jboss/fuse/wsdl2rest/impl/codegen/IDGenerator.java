package org.jboss.fuse.wsdl2rest.impl.codegen;

import java.util.UUID;

public class IDGenerator {
        
    public String getNext() {
        return UUID.randomUUID().toString();
    }
}