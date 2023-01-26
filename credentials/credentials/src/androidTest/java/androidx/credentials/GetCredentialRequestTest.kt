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

import com.google.common.truth.Truth.assertThat

import org.junit.Assert.assertThrows

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetCredentialRequestTest {

    @Test
    fun constructor_emptyCredentialOptions_throws() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { GetCredentialRequest(ArrayList()) }
    }

    @Test
    fun constructor() {
        val expectedIsAutoSelectAllowed = true
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption("json"))

        val request = GetCredentialRequest(
            expectedCredentialOptions,
            expectedIsAutoSelectAllowed
        )

        assertThat(request.isAutoSelectAllowed).isEqualTo(expectedIsAutoSelectAllowed)
        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(
                expectedCredentialOptions[i]
            )
        }
    }

    @Test
    fun constructor_defaultAutoSelect() {
        val options = ArrayList<CredentialOption>()
        options.add(GetPasswordOption())

        val request = GetCredentialRequest(options)

        assertThat(request.isAutoSelectAllowed).isFalse()
    }

    @Test
    fun builder_addCredentialOption() {
        val expectedIsAutoSelectAllowed = true
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption("json"))

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(expectedCredentialOptions[0])
            .addCredentialOption(expectedCredentialOptions[1])
            .setAutoSelectAllowed(expectedIsAutoSelectAllowed)
            .build()

        assertThat(request.isAutoSelectAllowed).isEqualTo(expectedIsAutoSelectAllowed)
        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(
                expectedCredentialOptions[i]
            )
        }
    }

    @Test
    fun builder_setCredentialOptions() {
        val expectedIsAutoSelectAllowed = true
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption("json"))

        val request = GetCredentialRequest.Builder()
            .setCredentialOptions(expectedCredentialOptions)
            .setAutoSelectAllowed(expectedIsAutoSelectAllowed)
            .build()

        assertThat(request.isAutoSelectAllowed).isEqualTo(expectedIsAutoSelectAllowed)
        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(
                expectedCredentialOptions[i]
            )
        }
    }

    @Test
    fun builder_defaultAutoSelect() {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPasswordOption())
            .build()

        assertThat(request.isAutoSelectAllowed).isFalse()
    }
}