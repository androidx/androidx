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

package androidx.credentials.providerevents

import android.content.Intent
import android.content.pm.SigningInfo
import android.net.Uri
import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException
import androidx.credentials.providerevents.exception.ImportCredentialsSystemErrorException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownCallerException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
@SmallTest
class IntentHandlerTest {

    @Test
    fun retrieveProviderImportCredentialsRequest_success() {
        // 1. Setup
        val intent = Intent()
        val signingInfo = SigningInfo()

        intent.putExtra(EXTRA_REQUEST_JSON, TEST_REQUEST_JSON)
        intent.putExtra(EXTRA_PACKAGE_NAME, TEST_PACKAGE_NAME)
        intent.putExtra(EXTRA_SIGNING_INFO, signingInfo)
        intent.putExtra(EXTRA_CRED_ID, "testCredId")
        intent.setData(Uri.EMPTY)

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.request.requestJson).isEqualTo(TEST_REQUEST_JSON)
        Truth.assertThat(result.callingAppInfo.packageName).isEqualTo(TEST_PACKAGE_NAME)
        Truth.assertThat(result.callingAppInfo.signingInfo).isEqualTo(signingInfo)
    }

    @Test
    fun retrieveProviderImportCredentialsRequest_nullExtras_returnsNull() {
        // 1. Setup
        val intent = Intent()
        // Note: We are explicitly not adding any extras to the intent.

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        Truth.assertThat(result).isNull()
    }

    @Test
    fun retrieveProviderImportCredentialsRequest_missingRequest_returnsNull() {
        // 1. Setup
        val intent = Intent()
        // We add other extras but not the required ImportCredentialsRequest
        intent.putExtra(EXTRA_PACKAGE_NAME, TEST_PACKAGE_NAME)
        intent.putExtra(EXTRA_SIGNING_INFO, SigningInfo())
        intent.putExtra(EXTRA_CRED_ID, "testCredId")

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        Truth.assertThat(result).isNull()
    }

    @Test
    fun retrieveProviderImportCredentialsRequest_missingPackageName_returnsNull() {
        // 1. Setup
        val intent = Intent()

        intent.putExtra(EXTRA_REQUEST_JSON, TEST_REQUEST_JSON)
        intent.putExtra(EXTRA_SIGNING_INFO, SigningInfo())
        intent.putExtra(EXTRA_CRED_ID, "testCredId")

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        Truth.assertThat(result).isNull()
    }

    @Test
    fun retrieveProviderImportCredentialsRequest_missingSigningInfo_returnsNull() {
        // 1. Setup
        val intent = Intent()

        intent.putExtra(EXTRA_REQUEST_JSON, TEST_REQUEST_JSON)
        intent.putExtra(EXTRA_PACKAGE_NAME, TEST_PACKAGE_NAME)
        intent.putExtra(EXTRA_CRED_ID, "testCredId")
        // Note: We are explicitly not adding the SigningInfo extra

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        // The call to getParcelable for SigningInfo will return null, causing the method to fail
        // and likely throw a NullPointerException when passed to CallingAppInfo.create.
        // A robust test checks for the expected null return due to this failure path.
        // The test passes if a null is returned as the internal check for signingInfo should handle
        // it.
        Truth.assertThat(result).isNull()
    }

    @Test
    fun retrieveProviderImportCredentialsRequest_missingCredId_returnsNull() {
        // 1. Setup
        val intent = Intent()
        val signingInfo = SigningInfo()

        intent.putExtra(EXTRA_REQUEST_JSON, TEST_REQUEST_JSON)
        intent.putExtra(EXTRA_PACKAGE_NAME, TEST_PACKAGE_NAME)
        intent.putExtra(EXTRA_SIGNING_INFO, signingInfo)
        intent.setData(Uri.EMPTY)

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        Truth.assertThat(result).isNull()
    }

    @Test
    fun setAndRetrieveImportCredentialsException_handlesAllExceptionTypes() {
        // A list of all specific exception types to be tested.
        val exceptionTypes =
            listOf(
                ImportCredentialsInvalidJsonException("Invalid JSON format"),
                ImportCredentialsProviderConfigurationException("Provider not configured"),
                ImportCredentialsSystemErrorException("A system error occurred"),
                ImportCredentialsUnknownCallerException("Caller is not recognized"),
                ImportCredentialsUnknownErrorException("An unknown error occurred"),
            )

        // Iterate through each exception type to test the set/retrieve cycle.
        exceptionTypes.forEach { originalException ->
            // 1. Setup: Create a new Intent for each exception type.
            val intent = Intent()

            // 2. Execution: Call the setter to add the current exception to the Intent.
            IntentHandler.setImportCredentialsException(intent, originalException)

            // 3. Execution: Call the retriever to get the exception back from the Intent.
            val retrievedException = IntentHandler.retrieveImportCredentialsException(intent)

            // 4. Verification
            Truth.assertThat(retrievedException).isNotNull()
            // Check that the retrieved object is of the exact same class as the original.
            Truth.assertThat(retrievedException).isInstanceOf(originalException::class.java)
            // Verify that the exception's type property matches.
            Truth.assertThat(retrievedException!!.type).isEqualTo(originalException.type)
            // Verify that the error message is correctly preserved.
            Truth.assertThat(retrievedException.errorMessage)
                .isEqualTo(originalException.errorMessage)
        }
    }

    @Test
    fun retrieveImportCredentialsException_nullWhenNoExceptionInIntent() {
        val intent = Intent()

        val retrievedException = IntentHandler.retrieveImportCredentialsException(intent)

        Truth.assertThat(retrievedException).isNull()
    }

    private companion object {
        private const val EXTRA_REQUEST_JSON =
            "androidx.credentials.providerevents.extra.IMPORT_CREDENTIALS_REQUEST_JSON"
        private const val EXTRA_PACKAGE_NAME =
            "androidx.credentials.providerevents.extra.CALLING_PACKAGE_NAME"
        private const val EXTRA_SIGNING_INFO =
            "androidx.credentials.providerevents.extra.SIGNING_INFO"
        private const val EXTRA_CRED_ID = "androidx.credentials.providerevents.extra.CREDENTIAL_ID"
        private val TEST_REQUEST_JSON =
            "{\"credentialTypes\":[\"basic-auth\"],\"knownExtensions\":[\"shared\"]}"
        private val TEST_PACKAGE_NAME = "com.example.test.app"
    }
}
