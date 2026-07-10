/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.projected

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.interfaces.Feature
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
@RunWith(AndroidJUnit4::class)
class ProjectedDeviceCapabilityProviderFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private lateinit var projectedDeviceCapabilityProviderFactory:
        ProjectedDeviceCapabilityProviderFactory

    @Before
    fun setUp() {
        projectedDeviceCapabilityProviderFactory = ProjectedDeviceCapabilityProviderFactory()
    }

    @Test
    fun requirements_containsFullStackAndProjected() {
        assertThat(projectedDeviceCapabilityProviderFactory.requirements)
            .containsExactly(Feature.FULLSTACK, Feature.PROJECTED)
    }

    @Test
    fun create_returnsProjectedDeviceCapabilityProviderInstance() {
        val xrDeviceCapabilityProvider =
            projectedDeviceCapabilityProviderFactory.create(context, testDispatcher)

        assertThat(xrDeviceCapabilityProvider)
            .isInstanceOf(ProjectedDeviceCapabilityProvider::class.java)
    }
}
