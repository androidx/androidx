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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.internal.AppFunctionSerializableFactory

class MissingFactoryClass(val item: String)

data class Attachment(val uri: String)

data class Note(val title: String, val attachment: Attachment)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class `$AttachmentFactory` : AppFunctionSerializableFactory<Attachment> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): Attachment {
        return Attachment(checkNotNull(appFunctionData.getString("uri")))
    }

    override fun toAppFunctionData(appFunctionSerializable: Attachment): AppFunctionData {
        return AppFunctionData.Builder("androidx.appfunctions.Attachment")
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
        return AppFunctionData.Builder("androidx.appfunctions.Note")
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
