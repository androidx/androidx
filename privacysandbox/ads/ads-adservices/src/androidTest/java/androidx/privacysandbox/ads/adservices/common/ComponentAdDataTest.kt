/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.common

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext16OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class ComponentAdDataTest {
    private val uri: String = "abc.com"
    private val renderUri: Uri = Uri.parse(uri)
    private val adRenderId: String = "ad-render-id"

    @Test
    fun testToString() {
        val result = "ComponentAdData: renderUri=$renderUri, adRenderId='$adRenderId'"
        val request = ComponentAdData(renderUri, adRenderId)

        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val componentAdData1 = ComponentAdData(renderUri, adRenderId)
        val componentAdData2 = ComponentAdData(Uri.parse(uri), "ad-render-id")

        Truth.assertThat(componentAdData1 == componentAdData2).isTrue()
    }

    @Test
    fun testNotEquals() {
        val componentAdData1 = ComponentAdData(renderUri, adRenderId)
        val componentAdData2 = ComponentAdData(Uri.parse("test.com"), "ad-render-id")

        Truth.assertThat(componentAdData1 == componentAdData2).isFalse()
    }

    @Test
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 16)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 16)
    fun testConversion() {
        /* API is available */
        Assume.assumeTrue(
            "minSdkVersion = API 31 ext 16",
            AdServicesInfo.adServicesVersion() >= 16 || AdServicesInfo.extServicesVersionS() >= 16,
        )

        val componentAdData = ComponentAdData(renderUri, adRenderId)
        val actual = componentAdData.convertToAdServices()
        val expected = android.adservices.common.ComponentAdData(renderUri, adRenderId)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}
