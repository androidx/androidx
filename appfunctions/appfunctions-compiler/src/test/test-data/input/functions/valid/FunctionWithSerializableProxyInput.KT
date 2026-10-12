package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import java.time.LocalDateTime

class FunctionWithSerializableProxyInput {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithLocalDateTime(
        appFunctionContext: AppFunctionContext,
        localDateTime: LocalDateTime
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithSerializableWithProxy(
        appFunctionContext: AppFunctionContext,
        serializableWithProxyType: SerializableWithProxyType
    ) {}
}
