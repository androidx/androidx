package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import java.time.LocalDateTime

class FunctionWithSerializableProxyInput {
    @AppFunction
    fun simpleFunctionWithLocalDateTime(
        appFunctionContext: AppFunctionContext,
        localDateTime: LocalDateTime
    ) {}

    @AppFunction
    fun simpleFunctionWithSerializableWithProxy(
        appFunctionContext: AppFunctionContext,
        serializableWithProxyType: SerializableWithProxyType
    ) {}
}
