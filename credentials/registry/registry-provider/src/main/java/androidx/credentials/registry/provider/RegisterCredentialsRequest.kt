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

import androidx.annotation.RestrictTo

/**
 * A request to register credentials with Credential Manager.
 *
 * @constructor
 * @property type the type of the credentials being registered
 * @property id the unique id that identifies this registry, such that it won't be overwritten by
 *   other different registries of the same `type`
 * @property credentials the credentials to register
 * @property matcher the matcher wasm binary in bytes; the matcher will be interpreted and run in a
 *   safe and privacy-preserving sandbox upon an incoming request and it should output the qualified
 *   credentials given the [credentials] and the request
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RegisterCredentialsRequest(
    public val type: String,
    public val id: String,
    public val credentials: ByteArray,
    public val matcher: ByteArray,
)
