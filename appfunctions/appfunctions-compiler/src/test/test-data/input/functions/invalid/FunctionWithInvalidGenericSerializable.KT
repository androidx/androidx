package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
data class SetField<T>(val value: T)

@AppFunctionSerializable
data class UpdateNoteParams(
    val content: SetField<Byte>? = null, // Unsupported type "Byte"
)

class FunctionWithGenericSerializable {
    @AppFunction
    suspend fun test(appFunctionContext: AppFunctionContext, params: UpdateNoteParams) {}
}