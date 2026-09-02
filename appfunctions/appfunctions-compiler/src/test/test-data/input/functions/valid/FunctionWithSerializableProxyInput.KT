package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import java.time.LocalDateTime

class FunctionWithSerializableProxyInput {
    @AppFunctionDeclaration
    fun simpleFunctionWithLocalDateTime(
        appFunctionContext: AppFunctionContext,
        localDateTime: LocalDateTime
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionWithSerializableWithProxy(
        appFunctionContext: AppFunctionContext,
        serializableWithProxyType: SerializableWithProxyType
    ) {}
}
