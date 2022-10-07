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
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Note that "PublicKeyCredential" and "Passkey" are used interchangeably.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class PublicKeyCredentialTest {

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) { PublicKeyCredential("") }
    }

    @Test
    fun constructor_success() {
        PublicKeyCredential(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        )
    }

    @Test
    fun getter_authJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val publicKeyCredential = PublicKeyCredential(testJsonExpected)
        val testJsonActual = publicKeyCredential.authenticationResponseJson
        Truth.assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }
}