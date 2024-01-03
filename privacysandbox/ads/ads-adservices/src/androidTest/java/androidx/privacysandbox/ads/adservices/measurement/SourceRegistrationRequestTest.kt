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

package androidx.privacysandbox.ads.adservices.measurement

import android.net.Uri
import android.view.InputEvent
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@ExperimentalFeatures.RegisterSourceOptIn
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class SourceRegistrationRequestTest {
    @Test
    fun testToString() {
        val result = "AppSourcesRegistrationRequest { RegistrationUris=" +
            "[[www.abc.com, www.xyz.com]], InputEvent=null }"

        val uri1 = Uri.parse("www.abc.com")
        val uri2 = Uri.parse("www.xyz.com")
        val params = listOf(uri1, uri2)
        val request = SourceRegistrationRequest.Builder(params).build()
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val uri1 = Uri.parse("www.abc.com")
        val uri2 = Uri.parse("www.xyz.com")
        val params = listOf(uri1, uri2)
        val inputEvent = mock(InputEvent::class.java)
        val request1 = SourceRegistrationRequest.Builder(params)
            .setInputEvent(inputEvent)
            .build()
        val request2 = SourceRegistrationRequest(params, inputEvent)
        val request3 = SourceRegistrationRequest.Builder(params).build()

        Truth.assertThat(request1 == request2).isTrue()
        Truth.assertThat(request1 != request3).isTrue()
    }
}
