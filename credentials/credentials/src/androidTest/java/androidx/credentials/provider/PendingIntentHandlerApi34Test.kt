/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.credentials.provider

import android.content.Intent
import android.content.pm.SigningInfo
import android.credentials.CredentialOption
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.os.ResultReceiver
import android.service.credentials.CallingAppInfo
import android.service.credentials.CreateCredentialRequest
import android.service.credentials.GetCredentialRequest
import androidx.credentials.CreateCustomCredentialResponse
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CustomCredential
import androidx.credentials.DigitalCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.assertEquals
import androidx.credentials.createDummyProviderGetCredentialRequest
import androidx.credentials.equals
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.domerrors.ConstraintError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.provider.PendingIntentHandler.Companion.EXTRA_LARGE_PAYLOAD_RESULT_RECEIVER
import androidx.credentials.provider.PendingIntentHandler.Companion.EXTRA_PASS_IT_BY_RESULT_RECEIVER
import androidx.credentials.provider.PendingIntentHandler.Companion.EXTRA_RP_PID
import androidx.credentials.provider.PendingIntentHandler.Companion.setCreateCredentialResponse
import androidx.credentials.setUpCreatePasswordRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
class PendingIntentHandlerApi34Test {
    companion object {
        private val GET_CREDENTIAL_OPTION =
            CredentialOption.Builder("type", Bundle(), Bundle()).build()

        private val GET_CREDENTIAL_REQUEST =
            GetCredentialRequest(
                CallingAppInfo("package_name", SigningInfo()),
                ArrayList(setOf(GET_CREDENTIAL_OPTION)),
            )

        private const val BIOMETRIC_AUTHENTICATOR_TYPE = 1

        private const val BIOMETRIC_AUTHENTICATOR_ERROR_CODE = 5

        private const val BIOMETRIC_AUTHENTICATOR_ERROR_MSG = "error"

        private const val FRAMEWORK_EXPECTED_CONSTANT_ERROR_CODE =
            "androidx.credentials.provider.BIOMETRIC_AUTH_ERROR_CODE"

        private const val FRAMEWORK_EXPECTED_CONSTANT_ERROR_MESSAGE =
            "androidx.credentials.provider.BIOMETRIC_AUTH_ERROR_MESSAGE"

        private const val FRAMEWORK_EXPECTED_CONSTANT_AUTH_RESULT =
            "androidx.credentials.provider.BIOMETRIC_AUTH_RESULT"

        private val context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun test_constantsMatchFrameworkExpectations_success() {
        assertThat(AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE)
            .isEqualTo(FRAMEWORK_EXPECTED_CONSTANT_AUTH_RESULT)
        assertThat(AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR)
            .isEqualTo(FRAMEWORK_EXPECTED_CONSTANT_ERROR_CODE)
        assertThat(AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE)
            .isEqualTo(FRAMEWORK_EXPECTED_CONSTANT_ERROR_MESSAGE)
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithSuccessBpAuthJetpack_retrieveJetpackResult() {
        for (jetpackResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.values) {
            val biometricPromptResult = BiometricPromptResult(AuthenticationResult(jetpackResult))
            val request = setUpCreatePasswordRequest()
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(biometricPromptResult.authenticationResult)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationResult!!.authenticationType,
                jetpackResult,
            )
        }
    }

    @Test
    fun test_retrieveProviderGetCredReqWithSuccessBpAuthJetpack_retrieveJetpackResult() {
        for (jetpackResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.values) {
            val biometricPromptResult = BiometricPromptResult(AuthenticationResult(jetpackResult))
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(GET_CREDENTIAL_REQUEST, request!!)
            Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
            Assert.assertEquals(
                request.biometricPromptResult!!.authenticationResult!!.authenticationType,
                jetpackResult,
            )
        }
    }

    // While possible to test non-conversion logic, that would equate functionally to the normal
    // jetpack tests as there is no validation.
    @Test
    fun test_retrieveProviderCreateCredReqWithSuccessBpAuthFramework_correctlyConvertedResult() {
        for (frameworkResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationResult.createFrom(
                        uiAuthenticationType = frameworkResult,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val request = setUpCreatePasswordRequest()
            val expectedResult =
                AuthenticationResult.biometricFrameworkToJetpackResultMap[frameworkResult]
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(biometricPromptResult.authenticationResult)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationResult!!.authenticationType,
                expectedResult,
            )
        }
    }

    // While possible to test non-conversion logic, that would equate functionally to the normal
    // jetpack tests as there is no validation.
    @Test
    fun test_retrieveProviderGetCredReqWithSuccessBpAuthFramework_correctlyConvertedResult() {
        for (frameworkResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationResult.createFrom(
                        uiAuthenticationType = frameworkResult,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val expectedResult =
                AuthenticationResult.biometricFrameworkToJetpackResultMap[frameworkResult]
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(GET_CREDENTIAL_REQUEST, request!!)
            Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
            Assert.assertEquals(
                request.biometricPromptResult!!.authenticationResult!!.authenticationType,
                expectedResult,
            )
        }
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithFailureBpAuthJetpack_retrieveJetpackError() {
        for (jetpackError in AuthenticationError.biometricFrameworkToJetpackErrorMap.values) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError(jetpackError, BIOMETRIC_AUTHENTICATOR_ERROR_MSG)
                )
            val request = setUpCreatePasswordRequest()
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                jetpackError,
            )
        }
    }

