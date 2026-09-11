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

package androidx.credentials

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * Options for generating a binding token that links the request session between the caller and the
 * user-selected credential provider during credential creation.
 *
 * @property proofingToken the cryptographic proofing token provided by the issuer while making the
 *   [CreateDigitalCredentialRequest]
 * @property bindingAlgorithm the hashing algorithm to use, either [ALGORITHM_SHA_256] or
 *   [ALGORITHM_SHA_384]; defaults to [ALGORITHM_SHA_256]
 */
@ExperimentalDigitalCredentialApi
class BindingTokenOptions
@JvmOverloads
constructor(
    val proofingToken: ByteArray,
    @property:BindingAlgorithm val bindingAlgorithm: @BindingAlgorithm String = ALGORITHM_SHA_256,
) {
    /** Algorithms supported for computing a binding token. */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(ALGORITHM_SHA_256, ALGORITHM_SHA_384)
    annotation class BindingAlgorithm

    companion object {
        const val ALGORITHM_SHA_256: String = "SHA-256"
        const val ALGORITHM_SHA_384: String = "SHA-384"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BindingTokenOptions) return false
        return proofingToken.contentEquals(other.proofingToken) &&
            bindingAlgorithm == other.bindingAlgorithm
    }

    override fun hashCode(): Int {
        var result = proofingToken.contentHashCode()
        result = 31 * result + bindingAlgorithm.hashCode()
        return result
    }
}
