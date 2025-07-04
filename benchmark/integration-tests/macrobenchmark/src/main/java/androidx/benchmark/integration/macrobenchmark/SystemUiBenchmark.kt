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

package androidx.benchmark.integration.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 29)
class SystemUiBenchmark {
    @get:Rule val baselineRule = BaselineProfileRule()

    @Test
    @Ignore("Suppressed for CI runs since killing SystemUi locks the device")
    fun baselineProfiles() {
        baselineRule.collect(
            packageName = PACKAGE_NAME,
            maxIterations = 1,
            stableIterations = 1,
            profileBlock = {
                pressHome()
                device.openNotification()
                pressHome()
                device.openQuickSettings()
                pressHome()
            },
        )
    }

    companion object {
        private const val PACKAGE_NAME = "com.android.systemui"
    }
}
