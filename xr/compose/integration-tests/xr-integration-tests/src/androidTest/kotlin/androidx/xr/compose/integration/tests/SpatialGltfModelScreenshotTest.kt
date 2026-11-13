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

@file:kotlin.OptIn(androidx.xr.compose.subspace.ExperimentalSpatialGltfModelApi::class)

package androidx.xr.compose.integration.tests

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialGltfModelState
import androidx.xr.compose.subspace.SpatialGltfModelStatus
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Paths
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screenshot tests for [SpatialGltfModel]. */
@LargeTest
@XrDeviceTest
@RunWith(AndroidJUnit4::class)
class SpatialGltfModelScreenshotTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    companion object {
        private const val TIMEOUT_MILLIS = 10_000L
        private val DRAGON_MODEL_PATH = Paths.get("models", "Dragon_Evolved.gltf")
        private val ARROWS_MODEL_URI = Uri.parse("models/xyzArrows.glb")
    }

    @Test
    fun spatialGltfModel_fromPath_rendersDragonModel() {
        lateinit var modelState: SpatialGltfModelState
        composeTestRule.setXrScreenshotContent {
            modelState =
                rememberSpatialGltfModelState(SpatialGltfModelSource.fromPath(DRAGON_MODEL_PATH))
            SpatialGltfModel(
                state = modelState,
                modifier = SubspaceModifier.size(650.dp).testTag("dragonModel"),
            )
        }

        composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_gltf_model_dragon_from_path",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun spatialGltfModel_fromUri_rendersArrowsModel() {
        lateinit var modelState: SpatialGltfModelState
        composeTestRule.setXrScreenshotContent {
            modelState =
                rememberSpatialGltfModelState(SpatialGltfModelSource.fromUri(ARROWS_MODEL_URI))
            SpatialBox(modifier = SubspaceModifier.size(550.dp)) {
                SpatialGltfModel(
                    state = modelState,
                    modifier =
                        SubspaceModifier.size(450.dp)
                            .rotate(pitch = -15f, yaw = 25f, roll = 0f)
                            .testTag("arrowsModel"),
                )
            }
        }

        composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_gltf_model_arrows_from_uri",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun spatialGltfModel_nodeTransformModified_rendersUpdatedModel() {
        lateinit var modelState: SpatialGltfModelState
        var transformApplied by mutableStateOf(false)
        composeTestRule.setXrScreenshotContent {
            modelState =
                rememberSpatialGltfModelState(SpatialGltfModelSource.fromPath(DRAGON_MODEL_PATH))
            LaunchedEffect(modelState.status) {
                if (modelState.status == SpatialGltfModelStatus.Loaded && !transformApplied) {
                    val headNode = modelState.nodes.first { it.name == "Head" }
                    headNode.localScale = Vector3(2.2f, 2.2f, 2.2f)
                    headNode.localPose =
                        Pose(
                            headNode.localPose.translation + Vector3(0f, 0.25f, 0.15f),
                            headNode.localPose.rotation *
                                Quaternion.fromEulerAngles(pitch = -20f, yaw = 40f, roll = 0f),
                        )
                    val leftWingNode = modelState.nodes.first { it.name == "Wing1.L" }
                    leftWingNode.localPose =
                        Pose(
                            leftWingNode.localPose.translation,
                            leftWingNode.localPose.rotation *
                                Quaternion.fromEulerAngles(pitch = 0f, yaw = 0f, roll = 45f),
                        )
                    val rightWingNode = modelState.nodes.first { it.name == "Wing1.R" }
                    rightWingNode.localPose =
                        Pose(
                            rightWingNode.localPose.translation,
                            rightWingNode.localPose.rotation *
                                Quaternion.fromEulerAngles(pitch = 0f, yaw = 0f, roll = -45f),
                        )
                    transformApplied = true
                }
            }
            SpatialBox(modifier = SubspaceModifier.width(750.dp).height(550.dp).depth(450.dp)) {
                SpatialGltfModel(
                    state = modelState,
                    modifier = SubspaceModifier.size(600.dp).testTag("dragonModified"),
                )
            }
        }

        composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { transformApplied }
        composeTestRule.waitForIdle()

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_gltf_model_node_transform_modified",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun spatialGltfModel_materialOverride_rendersUpdatedMaterial() {
        lateinit var modelState: SpatialGltfModelState
        var materialApplied by mutableStateOf(false)
        composeTestRule.setXrScreenshotContent {
            val session = checkNotNull(LocalSession.current)
            modelState =
                rememberSpatialGltfModelState(SpatialGltfModelSource.fromPath(DRAGON_MODEL_PATH))
            var overrideMaterial by remember { mutableStateOf<KhronosPbrMaterial?>(null) }
            DisposableEffect(Unit) { onDispose { overrideMaterial?.close() } }
            LaunchedEffect(modelState.status) {
                if (
                    modelState.status == SpatialGltfModelStatus.Loaded && overrideMaterial == null
                ) {
                    val material =
                        KhronosPbrMaterial.create(session, AlphaMode.OPAQUE).apply {
                            setBaseColorFactor(Vector4(1.0f, 0.72f, 0.12f, 1.0f))
                            setMetallicFactor(0.4f)
                            setRoughnessFactor(0.35f)
                        }
                    val dragonNode = modelState.nodes.first { it.name == "Dragon" }
                    dragonNode.setMaterialOverride(material, primitiveIndex = 0)
                    overrideMaterial = material
                    materialApplied = true
                }
            }
            SpatialGltfModel(
                state = modelState,
                modifier = SubspaceModifier.size(650.dp).testTag("dragonMaterialOverride"),
            )
        }

        composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { materialApplied }
        composeTestRule.waitForIdle()

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_gltf_model_material_override",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun spatialGltfModel_withChildSpatialPanel_rendersModelAndPanel() {
        lateinit var modelState: SpatialGltfModelState
        composeTestRule.setXrScreenshotContent {
            modelState =
                rememberSpatialGltfModelState(SpatialGltfModelSource.fromPath(DRAGON_MODEL_PATH))
            SpatialBox(modifier = SubspaceModifier.width(700.dp).height(660.dp).depth(400.dp)) {
                SpatialGltfModel(
                    state = modelState,
                    modifier =
                        SubspaceModifier.offset(y = (-80).dp)
                            .size(500.dp)
                            .testTag("dragonWithPanel"),
                ) {
                    ColoredSpatialPanel(
                        tag = "childPanel",
                        label = "Dragon Panel",
                        width = 260.dp,
                        height = 120.dp,
                        color = Color(0xFF1565C0),
                        modifier = SubspaceModifier.offset(y = (-933).dp, z = 400.dp).scale(6.33f),
                    )
                }
            }
        }

        composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)
        composeTestRule.onNodeWithText("Dragon Panel").assertIsDisplayed()

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_gltf_model_with_child_panel",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun spatialGltfModel_inSpatialRowWithModifiers_rendersLayout() {
        lateinit var modelState: SpatialGltfModelState
        composeTestRule.setXrScreenshotContent {
            modelState =
                rememberSpatialGltfModelState(SpatialGltfModelSource.fromPath(DRAGON_MODEL_PATH))
            SpatialRow(modifier = SubspaceModifier.width(960.dp).height(440.dp)) {
                ColoredSpatialPanel(
                    tag = "leftPanel",
                    label = "Left Panel",
                    width = 200.dp,
                    height = 240.dp,
                    color = Color.Blue,
                )
                SpatialBox(modifier = SubspaceModifier.width(520.dp).height(400.dp).depth(360.dp)) {
                    SpatialGltfModel(
                        state = modelState,
                        modifier =
                            SubspaceModifier.offset(y = (-125).dp)
                                .size(440.dp)
                                .rotate(pitch = -10f, yaw = 30f, roll = 0f)
                                .scale(0.9f)
                                .testTag("centerModel"),
                    )
                }
                ColoredSpatialPanel(
                    tag = "rightPanel",
                    label = "Right Panel",
                    width = 200.dp,
                    height = 240.dp,
                    color = Color.Red,
                )
            }
        }

        composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)
        composeTestRule.onNodeWithText("Left Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right Panel").assertIsDisplayed()

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_gltf_model_in_spatial_row",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    fun spatialGltfModel_invalidUri_failsBeforeScreenshotCapture() {
        lateinit var modelState: SpatialGltfModelState
        val invalidUri = Uri.fromFile(File(composeTestRule.activity.cacheDir, "xyzArrows.glb"))
        composeTestRule.setXrScreenshotContent {
            modelState = rememberSpatialGltfModelState(SpatialGltfModelSource.fromUri(invalidUri))
            SpatialGltfModel(
                state = modelState,
                modifier = SubspaceModifier.size(450.dp).testTag("invalidModel"),
            )
        }

        val error =
            assertThrows(AssertionError::class.java) {
                composeTestRule.waitForGltfModelToLoad(modelState, timeoutMillis = TIMEOUT_MILLIS)
            }
        assertThat(modelState.status).isInstanceOf(SpatialGltfModelStatus.Failed::class.java)
        assertThat(error.cause).isNotNull()
    }

    @Composable
    @SubspaceComposable
    private fun ColoredSpatialPanel(
        tag: String,
        label: String,
        width: Dp,
        height: Dp,
        color: Color,
        textColor: Color =
            if (color == Color.Yellow || color == Color.Cyan) Color.Black else Color.White,
        modifier: SubspaceModifier = SubspaceModifier,
    ) {
        SpatialPanel(modifier = modifier.width(width).height(height).testTag(tag)) {
            Box(
                modifier = Modifier.fillMaxSize().background(color).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, color = textColor, fontSize = 20.sp)
            }
        }
    }
}
