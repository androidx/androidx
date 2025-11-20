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

package androidx.credentials.providerevents.playservices.controller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.credentials.providerevents.IntentHandler
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsNoExportOptionException
import androidx.credentials.providerevents.playservices.controller.ImportCredentialsController.Companion.maybeReportErrorResultCode
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportCredentialsControllerTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun generateErrorStringUnknown_returnsCorrectStringForGivenCode() {
        val resultCode = 123
        val expected = "activity with result code: 123 indicating not RESULT_OK"
        val actual =
            ImportCredentialsController.generateErrorStringUnknown(
                resultCode
            ) // Call the static method directly
        assertThat(actual).isEqualTo(expected)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun cancellationReviewer_nullSignal_returnsFalse() {
        val actual = ImportCredentialsController.cancellationReviewer(null)
        assertThat(actual).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun cancellationReviewer_nonCanceledSignal_returnsFalse() {
        // Mocked, but behaves as non-canceled by default or explicitly set
        val actual = ImportCredentialsController.cancellationReviewer(CancellationSignal())
        assertThat(actual).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun convertToPlayServicesRequest_transformsCorrectly() {
        val requestJson = "{ \"user\": \"test\" }"
        val testRequest = ImportCredentialsRequest(requestJson)

        // Create a real Uri for the test
        val tempFile = File.createTempFile("test_uri", ".json")
        val testUri: Uri = Uri.fromFile(tempFile)

        val actualPlayServicesRequest =
            ImportCredentialsController.convertToPlayServicesRequest(testRequest, testUri)

        assertThat(actualPlayServicesRequest.requestJson).isEqualTo(requestJson)
        assertThat(actualPlayServicesRequest.uri).isEqualTo(testUri)

        tempFile.delete() // Clean up the temporary file
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun maybeReportErrorResultCode_resultCodeOk() {
        val result =
            maybeReportErrorResultCode(
                Activity.RESULT_OK,
                { s, f -> f() },
                { e -> run { Assert.fail("No error should be thrown") } },
                CancellationSignal(),
                null,
            )
        assertThat(result).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun maybeReportErrorResultCode_resultCodeCancelled() {
        val result =
            maybeReportErrorResultCode(
                Activity.RESULT_CANCELED,
                { s, f -> f() },
                { e ->
                    run {
                        assertThat(e)
                            .isInstanceOf(ImportCredentialsCancellationException::class.java)
                    }
                },
                CancellationSignal(),
                null,
            )
        assertThat(result).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun maybeReportErrorResultCode_providerException() {
        val exception = ImportCredentialsNoExportOptionException("no export entry")
        val result =
            maybeReportErrorResultCode(
                Activity.RESULT_CANCELED,
                { s, f -> f() },
                { e ->
                    run {
                        assertThat(e)
                            .isInstanceOf(ImportCredentialsNoExportOptionException::class.java)
                        assertThat(e.errorMessage).isEqualTo(exception.errorMessage)
                    }
                },
                CancellationSignal(),
                Intent().apply { IntentHandler.setImportCredentialsException(this, exception) },
            )
        assertThat(result).isTrue()
    }
}
