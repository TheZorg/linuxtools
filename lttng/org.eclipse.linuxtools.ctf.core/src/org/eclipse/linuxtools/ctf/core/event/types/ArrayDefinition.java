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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.linuxtools.ctf.core.event.scope.IDefinitionScope;

/**
 * Array definition, used for compound definitions and fixed length strings
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
@NonNullByDefault
public abstract class ArrayDefinition extends Definition {

    /**
     * Constructor
     *
     * @param declaration
     *            the event declaration
     *
     * @param definitionScope
     *            the definition is in a scope, (normally a struct) what is it?
     * @param fieldName
     *            the name of the definition. (it is a field in the parent
     *            scope)
     * @since 3.0
     */
    public ArrayDefinition(IDeclaration declaration, @Nullable IDefinitionScope definitionScope, String fieldName) {
        super(declaration, definitionScope, fieldName);
    }

    /**
     * @return the definitions
     * @since 3.0
     */
    public abstract List<Definition> getDefinitions();

    /**
     * Get the element at i
     *
     * @param i
     *            the index (cannot be negative)
     * @return The element at I, if I &gt; length, null, if I &lt; 0, the method
     *         throws an out of bounds exception
     */
    @Nullable
    public abstract Definition getElem(int i);

}