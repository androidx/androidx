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

package androidx.credentials.provider

import android.os.Bundle
import androidx.core.os.BuildCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BeginGetCredentialOptionTest {

    @Test
    fun constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        BeginGetCredentialOption("id", "type", Bundle.EMPTY)
    }

    @Test
    fun getter_id() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedId = "superman"

        val beginGetCredentialOption = BeginGetCredentialOption(expectedId, "type", Bundle.EMPTY)
        val actualId = beginGetCredentialOption.id

        assertThat(actualId).isEqualTo(expectedId)
    }

    @Test
    fun getter_type() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedType = "superman"

        val beginGetCredentialOption =
            BeginGetCredentialOption("lamborghini", expectedType, Bundle.EMPTY)
        val actualType = beginGetCredentialOption.type

        assertThat(actualType).isEqualTo(expectedType)
    }

    @Test
    fun getter_bundle() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedKey = "query"
        val expectedValue = "data"
        val expectedBundle = Bundle()
        expectedBundle.putString(expectedKey, expectedValue)

        val beginGetCredentialOption =
            BeginGetCredentialOption("lamborghini", "hurracan", expectedBundle)
        val actualBundle = beginGetCredentialOption.candidateQueryData

        assertThat(actualBundle.getString(expectedKey)).isEqualTo(expectedValue)
    }
}