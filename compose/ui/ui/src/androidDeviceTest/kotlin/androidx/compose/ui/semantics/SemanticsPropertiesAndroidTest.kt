/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.semantics

import android.credentials.GetCredentialException
import android.credentials.GetCredentialRequest
import android.credentials.GetCredentialResponse
import android.os.Bundle
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
class SemanticsPropertiesAndroidTest {

    private fun createGetCredentialRequest(): GetCredentialRequest {
        val option =
            android.credentials.CredentialOption.Builder(
                    "com.example.credential.TYPE",
                    Bundle(),
                    Bundle(),
                )
                .build()
        return GetCredentialRequest.Builder(Bundle()).addCredentialOption(option).build()
    }

    @Test
    fun credentialRequestProperty_holdsCorrectData() {
        val platformRequest = createGetCredentialRequest()
        val callback =
            object : OutcomeReceiver<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {}

                override fun onError(error: GetCredentialException) {}
            }
        val expectedData = CredentialRequestData(platformRequest, callback)

        val config = SemanticsConfiguration().apply { this.credentialRequest = expectedData }

        val actualData = config.getOrNull(SemanticsPropertiesAndroid.CredentialRequest)
        assertThat(actualData).isSameInstanceAs(expectedData)
        assertThat(actualData?.request).isSameInstanceAs(platformRequest)
        assertThat(actualData?.callback).isSameInstanceAs(callback)
    }
}
