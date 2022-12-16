/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.playservices.createpublickeycredential

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestUtils.Companion.createJsonObjectFromPublicKeyCredentialCreationOptions
import androidx.credentials.playservices.TestUtils.Companion.isSubsetJson
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerUtils.Companion.CREATE_REQUEST_INPUT_REQUIRED_AND_OPTIONAL
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerUtils.Companion.CREATE_REQUEST_INPUT_REQUIRED_ONLY
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerUtils.Companion.CREATE_REQUEST_MISSING_REQUIRED_NONEXISTENT
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerUtils.Companion.CREATE_REQUEST_REQUIRED_EMPTY
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialProviderCreatePublicKeyCredentialControllerTest {
    @Test
    fun convertRequestToPlayServices_correctRequiredOnlyRequest_success() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            try {
                val expectedJson =
                    JSONObject(CREATE_REQUEST_INPUT_REQUIRED_ONLY)

                val actualResponse = getInstance(activity!!).convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(CREATE_REQUEST_INPUT_REQUIRED_ONLY))
                val actualJson =
                    createJsonObjectFromPublicKeyCredentialCreationOptions(actualResponse)

                assertThat(
                    isSubsetJson(expectedJson, actualJson)
                ).isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_correctRequiredAndOptionalRequest_success() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            try {
                val expectedJson = JSONObject(
                    CREATE_REQUEST_INPUT_REQUIRED_AND_OPTIONAL
                )

                val actualResponse =
                    getInstance(
                        activity!!
                    )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CREATE_REQUEST_INPUT_REQUIRED_ONLY
                            )
                        )
                val actualJson =
                    createJsonObjectFromPublicKeyCredentialCreationOptions(
                        actualResponse
                    )

                assertThat(
                    isSubsetJson(
                        expectedJson,
                        actualJson
                    )
                ).isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_missingRequired_throws() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->

            Assert.assertThrows("Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    getInstance(
                        activity!!
                    )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CREATE_REQUEST_MISSING_REQUIRED_NONEXISTENT
                            )
                        )
                })
        }
    }

    @Test
    fun convertRequestToPlayServices_emptyRequired_throws() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->

            Assert.assertThrows("Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    getInstance(
                        activity!!
                    )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CREATE_REQUEST_REQUIRED_EMPTY
                            )
                        )
                })
        }
    }
}