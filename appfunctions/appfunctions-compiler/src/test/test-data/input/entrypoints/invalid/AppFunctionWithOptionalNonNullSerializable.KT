package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionSerializable(isDescribedByKDoc = false)
class SerializableData(val intValue: Int)

@AppFunctionServiceEntryPoint("AppFunctionWithOptionalNonNullSerializableService", "app_functions_v2")
abstract class AppFunctionWithOptionalNonNullSerializable : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun withOptionalNonNullSerializable(
        optionalNonNullSerializable: SerializableData = SerializableData(0)
    ) {}
}