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
package androidx.credentials.provider

import android.os.Bundle
import androidx.credentials.equals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
@SmallTest
class BeginGetCustomCredentialOptionTest {
    @Test
    fun constructor_success() {
        val expectedBundle = Bundle()
        expectedBundle.putString("random", "random_value")
        val expectedType = "type"
        val expectedId = "id"
        val option = BeginGetCustomCredentialOption(
            expectedId, expectedType, expectedBundle
        )
        Truth.assertThat(option.type).isEqualTo(expectedType)
        Truth.assertThat(option.id).isEqualTo(expectedId)
        Truth.assertThat(equals(option.candidateQueryData, expectedBundle)).isTrue()
    }

    @Test
    fun constructor_emptyType_throwsIAE() {
        assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            BeginGetCustomCredentialOption(
                "id",
                "",
                Bundle()
            )
        }
    }

    @Test
    fun constructor_emptyId_throwsIAE() {
        assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            BeginGetCustomCredentialOption(
                "",
                "type",
                Bundle()
            )
        }
    }
}
