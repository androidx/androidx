/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import android.os.Build
import androidx.benchmark.junit4.PerfettoTraceRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ProfileInstallBroadcastTest {
    @OptIn(ExperimentalPerfettoCaptureApi::class)
    @get:Rule
    val perfettoTraceRule = PerfettoTraceRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun installProfile() {
        assertNull(ProfileInstallBroadcast.installProfile(Packages.TARGET))
    }

    @Test
    fun skipFileOperation() {
        assertNull(ProfileInstallBroadcast.skipFileOperation(Packages.TARGET, "WRITE_SKIP_FILE"))
        assertNull(ProfileInstallBroadcast.skipFileOperation(Packages.TARGET, "DELETE_SKIP_FILE"))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun saveProfile() {
        assertNull(ProfileInstallBroadcast.saveProfile(Packages.TARGET))
    }

    @Test
    fun dropShaderCache() {
        assertNull(ProfileInstallBroadcast.dropShaderCache(Packages.TARGET))
    }
}
