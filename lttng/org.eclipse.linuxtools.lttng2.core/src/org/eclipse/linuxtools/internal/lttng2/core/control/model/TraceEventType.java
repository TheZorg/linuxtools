/**********************************************************************
 * Copyright (c) 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.internal.lttng2.core.control.model;

/**
 * <p>
 * Trace event type enumeration.
 * </p>
 * 
 * @author Bernd Hufmann
 */
public enum TraceEventType { 
    TRACEPOINT("tracepoint"), //$NON-NLS-1$
    SYSCALL("syscall"), //$NON-NLS-1$
    PROBE("probe"),  //$NON-NLS-1$
    UNKNOWN("unknown"); //$NON-NLS-1$

    private final String fInName;

    private TraceEventType(String name) {
        fInName = name;
    }

    public String getInName() {
        return fInName;
    }
};
