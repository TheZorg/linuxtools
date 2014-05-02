/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.ctf.core.event.types;

import org.eclipse.linuxtools.ctf.core.event.io.BitBuffer;
import org.eclipse.linuxtools.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;

/**
 * An Lttng event header declaration is a declaration of a structure defined in
 * the ctf spec examples. It is used in Lttng traces. This will accelerate
 * reading of the trace.
 *
 * Reminder
 *
 * <pre>
 * struct event_header_large {
 *     enum : uint16_t { compact = 0 ... 65534, extended = 65535 } id;
 *     variant <id> {
 *         struct {
 *             uint32_clock_monotonic_t timestamp;
 *         } compact;
 *         struct {
 *             uint32_t id;
 *             uint64_clock_monotonic_t timestamp;
 *         } extended;
 *     } v;
 * } align(8);
 * </pre>
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
public class EventHeaderLargeDecl extends Declaration {
    private static final String ID = "id"; //$NON-NLS-1$
    private static final String V = "v"; //$NON-NLS-1$
    private static final String EXTENDED = "extended"; //$NON-NLS-1$
    private static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
    private static final String COMPACT = "compact"; //$NON-NLS-1$

    @Override
    public EventHeaderDef createDefinition(IDefinitionScope definitionScope, String fieldName, BitBuffer input) throws CTFReaderException {
        alignRead(input);
        int first = (int) input.get(16, false);
        int second = (int) input.get(32, false);
        if (first != 65535) {
            return new EventHeaderDef(this, first, second, 32);
        }
        long timestampLong = input.get(64, false);
        return new EventHeaderDef(this, second, timestampLong, 64);

    }

    @Override
    public long getAlignment() {
        return 8;
    }

    @Override
    public int getMaximumSize() {
        return 112;
    }

    /**
     * Check if a given struct declaration is an event header
     *
     * @param declaration
     *            the declaration
     * @return true if the
     */
    public static boolean isA(StructDeclaration declaration) {

        IDeclaration iDeclaration = declaration.getFields().get(ID);
        if (!(iDeclaration instanceof EnumDeclaration)) {
            return false;
        }
        EnumDeclaration eId = (EnumDeclaration) iDeclaration;
        if (eId.getContainerType().getLength() != 16) {
            return false;
        }
        iDeclaration = declaration.getFields().get(V);

        if (!(iDeclaration instanceof VariantDeclaration)) {
            return false;
        }
        VariantDeclaration vDec = (VariantDeclaration) iDeclaration;
        if (!vDec.hasField(COMPACT) || !vDec.hasField(EXTENDED)) {
            return false;
        }
        iDeclaration = vDec.getFields().get(COMPACT);
        StructDeclaration compactDec = (StructDeclaration) iDeclaration;
        if (!compactDec.hasField(TIMESTAMP)) {
            return false;
        }
        iDeclaration = compactDec.getFields().get(TIMESTAMP);
        IntegerDeclaration tsDec = (IntegerDeclaration) iDeclaration;
        if (tsDec.getLength() != 32 || tsDec.isSigned()) {
            return false;
        }
        iDeclaration = vDec.getFields().get(EXTENDED);
        StructDeclaration extendedDec = (StructDeclaration) iDeclaration;
        if (!extendedDec.hasField(TIMESTAMP)) {
            return false;
        }
        iDeclaration = extendedDec.getFields().get(TIMESTAMP);
        tsDec = (IntegerDeclaration) iDeclaration;
        if (tsDec.getLength() != 64 || tsDec.isSigned()) {
            return false;
        }
        iDeclaration = extendedDec.getFields().get(ID);
        if (!(iDeclaration instanceof IntegerDeclaration)) {
            return false;
        }
        IntegerDeclaration iId = (IntegerDeclaration) iDeclaration;
        if (iId.getLength() != 32 || iId.isSigned()) {
            return false;
        }
        return true;
    }

}
