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
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class KeyedFrequencyCapTest {
    private val adCounterKey: Int = 1
    private val maxCount: Int = 3
    private val interval: Duration = Duration.ofSeconds(1)

    @Test
    fun testToString() {
        val result =
            "KeyedFrequencyCap: adCounterKey=$adCounterKey, maxCount=$maxCount, " +
                "interval=$interval"
        val request = KeyedFrequencyCap(adCounterKey, maxCount, interval)
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val keyedFrequencyCap1 = KeyedFrequencyCap(adCounterKey, maxCount, interval)
        var keyedFrequencyCap2 = KeyedFrequencyCap(1, 3, Duration.ofSeconds(1))
        Truth.assertThat(keyedFrequencyCap1 == keyedFrequencyCap2).isTrue()
    }
}
