package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

/** An example generic AppFunctionSerializable. */
@AppFunctionSerializable(isDescribedByKdoc = true)
data class SetField<T>(val value: T)

/** An attachment for a note entity. */
@AppFunctionSerializable(isDescribedByKdoc = true)
data class Attachment(val uri: String)

/** Parameters to update a note entity. */
@AppFunctionSerializable(isDescribedByKdoc = true)
data class UpdateNoteParams(
    val title: SetField<String>? = null,
    val nullableTitle: SetField<String?>? = null,
    val attachments: SetField<List<Attachment>>? = null,
    val nullableAttachments: SetField<List<Attachment>?>? = null,
)

class FunctionWithGenericSerializable {
    @AppFunction
    suspend fun test(
        appFunctionContext: AppFunctionContext,
        params: UpdateNoteParams,
        topLevelGenericParam: SetField<Int>?
    ) {}
}