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

package androidx.xr.compose.subspace

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialGltfModelStatus.Failed
import androidx.xr.compose.subspace.SpatialGltfModelStatus.Loaded
import androidx.xr.compose.subspace.SpatialGltfModelStatus.Loading
import androidx.xr.compose.subspace.draw.alpha
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.sizeIn
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfModelResource
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.FakeGltfAnimationFeature
import androidx.xr.scenecore.testing.FakeGltfEntity
import androidx.xr.scenecore.testing.FakeGltfModelNodeFeature
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@TargetApi(Build.VERSION_CODES.O) // needed for the Paths.get API
class SpatialGltfModelTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    // --- Test Cases ---

    // 1. Model Loading and Source Types
    @Test
    fun spatialModel_fromPath_loadsAndRenders() {
        // Verify that a model is successfully loaded and rendered when using
        // `SpatialModelSource.fromPath` with a valid asset path.

        val loadedAssets = mutableListOf<String>()

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource {
                        loadedAssets.add(assetName)
                        return it.loadGltfByAssetName(assetName)
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        )
                )
            }
        }

        assertThat(loadedAssets).containsExactly("asset.glb")
    }

    @Test
    fun spatialModel_fromData_loadsAndRenders() {
        // Verify that a model is successfully loaded and rendered when using
        // `SpatialModelSource.fromData` with a `ByteArray`.

        val loadedAssetData = mutableListOf<ByteArray>()
        val loadedAssetKeys = mutableListOf<String>()

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByByteArray(
                        assetData: ByteArray,
                        assetKey: String,
                    ): GltfModelResource {
                        loadedAssetData.add(assetData)
                        loadedAssetKeys.add(assetKey)
                        return it.loadGltfByByteArray(assetData, assetKey)
                    }
                }
            }
        )

        val testAssetData = ByteArray(0)

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source =
                                SpatialGltfModelSource.fromData(
                                    assetData = testAssetData,
                                    assetKey = "testAsset",
                                )
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(loadedAssetData).containsExactly(testAssetData)
        assertThat(loadedAssetKeys).containsExactly("testAsset")
    }

    @Test
    fun spatialModel_fromUri_loadsAndRenders() {
        // Verify that a model is successfully loaded and rendered when using
        // `SpatialModelSource.fromUri`.

        val loadedAssets = mutableListOf<String>()

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource {
                        loadedAssets.add(assetName)
                        return it.loadGltfByAssetName(assetName)
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source =
                                SpatialGltfModelSource.fromUri(
                                    Uri.parse("http://test.com/asset.glb")
                                )
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(loadedAssets).containsExactly("http://test.com/asset.glb")
    }

    @Test
    fun spatialModel_invalidSource_handlesError() {
        // Test the behavior when the `SpatialModelSource` points to a non-existent or corrupt file.
        // The composable should handle the loading failure gracefully.

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource =
                        throw IllegalStateException()
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromUri(Uri.parse("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertDoesNotExist()
    }

    @Test
    fun spatialModel_sourceChanged_reloadsModel() {
        // When the `source` parameter of the `SpatialModel` composable changes, verify that the old
        // model is disposed of and the new model is loaded.

        val disposedAssets = mutableListOf<GltfModelResource>()
        val createdAssets = mutableMapOf<String, GltfModelResource>()

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource {
                        val result = it.loadGltfByAssetName(assetName)
                        createdAssets[assetName] = result
                        return result
                    }

                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override fun dispose() {
                                disposedAssets.add(loadedGltf)
                                entity.dispose()
                            }
                        }
                    }
                }
            }
        )

        val uri = mutableStateOf("asset.glb")

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromUri(Uri.parse(uri.value))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(createdAssets).hasSize(1)
        assertThat(createdAssets).containsKey("asset.glb")
        assertThat(disposedAssets).isEmpty()

        uri.value = "second_asset.glb"

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(createdAssets).hasSize(2)
        assertThat(createdAssets).containsKey("second_asset.glb")
        assertThat(disposedAssets).containsExactly(createdAssets["asset.glb"])
    }

    @Test
    fun spatialModel_sourceChanged_fromValidToInvalid() {
        // When the `source` parameter of the `SpatialModel` composable changes from a valid asset
        // to an invalid one, verify that the old model is disposed of and the composable is removed
        // from the layout.

        val disposedAssets = mutableListOf<GltfModelResource>()
        val createdAssets = mutableMapOf<String, GltfModelResource>()

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource {
                        if (assetName == "invalid.glb") {
                            throw IllegalStateException()
                        }

                        val asset = it.loadGltfByAssetName(assetName)
                        createdAssets[assetName] = asset
                        return asset
                    }

                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override fun dispose() {
                                disposedAssets.add(loadedGltf)
                                entity.dispose()
                            }
                        }
                    }
                }
            }
        )

        var sourcePath by mutableStateOf("valid.glb")

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get(sourcePath))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(createdAssets).hasSize(1)
        assertThat(createdAssets).containsKey("valid.glb")
        assertThat(disposedAssets).isEmpty()

        // Change to a source that will fail to load
        sourcePath = "invalid.glb"

        composeTestRule.onSubspaceNodeWithTag("model").assertDoesNotExist()
        assertThat(createdAssets).hasSize(1)
        assertThat(disposedAssets).containsExactly(createdAssets["valid.glb"])
    }

    @Test
    fun spatialModel_sourceChanged_fromInvalidToValid() {
        // When the `source` parameter of the `SpatialModel` composable changes from an invalid
        // asset to a valid one, verify that the composable is initially not part of the layout but
        // then become part of the layout and loads the valid asset.

        val disposedAssets = mutableListOf<GltfModelResource>()
        val createdAssets = mutableMapOf<String, GltfModelResource>()

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource {
                        if (assetName == "invalid.glb") {
                            throw IllegalStateException()
                        }

                        val asset = it.loadGltfByAssetName(assetName)
                        createdAssets[assetName] = asset
                        return asset
                    }

                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override fun dispose() {
                                disposedAssets.add(loadedGltf)
                                entity.dispose()
                            }
                        }
                    }
                }
            }
        )

        var state by
            mutableStateOf(
                SpatialGltfModelState(
                    source = SpatialGltfModelSource.fromPath(Paths.get("invalid.glb"))
                )
            )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertDoesNotExist()
        assertThat(createdAssets).hasSize(0)
        val status = state.status.value
        assertIs<Failed>(status)
        assertIs<IllegalStateException>(status.exception)

        // Change to a source that will load successfully
        state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("valid.glb")))

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(createdAssets).hasSize(1)
        assertThat(createdAssets).containsKey("valid.glb")
        assertThat(disposedAssets).isEmpty()
        assertIs<Loaded>(state.status.value)
    }

    // 2. Layout and Sizing
    @Test
    fun spatialModel_noModifier_takesIntrinsicSize() {
        // When no `content` or size `SubspaceModifier` is provided, assert that the
        // `SpatialModel`'s layout size matches the intrinsic bounding box of the loaded 3D asset.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        // The glTF size is 1m x 1m x 1m and 1000 dp per meter the size should be 1000.dp x 1000.dp
        // x 1000.dp
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(1000.dp)
            .assertHeightIsEqualTo(1000.dp)
            .assertDepthIsEqualTo(1000.dp)
    }

    @Test
    fun spatialModel_noModifier_remeasuresAfterModelLoads() {
        // When no `content` or size `SubspaceModifier` is provided, assert that the
        // `SpatialModel`'s layout size is initially zero but then matches the intrinsic bounding
        // box of the 3D asset after it loads.

        val completableDeferred = CompletableDeferred<GltfModelResource>()

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource =
                        completableDeferred.await()

                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        // The model hasn't loaded yet so the size is initially 0.dp x 0.dp x 0.dp
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)
            .assertDepthIsEqualTo(0.dp)

        completableDeferred.complete(object : GltfModelResource {})

        // The glTF size is 1m x 1m x 1m and 1000 dp per meter the size should be 1000.dp x 1000.dp
        // x 1000.dp
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(1000.dp)
            .assertHeightIsEqualTo(1000.dp)
            .assertDepthIsEqualTo(1000.dp)
    }

    @Test
    fun spatialModel_withExplicitSizeModifier_scalesModelToFit() {
        // Apply `SubspaceModifier.size()` to the `SpatialModel`. Verify that the layout's size
        // matches the modifier and that the rendered model is scaled up or down to fit within those
        // bounds.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model").size(200.dp),
                )
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertDepthIsEqualTo(200.dp)

        // The glTF size is 1m x 1m x 1m so the scale should be 0.2f to fit 1000.dp (at 1000 dp per
        // meter) into the 200.dp space.
        assertThat(composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().scale)
            .isEqualTo(0.2f)
    }

    @Test
    fun spatialModel_withFillMaxSize_fillsAvailableSpace() {
        // Use `SubspaceModifier.fillMaxSize()` and assert that the `SpatialModel` expands to fill
        // the constraints provided by its parent.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val entity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by entity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialBox(SubspaceModifier.size(200.dp)) {
                    SpatialGltfModel(
                        state =
                            rememberSpatialGltfModelState(
                                source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                            ),
                        modifier = SubspaceModifier.testTag("model").fillMaxSize(),
                    )
                }
            }
        }

        // The glTF size is 1m x 1m x 1m
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertDepthIsEqualTo(200.dp)
        assertThat(composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().scale)
            .isEqualTo(0.2f)
    }

    @Test
    fun spatialModel_withNonUniformSizeModifier_scalesToFitMostConstrainingDimension() {
        // A non-uniform model (2m x 1m x 1m) in a non-uniform space (300dp x 400dp x 200dp).
        // The model should scale to fit the most constraining dimension.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val gltfEntity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by gltfEntity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromCenterAndHalfExtents(
                                    center = Vector3.Zero,
                                    // Intrinsic size: 2m wide, 1m tall, 1m deep
                                    halfExtents =
                                        FloatSize3d(width = 1f, height = 0.5f, depth = 0.5f),
                                )
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier =
                        SubspaceModifier.testTag("model")
                            // Layout size: 300dp wide, 400dp tall, 200dp deep
                            .size(DpVolumeSize(width = 300.dp, height = 400.dp, depth = 200.dp)),
                )
            }
        }

        // The layout size must match the modifier exactly.
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(400.dp)
            .assertDepthIsEqualTo(200.dp)

        // Intrinsic size is 2000dp x 1000dp x 1000dp.
        // Layout size is 300dp x 400dp x 200dp.
        // Scale ratios:
        // Width:  300 / 2000 = 0.15
        // Height: 400 / 1000 = 0.4
        // Depth:  200 / 1000 = 0.2
        // The width is the most constraining dimension, so the scale should be 0.15.
        assertThat(composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().scale)
            .isEqualTo(0.15f)
    }

    @Test
    fun spatialModel_inConstrainedSpace_scalesAndReportsCorrectSize() {
        // A tall model (1m x 2m x 1m) placed in a cubic space (1m x 1m x 1m) should scale down by
        // 0.5 to fit the height. Its resulting layout size should be 0.5m x 1m x 0.5m, not the 1m x
        // 1m x 1m of the parent.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val gltfEntity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by gltfEntity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromCenterAndHalfExtents(
                                    center = Vector3.Zero,
                                    halfExtents =
                                        FloatSize3d(width = 0.5f, height = 1f, depth = 0.5f),
                                )
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                // Parent provides the constraints
                SpatialBox(SubspaceModifier.size(1000.dp)) {
                    SpatialGltfModel(
                        state =
                            rememberSpatialGltfModelState(
                                source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                            ),
                        // The model itself has no size modifier, so it should wrap its content.
                        modifier = SubspaceModifier.testTag("model"),
                    )
                }
            }
        }

        // The model's intrinsic size is 1000dp x 2000dp x 1000dp.
        // The available space is 1000dp x 1000dp x 1000dp.
        // Height is the most constraining dimension, so the scale factor is 1000/2000 = 0.5.
        // The final layout size should be the intrinsic size multiplied by the scale factor.
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(500.dp) // 1000dp * 0.5
            .assertHeightIsEqualTo(1000.dp) // 2000dp * 0.5
            .assertDepthIsEqualTo(500.dp) // 1000dp * 0.5

        // The scale of the entity itself should be the calculated scale factor.
        assertThat(composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().scale)
            .isEqualTo(0.5f)
    }

    @Test
    fun spatialModel_withSizeModifier_scalesToFitDepthWhenMostConstraining() {
        // A non-uniform model (1m x 1m x 2m) in a non-uniform space.
        // The model should scale to fit the depth, as it is the most constraining dimension.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        val gltfEntity = it.createGltfEntity(pose, loadedGltf, parentEntity)
                        return object : GltfEntity by gltfEntity {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromCenterAndHalfExtents(
                                    center = Vector3.Zero,
                                    // Intrinsic size: 1m wide, 1m tall, 2m deep
                                    halfExtents =
                                        FloatSize3d(width = 0.5f, height = 0.5f, depth = 1f),
                                )
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier =
                        SubspaceModifier.testTag("model")
                            // Layout size: 400dp wide, 300dp tall, 200dp deep
                            .size(DpVolumeSize(width = 400.dp, height = 300.dp, depth = 200.dp)),
                )
            }
        }

        // The layout size must match the modifier exactly.
        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(400.dp)
            .assertHeightIsEqualTo(300.dp)
            .assertDepthIsEqualTo(200.dp)

        // Intrinsic size is 1000dp x 1000dp x 2000dp.
        // Layout size is 400dp x 300dp x 200dp.
        // Scale ratios:
        // Width:  400 / 1000 = 0.4
        // Height: 300 / 1000 = 0.3
        // Depth:  200 / 2000 = 0.1
        // The depth is the most constraining dimension, so the scale should be 0.1.
        assertThat(composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().scale)
            .isEqualTo(0.1f)
    }

    @Test
    fun spatialModel_withZeroIntrinsicSize_takesMinConstraintSize() {
        // A model that has a zero intrinsic size should use the min constraints as its layout size.

        composeTestRule.configureFakeSession(
            defaultDpPerMeter = 1000f,
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return object :
                            GltfEntity by it.createGltfEntity(pose, loadedGltf, parentEntity) {
                            override val gltfModelBoundingBox: BoundingBox =
                                BoundingBox.fromCenterAndHalfExtents(
                                    center = Vector3.Zero,
                                    halfExtents = FloatSize3d(),
                                )
                        }
                    }
                }
            },
        )

        composeTestRule.setContent {
            Subspace {
                // Provide non-zero min constraints
                SpatialBox(
                    modifier =
                        SubspaceModifier.sizeIn(
                            minWidth = 10.dp,
                            minHeight = 20.dp,
                            minDepth = 30.dp,
                        ),
                    propagateMinConstraints = true,
                ) {
                    SpatialGltfModel(
                        state =
                            rememberSpatialGltfModelState(
                                source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                            ),
                        modifier = SubspaceModifier.testTag("model"),
                    )
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(30.dp)
    }

    // 3. State Management (SpatialModelState)
    @Test
    fun state_isSpatialModelReady_isTrueAfterLoad() {
        // Pass a `SpatialModelState` and assert that `isSpatialModelReady.value` is `false`
        // initially and becomes `true` after the model has finished loading.

        val completableDeferred = CompletableDeferred<GltfModelResource>()
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource =
                        completableDeferred.await()
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertIs<Loading>(state.status.value)

        completableDeferred.complete(object : GltfModelResource {}) // simulate loading the glTF

        composeTestRule.waitForIdle()
        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertIs<Loaded>(state.status.value)
    }

    @Test
    fun state_isSpatialModelReady_isFalseAfterSourceChanges() {
        // Pass a `SpatialModelState` and assert that `isSpatialModelReady.value` is `false`
        // initially and becomes `true` after the model has finished loading.

        val assets =
            mapOf(
                "first_asset.glb" to CompletableDeferred<GltfModelResource>(),
                "second_asset.glb" to CompletableDeferred(),
            )
        var state by
            mutableStateOf(
                SpatialGltfModelState(
                    source = SpatialGltfModelSource.fromPath(Paths.get("first_asset.glb"))
                )
            )

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource =
                        assertNotNull(assets[assetName]).await()
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertIs<Loading>(state.status.value)

        assets["first_asset.glb"]?.complete(
            object : GltfModelResource {}
        ) // simulate loading the glTF

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertIs<Loaded>(state.status.value)

        state =
            SpatialGltfModelState(
                source = SpatialGltfModelSource.fromPath(Paths.get("second_asset.glb"))
            )

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertIs<Loading>(state.status.value)
    }

    @Test
    fun state_startAnimation_updatesIsAnimatingState() {
        // Call `state.startAnimation()` and assert that `isAnimating.value` becomes `true` and then
        // returns to `false` after the animation completes.

        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))
        var testEntity: GltfEntity? = null

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).also { entity ->
                            testEntity = entity
                            (entity as FakeGltfEntity).addAnimation(FakeGltfAnimationFeature())
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Stopped)

        animation.start()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Playing)

        // simulate the animation stopping on its own
        testEntity?.animations?.get(0)?.stopAnimation()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Stopped)
    }

    @Test
    fun state_loopAnimation_isAnimatingRemainsTrue() {
        // Call `state.loopAnimation()` and assert that `isAnimating.value` becomes `true` and stays
        // `true`.

        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).also { entity ->
                            (entity as FakeGltfEntity).addAnimation(FakeGltfAnimationFeature())
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Stopped)

        animation.loop()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Playing)

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Playing)
    }

    @Test
    fun state_stopAllAnimations_stopsLoopingAnimation() {
        // Start a looping animation and then call `state.stopAllAnimations()`. Assert that
        // `isAnimating.value` becomes `false`.

        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).also { entity ->
                            (entity as FakeGltfEntity).addAnimation(FakeGltfAnimationFeature())
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Stopped)

        animation.loop()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Playing)

        animation.stop()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Stopped)
    }

    @Test
    fun state_startAnimationByName_playsCorrectAnimation() {
        // For a model with multiple named animations, call `state.startAnimation("name")` and
        // verify that the specific animation is played.

        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))
        var fakeGltfEntity: FakeGltfEntity? = null

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).apply {
                            fakeGltfEntity = this as FakeGltfEntity
                            fakeGltfEntity.addAnimation(FakeGltfAnimationFeature())
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Stopped)
        assertThat(fakeGltfEntity?.currentAnimationName).isNull()

        animation.start()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(fakeGltfEntity?.animations?.get(0)?.animationState)
            .isEqualTo(GltfEntity.AnimationState.PLAYING)
    }

    @Test
    fun animation_onPauseAndResume_updatesState() {
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))
        var fakeGltfEntity: FakeGltfEntity?

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).apply {
                            fakeGltfEntity = this as FakeGltfEntity
                            fakeGltfEntity.addAnimation(FakeGltfAnimationFeature())
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]
        animation.start()

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Playing)

        animation.pause()
        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Paused)

        animation.start()
        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(animation.animationState)
            .isEqualTo(SpatialGltfModelAnimation.AnimationState.Playing)
    }

    @Test
    fun animation_seekTo_updatesStartTime() {
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))
        var fakeGltfEntity: FakeGltfEntity?
        var fakeAnimation: FakeGltfAnimationFeature? = null

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).apply {
                            fakeGltfEntity = this as FakeGltfEntity
                            fakeAnimation = FakeGltfAnimationFeature()
                            fakeGltfEntity.addAnimation(fakeAnimation)
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]

        // Seek while stopped sets the start time
        animation.seekTo(5.seconds)
        animation.start()
        assertThat(fakeAnimation?.seekStartTimeSeconds).isEqualTo(5.0f)

        // Seek while playing updates the animation time
        animation.seekTo(10.seconds)
        assertThat(fakeAnimation?.seekStartTimeSeconds).isEqualTo(10.0f)
    }

    @Test
    fun animation_speed_updatesAnimationSpeed() {
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))
        var fakeGltfEntity: FakeGltfEntity?
        var fakeAnimation: FakeGltfAnimationFeature? = null

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).apply {
                            fakeGltfEntity = this as FakeGltfEntity
                            fakeAnimation = FakeGltfAnimationFeature()
                            fakeGltfEntity.addAnimation(fakeAnimation)
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]

        animation.speed = 2.0f
        assertThat(fakeAnimation?.speed).isEqualTo(2.0f)

        animation.start()
        assertThat(fakeAnimation?.speed).isEqualTo(2.0f)
    }

    @Test
    fun animation_seekToNegativeValue_throwsException() {
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))

        composeTestRule.configureFakeSession(
            renderingRuntime = {
                object : RenderingRuntime by it {
                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return it.createGltfEntity(pose, loadedGltf, parentEntity).apply {
                            (this as FakeGltfEntity).addAnimation(FakeGltfAnimationFeature())
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val animation = state.animations[0]

        assertFailsWith<IllegalArgumentException> { animation.seekTo((-1).seconds) }
    }

    // 4. Composition and Lifecycle
    @Test
    fun spatialModel_onEnterComposition_entityIsCreated() {
        // Assert that when `SpatialModel` enters the composition, a corresponding `GltfModelEntity`
        // is created in the scene graph.

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("model.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                )
            }
        }

        val entity =
            checkNotNull(
                composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().semanticsEntity
            )
        val gltfEntities =
            checkNotNull(
                composeTestRule.session?.scene?.getEntitiesOfType(GltfModelEntity::class.java)
            )
        assertThat(gltfEntities).hasSize(1)
        assertThat(entity).isEqualTo(gltfEntities[0])
    }

    @Test
    fun spatialModel_onLeaveComposition_entityIsDisposed() {
        // When the `SpatialModel` is removed from the composition (e.g., via conditional
        // rendering), assert that its underlying `GltfModelEntity` is properly disposed of and
        // removed from the scene.

        var isInComposition by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                if (isInComposition) {
                    SpatialGltfModel(
                        state =
                            rememberSpatialGltfModelState(
                                source = SpatialGltfModelSource.fromPath(Paths.get("model.glb"))
                            ),
                        modifier = SubspaceModifier.testTag("model"),
                    )
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        assertThat(composeTestRule.session?.scene?.getEntitiesOfType(GltfModelEntity::class.java))
            .hasSize(1)

        isInComposition = false

        composeTestRule.onSubspaceNodeWithTag("model").assertDoesNotExist()
        assertThat(composeTestRule.session?.scene?.getEntitiesOfType(GltfModelEntity::class.java))
            .isEmpty()
    }

    @Test
    fun spatialModel_recomposition_retainsState() {
        // Trigger recomposition of the `SpatialModel`'s parent and verify that the model and its
        // state (e.g., `isSpatialModelReady`) are preserved without being reloaded.

        var parentSize by mutableStateOf(200.dp)
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("model.glb")))

        // Before we load the glTF, make sure that the initial state is false
        assertIs<Loading>(state.status.value)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.size(parentSize)) {
                    SpatialGltfModel(modifier = SubspaceModifier.testTag("model"), state = state)
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val entityBeforeRecomposition =
            checkNotNull(
                composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().semanticsEntity
            )
        assertIs<Loaded>(state.status.value)

        parentSize = 250.dp

        assertIs<Loaded>(state.status.value)
        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        val entityAfterRecomposition =
            checkNotNull(
                composeTestRule.onSubspaceNodeWithTag("model").fetchSemanticsNode().semanticsEntity
            )
        assertIs<Loaded>(state.status.value)
        assertThat(entityBeforeRecomposition).isSameInstanceAs(entityAfterRecomposition)
    }

    // 5. Modifiers and Integration
    @Test
    fun spatialModel_withOffsetModifier_isPositionedCorrectly() {
        // Apply `SubspaceModifier.offset()` and assert that the `SpatialModel` is placed at the
        // correct offset relative to its parent.

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier =
                        SubspaceModifier.testTag("model").offset(x = 50.dp, y = 30.dp, z = 25.dp),
                )
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("model")
            .assertPositionIsEqualTo(expectedX = 50.dp, expectedY = 30.dp, expectedZ = 25.dp)
    }

    @Test
    fun spatialModel_withAlphaModifier_changesOpacity() {
        // Apply `SubspaceModifier.alpha()` and verify that the underlying `CoreModelEntity`'s alpha
        // property is updated.

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model").alpha(0.5f),
                )
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("model")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.5f)
    }

    @Test
    fun spatialModel_withContent_composesContent() {
        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model"),
                ) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("child")) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("model").assertExists()
        composeTestRule.onSubspaceNodeWithTag("child").assertExists()
    }

    @Test
    fun state_nodes_arePopulatedAfterLoad() {
        val completableDeferred = CompletableDeferred<GltfModelResource>()
        val state =
            SpatialGltfModelState(source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb")))

        val fakeRtNode = FakeGltfModelNodeFeature(name = "TestNode")

        composeTestRule.configureFakeSession(
            renderingRuntime = { runtime ->
                object : RenderingRuntime by runtime {
                    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource =
                        completableDeferred.await()

                    override fun createGltfEntity(
                        pose: Pose,
                        loadedGltf: GltfModelResource,
                        parentEntity: Entity?,
                    ): GltfEntity {
                        return object : FakeGltfEntity() {
                            override val nodes = listOf(fakeRtNode)
                        }
                    }
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialGltfModel(state = state, modifier = SubspaceModifier.testTag("model"))
            }
        }

        assertThat(state.nodes).isEmpty()

        completableDeferred.complete(object : GltfModelResource {})
        composeTestRule.waitForIdle()

        assertThat(state.nodes).hasSize(1)
        assertThat(state.nodes.first().name).isEqualTo("TestNode")
    }

    @Test
    fun spatialModel_withContent_centersContentByDefault() {

        composeTestRule.setContent {
            Subspace {
                // Create a model with a fixed size of 200.dp
                SpatialGltfModel(
                    state =
                        rememberSpatialGltfModelState(
                            source = SpatialGltfModelSource.fromPath(Paths.get("asset.glb"))
                        ),
                    modifier = SubspaceModifier.testTag("model").size(200.dp),
                ) {
                    // Place a smaller child (50.dp) inside it
                    SpatialPanel(modifier = SubspaceModifier.testTag("child").size(50.dp)) {}
                }
            }
        }

        // Since the parent (model) is at the root (0,0,0) and the child is centered
        // by default, the child's center should also be at (0,0,0).
        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }
}
