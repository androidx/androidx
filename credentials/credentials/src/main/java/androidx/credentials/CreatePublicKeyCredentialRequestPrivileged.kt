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

package androidx.credentials

/**
 * A privileged request to register a passkey from the user’s public key credential provider, where
 * the caller can modify the rp. Only callers with privileged permission, e.g. user’s default
 * brower, caBLE, can use this.
 *
 * @property requestJson the privileged request in JSON format
 * @property allowHybrid defines whether hybrid credentials are allowed to fulfill this request,
 * true by default
 * @property rp the expected true RP ID which will override the one in the [requestJson]
 * @property clientDataHash a hash that is used to verify the [rp] Identity
 * @throws NullPointerException If any of [allowHybrid], [requestJson], [rp], or [clientDataHash] is
 * null. This is handled by the Kotlin runtime
 * @throws IllegalArgumentException If any of [requestJson], [rp], or [clientDataHash] is empty
 *
 * @hide
 */
class CreatePublicKeyCredentialRequestPrivileged @JvmOverloads constructor(
    requestJson: String,
    val rp: String,
    val clientDataHash: String,
    @get:JvmName("allowHybrid")
    val allowHybrid: Boolean = true
) : CreatePublicKeyCredentialBaseRequest(requestJson) {

    init {
        require(rp.isNotEmpty()) { "rp must not be empty" }
        require(clientDataHash.isNotEmpty()) { "clientDataHash must not be empty" }
    }

    /** A builder for [CreatePublicKeyCredentialRequestPrivileged]. */
    class Builder(var requestJson: String, var rp: String, var clientDataHash: String) {

        private var allowHybrid: Boolean = true

        /**
         * Sets the privileged request in JSON format.
         */
        fun setRequestJson(requestJson: String): Builder {
            this.requestJson = requestJson
            return this
        }

        /**
         * Sets whether hybrid credentials are allowed to fulfill this request, true by default.
         */
        fun setAllowHybrid(allowHybrid: Boolean): Builder {
            this.allowHybrid = allowHybrid
            return this
        }

        /**
         * Sets the expected true RP ID which will override the one in the [requestJson].
         */
        fun setRp(rp: String): Builder {
            this.rp = rp
            return this
        }

        /**
         * Sets a hash that is used to verify the [rp] Identity.
         */
        fun setClientDataHash(clientDataHash: String): Builder {
            this.clientDataHash = clientDataHash
            return this
        }

        /** Builds a [CreatePublicKeyCredentialRequestPrivileged]. */
        fun build(): CreatePublicKeyCredentialRequestPrivileged {
            return CreatePublicKeyCredentialRequestPrivileged(this.requestJson,
                this.rp, this.clientDataHash, this.allowHybrid)
        }
    }
}
