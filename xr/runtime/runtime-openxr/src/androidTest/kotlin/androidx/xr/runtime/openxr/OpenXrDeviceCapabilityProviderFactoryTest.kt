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

package androidx.xr.runtime.openxr

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.ExperimentalXrDeviceLifecycleApi
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Before

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalXrDeviceLifecycleApi::class)
class OpenXrDeviceCapabilityProviderFactoryTest {

    private lateinit var underTest: OpenXrDeviceCapabilityProviderFactory

    @Before
    fun setUp() {
        underTest = OpenXrDeviceCapabilityProviderFactory()
    }

    @Test
    fun create_returnsOpenXrDeviceCapabilityProvider() = runTest {
        val provider =
            underTest.create(ApplicationProvider.getApplicationContext(), this.coroutineContext)

        assertThat(provider).isInstanceOf(OpenXrDeviceCapabilityProvider::class.java)
    }
}
