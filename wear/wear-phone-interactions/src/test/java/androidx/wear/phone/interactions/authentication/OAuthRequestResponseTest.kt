/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.phone.interactions.authentication

import android.net.Uri
import androidx.wear.phone.interactions.WearPhoneInteractionsTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [OAuthRequest] and [OAuthResponse] */
@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument
public class OAuthRequestResponseTest {
    internal companion object {
        private const val authProviderUrl = "http://account.myapp.com/auth"
        private const val clientId = "iamtheclient"
        private const val appPackageName = "com.friendlyapp"
        private const val redirectUrl = OAuthRequest.WEAR_REDIRECT_URL_PREFIX
        private const val redirectUrlWithPackageName = "$redirectUrl$appPackageName"
        private const val customRedirectUrl = "https://app.example.com/oauth2redirect"
        private const val customRedirectUrlWithPackageName = "$customRedirectUrl/$appPackageName"
        private val responseUrl = Uri.parse("http://myresponseurl")
    }

    private fun checkBuildSuccess(
        builder: OAuthRequest.Builder,
        expectedAuthProviderUrl: String,
        expectedClientId: String,
        expectedRedirectUri: String,
        expectedCodeChallenge: String,
    ) {
        var request: OAuthRequest? = null
        try {
            request = builder.build()
        } catch (e: Exception) {
            fail("The build shall succeed and this line will not be executed")
        }

        val requestUrl = request!!.requestUrl
        assertEquals(requestUrl.toString().indexOf(expectedAuthProviderUrl), 0)
        assertEquals(requestUrl.getQueryParameter("redirect_uri"), expectedRedirectUri)
        assertEquals(requestUrl.getQueryParameter("client_id"), expectedClientId)
        assertEquals(requestUrl.getQueryParameter("response_type"), "code")
        assertEquals(requestUrl.getQueryParameter("code_challenge"), expectedCodeChallenge)
        assertEquals(requestUrl.getQueryParameter("code_challenge_method"), "S256")
    }

    private fun checkBuildFailure(builder: OAuthRequest.Builder, errorMsg: String) {
        try {
            builder.build()
            fail("should fail without providing correct/adequate info for building request")
        } catch (e: Exception) {
            assertEquals(errorMsg, e.message)
        }
    }

    @Test
    public fun testRequestBuildScuccessWithSetters() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(authProviderUrl = Uri.parse(authProviderUrl))
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildScuccessWithCustomRedirectUti() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(authProviderUrl = Uri.parse(authProviderUrl))
            .setClientId(clientId)
            .setRedirectUrl(Uri.parse(customRedirectUrl))
            .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            customRedirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildSuccessWithCompleteUrl() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                authProviderUrl = Uri.parse(
                    "$authProviderUrl?client_id=$clientId" +
                        "&redirect_uri=$redirectUrlWithPackageName" +
                        "&response_type=code" +
                        "&code_challenge=${codeChallenge.value}" +
                        "&code_challenge_method=S256"
                )
            )

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildFailureWithoutAuthProviderUrl() {
        val builder = OAuthRequest.Builder(appPackageName)
            .setClientId(clientId)
            .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The request requires the auth provider url to be provided."
        )
    }

    @Test
    public fun testRequestBuildFailureWithoutClientId() {
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(authProviderUrl = Uri.parse(authProviderUrl))
            .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The use of Proof Key for Code Exchange is required for authentication, " +
                "please provide client_id in the request."
        )
    }

    @Test
    public fun testRequestBuildFailureWithConflictedClientId() {
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?client_id=XXX")
            )
            .setClientId(clientId)
            .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The 'client_id' query param already exists in the authProviderUrl, " +
                "expect to have the value of '$clientId', but " +
                "'XXX' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically."
        )
    }

    @Test
    public fun testRequestBuildSuccessWithDuplicatedClientId() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?client_id=$clientId")
            )
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildFailureWithoutCodeChallenge() {
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(authProviderUrl = Uri.parse(authProviderUrl))
            .setClientId(clientId)

        checkBuildFailure(
            builder,
            "The use of Proof Key for Code Exchange is required for authentication, " +
                "please provide code_challenge in the request."
        )
    }

    @Test
    public fun testRequestBuildFailureWithConflictedCodeChallenge() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?code_challenge=XXX")
            )
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildFailure(
            builder,
            "The 'code_challenge' query param already exists in the authProviderUrl, " +
                "expect to have the value of '${codeChallenge.value}', but " +
                "'XXX' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically."
        )
    }

    @Test
    public fun testRequestBuildSuccessWithDuplicatedCodeChallenge() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?code_challenge=${codeChallenge.value}")
            )
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildFailureWithWrongResponseType() {
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?response_type=XXX")
            )
            .setClientId(clientId)
            .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The 'response_type' query param already exists in the authProviderUrl, " +
                "expect to have the value of 'code', but " +
                "'XXX' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically."
        )
    }

    @Test
    public fun testRequestBuildFailureWithDuplicatedResponseType() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?response_type=code")
            )
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildFailureWithWrongCodeChallengeMethod() {
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?code_challenge_method=PLAIN")
            )
            .setClientId(clientId)
            .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The 'code_challenge_method' query param already exists in the authProviderUrl, " +
                "expect to have the value of 'S256', but " +
                "'PLAIN' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically."
        )
    }

    @Test
    public fun testRequestBuildFailureWithDuplicatedCodeChallengeMethod() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?code_challenge_method=S256")
            )
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value
        )
    }

    @Test
    public fun testRequestBuildFailureWithConflictedRedirectUri() {
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder = OAuthRequest.Builder(appPackageName)
            .setAuthProviderUrl(
                Uri.parse("$authProviderUrl?redirect_uri=$redirectUrlWithPackageName")
            )
            .setRedirectUrl(Uri.parse(customRedirectUrl))
            .setClientId(clientId)
            .setCodeChallenge(codeChallenge)

        checkBuildFailure(
            builder,
            "The 'redirect_uri' query param already exists in the authProviderUrl, " +
                "expect to have the value of '$customRedirectUrlWithPackageName', but " +
                "'$redirectUrlWithPackageName' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically."
        )
    }

    @Test
    public fun testNoErrorResponseBuild() {
        val response = OAuthResponse.Builder().setResponseUrl(responseUrl).build()

        assertEquals(RemoteAuthClient.NO_ERROR, response.errorCode)
        assertEquals(responseUrl, response.responseUrl)
    }

    @Test
    public fun testErrorResponseBuild() {
        val response1 = OAuthResponse.Builder()
            .setErrorCode(RemoteAuthClient.ERROR_UNSUPPORTED)
            .build()

        assertEquals(RemoteAuthClient.ERROR_UNSUPPORTED, response1.errorCode)
        assertEquals(null, response1.responseUrl)

        val response2 = OAuthResponse.Builder()
            .setErrorCode(RemoteAuthClient.ERROR_PHONE_UNAVAILABLE)
            .build()

        assertEquals(RemoteAuthClient.ERROR_PHONE_UNAVAILABLE, response2.errorCode)
        assertEquals(null, response2.responseUrl)
    }
}