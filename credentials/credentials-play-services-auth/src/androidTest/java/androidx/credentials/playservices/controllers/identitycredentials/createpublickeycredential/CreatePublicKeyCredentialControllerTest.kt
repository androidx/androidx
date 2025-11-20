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

package androidx.credentials.playservices.controllers.identitycredentials.createpublickeycredential

import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestUtils
import androidx.credentials.playservices.controllers.utils.CreatePublicKeyCredentialControllerTestUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalDigitalCredentialApi::class)
class CreatePublicKeyCredentialControllerTest {

    @Test
    fun fromGmsException_unsupportedApiException_returnsUnsupportedException() {
        val gmsException = ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED))
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(gmsException)

            Truth.assertThat(result is CreateCredentialUnsupportedException)
        }
    }

    @Test
    fun fromGmsException_apiExceptionCancelled_returnsCancellationException() {
        val msg = "Operation cancelled"
        val apiException = ApiException(Status(CommonStatusCodes.CANCELED, msg))
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(apiException)

            Truth.assertThat(result is CreateCredentialCancellationException).isTrue()
            Truth.assertThat(result.message).contains(msg)
        }
    }

    @Test
    fun fromGmsException_apiExceptionRetryable_returnsInterruptedException() {
        val msg = "Network error"
        val apiException = ApiException(Status(CommonStatusCodes.NETWORK_ERROR, msg))
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(apiException)

            Truth.assertThat(result is CreateCredentialInterruptedException).isTrue()
            Truth.assertThat(result.message).contains(msg)
        }
    }

    @Test
    fun fromGmsException_apiExceptionOther_returnsUnknownException() {
        val msg = "Some other error"
        val apiException = ApiException(Status(CommonStatusCodes.ERROR, msg))
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(apiException)

            Truth.assertThat(result is CreateCredentialUnknownException).isTrue()
            Truth.assertThat(result.message).contains(msg)
        }
    }

    @Test
    fun fromGmsException_apiExceptionInternal_returnsUnknownException() {
        val msg = "Some internal error"
        val apiException = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR, msg))
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(apiException)

            Truth.assertThat(result is CreateCredentialNoCreateOptionException).isTrue()
            Truth.assertThat(result.message).contains(msg)
        }
    }

    @Test
    fun fromGmsException_unsupportedApiCallException_returnsUnsupportedException() {
        val unsupportedException = UnsupportedApiCallException(Feature("sample", 12))
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(unsupportedException)

            Truth.assertThat(result is CreateCredentialUnsupportedException).isTrue()
        }
    }

    @Test
    fun fromGmsException_otherException_returnsUnknownException() {
        val exception = Exception("Some other exception")
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePublicKeyCredentialController(activity!!)
            val result = controller.fromGmsException(exception)

            Truth.assertThat(result is CreateCredentialUnknownException).isTrue()
        }
    }

    @Test
    fun convertRequestToPlayServices_success() {
        val request =
            CreatePublicKeyCredentialRequest(
                requestJson =
                    CreatePublicKeyCredentialControllerTestUtils
                        .MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT,
                isConditional = true,
            )
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val convertedRequest =
                CreatePublicKeyCredentialController(activity!!)
                    .convertRequestToPlayServices(request)

            Truth.assertThat(convertedRequest.origin).isEqualTo(request.origin)
            Truth.assertThat(convertedRequest.requestJson).isEqualTo(request.requestJson)
            Truth.assertThat(convertedRequest.type).isEqualTo(request.type)
            TestUtils.Companion.equals(
                convertedRequest.candidateQueryData,
                request.candidateQueryData,
            )
            TestUtils.Companion.equals(convertedRequest.credentialData, request.credentialData)
            val jetpackRequestFromConvertedRequest =
                CreateCredentialRequest.createFrom(
                    convertedRequest.type,
                    convertedRequest.candidateQueryData,
                    convertedRequest.credentialData,
                    false,
                )
            Truth.assertThat(jetpackRequestFromConvertedRequest is CreatePublicKeyCredentialRequest)
            val createPublicKeyReq =
                jetpackRequestFromConvertedRequest as CreatePublicKeyCredentialRequest
            Truth.assertThat(createPublicKeyReq.isConditional)
        }
    }
}
