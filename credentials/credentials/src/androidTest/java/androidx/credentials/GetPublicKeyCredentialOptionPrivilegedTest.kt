/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Combines with [GetPublicKeyCredentialOptionPrivilegedFailureInputsTest] for full tests.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class GetPublicKeyCredentialOptionPrivilegedTest {

    @Test
    fun constructor_success() {
        GetPublicKeyCredentialOptionPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RP", "ClientDataHash"
        )
    }

    @Test
    fun constructor_setsAllowHybridToTrueByDefault() {
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged(
            "JSON", "RP", "HASH"
        )
        val allowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid
        assertThat(allowHybridActual).isTrue()
    }

    @Test
    fun constructor_setsAllowHybridFalse() {
        val allowHybridExpected = false
        val getPublicKeyCredentialOptPriv = GetPublicKeyCredentialOptionPrivileged(
            "JSON", "RP", "HASH", allowHybridExpected
        )
        val getAllowHybridActual = getPublicKeyCredentialOptPriv.allowHybrid
        assertThat(getAllowHybridActual).isEqualTo(allowHybridExpected)
    }

    @Test
    fun builder_build_nonDefaultAllowHybrid_false() {
        val allowHybridExpected = false
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged
            .Builder(
                "testJson",
                "RP", "Hash",
            ).setAllowHybrid(allowHybridExpected).build()
        val getAllowHybridActual = getPublicKeyCredentialOptionPrivileged.allowHybrid
        assertThat(getAllowHybridActual).isEqualTo(allowHybridExpected)
    }

    @Test
    fun builder_build_defaultAllowHybrid_true() {
        val defaultPrivilegedRequest = GetPublicKeyCredentialOptionPrivileged.Builder(
            "{\"Data\":5}",
            "RP", "HASH"
        ).build()
        assertThat(defaultPrivilegedRequest.allowHybrid).isTrue()
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val getPublicKeyCredentialOptionPrivileged =
            GetPublicKeyCredentialOptionPrivileged(testJsonExpected, "RP", "HASH")
        val testJsonActual = getPublicKeyCredentialOptionPrivileged.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_rp_success() {
        val testRpExpected = "RP"
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            testRpExpected, "X342%4dfd7&"
        )
        val testRpActual = getPublicKeyCredentialOptionPrivileged.rp
        assertThat(testRpActual).isEqualTo(testRpExpected)
    }

    @Test
    fun getter_clientDataHash_success() {
        val clientDataHashExpected = "X342%4dfd7&"
        val getPublicKeyCredentialOptionPrivileged = GetPublicKeyCredentialOptionPrivileged(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "RP", clientDataHashExpected
        )
        val clientDataHashActual = getPublicKeyCredentialOptionPrivileged.clientDataHash
        assertThat(clientDataHashActual).isEqualTo(clientDataHashExpected)
    }
}