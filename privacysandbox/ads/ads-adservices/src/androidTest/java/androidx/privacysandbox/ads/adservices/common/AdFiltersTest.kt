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

package androidx.privacysandbox.ads.adservices.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext8OptIn::class)
@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class AdFiltersTest {
    private val frequencyCapFilters: FrequencyCapFilters = FrequencyCapFilters(
        listOf(KeyedFrequencyCap(1, 3, Duration.ofSeconds(1))),
        listOf(KeyedFrequencyCap(2, 4, Duration.ofSeconds(2))),
        listOf(KeyedFrequencyCap(3, 3, Duration.ofSeconds(3))),
        listOf(KeyedFrequencyCap(4, 4, Duration.ofSeconds(4)),
            KeyedFrequencyCap(5, 3, Duration.ofSeconds(5)),
            KeyedFrequencyCap(6, 4, Duration.ofSeconds(6))))

    @Test
    fun testToString() {
        val result = "AdFilters: frequencyCapFilters=$frequencyCapFilters"
        val request = AdFilters(frequencyCapFilters)
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val adFilters1 = AdFilters(frequencyCapFilters)
        var adFilters2 = AdFilters(FrequencyCapFilters(
            listOf(KeyedFrequencyCap(1, 3, Duration.ofSeconds(1))),
            listOf(KeyedFrequencyCap(2, 4, Duration.ofSeconds(2))),
            listOf(KeyedFrequencyCap(3, 3, Duration.ofSeconds(3))),
            listOf(KeyedFrequencyCap(4, 4, Duration.ofSeconds(4)),
                KeyedFrequencyCap(5, 3, Duration.ofSeconds(5)),
                KeyedFrequencyCap(6, 4, Duration.ofSeconds(6)))))
        Truth.assertThat(adFilters1 == adFilters2).isTrue()
    }
}
