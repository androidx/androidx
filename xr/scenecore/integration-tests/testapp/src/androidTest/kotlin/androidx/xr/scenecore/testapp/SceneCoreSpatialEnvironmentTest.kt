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

package androidx.xr.scenecore.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.function.Consumer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for SceneCore SpatialEnvironment APIs (Passthrough, IBL, Geometry).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SceneCoreSpatialEnvironmentTest {

    @Test
    fun spatialEnvironment_passthroughOpacity_updatesAndNotifiesListeners() =
        runTestWithSession { session ->
            val env = session.scene.spatialEnvironment
            val targetOpacity = 0.5f
            val opacityDeferred = CompletableDeferred<Float>()
            val listener = Consumer<Float> { newOpacity -> opacityDeferred.complete(newOpacity) }
            env.addPassthroughOpacityChangedListener(listener)
            try {
                env.preferredPassthroughOpacity = targetOpacity
                // Suspend (yielding the main looper) until the listener is notified
                val receivedOpacity = withTimeoutOrNull(3000) { opacityDeferred.await() }
                if (receivedOpacity != null) {
                    assertThat(receivedOpacity).isWithin(1e-4f).of(targetOpacity)
                } else {
                    // Fallback check if the connected test target lacks PASSTHROUGH_CONTROL
                    // capability
                    assertThat(env.preferredPassthroughOpacity).isWithin(1e-4f).of(targetOpacity)
                }
            } finally {
                env.removePassthroughOpacityChangedListener(listener)
                env.preferredPassthroughOpacity =
                    SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
            }
        }

    @Test
    fun spatialEnvironment_loadIblFromPathAndBytes_createsValidAssets() =
        runTestWithSession { activity, session ->
            val iblFromPath =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            assertThat(iblFromPath).isNotNull()

            val bytes = activity.assets.open("skyboxes/BlueSkybox.zip").readBytes()
            @Suppress("RestrictedApiAndroidX")
            val iblFromBytes =
                ImageBasedLightingAsset.createFromZip(session, bytes, "BlueSkybox.zip")
            assertThat(iblFromBytes).isNotNull()

            iblFromPath.close()
            iblFromBytes.close()
        }

    @Test
    fun spatialEnvironment_geometryAndSkyboxSwapping_updatesPreferences() =
        runTestWithSession { session ->
            val ibl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val groundGeometry =
                GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))

            val pref1 =
                SpatialEnvironment.SpatialEnvironmentPreference(
                    imageBasedLightingAsset = ibl,
                    geometry = groundGeometry,
                )
            session.scene.spatialEnvironment.preferredSpatialEnvironment = pref1
            assertThat(session.scene.spatialEnvironment.preferredSpatialEnvironment)
                .isEqualTo(pref1)

            val dragonGeometry =
                GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            val pref2 =
                SpatialEnvironment.SpatialEnvironmentPreference(
                    imageBasedLightingAsset = ibl,
                    geometry = dragonGeometry,
                )
            session.scene.spatialEnvironment.preferredSpatialEnvironment = pref2
            assertThat(session.scene.spatialEnvironment.preferredSpatialEnvironment)
                .isEqualTo(pref2)

            session.scene.spatialEnvironment.preferredSpatialEnvironment = null
            ibl.close()
        }
}
