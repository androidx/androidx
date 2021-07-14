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
import java.security.MessageDigest
import java.util.Base64

/* ktlint-disable max-line-length */
/**
 * Authorization code challenge.
 *
 * Related specifications:
 * [Proof Key for Code Exchange by OAuth Public Clients (RFC 7636)](https://tools.ietf.org/html/rfc7636)
 */
/* ktlint-enable max-line-length */
@RequiresApi(Build.VERSION_CODES.O)
public class CodeChallenge constructor(
    codeVerifier: CodeVerifier
) {
    /**
     * The challenge value.
     */
    public val value: String

    /**
     * Computes the code challenge value using the specified verifier with SHA-256.
     */
    init {
        val md = MessageDigest.getInstance("SHA-256")
        val hash: ByteArray = md.digest(codeVerifier.getValueBytes())
        value = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CodeChallenge) {
            return other.value == value
        }
        return false
    }

    override fun hashCode(): Int {
        return value.toByteArray(StandardCharsets.UTF_8).contentHashCode()
    }
}