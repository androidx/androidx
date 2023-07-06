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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginGetCredentialRequestTest {
    @Test
    fun constructor_success() {
        BeginGetCredentialRequest(emptyList(), null)
    }

    @Test
    fun getter_beginGetCredentialOptions() {
        val expectedKey = "query"
        val expectedValue = "data"
        val expectedBundle = Bundle()
        expectedBundle.putString(expectedKey, expectedValue)
        val expectedId = "key"
        val expectedType = "mach-10"
        val expectedBeginGetCredentialOptionsSize = 1

        val beginGetCredentialRequest = BeginGetCredentialRequest(
            listOf(
                BeginGetCustomCredentialOption(expectedId, expectedType, expectedBundle)
            ),
            null
        )
        val actualBeginGetCredentialOptionList = beginGetCredentialRequest.beginGetCredentialOptions
        val actualBeginGetCredentialOptionsSize = actualBeginGetCredentialOptionList.size
        assertThat(actualBeginGetCredentialOptionsSize)
            .isEqualTo(expectedBeginGetCredentialOptionsSize)
        val actualBundleValue = actualBeginGetCredentialOptionList[0].candidateQueryData
            .getString(expectedKey)
        val actualId = actualBeginGetCredentialOptionList[0].id
        val actualType = actualBeginGetCredentialOptionList[0].type

        assertThat(actualBundleValue).isEqualTo(expectedValue)
        assertThat(actualId).isEqualTo(expectedId)
        assertThat(actualType).isEqualTo(expectedType)
    }

    @Test
    fun getter_nullCallingAppInfo() {
        val expectedCallingAppInfo: CallingAppInfo? = null

        val beginGetCredentialRequest = BeginGetCredentialRequest(
            emptyList(),
            expectedCallingAppInfo
        )
        val actualCallingAppInfo = beginGetCredentialRequest.callingAppInfo

        assertThat(actualCallingAppInfo).isEqualTo(expectedCallingAppInfo)
    }

    @Test
    fun getter_nonNullCallingAppInfo() {
        val expectedPackageName = "john.wick.four.credentials"
        val expectedCallingAppInfo = CallingAppInfo(
            expectedPackageName,
            SigningInfo()
        )

        val beginGetCredentialRequest = BeginGetCredentialRequest(
            emptyList(),
            expectedCallingAppInfo
        )
        val actualCallingAppInfo = beginGetCredentialRequest.callingAppInfo
        val actualPackageName = actualCallingAppInfo!!.packageName

        assertThat(actualPackageName).isEqualTo(expectedPackageName)
    }
}
