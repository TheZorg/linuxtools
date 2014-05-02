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

import org.eclipse.jdt.annotation.NonNull;

/**
 * A fixed size struct declaration is a declaration of a structure that has no
 * variant or sequence fields. This will accelerate reading of the trace.
 *
 * @author Matthew
 * @since 3.0
 */
public class FixedSizeStructDeclaration extends StructDeclaration {

    /**
     * Check if this struct is fixed size
     *
     * @param sd
     *            the struct
     * @return if the struct is of fixed size
     */
    public static boolean canBeFlattened(@NonNull StructDeclaration sd) {
        for (String field : sd.getFieldsList()) {
            IDeclaration dec = sd.getFields().get(field);
            if (!isFixedSize(dec)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFixedSize(IDeclaration dec) {
        if ((dec instanceof IntegerDeclaration) ||
                (dec instanceof EnumDeclaration) ||
                (dec instanceof FloatDeclaration)) {
            return true;
        }
        if (dec instanceof ArrayDeclaration) {
            return isFixedSize(((ArrayDeclaration) dec).getElementType());
        }
        if (dec instanceof StructDeclaration) {
            StructDeclaration sDec = ((StructDeclaration) dec);
            return canBeFlattened(sDec);
        }
        return false;
    }

    /**
     * Copy constructor
     *
     * @param sd
     *            the struct declaration
     */
    public FixedSizeStructDeclaration(StructDeclaration sd) {
        super(sd.getAlignment());
        for (String name : sd.getFieldsList()) {
            depthFirstAdd(name, sd.getFields().get(name));
        }
    }

    private void depthFirstAdd(String path, IDeclaration dec) {
        if ((dec instanceof IntegerDeclaration) ||
                (dec instanceof EnumDeclaration) ||
                (dec instanceof FloatDeclaration)) {
            addField(path, dec);
            return;
        }
        if (dec instanceof ArrayDeclaration) {
            ArrayDeclaration ad = (ArrayDeclaration) dec;
            int lastIndexOf = path.lastIndexOf('.');

            String name = (lastIndexOf > 0) ? path.substring(lastIndexOf) : path;
            if( ((ArrayDeclaration) dec).isString() ) {
                addField(path, dec);
            } else {
                for (int i = 0; i < ad.getLength(); i++) {
                    depthFirstAdd(path + '.' + name + '[' + i + ']', ad.getElementType());
                }
            }
        }
        if (dec instanceof StructDeclaration) {
            StructDeclaration sDec = ((StructDeclaration) dec);
            for (String name : sDec.getFieldsList()) {
                depthFirstAdd(path + '.' + name, sDec.getFields().get(name));
            }
        }
    }

}
