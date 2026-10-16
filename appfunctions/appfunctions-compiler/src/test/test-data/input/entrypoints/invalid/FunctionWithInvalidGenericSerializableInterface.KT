package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSerializableInterface
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

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

@AppFunctionServiceEntryPoint("FunctionWithGenericSerializableService", "app_functions_v2")
abstract class FunctionWithGenericSerializable : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    suspend fun test(params: UpdateNoteParams) {}
}
