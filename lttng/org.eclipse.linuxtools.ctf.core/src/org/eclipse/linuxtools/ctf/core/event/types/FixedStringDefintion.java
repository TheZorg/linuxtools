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

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.linuxtools.ctf.core.event.scope.IDefinitionScope;

import com.google.common.collect.ImmutableList;

/**
 * A fixed length string definition
 * @author Matthew Khouzam
 * @since 3.0
 */
@NonNullByDefault
public final class FixedStringDefintion extends ArrayDefinition {

    private final String fContent;
    @Nullable private transient List<Definition> fDefs;

    /**
     * An fixed length string declaration, it's created by sequence or array
     * defintions
     *
     * @param declaration
     *            the declaration
     * @param definitionScope
     *            the definition scope
     * @param fieldName
     *            the field name
     * @param content
     *            the string content
     */
    public FixedStringDefintion(Declaration declaration,
            @Nullable IDefinitionScope definitionScope,
            String fieldName,
            String content) {
        super(declaration, definitionScope, fieldName);
        fContent = content;

    }

    @SuppressWarnings("null")
    @Override
    public List<Definition> getDefinitions() {
        if (fDefs == null) {
            ImmutableList.Builder<Definition> builder = new ImmutableList.Builder<>();
            for (int i = 0; i < ((ArrayDeclaration) getDeclaration()).getLength(); i++) {
                builder.add(new IntegerDefinition(IntegerDeclaration.UINT_8_DECL, getDefinitionScope(), getFieldName() + '[' + i + ']', fContent.charAt(i)));
            }
            fDefs = builder.build();
        }
        return (fDefs == null? Collections.EMPTY_LIST: fDefs);
    }

    @Nullable
    @Override
    public Definition getElem(int i) {
        if( i > ((ArrayDeclaration)getDeclaration()).getLength()) {
            return null;
        }
        return getDefinitions().get(i);
    }

    @Override
    public String toString() {
        return fContent;
    }
}
