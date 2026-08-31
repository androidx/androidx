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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialGltfModelState
import androidx.xr.compose.subspace.SpatialGltfModelStatus
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [SpatialGltfModel] covering the SpatialGltfModel Introspection CUJ:
 * - 3D glTF asset loading into a Subspace via [SpatialGltfModelSource.fromPath],
 *   [SpatialGltfModelSource.fromUri], and [SpatialGltfModelSource.fromResource]
 * - Graceful failure handling for invalid / non-existent URIs and resources
 * - Node hierarchy introspection
 * - Programmatic node transform (localPose and localScale) manipulation
 * - PBR material override application and clearing
 * - Nested child spatial content rendering
 *
 * Future CUJ roadmap / pending items:
 * - TODO(b/527562998): Add glTF animation playback and dynamic node attachment tracking tests once
 *   animation APIs are re-introduced post-Beta 1.0.
 * - TODO: Add test for non-mesh node material override error propagation once the native layer
 *   gracefully translates invalid operations to catchable Java exceptions rather than hard aborts.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialGltfModelTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Helper function that sets up a [Subspace] composable with a [SpatialGltfModel] and waits
     * until the model transitions to a terminal state ([SpatialGltfModelStatus.Loaded] or
     * [SpatialGltfModelStatus.Failed]).
     */
    private fun setGltfModelContentAndWait(
        source: SpatialGltfModelSource = SpatialGltfModelSource.fromPath(DEFAULT_MODEL_PATH),
        additionalSubspaceContent: (@Composable @SubspaceComposable () -> Unit)? = null,
        childContent: (@Composable @SubspaceComposable () -> Unit)? = null,
    ): SpatialGltfModelState {
        var state: SpatialGltfModelState? = null
        var isSpatialSupported = true

        composeTestRule.setContent {
            val spatialConfig = LocalSpatialConfiguration.current
            if (!spatialConfig.hasXrSpatialFeature) {
                isSpatialSupported = false
            }
            Subspace {
                additionalSubspaceContent?.invoke()
                val rememberedState = rememberSpatialGltfModelState(source = source)
                state = rememberedState
                if (childContent != null) {
                    SpatialGltfModel(state = rememberedState) { childContent() }
                } else {
                    SpatialGltfModel(state = rememberedState)
                }
            }
        }

        // Avoid waiting for the full timeout on non-XR environments where Subspace is a no-op
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            isSpatialSupported,
        )

        composeTestRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            val currentState = state
            currentState != null &&
                (currentState.status is SpatialGltfModelStatus.Loaded ||
                    currentState.status is SpatialGltfModelStatus.Failed)
        }

        return checkNotNull(state)
    }

    /**
     * Verifies that a glTF asset loads and renders successfully in the Subspace from an asset path.
     *
     * This test serves as a critical integration check for asynchronous session initialization.
     * Historically, initializing the Session on a background thread could cause thread-boundary
     * crashes in apps using impress features (such as glTF model loaders). Verifying successful
     * glTF rendering ensures that asynchronous session creation remains fully thread-safe and
     * stable.
     *
     * TODO: b/519600136 - Look into testing Session initialization directly.
     */
    @Test
    fun spatialGltfModel_fromPath_loadsSuccessfully() {
        val state = setGltfModelContentAndWait()
        assertThat(state.status).isEqualTo(SpatialGltfModelStatus.Loaded)
    }

    /** Verifies that a binary glTF (.glb) model loads successfully from a [Uri]. */
    @Test
    fun spatialGltfModel_fromUri_loadsSuccessfully() {
        val state =
            setGltfModelContentAndWait(
                source = SpatialGltfModelSource.fromUri(Uri.parse("models/xyzArrows.glb"))
            )
        assertThat(state.status).isEqualTo(SpatialGltfModelStatus.Loaded)
    }

    /** Verifies that loading from a non-existent [Uri] transitions gracefully to Failed. */
    @Test
    fun spatialGltfModel_fromNonExistentUri_failsGracefully() {
        val state =
            setGltfModelContentAndWait(
                source = SpatialGltfModelSource.fromUri(Uri.parse("models/non_existent_model.glb"))
            )
        assertThat(state.status).isInstanceOf(SpatialGltfModelStatus.Failed::class.java)
    }

    /**
     * Verifies that loading via [SpatialGltfModelSource.fromResource] transitions gracefully to
     * [SpatialGltfModelStatus.Failed].
     */
    @Test
    fun spatialGltfModel_fromResource_failsGracefully() {
        val state =
            setGltfModelContentAndWait(
                source =
                    SpatialGltfModelSource.fromResource(
                        composeTestRule.activity,
                        android.R.drawable.ic_menu_camera,
                    )
            )
        assertThat(state.status).isInstanceOf(SpatialGltfModelStatus.Failed::class.java)
    }

    /** Verifies that glTF nodes can be inspected once the asset is loaded. */
    @Test
    fun spatialGltfModel_nodesAreAccessible() {
        val state = setGltfModelContentAndWait()
        assertThat(state.status).isEqualTo(SpatialGltfModelStatus.Loaded)

        val nodes = checkNotNull(state.nodes)
        assertThat(nodes).isNotEmpty()

        val nodeNames = nodes.mapNotNull { it.name }
        assertThat(nodeNames).contains("Dragon")
    }

    /** Verifies that a node's local transform and scale can be updated programmatically. */
    @Test
    fun spatialGltfModel_nodeTransformCanBeModified() {
        val state = setGltfModelContentAndWait()
        assertThat(state.status).isEqualTo(SpatialGltfModelStatus.Loaded)

        val dragonNode = state.nodes?.firstOrNull { it.name == "Dragon" }
        assertThat(dragonNode).isNotNull()

        val targetPose = Pose(translation = Vector3(1.0f, 2.0f, 3.0f))
        val targetScale = Vector3(2.0f, 2.0f, 2.0f)

        var readPose: Pose? = null
        var readScale: Vector3? = null

        composeTestRule.runOnUiThread {
            dragonNode!!.localPose = targetPose
            dragonNode!!.localScale = targetScale
            readPose = dragonNode!!.localPose
            readScale = dragonNode!!.localScale
        }
        composeTestRule.waitForIdle()

        val translation = checkNotNull(readPose?.translation)
        val scale = checkNotNull(readScale)

        assertThat(translation.x).isWithin(0.01f).of(1.0f)
        assertThat(translation.y).isWithin(0.01f).of(2.0f)
        assertThat(translation.z).isWithin(0.01f).of(3.0f)

        assertThat(scale.x).isWithin(0.01f).of(2.0f)
        assertThat(scale.y).isWithin(0.01f).of(2.0f)
        assertThat(scale.z).isWithin(0.01f).of(2.0f)
    }

    /** Verifies applying and clearing material overrides on a mesh node. */
    @Test
    fun spatialGltfModel_materialOverrideCanBeAppliedAndCleared() {
        var material: KhronosPbrMaterial? = null
        var isCreated by mutableStateOf(false)

        val state =
            setGltfModelContentAndWait(
                additionalSubspaceContent = {
                    val session = LocalSession.current
                    LaunchedEffect(session) {
                        if (session != null) {
                            val mat = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
                            mat.setMetallicFactor(1.0f)
                            mat.setRoughnessFactor(0.0f)
                            material = mat
                            isCreated = true
                        }
                    }
                }
            )

        composeTestRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            isCreated || state.status is SpatialGltfModelStatus.Failed
        }

        assertThat(state.status).isEqualTo(SpatialGltfModelStatus.Loaded)
        val dragonNode = checkNotNull(state.nodes?.firstOrNull { it.name == "Dragon" })
        val mat = checkNotNull(material)

        composeTestRule.runOnUiThread {
            // Apply override
            dragonNode.setMaterialOverride(mat, 0)

            // Clear override
            dragonNode.clearMaterialOverride(0)
        }
        composeTestRule.waitForIdle()
    }

    /** Verifies that child spatial content placed inside SpatialGltfModel is mounted. */
    @Test
    fun spatialGltfModel_rendersChildSpatialContent() {
        val state =
            setGltfModelContentAndWait(
                childContent = { SpatialPanel { Text("Child Spatial Panel") } }
            )

        assertThat(state.status).isEqualTo(SpatialGltfModelStatus.Loaded)
        composeTestRule.onNodeWithText("Child Spatial Panel").assertIsDisplayed()
    }

    private companion object {
        val DEFAULT_MODEL_PATH: Path = Paths.get("models", "Dragon_Evolved.gltf")
        const val TIMEOUT_MILLIS: Long = 5_000L
    }
}
