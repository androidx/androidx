package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
data class SetField<T>(val value: T)

@AppFunctionSerializable(isDescribedByKDoc = false)
data class UpdateNoteParams(
    val content: SetField<Byte>? = null, // Unsupported type "Byte"
)

class FunctionWithGenericSerializable {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    suspend fun test(appFunctionContext: AppFunctionContext, params: UpdateNoteParams) {}
}