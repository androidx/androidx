/*
 * Copyright 2026 The Android Open Source Project
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
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AppFunctionDataSerializeTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testSerialize() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val data =
            AppFunctionData.serialize(
                Note.NOTE_OBJECT_TYPE_METADATA,
                AppFunctionComponentsMetadata(),
                note,
                Note::class.java,
            )

        assertThat(data.getString("title")).isEqualTo("Test Title")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("Test Uri")
    }

    @Test
    fun testInlineSerialize() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val data =
            AppFunctionData.serialize(
                Note.NOTE_OBJECT_TYPE_METADATA,
                AppFunctionComponentsMetadata(),
                note,
            )

        assertThat(data.getString("title")).isEqualTo("Test Title")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("Test Uri")
    }

    @Test
    fun testSerialize_missingFactory() {
        val missingFactoryClass = MissingFactoryClass("test")

        assertFailsWith(IllegalArgumentException::class) {
            AppFunctionData.serialize(missingFactoryClass, MissingFactoryClass::class.java)
        }
    }

    @Test
    fun testSerialize_missingRequiredField() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val invalidMetadata =
            AppFunctionObjectTypeMetadata(
                properties =
                    Note.NOTE_OBJECT_TYPE_METADATA.properties +
                        mapOf("extraRequired" to AppFunctionStringTypeMetadata(isNullable = false)),
                required = Note.NOTE_OBJECT_TYPE_METADATA.required + "extraRequired",
                qualifiedName = Note.NOTE_OBJECT_TYPE_METADATA.qualifiedName,
                isNullable = Note.NOTE_OBJECT_TYPE_METADATA.isNullable,
            )

        assertFailsWith(IllegalArgumentException::class) {
            AppFunctionData.serialize(
                invalidMetadata,
                AppFunctionComponentsMetadata(),
                note,
                Note::class.java,
            )
        }
    }

    @Test
    fun testSerialize_invalidType() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val invalidMetadata =
            AppFunctionObjectTypeMetadata(
                properties =
                    Note.NOTE_OBJECT_TYPE_METADATA.properties.toMutableMap().apply {
                        this["title"] = AppFunctionIntTypeMetadata(isNullable = false)
                    },
                required = Note.NOTE_OBJECT_TYPE_METADATA.required,
                qualifiedName = Note.NOTE_OBJECT_TYPE_METADATA.qualifiedName,
                isNullable = Note.NOTE_OBJECT_TYPE_METADATA.isNullable,
            )

        assertFailsWith(IllegalArgumentException::class) {
            AppFunctionData.serialize(
                invalidMetadata,
                AppFunctionComponentsMetadata(),
                note,
                Note::class.java,
            )
        }
    }

    @Test
    fun serializeAllOfTypeObject_allRequiredField_success() {
        val data =
            AppFunctionData.serialize(
                OpenableNote.OPENABLE_NOTE_ALL_OF_TYPE_METADATA,
                OpenableNote.COMPONENT_METADATA,
                OpenableNote(
                    title = "test",
                    attachment = Attachment(uri = "test"),
                    intentToOpen =
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(),
                            PendingIntent.FLAG_IMMUTABLE,
                        ),
                ),
                OpenableNote::class.java,
            )

        assertThat(data.getString("title")).isEqualTo("test")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("test")
        assertThat(data.getParcelable<PendingIntent>("intentToOpen")).isNotNull()
        // Also ensure that read validation is applied
        assertFailsWith<IllegalArgumentException> { data.getInt("intentToOpen") }
    }

    @Test
    fun serializeAllOfTypeObject_missingRequiredField_failure() {
        val invalidAllOfMetadata =
            AppFunctionAllOfTypeMetadata(
                qualifiedName = checkNotNull(OpenableNote::class.java.canonicalName),
                isNullable = true,
                matchAll =
                    OpenableNote.OPENABLE_NOTE_ALL_OF_TYPE_METADATA.matchAll +
                        AppFunctionObjectTypeMetadata(
                            properties =
                                mapOf(
                                    "extraRequired" to
                                        AppFunctionStringTypeMetadata(isNullable = false)
                                ),
                            required = listOf("extraRequired"),
                            qualifiedName = "com.example.Extra",
                            isNullable = false,
                        ),
            )

        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.serialize(
                invalidAllOfMetadata,
                OpenableNote.COMPONENT_METADATA,
                OpenableNote(
                    title = "test",
                    attachment = Attachment(uri = "test"),
                    intentToOpen =
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(),
                            PendingIntent.FLAG_IMMUTABLE,
                        ),
                ),
                OpenableNote::class.java,
            )
        }
    }
}
