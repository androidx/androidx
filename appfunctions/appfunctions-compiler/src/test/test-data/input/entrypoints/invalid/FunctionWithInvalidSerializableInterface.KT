package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSerializableInterface
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionSerializableInterface
interface Note {
    val id: String
}

@AppFunctionSerializable(isDescribedByKDoc = false)
data class UpdateNoteParams(
    val content: Note,
)

@AppFunctionServiceEntryPoint("FunctionWithNestedSerializableInterfaceService", "app_functions_v2")
abstract class FunctionWithNestedSerializableInterface : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    suspend fun test(params: UpdateNoteParams) {}
}