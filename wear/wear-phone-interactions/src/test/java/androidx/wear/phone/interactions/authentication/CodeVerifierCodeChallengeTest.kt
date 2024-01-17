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
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config

/** Unit tests for [CodeVerifier] and [CodeChallenge] */
@Config(minSdk = 26)
@RequiresApi(Build.VERSION_CODES.O)
public class CodeVerifierCodeChallengeTest {
    @Test
    public fun testVerifierDefaultConstructor() {
        val verifier = CodeVerifier()
        assertThat(verifier.value.length).isEqualTo(43)
    }

    @Test
    public fun testVerifierConstructor() {
        val verifier = CodeVerifier(96)
        assertThat(verifier.value.length).isEqualTo(128)
    }

    @Test
    public fun testVerifierConstructorInvalidParam() {
        try {
            CodeVerifier(100)
            fail("should fail due to verifier over the required length")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    public fun testVerifierEquality() {
        val verifier = CodeVerifier()
        assertThat(CodeVerifier(verifier.value)).isEqualTo(verifier)
    }

    @Test
    public fun testVerifierInequality() {
        assertThat(CodeVerifier()).isNotEqualTo(CodeVerifier())
        assertThat(CodeVerifier(50)).isNotEqualTo(CodeVerifier(50))
        assertThat(CodeVerifier()).isNotEqualTo(null)
    }

    @Test
    public fun testChallengeTransformS256() {
        // see https://tools.ietf.org/html/rfc7636#appendix-A
        val verifier = CodeVerifier("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        val challenge = CodeChallenge(verifier)
        assertThat(challenge.value).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
    }

    @Test
    public fun testChallengeEquality() {
        val verifierValue = "jdshfkshg-8973834_SDFSSGE"
        assertThat(
            CodeChallenge(CodeVerifier(verifierValue))
        ).isEqualTo(CodeChallenge(CodeVerifier(verifierValue)))
    }

    @Test
    public fun testChallengeInequality() {
        assertThat(CodeChallenge(CodeVerifier())).isNotEqualTo(CodeChallenge(CodeVerifier()))
        assertThat(CodeChallenge(CodeVerifier())).isNotEqualTo(null)
    }
}
