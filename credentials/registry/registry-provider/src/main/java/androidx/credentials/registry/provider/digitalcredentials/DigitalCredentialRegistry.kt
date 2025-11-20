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

package androidx.credentials.registry.provider.digitalcredentials

import androidx.credentials.DigitalCredential.Companion.TYPE_DIGITAL_CREDENTIAL
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.registry.provider.RegisterCredentialsRequest
import androidx.credentials.registry.provider.RegistryManager

/**
 * A request to register digital credentials with Credential Manager.
 *
 * The ([type], [id]) properties together act as a primary key for this registry record stored with
 * the Registry Manager. You later can use them to perform overwrite or deletion.
 *
 * @param id the unique id that identifies this registry, such that it won't be overwritten by other
 *   different registries of the same `type`; in other words, registering the registry with the same
 *   id will overwrite the existing one when applicable
 * @param credentials the credentials in raw bytes
 * @param matcher the matcher wasm binary in bytes; the matcher will be interpreted and run in a
 *   safe and privacy-preserving sandbox upon an incoming request and it should output the qualified
 *   credentials given the [credentials] and the request
 * @param intentAction the intent action that will be used to launch your fulfillment activity when
 *   one of your credentials was chosen by the user, default to
 *   [RegistryManager.ACTION_GET_CREDENTIAL] when unspecified; when Credential Manager launches your
 *   fulfillment activity, it will build an intent with the given `intentAction` targeting your
 *   package, so this is useful when you need to define different fulfillment activities for
 *   different registries
 * @constructor
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
public abstract class DigitalCredentialRegistry
@JvmOverloads
constructor(
    id: String,
    credentials: ByteArray,
    matcher: ByteArray,
    intentAction: String = RegistryManager.ACTION_GET_CREDENTIAL,
) :
    RegisterCredentialsRequest(
        type = TYPE_DIGITAL_CREDENTIAL,
        id = id,
        credentials = credentials,
        matcher = matcher,
        intentAction = intentAction,
    ) {
    public companion object {
        /**
         * The credential should be displayed with the UI styles that serves the verification use
         * case. This is the default use case.
         */
        public const val DISPLAY_TYPE_VERIFICATION: Int = 0
    }
}
