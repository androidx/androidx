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

package androidx.privacysandbox.ads.adservices.adid

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AdIdTest {
    @Test
    fun testToString() {
        val result = "AdId: adId=1234, isLimitAdTrackingEnabled=false"
        val adId = AdId("1234", false)
        Truth.assertThat(adId.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val adId1 = AdId("1234", false)
        val adId2 = AdId("1234", false)
        Truth.assertThat(adId1 == adId2).isTrue()

        val adId3 = AdId("1234", true)
        Truth.assertThat(adId1 == adId3).isFalse()
    }
}
