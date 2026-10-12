package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false) class SimpleSetField<T>(val value: T)

@AppFunctionSerializable(isDescribedByKDoc = false)
class SimpleNote(val title: String, val content: String, val attachment: SimpleAttachment) {
    @AppFunctionSerializable(isDescribedByKDoc = false)
    class SimpleAttachment(val displayName: String, val mimeType: String? = null)
}

@AppFunctionSerializable(isDescribedByKDoc = false)
class SimpleMessage(val title: String, val content: String, val attachment: SimpleAttachment) {
    @AppFunctionSerializable(isDescribedByKDoc = false)
    class SimpleAttachment(val displayName: String, val mimeType: String? = null)
}

@AppFunctionSerializable(isDescribedByKDoc = false)
class CreateSimpleNoteParams(
    val title: String,
    val content: String? = null,
    val attachments: List<SimpleNote.SimpleAttachment> = emptyList(),
)

@AppFunctionSerializable(isDescribedByKDoc = false)
class UpdateSimpleMessageParams(
    val title: SimpleSetField<String>? = null,
    val content: SimpleSetField<String?>? = null,
    val attachments: SimpleSetField<List<SimpleMessage.SimpleAttachment>>? = null,
)

@AppFunctionSerializable(isDescribedByKDoc = false)
class ContainsBothAttachments(
    val noteAttachment: SimpleNote.SimpleAttachment,
    val messageAttachment: SimpleMessage.SimpleAttachment,
)
