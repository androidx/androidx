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

package androidx.credentials.e2ee

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Hkdf

/**
 * A public-private key pair usable for signing, representing an end user identity in an end-to-end
 * encrypted messaging system.
 *
 * @property public The public key, stored as a byte array.
 * @property private The private key, stored as a byte array.
 * @property type The type of signing key, e.g. Ed25519.
 */
class IdentityKey
private constructor(val public: ByteArray, val private: ByteArray, @IdentityKeyType val type: Int) {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(IDENTITY_KEY_TYPE_RESERVED, IDENTITY_KEY_TYPE_ED25519)
    annotation class IdentityKeyType

    companion object {
        /**
         * The default signing key type, which should not be used. This is required to match
         * https://www.iana.org/assignments/cose/cose.xhtml#algorithms
         */
        const val IDENTITY_KEY_TYPE_RESERVED = 0

        /**
         * A signing key on Ed25519. The value matches
         * https://www.iana.org/assignments/cose/cose.xhtml#algorithms
         */
        const val IDENTITY_KEY_TYPE_ED25519 = 6

        /**
         * Creates a [IdentityKey], a public/private key pair usable for signing. It is intended for
         * use with the WebAuthn PRF extension (https://w3c.github.io/webauthn/#prf-extension). The
         * generated IdentityKey is deterministic given prf and salt, thus the prf value must be
         * kept secret. Currently, only Ed25519 is supported as a key type.
         *
         * @param prf The PRF output of WebAuthn used in the key derivation.
         * @param salt An optional salt used in the key derivation.
         * @param keyType The type of IdentityKey to generate, e.g. Ed25519.
         * @return a [IdentityKey], a public/private key pair usable for signing.
         * @throws IllegalArgumentException if the key type is not supported.
         */
        @JvmStatic
        @WorkerThread
        fun createFromPrf(
            prf: ByteArray,
            salt: ByteArray?,
            @IdentityKeyType keyType: Int
        ): IdentityKey {
            if (keyType != IDENTITY_KEY_TYPE_ED25519) {
                throw IllegalArgumentException("Only Ed25519 is supported at this stage.")
            }

            val hkdf: ByteArray =
                Hkdf.computeHkdf(
                    "HmacSHA256",
                    prf,
                    // According to RFC 5869, Section 2.2 the salt is optional. If no salt is
                    // provided, the HKDF uses a salt that is an array of zeros of the same length
                    // as the hash digest.
                    /* salt= */ salt ?: ByteArray(32),
                    /* info= */ ByteArray(0),
                    /* size= */ 32
                )
            val keyPair: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPairFromSeed(hkdf)
            return IdentityKey(keyPair.publicKey, keyPair.privateKey, IDENTITY_KEY_TYPE_ED25519)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is IdentityKey) return false
        if (
            type != other.type ||
                !private.contentEquals(other.private) ||
                !public.contentEquals(other.public)
        )
            return false
        return true
    }

    override fun hashCode(): Int {
        var result = public.contentHashCode()
        result = 31 * result + private.contentHashCode()
        result = 31 * result + type
        return result
    }
}
