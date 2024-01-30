package com.sdkwithvalues

import java.lang.StackTraceElement

public object PrivacySandboxThrowableParcelConverter {
    public fun fromThrowableParcel(throwableParcel: PrivacySandboxThrowableParcel): Throwable {
        val exceptionClass = throwableParcel.exceptionClass
        val stackTrace = throwableParcel.stackTrace
        val errorMessage = "[$exceptionClass] ${throwableParcel.errorMessage}"
        val cause = throwableParcel.cause?.firstOrNull()?.let {
            fromThrowableParcel(it)
        }
        val exception = if (throwableParcel.isCancellationException) {
            PrivacySandboxCancellationException(errorMessage, cause)
        } else {
            PrivacySandboxException(errorMessage, cause)
        }
        for (suppressed in throwableParcel.suppressedExceptions) {
            exception.addSuppressed(fromThrowableParcel(suppressed))
        }
        exception.stackTrace =
            stackTrace.map {
                StackTraceElement(
                    it.declaringClass,
                    it.methodName,
                    it.fileName,
                    it.lineNumber
                )
            }.toTypedArray()
        return exception
    }
}
