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

@file:JvmName("BindingTokenUtils")

package androidx.credentials.provider

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.credentials.ExperimentalDigitalCredentialApi

/**
 * A binding token delivered to the credential provider that links the request session between the
 * caller and the user-selected credential provider.
 *
 * @property bindingToken the computed binding token hash bytes
 * @property holderAppId the identifier of the holder application that received the token (the
 *   holder identifier injected into the binding token)
 * @property version the binding token version
 */
@ExperimentalDigitalCredentialApi
public class BindingToken
@JvmOverloads
constructor(
    public val bindingToken: ByteArray,
    public val holderAppId: String,
    @property:BindingTokenVersion public val version: @BindingTokenVersion String = VERSION_PREVIEW,
) {
    /** Supported versions for [BindingToken]. */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(VERSION_PREVIEW)
    public annotation class BindingTokenVersion

    public companion object {
        public const val VERSION_PREVIEW: String = "preview"

        public const val EXTRA_BINDING_TOKEN: String =
            "androidx.credentials.provider.extra.BINDING_TOKEN"
        public const val EXTRA_BINDING_TOKEN_HOLDER_APP_ID: String =
            "androidx.credentials.provider.extra.BINDING_TOKEN_HOLDER_APP_ID"
        public const val EXTRA_BINDING_TOKEN_VERSION: String =
            "androidx.credentials.provider.extra.BINDING_TOKEN_VERSION"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BindingToken) return false
        return bindingToken.contentEquals(other.bindingToken) &&
            holderAppId == other.holderAppId &&
            version == other.version
    }

    override fun hashCode(): Int {
        var result = bindingToken.contentHashCode()
        result = 31 * result + holderAppId.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}

/** Returns the [BindingToken] associated with this request, or `null` if none was provided. */
@ExperimentalDigitalCredentialApi
public fun ProviderCreateCredentialRequest.retrieveBindingToken(): BindingToken? {
    val bundle = this.sourceBundle ?: return null
    val tokenBytes = bundle.getByteArray(BindingToken.EXTRA_BINDING_TOKEN) ?: return null
    val holderAppId =
        bundle.getString(BindingToken.EXTRA_BINDING_TOKEN_HOLDER_APP_ID) ?: return null
    val version =
        bundle.getString(BindingToken.EXTRA_BINDING_TOKEN_VERSION) ?: BindingToken.VERSION_PREVIEW
    return BindingToken(tokenBytes, holderAppId, version)
}
