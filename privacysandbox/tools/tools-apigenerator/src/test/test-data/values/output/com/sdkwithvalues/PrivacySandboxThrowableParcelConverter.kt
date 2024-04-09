package com.sdkwithvalues

import java.lang.StackTraceElement
import kotlin.coroutines.cancellation.CancellationException

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

    public fun toThrowableParcel(throwable: Throwable): PrivacySandboxThrowableParcel {
        val parcel = PrivacySandboxThrowableParcel()
        parcel.exceptionClass = throwable::class.qualifiedName
        parcel.errorMessage = throwable.message
        parcel.stackTrace = throwable.stackTrace.map {
            val stackFrame = ParcelableStackFrame()
            stackFrame.declaringClass = it.className
            stackFrame.methodName = it.methodName
            stackFrame.fileName = it.fileName
            stackFrame.lineNumber = it.lineNumber
            stackFrame
        }.toTypedArray()
        throwable.cause?.let {
            parcel.cause = arrayOf(toThrowableParcel(it))
        }
        parcel.suppressedExceptions =
            throwable.suppressedExceptions.map {
                toThrowableParcel(it)
            }.toTypedArray()
        parcel.isCancellationException = throwable is CancellationException
        return parcel
    }
}
