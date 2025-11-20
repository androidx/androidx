/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appfunctions

import android.app.PendingIntent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.Attachment.Companion.ATTACHMENT_OBJECT_TYPE_METADATA
import androidx.appfunctions.internal.AppFunctionSerializableFactory
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPendingIntentTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata

class MissingFactoryClass(val item: String)

data class Attachment(val uri: String) {
    internal companion object {
        val ATTACHMENT_OBJECT_TYPE_METADATA: AppFunctionObjectTypeMetadata =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("uri" to AppFunctionStringTypeMetadata(isNullable = false)),
                required = listOf("uri"),
                qualifiedName = "androidx.appfunctions.Attachment",
                isNullable = true,
            )
    }
}

data class Note(val title: String, val attachment: Attachment) {
    internal companion object {

        val NOTE_OBJECT_TYPE_METADATA: AppFunctionObjectTypeMetadata =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "title" to AppFunctionStringTypeMetadata(isNullable = false),
                        "attachment" to ATTACHMENT_OBJECT_TYPE_METADATA,
                    ),
                required = listOf("title", "attachment"),
                qualifiedName = "androidx.appfunctions.Note",
                isNullable = true,
            )
    }
}

data class OpenableNote(
    val title: String,
    val attachment: Attachment,
    val intentToOpen: PendingIntent,
) {
    companion object {
        val OPENABLE_NOTE_ALL_OF_TYPE_METADATA: AppFunctionAllOfTypeMetadata =
            AppFunctionAllOfTypeMetadata(
                qualifiedName = checkNotNull(OpenableNote::class.java.canonicalName),
                isNullable = true,
                matchAll =
                    listOf(
                        Note.NOTE_OBJECT_TYPE_METADATA,
                        AppFunctionReferenceTypeMetadata(
                            referenceDataType = "com.example.AppFunctionOpenable",
                            isNullable = false,
                        ),
                    ),
            )

        val COMPONENT_METADATA: AppFunctionComponentsMetadata =
            AppFunctionComponentsMetadata(
                mapOf(
                    "com.example.AppFunctionOpenable" to
                        AppFunctionObjectTypeMetadata(
                            properties =
                                mapOf(
                                    "intentToOpen" to
                                        AppFunctionPendingIntentTypeMetadata(isNullable = false)
                                ),
                            required = listOf("intentToOpen"),
                            qualifiedName = "com.example.AppFunctionOpenable",
                            isNullable = true,
                        )
                )
            )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class `$AttachmentFactory` : AppFunctionSerializableFactory<Attachment> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): Attachment {
        return Attachment(checkNotNull(appFunctionData.getString("uri")))
    }

    override fun toAppFunctionData(appFunctionSerializable: Attachment): AppFunctionData {
        return getAppFunctionDataBuilder("androidx.appfunctions.Attachment")
            .setString("uri", appFunctionSerializable.uri)
            .build()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class `$NoteFactory` : AppFunctionSerializableFactory<Note> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): Note {
        return Note(
            title = checkNotNull(appFunctionData.getString("title")),
            attachment =
                checkNotNull(appFunctionData.getAppFunctionData("attachment"))
                    .deserialize(Attachment::class.java),
        )
    }

    override fun toAppFunctionData(appFunctionSerializable: Note): AppFunctionData {
        return getAppFunctionDataBuilder("androidx.appfunctions.Note")
            .setString("title", appFunctionSerializable.title)
            .setAppFunctionData(
                "attachment",
                AppFunctionData.serialize(
                    appFunctionSerializable.attachment,
                    Attachment::class.java,
                ),
            )
            .build()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class `$OpenableNoteFactory` : AppFunctionSerializableFactory<OpenableNote> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): OpenableNote {
        return OpenableNote(
            title = checkNotNull(appFunctionData.getString("title")),
            attachment =
                checkNotNull(appFunctionData.getAppFunctionData("attachment"))
                    .deserialize(Attachment::class.java),
            intentToOpen = checkNotNull(appFunctionData.getPendingIntent("intentToOpen")),
        )
    }

    override fun toAppFunctionData(appFunctionSerializable: OpenableNote): AppFunctionData {
        return getAppFunctionDataBuilder("androidx.appfunctions.OpenableNote")
            .setString("title", appFunctionSerializable.title)
            .setAppFunctionData(
                "attachment",
                AppFunctionData.serialize(
                    appFunctionSerializable.attachment,
                    Attachment::class.java,
                ),
            )
            .setPendingIntent("intentToOpen", appFunctionSerializable.intentToOpen)
            .build()
    }
}
