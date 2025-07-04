package com.testdata

import android.app.PendingIntent
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.AppFunctionSerializableInterface
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSchemaCapability
import java.time.LocalDateTime

@AppFunctionSchemaCapability
interface AppFunctionOpenable {
    val pendingIntent: PendingIntent
}

@AppFunctionSerializable
data class SetField<out T>(val value: T)

@AppFunctionSerializableInterface
interface Attachment {
    val uri: String
}

@AppFunctionSerializableInterface
interface Note {
    val id: String
    val title: String
    val content: List<String>?
    val attachments: List<Attachment>
    val createTime: LocalDateTime?
}

@AppFunctionSchemaDefinition(category = "note", name = "updateNote", version = 2)
interface UpdateNoteFunction<Params: UpdateNoteFunction.Parameters, Res: UpdateNoteFunction.Response> {

    suspend fun updateNote(
        appFunctionContext: AppFunctionContext,
        parameters: Params
    ): Res

    @AppFunctionSerializableInterface
    interface Parameters {
        val noteId: String
        val title: SetField<String>?
        val content: SetField<String?>?
        val attachments: SetField<List<Attachment>>?
    }

    @AppFunctionSerializableInterface
    interface Response {
        val note: Note
    }
}

@AppFunctionSchemaDefinition(category = "note", name = "showNote", version = 2)
interface ShowNoteFunction<Params: ShowNoteFunction.Parameters, Res: ShowNoteFunction.Response> {

    suspend fun showNote(
        appFunctionContext: AppFunctionContext,
        parameters: Params
    ): Res

    @AppFunctionSerializableInterface
    interface Parameters {
        val noteId: String
    }

    @AppFunctionSerializableInterface
    interface Response: AppFunctionOpenable
}
