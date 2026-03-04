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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.hardware.display.DisplayManager
import android.view.View
import android.view.ViewGroup
import androidx.test.rule.GrantPermissionRule
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Matrix4.Companion.fromPose
import androidx.xr.runtime.math.Matrix4.Companion.fromScale
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.MoveEventListener
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeGltfFeature.Companion.createWithMockFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.InputEvent
import com.android.extensions.xr.node.Mat4f
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.android.extensions.xr.node.Quatf
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.ReformOptions
import com.android.extensions.xr.node.ShadowInputEvent
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.ShadowNodeTransform
import com.android.extensions.xr.node.ShadowReformEvent
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Expect
import com.google.common.truth.Truth
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MovableComponentImplTest {
    @Rule @JvmField val expect: Expect = Expect.create()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val xrExtensions = getXrExtensions()!!
    private val entityManager = EntityManager()
    private val mockPanelShadowRenderer = mock<EntityShadowRenderer>()
    private val nodeRepository: NodeRepository = NodeRepository.getInstance()

    @Rule
    @JvmField
    var grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING")

    private lateinit var sceneRuntime: SpatialSceneRuntime
    private lateinit var activitySpaceImpl: ActivitySpaceImpl
    private lateinit var activitySpaceNode: Node
    private val mockGltfFeature: GltfFeature = mock<GltfFeature>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        sceneRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, entityManager)
        activitySpaceImpl = sceneRuntime.activitySpace as ActivitySpaceImpl
        activitySpaceNode = activitySpaceImpl.mNode
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        sceneRuntime.destroy()
        Dispatchers.resetMain()
    }

    private fun createTestEntity(): Entity {
        return sceneRuntime.createEntity(Pose(), "test", sceneRuntime.activitySpace)
    }

    private fun createTestPanelEntity(): PanelEntity {
        val display = activity.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.setLayoutParams(ViewGroup.LayoutParams(640, 480))
        val node = xrExtensions.createNode()

        val panelEntity =
            PanelEntityImpl(
                displayContext,
                node,
                view,
                xrExtensions,
                entityManager,
                PixelDimensions(10, 10),
                "panelShadow",
                fakeExecutor,
            )
        panelEntity.parent = activitySpaceImpl
        return panelEntity
    }

    private fun setActivitySpacePose(pose: Pose, scale: Float = 1f) {
        val poseMatrix = fromPose(pose)
        val scaleMatrix = fromScale(scale)
        val scaledPoseMatrix = poseMatrix.times(scaleMatrix)
        val mat4f = Mat4f(scaledPoseMatrix.data)
        val nodeTransformEvent = ShadowNodeTransform.create(mat4f)

        val shadowNode = ShadowNode.extract(activitySpaceNode)
        shadowNode.transformExecutor.execute {
            shadowNode.transformListener.accept(nodeTransformEvent)
        }
        fakeExecutor.runAll()
    }

    private fun getEntityNode(entity: Entity): Node {
        return (entity as AndroidXrEntity).mNode
    }

    private fun sendReformEvent(node: Node, reformEvent: ReformEvent) {
        val options = nodeRepository.getReformOptions(node)
        options.eventExecutor.execute { options.eventCallback.accept(reformEvent) }
        Truth.assertThat(fakeExecutor.hasNext()).isTrue()
        fakeExecutor.runAll()
    }

    private fun sendInputEvent(node: ShadowNode, inputEvent: InputEvent?) {
        node.inputExecutor.execute { node.inputListener.accept(inputEvent) }
        fakeExecutor.runAll()
    }

    @Test
    fun addMovableComponent_addsReformOptionsToNode() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = false,
                scaleInZ = false,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))

        Truth.assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_MOVE)
        Truth.assertThat(options.flags).isEqualTo(ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT)
        Truth.assertThat(options.eventCallback).isNotNull()
        Truth.assertThat(options.eventExecutor).isNotNull()
    }

    private fun createGltfEntity(activity: Activity): GltfEntityImpl {
        val nodeHolder: NodeHolder<*> =
            NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val fakeGltfFeature = createWithMockFeature(mockGltfFeature, nodeHolder)

        return GltfEntityImpl(
            activity,
            fakeGltfFeature,
            activitySpaceImpl,
            xrExtensions,
            entityManager,
            fakeExecutor,
        )
    }

    @Test
    fun addSystemMovableComponentToGltfEntity_returnsTrue() {
        val entity: GltfEntity = createGltfEntity(activity)
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()

        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun addCustomMovableComponentToGltfEntity_returnsTrue() {
        val entity: GltfEntity = createGltfEntity(activity)
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = false,
                scaleInZ = false,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()

        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun addAnchorableMovableComponentToGltfEntity_returnsTrue() {
        val entity: GltfEntity = createGltfEntity(activity)
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()

        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun addMovableComponentToGltfEntity_ReparentsGltfEntity() {
        val gltfEntity: GltfEntity = createGltfEntity(activity)
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()

        val entity = gltfEntity as AndroidXrEntity
        // Cache parent before adding the component
        val parentBefore = nodeRepository.getParent(entity.mNode)
        // Assert the component has be added.
        Truth.assertThat(gltfEntity.addComponent(movableComponent)).isTrue()
        // Get parent after adding the component
        val parentAfter = nodeRepository.getParent(entity.mNode)
        // Assert it has been reparented by Impress.
        Truth.assertThat(parentBefore !== parentAfter).isTrue()
    }

    @Test
    fun movableComponentAttachedToGltf_propagatesInputEvents() {
        val gltfEntity: GltfEntity = createGltfEntity(activity)
        val moveEvent = AtomicReference<MoveEvent>(null)
        val moveEventCounter = AtomicInteger(0)
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = false,
                scaleInZ = false,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        val eventListener = MoveEventListener { event: MoveEvent ->
            moveEvent.set(event)
            moveEventCounter.incrementAndGet()
        }
        movableComponent.addMoveEventListener(eventListener)

        val entity = gltfEntity as AndroidXrEntity?
        val expectedTransform =
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)
        val transform = Mat4f(expectedTransform)
        val hitPosition = Vec3(1f, 2f, 3f)
        val extensionHitInfo =
            InputEvent.HitInfo(1, checkNotNull(entity).mNode, transform, hitPosition)

        val inputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                extensionHitInfo,
                extensionHitInfo,
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_DOWN,
            )
        Truth.assertThat(gltfEntity.addComponent(movableComponent)).isTrue()

        val shadowNode = ShadowNode.extract(checkNotNull(entity).mNode)
        Truth.assertThat(shadowNode.inputListener).isNotNull()
        Truth.assertThat(shadowNode.inputExecutor).isEqualTo(fakeExecutor)
        shadowNode.inputExecutor.execute { shadowNode.inputListener.accept(inputEvent) }

        fakeExecutor.runAll()
        Truth.assertThat(moveEventCounter.get() == 1).isTrue()
        Truth.assertThat(moveEvent.get()).isNotNull()
    }

    @Test
    fun addMovableComponent_addsSystemMovableFlagToNode() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))

        Truth.assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_MOVE)
        Truth.assertThat(options.flags)
            .isEqualTo(
                ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT or
                    ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT
            )
    }

    @Test
    fun addMovableComponent_addsScaleInZFlagToNode() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = false,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))

        Truth.assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_MOVE)
        Truth.assertThat(options.flags)
            .isEqualTo(
                ReformOptions.FLAG_SCALE_WITH_DISTANCE or ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT
            )
    }

    @Test
    fun addMovableComponent_addsAllFlagsToNode() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))

        Truth.assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_MOVE)
        Truth.assertThat(options.flags)
            .isEqualTo(
                (ReformOptions.FLAG_SCALE_WITH_DISTANCE or
                    ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT or
                    ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT)
            )
    }

    @Test
    fun setSystemMovableFlag_alsoUpdatesEntityPoseAndScale() {
        val entity = createTestEntity() as AndroidXrEntity
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )

        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()

        val expectedPose = Pose(Vector3(2f, 2f, 2f), Quaternion(0.5f, 0.5f, 0.5f, 0.5f))
        val expectedScale = Vector3(1.2f, 1.2f, 1.2f)

        entity.setPose(Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 0f, 0f, 1f)))
        entity.setScale(Vector3(1f, 1f, 1f))

        val reformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ 0,
                /* id= */ 0,
            )
        val shadowReformEvent = ShadowReformEvent.extract(reformEvent)
        shadowReformEvent.setProposedPosition(Vec3(2f, 2f, 2f))
        shadowReformEvent.setProposedOrientation(Quatf(0.5f, 0.5f, 0.5f, 0.5f))
        shadowReformEvent.setProposedScale(Vec3(1.2f, 1.2f, 1.2f))

        sendReformEvent(entity.mNode, reformEvent)

        expect.that(entity.getPose()).isEqualTo(expectedPose)
        expect.that(entity.getScale()).isEqualTo(expectedScale)
    }

    @Test
    fun systemMovableFlagNotSet_doesNotUpdateEntityPoseAndScale() {
        val entity = createTestEntity() as AndroidXrEntity
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = false,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )

        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()

        val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 0f, 0f, 1f))
        val expectedScale = Vector3(1f, 1f, 1f)
        entity.setPose(Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 0f, 0f, 1f)))
        entity.setScale(Vector3(1f, 1f, 1f))

        val reformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ 0,
                /* id= */ 0,
            )
        val shadowReformEvent = ShadowReformEvent.extract(reformEvent)
        shadowReformEvent.setProposedPosition(Vec3(2f, 2f, 2f))
        shadowReformEvent.setProposedOrientation(Quatf(0.5f, 0.5f, 0.5f, 0.5f))
        shadowReformEvent.setProposedScale(Vec3(1.2f, 1.2f, 1.2f))

        sendReformEvent(entity.mNode, reformEvent)

        expect.that(entity.getPose()).isEqualTo(expectedPose)
        expect.that(entity.getScale()).isEqualTo(expectedScale)
    }

    @Test
    fun setSizeOnMovableComponent_setsSizeOnNodeReformOptions() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))

        movableComponent.size = Dimensions(2f, 2f, 2f)
        Truth.assertThat(options.currentSize.x).isEqualTo(2f)
        Truth.assertThat(options.currentSize.y).isEqualTo(2f)
        Truth.assertThat(options.currentSize.z).isEqualTo(2f)
    }

    @Test
    fun addMovableComponentToPanelEntity_updatesComponentSize() {
        val panelEntity = createTestPanelEntity()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )

        // Initial size of movableComponent should be (0, 0, 0)
        Truth.assertThat(movableComponent.size).isEqualTo(Dimensions(0f, 0f, 0f))

        Truth.assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        // After attaching, size should match entity size
        Truth.assertThat(movableComponent.size).isEqualTo(panelEntity.size)
    }

    @Test
    fun addMovableComponentToMainPanelEntity_updatesComponentSize() {
        val mainPanelEntity = sceneRuntime.mainPanelEntity
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )

        // Initial size of movableComponent should be (0, 0, 0)
        Truth.assertThat(movableComponent.size).isEqualTo(Dimensions(0f, 0f, 0f))

        Truth.assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()

        // After attaching, size should match entity size
        Truth.assertThat(movableComponent.size).isEqualTo(mainPanelEntity.size)
    }

    @Test
    fun scaleWithDistanceOnMovableComponent_defaultsToDefaultMode() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )

        // Default value for scaleWithDistanceMode is DEFAULT.
        Truth.assertThat(movableComponent.scaleWithDistanceMode)
            .isEqualTo(MovableComponent.ScaleWithDistanceMode.DEFAULT)
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        Truth.assertThat(
                nodeRepository.getReformOptions(getEntityNode(entity)).scaleWithDistanceMode
            )
            .isEqualTo(ReformOptions.SCALE_WITH_DISTANCE_MODE_DEFAULT)
    }

    @Test
    fun setScaleWithDistanceOnMovableComponent_setsScaleWithDistanceOnNodeReformOptions() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()

        movableComponent.scaleWithDistanceMode = MovableComponent.ScaleWithDistanceMode.DMM

        Truth.assertThat(movableComponent.scaleWithDistanceMode)
            .isEqualTo(MovableComponent.ScaleWithDistanceMode.DMM)
        Truth.assertThat(
                nodeRepository.getReformOptions(getEntityNode(entity)).scaleWithDistanceMode
            )
            .isEqualTo(ReformOptions.SCALE_WITH_DISTANCE_MODE_DMM)
    }

    @Test
    fun setPropertiesOnMovableComponentAttachLater_setsPropertiesOnNodeReformOptions() {
        val entity = createTestEntity() as AndroidXrEntity
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        movableComponent.size = Dimensions(2f, 2f, 2f)
        val mockMoveEventListener = mock<MoveEventListener>()
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), mockMoveEventListener)
        Truth.assertThat(movableComponent.reformEventConsumer).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(entity.mNode)

        Truth.assertThat(options.currentSize.x).isEqualTo(2f)
        Truth.assertThat(options.currentSize.y).isEqualTo(2f)
        Truth.assertThat(options.currentSize.z).isEqualTo(2f)
        Truth.assertThat(options.eventCallback).isNotNull()
        Truth.assertThat(options.eventExecutor).isNotNull()
        Truth.assertThat(entity.reformEventConsumerMap).isNotEmpty()
    }

    @Test
    fun addMoveEventListener_onlyInvokedOnMoveEvent() {
        val entity = createTestEntity() as AndroidXrEntity
        val initialTranslation = Vector3(1f, 2f, 3f)
        val initialScale = Vector3(1.1f, 1.1f, 1.1f)
        entity.setPose(Pose(initialTranslation, Quaternion()))
        entity.setScale(initialScale)
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(entity.mNode)
        val mockMoveEventListener = mock<MoveEventListener>()

        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), mockMoveEventListener)
        Truth.assertThat(options.eventCallback).isNotNull()
        Truth.assertThat(options.eventExecutor).isNotNull()
        Truth.assertThat(entity.reformEventConsumerMap).isNotEmpty()

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendReformEvent(entity.mNode, resizeReformEvent)
        verify(mockMoveEventListener, never()).onMoveEvent(any())

        val moveReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendReformEvent(entity.mNode, moveReformEvent)
        val moveEventCaptor = argumentCaptor<MoveEvent>()
        verify(mockMoveEventListener).onMoveEvent(moveEventCaptor.capture())
        val capturedEvents = moveEventCaptor.allValues
        val moveEvent = capturedEvents[0]
        Truth.assertThat(moveEvent.previousPose.translation).isEqualTo(initialTranslation)
        Truth.assertThat(moveEvent.previousScale).isEqualTo(initialScale)
    }

    @Test
    fun addMoveEventListenerWithExecutor_invokesListenerOnGivenExecutor() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))
        val mockMoveEventListener = mock<MoveEventListener>()
        val mockMoveEventListener2 = mock<MoveEventListener>()
        val executorService = FakeScheduledExecutorService()

        movableComponent.addMoveEventListener(executorService, mockMoveEventListener)
        movableComponent.addMoveEventListener(
            MoreExecutors.directExecutor(),
            mockMoveEventListener2,
        )
        Truth.assertThat(options.eventCallback).isNotNull()
        Truth.assertThat(options.eventExecutor).isNotNull()

        val reformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendReformEvent(getEntityNode(entity), reformEvent)
        Truth.assertThat(executorService.hasNext()).isTrue()
        executorService.runAll()
        verify(mockMoveEventListener).onMoveEvent(any())

        verify(mockMoveEventListener2).onMoveEvent(any())
    }

    @Test
    fun addMoveEventListenerMultiple_invokesAllListeners() {
        val entity = createTestEntity()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockMoveEventListener1 = mock<MoveEventListener>()
        val mockMoveEventListener2 = mock<MoveEventListener>()
        val executorService = FakeScheduledExecutorService()

        movableComponent.addMoveEventListener(executorService, mockMoveEventListener1)
        movableComponent.addMoveEventListener(executorService, mockMoveEventListener2)
        val reformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendReformEvent(getEntityNode(entity), reformEvent)
        Truth.assertThat(executorService.hasNext()).isTrue()
        executorService.runAll()

        verify(mockMoveEventListener1).onMoveEvent(any())

        verify(mockMoveEventListener2).onMoveEvent(any())
    }

    @Test
    fun addMoveEventListenerOnDefaultExecutor_invokesListenerOnDefaultExecutor() {
        val entity = createTestEntity()
        val movableComponent: MovableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val options = nodeRepository.getReformOptions(getEntityNode(entity))
        val mockMoveEventListener = mock<MoveEventListener>()

        movableComponent.addMoveEventListener(mockMoveEventListener)
        Truth.assertThat(options.eventCallback).isNotNull()
        Truth.assertThat(options.eventExecutor).isNotNull()

        val reformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendReformEvent(getEntityNode(entity), reformEvent)
        verify(mockMoveEventListener).onMoveEvent(any())
    }

    @Test
    fun removeMoveEventListenerMultiple_removesGivenListener() {
        val entity = createTestEntity()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockMoveEventListener1 = mock<MoveEventListener>()
        val mockMoveEventListener2 = mock<MoveEventListener>()
        val executorService = FakeScheduledExecutorService()

        movableComponent.addMoveEventListener(executorService, mockMoveEventListener1)
        movableComponent.addMoveEventListener(executorService, mockMoveEventListener2)
        val reformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendReformEvent(getEntityNode(entity), reformEvent)
        executorService.runAll()

        // Verify both listeners are invoked.
        verify(mockMoveEventListener1).onMoveEvent(any())

        verify(mockMoveEventListener2).onMoveEvent(any())

        movableComponent.removeMoveEventListener(mockMoveEventListener1)
        sendReformEvent(getEntityNode(entity), reformEvent)
        Truth.assertThat(executorService.hasNext()).isTrue()
        executorService.runAll()

        // The first listener, which we removed, should not be invoked again.
        verify(mockMoveEventListener1).onMoveEvent(any())

        verify(mockMoveEventListener2, times(2)).onMoveEvent(any())
    }

    @Test
    fun removeMovableComponent_clearsMoveReformOptionsAndMoveEventListeners() {
        val entity = createTestEntity()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockMoveEventListener = mock<MoveEventListener>()

        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), mockMoveEventListener)
        Truth.assertThat(movableComponent.reformEventConsumer).isNotNull()
        Truth.assertThat((entity as AndroidXrEntity).reformEventConsumerMap).isNotEmpty()

        entity.removeComponent(movableComponent)
        Truth.assertThat(nodeRepository.getReformOptions(getEntityNode(entity))).isNull()
        Truth.assertThat(entity.reformEventConsumerMap).isEmpty()
    }

    @Test
    fun movableComponent_canAttachAgainAfterDetach() {
        val entity = createTestEntity()
        Truth.assertThat(entity).isNotNull()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
        entity.removeComponent(movableComponent)
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun anchorableComponentMoving_sendMoveEvent_rendersShadow() {
        // Set the activity space pose to be 1 unit down and to the left of the origin.
        val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion(0f, 0f, 0f, 1f))
        setActivitySpacePose(activitySpacePose)
        val entity = createTestPanelEntity()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveEventListener: MoveEventListener = TestMoveEventListener(movableComponent)
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)

        val moveStartReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )
        val moveOngoingReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_ONGOING,
                /* id= */ 0,
            )

        val proposedPoseInActivitySpace =
            Pose(Vector3(1.0f, 1.0f, 1.0f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f))
        val proposedPosition =
            Vec3(
                proposedPoseInActivitySpace.translation.x,
                proposedPoseInActivitySpace.translation.y,
                proposedPoseInActivitySpace.translation.z,
            )
        val proposedPoseInOxr = proposedPoseInActivitySpace.translate(activitySpacePose.translation)
        val expectedPlanePoseInOxr = proposedPoseInOxr.translate(Vector3(0.0f, 1.0f, 0.0f))

        // Put the proposed position at 1 above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(moveStartReformEvent).setProposedPosition(proposedPosition)
        ShadowReformEvent.extract(moveOngoingReformEvent).setProposedPosition(proposedPosition)

        sendReformEvent(getEntityNode(entity), moveStartReformEvent)
        sendReformEvent(getEntityNode(entity), moveOngoingReformEvent)

        // Since it is by the plane a call should be made to the panel shadow renderer.
        verify(mockPanelShadowRenderer)
            .updateShadow(proposedPoseInOxr, expectedPlanePoseInOxr, FloatSize2d(10.0f, 10.0f))
    }

    @Test
    fun anchorableComponentNotMoving_sendMoveEvent_hidesShadow() {
        val entity = createTestPanelEntity()
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveEventListener: MoveEventListener = TestMoveEventListener(movableComponent)
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)

        val moveStartReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )
        val moveOngoingReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_ONGOING,
                /* id= */ 0,
            )
        val moveEndReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_END,
                /* id= */ 0,
            )

        sendReformEvent(getEntityNode(entity), moveStartReformEvent)
        sendReformEvent(getEntityNode(entity), moveEndReformEvent)
        sendReformEvent(getEntityNode(entity), moveOngoingReformEvent)

        verify(mockPanelShadowRenderer, never())
            .updateShadow(any(), any(), eq(FloatSize2d(10.0f, 10.0f)))
    }

    @Test
    fun movableComponent_endMovement_hidesShadow() {
        val entity = createTestPanelEntity()
        // Set anchorPlacement to any plane.
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        Truth.assertThat(movableComponent).isNotNull()
        Truth.assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveEventListener: MoveEventListener = TestMoveEventListener(movableComponent)
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)

        val moveStartReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )
        val moveEndReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_END,
                /* id= */ 0,
            )

        sendReformEvent(getEntityNode(entity), moveStartReformEvent)
        sendReformEvent(getEntityNode(entity), moveEndReformEvent)

        verify(mockPanelShadowRenderer).destroy()
    }

    @Test
    fun anchorableComponentMovingGltf_sendMoveEvent_rendersShadow() {
        // Set the activity space pose to be 1 unit down and to the left of the origin.
        val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion(0f, 0f, 0f, 1f))
        setActivitySpacePose(activitySpacePose)
        val gltfEntity = createGltfEntity(activity)
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        assertTrue(gltfEntity.addComponent(movableComponent))

        val moveEventListener: MoveEventListener = TestMoveEventListener(movableComponent)
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)

        val moveStartInputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_DOWN,
            )
        val entity = gltfEntity as AndroidXrEntity
        val shadowNode = ShadowNode.extract(entity.mNode)

        assertNotNull(shadowNode.inputListener)
        assertEquals(shadowNode.inputExecutor, fakeExecutor)
        sendInputEvent(shadowNode, moveStartInputEvent)
        val moveOngoingIputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(1f, 1f, 1f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_MOVE,
            )

        sendInputEvent(shadowNode, moveOngoingIputEvent)

        val proposedPoseInActivitySpace =
            Pose(Vector3(1.0f, 1.0f, 1.0f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f))
        val proposedPosition =
            Vec3(
                proposedPoseInActivitySpace.translation.x,
                proposedPoseInActivitySpace.translation.y,
                proposedPoseInActivitySpace.translation.z,
            )
        val proposedPoseInOxr = proposedPoseInActivitySpace.translate(activitySpacePose.translation)
        val expectedPlanePoseInOxr = proposedPoseInOxr.translate(Vector3(0.0f, 1.0f, 0.0f))

        gltfEntity.setPose(proposedPoseInActivitySpace)

        fakeExecutor.runAll()
        // Since it is by the plane a call should be made to the panel shadow renderer.
        verify(mockPanelShadowRenderer)
            .updateShadow(proposedPoseInOxr, expectedPlanePoseInOxr, FloatSize2d(1.0f, 1.0f))
    }

    @Test
    fun anchorableComponentNotMovingGltf_sendMoveEvent_hidesShadow() {
        val gltfEntity = createGltfEntity(activity)
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        assertTrue(gltfEntity.addComponent(movableComponent))

        val moveEventListener: MoveEventListener = TestMoveEventListener(movableComponent)
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)

        val moveStartInputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_DOWN,
            )

        val moveOngoingInputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_MOVE,
            )
        val moveEndInputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_UP,
            )

        val entity = gltfEntity as AndroidXrEntity
        val shadowNode = ShadowNode.extract(entity.mNode)
        sendInputEvent(shadowNode, moveStartInputEvent)
        sendInputEvent(shadowNode, moveOngoingInputEvent)
        sendInputEvent(shadowNode, moveEndInputEvent)

        verify(mockPanelShadowRenderer, never())
            .updateShadow(any(), any(), eq(FloatSize2d(10.0f, 1.0f)))
        0
    }

    @Test
    fun movableComponentGltf_endMovement_hidesShadow() {
        val gltfEntity = createGltfEntity(activity)
        // Set anchorPlacement to any plane.
        val movableComponent =
            MovableComponentImpl(
                systemMovable = true,
                scaleInZ = false,
                userAnchorable = true,
                activitySpaceImpl = activitySpaceImpl,
                entityShadowRenderer = mockPanelShadowRenderer,
                runtimeExecutor = fakeExecutor,
            )
        assertTrue(gltfEntity.addComponent(movableComponent))

        val moveEventListener: MoveEventListener = TestMoveEventListener(movableComponent)
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)

        val moveStartInputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_DOWN,
            )

        val moveEndInputEvent =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT, /* timestamp */
                0,
                Vec3(0f, 0f, 0f),
                Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_UP,
            )

        val entity = gltfEntity as AndroidXrEntity
        val shadowNode = ShadowNode.extract(entity.mNode)
        sendInputEvent(shadowNode, moveStartInputEvent)
        sendInputEvent(shadowNode, moveEndInputEvent)

        verify(mockPanelShadowRenderer).destroy()
    }

    internal inner class TestMoveEventListener(var movableComponent: MovableComponent) :
        MoveEventListener {
        var callCount: Int = 0
        var lastMoveEvent: MoveEvent? = null

        override fun onMoveEvent(event: MoveEvent) {
            lastMoveEvent = event
            callCount++
            val currentPoseInParentSpace = event.currentPose
            val currentPoseInOxr =
                event.initialParent.transformPoseTo(
                    currentPoseInParentSpace,
                    sceneRuntime.perceptionSpaceActivityPose,
                )
            val planePoseInOxr = currentPoseInOxr.translate(Vector3(0.0f, 1.0f, 0.0f))
            when (event.moveState) {
                MoveEvent.MOVE_STATE_START -> {}
                MoveEvent.MOVE_STATE_ONGOING ->
                    // Notify movable component that there is a plane 1 unit up from proposed pose.
                    movableComponent.setPlanePoseForMoveUpdatePose(planePoseInOxr, currentPoseInOxr)
                MoveEvent.MOVE_STATE_END ->
                    movableComponent.setPlanePoseForMoveUpdatePose(null, currentPoseInOxr)
                else -> {}
            }
        }
    }
}
