package org.eclipse.linuxtools.ctf.core.event.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.linuxtools.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.linuxtools.ctf.core.event.scope.LexicalScope;

/**
 * Scoped defintion. a defintion where you can lookup various datatypes
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
@NonNullByDefault
public abstract class ScopedDefinition extends Definition implements IDefinitionScope {

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
    public ScopedDefinition(IDeclaration declaration, @Nullable IDefinitionScope definitionScope, String fieldName) {
        super(declaration, definitionScope, fieldName);
    }

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
     * @param scope
     *            the scope of the definition
     * @since 3.0
     */

    public ScopedDefinition(IDeclaration declaration, @Nullable IDefinitionScope definitionScope, String fieldName, LexicalScope scope) {
        super(declaration, definitionScope, fieldName, scope);
    }

    /**
     * Lookup an array in a struct. If the name returns a non-array (like an
     * int) than the method returns null
     *
     * @param name
     *            the name of the array
     * @return the array or null.
     * @since 3.0
     */
    @Nullable
    public ArrayDefinition lookupArray(String name) {
        Definition def = lookupDefinition(name);
        return (ArrayDefinition) ((def.getDeclaration() instanceof ArrayDeclaration) ? def : null);
    }

    /**
     * Lookup an enum in a struct. If the name returns a non-enum (like an int)
     * than the method returns null
     *
     * @param name
     *            the name of the enum
     * @return the enum or null.
     */
    @Nullable
    public EnumDefinition lookupEnum(String name) {
        Definition def = lookupDefinition(name);
        return (EnumDefinition) ((def instanceof EnumDefinition) ? def : null);
    }

    /**
     * Lookup an integer in a struct. If the name returns a non-integer (like an
     * float) than the method returns null
     *
     * @param name
     *            the name of the integer
     * @return the integer or null.
     */
    @Nullable
    public IntegerDefinition lookupInteger(String name) {
        Definition def = lookupDefinition(name);
        return (IntegerDefinition) ((def instanceof IntegerDefinition) ? def : null);
    }

    /**
     * Lookup a sequence in a struct. If the name returns a non-sequence (like
     * an int) than the method returns null
     *
     * @param name
     *            the name of the sequence
     * @return the sequence or null.
     * @since 3.0
     */
    @Nullable
    public ArrayDefinition lookupSequence(String name) {
        Definition def = lookupDefinition(name);
        return (ArrayDefinition) ((def.getDeclaration() instanceof SequenceDeclaration) ? def : null);
    }

    /**
     * Lookup a string in a struct. If the name returns a non-string (like an
     * int) than the method returns null
     *
     * @param name
     *            the name of the string
     * @return the string or null.
     */
    @Nullable
    public StringDefinition lookupString(String name) {
        Definition def = lookupDefinition(name);
        return (StringDefinition) ((def instanceof StringDefinition) ? def : null);
    }

    /**
     * Lookup a struct in a struct. If the name returns a non-struct (like an
     * int) than the method returns null
     *
     * @param name
     *            the name of the struct
     * @return the struct or null.
     */
    @Nullable
    public StructDefinition lookupStruct(String name) {
        Definition def = lookupDefinition(name);
        return (StructDefinition) ((def instanceof StructDefinition) ? def : null);
    }

    /**
     * Lookup a variant in a struct. If the name returns a non-variant (like an
     * int) than the method returns null
     *
     * @param name
     *            the name of the variant
     * @return the variant or null.
     */
    @Nullable
    public VariantDefinition lookupVariant(String name) {
        Definition def = lookupDefinition(name);
        return (VariantDefinition) ((def instanceof VariantDefinition) ? def : null);
    }
}
