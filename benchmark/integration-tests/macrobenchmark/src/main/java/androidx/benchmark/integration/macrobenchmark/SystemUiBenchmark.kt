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
    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    @Ignore
    fun baselineProfiles() {
        baselineRule.collect(
            packageName = PACKAGE_NAME,
            profileBlock = {
                pressHome()
                repeat(5) {
                    device.openNotification()
                    pressHome()
                    device.waitForIdle()
                    device.openQuickSettings()
                    pressHome()
                }
                device.waitForIdle()
                Thread.sleep(SLEEP_TIMEOUT)
                pressHome()
            }
        )
    }

    companion object {
        private const val PACKAGE_NAME = "com.android.systemui"
        private const val SLEEP_TIMEOUT = 5000L
    }
}
