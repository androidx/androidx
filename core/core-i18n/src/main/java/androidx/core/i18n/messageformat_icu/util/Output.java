/*
 *******************************************************************************
 * Copyright (C) 2011-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package androidx.core.i18n.messageformat_icu.util;

import androidx.annotation.RestrictTo;

/**
 * Simple struct-like class for output parameters.
 * @param <T> The type of the parameter.
 * icu_annot::stable ICU 4.8
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Output<T> {
    /**
     * The value field
     * icu_annot::stable ICU 4.8
     */
    public T value;

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 4.8
     */
    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }

    /**
     * Constructs an empty <code>Output</code>
     * icu_annot::stable ICU 4.8
     */
    public Output() {
        
    }

    /**
     * Constructs an <code>Output</code> withe the given value.
     * @param value the initial value
     * icu_annot::stable ICU 4.8
     */
    public Output(T value) {
        this.value = value;
    }
}
