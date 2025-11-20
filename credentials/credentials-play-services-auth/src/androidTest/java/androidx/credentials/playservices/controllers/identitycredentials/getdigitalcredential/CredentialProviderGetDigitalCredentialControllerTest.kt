/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.identitycredentials.getdigitalcredential

import android.content.ComponentName
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalDigitalCredentialApi::class)
class CredentialProviderGetDigitalCredentialControllerTest {
    @Test
    fun convertRequestToPlayServices_success() {
        val request =
            GetCredentialRequest(
                credentialOptions =
                    listOf(
                        GetDigitalCredentialOption("{\"request\":{\"json\":{\"test\":\"val\"}}}"),
                        GetDigitalCredentialOption("{\"request\":\"val\",\"key2\":\"val2\"}"),
                    ),
                origin = "origin",
                preferIdentityDocUi = true,
                preferUiBrandingComponentName = ComponentName("pkg", "cls"),
                preferImmediatelyAvailableCredentials = true,
            )
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val convertedRequest =
                CredentialProviderGetDigitalCredentialController(activity!!)
                    .convertRequestToPlayServices(request)

            assertThat(convertedRequest.origin).isEqualTo(request.origin)
            TestUtils.equals(
                convertedRequest.data,
                GetCredentialRequest.getRequestMetadataBundle(request),
            )
            request.credentialOptions.forEachIndexed { idx, expectedOption ->
                val actualOption = convertedRequest.credentialOptions[idx]
                assertThat(actualOption.type).isEqualTo(expectedOption.type)
                if (expectedOption is GetDigitalCredentialOption) {
                    assertThat(actualOption.requestMatcher).isEqualTo(expectedOption.requestJson)
                }
                TestUtils.equals(actualOption.credentialRetrievalData, expectedOption.requestData)
                TestUtils.equals(actualOption.candidateQueryData, expectedOption.candidateQueryData)
            }
        }
    }
}
