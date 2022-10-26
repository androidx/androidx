package com.mysdk;

import com.mysdk.ParcelableStackFrame;

parcelable PrivacySandboxThrowableParcel {
    @nullable(heap=true) PrivacySandboxThrowableParcel cause;
    ParcelableStackFrame[] stackTrace;
    PrivacySandboxThrowableParcel[] suppressedExceptions;
    String errorMessage;
    String exceptionClass;
}