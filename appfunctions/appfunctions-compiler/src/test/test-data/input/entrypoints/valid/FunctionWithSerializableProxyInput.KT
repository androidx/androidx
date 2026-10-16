package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import java.time.LocalDateTime

@AppFunctionServiceEntryPoint("FunctionWithSerializableProxyInputService", "app_functions_v2")
abstract class FunctionWithSerializableProxyInput : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithLocalDateTime(
        localDateTime: LocalDateTime
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithSerializableWithProxy(
        serializableWithProxyType: SerializableWithProxyType
    ) {}
}
