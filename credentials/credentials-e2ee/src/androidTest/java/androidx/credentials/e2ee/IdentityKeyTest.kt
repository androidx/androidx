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

import android.util.Base64
import com.google.common.io.BaseEncoding
import com.google.common.truth.Truth.assertThat
import java.util.Random
import org.junit.Test

class IdentityKeyTest {

    var sRandom: Random = Random()

    private fun randBytes(numBytes: Int): ByteArray {
        val bytes = ByteArray(numBytes)
        sRandom.nextBytes(bytes)
        return bytes
    }

    private fun hexEncode(bytes: ByteArray): String {
        return BaseEncoding.base16().lowerCase().encode(bytes)
    }

    @Test
    fun identityKeyWithFixedInputs_mustProduceExpectedOutput() {
        val prf = ByteArray(32)
        val salt = ByteArray(32)
        // with an all-zero PRF and salt, this is the expected key
        val expectedPrivKeyHex = "df7204546f1bee78b85324a7898ca119b387e01386d1aef037781d4a8a036aee"
        val expectedPubKeyHex = "ba33d523fd7bf0d06ce9298c3440be1bea3748c6270ae3e07ae8ea19abb8ed23"

        val identityKey =
            IdentityKey.createFromPrf(prf, salt, IdentityKey.IDENTITY_KEY_TYPE_ED25519)

        assertThat(identityKey.private).isNotNull()
        assertThat(identityKey.public).isNotNull()
        assertThat(hexEncode(identityKey.private)).isEqualTo(expectedPrivKeyHex)
        assertThat(hexEncode(identityKey.public)).isEqualTo(expectedPubKeyHex)
    }

    @Test
    fun identityKeyWithoutSalt_mustBeIdenticalToEmptySalt() {
        for (i in 1..10) {
            val prf = randBytes(32)
            val identityKey =
                IdentityKey.createFromPrf(
                    prf,
                    /* salt= */ null,
                    IdentityKey.IDENTITY_KEY_TYPE_ED25519
                )
            val identityKey2 =
                IdentityKey.createFromPrf(prf, ByteArray(32), IdentityKey.IDENTITY_KEY_TYPE_ED25519)

            assertThat(identityKey).isEqualTo(identityKey2)
        }
    }

    @Test
    fun identityKey_canBeGeneratedUsingWebAuthnPrfOutput() {
        /*
        Ideally, we would test the full webauthn interaction (set the PRF extension to true, call
        navigator.credentials.create, read the PRF output). The problem is that this would tie
        androidX to the implementation of a password manager.
        Instead, we manually copy the prfOutput value from //com/google/android/gms/fido/authenticator/embedded/AuthenticationRequestHandlerTest.java,
        like a test vector. Even if the two values get out of sync, what we care about is the Base64
        format, as the PRF output is fully random-looking by definition.
         */
        val prfOutput =
            Base64.decode(
                "f2HM0TolWHyYJ/+LQDW8N2vRdE0+risMV/tIKXQdj7tVKdGChdJuMyz1" +
                    "/iX7x4y3GvHLlmja1A8qCsKsekW22Q==",
                Base64.DEFAULT
            )
        val salt = ByteArray(32)
        val expectedPrivKeyHex = "bccdec572ae1be6b3c3f3473781965a1935d2614c928f5430b79188950658ad6"
        val expectedPubKeyHex = "23fa91da0af9edefae9c53c584f933f3d02f934aebddb70511adac91f255afda"

        val identityKey =
            IdentityKey.createFromPrf(prfOutput, salt, IdentityKey.IDENTITY_KEY_TYPE_ED25519)

        assertThat(prfOutput).isNotNull()
        assertThat(identityKey.private).isNotNull()
        assertThat(identityKey.public).isNotNull()
        assertThat(hexEncode(identityKey.private)).isEqualTo(expectedPrivKeyHex)
        assertThat(hexEncode(identityKey.public)).isEqualTo(expectedPubKeyHex)
    }
}
