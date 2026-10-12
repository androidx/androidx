package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSerializableInterface

@AppFunctionSerializable(isDescribedByKDoc = false)
data class SetField<T>(val value: T)

@AppFunctionSerializableInterface
interface Note {
    val id: String
}

@AppFunctionSerializable(isDescribedByKDoc = false)
data class UpdateNoteParams(
    val content: SetField<Note>? = null,
)

class FunctionWithGenericSerializable {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    suspend fun test(appFunctionContext: AppFunctionContext, params: UpdateNoteParams) {}
}
