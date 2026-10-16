package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

/** An example generic AppFunctionSerializable. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SetField<T>(val value: T)

/** An attachment for a note entity. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Attachment(val uri: String)

/** Parameters to update a note entity. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class UpdateNoteParams(
    val title: SetField<String>? = null,
    val nullableTitle: SetField<String?>? = null,
    val attachments: SetField<List<Attachment>>? = null,
    val nullableAttachments: SetField<List<Attachment>?>? = null,
)

@AppFunctionServiceEntryPoint("FunctionWithGenericSerializableService", "app_functions_v2")
abstract class FunctionWithGenericSerializable : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    suspend fun test(
        params: UpdateNoteParams,
        topLevelGenericParam: SetField<Int>?
    ) {}
}