    @Test
    fun test_retrieveProviderGetCredReqWithFailureBpAuthJetpack_retrieveJetpackError() {
        for (jetpackError in AuthenticationError.biometricFrameworkToJetpackErrorMap.values) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError(jetpackError, BIOMETRIC_AUTHENTICATOR_ERROR_MSG)
                )
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val retrievedRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(GET_CREDENTIAL_REQUEST, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                jetpackError,
            )
        }
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithFailureBpAuthFramework_correctlyConvertedError() {
        for (frameworkError in AuthenticationError.biometricFrameworkToJetpackErrorMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError.createFrom(
                        uiErrorCode = frameworkError,
                        uiErrorMessage = BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val expectedErrorCode =
                AuthenticationError.biometricFrameworkToJetpackErrorMap[frameworkError]
            val request = setUpCreatePasswordRequest()
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                expectedErrorCode,
            )
        }
    }

    @Test
    fun test_retrieveProviderGetCredReqWithFailureBpAuthFramework_correctlyConvertedError() {
        for (frameworkError in AuthenticationError.biometricFrameworkToJetpackErrorMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError.createFrom(
                        uiErrorCode = frameworkError,
                        uiErrorMessage = BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val expectedErrorCode =
                AuthenticationError.biometricFrameworkToJetpackErrorMap[frameworkError]
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val retrievedRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(GET_CREDENTIAL_REQUEST, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                expectedErrorCode,
            )
        }
    }

    @Test
    fun createCredentialException_success() {
        val intent = Intent()
        val expected = CreateCredentialInterruptedException("message")

        PendingIntentHandler.setCreateCredentialException(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected::class.java)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
    }

    @Test
    fun createCredentialException_domException_success() {
        val intent = Intent()
        val expected = CreatePublicKeyCredentialDomException(ConstraintError(), "Error msg")

        PendingIntentHandler.setCreateCredentialException(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected::class.java)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
        val actualConverted = actual as CreatePublicKeyCredentialDomException
        assertThat(actualConverted.domError).isInstanceOf((expected.domError)::class.java)
    }

    @Test
    fun createCredentialException_emptyIntent_returnsNull() {
        val intent = Intent()

        assertThat(PendingIntentHandler.retrieveCreateCredentialException(intent)).isNull()
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithSuccessfulBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(AuthenticationResult(BIOMETRIC_AUTHENTICATOR_TYPE))
        val request = setUpCreatePasswordRequest()
        val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

        val retrievedRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

        Assert.assertNotNull(request)
        equals(request, retrievedRequest!!)
        Assert.assertNotNull(biometricPromptResult.authenticationResult)
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithFailureBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(
                AuthenticationError(
                    BIOMETRIC_AUTHENTICATOR_ERROR_CODE,
                    BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                )
            )
        val request = setUpCreatePasswordRequest()
        val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

        val retrievedRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

        Assert.assertNotNull(retrievedRequest)
        equals(request, retrievedRequest!!)
        Assert.assertEquals(biometricPromptResult, retrievedRequest.biometricPromptResult)
    }

    @Test
    fun test_retrieveProviderGetCredReqWithSuccessfulBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(AuthenticationResult(BIOMETRIC_AUTHENTICATOR_TYPE))
        val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        Assert.assertNotNull(request)
        equals(GET_CREDENTIAL_REQUEST, request!!)
        Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
    }

    @Test
    fun test_retrieveProviderGetCredReqWithFailingBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(
                AuthenticationError(
                    BIOMETRIC_AUTHENTICATOR_ERROR_CODE,
                    BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                )
            )
        val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        Assert.assertNotNull(request)
        equals(GET_CREDENTIAL_REQUEST, request!!)
        Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
    }

    private fun prepareIntentWithGetRequest(
        request: GetCredentialRequest,
        biometricPromptResult: BiometricPromptResult,
    ): Intent {
        val intent = Intent()
        intent.putExtra(
            android.service.credentials.CredentialProviderService.EXTRA_GET_CREDENTIAL_REQUEST,
            request,
        )
        prepareIntentWithBiometricResult(intent, biometricPromptResult)
        return intent
    }

    private fun prepareIntentWithCreateRequest(
        request: CreateCredentialRequest,
        biometricPromptResult: BiometricPromptResult,
    ): Intent {
        val intent = Intent()
        intent.putExtra(
            android.service.credentials.CredentialProviderService.EXTRA_CREATE_CREDENTIAL_REQUEST,
            request,
        )
        prepareIntentWithBiometricResult(intent, biometricPromptResult)
        return intent
    }

    private fun prepareIntentWithBiometricResult(
        intent: Intent,
        biometricPromptResult: BiometricPromptResult,
    ) {
        if (biometricPromptResult.isSuccessful) {
            Assert.assertNotNull(biometricPromptResult.authenticationResult)
            var extraResultKey = AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE
            intent.putExtra(
                extraResultKey,
                biometricPromptResult.authenticationResult!!.authenticationType,
            )
        } else {
            Assert.assertNotNull(biometricPromptResult.authenticationError)
            var extraErrorKey = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR
            var extraErrorMessageKey = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE
            intent.putExtra(extraErrorKey, biometricPromptResult.authenticationError!!.errorCode)
            intent.putExtra(
                extraErrorMessageKey,
                biometricPromptResult.authenticationError!!.errorMsg,
            )
        }
    }

    @Test
    fun test_credentialException() {
        val intent = Intent()
        val initialException = GetCredentialInterruptedException("message")

        PendingIntentHandler.setGetCredentialException(intent, initialException)

        val finalException = intent.getGetCredentialException()
        assertThat(finalException).isNotNull()
        assertThat(finalException!!.type).isEqualTo(initialException.type)
        assertThat(finalException.message).isEqualTo(initialException.message)
    }

    @Test
    fun test_credentialException_nullWhenEmptyIntent() {
        val intent = Intent()

        assertThat(intent.getGetCredentialException()).isNull()
    }

    @Test
    fun test_beginGetResponse() {
        val intent = Intent()
        val initialResponse = BeginGetCredentialResponse.Builder().build()

        PendingIntentHandler.setBeginGetCredentialResponse(intent, initialResponse)

        val finalResponse = intent.getBeginGetResponse()
        assertThat(finalResponse).isNotNull()
        assertEquals(context, finalResponse!!, initialResponse)
    }

    @Test
    fun test_beginGetResponse_nullWhenEmptyIntent() {
        val intent = Intent()

        assertThat(intent.getBeginGetResponse()).isNull()
    }

    @Test
    fun test_credentialResponse() {
        val intent = Intent()
        val credential = PasswordCredential("a", "b")
        val initialResponse = GetCredentialResponse(credential)

        PendingIntentHandler.setGetCredentialResponse(
            intent,
            initialResponse,
            createDummyProviderGetCredentialRequest(),
        )

        val finalResponse = intent.getGetCredentialResponse()
        assertThat(finalResponse).isNotNull()
        assertEquals(finalResponse!!, initialResponse)
    }

    @Test
    fun test_credentialResponse_nullWhenEmptyIntent() {
        val intent = Intent()

        assertThat(intent.getGetCredentialResponse()).isNull()
    }

    @Test
    fun createCredentialCredentialResponse_passwordResponse_success() {
        val intent = Intent()
        val expected = CreatePasswordResponse()

        PendingIntentHandler.setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    fun setCreateCredentialResponse_customResponse_success() {
        val intent = Intent()
        val customData = Bundle()
        customData.putString("k1", "text")
        customData.putBinder("k2", Binder())
        val expected = CreateCustomCredentialResponse("type", customData)

        setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    fun retrieveCreateCredentialResponse_emptyResponse_returnsNull() {
        val actual = PendingIntentHandler.retrieveCreateCredentialResponse("type", Intent())

        assertThat(actual).isNull()
    }

    @Test
    fun test_credentialResponse_largePayload_usesResultReceiver() {
        val intent = Intent()
        val largeData = Bundle()
        val byteArray = Random.nextBytes(1024 * 1024 + 100)
        largeData.putByteArray("large_array", byteArray)
        val customCredential = androidx.credentials.CustomCredential("type", largeData)
        val initialResponse = GetCredentialResponse(customCredential)

        val requestData = Bundle()
        var receivedIntent: Intent? = null
        val receiver =
            object : android.os.ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    receivedIntent = resultData?.getParcelable("RESULT_DATA", Intent::class.java)
                }
            }
        requestData.putParcelable(EXTRA_LARGE_PAYLOAD_RESULT_RECEIVER, receiver)
        requestData.putInt(EXTRA_RP_PID, Process.myPid())
        val option =
            androidx.credentials.GetCustomCredentialOption(
                "type",
                requestData,
                Bundle(),
                false,
                true,
            )
        val request =
            ProviderGetCredentialRequest(
                listOf(option),
                androidx.credentials.provider.CallingAppInfo.create("pkg", SigningInfo(), "origin"),
            )

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse, request)

        assertThat(intent.getBooleanExtra(EXTRA_PASS_IT_BY_RESULT_RECEIVER, false)).isTrue()
        assertThat(receivedIntent).isNotNull()
        // Verify that the GetCredentialResponse can be successfully retrieved from the same
        // process.
        val finalResponse = PendingIntentHandler.retrieveGetCredentialResponse(receivedIntent!!)
        assertThat(finalResponse!!.credential.data.getByteArray("large_array")).isEqualTo(byteArray)
    }

    @Test
    fun test_credentialResponse_largePayload_noReceiver_doesNotUseResultReceiver() {
        val intent = Intent()
        val largeData = Bundle()
        val byteArray = ByteArray(205000)
        largeData.putByteArray("large_array", byteArray)
        val customCredential = androidx.credentials.CustomCredential("type", largeData)
        val initialResponse = GetCredentialResponse(customCredential)

        val option =
            androidx.credentials.GetCustomCredentialOption("type", Bundle(), Bundle(), false, true)
        val request =
            ProviderGetCredentialRequest(
                listOf(option),
                androidx.credentials.provider.CallingAppInfo.create("pkg", SigningInfo(), "origin"),
            )

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse, request)

        assertThat(intent.hasExtra(EXTRA_PASS_IT_BY_RESULT_RECEIVER)).isFalse()
    }

    @OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)
    @Test
    fun test_credentialResponse_multipleCredentials_success() {
        val intent = Intent()
        val credential1 =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val1\"}}")
        val credential2 =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val2\"}}")
        val initialResponse = GetCredentialResponse(listOf(credential1, credential2))

        val option = GetDigitalCredentialOption("{\"providers\":[{\"protocol\":\"openid4vp\"}]}")
        val request =
            ProviderGetCredentialRequest(
                listOf(option),
                androidx.credentials.provider.CallingAppInfo.create("pkg", SigningInfo(), "origin"),
            )

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse, request)

        val finalResponse = PendingIntentHandler.retrieveGetCredentialResponse(intent)
        assertThat(finalResponse).isNotNull()
        assertThat(finalResponse!!.credentials).hasSize(2)
        val retrievedCred1 = finalResponse.credentials[0] as DigitalCredential
        assertThat(retrievedCred1.credentialJson)
            .isEqualTo("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val1\"}}")
        val retrievedCred2 = finalResponse.credentials[1] as DigitalCredential
        assertThat(retrievedCred2.credentialJson)
            .isEqualTo("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val2\"}}")
    }

    @OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)
    @Test
    fun test_credentialResponse_multipleConcreteCredentials_success() {
        val intent = Intent()
        val passwordCred = PasswordCredential("username", "password")
        val publicKeyCred =
            PublicKeyCredential(
                "{\"id\":\"test_id\",\"rawId\":\"test_raw_id\",\"response\":{\"clientDataJSON\":\"client_data\",\"authenticatorData\":\"auth_data\",\"signature\":\"sig\"},\"type\":\"public-key\"}"
            )
        val digitalCred =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"jwt_token_123\"}}")
        val initialResponse =
            GetCredentialResponse(listOf(passwordCred, publicKeyCred, digitalCred))

        val option = GetCustomCredentialOption(passwordCred.type, Bundle(), Bundle(), false, true)
        val request =
            ProviderGetCredentialRequest(
                listOf(option),
                androidx.credentials.provider.CallingAppInfo.create("pkg", SigningInfo(), "origin"),
            )

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse, request)

        val finalResponse = PendingIntentHandler.retrieveGetCredentialResponse(intent)
        assertThat(finalResponse).isNotNull()
        assertThat(finalResponse!!.credentials).hasSize(3)

        val retrievedPassword = finalResponse.credentials[0] as PasswordCredential
        assertThat(retrievedPassword.id).isEqualTo("username")
        assertThat(retrievedPassword.password).isEqualTo("password")

        val retrievedPubKey = finalResponse.credentials[1] as PublicKeyCredential
        assertThat(retrievedPubKey.authenticationResponseJson).contains("test_id")

        val retrievedDigital = finalResponse.credentials[2] as DigitalCredential
        assertThat(retrievedDigital.credentialJson)
            .isEqualTo("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"jwt_token_123\"}}")
    }

    @OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)
    @Test
    fun test_credentialResponse_multipleCredentials_largePayload_success() {
        val intent = Intent()
        val largeVpToken1 = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9." + "a".repeat(300000)
        val largeJson1 =
            """{"protocol":"openid4vp","data":{"vp_token":"$largeVpToken1","presentation_submission":{"id":"sub_mdl","definition_id":"org.iso.18013.5.mDL","descriptor_map":[{"id":"mdl","format":"mso_mdoc","path":"$"}]}}}"""
        val cred1 = DigitalCredential(largeJson1)

        val largeVpToken2 = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9." + "b".repeat(400000)
        val largeJson2 =
            """{"protocol":"openid4vp","data":{"vp_token":"$largeVpToken2","presentation_submission":{"id":"sub_pid","definition_id":"eu.europa.ec.eudiw.pid","descriptor_map":[{"id":"pid","format":"sd_jwt_vc","path":"$"}]}}}"""
        val cred2 = DigitalCredential(largeJson2)

        val initialResponse = GetCredentialResponse(listOf(cred1, cred2))

        var receivedIntent: Intent? = null
        val receiver =
            object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    @Suppress("DEPRECATION")
                    receivedIntent = resultData?.getParcelable("RESULT_DATA")
                }
            }
        val requestJson =
            """{"providers":[{"protocol":"openid4vp","request":"eyJhbGciOiJFUzI1NiIs...request_jwt..."}]}"""
        val option = GetDigitalCredentialOption(requestJson)
        option.requestData.putParcelable(EXTRA_LARGE_PAYLOAD_RESULT_RECEIVER, receiver)
        option.requestData.putInt(EXTRA_RP_PID, Process.myPid())
        val request =
            ProviderGetCredentialRequest(
                listOf(option),
                androidx.credentials.provider.CallingAppInfo.create("pkg", SigningInfo(), "origin"),
            )

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse, request)

        assertThat(intent.getBooleanExtra(EXTRA_PASS_IT_BY_RESULT_RECEIVER, false)).isTrue()
        assertThat(receivedIntent).isNotNull()

        val finalResponse = PendingIntentHandler.retrieveGetCredentialResponse(receivedIntent!!)
        assertThat(finalResponse).isNotNull()
        assertThat(finalResponse!!.credentials).hasSize(2)

        val retrievedCred1 = finalResponse.credentials[0] as DigitalCredential
        assertThat(retrievedCred1.credentialJson).isEqualTo(largeJson1)

        val retrievedCred2 = finalResponse.credentials[1] as DigitalCredential
        assertThat(retrievedCred2.credentialJson).isEqualTo(largeJson2)
    }
}
