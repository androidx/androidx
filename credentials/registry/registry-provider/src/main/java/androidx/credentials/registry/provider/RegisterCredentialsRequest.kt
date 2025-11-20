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

package androidx.credentials.registry.provider

/**
 * A request to register credentials with Credential Manager.
 *
 * @property type the type of the credentials being registered; this matches the
 *   [androidx.credentials.Credential.type] that these registered credentials will be returned as
 * @property id the unique id that identifies this registry, such that it won't be overwritten by
 *   other different registries of the same `type`
 * @property credentials the credentials to register
 * @property matcher the matcher wasm binary in bytes; the matcher will be interpreted and run in a
 *   safe and privacy-preserving sandbox upon an incoming request and it should output the qualified
 *   credentials given the [credentials] and the request; an invalid matcher (e.g. one that fails
 *   wasm interpretation or causes exceptions) will mean that your credentials will never be
 *   surfaced to the user
 * @property intentAction the intent action that will be used to launch your fulfillment activity
 *   when one of your credentials was chosen by the user, default to
 *   [RegistryManager.ACTION_GET_CREDENTIAL] when unspecified; when Credential Manager launches your
 *   fulfillment activity, it will build an intent with the given `intentAction` targeting your
 *   package, so this is useful when you need to define different fulfillment activities for
 *   different registries
 * @constructor
 * @throws IllegalArgumentException if [id] or [intentAction] length is greater than 64 characters
 */
public abstract class RegisterCredentialsRequest
@JvmOverloads
constructor(
    public val type: String,
    public val id: String,
    public val credentials: ByteArray,
    public val matcher: ByteArray,
    public val intentAction: String = RegistryManager.ACTION_GET_CREDENTIAL,
) {
    init {
        require(id.length <= 64) { "`id` length must be less than 64" }
        require(intentAction.length <= 64) { "`intentAction` length must be less than 64" }
    }
}
