package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable class SimpleSetField<T>(val value: T)

@AppFunctionSerializable
class SimpleNote(val title: String, val content: String, val attachment: SimpleAttachment) {
    @AppFunctionSerializable
    class SimpleAttachment(val displayName: String, val mimeType: String? = null)
}

@AppFunctionSerializable
class SimpleMessage(val title: String, val content: String, val attachment: SimpleAttachment) {
    @AppFunctionSerializable
    class SimpleAttachment(val displayName: String, val mimeType: String? = null)
}

@AppFunctionSerializable
class CreateSimpleNoteParams(
    val title: String,
    val content: String? = null,
    val attachments: List<SimpleNote.SimpleAttachment> = emptyList(),
)

@AppFunctionSerializable
class UpdateSimpleMessageParams(
    val title: SimpleSetField<String>? = null,
    val content: SimpleSetField<String?>? = null,
    val attachments: SimpleSetField<List<SimpleMessage.SimpleAttachment>>? = null,
)

@AppFunctionSerializable
class ContainsBothAttachments(
    val noteAttachment: SimpleNote.SimpleAttachment,
    val messageAttachment: SimpleMessage.SimpleAttachment,
)
