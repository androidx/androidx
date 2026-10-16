package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionSerializable(isDescribedByKDoc = false)
data class SetField<T>(val value: T)

@AppFunctionSerializable(isDescribedByKDoc = false)
data class UpdateNoteParams(
    val content: SetField<Byte>? = null, // Unsupported type "Byte"
)

@AppFunctionServiceEntryPoint("FunctionWithGenericSerializableService", "app_functions_v2")
abstract class FunctionWithGenericSerializable : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    suspend fun test(params: UpdateNoteParams) {}
}