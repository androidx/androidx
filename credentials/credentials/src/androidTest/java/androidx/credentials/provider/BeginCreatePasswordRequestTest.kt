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

import android.content.pm.SigningInfo
import android.os.Bundle
import android.service.credentials.CallingAppInfo
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.credentials.equals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(34)
class BeginCreatePasswordRequestTest {
    @Test
    fun constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        BeginCreatePasswordCredentialRequest(
            CallingAppInfo(
                "sample_package_name",
                SigningInfo()
            ),
            Bundle()
        )
    }

    @Test
    fun getter_callingAppInfo() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        val expectedCandidateQueryBundle = Bundle()
        expectedCandidateQueryBundle.putString("key", "value")
        val expectedPackageName = "sample_package_name"
        val expectedSigningInfo = SigningInfo()
        val expectedCallingAppInfo = CallingAppInfo(
            expectedPackageName,
            expectedSigningInfo
        )

        val request = BeginCreatePasswordCredentialRequest(
            expectedCallingAppInfo, expectedCandidateQueryBundle)

        equals(request.candidateQueryData, expectedCandidateQueryBundle)
        assertThat(request.callingAppInfo?.packageName).isEqualTo(expectedPackageName)
        assertThat(request.callingAppInfo?.signingInfo).isEqualTo(expectedSigningInfo)
    }

    // TODO ("Add framework conversion, createFrom tests")
}