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

package androidx.privacysandbox.ads.adservices.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class AdDataTest {
    private val uri: Uri = Uri.parse("abc.com")
    private val metadata = "metadata"
    private val adCounterKeys: Set<Int> = setOf<Int>(1, 2, 3)
    private val adFilters: AdFilters = AdFilters(FrequencyCapFilters(
        listOf(KeyedFrequencyCap(1, 3, Duration.ofSeconds(1))),
        listOf(KeyedFrequencyCap(2, 4, Duration.ofSeconds(2))),
        listOf(KeyedFrequencyCap(3, 3, Duration.ofSeconds(3))),
        listOf(KeyedFrequencyCap(4, 4, Duration.ofSeconds(4)),
            KeyedFrequencyCap(5, 3, Duration.ofSeconds(5)),
            KeyedFrequencyCap(6, 4, Duration.ofSeconds(6)))))
    private val adRenderId: String = "ad-render-id"

    @Test
    fun testToString() {
        val result = "AdData: renderUri=$uri, metadata='$metadata', " +
            "adCounterKeys=$adCounterKeys, adFilters=$adFilters, adRenderId=$adRenderId"
        val request = AdData(uri, metadata, adCounterKeys, adFilters, adRenderId)
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val adData1 = AdData(uri, metadata, adCounterKeys, adFilters, adRenderId)
        var adData2 = AdData(Uri.parse("abc.com"), "metadata", setOf<Int>(1, 2, 3),
            AdFilters(FrequencyCapFilters(
                listOf(KeyedFrequencyCap(1, 3, Duration.ofSeconds(1))),
                listOf(KeyedFrequencyCap(2, 4, Duration.ofSeconds(2))),
                listOf(KeyedFrequencyCap(3, 3, Duration.ofSeconds(3))),
                listOf(KeyedFrequencyCap(4, 4, Duration.ofSeconds(4)),
                    KeyedFrequencyCap(5, 3, Duration.ofSeconds(5)),
                    KeyedFrequencyCap(6, 4, Duration.ofSeconds(6))))),
            "ad-render-id")
        Truth.assertThat(adData1 == adData2).isTrue()
    }
}
