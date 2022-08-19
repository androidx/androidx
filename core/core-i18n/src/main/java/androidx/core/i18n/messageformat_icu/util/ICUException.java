/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package androidx.core.i18n.messageformat_icu.util;

import androidx.annotation.RestrictTo;

/**
 * Base class for unchecked, ICU-specific exceptions.
 *
 * icu_annot::draft ICU 53
 * icu_annot::provisional This API might change or be removed in a future release.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ICUException extends RuntimeException {
    private static final long serialVersionUID = -3067399656455755650L;

    /**
     * Default constructor.
     *
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUException(String message, Throwable cause) {
        super(message, cause);
    }
}
