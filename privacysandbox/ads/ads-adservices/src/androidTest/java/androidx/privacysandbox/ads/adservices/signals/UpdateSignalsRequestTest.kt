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

package androidx.privacysandbox.ads.adservices.signals

import android.net.Uri
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFeatures.Ext12OptIn::class)
@SdkSuppress(minSdkVersion = 34)
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 12)
class UpdateSignalsRequestTest {
    @Test
    fun testToString() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", AdServicesInfo.adServicesVersion() >= 12)
        val result = "UpdateSignalsRequest: updateUri=example.com"
        val request = UpdateSignalsRequest(Uri.parse("example.com"))
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testToString_emptyUri() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", AdServicesInfo.adServicesVersion() >= 12)
        val result = "UpdateSignalsRequest: updateUri="
        val request = UpdateSignalsRequest(Uri.parse(""))
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals_equalRequests() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", AdServicesInfo.adServicesVersion() >= 12)
        val request1 = UpdateSignalsRequest(Uri.parse("example.com"))
        val request2 = UpdateSignalsRequest(Uri.parse("example.com"))
        Truth.assertThat(request1).isEqualTo(request2)
    }

    @Test
    fun testEquals_UnequalRequests() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", AdServicesInfo.adServicesVersion() >= 12)
        val request1 = UpdateSignalsRequest(Uri.parse("example1.com"))
        val request2 = UpdateSignalsRequest(Uri.parse("example2.com"))
        Truth.assertThat(request1).isNotEqualTo(request2)
    }

    @Test
    fun testHashCode() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", AdServicesInfo.adServicesVersion() >= 12)
        val request1 = UpdateSignalsRequest(Uri.parse("example.com"))
        val request2 = UpdateSignalsRequest(Uri.parse("example.com"))
        Truth.assertThat(request1.hashCode() == request2.hashCode())
    }
}
