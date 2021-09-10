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
import androidx.wear.phone.interactions.authentication.RemoteAuthClient.Companion.ErrorCode

/**
 * The authentication response to be sent back to the client after completing the OAuth2 flow.
 */
public class OAuthResponse internal constructor(
    /** The error code that indicated the request result status. */
    @ErrorCode public val errorCode: Int = RemoteAuthClient.NO_ERROR,

    /** The Url of the auth response. In case of an error it will be null. */
    public val responseUrl: Uri?
) {
    /**
     * Builder for constructing new instance of authentication response.
     */
    public class Builder {
        @ErrorCode
        private var errorCode: Int = RemoteAuthClient.NO_ERROR
        private var responseUrl: Uri? = null

        /** Set the error code to indicate the request result status */
        public fun setErrorCode(@ErrorCode errorCode: Int): Builder =
            this.apply {
                this.errorCode = errorCode
            }

        /** Set the Url of the auth response */
        public fun setResponseUrl(responseUrl: Uri): Builder = this.apply {
            this.responseUrl = responseUrl
        }

        /** Build the response instance specified by this builder*/
        public fun build(): OAuthResponse = OAuthResponse(errorCode, responseUrl)
    }
}
