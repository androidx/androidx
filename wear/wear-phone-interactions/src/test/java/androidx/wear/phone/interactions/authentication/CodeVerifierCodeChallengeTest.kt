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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Unit tests for [CodeVerifier] and [CodeChallenge] */
public class CodeVerifierCodeChallengeTest {
    @Test
    public fun testVerifierDefaultConstructor() {
        val verifier = CodeVerifier()
        assertEquals(43, verifier.value.length)
    }

    @Test
    public fun testVerifierConstructor() {
        val verifier = CodeVerifier(96)
        assertEquals(128, verifier.value.length)
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
        assertTrue(verifier.equals(CodeVerifier(verifier.value)))
    }

    @Test
    public fun testVerifierInequality() {
        assertFalse(CodeVerifier().equals(CodeVerifier()))
        assertFalse(CodeVerifier(50).equals(CodeVerifier(50)))
        assertFalse(CodeVerifier().equals(null))
    }

    @Test
    public fun testChallengeTransformS256() {
        // see https://tools.ietf.org/html/rfc7636#appendix-A
        val verifier = CodeVerifier("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        val challenge = CodeChallenge(verifier)
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge.value)
    }

    @Test
    public fun testChallengeEquality() {
        val verifierValue = "jdshfkshg-8973834_SDFSSGE"
        assertTrue(
            CodeChallenge(CodeVerifier(verifierValue)).equals(
                CodeChallenge(CodeVerifier(verifierValue))
            )
        )
    }

    @Test
    public fun testChallengeInequality() {
        assertFalse(CodeChallenge(CodeVerifier()).equals(CodeChallenge(CodeVerifier())))
        assertFalse(CodeChallenge(CodeVerifier()).equals(null))
    }
}