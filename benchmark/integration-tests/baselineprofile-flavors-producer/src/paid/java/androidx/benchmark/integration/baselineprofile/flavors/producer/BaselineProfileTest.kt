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

package androidx.benchmark.integration.baselineprofile.flavors.producer

import android.content.Intent
import android.os.Build
import androidx.benchmark.DeviceInfo
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 29)
class BaselineProfileTest {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun startupBaselineProfile() {
        assumeTrue(DeviceInfo.isRooted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)

        // Collects the baseline profile
        baselineRule.collect(
            packageName = PACKAGE_NAME,
            profileBlock = {
                startActivityAndWait(Intent(ACTION))
                device.waitForIdle()
            }
        )
    }

    companion object {
        private const val PACKAGE_NAME =
            "androidx.benchmark.integration.baselineprofile.flavors.consumer.paid"
        private const val ACTION =
            "androidx.benchmark.integration.baselineprofile.flavors.consumer.paid.EMPTY_ACTIVITY"
    }
}
