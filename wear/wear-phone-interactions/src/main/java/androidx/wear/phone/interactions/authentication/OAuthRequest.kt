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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

/**
 * The OAuth request to be sent to the server to start the OAuth 2 authentication flow.
 */
public class OAuthRequest internal constructor(
    /** The package name of the app sending the auth request. */
    public val packageName: String,

    /**
     * The Url of the auth request.
     *
     * The request is expected to create a URL with the following format:
     *
     * ```
     *     https://authorization-server.com/auth?client_id=XXXXX
     *     &redirect_uri=https://wear.googleapis.com/3p_auth/mypackagename
     *     &response_type=code
     *     &code_challenge=XXXXX...XXX
     *     &code_challenge_method=S256
     * ```
     */
    public val requestUrl: Uri
) {
    public companion object {
        /**
         * The default google-specific custom URL to route the response from the auth
         * server back to the 1P companion app, which then forwards it to the 3P app that made
         * the request on the wear device.
         *
         * To deliver an Auth response to your Wear app, set the redirect_uri
         * parameter on the Auth request, with your app's package name appended.
         *
         * For example, if your app's package name is com.package.name, with 1P companion app
         * paired,  the redirect_uri query will be WEAR_REDIRECT_URL_PREFIX + "com.package.name".
         */
        public const val WEAR_REDIRECT_URL_PREFIX: String = "https://wear.googleapis.com/3p_auth/"
    }

    /**
     * Builder for constructing new instance of OAuth request.
     */
    public class Builder(private val packageName: String) {
        private var authProviderUrl: Uri? = null
        private var codeChallenge: CodeChallenge? = null
        private var clientId: String? = null
        private var redirectUrl: Uri? = null
        /**
         * Set the url of the auth provider site.
         * It provides the address pointing to the 3p/4p auth site. Appending query parameters in
         * this uri is optional, it is recommended to let the builder append query parameters
         * automatically through the use of setters (no setter is required for the builder to
         * append the redirect_uri).
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setAuthProviderUrl(authProviderUrl: Uri): Builder =
            this.apply {
                this.authProviderUrl = authProviderUrl
            }

        /**
         * Set the code challenge for authentication with PKCE (proof key for code exchange).
         * With this setter called, the builder appends the "code_challenge",
         * "code_challenge_method" and "response_type" queries to the requestUrl.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setCodeChallenge(codeChallenge: CodeChallenge): Builder =
            this.apply {
                this.codeChallenge = codeChallenge
            }

        /**
         * Set the client id of this OAuth request.
         * With this setter called. the builder appends the "client_id" to the requestUrl.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setClientId(clientId: String): Builder =
            this.apply {
                this.clientId = clientId
            }

        /**
         * Set the redirect url the companion app registered to, so that the response will be
         * routed from the auth server back to the companion.
         *
         * Calling this method is optional. If the redirect URL is not specified, it will be
         * automatically set to [WEAR_REDIRECT_URL_PREFIX]
         *
         * Note, the app package name should NOT be included here, it will be appended to the end
         * of redirect_uri automatically in [Builder.build].
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public fun setRedirectUrl(redirectUrl: Uri): Builder =
            this.apply {
                this.redirectUrl = redirectUrl
            }

        @RequiresApi(Build.VERSION_CODES.O)
        internal fun composeRequestUrl(): Uri {
            require(authProviderUrl != null) {
                "The request requires the auth provider url to be provided."
            }
            val requestUriBuilder = authProviderUrl!!.buildUpon()

            clientId?.let {
                appendQueryParameter(requestUriBuilder, "client_id", clientId!!)
            }

            /**
             * Set the request url by redirecting the auth provider URL with the WearOS auth
             * site [WEAR_REDIRECT_URL_PREFIX].
             * The receiving app's package name is also required as the 3rd path component in the
             * redirect_uri, this allows Wear to ensure other apps cannot reuse your redirect_uri
             * to receive responses.
             */
            appendQueryParameter(
                requestUriBuilder,
                "redirect_uri",
                Uri.withAppendedPath(
                    if (redirectUrl == null) Uri.parse(WEAR_REDIRECT_URL_PREFIX) else redirectUrl,
                    packageName
                ).toString()
            )

            codeChallenge?.let {
                appendQueryParameter(requestUriBuilder, "response_type", "code")
                appendQueryParameter(
                    requestUriBuilder,
                    "code_challenge",
                    it.value
                )
                appendQueryParameter(requestUriBuilder, "code_challenge_method", "S256")
            }

            return requestUriBuilder.build()
        }

        private fun appendQueryParameter(
            requestUriBuilder: Uri.Builder,
            queryKey: String,
            expectedQueryParam: String
        ) {
            val currentQueryParam = authProviderUrl!!.getQueryParameter(queryKey)
            currentQueryParam?.let {
                require(expectedQueryParam == currentQueryParam) {
                    "The '$queryKey' query param already exists in the authProviderUrl, " +
                        "expect to have the value of '$expectedQueryParam', but " +
                        "'$currentQueryParam' is given. Please correct it,  or leave it out " +
                        "to allow the request builder to append it automatically."
                }
            } ?: run {
                requestUriBuilder.appendQueryParameter(queryKey, expectedQueryParam)
            }
        }

        /** Build the request instance specified by this builder */
        @RequiresApi(Build.VERSION_CODES.O)
        public fun build(): OAuthRequest {
            val requestUrl = composeRequestUrl()
            checkValidity(requestUrl)
            return OAuthRequest(packageName, requestUrl)
        }

        // check the validity of the request for the OAuth2 flow with PKCE
        private fun checkValidity(requestUrl: Uri) {
            // check that client_id is provided in the request
            queryParameterCheck(requestUrl, "client_id", null)
            // check that code_challenge is provided in the request
            queryParameterCheck(requestUrl, "code_challenge", null)
            // check that code_challenge_mode is provided in the request, and with value 'S256'
            queryParameterCheck(requestUrl, "code_challenge_method", "S256")
            // check that response_type is provided in the request, and with value 'code'
            queryParameterCheck(requestUrl, "response_type", "code")
        }

        private fun queryParameterCheck(
            requestUrl: Uri,
            queryKey: String,
            expectedQueryParameter: String?
        ) {
            val queryParam = requestUrl.getQueryParameter(queryKey)
            require(queryParam != null) {
                "The use of Proof Key for Code Exchange is required for authentication, " +
                    "please provide $queryKey in the request."
            }
            expectedQueryParameter?.let {
                require(queryParam == expectedQueryParameter) {
                    "The use of Proof Key for Code Exchange is required for authentication, " +
                        "the query parameter '$queryKey' is expected to have value of " +
                        "'$expectedQueryParameter', but '$queryParam' is set"
                }
            }
        }
    }

    internal fun toBundle(): Bundle = Bundle().apply {
        putParcelable(RemoteAuthClient.KEY_REQUEST_URL, requestUrl)
        putString(RemoteAuthClient.KEY_PACKAGE_NAME, packageName)
    }
}
