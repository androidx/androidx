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

package androidx.privacysandbox.ads.adservices.measurement

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class WebSourceRegistrationRequestTest {
    @Test
    fun testToString() {
        val result = "WebSourceRegistrationRequest { WebSourceParams=" +
            "[[WebSourceParams { RegistrationUri=www.abc.com, DebugKeyAllowed=false }]], " +
            "TopOriginUri=www.abc.com, InputEvent=null, AppDestination=null, WebDestination=null," +
            " VerifiedDestination=null }"

        val uri = Uri.parse("www.abc.com")
        val params = listOf(WebSourceParams(uri, false))
        val request = WebSourceRegistrationRequest.Builder(params, uri).build()
        Truth.assertThat(request.toString()).isEqualTo(result)

        val result2 = "WebSourceRegistrationRequest { WebSourceParams=[[WebSourceParams " +
            "{ RegistrationUri=www.abc.com, DebugKeyAllowed=false }]], TopOriginUri=www.abc.com, " +
            "InputEvent=null, AppDestination=www.abc.com, WebDestination=www.abc.com, " +
            "VerifiedDestination=www.abc.com }"

        val params2 = listOf(WebSourceParams(uri, false))
        val request2 = WebSourceRegistrationRequest.Builder(params2, uri)
            .setWebDestination(uri)
            .setAppDestination(uri)
            .setVerifiedDestination(uri)
            .build()
        Truth.assertThat(request2.toString()).isEqualTo(result2)
    }

    @Test
    fun testEquals() {
        val uri = Uri.parse("www.abc.com")

        val params = listOf(WebSourceParams(uri, false))
        val request1 = WebSourceRegistrationRequest.Builder(params, uri)
            .setWebDestination(uri)
            .setAppDestination(uri)
            .setVerifiedDestination(uri)
            .build()
        val request2 = WebSourceRegistrationRequest(
            params,
            uri,
            null,
            uri,
            uri,
            uri)
        val request3 = WebSourceRegistrationRequest.Builder(params, uri).build()

        Truth.assertThat(request1 == request2).isTrue()
        Truth.assertThat(request1 != request3).isTrue()
    }
}
