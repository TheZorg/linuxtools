/*******************************************************************************
 * Copyright (c) 2011-2012 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.ctf.core.event.types;

import org.eclipse.linuxtools.internal.ctf.core.event.io.BitBuffer;

public class FloatDefinition extends Definition {
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final FloatDeclaration declaration;
    private double value;

    // ------------------------------------------------------------------------
    // Contructors
    // ------------------------------------------------------------------------

    public FloatDefinition(FloatDeclaration declaration,
            IDefinitionScope definitionScope, String fieldName) {
        super(definitionScope, fieldName);
        this.declaration = declaration;
    }
    // ------------------------------------------------------------------------
    // Gettters/Setters/Predicates
    // ------------------------------------------------------------------------

    public double getValue() {
        return value;
    }

    public void setValue(double val) {
        value = val;
    }

    public FloatDeclaration getDeclaration() {
        return declaration;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------



    @Override
    public void read(BitBuffer input) {
        int exp = declaration.getExponent();
        int mant = declaration.getMantissa();
        if( (exp + mant) == 32 ){
            value = readRawFloat32(input, mant , exp);
        }
        else if((exp + mant) == 64)
        {
            value = readRawFloat64(input, mant,exp);
        }
        else
        {
            value = Double.NaN;
        }
    }



    private static double readRawFloat64(BitBuffer input, final int manBits, final int expBits) {
       long low = input.getInt(32, false);
       low = low & 0x00000000FFFFFFFFL;
       long high = input.getInt(32, false);
       high = high & 0x00000000FFFFFFFFL;
       long temp = (high << 32) | low;
       return createFloat(temp, manBits-1, expBits);
    }

    /**
     * @param rawValue
     * @param manBits
     * @param expBits
     */
    private static double createFloat(long rawValue, final int manBits, final int expBits) {
        long manShift = 1L << (manBits);
        long manMask = manShift - 1;
        long expMask = (1L << expBits) - 1;

        int exp = (int) ((rawValue >> (manBits))&expMask)+1;
        long man = (rawValue & manMask);
        double expPow = Math.pow(2.0, exp - (1 << (expBits - 1)));
        double ret = man * 1.0f;
        ret /= manShift;
        ret += 1.0;
        ret *= expPow;
        return ret;
    }

    private static double readRawFloat32(BitBuffer input, final int manBits,
    final int expBits) {
        long temp = input.getInt(32, false);
        return createFloat(temp, manBits-1, expBits);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}