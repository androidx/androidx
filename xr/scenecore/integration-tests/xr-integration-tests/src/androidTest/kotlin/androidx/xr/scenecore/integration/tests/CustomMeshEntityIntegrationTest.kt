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

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.ByteBufferRegion
import androidx.xr.scenecore.CustomMesh
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.KhronosUnlitMaterial
import androidx.xr.scenecore.MeshBuffer
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.MeshSubset
import androidx.xr.scenecore.MeshSubsetTopology
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.VertexAttribute
import androidx.xr.scenecore.VertexAttributeType
import androidx.xr.scenecore.VertexLayout
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SnapshotXrSpace
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Integration tests for [CustomMesh], [MeshBuffer], [MeshEntity], [KhronosPbrMaterial],
 * [KhronosUnlitMaterial], and [Texture] (CUJ #35: b/581430543).
 *
 * Verified API & runtime notes on `androidx-main`:
 * - [Texture.create], [KhronosPbrMaterial.create], and [KhronosUnlitMaterial.create] enforce
 *   `Looper.getMainLooper().isCurrentThread` in `SpatialRenderingRuntime`, and SplitEngine /
 *   Impress mesh & material calls (`MeshBuffer`, `CustomMesh`, `MeshEntity.create`,
 *   `MeshEntity.setMaterial`, `MeshFeatureImpl.setReformAffordanceEnabled`) as well as Android
 *   `View` / [PanelEntity] creation are `@MainThread`, so only those calls run inside
 *   `sessionRule.onMainThread`. Entity transforms, parenting, enabled/alpha state,
 *   [InteractableComponent] attach/detach, and all assertions run on the test thread.
 * - [MeshEntity] uses Impress geometry affordances (`MeshFeatureImpl.setReformAffordanceEnabled`)
 *   rather than CPM `ReformOptions`, so [SnapshotXrSpace.isMovable], [SnapshotXrSpace.isResizable],
 *   and CPM `resizeAffordanceNodes` remain `false` / empty on `mesh_entity_subspace_*` nodes.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class CustomMeshEntityIntegrationTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun customMesh_builderFromMeshData_texturedPbrAndUnlitMaterials_createsAndUpdatesMeshEntity() {
        val session = sessionRule.session
        val layout = buildQuadVertexLayout()
        val vertexRegion = allocateQuadVertexRegion(halfExtent = 0.5f)
        val indexRegion = allocateQuadIndexRegion()
        val initialBounds =
            BoundingBox.fromMinMax(Vector3(-0.5f, -0.5f, 0f), Vector3(0.5f, 0.5f, 0f))

        val (pbrMaterial, unlitMaterial, customMesh, createdEntity) =
            sessionRule.onMainThread {
                val tex = Texture.create(session, Paths.get("textures", "pattern.png"))

                val pbr = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
                pbr.setBaseColorTexture(tex)
                pbr.setBaseColorFactor(Vector4(0.9f, 0.9f, 0.9f, 1.0f))
                pbr.setMetallicFactor(0.2f)
                pbr.setRoughnessFactor(0.6f)

                val unlit = KhronosUnlitMaterial.create(session, AlphaMode.BLEND)
                unlit.setBaseColorFactor(Vector4(0.2f, 0.8f, 0.4f, 0.85f))

                val mesh =
                    CustomMesh.BuilderFromMeshData(session, layout)
                        .addVertexData(vertexRegion)
                        .setIndexData(indexRegion)
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 6))
                        .setBounds(initialBounds)
                        .build()

                val entity =
                    MeshEntity.create(
                        session = session,
                        mesh = mesh,
                        materials = listOf(pbr),
                        pose = Pose(Vector3(0f, 0f, -1.5f)),
                        parent = session.scene.activitySpace,
                    )
                CreatedMeshFixture(pbr, unlit, mesh, entity)
            }

        assertThat(createdEntity.mesh).isSameInstanceAs(customMesh)
        assertThat(createdEntity.boneCount).isEqualTo(0)
        assertThat(createdEntity.materials).containsExactly(pbrMaterial)
        assertThat(customMesh.meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.STATIC)
        assertThat(customMesh.subsets).hasSize(1)
        awaitVisibleMeshSubspaceCount(expectedCount = 1)

        // Swap material and update mesh bounds via @MainThread Impress APIs.
        val expandedBounds =
            BoundingBox.fromMinMax(Vector3(-0.6f, -0.6f, -0.1f), Vector3(0.6f, 0.6f, 0.1f))
        sessionRule.onMainThread {
            createdEntity.setMaterial(unlitMaterial, subsetIndex = 0)
            customMesh.bounds = expandedBounds
        }
        assertThat(createdEntity.materials).containsExactly(unlitMaterial)
        assertThat(customMesh.bounds.min.x).isWithin(FLOAT_TOLERANCE).of(-0.6f)
        assertThat(customMesh.bounds.max.z).isWithin(FLOAT_TOLERANCE).of(0.1f)

        // Verify thread-safe Entity transforms, alpha, and enabled state on the test thread + CPM.
        val updatedPose =
            Pose(Vector3(0.25f, -0.1f, -1.75f), Quaternion.fromEulerAngles(0f, 10f, 0f))
        createdEntity.setPose(updatedPose, Space.PARENT)
        createdEntity.setScale(1.35f)
        createdEntity.setAlpha(0.6f)

        val actualPose = createdEntity.getPose(Space.PARENT)
        assertThat(actualPose.translation.x).isWithin(FLOAT_TOLERANCE).of(updatedPose.translation.x)
        assertThat(actualPose.translation.y).isWithin(FLOAT_TOLERANCE).of(updatedPose.translation.y)
        assertThat(actualPose.translation.z).isWithin(FLOAT_TOLERANCE).of(updatedPose.translation.z)
        assertThat(createdEntity.getScale()).isWithin(FLOAT_TOLERANCE).of(1.35f)
        assertThat(createdEntity.getAlpha()).isWithin(FLOAT_TOLERANCE).of(0.6f)

        createdEntity.setEnabled(false)
        assertThat(createdEntity.isEnabled(includeParents = false)).isFalse()
        awaitVisibleMeshSubspaceCount(expectedCount = 0)

        createdEntity.setEnabled(true)
        assertThat(createdEntity.isEnabled(includeParents = false)).isTrue()
        awaitVisibleMeshSubspaceCount(expectedCount = 1)
    }

    @Test
    fun customMesh_meshBufferMultiSubsetAndHierarchy_propagatesParentTransformsAndVisibility() {
        val session = sessionRule.session
        val layout = buildQuadVertexLayout()
        val vertexRegion = allocateQuadVertexRegion(halfExtent = 0.4f)
        val indexRegion = allocateQuadIndexRegion()
        val bounds = BoundingBox.fromMinMax(Vector3(-0.4f, -0.4f, 0f), Vector3(0.4f, 0.4f, 0f))

        val container =
            Entity.create(
                session = session,
                name = "meshHierarchyContainer",
                pose = Pose(Vector3(0.5f, 0f, -1.5f)),
                parent = session.scene.activitySpace,
            )
        container.setScale(1.5f)
        container.setAlpha(0.8f)

        val createdPanel = sessionRule.onMainThread {
            val panelView = View(sessionRule.activity)
            PanelEntity.create(
                session = session,
                view = panelView,
                pixelDimensions = IntSize2d(200, 200),
                name = "meshParentPanel",
                pose = Pose(Vector3(-0.5f, 0.25f, -1.6f)),
                parent = session.scene.activitySpace,
            )
        }

        val (material0, material1, meshBuffer, customMesh, createdMeshEntity) =
            sessionRule.onMainThread {
                val mat0 = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
                mat0.setBaseColorFactor(Vector4(1.0f, 0.2f, 0.2f, 1.0f))

                val mat1 = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
                mat1.setBaseColorFactor(Vector4(0.2f, 0.4f, 1.0f, 1.0f))

                val buffer =
                    MeshBuffer.create(
                        session = session,
                        vertexLayout = layout,
                        vertexData = listOf(vertexRegion),
                        indexData = indexRegion,
                    )

                val mesh =
                    CustomMesh.BuilderFromMeshBuffer(session, buffer)
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 3, 3))
                        .setBounds(bounds)
                        .build()

                val entity =
                    MeshEntity.create(
                        session = session,
                        mesh = mesh,
                        materials = listOf(mat0, mat1),
                        pose = Pose(Vector3(0.2f, 0f, 0f)),
                        parent = container,
                    )
                HierarchyMeshFixture(mat0, mat1, buffer, mesh, entity)
            }

        assertThat(meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.STATIC)
        assertThat(customMesh.subsets).hasSize(2)
        assertThat(createdMeshEntity.materials).containsExactly(material0, material1).inOrder()
        assertThat(createdMeshEntity.parent).isEqualTo(container)
        awaitVisibleMeshSubspaceCount(expectedCount = 1)

        // Scale and alpha propagate from Entity parent.
        assertThat(createdMeshEntity.getScale(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(1.5f)
        assertThat(createdMeshEntity.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.8f)
        assertThat(createdMeshEntity.getPose(Space.ACTIVITY).translation.x)
            .isWithin(FLOAT_TOLERANCE)
            .of(0.5f + 0.2f * 1.5f)

        container.setEnabled(false)
        assertThat(createdMeshEntity.isEnabled(includeParents = false)).isTrue()
        assertThat(createdMeshEntity.isEnabled(includeParents = true)).isFalse()
        awaitVisibleMeshSubspaceCount(expectedCount = 0)

        container.setEnabled(true)
        assertThat(createdMeshEntity.isEnabled(includeParents = true)).isTrue()
        awaitVisibleMeshSubspaceCount(expectedCount = 1)

        // Reparent under a PanelEntity and verify world pose + visibility propagation.
        createdMeshEntity.parent = createdPanel
        assertThat(createdMeshEntity.parent).isEqualTo(createdPanel)
        assertThat(createdMeshEntity.getPose(Space.ACTIVITY).translation.x)
            .isWithin(FLOAT_TOLERANCE)
            .of(-0.5f + 0.2f)
        assertThat(createdMeshEntity.getPose(Space.ACTIVITY).translation.y)
            .isWithin(FLOAT_TOLERANCE)
            .of(0.25f)

        createdPanel.setEnabled(false)
        assertThat(createdMeshEntity.isEnabled(includeParents = true)).isFalse()
        awaitVisibleMeshSubspaceCount(expectedCount = 0)

        createdPanel.setEnabled(true)
        assertThat(createdMeshEntity.isEnabled(includeParents = true)).isTrue()
        awaitVisibleMeshSubspaceCount(expectedCount = 1)
    }

    @Test
    fun customMesh_interactableComponent_receivesHoverClickAndSuppressesWhenDisabledOrDetached() {
        val session = sessionRule.session
        val frontEvents = Collections.synchronizedList(mutableListOf<InputEvent>())
        val frontHoverExitLatch = CountDownLatch(1)
        val frontDownCount = AtomicInteger(0)
        val sentinelDownCount = AtomicInteger(0)
        val sentinelLatch1 = CountDownLatch(1)
        val sentinelLatch2 = CountDownLatch(1)
        val frontReenableLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val layout = buildQuadVertexLayout()
        val (front, sentinel) =
            sessionRule.onMainThread {
                val fMat = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
                fMat.setBaseColorFactor(Vector4(0.1f, 0.8f, 0.9f, 1.0f))

                val sMat = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
                sMat.setBaseColorFactor(Vector4(0.9f, 0.2f, 0.2f, 1.0f))

                val fMesh =
                    CustomMesh.BuilderFromMeshData(session, layout)
                        .addVertexData(allocateQuadVertexRegion(halfExtent = 0.6f))
                        .setIndexData(allocateQuadIndexRegion())
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 6))
                        .setBounds(
                            BoundingBox.fromMinMax(
                                Vector3(-0.6f, -0.6f, 0f),
                                Vector3(0.6f, 0.6f, 0f),
                            )
                        )
                        .build()

                val sMesh =
                    CustomMesh.BuilderFromMeshData(session, layout)
                        .addVertexData(allocateQuadVertexRegion(halfExtent = 1.0f))
                        .setIndexData(allocateQuadIndexRegion())
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 6))
                        .setBounds(
                            BoundingBox.fromMinMax(
                                Vector3(-1.0f, -1.0f, 0f),
                                Vector3(1.0f, 1.0f, 0f),
                            )
                        )
                        .build()

                val fEntity =
                    MeshEntity.create(
                        session = session,
                        mesh = fMesh,
                        materials = listOf(fMat),
                        pose = Pose(Vector3(0f, 0f, -1.5f)),
                        parent = session.scene.activitySpace,
                    )

                val sEntity =
                    MeshEntity.create(
                        session = session,
                        mesh = sMesh,
                        materials = listOf(sMat),
                        pose = Pose(Vector3(0f, 0f, -2.0f)),
                        parent = session.scene.activitySpace,
                    )
                fEntity to sEntity
            }

        val frontInteractable =
            InteractableComponent.create(session = session) { event ->
                frontEvents.add(event)
                if (event.action == InputEvent.Action.HOVER_EXIT) {
                    frontHoverExitLatch.countDown()
                }
                if (event.action == InputEvent.Action.DOWN) {
                    if (frontDownCount.incrementAndGet() == 2) {
                        frontReenableLatch.countDown()
                    }
                }
            }
        val sentinelInteractable =
            InteractableComponent.create(session = session) { event ->
                if (event.action == InputEvent.Action.DOWN) {
                    val count = sentinelDownCount.incrementAndGet()
                    if (count == 1) sentinelLatch1.countDown()
                    if (count == 2) sentinelLatch2.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val (frontAdded, sentinelAdded) =
            sessionRule.onMainThread {
                front.addComponent(frontInteractable) to sentinel.addComponent(sentinelInteractable)
            }
        assertThat(frontAdded).isTrue()
        assertThat(sentinelAdded).isTrue()

        val initialTask = awaitVisibleMeshSubspaceCount(expectedCount = 2)
        val task =
            if (isProtoSceneCaptureEnabled) {
                val (settledTask, meshSubspaces) =
                    checkNotNull(
                        waitForSpatialScene(
                            description =
                                "interactable MeshEntity BoxCollider worldExtents populated",
                            timeout = SCENE_TIMEOUT,
                            select = { scene ->
                                val candidateTask = scene.findTaskByActivity(sessionRule.activity)
                                val subspaces =
                                    candidateTask
                                        ?.subspaceNodes
                                        ?.filter {
                                            it.name.startsWith(MESH_SUBSPACE_PREFIX) && it.isVisible
                                        }
                                        .orEmpty()
                                candidateTask?.let { it to subspaces }
                            },
                            until = { pair ->
                                pair != null &&
                                    pair.second.size == 2 &&
                                    pair.second.all { subspace ->
                                        val extents = subspace.worldExtents
                                        extents != null && extents.x > 0f && extents.y > 0f
                                    }
                            },
                        )
                    )
                for (sub in meshSubspaces) {
                    val extents = checkNotNull(sub.worldExtents)
                    assertWithMessage(
                            "Expected square BoxCollider worldExtents on ${sub.name}, " +
                                "got $extents (children=${sub.children.map { it.name }})"
                        )
                        .that(extents.x / extents.y)
                        .isWithin(0.02f)
                        .of(1.0f)
                }
                val widths = meshSubspaces.map { checkNotNull(it.worldExtents).x }.sorted()
                assertWithMessage(
                        "Expected sentinel/front BoxCollider width ratio ~ 2.0/1.2, got $widths"
                    )
                    .that(widths[1] / widths[0])
                    .isWithin(0.05f)
                    .of(2.0f / 1.2f)
                settledTask
            } else {
                initialTask
            }

        // Phase 1: Hover + drag click on front mesh, then aim off mesh for HOVER_EXIT.
        val startPoint = Vector3(0f, 0f, -1.5f)
        val dragPoint = Vector3(0.2f, 0.1f, -1.5f)
        task.performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = startPoint)
            press()
            animate(durationMs = 150L) { fraction ->
                pointAt(offset = Vector3.lerp(startPoint, dragPoint, fraction))
            }
            release()
            pointAt(offset = Vector3(3.0f, 3.0f, -1.5f))
        }
        assertWithMessage("Timed out waiting for HOVER_EXIT on front MeshEntity")
            .that(frontHoverExitLatch.await(5, TimeUnit.SECONDS))
            .isTrue()

        val recordedActions = synchronized(frontEvents) { frontEvents.map { it.action } }
        assertThat(recordedActions)
            .containsAtLeast(
                InputEvent.Action.HOVER_ENTER,
                InputEvent.Action.DOWN,
                InputEvent.Action.MOVE,
                InputEvent.Action.UP,
                InputEvent.Action.HOVER_EXIT,
            )
        val downEvent =
            synchronized(frontEvents) { frontEvents.first { it.action == InputEvent.Action.DOWN } }
        val hitInfo = downEvent.hitInfoList.firstOrNull { it.inputEntity == front }
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo!!.hitPosition?.z).isWithin(0.2f).of(-1.5f)
        assertThat(sentinelDownCount.get()).isEqualTo(0)

        // Phase 2: Disable front MeshEntity and await CPM visibility update before firing ray.
        front.setEnabled(false)
        val taskAfterDisable = awaitVisibleMeshSubspaceCount(expectedCount = 1)
        taskAfterDisable.performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = startPoint)
            select()
        }
        assertThat(sentinelLatch1.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(frontDownCount.get()).isEqualTo(1)

        // Phase 3: Re-enable front MeshEntity so it intercepts again.
        front.setEnabled(true)
        val taskAfterReenable = awaitVisibleMeshSubspaceCount(expectedCount = 2)
        taskAfterReenable.performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = startPoint)
            select()
        }
        assertThat(frontReenableLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(sentinelDownCount.get()).isEqualTo(1)

        // Phase 4: Drain queued Main-thread input before and after detaching InteractableComponent.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        sessionRule.onMainThread { front.removeComponent(frontInteractable) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        taskAfterReenable.performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = startPoint)
            select()
        }
        assertThat(sentinelLatch2.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(frontDownCount.get()).isEqualTo(2)
        assertThat(sentinelDownCount.get()).isEqualTo(2)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun customMesh_movableAndResizableComponents_attachesConfiguresAndDetachesCleanly() {
        val session = sessionRule.session
        val moveCount = AtomicInteger(0)
        val postDetachUpLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val layout = buildQuadVertexLayout()
        val created = sessionRule.onMainThread {
            val mat = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
            mat.setBaseColorFactor(Vector4(0.95f, 0.65f, 0.1f, 1.0f))

            val mesh =
                CustomMesh.BuilderFromMeshData(session, layout)
                    .addVertexData(allocateQuadVertexRegion(halfExtent = 0.7f))
                    .setIndexData(allocateQuadIndexRegion())
                    .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 6))
                    .setBounds(
                        BoundingBox.fromMinMax(
                            Vector3(-0.7f, -0.7f, 0f),
                            Vector3(0.7f, 0.7f, 0f),
                        )
                    )
                    .build()

            MeshEntity.create(
                session = session,
                mesh = mesh,
                materials = listOf(mat),
                pose = Pose(Vector3(0f, 0f, -1.5f)),
                parent = session.scene.activitySpace,
            )
        }

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)
        movableComponent.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveStart(
                    entity: Entity,
                    initialInputRay: Ray,
                    initialPose: Pose,
                    initialScale: Float,
                    initialParent: Entity,
                ) {
                    moveCount.incrementAndGet()
                }

                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) = Unit
            }
        )
        // Verify ResizableComponent attaches alongside MovableComponent and accepts config.
        val resizableComponent =
            ResizableComponent.create(
                session = session,
                minimumSize = FloatSize3d(0.4f, 0.4f, 0.4f),
                maximumSize = FloatSize3d(3.0f, 3.0f, 3.0f),
                resizeEventListener = Consumer { _ -> },
            )
        // MeshFeatureImpl.setReformAffordanceEnabled is @MainThread on SplitEngine Impress.
        val (movableAdded, resizableAdded) =
            sessionRule.onMainThread {
                created.addComponent(movableComponent) to created.addComponent(resizableComponent)
            }
        assertThat(movableAdded).isTrue()
        assertThat(resizableAdded).isTrue()
        resizableComponent.affordanceSize = FloatSize3d(1.4f, 1.4f, 0.0f)
        resizableComponent.isFixedAspectRatioEnabled = true

        val initialTask = awaitVisibleMeshSubspaceCount(expectedCount = 1)
        val attachedSubspace = findSingleVisibleMeshSubspace(initialTask)
        // MeshEntity uses Impress geometry affordances rather than CPM ReformOptions.
        assertThat(attachedSubspace.isVisible).isTrue()
        assertThat(attachedSubspace.isMovable).isFalse()
        assertThat(attachedSubspace.isResizable).isFalse()
        assertThat(attachedSubspace.moveAffordanceNodes).isEmpty()
        assertThat(attachedSubspace.resizeAffordanceNodes).isEmpty()

        val initialPos = created.getPose(Space.ACTIVITY).translation

        // Detach reform components and verify CPM subspace node state + post-detach zero-move
        // suppression (active MovableComponent footprint drag is tracked in b/584908075).
        sessionRule.onMainThread {
            created.removeComponent(movableComponent)
            created.removeComponent(resizableComponent)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val (postDetachTask, postDetachSubspace) =
            checkNotNull(
                waitForSpatialScene(
                    description = "MeshEntity subspace settled after detaching reform components",
                    timeout = SCENE_TIMEOUT,
                    select = { scene ->
                        val candidateTask = scene.findTaskByActivity(sessionRule.activity)
                        val subspace = candidateTask?.let(::findSingleVisibleMeshSubspaceOrNull)
                        if (candidateTask != null && subspace != null) {
                            candidateTask to subspace
                        } else {
                            null
                        }
                    },
                    until = { pair ->
                        pair != null &&
                            !pair.second.isMovable &&
                            !pair.second.isResizable &&
                            (!isProtoSceneCaptureEnabled ||
                                pair.second.subspaceBoundsInMeters == null)
                    },
                )
            )
        assertThat(postDetachSubspace.isVisible).isTrue()
        assertThat(postDetachSubspace.isMovable).isFalse()
        assertThat(postDetachSubspace.isResizable).isFalse()
        if (isProtoSceneCaptureEnabled) {
            assertThat(postDetachSubspace.subspaceBoundsInMeters).isNull()
        }

        val interactable =
            InteractableComponent.create(session = session) { event ->
                if (event.action == InputEvent.Action.UP) {
                    postDetachUpLatch.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val postDetachAdded = sessionRule.onMainThread { created.addComponent(interactable) }
        assertThat(postDetachAdded).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val targetTask =
            if (isProtoSceneCaptureEnabled) {
                val (settledTask, interactableSubspace) =
                    checkNotNull(
                        waitForSpatialScene(
                            description =
                                "post-detach InteractableComponent BoxCollider worldExtents populated",
                            timeout = SCENE_TIMEOUT,
                            select = { scene ->
                                val candidateTask = scene.findTaskByActivity(sessionRule.activity)
                                val subspace =
                                    candidateTask?.let(::findSingleVisibleMeshSubspaceOrNull)
                                if (candidateTask != null && subspace != null) {
                                    candidateTask to subspace
                                } else {
                                    null
                                }
                            },
                            until = { pair ->
                                val extents = pair?.second?.worldExtents
                                extents != null && extents.x > 0f && extents.y > 0f
                            },
                        )
                    )
                val extents = checkNotNull(interactableSubspace.worldExtents)
                assertWithMessage(
                        "Expected square BoxCollider worldExtents on " +
                            "${interactableSubspace.name}, got $extents " +
                            "(children=${interactableSubspace.children.map { it.name }})"
                    )
                    .that(extents.x / extents.y)
                    .isWithin(0.02f)
                    .of(1.0f)
                settledTask
            } else {
                postDetachTask
            }

        val postDetachEnd = initialPos + Vector3(-0.35f, 0f, 0f)
        targetTask.performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = initialPos)
            press()
            animate(durationMs = 150L) { fraction ->
                pointAt(offset = Vector3.lerp(initialPos, postDetachEnd, fraction))
            }
            release()
            pointAt(offset = Vector3(3.0f, 3.0f, -1.5f))
        }
        assertThat(postDetachUpLatch.await(5, TimeUnit.SECONDS)).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(moveCount.get()).isEqualTo(0)
        assertThat((created.getPose(Space.ACTIVITY).translation - initialPos).length)
            .isLessThan(1e-3f)
    }

    // TODO(b/584908075): Remove @Ignore once fixed.
    @Ignore(
        "b/584908075: SplitEngineMeshCollider raycasts unskinned footprint.glb, blocking MeshEntity MovableComponent translation"
    )
    @Test
    fun customMesh_movableComponent_movesEntityOnFootprintDrag() {
        val session = sessionRule.session
        val moveEndLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val layout = buildQuadVertexLayout()
        val created = sessionRule.onMainThread {
            val mat = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
            mat.setBaseColorFactor(Vector4(0.95f, 0.65f, 0.1f, 1.0f))

            val mesh =
                CustomMesh.BuilderFromMeshData(session, layout)
                    .addVertexData(allocateQuadVertexRegion(halfExtent = 0.7f))
                    .setIndexData(allocateQuadIndexRegion())
                    .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 6))
                    .setBounds(
                        BoundingBox.fromMinMax(
                            Vector3(-0.7f, -0.7f, 0f),
                            Vector3(0.7f, 0.7f, 0f),
                        )
                    )
                    .build()

            MeshEntity.create(
                session = session,
                mesh = mesh,
                materials = listOf(mat),
                pose = Pose(Vector3(0f, 0f, -1.5f)),
                parent = session.scene.activitySpace,
            )
        }

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)
        movableComponent.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    moveEndLatch.countDown()
                }
            }
        )
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val movableAdded = sessionRule.onMainThread { created.addComponent(movableComponent) }
        assertThat(movableAdded).isTrue()

        val task = awaitVisibleMeshSubspaceCount(expectedCount = 1)
        val activitySpaceNode = checkNotNull(task.findNodeByName(ACTIVITY_SPACE_NODE_NAME))
        val initialPos = created.getPose(Space.ACTIVITY).translation
        val dragStart = Vector3(0f, -0.70f, -1.5f)
        val dragEnd = Vector3(0.40f, -0.55f, -1.5f)
        activitySpaceNode.performSpatialInteraction {
            moveOriginTo(Vector3(dragStart.x, dragStart.y, 0f))
            pointAt(offset = dragStart)
            press()
            animate(durationMs = 200L) { fraction ->
                val p = Vector3.lerp(dragStart, dragEnd, fraction)
                moveOriginTo(Vector3(p.x, p.y, 0f))
                pointAt(offset = p)
            }
            release()
        }

        assertWithMessage("Timed out waiting for MoveEvent.onMoveEnd on MeshEntity")
            .that(moveEndLatch.await(3, TimeUnit.SECONDS))
            .isTrue()
        val movedPos = created.getPose(Space.ACTIVITY).translation
        assertThat((movedPos - initialPos).length).isGreaterThan(0.1f)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    fun customMesh_dynamicMeshBuffer_updatesVerticesAndBounds() {
        val session = sessionRule.session
        val sentinelDownCount = AtomicInteger(0)
        val meshDownCount = AtomicInteger(0)
        val sentinelLatch = CountDownLatch(1)
        val meshLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val layout = buildQuadVertexLayout()
        val initialVertices = allocateQuadVertexRegion(halfExtent = 0.35f)
        val updatedVertices = allocateQuadVertexRegion(halfExtent = 0.7f)
        val stripIndices = allocateStripIndexRegion()
        val flippedStripIndices = allocateFlippedStripIndexRegion()
        val initialBounds =
            BoundingBox.fromMinMax(Vector3(-0.35f, -0.35f, 0f), Vector3(0.35f, 0.35f, 0f))
        val updatedBounds =
            BoundingBox.fromMinMax(Vector3(-0.7f, -0.7f, 0f), Vector3(0.7f, 0.7f, 0f))

        val (meshBuffer, customMesh, created, sentinel) =
            sessionRule.onMainThread {
                val mat = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
                mat.setBaseColorFactor(Vector4(0.3f, 0.9f, 0.5f, 1.0f))

                val sMat = KhronosUnlitMaterial.create(session, AlphaMode.OPAQUE)
                sMat.setBaseColorFactor(Vector4(0.9f, 0.2f, 0.2f, 1.0f))

                val dynBuffer =
                    MeshBuffer.createDynamic(
                        session = session,
                        vertexLayout = layout,
                        vertexCount = 4,
                        indexCount = 4,
                        vertexData = listOf(initialVertices),
                        indexData = stripIndices,
                    )

                val mesh =
                    CustomMesh.BuilderFromMeshBuffer(session, dynBuffer)
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLE_STRIP, 0, 4))
                        .setBounds(initialBounds)
                        .build()

                val sMesh =
                    CustomMesh.BuilderFromMeshData(session, layout)
                        .addVertexData(allocateQuadVertexRegion(halfExtent = 1.0f))
                        .setIndexData(allocateQuadIndexRegion())
                        .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 6))
                        .setBounds(
                            BoundingBox.fromMinMax(
                                Vector3(-1.0f, -1.0f, 0f),
                                Vector3(1.0f, 1.0f, 0f),
                            )
                        )
                        .build()

                val dynEntity =
                    MeshEntity.create(
                        session = session,
                        mesh = mesh,
                        materials = listOf(mat),
                        pose = Pose(Vector3(0f, 0f, -1.5f)),
                        parent = session.scene.activitySpace,
                    )

                val sEntity =
                    MeshEntity.create(
                        session = session,
                        mesh = sMesh,
                        materials = listOf(sMat),
                        pose = Pose(Vector3(0f, 0f, -2.0f)),
                        parent = session.scene.activitySpace,
                    )
                DynamicMeshFixture(dynBuffer, mesh, dynEntity, sEntity)
            }
        assertThat(meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.DYNAMIC)
        assertThat(customMesh.subsets[0].topology).isEqualTo(MeshSubsetTopology.TRIANGLE_STRIP)

        val meshInteractable =
            InteractableComponent.create(session = session) { event ->
                if (event.action == InputEvent.Action.DOWN) {
                    meshDownCount.incrementAndGet()
                    meshLatch.countDown()
                }
            }
        val sentinelInteractable =
            InteractableComponent.create(session = session) { event ->
                if (event.action == InputEvent.Action.DOWN) {
                    sentinelDownCount.incrementAndGet()
                    sentinelLatch.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val (meshAdded, sentinelAdded) =
            sessionRule.onMainThread {
                created.addComponent(meshInteractable) to
                    sentinel.addComponent(sentinelInteractable)
            }
        assertThat(meshAdded).isTrue()
        assertThat(sentinelAdded).isTrue()

        val task = awaitVisibleMeshSubspaceCount(expectedCount = 2)
        val activitySpaceNode = checkNotNull(task.findNodeByName(ACTIVITY_SPACE_NODE_NAME))

        // Before vertex expansion (halfExtent = 0.35f), x = 0.55f misses the front mesh and
        // hits the sentinel behind it at z = -2.0f.
        val probeOrigin = Vector3(0.55f, 0f, 0f)
        val probeTarget = Vector3(0.55f, 0f, -1.5f)
        activitySpaceNode.performSpatialInteraction {
            moveOriginTo(probeOrigin)
            pointAt(offset = probeTarget)
            select()
        }
        assertWithMessage("Expected initial probe ray at x=0.55f to reach sentinel behind mesh")
            .that(sentinelLatch.await(5, TimeUnit.SECONDS))
            .isTrue()
        assertThat(meshDownCount.get()).isEqualTo(0)

        sessionRule.onMainThread {
            meshBuffer.updateVertexData(bufferIndex = 0, vertexData = updatedVertices)
            meshBuffer.updateIndexData(indexData = flippedStripIndices)
            customMesh.bounds = updatedBounds
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(customMesh.bounds.max.x).isWithin(FLOAT_TOLERANCE).of(0.7f)
        assertThat(customMesh.bounds.min.y).isWithin(FLOAT_TOLERANCE).of(-0.7f)

        // After vertex expansion (halfExtent = 0.7f), the same x = 0.55f ray intercepts the
        // front dynamic mesh and occludes the sentinel behind it.
        activitySpaceNode.performSpatialInteraction {
            moveOriginTo(probeOrigin)
            pointAt(offset = probeTarget)
            select()
        }
        assertWithMessage("Expected expanded dynamic mesh to intercept probe ray at x=0.55f")
            .that(meshLatch.await(5, TimeUnit.SECONDS))
            .isTrue()
        assertThat(sentinelDownCount.get()).isEqualTo(1)
    }

    private fun buildQuadVertexLayout(): VertexLayout =
        VertexLayout.Builder()
            .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
            .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
            .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
            .build()

    private fun allocateQuadVertexRegion(halfExtent: Float): ByteBufferRegion {
        // 4 vertices * (3 pos + 3 normal + 2 uv) * 4 bytes = 128 bytes, CCW front-facing +Z.
        val byteCount = 4 * 8 * Float.SIZE_BYTES
        val buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()
        floatBuffer.put(
            floatArrayOf(
                -halfExtent,
                -halfExtent,
                0f,
                0f,
                0f,
                1f,
                0f,
                1f,
                halfExtent,
                -halfExtent,
                0f,
                0f,
                0f,
                1f,
                1f,
                1f,
                halfExtent,
                halfExtent,
                0f,
                0f,
                0f,
                1f,
                1f,
                0f,
                -halfExtent,
                halfExtent,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
            )
        )
        return ByteBufferRegion(buffer, 0, byteCount)
    }

    private fun allocateQuadIndexRegion(): ByteBufferRegion {
        val indices = intArrayOf(0, 1, 2, 0, 2, 3)
        val byteCount = indices.size * Int.SIZE_BYTES
        val buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(indices)
        return ByteBufferRegion(buffer, 0, byteCount)
    }

    private fun allocateStripIndexRegion(): ByteBufferRegion {
        val indices = intArrayOf(0, 1, 3, 2)
        val byteCount = indices.size * Int.SIZE_BYTES
        val buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(indices)
        return ByteBufferRegion(buffer, 0, byteCount)
    }

    private fun allocateFlippedStripIndexRegion(): ByteBufferRegion {
        val indices = intArrayOf(1, 2, 0, 3)
        val byteCount = indices.size * Int.SIZE_BYTES
        val buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(indices)
        return ByteBufferRegion(buffer, 0, byteCount)
    }

    private fun awaitVisibleMeshSubspaceCount(expectedCount: Int): SnapshotXrSpace =
        checkNotNull(
            waitForSpatialScene(
                description = "visible MeshEntity subspace node count == $expectedCount",
                timeout = SCENE_TIMEOUT,
                select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
                until = { task ->
                    task != null &&
                        task.subspaceNodes.count {
                            it.name.startsWith(MESH_SUBSPACE_PREFIX) && it.isVisible
                        } == expectedCount
                },
            )
        )

    private fun findSingleVisibleMeshSubspace(task: SnapshotXrSpace): SnapshotXrSpace =
        checkNotNull(findSingleVisibleMeshSubspaceOrNull(task)) {
            "Expected single visible $MESH_SUBSPACE_PREFIX node in task ${task.name}"
        }

    private fun findSingleVisibleMeshSubspaceOrNull(task: SnapshotXrSpace): SnapshotXrSpace? =
        task.subspaceNodes.singleOrNull { it.name.startsWith(MESH_SUBSPACE_PREFIX) && it.isVisible }

    private data class CreatedMeshFixture(
        val pbrMaterial: KhronosPbrMaterial,
        val unlitMaterial: KhronosUnlitMaterial,
        val customMesh: CustomMesh,
        val entity: MeshEntity,
    )

    private data class HierarchyMeshFixture(
        val material0: KhronosUnlitMaterial,
        val material1: KhronosUnlitMaterial,
        val meshBuffer: MeshBuffer,
        val customMesh: CustomMesh,
        val entity: MeshEntity,
    )

    private data class DynamicMeshFixture(
        val meshBuffer: MeshBuffer,
        val customMesh: CustomMesh,
        val entity: MeshEntity,
        val sentinel: MeshEntity,
    )

    private companion object {
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val FLOAT_TOLERANCE = 1e-3f
        const val MESH_SUBSPACE_PREFIX = "mesh_entity_subspace_"
        val SCENE_TIMEOUT = 5.seconds
    }
}
