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

import android.app.Activity
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestUtils.Companion.isSubsetJson
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.ALL_REQUIRED_AND_OPTIONAL_SIGNATURE
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.ALL_REQUIRED_FIELDS_SIGNATURE
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.MAIN_CREATE_JSON_MISSING_REQUIRED_FIELD
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD
import androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.Companion.createJsonObjectFromPublicKeyCredentialCreationOptions
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
class CredentialProviderCreatePublicKeyCredentialControllerTest(val useFragmentActivity: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(true, false)
    }

    @DoNotInline
    private fun launchTestActivity(callback: (activity: Activity) -> Unit) {
        if (useFragmentActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            var activityScenario =
                            ActivityScenario.launch(
                                    androidx.credentials.playservices
                                            .TestCredentialsFragmentActivity::class.java)
            activityScenario.onActivity { activity: Activity ->
                callback.invoke(activity)
            }
        } else {
            var activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
            activityScenario.onActivity { activity: Activity ->
                callback.invoke(activity)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_correctRequiredOnlyRequest_success() {
        launchTestActivity { activity: Activity ->
            try {
                val expectedJson = JSONObject(MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT)

                val actualResponse = getInstance(activity).convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT))
                val actualJson =
                    createJsonObjectFromPublicKeyCredentialCreationOptions(actualResponse)
                val requiredKeys =
                    JSONObject(ALL_REQUIRED_FIELDS_SIGNATURE)

                assertThat(isSubsetJson(expectedJson, actualJson, requiredKeys)).isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_correctRequiredAndOptionalRequest_success() {
        launchTestActivity { activity: Activity ->
            try {
                val expectedJson = JSONObject(
                    MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT)

                val actualResponse = getInstance(activity)
                        .convertRequestToPlayServices(CreatePublicKeyCredentialRequest(
                            MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT))
                val actualJson =
                    createJsonObjectFromPublicKeyCredentialCreationOptions(actualResponse)
                val requiredKeys =
                    JSONObject(ALL_REQUIRED_AND_OPTIONAL_SIGNATURE)

                assertThat(isSubsetJson(expectedJson, actualJson, requiredKeys)).isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_missingRequired_throws() {
        launchTestActivity { activity: Activity ->

            Assert.assertThrows("Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    getInstance(
                        activity
                    ).convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                MAIN_CREATE_JSON_MISSING_REQUIRED_FIELD
                            )) })
        }
    }

    @Test
    fun convertRequestToPlayServices_emptyRequired_throws() {
        launchTestActivity { activity: Activity ->

            Assert.assertThrows("Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable { getInstance(activity
                    ).convertRequestToPlayServices(CreatePublicKeyCredentialRequest(
                                MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY)) })
        }
    }
    @Test
    fun convertRequestToPlayServices_missingOptionalRequired_throws() {
        launchTestActivity { activity: Activity ->

            Assert.assertThrows("Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    getInstance(
                        activity
                    ).convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD)) })
        }
    }

    @Test
    fun convertRequestToPlayServices_emptyOptionalRequired_throws() {
        launchTestActivity { activity: Activity ->

            Assert.assertThrows("Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable { getInstance(activity).convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD)) })
        }
    }

    @Test
    fun convertRequestToPlayServices_missingOptionalNotRequired_success() {
        launchTestActivity { activity: Activity ->
            try {
                val expectedJson = JSONObject(OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD)

                val actualResponse =
                    getInstance(activity)
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD))
                val actualJson = createJsonObjectFromPublicKeyCredentialCreationOptions(
                        actualResponse)
                val requiredKeys =
                    JSONObject(OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE)

                assertThat(isSubsetJson(expectedJson, actualJson, requiredKeys)).isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
        }
    }
}