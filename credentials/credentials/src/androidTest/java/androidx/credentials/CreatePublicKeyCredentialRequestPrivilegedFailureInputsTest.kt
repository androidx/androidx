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

import androidx.testutils.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Combines with [CreatePublicKeyCredentialRequestPrivilegedTest] for full tests.
 */
@RunWith(Parameterized::class)
class CreatePublicKeyCredentialRequestPrivilegedFailureInputsTest(
    val requestJson: String,
    val rp: String,
    val clientDataHash: String
    ) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun failureCases(): Collection<Array<Any>> {
            return listOf(
                arrayOf("{\"hi\":21}", "rp", ""),
                arrayOf("{\"hi\":21}", "", "clientDataHash"),
                arrayOf("", "rp", "clientDataHash")
            ) // coverage is complete, null is not a problem in Kotlin.
        }
    }

    @Test
    fun constructor_emptyInput_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            CreatePublicKeyCredentialRequestPrivileged(requestJson, rp, clientDataHash)
        }
    }

    @Test
    fun builder_build_emptyInput_throwsIllegalArgumentException() {
        var builder = CreatePublicKeyCredentialRequestPrivileged.Builder(requestJson,
            rp, clientDataHash)
        assertThrows<IllegalArgumentException> {
            builder.build()
        }
    }
}