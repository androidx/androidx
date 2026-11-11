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
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitUntil
import java.nio.file.Paths
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies 3D glTF rendering, Khronos PBR material overrides, and Image-Based Lighting (IBL)
 * skyboxes.
 *
 * Follows the XR Screenshot Testing architecture:
 * 1. Uses [createXrSessionRule] ([XrSessionRule]) to run serially on the instrumentation thread and
 *    configure the scene with SceneCore APIs (Full Space Mode via [requestFullSpace],
 *    `preferredPassthroughOpacity = 0.0f`, IBL skybox preference, and hidden 2D main panel).
 * 2. Waits for async environment/passthrough state via [waitForSpatialEnvironmentSettled] and scene
 *    graph readiness via [waitUntil] + [SpatialSceneHelper.captureSpatialScene], then captures the
 *    stabilized 3D compositor viewport via [XrSessionRule.captureToBitmap].
 * 3. Asserts the captured bitmap against the golden image using
 *    `androidx.test.screenshot.assertAgainstGolden` with [XrScreenshotTestRule] and [MSSIMMatcher].
 */
@RunWith(AndroidJUnit4::class)
@XrDeviceTest
@LargeTest
class GltfMaterialAndIblScreenshotTest {

    @get:Rule val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    @get:Rule val sessionRule = createXrSessionRule()

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        scene.mainPanelEntity.setEnabled(false)

        sessionRule.onMainThread {
            /**
             * The studio lighting asset provides 360-degree lighting with a black skybox. This
             * allows us to provide the GLTF model with light for rendering without the skybox
             * affecting the quality of the screenshot.
             */
            val studioLightingIbl =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    STUDIO_LIGHTING_IBL_PATH,
                )
            scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
            scene.spatialEnvironment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(studioLightingIbl, null)
        }

        sessionRule.waitForSpatialEnvironmentSettled()
    }

    @Test
    fun gltfModelWithBlueSkyboxIbl_rendersInFullSpace() {
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, DRAGON_GLTF_PATH)

            GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = MODEL_POSE,
                    parent = session.scene.activitySpace,
                )
                .apply { setScale(MODEL_SCALE) }
        }

        waitUntil("glTF subspace visible") {
            val task =
                SpatialSceneHelper.captureSpatialScene().findTaskByActivity(sessionRule.activity)
            task != null &&
                task.isFullSpace &&
                task.taskWindowLeash?.isVisible != true &&
                task.subspaceNodes.count { it.isVisible } == 1
        }
        sessionRule
            .captureToBitmap(autoCrop = true)
            .assertAgainstGolden(
                screenshotRule,
                "gltf_dragon_studio_lighting_ibl",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun gltfModelWithPbrMaterialOverride_rendersUpdatedMaterialInFullSpace() {
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, DRAGON_GLTF_PATH)
            val material = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
            // TODO(b/573225031): Add a testcase with passthrough enabled to verify system
            // lighting and background reflection on PBR materials.
            material.setBaseColorFactor(Vector4(0.15f, 0.75f, 0.95f, 1.0f))
            material.setMetallicFactor(0.9f)
            material.setRoughnessFactor(0.2f)

            // Pre-configure the entity and its PBR material override while detached
            // (parent = null) so that the very first frame presented in ActivitySpace already
            // has the material applied.
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = MODEL_POSE,
                    parent = null,
                )
            entity.setScale(MODEL_SCALE)

            checkNotNull(entity.nodes.firstOrNull { it.name == DRAGON_NODE_NAME }) {
                    "Expected node '$DRAGON_NODE_NAME' in Dragon_Evolved.gltf"
                }
                .setMaterialOverride(material)
            entity.parent = session.scene.activitySpace
        }

        waitUntil("glTF PBR subspace visible") {
            val task =
                SpatialSceneHelper.captureSpatialScene().findTaskByActivity(sessionRule.activity)
            task != null &&
                task.isFullSpace &&
                task.taskWindowLeash?.isVisible != true &&
                task.subspaceNodes.count { it.isVisible } == 1
        }
        sessionRule
            .captureToBitmap(autoCrop = true)
            .assertAgainstGolden(
                screenshotRule,
                "gltf_dragon_pbr_material_override",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    private companion object {
        const val DRAGON_NODE_NAME = "Dragon"
        const val MODEL_SCALE = 0.35f
        val DRAGON_GLTF_PATH = Paths.get("models", "Dragon_Evolved.gltf")
        val STUDIO_LIGHTING_IBL_PATH = Paths.get("skyboxes", "studio_lighting_ibl.zip")
        val MODEL_POSE = Pose(translation = Vector3(0f, -0.6f, -2.5f))
    }
}
