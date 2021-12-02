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

import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

/* ktlint-disable max-line-length */
/**
 * Authorisation code verifier.
 *
 * Related specifications:
 * [Proof Key for Code Exchange by OAuth Public Clients (RFC 7636).](https://tools.ietf.org/html/rfc7636)
 */
/* ktlint-enable max-line-length */
@RequiresApi(Build.VERSION_CODES.O)
public class CodeVerifier {
    private companion object {
        /**
         * The minimum byte length of a code verifier.
         */
        private const val MIN_LENGTH_BYTE = 32

        /**
         * The maximum character length of a code verifier.
         */
        private const val MAX_LENGTH_BYTE = 96

        /**
         * The minimum character length of a code verifier with base64url-encoded.
         */
        private const val MIN_LENGTH_BASE64URL = 43

        /**
         * The maximum character length of a code verifier with base64url-encoded.
         */
        private const val MAX_LENGTH_BASE64URL = 128

        /**
         * The secure random generator.
         */
        private val SECURE_RANDOM: SecureRandom = SecureRandom()
    }

    /**
     * The verifier value.
     */
    public val value: String

    @JvmOverloads
    public constructor(
        /**
         * It is RECOMMENDED that the output of a suitable random number generator be used to create
         * a 32-octet sequence. The octet sequence is then base64url-encoded to produce a
         * 43-octet URL safe string to use as the code verifier.
         */
        byteLength: Int = 32
    ) {
        /**
         * Generates a new code verifier with a cryptographic random value of the specified byte
         * length, Base64URL-encoded
         */
        require((byteLength >= MIN_LENGTH_BYTE) and (byteLength <= MAX_LENGTH_BYTE)) {
            "code verifier for PKCE must has a minimum length of $MIN_LENGTH_BASE64URL characters" +
                " and a maximum length of $MAX_LENGTH_BASE64URL characters, please generate " +
                "the code verifier with byte length between $MIN_LENGTH_BYTE and $MAX_LENGTH_BYTE"
        }

        val randomBytes = ByteArray(byteLength)

        SECURE_RANDOM.nextBytes(randomBytes)

        value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    public constructor(value: String) {
        this.value = value
    }

    internal fun getValueBytes(): ByteArray {
        return value.toByteArray(StandardCharsets.UTF_8)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CodeVerifier) {
            return other.value == value
        }
        return false
    }

    override fun hashCode(): Int {
        return getValueBytes().contentHashCode()
    }
}