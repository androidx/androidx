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

package androidx.benchmark.integration.macrobenchmark

import androidx.benchmark.macro.getInstalledPackageInfo
import androidx.benchmark.macro.isSystemApp
import androidx.test.filters.MediumTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

@MediumTest
class SystemAppTest {

    @Test
    fun targetIsNotASystemApp() {
        val applicationInfo = getInstalledPackageInfo(BENCHMARK_TARGET)
        assertFalse(applicationInfo.isSystemApp())
    }

    @Test
    fun sysUiIsASystemApp() {
        val applicationInfo = getInstalledPackageInfo(SYSTEM_UI)
        assertTrue(applicationInfo.isSystemApp())
    }

    companion object {
        // Macrobenchmark target package
        private const val BENCHMARK_TARGET = "androidx.benchmark.integration.macrobenchmark.target"

        // System UI
        private const val SYSTEM_UI = "com.android.systemui"
    }
}
