/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime

import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpatialApiVersionHelperTest {

    @After
    fun tearDown() {
        // Clean up the static state of the fake provider after each test to ensure isolation.
        FakeSpatialApiVersionProvider.testSpatialApiVersion = null
        FakeSpatialApiVersionProvider.testPreviewSpatialApiVersion = null
    }

    @Test
    fun spatialApiVersion_returnsVersionFromServiceProvider() {
        FakeSpatialApiVersionProvider.testSpatialApiVersion = 99
        assertThat(SpatialApiVersionHelper.spatialApiVersion).isEqualTo(99)
    }

    @Test
    fun previewSpatialApiVersion_returnsVersionFromServiceProvider() {
        FakeSpatialApiVersionProvider.testPreviewSpatialApiVersion = 199
        assertThat(SpatialApiVersionHelper.previewSpatialApiVersion).isEqualTo(199)
    }

    @Test
    fun spatialApiVersion_whenNotSet_returnsLatestStableApiLevel() {
        assertThat(SpatialApiVersionHelper.spatialApiVersion)
            .isEqualTo(SpatialApiVersions.LATEST_STABLE_API_LEVEL)
    }

    @Test
    fun previewSpatialApiVersion_whenNotSet_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { SpatialApiVersionHelper.previewSpatialApiVersion }
    }
}
