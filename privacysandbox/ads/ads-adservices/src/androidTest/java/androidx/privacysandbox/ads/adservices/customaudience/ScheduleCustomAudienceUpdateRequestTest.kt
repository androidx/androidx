/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.customaudience

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Duration
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext14OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ScheduleCustomAudienceUpdateRequestTest {
    private val updateUri: Uri = Uri.parse("abc.com")
    private val minDelayDuration = Duration.ofMinutes(30)
    private val partialCustomAudienceList = listOf(PartialCustomAudience("partialCa1"))
    private val shouldReplacePendingUpdates = true

    @Test
    fun testToString() {
        val request =
            ScheduleCustomAudienceUpdateRequest(
                updateUri,
                minDelayDuration,
                partialCustomAudienceList,
                shouldReplacePendingUpdates,
            )
        val result =
            "ScheduleCustomAudienceUpdateRequest: updateUri=$updateUri, " +
                "minDelay=$minDelayDuration, partialCustomAudienceList=$partialCustomAudienceList, " +
                "shouldReplacePendingUpdates=$shouldReplacePendingUpdates"
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val request1 =
            ScheduleCustomAudienceUpdateRequest(
                updateUri,
                minDelayDuration,
                partialCustomAudienceList,
                shouldReplacePendingUpdates,
            )
        val request2 =
            ScheduleCustomAudienceUpdateRequest(
                updateUri,
                minDelayDuration,
                partialCustomAudienceList,
                shouldReplacePendingUpdates,
            )
        Truth.assertThat(request1 == request2).isTrue()
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    @Suppress("deprecation") // Suppress deprecated Builder
    @Test
    fun testConvertToAdServices() {
        /* API is not available */
        Assume.assumeTrue(
            "minSdkVersion = API 31 ext 14",
            AdServicesInfo.adServicesVersion() >= 14 || AdServicesInfo.extServicesVersionS() >= 14,
        )

        val scheduleCustomAudienceUpdateRequest =
            ScheduleCustomAudienceUpdateRequest(
                updateUri,
                minDelayDuration,
                partialCustomAudienceList,
                shouldReplacePendingUpdates,
            )
        val result = scheduleCustomAudienceUpdateRequest.convertToAdServices()
        val expected =
            android.adservices.customaudience.ScheduleCustomAudienceUpdateRequest.Builder(
                    updateUri,
                    minDelayDuration,
                    partialCustomAudienceList.map { it.convertToAdServices() },
                )
                .setShouldReplacePendingUpdates(shouldReplacePendingUpdates)
                .build()
        Truth.assertThat(result).isEqualTo(expected)
    }
}
