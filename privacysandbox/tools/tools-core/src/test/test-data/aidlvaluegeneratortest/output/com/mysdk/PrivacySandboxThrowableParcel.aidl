package com.mysdk;

import com.mysdk.ParcelableStackFrame;

parcelable PrivacySandboxThrowableParcel {
    ParcelableStackFrame[] stackTrace;
    PrivacySandboxThrowableParcel[] cause;
    PrivacySandboxThrowableParcel[] suppressedExceptions;
    String errorMessage;
    String exceptionClass;
    boolean isCancellationException;
}