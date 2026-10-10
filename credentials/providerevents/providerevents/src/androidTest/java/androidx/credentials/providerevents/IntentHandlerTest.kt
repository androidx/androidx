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
import android.os.Bundle
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.exception.ImportCredentialsNoExportOptionException
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException
import androidx.credentials.providerevents.exception.ImportCredentialsSystemErrorException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownCallerException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.credentials.providerevents.service.BatchCreateCredentialResponse
import androidx.credentials.providerevents.service.CreateCredentialResult
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
                ImportCredentialsCancellationException("User cancelled the import"),
                ImportCredentialsInvalidJsonException("Invalid JSON format"),
                ImportCredentialsNoExportOptionException("No export option available"),
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

    @Test
    fun retrieveProviderBatchCreateCredentialRequest_success() {
        val intent =
            Intent()
                .putParcelableArrayListExtra(
                    EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS,
                    arrayListOf(
                        ProviderCreateCredentialRequest.asBundle(providerRequest(RP_PACKAGE_1)),
                        ProviderCreateCredentialRequest.asBundle(providerRequest(RP_PACKAGE_2)),
                    ),
                )

        val request = IntentHandler.retrieveProviderBatchCreateCredentialRequest(intent)

        Truth.assertThat(request).isNotNull()
        Truth.assertThat(request!!.requests.map { it.callingAppInfo.packageName })
            .containsExactly(RP_PACKAGE_1, RP_PACKAGE_2)
            .inOrder()
        val callingRequest = request.requests[0].callingRequest
        Truth.assertThat(callingRequest).isInstanceOf(CreatePublicKeyCredentialRequest::class.java)
        Truth.assertThat((callingRequest as CreatePublicKeyCredentialRequest).requestJson)
            .isEqualTo(TEST_CREATION_REQUEST_JSON)
    }

    @Test
    fun retrieveProviderBatchCreateCredentialRequest_emptyIntent_returnsNull() {
        Truth.assertThat(IntentHandler.retrieveProviderBatchCreateCredentialRequest(Intent()))
            .isNull()
    }

    @Test
    fun retrieveProviderBatchCreateCredentialRequest_emptyList_returnsNull() {
        val intent =
            Intent()
                .putParcelableArrayListExtra(
                    EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS,
                    arrayListOf<Bundle>(),
                )

        Truth.assertThat(IntentHandler.retrieveProviderBatchCreateCredentialRequest(intent))
            .isNull()
    }

    @Test
    fun retrieveProviderBatchCreateCredentialRequest_malformedRequest_returnsNull() {
        val intent =
            Intent()
                .putParcelableArrayListExtra(
                    EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS,
                    arrayListOf(
                        ProviderCreateCredentialRequest.asBundle(providerRequest(RP_PACKAGE_1)),
                        Bundle(),
                    ),
                )

        Truth.assertThat(IntentHandler.retrieveProviderBatchCreateCredentialRequest(intent))
            .isNull()
    }

    @Test
    fun retrieveProviderBatchCreateCredentialRequest_duplicatePackageNames_returnsNull() {
        val intent =
            Intent()
                .putParcelableArrayListExtra(
                    EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS,
                    arrayListOf(
                        ProviderCreateCredentialRequest.asBundle(providerRequest(RP_PACKAGE_1)),
                        ProviderCreateCredentialRequest.asBundle(providerRequest(RP_PACKAGE_1)),
                    ),
                )

        Truth.assertThat(IntentHandler.retrieveProviderBatchCreateCredentialRequest(intent))
            .isNull()
    }

    @Test
    fun setAndRetrieveBatchCreateCredentialResponse_roundTrip() {
        val intent = Intent()
        val response =
            BatchCreateCredentialResponse(
                listOf(
                    CreateCredentialResult(
                        RP_PACKAGE_1,
                        CreatePublicKeyCredentialResponse(TEST_REGISTRATION_RESPONSE_JSON),
                    ),
                    CreateCredentialResult(
                        RP_PACKAGE_2,
                        CreateCredentialUnknownException("creation failed"),
                    ),
                )
            )

        IntentHandler.setBatchCreateCredentialResponse(intent, response)
        val retrieved = IntentHandler.retrieveBatchCreateCredentialResponse(intent)

        Truth.assertThat(retrieved).isNotNull()
        val results = retrieved!!.results
        Truth.assertThat(results.map { it.packageName })
            .containsExactly(RP_PACKAGE_1, RP_PACKAGE_2)
            .inOrder()
        Truth.assertThat(results[0].exception).isNull()
        Truth.assertThat(
                (results[0].response as CreatePublicKeyCredentialResponse).registrationResponseJson
            )
            .isEqualTo(TEST_REGISTRATION_RESPONSE_JSON)
        Truth.assertThat(results[1].response).isNull()
        Truth.assertThat(results[1].exception!!.type)
            .isEqualTo(CreateCredentialUnknownException().type)
        Truth.assertThat(results[1].exception!!.errorMessage.toString())
            .isEqualTo("creation failed")
    }

    @Test
    fun retrieveBatchCreateCredentialResponse_emptyIntent_returnsNull() {
        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialResponse(Intent())).isNull()
    }

    @Test
    fun retrieveBatchCreateCredentialResponse_resultWithoutPackageName_returnsNull() {
        val intent =
            Intent()
                .putParcelableArrayListExtra(
                    EXTRA_BATCH_CREATE_CREDENTIAL_RESULTS,
                    arrayListOf(Bundle()),
                )

        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialResponse(intent)).isNull()
    }

    @Test
    fun setAndRetrieveBatchCreateCredentialException_roundTrip() {
        val intent = Intent()

        IntentHandler.setBatchCreateCredentialException(
            intent,
            CreateCredentialUnknownException("batch failed"),
        )
        val retrieved = IntentHandler.retrieveBatchCreateCredentialException(intent)

        Truth.assertThat(retrieved).isNotNull()
        Truth.assertThat(retrieved!!.type).isEqualTo(CreateCredentialUnknownException().type)
        Truth.assertThat(retrieved.errorMessage.toString()).isEqualTo("batch failed")
    }

    @Test
    fun retrieveBatchCreateCredentialException_emptyIntent_returnsNull() {
        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialException(Intent())).isNull()
    }

    @Test
    fun setBatchCreateCredentialResponse_clearsException() {
        val intent = Intent()
        IntentHandler.setBatchCreateCredentialException(
            intent,
            CreateCredentialUnknownException("batch failed"),
        )

        IntentHandler.setBatchCreateCredentialResponse(
            intent,
            BatchCreateCredentialResponse(
                listOf(
                    CreateCredentialResult(
                        RP_PACKAGE_1,
                        CreatePublicKeyCredentialResponse(TEST_REGISTRATION_RESPONSE_JSON),
                    )
                )
            ),
        )

        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialException(intent)).isNull()
        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialResponse(intent)).isNotNull()
    }

    @Test
    fun setBatchCreateCredentialException_clearsResponse() {
        val intent = Intent()
        IntentHandler.setBatchCreateCredentialResponse(
            intent,
            BatchCreateCredentialResponse(
                listOf(
                    CreateCredentialResult(
                        RP_PACKAGE_1,
                        CreatePublicKeyCredentialResponse(TEST_REGISTRATION_RESPONSE_JSON),
                    )
                )
            ),
        )

        IntentHandler.setBatchCreateCredentialException(
            intent,
            CreateCredentialUnknownException("batch failed"),
        )

        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialResponse(intent)).isNull()
        Truth.assertThat(IntentHandler.retrieveBatchCreateCredentialException(intent)).isNotNull()
    }

    private fun providerRequest(packageName: String): ProviderCreateCredentialRequest =
        ProviderCreateCredentialRequest(
            callingRequest = CreatePublicKeyCredentialRequest(TEST_CREATION_REQUEST_JSON),
            callingAppInfo = CallingAppInfo(packageName, SigningInfo()),
        )

    private companion object {
        private const val EXTRA_REQUEST_JSON =
            "androidx.credentials.providerevents.extra.IMPORT_CREDENTIALS_REQUEST_JSON"
        private const val EXTRA_PACKAGE_NAME =
            "androidx.credentials.providerevents.extra.CALLING_PACKAGE_NAME"
        private const val EXTRA_SIGNING_INFO =
            "androidx.credentials.providerevents.extra.SIGNING_INFO"
        private const val EXTRA_CRED_ID = "androidx.credentials.providerevents.extra.CREDENTIAL_ID"
        private const val EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS =
            "androidx.credentials.providerevents.extra.BATCH_CREATE_CREDENTIAL_REQUESTS"
        private const val EXTRA_BATCH_CREATE_CREDENTIAL_RESULTS =
            "androidx.credentials.providerevents.extra.BATCH_CREATE_CREDENTIAL_RESULTS"
        private const val RP_PACKAGE_1 = "com.example.rp1"
        private const val RP_PACKAGE_2 = "com.example.rp2"
        private const val TEST_CREATION_REQUEST_JSON =
            "{\"rp\":{\"id\":\"example.com\",\"name\":\"Example\"}," +
                "\"user\":{\"id\":\"dXNlcg\",\"name\":\"user@example.com\"," +
                "\"displayName\":\"User\"},\"challenge\":\"Y2hhbGxlbmdl\"," +
                "\"pubKeyCredParams\":[{\"type\":\"public-key\",\"alg\":-7}]}"
        private const val TEST_REGISTRATION_RESPONSE_JSON = "{\"id\":\"credential-id\"}"
        private val TEST_REQUEST_JSON =
            "{\"credentialTypes\":[\"basic-auth\"],\"knownExtensions\":[\"shared\"]}"
        private val TEST_PACKAGE_NAME = "com.example.test.app"
    }
}
