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

import android.content.Context
import android.content.pm.SigningInfo
import android.os.Bundle
import androidx.credentials.assertEquals
import androidx.credentials.equals
import androidx.credentials.getTestCallingAppInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginCreatePasswordRequestTest {
    private val mContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun constructor_success() {
        BeginCreatePasswordCredentialRequest(getTestCallingAppInfo(mContext), Bundle())
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun getter_callingAppInfo() {
        val expectedCandidateQueryBundle = Bundle()
        expectedCandidateQueryBundle.putString("key", "value")
        val expectedPackageName = "sample_package_name"
        val expectedSigningInfo = SigningInfo()
        val expectedCallingAppInfo = CallingAppInfo(expectedPackageName, expectedSigningInfo)

        val request =
            BeginCreatePasswordCredentialRequest(
                expectedCallingAppInfo,
                expectedCandidateQueryBundle
            )

        equals(request.candidateQueryData, expectedCandidateQueryBundle)
        assertThat(request.callingAppInfo).isEqualTo(expectedCallingAppInfo)
    }

    @Test
    fun constructor_createFrom_success() {
        BeginCreatePasswordCredentialRequest.createFrom(
            Bundle(),
            getTestCallingAppInfo(mContext),
        )
    }

    @Test
    fun constructor_createFrom_noCallingAppInfo_success() {
        BeginCreatePasswordCredentialRequest.createFrom(
            Bundle(),
            null,
        )
    }

    @Test
    fun bundleConversion_success() {
        val expected =
            BeginCreatePasswordCredentialRequest(
                getTestCallingAppInfo(mContext, "origin"),
                Bundle().apply {
                    putBoolean("test1", true)
                    putBundle("test2", Bundle())
                    putString("test3", "test")
                }
            )

        val actual =
            BeginCreateCredentialRequest.fromBundle(BeginCreateCredentialRequest.asBundle(expected))

        assertThat(actual).isInstanceOf(BeginCreatePasswordCredentialRequest::class.java)
        assertEquals(actual!!, expected)
    }

    @Test
    fun bundleConversion_noCallingAppInfo_success() {
        val expected =
            BeginCreatePasswordCredentialRequest(
                null,
                Bundle().apply {
                    putBoolean("test1", true)
                    putBundle("test2", Bundle())
                    putString("test3", "test")
                }
            )

        val actual =
            BeginCreateCredentialRequest.fromBundle(BeginCreateCredentialRequest.asBundle(expected))

        assertThat(actual).isInstanceOf(BeginCreatePasswordCredentialRequest::class.java)
        assertEquals(actual!!, expected)
    }

    @Test
    fun bundleConversion_emptyBundle_returnsNull() {
        val actual = BeginCreateCredentialRequest.fromBundle(Bundle())

        assertThat(actual).isNull()
    }
}
