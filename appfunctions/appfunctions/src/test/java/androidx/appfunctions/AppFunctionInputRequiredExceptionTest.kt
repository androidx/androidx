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
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionInputRequiredExceptionTest {

    @Test
    fun errorCodeAndCategory() {
        val exception = AppFunctionInputRequiredException(mapOf(KEY_FORM to FORM_REQUEST))

        assertThat(exception.internalErrorCode).isEqualTo(AppFunctionException.ERROR_INPUT_REQUIRED)
        assertThat(exception.errorCategory).isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)
    }

    @Test
    fun create_withEmptyRequests_throws() {
        assertFailsWith<IllegalArgumentException> { AppFunctionInputRequiredException(emptyMap()) }
    }

    @Test
    fun inputRequests_returnsRequestsFromConstructor() {
        val requests = mapOf(KEY_FORM to FORM_REQUEST, KEY_PENDING_ACTION to createPendingAction())

        val exception = AppFunctionInputRequiredException(requests, "testMessage")

        assertThat(exception.inputRequests).isEqualTo(requests)
        assertThat(exception.errorMessage).isEqualTo("testMessage")
    }

    @Test
    fun recreateFromExtras_preservesInputRequests() {
        val exception =
            AppFunctionInputRequiredException(mapOf(KEY_FORM to FORM_REQUEST), "testMessage")

        val recreated =
            AppFunctionInputRequiredException(exception.errorMessage, exception.extras.parceled())

        assertThat(recreated.errorMessage).isEqualTo("testMessage")
        assertThat(recreated.inputRequests).containsExactly(KEY_FORM, FORM_REQUEST)
    }

    @Test
    fun recreateFromExtras_withoutRequests_returnsEmptyRequests() {
        val exception = AppFunctionInputRequiredException("testMessage", Bundle())

        assertThat(exception.inputRequests).isEmpty()
    }

    @Test
    fun recreateFromExtras_skipsUnknownAndMalformedRequests() {
        val extras = AppFunctionInputRequiredException(mapOf(KEY_FORM to FORM_REQUEST)).extras
        val requests = extras.getBundle(EXTRA_INPUT_REQUESTS)!!
        requests.putBundle(
            "unknown",
            Bundle().apply { putInt("requestType", Int.MAX_VALUE) },
        )
        requests.putBundle(
            "malformed",
            Bundle().apply {
                putInt("requestType", AppFunctionInputRequest.REQUEST_TYPE_ELICITATION)
                putInt("elicitationType", 1)
            },
        )

        val recreated = AppFunctionInputRequiredException(null, extras)

        assertThat(recreated.inputRequests).containsExactly(KEY_FORM, FORM_REQUEST)
    }

    @Test
    fun recreateFromExtras_skipsNullKey() {
        val extras = AppFunctionInputRequiredException(mapOf(KEY_FORM to FORM_REQUEST)).extras
        val requests = extras.getBundle(EXTRA_INPUT_REQUESTS)!!
        requests.putBundle(null, FORM_REQUEST.toBundle())

        val recreated = AppFunctionInputRequiredException(null, extras)

        assertThat(recreated.inputRequests).containsExactly(KEY_FORM, FORM_REQUEST)
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
        const val KEY_FORM = "form"
        const val KEY_PENDING_ACTION = "pendingAction"
        const val EXTRA_INPUT_REQUESTS = "androidx.appfunctions.extra.INPUT_REQUESTS"

        val FORM_REQUEST =
            AppFunctionElicitationRequest.Form(
                instruction = "Which note would you like to delete?",
                responseDataTypeMetadata =
                    AppFunctionStringTypeMetadata(
                        isNullable = false,
                        enumValues = setOf("Groceries", "Tax Return"),
                    ),
            )
    }
}
