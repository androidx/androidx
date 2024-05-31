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

package androidx.privacysandbox.ads.adservices.topics

import android.annotation.SuppressLint
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("NewApi")
@SmallTest
@RunWith(AndroidJUnit4::class)
class GetTopicsRequestHelperTest {
    private val mValidAdServicesSdkExt4Version = AdServicesInfo.adServicesVersion() >= 4
    private val mValidAdServicesSdkExt5Version = AdServicesInfo.adServicesVersion() >= 5
    private val mValidAdExtServicesSdkExtVersion = AdServicesInfo.extServicesVersionS() >= 9

    @Test
    fun testRequestWithoutRecordObservation() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            mValidAdServicesSdkExt4Version || mValidAdExtServicesSdkExtVersion
        )

        var request = GetTopicsRequest("sdk1")
        var convertedRequest =
            GetTopicsRequestHelper.convertRequestWithoutRecordObservation(request)

        assertEquals("sdk1", convertedRequest.adsSdkName)
    }

    @Test
    fun testRequestWithRecordObservation() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExt5Version || mValidAdExtServicesSdkExtVersion
        )

        var request = GetTopicsRequest("sdk1", true)
        var convertedRequest = GetTopicsRequestHelper.convertRequestWithRecordObservation(request)

        assertEquals("sdk1", convertedRequest.adsSdkName)
        assertEquals(true, convertedRequest.shouldRecordObservation())
    }
}
