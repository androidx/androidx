package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSerializableInterface

@AppFunctionSerializableInterface
interface Note {
    val id: String
}

@AppFunctionSerializable
data class UpdateNoteParams(
    val content: Note,
)

class FunctionWithNestedSerializableInterface {
    @AppFunctionDeclaration
    suspend fun test(appFunctionContext: AppFunctionContext, params: UpdateNoteParams) {}
}