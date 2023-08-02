/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package androidx.core.i18n.messageformat_icu.util;

import androidx.annotation.RestrictTo;

/**
 * Unchecked version of {@link CloneNotSupportedException}.
 * Some ICU APIs do not throw the standard exception but instead wrap it
 * into this unchecked version.
 *
 * icu_annot::draft ICU 53
 * icu_annot::provisional This API might change or be removed in a future release.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ICUCloneNotSupportedException extends ICUException {
    private static final long serialVersionUID = -4824446458488194964L;

    /**
     * Default constructor.
     *
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception (normally a {@link CloneNotSupportedException})
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception (normally a {@link CloneNotSupportedException})
     * icu_annot::draft ICU 53
     * icu_annot::provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
