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

package androidx.benchmark

import androidx.benchmark.perfetto.PerfettoHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeviceInfoTest {
    @SdkSuppress(minSdkVersion = PerfettoHelper.MIN_SDK_VERSION)
    @Test
    fun misconfiguredForTracing() {
        // NOTE: tests device capability, not implementation of DeviceInfo
        assertFalse(
            DeviceInfo.misconfiguredForTracing,
            "${DeviceInfo.typeLabel} is incorrectly configured for tracing," +
                " and is not CTS compatible. All Perfetto/Atrace capture will fail."
        )
    }
}
