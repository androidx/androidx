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
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import org.junit.Test
import org.junit.runner.RunWith

/** Device integration tests for loading SceneCore [ImageBasedLightingAsset]s. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ImageBasedLightingAssetTest {

    @Test
    @XrDeviceTest
    fun imageBasedLightingAsset_createFromPathAndBytes_createsAssets() =
        runTestWithSession { activity, session ->
            val iblFromPath =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            try {
                assertThat(iblFromPath).isNotNull()
            } finally {
                iblFromPath.close()
            }

            val bytes = activity.assets.open("skyboxes/BlueSkybox.zip").use { it.readBytes() }

            @Suppress("RestrictedApiAndroidX")
            val iblFromBytes =
                ImageBasedLightingAsset.createFromZip(session, bytes, "BlueSkybox.zip")
            try {
                assertThat(iblFromBytes).isNotNull()
            } finally {
                iblFromBytes.close()
            }
        }
}
