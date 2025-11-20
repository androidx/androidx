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

package androidx.credentials.playservices.controllers.identitycredentials.createdigitalcredential

import android.os.Bundle
import androidx.credentials.CreateCredentialRequest.Companion.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED
import androidx.credentials.CreateDigitalCredentialRequest
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.identitycredentials.CreateCredentialResponse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalDigitalCredentialApi::class)
class CreateDigitalCredentialControllerTest {

    @Test
    fun convertRequestToPlayServices() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val convertedRequest =
                CreateDigitalCredentialController(activity!!)
                    .convertRequestToPlayServices(DEFAULT_REQUEST)

            assertThat(convertedRequest.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
            val credentialData = convertedRequest.credentialData
            assertThat(credentialData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON"))
                .isNotNull()
            assertThat(credentialData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON"))
                .isEqualTo(DEFAULT_REQUEST_JSON)
            val candidateQueryData = convertedRequest.candidateQueryData
            assertThat(candidateQueryData.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)).isFalse()
            assertThat(convertedRequest.origin).isEqualTo(DEFAULT_ORIGIN)
            assertThat(convertedRequest.requestJson).isEqualTo(DEFAULT_REQUEST_JSON)
            assertThat(convertedRequest.resultReceiver).isNotNull()
        }
    }

    @Test
    fun convertResponseToCredentialManager() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val convertedResponse =
                CreateDigitalCredentialController(activity!!)
                    .convertResponseToCredentialManager(DEFAULT_RESPONSE)

            assertThat(convertedResponse.type).isEqualTo(DigitalCredential.TYPE_DIGITAL_CREDENTIAL)
        }
    }

    companion object {
        const val DEFAULT_REQUEST_JSON = "{testRequestKey: testResponseValue}"
        const val DEFAULT_ORIGIN = "origin"
        val DEFAULT_REQUEST = CreateDigitalCredentialRequest(DEFAULT_REQUEST_JSON, DEFAULT_ORIGIN)

        const val DEFAULT_RESPONSE_JSON = "{testResponseKey: testResponseValue}"
        val DEFAULT_RESPONSE_BUNDLE =
            Bundle().apply {
                putString("androidx.credentials.BUNDLE_KEY_RESPONSE_JSON", DEFAULT_RESPONSE_JSON)
            }

        val DEFAULT_RESPONSE =
            CreateCredentialResponse(
                DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                DEFAULT_RESPONSE_BUNDLE,
            )
    }
}
