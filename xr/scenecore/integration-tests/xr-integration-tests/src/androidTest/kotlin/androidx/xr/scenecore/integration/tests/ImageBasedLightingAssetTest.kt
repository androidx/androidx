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

package androidx.xr.scenecore.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import java.nio.file.Paths
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device integration tests for loading SceneCore [ImageBasedLightingAsset]s.
 *
 * [ImageBasedLightingAsset.createFromZip] returns a non-null asset or throws, so these tests verify
 * that loading and closing the asset on a real runtime completes without throwing. They
 * intentionally do not request Full Space Mode because asset loading does not depend on the space
 * mode; the visual effect of an applied IBL is covered by `SpatialEnvironmentTest`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ImageBasedLightingAssetTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun createFromZip_withAssetPath_loadsWithoutThrowing() {
        sessionRule.onMainThread {
            ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
                .close()
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun createFromZip_withByteArray_loadsWithoutThrowing() {
        // Read the ~1.7 MB asset on the instrumentation thread to avoid stalling the main Looper.
        val bytes =
            sessionRule.activity.assets.open("skyboxes/BlueSkybox.zip").use { it.readBytes() }

        sessionRule.onMainThread {
            @Suppress("RestrictedApiAndroidX")
            ImageBasedLightingAsset.createFromZip(session, bytes, "BlueSkybox.zip").close()
        }
    }
}
