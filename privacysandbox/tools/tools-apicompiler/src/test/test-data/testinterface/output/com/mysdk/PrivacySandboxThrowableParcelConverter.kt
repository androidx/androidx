package com.mysdk

public object PrivacySandboxThrowableParcelConverter {
    public fun toThrowableParcel(throwable: Throwable): PrivacySandboxThrowableParcel {
        val parcel = PrivacySandboxThrowableParcel()
        parcel.exceptionClass = throwable::class.qualifiedName
        parcel.errorMessage = throwable.message
        parcel.stackTrace = throwable.stackTrace.map {
            val parcel = ParcelableStackFrame()
            parcel.declaringClass = it.className
            parcel.methodName = it.methodName
            parcel.fileName = it.fileName
            parcel.lineNumber = it.lineNumber
            parcel
        }.toTypedArray()
        throwable.cause?.let {
            parcel.cause = toThrowableParcel(it)
        }
        parcel.suppressedExceptions =
            throwable.suppressedExceptions.map {
                toThrowableParcel(it)
            }.toTypedArray()
        return parcel
    }
}
