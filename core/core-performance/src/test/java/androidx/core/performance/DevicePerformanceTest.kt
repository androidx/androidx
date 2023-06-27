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

package androidx.core.performance

import android.os.Build.VERSION_CODES.R
import androidx.core.performance.testing.FakeDevicePerformanceSupplier
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for [DevicePerformance]. */
@RunWith(RobolectricTestRunner::class)
class DevicePerformanceTest {

    @Test
    @Config(minSdk = R)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun mediaPerformanceClass() = runTest {
        val fake = FakeDevicePerformanceSupplier(30)
        val pc = DevicePerformance.create(fake)
        assertThat(pc.mediaPerformanceClass).isEqualTo(30)
        assertThat(fake.mediaPerformanceClassFlow.toList()).containsExactly(30)
    }
}
