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
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider.Companion.testSpatialApiVersion
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.MoveEventListener
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.ResizeEvent
import androidx.xr.scenecore.runtime.ResizeEventListener
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.FakeSurfaceFeature
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.ReformOptions
import com.android.extensions.xr.node.ShadowReformEvent
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ResizableComponentImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val xrExtensions = getXrExtensions()!!
    private val entityManager = EntityManager()
    private val panelShadowRenderer: PanelShadowRenderer = mock<PanelShadowRenderer>()
    private val nodeRepository: NodeRepository = NodeRepository.getInstance()
    private lateinit var activitySpaceImpl: ActivitySpaceImpl
    private lateinit var fakeSceneRuntime: SpatialSceneRuntime

    @Before
    fun setUp() {
        TruthJUnit.assume().that(xrExtensions).isNotNull()
        testSpatialApiVersion = 1
        val activitySpaceNode = xrExtensions.createNode()
        activitySpaceImpl =
            ActivitySpaceImpl(
                activitySpaceNode,
                activity,
                xrExtensions,
                entityManager,
                { xrExtensions.getSpatialState(activity) },
                unscaledGravityAlignedActivitySpace = false,
                fakeExecutor,
            )
        fakeSceneRuntime =
            SpatialSceneRuntime.create(
                activity,
                fakeExecutor,
                xrExtensions,
                entityManager,
                /* unscaledGravityAlignedActivitySpace= */ false,
            )
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        fakeSceneRuntime.destroy()
        testSpatialApiVersion = null
    }

    private fun createTestEntity(): Entity {
        return fakeSceneRuntime.createGroupEntity(Pose(), "test", fakeSceneRuntime.activitySpace)
    }

    /**
     * Creates a generic panel entity instance for testing by creating a dummy view to insert into
     * the panel, and setting the activity space as parent.
     */
    private fun createTestPanelEntity(pose: Pose, dimensions: Dimensions): PanelEntity {
        val display = activity.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.setLayoutParams(ViewGroup.LayoutParams(640, 480))
        return fakeSceneRuntime.createPanelEntity(
            displayContext,
            pose,
            view,
            dimensions,
            "testPanel",
            fakeSceneRuntime.activitySpace,
        )
    }

    private fun createTestSurfaceEntity(shape: SurfaceEntity.Shape): SurfaceEntity {
        val nodeHolder: NodeHolder<*> =
            NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val surface =
            fakeSceneRuntime.createSurfaceEntity(
                FakeSurfaceFeature(nodeHolder),
                Pose.Identity,
                fakeSceneRuntime.activitySpace,
            )
        surface.shape = shape
        return surface
    }

    @Test
    fun addResizableComponentToTwoEntity_fails() {
        val entity1 = createTestEntity()
        val entity2 = createTestEntity()
        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity1.addComponent(resizableComponent)).isTrue()
        assertThat(entity2.addComponent(resizableComponent)).isFalse()
    }

    @Test
    fun addResizableComponent_addsReformOptionsToNode() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_RESIZE)
        assertThat(options.minimumSize.x).isEqualTo(MIN_DIMENSIONS.width)
        assertThat(options.minimumSize.y).isEqualTo(MIN_DIMENSIONS.height)
        assertThat(options.minimumSize.z).isEqualTo(MIN_DIMENSIONS.depth)
        assertThat(options.maximumSize.x).isEqualTo(MAX_DIMENSIONS.width)
        assertThat(options.maximumSize.y).isEqualTo(MAX_DIMENSIONS.height)
        assertThat(options.maximumSize.z).isEqualTo(MAX_DIMENSIONS.depth)
    }

    @Test
    fun addResizableComponentToPanel_getsPanelSize() {
        val panelSize = Dimensions(1f, 20f, 0f)

        // case for attach without set size
        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        val entity = createTestPanelEntity(Pose.Identity, panelSize) as AndroidXrEntity
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.currentSize.x).isEqualTo(panelSize.width)
        assertThat(options.currentSize.y).isEqualTo(panelSize.height)
        assertThat(options.currentSize.z).isEqualTo(panelSize.depth)
        assertThat(resizableComponent.size.width).isEqualTo(panelSize.width)
        assertThat(resizableComponent.size.height).isEqualTo(panelSize.height)
        assertThat(resizableComponent.size.depth).isEqualTo(panelSize.depth)

        entity.removeComponent(resizableComponent)

        // case for preset size
        val inputSize = Dimensions(0f, 5f, 40f)

        resizableComponent.size = inputSize
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        assertThat(options.currentSize.x).isEqualTo(panelSize.width)
        assertThat(options.currentSize.y).isEqualTo(panelSize.height)
        assertThat(options.currentSize.z).isEqualTo(panelSize.depth)
        assertThat(resizableComponent.size.width).isEqualTo(panelSize.width)
        assertThat(resizableComponent.size.height).isEqualTo(panelSize.height)
        assertThat(resizableComponent.size.depth).isEqualTo(panelSize.depth)
    }

    @Test
    fun addResizableComponentToQuadSurface_getsQuadSize() {
        val quadSize = Dimensions(1f, 2f, 0f)

        // case for attach without set size
        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        val entity =
            createTestSurfaceEntity(
                SurfaceEntity.Shape.Quad(FloatSize2d(quadSize.width, quadSize.height))
            )
                as AndroidXrEntity
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.currentSize.x).isEqualTo(quadSize.width)
        assertThat(options.currentSize.y).isEqualTo(quadSize.height)
        assertThat(options.currentSize.z).isEqualTo(quadSize.depth)
        assertThat(resizableComponent.size.width).isEqualTo(quadSize.width)
        assertThat(resizableComponent.size.height).isEqualTo(quadSize.height)
        assertThat(resizableComponent.size.depth).isEqualTo(quadSize.depth)

        entity.removeComponent(resizableComponent)

        // case for preset size
        val inputSize = Dimensions(0f, 5f, 40f)

        resizableComponent.size = inputSize
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        assertThat(options.currentSize.x).isEqualTo(quadSize.width)
        assertThat(options.currentSize.y).isEqualTo(quadSize.height)
        assertThat(options.currentSize.z).isEqualTo(quadSize.depth)
        assertThat(resizableComponent.size.width).isEqualTo(quadSize.width)
        assertThat(resizableComponent.size.height).isEqualTo(quadSize.height)
        assertThat(resizableComponent.size.depth).isEqualTo(quadSize.depth)
    }

    @Test
    fun addResizableComponent_setsMinMaxSizeOnNodeReformOptions() {
        val inputMin = Dimensions(Float.NaN, 1f, 2f)
        val inputMax = Dimensions(Float.NaN, -1f, 1f)
        val expectMin = Dimensions(0f, 1f, 2f)
        val expectMax = Dimensions(Float.POSITIVE_INFINITY, 1f, 2f)

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, inputMin, inputMax)

        val entity = createTestEntity() as AndroidXrEntity
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        assertThat(options.minimumSize.x).isEqualTo(expectMin.width)
        assertThat(options.minimumSize.y).isEqualTo(expectMin.height)
        assertThat(options.minimumSize.z).isEqualTo(expectMin.depth)
        assertThat(options.maximumSize.x).isEqualTo(expectMax.width)
        assertThat(options.maximumSize.y).isEqualTo(expectMax.height)
        assertThat(options.maximumSize.z).isEqualTo(expectMax.depth)
    }

    @Test
    fun setSizeOnResizableComponent_setsSanitizedSizeOnCurrentAndReformOptions() {
        // case for valid size
        var inputSize = Dimensions(0f, 5f, 40f)
        var expectSize = Dimensions(0f, 5f, 40f)

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        val entity = createTestEntity() as AndroidXrEntity
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        resizableComponent.size = inputSize

        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.currentSize.x).isEqualTo(expectSize.width)
        assertThat(options.currentSize.y).isEqualTo(expectSize.height)
        assertThat(options.currentSize.z).isEqualTo(expectSize.depth)
        assertThat(resizableComponent.size.width).isEqualTo(expectSize.width)
        assertThat(resizableComponent.size.height).isEqualTo(expectSize.height)
        assertThat(resizableComponent.size.depth).isEqualTo(expectSize.depth)

        // case for invalid(NaN) size
        inputSize = Dimensions(Float.NaN, Float.NaN, 50f)
        expectSize = Dimensions(0f, 5f, 50f)
        resizableComponent.size = inputSize

        assertThat(options.currentSize.x).isEqualTo(expectSize.width)
        assertThat(options.currentSize.y).isEqualTo(expectSize.height)
        assertThat(options.currentSize.z).isEqualTo(expectSize.depth)
        assertThat(resizableComponent.size.width).isEqualTo(expectSize.width)
        assertThat(resizableComponent.size.height).isEqualTo(expectSize.height)
        assertThat(resizableComponent.size.depth).isEqualTo(expectSize.depth)
    }

    @Test
    fun getMinMaxSizeOnResizableComponent_returnsSanitizedInitialMinMaxSize() {
        val initMin = Dimensions(Float.NaN, -1f, 2f)
        val initMax = Dimensions(Float.NaN, 1f, 1f)
        val expectMin = Dimensions(0f, 0f, 2f)
        val expectMax = Dimensions(Float.POSITIVE_INFINITY, 1f, 2f)

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, initMin, initMax)

        assertThat(resizableComponent.minimumSize.width).isEqualTo(expectMin.width)
        assertThat(resizableComponent.minimumSize.height).isEqualTo(expectMin.height)
        assertThat(resizableComponent.minimumSize.depth).isEqualTo(expectMin.depth)
        assertThat(resizableComponent.maximumSize.width).isEqualTo(expectMax.width)
        assertThat(resizableComponent.maximumSize.height).isEqualTo(expectMax.height)
        assertThat(resizableComponent.maximumSize.depth).isEqualTo(expectMax.depth)
    }

    @Test
    fun setMinSizeOnResizableComponent_setsSanitizedMinMaxSizeOnNodeReformOptions() {
        val initMin = Dimensions(1f, 1f, 1f)
        val initMax = Dimensions(10f, 10f, 10f)
        val inputMin = Dimensions(20f, Float.NaN, -1f)
        val expectMin = Dimensions(20f, 1f, 0f)
        val expectMax = Dimensions(20f, 10f, 10f)

        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, initMin, initMax)
        assertThat(resizableComponent).isNotNull()

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        resizableComponent.minimumSize = inputMin

        assertThat(options.minimumSize.x).isEqualTo(expectMin.width)
        assertThat(options.minimumSize.y).isEqualTo(expectMin.height)
        assertThat(options.minimumSize.z).isEqualTo(expectMin.depth)
        assertThat(resizableComponent.minimumSize.width).isEqualTo(expectMin.width)
        assertThat(resizableComponent.minimumSize.height).isEqualTo(expectMin.height)
        assertThat(resizableComponent.minimumSize.depth).isEqualTo(expectMin.depth)
        assertThat(options.maximumSize.x).isEqualTo(expectMax.width)
        assertThat(options.maximumSize.y).isEqualTo(expectMax.height)
        assertThat(options.maximumSize.z).isEqualTo(expectMax.depth)
        assertThat(resizableComponent.maximumSize.width).isEqualTo(expectMax.width)
        assertThat(resizableComponent.maximumSize.height).isEqualTo(expectMax.height)
        assertThat(resizableComponent.maximumSize.depth).isEqualTo(expectMax.depth)
    }

    @Test
    fun setMaxSizeOnResizableComponent_setsSanitizedMinMaxSizeOnNodeReformOptions() {
        val initMin = Dimensions(2f, 2f, 2f)
        val initMax = Dimensions(10f, 10f, 10f)
        val inputMax = Dimensions(1f, Float.NaN, -1f)
        val expectMin = Dimensions(1f, 2f, 0f)
        val expectMax = Dimensions(1f, 10f, 0f)

        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, initMin, initMax)
        assertThat(resizableComponent).isNotNull()

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        resizableComponent.maximumSize = inputMax

        assertThat(options.minimumSize.x).isEqualTo(expectMin.width)
        assertThat(options.minimumSize.y).isEqualTo(expectMin.height)
        assertThat(options.minimumSize.z).isEqualTo(expectMin.depth)
        assertThat(resizableComponent.minimumSize.width).isEqualTo(expectMin.width)
        assertThat(resizableComponent.minimumSize.height).isEqualTo(expectMin.height)
        assertThat(resizableComponent.minimumSize.depth).isEqualTo(expectMin.depth)
        assertThat(options.maximumSize.x).isEqualTo(expectMax.width)
        assertThat(options.maximumSize.y).isEqualTo(expectMax.height)
        assertThat(options.maximumSize.z).isEqualTo(expectMax.depth)
        assertThat(resizableComponent.maximumSize.width).isEqualTo(expectMax.width)
        assertThat(resizableComponent.maximumSize.height).isEqualTo(expectMax.height)
        assertThat(resizableComponent.maximumSize.depth).isEqualTo(expectMax.depth)
    }

    @Test
    fun setFixedAspectRatioOnResizableComponent_setsFixedAspectRatioOnNodeReformOptions() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        // If no size was set the default aspect ratio will be 1.
        resizableComponent.isFixedAspectRatioEnabled = true
        assertThat(options.fixedAspectRatio).isEqualTo(1.0f)

        // Updating the size will update the aspect ratio if enabled.
        resizableComponent.size = Dimensions(1f, 2f, 1f)
        assertThat(options.fixedAspectRatio).isEqualTo(0.5f)

        // Disabling the aspect ratio will set the fixed aspect ratio to 0.
        resizableComponent.isFixedAspectRatioEnabled = false
        assertThat(options.fixedAspectRatio).isEqualTo(0.0f)

        // Updating the size will not update the aspect ratio if disabled.
        resizableComponent.size = Dimensions(3f, 1f, 1f)
        assertThat(options.fixedAspectRatio).isEqualTo(0.0f)

        // Enabling it will update the aspect ratio based on the current size.
        resizableComponent.isFixedAspectRatioEnabled = true
        assertThat(options.fixedAspectRatio).isEqualTo(3.0f)
    }

    @Test
    fun setFixedAspectRatioOnResizableComponent_setsPanelAspectRatio() {
        val panelDimensions = Dimensions(2.0f, 1.0f, 0.0f)
        val entity = createTestPanelEntity(Pose(), panelDimensions) as PanelEntityImpl

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()

        resizableComponent.isFixedAspectRatioEnabled = true

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.fixedAspectRatio)
            .isEqualTo(panelDimensions.width / panelDimensions.height)
    }

    @Test
    fun setForceShowResizeOverlayOnResizableComponent_setsForceShowResizeOverlayOnNodeReformOptions() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        resizableComponent.forceShowResizeOverlay = true

        assertThat(nodeRepository.getReformOptions(entity.getNode()).forceShowResizeOverlay)
            .isTrue()
    }

    @Test
    fun addResizableComponentLater_addsReformOptionsToNode() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()

        val testSize = Dimensions(1f, 1f, 1f)
        val testMinSize = Dimensions(0.25f, 0.25f, 0.25f)
        val testMaxSize = Dimensions(5f, 5f, 5f)
        resizableComponent.size = testSize
        resizableComponent.minimumSize = testMinSize
        resizableComponent.maximumSize = testMaxSize

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_RESIZE)
        assertThat(options.currentSize.x).isEqualTo(testSize.width)
        assertThat(options.currentSize.y).isEqualTo(testSize.height)
        assertThat(options.currentSize.z).isEqualTo(testSize.depth)
        assertThat(options.minimumSize.x).isEqualTo(testMinSize.width)
        assertThat(options.minimumSize.y).isEqualTo(testMinSize.height)
        assertThat(options.minimumSize.z).isEqualTo(testMinSize.depth)
        assertThat(options.maximumSize.x).isEqualTo(testMaxSize.width)
        assertThat(options.maximumSize.y).isEqualTo(testMaxSize.height)
        assertThat(options.maximumSize.z).isEqualTo(testMaxSize.depth)
    }

    @Test
    fun addResizeEventListener_onlyInvokedOnResizeEvent() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener = mock<ResizeEventListener>()

        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()
        assertThat(entity.reformEventConsumerMap).isNotEmpty()

        val moveReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), moveReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(mockResizeEventListener, never()).onResizeEvent(any())

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )
        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(mockResizeEventListener).onResizeEvent(any())
    }

    private fun sendResizeEvent(node: Node, reformEvent: ReformEvent?) {
        val options = nodeRepository.getReformOptions(node)
        options.eventExecutor.execute { options.eventCallback.accept(reformEvent) }
    }

    @Test
    fun addResizeEventListenerWithExecutor_invokesListenerOnGivenExecutor() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener = mock<ResizeEventListener>()
        val executorService = FakeScheduledExecutorService()

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener)

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        assertThat(executorService.hasNext()).isTrue()

        executorService.runAll()

        verify(mockResizeEventListener).onResizeEvent(any())
    }

    @Test
    fun addResizeEventListenerMultiple_invokesAllListeners() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener1 = mock<ResizeEventListener>()
        val mockResizeEventListener2 = mock<ResizeEventListener>()
        val executorService = FakeScheduledExecutorService()

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1)
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2)

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        assertThat(executorService.hasNext()).isTrue()

        executorService.runAll()

        verify(mockResizeEventListener1).onResizeEvent(any())
        verify(mockResizeEventListener2).onResizeEvent(any())
    }

    @Test
    fun removeResizeEventListenerMultiple_removesGivenListener() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener1 = mock<ResizeEventListener>()
        val mockResizeEventListener2 = mock<ResizeEventListener>()
        val executorService = FakeScheduledExecutorService()

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1)
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2)
        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        assertThat(executorService.hasNext()).isTrue()

        executorService.runAll()

        resizableComponent.removeResizeEventListener(mockResizeEventListener1)
        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        assertThat(executorService.hasNext()).isTrue()

        executorService.runAll()

        verify(mockResizeEventListener1).onResizeEvent(any())
        verify(mockResizeEventListener2, times(2)).onResizeEvent(any())
    }

    @Test
    fun removeAllResizeEventListeners_removesReformEventConsumer() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener1 = mock<ResizeEventListener>()
        val mockResizeEventListener2 = mock<ResizeEventListener>()
        val executorService = FakeScheduledExecutorService()

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1)
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2)

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        assertThat(executorService.hasNext()).isTrue()

        executorService.runAll()

        verify(mockResizeEventListener1).onResizeEvent(any())
        verify(mockResizeEventListener2).onResizeEvent(any())

        resizableComponent.removeResizeEventListener(mockResizeEventListener1)
        resizableComponent.removeResizeEventListener(mockResizeEventListener2)

        assertThat(entity.reformEventConsumerMap).isEmpty()
    }

    @Test
    fun removeResizableComponent_clearsResizeReformOptionsAndResizeEventListeners() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val mockResizeEventListener = mock<ResizeEventListener>()

        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        assertThat(resizableComponent.reformEventConsumer).isNotNull()

        entity.removeComponent(resizableComponent)

        assertThat(nodeRepository.getReformOptions(entity.getNode())).isNull()
        assertThat(entity.reformEventConsumerMap).isEmpty()
    }

    @Test
    fun addMoveAndResizeComponents_setsCombinedReformsOptions() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val movableComponent =
            MovableComponentImpl(
                true,
                true,
                false,
                activitySpaceImpl,
                panelShadowRenderer,
                fakeExecutor,
            )
        val resizableComponent =
            ResizableComponentImpl(
                fakeExecutor,
                xrExtensions,
                Dimensions(0f, 0f, 0f),
                Dimensions(5f, 5f, 5f),
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        assertThat(nodeRepository.getReformOptions(entity.getNode()).enabledReform)
            .isEqualTo(ReformOptions.ALLOW_MOVE or ReformOptions.ALLOW_RESIZE)
    }

    @Test
    fun addMoveAndResizeComponents_removingMoveKeepsResize() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val movableComponent =
            MovableComponentImpl(
                true,
                true,
                false,
                activitySpaceImpl,
                panelShadowRenderer,
                fakeExecutor,
            )
        val resizableComponent =
            ResizableComponentImpl(
                fakeExecutor,
                xrExtensions,
                Dimensions(0f, 0f, 0f),
                Dimensions(5f, 5f, 5f),
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val moveEventListener = mock<MoveEventListener>()
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)
        val resizeEventListener = mock<ResizeEventListener>()
        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            resizeEventListener,
        )
        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.enabledReform)
            .isEqualTo(ReformOptions.ALLOW_MOVE or ReformOptions.ALLOW_RESIZE)

        entity.removeComponent(movableComponent)

        assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_RESIZE)

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(resizeEventListener).onResizeEvent(any())

        val moveReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), moveReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(moveEventListener, never()).onMoveEvent(any())
    }

    @Test
    fun addMoveAndResizeComponents_removingResizeKeepsMove() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val movableComponent =
            MovableComponentImpl(
                true,
                true,
                false,
                activitySpaceImpl,
                panelShadowRenderer,
                fakeExecutor,
            )
        val resizableComponent =
            ResizableComponentImpl(
                fakeExecutor,
                xrExtensions,
                Dimensions(0f, 0f, 0f),
                Dimensions(5f, 5f, 5f),
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val moveEventListener = mock<MoveEventListener>()
        movableComponent.addMoveEventListener(MoreExecutors.directExecutor(), moveEventListener)
        val resizeEventListener = mock<ResizeEventListener>()
        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            resizeEventListener,
        )
        val options = nodeRepository.getReformOptions(entity.getNode())

        assertThat(options.enabledReform)
            .isEqualTo(ReformOptions.ALLOW_MOVE or ReformOptions.ALLOW_RESIZE)

        entity.removeComponent(resizableComponent)

        assertThat(options.enabledReform).isEqualTo(ReformOptions.ALLOW_MOVE)

        // Start the resize.
        val startReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), startReformEvent)

        val moveReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_MOVE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), moveReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(moveEventListener, times(2)).onMoveEvent(any())

        val resizeReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ 0,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), resizeReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(resizeEventListener, never()).onResizeEvent(any())
    }

    @Test
    fun resizableComponent_canAttachAgainAfterDetach() {
        val entity = createTestEntity()

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        entity.removeComponent(resizableComponent)

        assertThat(entity.addComponent(resizableComponent)).isTrue()
    }

    @Test
    fun resizableComponent_hidesEntityDuringResize() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        entity.setAlpha(0.9f)

        assertThat(entity.getAlpha()).isEqualTo(0.9f)

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener = mock<ResizeEventListener>()

        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()
        assertThat(entity.reformEventConsumerMap).isNotEmpty()

        // Start the resize.
        val startReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), startReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()
        val resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture())

        var resizeEvent = resizeEventCaptor.lastValue

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f)

        // End the resize.
        val endReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_END,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), endReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()
        // Verify that alpha is not restored until the resize event is processed.
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f)

        fakeExecutor.runAll()

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture())
        resizeEvent = resizeEventCaptor.allValues[2]

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        // Verify that alpha is restored after the resize event is processed.
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f)
    }

    @Test
    fun resizableComponent_withAutoHideContentDisabled_doesNotHideEntityDuringResize() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        entity.setAlpha(0.9f)

        assertThat(entity.getAlpha()).isEqualTo(0.9f)

        val options = nodeRepository.getReformOptions(entity.getNode())
        val mockResizeEventListener = mock<ResizeEventListener>()

        resizableComponent.autoHideContent = false
        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()
        assertThat(entity.reformEventConsumerMap).isNotEmpty()

        // Start the resize.
        val startReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), startReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        val resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture())
        var resizeEvent = resizeEventCaptor.lastValue

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f)

        // End the resize.
        val endReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_END,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), endReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture())
        resizeEvent = resizeEventCaptor.allValues[2]

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f)
    }

    @Test
    fun resizableComponent_updatesComponentSizeAfterResize() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        resizableComponent.size = Dimensions(1.0f, 2.0f, 3.0f)

        assertThat(options.currentSize.x).isEqualTo(1.0f)
        assertThat(options.currentSize.y).isEqualTo(2.0f)
        assertThat(options.currentSize.z).isEqualTo(3.0f)

        val mockResizeEventListener = mock<ResizeEventListener>()

        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()
        assertThat(entity.reformEventConsumerMap).isNotEmpty()

        // Start the resize.
        val startReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), startReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()
        val resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture())

        var resizeEvent = resizeEventCaptor.lastValue

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)

        // End the resize.
        val endReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_END,
                /* id= */ 0,
            )
        ShadowReformEvent.extract(endReformEvent).setProposedSize(Vec3(4.0f, 5.0f, 6.0f))

        sendResizeEvent(entity.getNode(), endReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture())

        resizeEvent = resizeEventCaptor.allValues[2]

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        assertThat(options.currentSize.x).isEqualTo(4.0f)
        assertThat(options.currentSize.y).isEqualTo(5.0f)
        assertThat(options.currentSize.z).isEqualTo(6.0f)
    }

    /**
     * Helper method to send a ReformEvent to the entity's node and immediately process it using the
     * fake executor.
     *
     * @param node The Node to which the ReformEvent is sent.
     * @param reformEvent The ReformEvent to send.
     */
    private fun sendAndProcessReformEvent(node: Node, reformEvent: ReformEvent?) {
        val options = nodeRepository.getReformOptions(node)
        options.eventExecutor.execute { options.eventCallback.accept(reformEvent) }
        fakeExecutor.runAll()
    }

    @Test
    fun resizableComponent_onResizeEvent_proposeSanitizedAndClampedSize() {
        // Setup
        val entity = createTestEntity() as AndroidXrEntity
        val resizableComponent =
            ResizableComponentImpl(
                fakeExecutor,
                xrExtensions,
                Dimensions(0.5f, 0.5f, 0.5f), // minSize
                Dimensions(10.0f, 10.0f, 10.0f),
            ) // maxSize

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        // Set an initial valid size for the component.
        resizableComponent.size = Dimensions(2.0f, 3.0f, 4.0f)

        // Verify initial state.
        assertThat(options.currentSize.x).isEqualTo(2.0f)
        assertThat(options.currentSize.y).isEqualTo(3.0f)
        assertThat(options.currentSize.z).isEqualTo(4.0f)

        val mockResizeEventListener = mock<ResizeEventListener>()
        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        // 1. Send a START resize event.
        val startReformEvent =
            ShadowReformEvent.create(
                ReformEvent.REFORM_TYPE_RESIZE,
                ReformEvent.REFORM_STATE_START,
                0,
            )
        ShadowReformEvent.extract(startReformEvent).setProposedSize(Vec3(3.0f, 4.0f, 5.0f))
        sendAndProcessReformEvent(entity.getNode(), startReformEvent)

        // Capture all ResizeEvent arguments in a single verification.
        var resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture())

        var capturedEvents = resizeEventCaptor.allValues

        // Event 1: RESIZE_STATE_START
        assertThat(capturedEvents[0].resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)
        // Use the default ReformOptions size when resizing start.
        assertThat(options.currentSize.x).isEqualTo(3.0f)
        assertThat(options.currentSize.y).isEqualTo(4.0f)
        assertThat(options.currentSize.z).isEqualTo(5.0f)
        assertThat(resizableComponent.size.width).isEqualTo(3.0f)
        assertThat(resizableComponent.size.height).isEqualTo(4.0f)
        assertThat(resizableComponent.size.depth).isEqualTo(5.0f)

        // 2. Send an END resize event with a NaN width.
        val endEventNanWidth =
            ShadowReformEvent.create(
                ReformEvent.REFORM_TYPE_RESIZE,
                ReformEvent.REFORM_STATE_END,
                0,
            )
        ShadowReformEvent.extract(endEventNanWidth).setProposedSize(Vec3(Float.NaN, 0.0f, 20.0f))
        sendAndProcessReformEvent(entity.getNode(), endEventNanWidth)

        resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture())

        capturedEvents = resizeEventCaptor.allValues

        // Event 2: RESIZE_STATE_END (NaN width proposed)
        assertThat(capturedEvents[1].resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        // When received invalid size from event, for NaN, propose a fallback previous value,
        // for value out of min/max, propose clamped value.
        assertThat(options.currentSize.x).isEqualTo(3.0f)
        assertThat(options.currentSize.y).isEqualTo(0.5f)
        assertThat(options.currentSize.z).isEqualTo(10.0f)
        assertThat(resizableComponent.size.width).isEqualTo(3.0f)
        assertThat(resizableComponent.size.height).isEqualTo(0.5f)
        assertThat(resizableComponent.size.depth).isEqualTo(10.0f)

        // 3. Send an END resize event with a NaN height.
        val endEventNanHeight =
            ShadowReformEvent.create(
                ReformEvent.REFORM_TYPE_RESIZE,
                ReformEvent.REFORM_STATE_END,
                0,
            )
        ShadowReformEvent.extract(endEventNanHeight).setProposedSize(Vec3(4.0f, Float.NaN, 6.0f))
        sendAndProcessReformEvent(entity.getNode(), endEventNanHeight)

        // Capture all ResizeEvent arguments in a single verification.
        resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener, times(3)).onResizeEvent(resizeEventCaptor.capture())

        capturedEvents = resizeEventCaptor.allValues

        // Event 3: RESIZE_STATE_END (NaN height proposed)
        assertThat(capturedEvents[2].resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        // Propose sanitized and clamped size
        assertThat(options.currentSize.x).isEqualTo(4.0f)
        assertThat(options.currentSize.y).isEqualTo(0.5f)
        assertThat(options.currentSize.z).isEqualTo(6.0f)
        assertThat(resizableComponent.size.width).isEqualTo(4.0f)
        assertThat(resizableComponent.size.height).isEqualTo(0.5f)
        assertThat(resizableComponent.size.depth).isEqualTo(6.0f)

        // 4. Send an END resize event with a NaN depth.
        val endEventNanDepth =
            ShadowReformEvent.create(
                ReformEvent.REFORM_TYPE_RESIZE,
                ReformEvent.REFORM_STATE_END,
                0,
            )
        ShadowReformEvent.extract(endEventNanDepth).setProposedSize(Vec3(4.0f, 5.0f, Float.NaN))
        sendAndProcessReformEvent(entity.getNode(), endEventNanDepth)

        // Capture all ResizeEvent arguments in a single verification.
        resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener, times(4)).onResizeEvent(resizeEventCaptor.capture())

        capturedEvents = resizeEventCaptor.allValues

        // Event 4: RESIZE_STATE_END (NaN depth proposed)
        assertThat(capturedEvents[3].resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        // Propose sanitized and clamped size
        assertThat(options.currentSize.x).isEqualTo(4.0f)
        assertThat(options.currentSize.y).isEqualTo(5.0f)
        assertThat(options.currentSize.z).isEqualTo(6.0f)
        assertThat(resizableComponent.size.width).isEqualTo(4.0f)
        assertThat(resizableComponent.size.height).isEqualTo(5.0f)
        assertThat(resizableComponent.size.depth).isEqualTo(6.0f)
    }

    @Test
    fun resizableComponent_withAutoUpdateSizeDisabled_doesNotUpdateComponentSizeAfterResize() {
        val entity = createTestEntity() as AndroidXrEntity

        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)

        assertThat(resizableComponent).isNotNull()
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val options = nodeRepository.getReformOptions(entity.getNode())
        resizableComponent.autoUpdateSize = false
        resizableComponent.size = Dimensions(1.0f, 2.0f, 3.0f)

        assertThat(options.currentSize.x).isEqualTo(1.0f)
        assertThat(options.currentSize.y).isEqualTo(2.0f)
        assertThat(options.currentSize.z).isEqualTo(3.0f)

        val mockResizeEventListener = mock<ResizeEventListener>()

        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        assertThat(options.eventCallback).isNotNull()
        assertThat(options.eventExecutor).isNotNull()
        assertThat(entity.reformEventConsumerMap).isNotEmpty()

        // Start the resize.
        val startReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_START,
                /* id= */ 0,
            )

        sendResizeEvent(entity.getNode(), startReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()
        val resizeEventCaptor = argumentCaptor<ResizeEvent>()

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture())

        var resizeEvent = resizeEventCaptor.lastValue

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)

        // End the resize.
        val endReformEvent =
            ShadowReformEvent.create(
                /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                /* state= */ ReformEvent.REFORM_STATE_END,
                /* id= */ 0,
            )
        ShadowReformEvent.extract(endReformEvent).setProposedSize(Vec3(4.0f, 5.0f, 6.0f))

        sendResizeEvent(entity.getNode(), endReformEvent)

        assertThat(fakeExecutor.hasNext()).isTrue()

        fakeExecutor.runAll()

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture())

        resizeEvent = resizeEventCaptor.allValues[2]

        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END)
        // Reform size should be unchanged.
        assertThat(options.currentSize.x).isEqualTo(1.0f)
        assertThat(options.currentSize.y).isEqualTo(2.0f)
        assertThat(options.currentSize.z).isEqualTo(3.0f)
    }

    @Test
    fun resizableComponent_restoresAlphaOnNonResizeEventWhenHidden() {
        val entity = createTestEntity() as AndroidXrEntity
        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        entity.setAlpha(0.9f)
        val mockResizeEventListener = mock<ResizeEventListener>()
        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        // Start a resize event to hide the content.
        sendAndProcessReformEvent(
            entity.getNode(),
            ShadowReformEvent.create(
                ReformEvent.REFORM_TYPE_RESIZE,
                ReformEvent.REFORM_STATE_START,
                0,
            ),
        )

        // Verify content is hidden.
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f)

        // Send a non-resize event (e.g., move).
        sendAndProcessReformEvent(
            entity.getNode(),
            ShadowReformEvent.create(ReformEvent.REFORM_TYPE_MOVE, 0, 0),
        )

        // Verify alpha is restored because a non-resize event was received.
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f)
        verify(mockResizeEventListener, times(1)).onResizeEvent(any())
    }

    @Test
    fun resizableComponent_restoresAlphaOnDetachWhenHidden() {
        val entity = createTestEntity() as AndroidXrEntity
        val resizableComponent =
            ResizableComponentImpl(fakeExecutor, xrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        entity.setAlpha(0.9f)
        val mockResizeEventListener = mock<ResizeEventListener>()
        resizableComponent.addResizeEventListener(
            MoreExecutors.directExecutor(),
            mockResizeEventListener,
        )

        // Start a resize event to hide the content.
        sendAndProcessReformEvent(
            entity.getNode(),
            ShadowReformEvent.create(
                ReformEvent.REFORM_TYPE_RESIZE,
                ReformEvent.REFORM_STATE_START,
                0,
            ),
        )

        // Verify content is hidden.
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f)

        // Detach the component.
        entity.removeComponent(resizableComponent)

        // Verify alpha is restored.
        assertThat(nodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f)
    }

    companion object {
        private val MIN_DIMENSIONS = Dimensions(0f, 0f, 0f)
        private val MAX_DIMENSIONS = Dimensions(10f, 10f, 10f)
    }
}
