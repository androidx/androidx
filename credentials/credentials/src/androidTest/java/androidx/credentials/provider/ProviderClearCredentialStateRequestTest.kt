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
import android.service.credentials.CallingAppInfo
import androidx.annotation.RequiresApi
import androidx.credentials.equals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderClearCredentialStateRequestTest {

    @RequiresApi(34)
    @Test
    fun testConstructor_success() {
        val callingAppInfo = CallingAppInfo("sample_package_name", SigningInfo())

        val request = ProviderClearCredentialStateRequest(callingAppInfo)

        assertThat(equals(callingAppInfo, request.callingAppInfo)).isTrue()
    }
}