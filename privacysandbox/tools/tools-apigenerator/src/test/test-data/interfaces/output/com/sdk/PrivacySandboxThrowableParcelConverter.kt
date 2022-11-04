package com.sdk

import java.lang.StackTraceElement

public object PrivacySandboxThrowableParcelConverter {
    public fun fromThrowableParcel(throwableParcel: PrivacySandboxThrowableParcel): Throwable {
        val exceptionClass = throwableParcel.exceptionClass
        val errorMessage = throwableParcel.errorMessage
        val stackTrace = throwableParcel.stackTrace
        val exception = PrivacySandboxException(
            "[$exceptionClass] $errorMessage",
            throwableParcel.cause?.firstOrNull()?.let {
                fromThrowableParcel(it)
            }
        )
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
