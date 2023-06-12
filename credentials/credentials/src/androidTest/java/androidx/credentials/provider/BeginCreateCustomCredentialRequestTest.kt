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

import android.content.pm.SigningInfo
import android.os.Bundle
import androidx.core.os.BuildCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginCreateCustomCredentialRequestTest {

    @Test
    fun constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        BeginCreateCustomCredentialRequest("type", Bundle.EMPTY, null)
    }

    @Test
    fun constructor_emptyType_throws() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        Assert.assertThrows(
            "Expected empty type to throw IAE",
            IllegalArgumentException::class.java
        ) {
            BeginCreateCustomCredentialRequest(
                "", Bundle.EMPTY,
                CallingAppInfo(
                    "package", SigningInfo()
                )
            )
        }
    }

    @Test
    fun getter_type() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedType = "ironman"
        val beginCreateCustomCredentialRequest =
            BeginCreateCustomCredentialRequest(expectedType, Bundle.EMPTY, null)
        val actualType = beginCreateCustomCredentialRequest.type
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
        val beginCreateCustomCredentialRequest =
            BeginCreateCustomCredentialRequest("type", expectedBundle, null)
        val actualBundle = beginCreateCustomCredentialRequest.candidateQueryData
        assertThat(actualBundle.getString(expectedKey)).isEqualTo(expectedValue)
    }
}