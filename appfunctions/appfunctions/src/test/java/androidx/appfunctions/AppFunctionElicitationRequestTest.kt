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
import android.os.Bundle
import android.os.Parcel
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionOneOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionElicitationRequestTest {

    @Test
    fun form_toBundleAndFromBundle_afterParceling() {
        for (metadata in SUPPORTED_FORM_DATA_TYPES) {
            val form =
                AppFunctionElicitationRequest.Form(
                    instruction = "Provide a value",
                    responseDataTypeMetadata = metadata,
                )

            val result = AppFunctionInputRequest.fromBundle(form.toBundle().parceled())

            assertThat(result).isEqualTo(form)
        }
    }

    @Test
    fun form_unsupportedResponseDataTypeMetadata_throws() {
        for (metadata in UNSUPPORTED_FORM_DATA_TYPES) {
            assertFailsWith<IllegalArgumentException> {
                AppFunctionElicitationRequest.Form(
                    instruction = "Provide a value",
                    responseDataTypeMetadata = metadata,
                )
            }
        }
    }

    @Test
    fun fromBundle_formWithUnsupportedResponseDataTypeMetadata_throws() {
        val unsupportedMetadata =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("title" to AppFunctionStringTypeMetadata(isNullable = false)),
                required = listOf("title"),
                qualifiedName = "com.example.Note",
                isNullable = false,
            )
        val bundle =
            Bundle().apply {
                putInt("requestType", AppFunctionInputRequest.REQUEST_TYPE_ELICITATION)
                putInt("elicitationType", 1)
                putString("instruction", "Provide a note")
                putBundle("responseDataTypeMetadata", unsupportedMetadata.toBundle())
            }

        assertFailsWith<IllegalArgumentException> { AppFunctionInputRequest.fromBundle(bundle) }
    }

    @Test
    fun pendingAction_toBundleAndFromBundle() {
        val pendingAction = createPendingAction()

        val result = AppFunctionInputRequest.fromBundle(pendingAction.toBundle())

        assertThat(result).isEqualTo(pendingAction)
    }

    @Test
    fun pendingAction_nonActivityPendingIntent_throws() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nonActivityPendingIntents =
            listOf(
                PendingIntent.getBroadcast(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                PendingIntent.getService(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            )

        for (pendingIntent in nonActivityPendingIntents) {
            assertFailsWith<IllegalArgumentException> {
                AppFunctionElicitationRequest.PendingAction(
                    displayMessage = "Confirm in the app",
                    pendingIntent = pendingIntent,
                )
            }
        }
    }

    @Test
    fun fromBundle_pendingActionWithNonActivityPendingIntent_throws() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val broadcastPendingIntent =
            PendingIntent.getBroadcast(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val bundle =
            Bundle().apply {
                putInt("requestType", AppFunctionInputRequest.REQUEST_TYPE_ELICITATION)
                putInt("elicitationType", 2)
                putCharSequence("displayMessage", "Confirm")
                putParcelable("pendingIntent", broadcastPendingIntent)
            }

        assertFailsWith<IllegalArgumentException> { AppFunctionInputRequest.fromBundle(bundle) }
    }

    @Test
    fun fromBundle_unknownRequestType_returnsNull() {
        val bundle = Bundle().apply { putInt("requestType", Int.MAX_VALUE) }

        assertThat(AppFunctionInputRequest.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_unknownElicitationType_returnsNull() {
        val bundle =
            Bundle().apply {
                putInt("requestType", AppFunctionInputRequest.REQUEST_TYPE_ELICITATION)
                putInt("elicitationType", Int.MAX_VALUE)
            }

        assertThat(AppFunctionInputRequest.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_malformedForm_throws() {
        val bundle =
            Bundle().apply {
                putInt("requestType", AppFunctionInputRequest.REQUEST_TYPE_ELICITATION)
                putInt("elicitationType", 1)
            }

        assertFailsWith<IllegalArgumentException> { AppFunctionInputRequest.fromBundle(bundle) }
    }

    @Test
    fun fromBundle_malformedPendingAction_throws() {
        val bundle =
            Bundle().apply {
                putInt("requestType", AppFunctionInputRequest.REQUEST_TYPE_ELICITATION)
                putInt("elicitationType", 2)
                putCharSequence("displayMessage", "Confirm")
            }

        assertFailsWith<IllegalArgumentException> { AppFunctionInputRequest.fromBundle(bundle) }
    }

    private fun createPendingAction(): AppFunctionElicitationRequest.PendingAction {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pendingIntent =
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        return AppFunctionElicitationRequest.PendingAction(
            displayMessage = "Confirm in the app",
            pendingIntent = pendingIntent,
        )
    }

    private fun Bundle.parceled(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        val SUPPORTED_FORM_DATA_TYPES =
            listOf(
                AppFunctionBooleanTypeMetadata(isNullable = false, description = "Confirm"),
                AppFunctionIntTypeMetadata(isNullable = false, enumValues = setOf(1, 2, 3)),
                AppFunctionLongTypeMetadata(isNullable = true),
                AppFunctionFloatTypeMetadata(isNullable = false),
                AppFunctionDoubleTypeMetadata(isNullable = true),
                AppFunctionStringTypeMetadata(
                    isNullable = false,
                    enumValues = setOf("Groceries", "Tax Return"),
                ),
            )

        val UNSUPPORTED_FORM_DATA_TYPES =
            listOf(
                AppFunctionUnitTypeMetadata(isNullable = false),
                AppFunctionBytesTypeMetadata(isNullable = false),
                AppFunctionParcelableTypeMetadata(
                    qualifiedName = "android.net.Uri",
                    isNullable = false,
                ),
                AppFunctionArrayTypeMetadata(
                    itemType = AppFunctionStringTypeMetadata(isNullable = false),
                    isNullable = false,
                ),
                AppFunctionObjectTypeMetadata(
                    properties =
                        mapOf("title" to AppFunctionStringTypeMetadata(isNullable = false)),
                    required = listOf("title"),
                    qualifiedName = "com.example.NoteSelection",
                    isNullable = false,
                ),
                AppFunctionReferenceTypeMetadata(
                    referenceDataType = "com.example.NoteSelection",
                    isNullable = false,
                ),
                AppFunctionAllOfTypeMetadata(
                    matchAll =
                        listOf(
                            AppFunctionObjectTypeMetadata(
                                properties = emptyMap(),
                                required = emptyList(),
                                qualifiedName = "com.example.Base",
                                isNullable = false,
                            )
                        ),
                    qualifiedName = "com.example.Derived",
                    isNullable = false,
                ),
                AppFunctionOneOfTypeMetadata(
                    matchOneOf =
                        listOf(
                            AppFunctionObjectTypeMetadata(
                                properties = emptyMap(),
                                required = emptyList(),
                                qualifiedName = "com.example.OptionA",
                                isNullable = false,
                            )
                        ),
                    qualifiedName = "com.example.Choice",
                    isNullable = false,
                ),
            )
    }
}
