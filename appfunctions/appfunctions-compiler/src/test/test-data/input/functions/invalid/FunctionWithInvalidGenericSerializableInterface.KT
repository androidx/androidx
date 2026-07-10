package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSerializableInterface

@AppFunctionSerializable
data class SetField<T>(val value: T)

@AppFunctionSerializableInterface
interface Note {
    val id: String
}

@AppFunctionSerializable
data class UpdateNoteParams(
    val content: SetField<Note>? = null,
)

class FunctionWithGenericSerializable {
    @AppFunction
    suspend fun test(appFunctionContext: AppFunctionContext, params: UpdateNoteParams) {}
}
