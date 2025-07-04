/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivityPanelEntity as RtActivityPanelEntity
import androidx.xr.runtime.internal.ActivityPose as RtActivityPose
import androidx.xr.runtime.internal.ActivityPose.HitTestFilterValue as RtHitTestFilterValue
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.AnchorEntity as RtAnchorEntity
import androidx.xr.runtime.internal.Component as RtComponent
import androidx.xr.runtime.internal.Dimensions as RtDimensions
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.GltfEntity as RtGltfEntity
import androidx.xr.runtime.internal.GltfModelResource as RtGltfModelResource
import androidx.xr.runtime.internal.HitTestResult as RtHitTestResult
import androidx.xr.runtime.internal.InputEventListener as RtInputEventListener
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.internal.Space as RtSpace
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.internal.SurfaceEntity as RtSurfaceEntity
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@Suppress("Deprecation")
// TODO - b/421386891: is/setHidden is deprecated; tests need to be updated with is/setEnabled
// TODO: b/329902726 - Add a fake runtime and verify CPM integration.
// TODO: b/369199417 - Update EntityTest once createGltfResourceAsync is default.
@RunWith(RobolectricTestRunner::class)
class EntityTest {
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockGltfModelEntityImpl = mock<RtGltfEntity>()
    private val mockPanelEntityImpl = mock<RtPanelEntity>()
    private val mockAnchorEntityImpl = mock<RtAnchorEntity>()
    private val mockActivityPanelEntity = mock<RtActivityPanelEntity>()
    private val mockGroupEntity = mock<RtEntity>()
    private val mockSurfaceEntity = mock<RtSurfaceEntity>()
    private val entityManager = EntityManager()
    private lateinit var session: Session

    private lateinit var lifecycleManager: LifecycleManager
    private lateinit var activitySpace: ActivitySpace
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var panelEntity: PanelEntity
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private lateinit var groupEntity: Entity
    private lateinit var surfaceEntity: SurfaceEntity

    private val entityActivity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private val mockEntityPlatformAdapter = mock<JxrPlatformAdapter>()
    private lateinit var entitySession: Session

    interface FakeComponent : Component

    interface SubtypeFakeComponent : FakeComponent

    // Introduce a test Runtime ActivitySpace to test the bounds changed callback.
    class TestRtActivitySpace : RtActivitySpace {
        private var boundsChangedListener: RtActivitySpace.OnBoundsChangedListener? = null
        var getBoundsCalled: Boolean = false
        var setScaleCalled: Boolean = false
        var getScaleCalled: Boolean = false
        var setAlphaCalled: Boolean = false
        var getAlphaCalled: Boolean = false
        var setHiddenCalled: Boolean = false
        var getHiddenCalled: Boolean = false
        var getWorldSpaceScaleCalled: Boolean = false
        var getActivitySpaceScaleCalled: Boolean = false

        override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {}

        override fun getPose(@SpaceValue relativeTo: Int): Pose {
            return Pose()
        }

        override val activitySpacePose: Pose = Pose()

        override fun transformPoseTo(pose: Pose, destination: RtActivityPose): Pose {
            return Pose()
        }

        override fun hitTest(
            origin: Vector3,
            direction: Vector3,
            @RtHitTestFilterValue hitTestFilter: Int,
        ): ListenableFuture<RtHitTestResult> {
            return Futures.immediateFuture(
                RtHitTestResult(Vector3(), Vector3(), 0, Float.POSITIVE_INFINITY)
            )
        }

        override fun hitTestRelativeToActivityPose(
            origin: Vector3,
            direction: Vector3,
            @RtHitTestFilterValue hitTestFilter: Int,
            activityPose: RtActivityPose,
        ): ListenableFuture<RtHitTestResult> {
            return Futures.immediateFuture(
                RtHitTestResult(Vector3(), Vector3(), 0, Float.POSITIVE_INFINITY)
            )
        }

        override fun getAlpha(@SpaceValue relativeTo: Int): Float =
            1.0f.apply { getAlphaCalled = true }

        override fun setAlpha(alpha: Float, @SpaceValue relativeTo: Int) {
            setAlphaCalled = true
        }

        override fun isHidden(includeParents: Boolean): Boolean {
            getHiddenCalled = true
            return false
        }

        override fun setHidden(hidden: Boolean): Unit {
            setHiddenCalled = true
        }

        override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
            setScaleCalled = true
        }

        override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
            getScaleCalled = true
            return Vector3()
        }

        override val worldSpaceScale: Vector3
            get() {
                getWorldSpaceScaleCalled = true
                return Vector3()
            }

        override val activitySpaceScale: Vector3
            get() {
                getActivitySpaceScaleCalled = true
                return Vector3()
            }

        override fun addInputEventListener(executor: Executor, listener: RtInputEventListener) {}

        override fun removeInputEventListener(listener: RtInputEventListener) {}

        override var parent: RtEntity? = null

        override fun addChild(child: RtEntity) {}

        override fun addChildren(children: List<RtEntity>) {}

        override val children: List<RtEntity> = emptyList()

        override var contentDescription: String = ""

        override fun dispose() {}

        override fun addComponent(component: RtComponent): Boolean = false

        override fun removeComponent(component: RtComponent) {}

        override fun removeAllComponents() {}

        override val bounds: RtDimensions =
            RtDimensions(1f, 1f, 1f).apply { getBoundsCalled = true }

        override fun getComponents(): List<RtComponent> = emptyList()

        override fun addOnBoundsChangedListener(listener: RtActivitySpace.OnBoundsChangedListener) {
            this.boundsChangedListener = listener
        }

        override fun removeOnBoundsChangedListener(
            listener: RtActivitySpace.OnBoundsChangedListener
        ) {
            this.boundsChangedListener = null
        }

        fun sendBoundsChanged(dimensions: RtDimensions) {
            boundsChangedListener?.onBoundsChanged(dimensions)
        }

        override val recommendedContentBoxInFullSpace: BoundingBox =
            BoundingBox(
                min = Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                max = Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
            )

        override fun <T : RtComponent> getComponentsOfType(type: Class<out T>): List<T> =
            emptyList()

        override fun setOnSpaceUpdatedListener(listener: Runnable?, executor: Executor?) {}
    }

    private val testActivitySpace = TestRtActivitySpace()
    private val mockAnchorEntity = mock<RtAnchorEntity>()

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setUp() = runBlocking {
        whenever(mockEntityPlatformAdapter.spatialEnvironment).thenReturn(mock())
        val mockActivitySpace = mock<RtActivitySpace>()
        whenever(mockEntityPlatformAdapter.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockEntityPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockEntityPlatformAdapter.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockEntityPlatformAdapter.mainPanelEntity).thenReturn(mock())
        whenever(mockEntityPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockAnchorEntity.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        whenever(mockEntityPlatformAdapter.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        entitySession =
            Session(
                entityActivity,
                fakeRuntimeFactory.createRuntime(entityActivity),
                mockEntityPlatformAdapter,
            )

        whenever(mockPlatformAdapter.spatialEnvironment).thenReturn(mock())
        whenever(mockPlatformAdapter.activitySpace).thenReturn(testActivitySpace)
        whenever(mockPlatformAdapter.activitySpaceRootImpl).thenReturn(testActivitySpace)
        whenever(mockPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockPlatformAdapter.loadGltfByAssetName(Mockito.anyString()))
            .thenReturn(Futures.immediateFuture(mock()))
        whenever(mockPlatformAdapter.createGltfEntity(any(), any(), any()))
            .thenReturn(mockGltfModelEntityImpl)
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockPanelEntityImpl)
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        whenever(mockAnchorEntityImpl.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        whenever(mockPlatformAdapter.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        whenever(mockPlatformAdapter.createGroupEntity(any(), any(), any()))
            .thenReturn(mockGroupEntity)
        whenever(mockPlatformAdapter.createSurfaceEntity(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockSurfaceEntity)
        whenever(mockPlatformAdapter.mainPanelEntity).thenReturn(mockPanelEntityImpl)
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)
        lifecycleManager = session.runtime.lifecycleManager
        session.configure(Config(headTracking = Config.HeadTrackingMode.LAST_KNOWN))
        activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)
        gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity = GltfModelEntity.create(mockPlatformAdapter, entityManager, gltfModel)
        panelEntity =
            PanelEntity.create(
                lifecycleManager = lifecycleManager,
                context = activity,
                adapter = mockPlatformAdapter,
                entityManager = entityManager,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
            )
        anchorEntity =
            AnchorEntity.create(
                mockPlatformAdapter,
                entityManager,
                FloatSize2d(),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
                10.seconds.toJavaDuration(),
            )
        activityPanelEntity =
            ActivityPanelEntity.create(
                lifecycleManager = lifecycleManager,
                mockPlatformAdapter,
                entityManager = entityManager,
                IntSize2d(640, 480),
                "test",
                activity,
            )
        groupEntity = GroupEntity.create(mockPlatformAdapter, entityManager, "test")
        surfaceEntity =
            SurfaceEntity.create(
                lifecycleManager = lifecycleManager,
                mockPlatformAdapter,
                entityManager,
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                Pose.Identity,
                SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f),
            )
    }

    @Test
    fun anchorEntityCreateWithNullTimeout_passesNullToImpl() {
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        anchorEntity =
            AnchorEntity.create(
                mockPlatformAdapter,
                entityManager,
                FloatSize2d(),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
            )

        assertThat(anchorEntity).isNotNull()
    }

    @Test
    fun anchorEntity_planeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> {
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)
        }
    }

    @Test
    fun allEntitySetParent_callsRuntimeEntityImplSetParent() {
        panelEntity.parent = activitySpace
        gltfModelEntity.parent = activitySpace
        anchorEntity.parent = activitySpace
        activityPanelEntity.parent = activitySpace

        verify(mockPanelEntityImpl).parent = session.platformAdapter.activitySpace
        verify(mockGltfModelEntityImpl).parent = session.platformAdapter.activitySpace
        verify(mockAnchorEntityImpl).parent = session.platformAdapter.activitySpace
        verify(mockActivityPanelEntity).parent = session.platformAdapter.activitySpace
    }

    @Test
    fun allEntitySetParentNull_SetsNullParent() {
        panelEntity.parent = null
        gltfModelEntity.parent = null
        anchorEntity.parent = null
        activityPanelEntity.parent = null

        verify(mockPanelEntityImpl).parent = null
        verify(mockGltfModelEntityImpl).parent = null
        verify(mockAnchorEntityImpl).parent = null
        verify(mockActivityPanelEntity).parent = null
    }

    @Test
    fun allEntityGetParent_callsRuntimeEntityImplGetParent() {
        val rtActivitySpace = session.platformAdapter.activitySpace
        whenever(mockActivityPanelEntity.parent).thenReturn(rtActivitySpace)
        whenever(mockPanelEntityImpl.parent).thenReturn(mockActivityPanelEntity)
        whenever(mockGltfModelEntityImpl.parent).thenReturn(mockPanelEntityImpl)
        whenever(mockGroupEntity.parent).thenReturn(mockGltfModelEntityImpl)
        whenever(mockAnchorEntityImpl.parent).thenReturn(mockGroupEntity)

        assertThat(activityPanelEntity.parent).isEqualTo(activitySpace)
        assertThat(panelEntity.parent).isEqualTo(activityPanelEntity)
        assertThat(gltfModelEntity.parent).isEqualTo(panelEntity)
        assertThat(groupEntity.parent).isEqualTo(gltfModelEntity)
        assertThat(anchorEntity.parent).isEqualTo(groupEntity)

        verify(mockActivityPanelEntity).parent
        verify(mockPanelEntityImpl).parent
        verify(mockGltfModelEntityImpl).parent
        verify(mockGroupEntity).parent
        verify(mockAnchorEntityImpl).parent
    }

    @Test
    fun allEntityGetParent_nullParent_callsRuntimeEntityImplGetParent() {
        whenever(mockActivityPanelEntity.parent).thenReturn(null)
        whenever(mockPanelEntityImpl.parent).thenReturn(null)
        whenever(mockGltfModelEntityImpl.parent).thenReturn(null)
        whenever(mockGroupEntity.parent).thenReturn(null)
        whenever(mockAnchorEntityImpl.parent).thenReturn(null)

        assertThat(activityPanelEntity.parent).isEqualTo(null)
        assertThat(panelEntity.parent).isEqualTo(null)
        assertThat(gltfModelEntity.parent).isEqualTo(null)
        assertThat(groupEntity.parent).isEqualTo(null)
        assertThat(anchorEntity.parent).isEqualTo(null)

        verify(mockActivityPanelEntity).parent
        verify(mockPanelEntityImpl).parent
        verify(mockGltfModelEntityImpl).parent
        verify(mockGroupEntity).parent
        verify(mockAnchorEntityImpl).parent
    }

    @Test
    fun allEntityAddChild_callsRuntimeEntityImplAddChild() {
        anchorEntity.addChild(panelEntity)
        panelEntity.addChild(gltfModelEntity)
        gltfModelEntity.addChild(activityPanelEntity)

        verify(mockAnchorEntityImpl).addChild(mockPanelEntityImpl)
        verify(mockPanelEntityImpl).addChild(mockGltfModelEntityImpl)
        verify(mockGltfModelEntityImpl).addChild(mockActivityPanelEntity)
    }

    @Test
    fun allEntitySetPose_callsRuntimeEntityImplSetPose() {
        val pose = Pose.Identity

        panelEntity.setPose(pose)
        gltfModelEntity.setPose(pose, Space.PARENT)
        anchorEntity.setPose(pose, Space.ACTIVITY)
        activityPanelEntity.setPose(pose, Space.REAL_WORLD)

        verify(mockPanelEntityImpl).setPose(any(), eq(RtSpace.PARENT))
        verify(mockGltfModelEntityImpl).setPose(any(), eq(RtSpace.PARENT))
        verify(mockAnchorEntityImpl).setPose(any(), eq(RtSpace.ACTIVITY))
        verify(mockActivityPanelEntity).setPose(any(), eq(RtSpace.REAL_WORLD))
    }

    @Test
    fun allEntityGetPose_callsRuntimeEntityImplGetPose() {
        whenever(mockPanelEntityImpl.getPose(RtSpace.PARENT)).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.getPose(RtSpace.PARENT)).thenReturn(Pose())
        whenever(mockAnchorEntityImpl.getPose(RtSpace.ACTIVITY)).thenReturn(Pose())
        whenever(mockActivityPanelEntity.getPose(RtSpace.REAL_WORLD)).thenReturn(Pose())
        val pose = Pose.Identity

        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(gltfModelEntity.getPose(Space.PARENT)).isEqualTo(pose)
        assertThat(anchorEntity.getPose(Space.ACTIVITY)).isEqualTo(pose)
        assertThat(activityPanelEntity.getPose(Space.REAL_WORLD)).isEqualTo(pose)

        verify(mockPanelEntityImpl).getPose(RtSpace.PARENT)
        verify(mockGltfModelEntityImpl).getPose(RtSpace.PARENT)
        verify(mockAnchorEntityImpl).getPose(RtSpace.ACTIVITY)
        verify(mockActivityPanelEntity).getPose(RtSpace.REAL_WORLD)
    }

    @Test
    fun allEntityGetActivitySpacePose_callsRuntimeEntityImplGetActivitySpacePose() {
        whenever(mockPanelEntityImpl.activitySpacePose).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.activitySpacePose).thenReturn(Pose())
        whenever(mockAnchorEntityImpl.activitySpacePose).thenReturn(Pose())
        whenever(mockActivityPanelEntity.activitySpacePose).thenReturn(Pose())
        val pose = Pose.Identity

        assertThat(panelEntity.activitySpacePose).isEqualTo(pose)
        assertThat(gltfModelEntity.activitySpacePose).isEqualTo(pose)
        assertThat(anchorEntity.activitySpacePose).isEqualTo(pose)
        assertThat(activityPanelEntity.activitySpacePose).isEqualTo(pose)

        verify(mockPanelEntityImpl).activitySpacePose
        verify(mockGltfModelEntityImpl).activitySpacePose
        verify(mockAnchorEntityImpl).activitySpacePose
        verify(mockActivityPanelEntity).activitySpacePose
    }

    @Test
    fun allEntitySetAlpha_callsRuntimeEntityImplSetAlpha() {
        val alpha = 0.1f

        panelEntity.setAlpha(alpha)
        gltfModelEntity.setAlpha(alpha, Space.PARENT)
        anchorEntity.setAlpha(alpha, Space.ACTIVITY)
        activityPanelEntity.setAlpha(alpha, Space.REAL_WORLD)
        groupEntity.setAlpha(alpha)
        activitySpace.setAlpha(alpha)

        verify(mockPanelEntityImpl).setAlpha(alpha, RtSpace.PARENT)
        verify(mockGltfModelEntityImpl).setAlpha(alpha, RtSpace.PARENT)
        verify(mockAnchorEntityImpl).setAlpha(alpha, RtSpace.ACTIVITY)
        verify(mockActivityPanelEntity).setAlpha(alpha, RtSpace.REAL_WORLD)
        verify(mockGroupEntity).setAlpha(alpha, RtSpace.PARENT)
        assertThat(testActivitySpace.setAlphaCalled).isTrue()
    }

    @Test
    fun allEntityGetAlpha_callsRuntimeEntityImplGetAlpha() {
        whenever(mockPanelEntityImpl.getAlpha(RtSpace.PARENT)).thenReturn(0.1f)
        whenever(mockGltfModelEntityImpl.getAlpha(RtSpace.PARENT)).thenReturn(0.1f)
        whenever(mockAnchorEntityImpl.getAlpha(RtSpace.ACTIVITY)).thenReturn(0.1f)
        whenever(mockActivityPanelEntity.getAlpha(RtSpace.REAL_WORLD)).thenReturn(0.1f)

        assertThat(panelEntity.getAlpha()).isEqualTo(0.1f)
        assertThat(gltfModelEntity.getAlpha(Space.PARENT)).isEqualTo(0.1f)
        assertThat(anchorEntity.getAlpha(Space.ACTIVITY)).isEqualTo(0.1f)
        assertThat(activityPanelEntity.getAlpha(Space.REAL_WORLD)).isEqualTo(0.1f)

        assertThat(activitySpace.getAlpha()).isEqualTo(1.0f)
        assertThat(testActivitySpace.getAlphaCalled).isTrue()

        verify(mockPanelEntityImpl).getAlpha(RtSpace.PARENT)
        verify(mockGltfModelEntityImpl).getAlpha(RtSpace.PARENT)
        verify(mockAnchorEntityImpl).getAlpha(RtSpace.ACTIVITY)
        verify(mockActivityPanelEntity).getAlpha(RtSpace.REAL_WORLD)
    }

    @Test
    fun allEntitySetHiddened_callsRuntimeEntityImplSetHidden() {
        panelEntity.setHidden(true)
        gltfModelEntity.setHidden(true)
        anchorEntity.setHidden(true)
        activityPanelEntity.setHidden(false)
        groupEntity.setHidden(false)
        activitySpace.setHidden(false)

        verify(mockPanelEntityImpl).setHidden(true)
        verify(mockGltfModelEntityImpl).setHidden(true)
        verify(mockAnchorEntityImpl).setHidden(true)
        verify(mockActivityPanelEntity).setHidden(false)
        verify(mockGroupEntity).setHidden(false)
        assertThat(testActivitySpace.setHiddenCalled).isTrue()
    }

    @Test
    fun allEntityGetActivitySpaceAlpha_callsRuntimeEntityImplGetActivitySpaceAlpha() {
        whenever(mockPanelEntityImpl.getAlpha(RtSpace.PARENT)).thenReturn(0.1f)
        whenever(mockGltfModelEntityImpl.getAlpha(RtSpace.PARENT)).thenReturn(0.1f)
        whenever(mockAnchorEntityImpl.getAlpha(RtSpace.ACTIVITY)).thenReturn(0.1f)
        whenever(mockActivityPanelEntity.getAlpha(RtSpace.REAL_WORLD)).thenReturn(0.1f)

        assertThat(panelEntity.getAlpha(Space.PARENT)).isEqualTo(0.1f)
        assertThat(gltfModelEntity.getAlpha(Space.PARENT)).isEqualTo(0.1f)
        assertThat(anchorEntity.getAlpha(Space.ACTIVITY)).isEqualTo(0.1f)
        assertThat(activityPanelEntity.getAlpha(Space.REAL_WORLD)).isEqualTo(0.1f)

        assertThat(activitySpace.getAlpha(Space.PARENT)).isEqualTo(1.0f)

        verify(mockPanelEntityImpl).getAlpha(RtSpace.PARENT)
        verify(mockGltfModelEntityImpl).getAlpha(RtSpace.PARENT)
        verify(mockAnchorEntityImpl).getAlpha(RtSpace.ACTIVITY)
        verify(mockActivityPanelEntity).getAlpha(RtSpace.REAL_WORLD)
    }

    @Test
    fun allEntitySetScale_callsRuntimeEntityImplSetScale() {
        val scale = 0.1f

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)
        // Note that in production we expect this to raise an exception, but that should be handled
        // by the runtime Entity.
        anchorEntity.setScale(scale, Space.ACTIVITY)
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        groupEntity.setScale(scale)
        // Note that in production we expect this to do nothing.
        activitySpace.setScale(scale)

        verify(mockPanelEntityImpl).setScale(any(), eq(RtSpace.PARENT))
        verify(mockGltfModelEntityImpl).setScale(any(), eq(RtSpace.PARENT))
        verify(mockAnchorEntityImpl).setScale(any(), eq(RtSpace.ACTIVITY))
        verify(mockActivityPanelEntity).setScale(any(), eq(RtSpace.REAL_WORLD))
        verify(mockGroupEntity).setScale(any(), eq(RtSpace.PARENT))
        assertThat(testActivitySpace.setScaleCalled).isTrue()
    }

    @Test
    fun allEntityGetScale_callsRuntimeEntityImplGetScale() {
        val runtimeScale = Vector3(0.1f, 0.1f, 0.1f)
        val sdkScale = 0.1f

        whenever(mockPanelEntityImpl.getScale(RtSpace.PARENT)).thenReturn(runtimeScale)
        whenever(mockGltfModelEntityImpl.getScale(RtSpace.PARENT)).thenReturn(runtimeScale)
        whenever(mockAnchorEntityImpl.getScale(RtSpace.ACTIVITY)).thenReturn(runtimeScale)
        whenever(mockActivityPanelEntity.getScale(RtSpace.REAL_WORLD)).thenReturn(runtimeScale)

        assertThat(panelEntity.getScale()).isEqualTo(sdkScale)
        assertThat(gltfModelEntity.getScale(Space.PARENT)).isEqualTo(sdkScale)
        assertThat(anchorEntity.getScale(Space.ACTIVITY)).isEqualTo(sdkScale)
        assertThat(activityPanelEntity.getScale(Space.REAL_WORLD)).isEqualTo(sdkScale)

        // This is unrealistic, but we want to make sure the SDK delegates to the runtimeImpl.
        assertThat(activitySpace.getScale()).isEqualTo(0f)
        assertThat(testActivitySpace.getScaleCalled).isTrue()

        verify(mockPanelEntityImpl).getScale(RtSpace.PARENT)
        verify(mockGltfModelEntityImpl).getScale(RtSpace.PARENT)
        verify(mockAnchorEntityImpl).getScale(RtSpace.ACTIVITY)
        verify(mockActivityPanelEntity).getScale(RtSpace.REAL_WORLD)
    }

    @Test
    fun allEntityTransformPoseTo_callsRuntimeEntityImplTransformPoseTo() {
        whenever(mockPanelEntityImpl.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockAnchorEntityImpl.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockActivityPanelEntity.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockGroupEntity.transformPoseTo(any(), any())).thenReturn(Pose())
        val pose = Pose.Identity

        assertThat(panelEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(gltfModelEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(anchorEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(activityPanelEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(groupEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)

        verify(mockPanelEntityImpl).transformPoseTo(any(), any())
        verify(mockGltfModelEntityImpl).transformPoseTo(any(), any())
        verify(mockAnchorEntityImpl).transformPoseTo(any(), any())
        verify(mockActivityPanelEntity).transformPoseTo(any(), any())
        verify(mockGroupEntity).transformPoseTo(any(), any())
    }

    @Test
    fun allPanelEntitySetSizeInPixels_callsRuntimeEntityImplsetSizeInPixels() {
        val dimensions = IntSize2d(320, 240)
        panelEntity.sizeInPixels = dimensions
        activityPanelEntity.sizeInPixels = dimensions

        verify(mockPanelEntityImpl).sizeInPixels = any()
        verify(mockActivityPanelEntity).sizeInPixels = any()
    }

    @Test
    fun allPanelEntityGetSizeInPixels_callsRuntimeEntityImplgetSizeInPixels() {
        val pixelDimensions = RtPixelDimensions(320, 240)
        val expectedPixelDimensions = pixelDimensions.toIntSize2d()

        whenever(mockPanelEntityImpl.sizeInPixels).thenReturn(pixelDimensions)
        whenever(mockActivityPanelEntity.sizeInPixels).thenReturn(pixelDimensions)

        assertThat(panelEntity.sizeInPixels).isEqualTo(expectedPixelDimensions)
        assertThat(activityPanelEntity.sizeInPixels).isEqualTo(expectedPixelDimensions)

        verify(mockPanelEntityImpl).sizeInPixels
        verify(mockActivityPanelEntity).sizeInPixels
    }

    @Test
    fun panelEntityGetPerceivedResolution_callsRuntimeAndConverts() {
        // Arrange
        val runtimePixelDimensions = RtPixelDimensions(100, 200)
        val runtimeResult = RtPerceivedResolutionResult.Success(runtimePixelDimensions)
        whenever(mockPanelEntityImpl.getPerceivedResolution()).thenReturn(runtimeResult)

        val result = panelEntity.getPerceivedResolution()
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(100)
        assertThat(successResult.perceivedResolution.height).isEqualTo(200)
        verify(mockPanelEntityImpl).getPerceivedResolution()

        val runtimeResult2 = RtPerceivedResolutionResult.InvalidCameraView()
        whenever(mockPanelEntityImpl.getPerceivedResolution()).thenReturn(runtimeResult2)
        assertThat(panelEntity.getPerceivedResolution())
            .isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)

        val runtimeResult3 = RtPerceivedResolutionResult.EntityTooClose()
        whenever(mockPanelEntityImpl.getPerceivedResolution()).thenReturn(runtimeResult3)
        assertThat(panelEntity.getPerceivedResolution())
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun activityPanelEntityGetPerceivedResolution_callsRuntimeAndConverts() {
        // Arrange
        val runtimePixelDimensions = RtPixelDimensions(100, 200)
        val runtimeResult = RtPerceivedResolutionResult.Success(runtimePixelDimensions)
        whenever(mockActivityPanelEntity.getPerceivedResolution()).thenReturn(runtimeResult)

        val result = activityPanelEntity.getPerceivedResolution()
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(100)
        assertThat(successResult.perceivedResolution.height).isEqualTo(200)
        verify(mockActivityPanelEntity).getPerceivedResolution()
    }

    @Test
    fun panelEntity_getPerceivedResolution_headTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(headTracking = Config.HeadTrackingMode.DISABLED))

        val exception =
            assertFailsWith<IllegalStateException> { panelEntity.getPerceivedResolution() }
        assertThat(exception.message).isEqualTo("Config.HeadTrackingMode is set to Disabled.")
    }

    @Test
    fun surfaceEntity_getPerceivedResolution_headTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(headTracking = Config.HeadTrackingMode.DISABLED))

        val exception =
            assertFailsWith<IllegalStateException> { surfaceEntity.getPerceivedResolution() }
        assertThat(exception.message).isEqualTo("Config.HeadTrackingMode is set to Disabled.")
    }

    @Test
    fun activityPanelEntity_getPerceivedResolution_headTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(headTracking = Config.HeadTrackingMode.DISABLED))

        val exception =
            assertFailsWith<IllegalStateException> { activityPanelEntity.getPerceivedResolution() }
        assertThat(exception.message).isEqualTo("Config.HeadTrackingMode is set to Disabled.")
    }

    @Test
    fun allEntityDispose_callsRuntimeEntityImplDispose() {
        gltfModelEntity.dispose()
        panelEntity.dispose()
        anchorEntity.dispose()
        activityPanelEntity.dispose()

        verify(mockGltfModelEntityImpl).dispose()
        verify(mockPanelEntityImpl).dispose()
        verify(mockAnchorEntityImpl).dispose()
        verify(mockActivityPanelEntity).dispose()
    }

    @Test
    fun activityPanelEntityLaunchActivity_callsImplLaunchActivity() {
        val launchIntent = Intent(activity.applicationContext, ComponentActivity::class.java)
        activityPanelEntity.launchActivity(launchIntent, null)

        verify(mockActivityPanelEntity).launchActivity(launchIntent, null)
    }

    @Test
    fun activityPanelEntityMoveActivity_callsImplMoveActivity() {
        activityPanelEntity.moveActivity(activity)

        verify(mockActivityPanelEntity).moveActivity(any())
    }

    @Test
    fun activitySpaceGetBounds_callsImplGetBounds() {
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)

        assertThat(activitySpace.bounds).isNotNull()
        assertThat(testActivitySpace.getBoundsCalled).isTrue()
    }

    @Test
    fun activitySpaceAddBoundsListener_receivesBoundsChangedCallback() {
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)
        var called = false
        val boundsChangedListener =
            Consumer<FloatSize3d> { newBounds ->
                assertThat(newBounds.width).isEqualTo(0.3f)
                assertThat(newBounds.height).isEqualTo(0.2f)
                assertThat(newBounds.depth).isEqualTo(0.1f)
                called = true
            }

        activitySpace.addOnBoundsChangedListener(directExecutor(), boundsChangedListener)
        testActivitySpace.sendBoundsChanged(RtDimensions(0.3f, 0.2f, 0.1f))
        assertThat(called).isTrue()

        called = false
        activitySpace.removeOnBoundsChangedListener(boundsChangedListener)
        testActivitySpace.sendBoundsChanged(RtDimensions(0.5f, 0.5f, 0.5f))
        assertThat(called).isFalse()
    }

    @Test
    fun addRemoveOnSpaceUpdatedListener_callsRuntimeSetOnSpaceUpdatedListener() {
        val mockRtActivitySpace = mock<RtActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockRtActivitySpace)
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)

        val listener = Runnable { print("Hello, World") }
        activitySpace.addOnSpaceUpdatedListener(listener)

        verify(mockRtActivitySpace).setOnSpaceUpdatedListener(any(), eq(null))

        activitySpace.removeOnSpaceUpdatedListener(listener)
        verify(mockRtActivitySpace).setOnSpaceUpdatedListener(eq(null), eq(null))
    }

    @Test
    fun addOnSpaceUpdatedListener_receivesRuntimeSetOnSpaceUpdatedListenerCallbacks() {
        val mockRtActivitySpace = mock<RtActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockRtActivitySpace)
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)

        var listenerCalled = false
        val captor = argumentCaptor<Runnable>()
        activitySpace.addOnSpaceUpdatedListener(directExecutor()) { listenerCalled = true }
        verify(mockRtActivitySpace).setOnSpaceUpdatedListener(captor.capture(), anyOrNull())
        captor.firstValue.run()
        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun setOnSpaceUpdatedListener_anchorEntity_withNullParams_callsRuntimeSetOnSpaceUpdatedListener() {
        anchorEntity.setOnSpaceUpdatedListener(null)
        verify(mockAnchorEntityImpl).setOnSpaceUpdatedListener(eq(null), eq(null))
    }

    @Test
    fun setOnSpaceUpdatedListener_anchorEntity_receivesRuntimeSetOnSpaceUpdatedListenerCallbacks() {
        var listenerCalled = false
        val captor = argumentCaptor<Runnable>()
        anchorEntity.setOnSpaceUpdatedListener(directExecutor()) { listenerCalled = true }

        verify(mockAnchorEntityImpl).setOnSpaceUpdatedListener(captor.capture(), any())
        assertThat(listenerCalled).isFalse()
        captor.firstValue.run()
        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun mainPanelEntity_isMainPanelEntity() {
        val mainPanelEntity = session.scene.mainPanelEntity
        assertThat(mainPanelEntity.isMainPanelEntity).isTrue()
    }

    @Test
    fun mainPanelEntity_isSingleton() {
        val mainPanelEntity = session.scene.mainPanelEntity
        val mainPanelEntity2 = session.scene.mainPanelEntity

        assertThat(mainPanelEntity2).isSameInstanceAs(mainPanelEntity)
        verify(mockPlatformAdapter, times(1)).mainPanelEntity
    }

    @Test
    fun groupEntity_isCreated() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
    }

    @Test
    fun groupEntity_canSetPose() {
        val entity = GroupEntity.create(session, "test")
        val setPose = Pose.Identity
        entity.setPose(setPose)

        val captor = argumentCaptor<Pose>()
        verify(mockGroupEntity).setPose(captor.capture(), eq(RtSpace.PARENT))

        val pose = captor.firstValue
        assertThat(pose.translation.x).isEqualTo(setPose.translation.x)
        assertThat(pose.translation.y).isEqualTo(setPose.translation.y)
        assertThat(pose.translation.z).isEqualTo(setPose.translation.z)
    }

    @Test
    fun groupEntity_canGetPose() {
        whenever(mockGroupEntity.getPose(RtSpace.PARENT)).thenReturn(Pose())

        val entity = GroupEntity.create(session, "test")
        val pose = Pose.Identity

        assertThat(entity.getPose()).isEqualTo(pose)
        verify(mockGroupEntity).getPose(RtSpace.PARENT)
    }

    @Test
    fun groupEntity_canGetActivitySpacePose() {
        whenever(mockGroupEntity.activitySpacePose).thenReturn(Pose())

        val entity = GroupEntity.create(session, "test")
        val pose = Pose.Identity

        assertThat(entity.activitySpacePose).isEqualTo(pose)
        verify(mockGroupEntity).activitySpacePose
    }

    @Test
    fun allEntity_addComponentInvokesOnAttach() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_addComponentFailsIfOnAttachFails() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(false)

        assertThat(panelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isFalse()
        verify(component).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_removeComponentInvokesOnDetach() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)

        panelEntity.removeComponent(component)
        verify(component).onDetach(panelEntity)

        gltfModelEntity.removeComponent(component)
        verify(component).onDetach(gltfModelEntity)

        anchorEntity.removeComponent(component)
        verify(component).onDetach(anchorEntity)

        activityPanelEntity.removeComponent(component)
        verify(component).onDetach(activityPanelEntity)
    }

    @Test
    fun allEntity_addingSameComponentTypeAgainSucceeds() {
        val component1 = mock<Component>()
        val component2 = mock<Component>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(panelEntity)
    }

    @Test
    fun allEntity_addDifferentComponentTypesInvokesOnAttachOnAll() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_removeAllComponentsInvokesOnDetachOnAll() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(activityPanelEntity)

        panelEntity.removeAllComponents()
        verify(component1).onDetach(panelEntity)
        verify(component2).onDetach(panelEntity)

        gltfModelEntity.removeAllComponents()
        verify(component1).onDetach(gltfModelEntity)
        verify(component2).onDetach(gltfModelEntity)

        anchorEntity.removeAllComponents()
        verify(component1).onDetach(anchorEntity)
        verify(component2).onDetach(anchorEntity)

        activityPanelEntity.removeAllComponents()
        verify(component1).onDetach(activityPanelEntity)
        verify(component2).onDetach(activityPanelEntity)
    }

    @Test
    fun allEntity_addSameComponentMultipleTimesInvokesOnAttachMutipleTimes() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_removeSameComponentMultipleTimesInvokesOnDetachOnce() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        panelEntity.removeComponent(component)
        panelEntity.removeComponent(component)
        verify(component).onDetach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        gltfModelEntity.removeComponent(component)
        gltfModelEntity.removeComponent(component)
        verify(component).onDetach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        anchorEntity.removeComponent(component)
        anchorEntity.removeComponent(component)
        verify(component).onDetach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)

        activityPanelEntity.removeComponent(component)
        activityPanelEntity.removeComponent(component)
        verify(component).onDetach(activityPanelEntity)
    }

    @Test
    fun anchorEntity_createPersistAnchorSuccess() {
        whenever(mockPlatformAdapter.createPersistedAnchorEntity(any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        val persistAnchorEntity =
            AnchorEntity.create(mockPlatformAdapter, entityManager, UUID.randomUUID())
        assertThat(persistAnchorEntity).isNotNull()
    }

    @Test
    fun allEntity_disposeRemovesAllComponents() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        panelEntity.dispose()
        verify(component).onDetach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        gltfModelEntity.dispose()
        verify(component).onDetach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        anchorEntity.dispose()
        verify(component).onDetach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)

        activityPanelEntity.dispose()
        verify(component).onDetach(activityPanelEntity)
    }

    @Test
    fun allEntity_getComponentsReturnsAttachedComponents() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)
        assertThat(panelEntity.getComponents()).containsExactly(component1, component2)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(gltfModelEntity)
        assertThat(gltfModelEntity.getComponents()).containsExactly(component1, component2)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(anchorEntity)
        assertThat(anchorEntity.getComponents()).containsExactly(component1, component2)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(activityPanelEntity)
        assertThat(activityPanelEntity.getComponents()).containsExactly(component1, component2)
    }

    @Test
    fun getComponentsOfType_returnsAttachedComponents() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        val component3 = mock<SubtypeFakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)
        whenever(component3.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        assertThat(panelEntity.addComponent(component3)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)
        verify(component3).onAttach(panelEntity)
        assertThat(panelEntity.getComponentsOfType(FakeComponent::class.java))
            .containsExactly(component2, component3)
    }

    @Test
    fun SurfaceEntity_redirectsCallsToRtEntity() {
        surfaceEntity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
        verify(mockSurfaceEntity).stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM

        @Suppress("UNUSED_VARIABLE") var unusedMode = surfaceEntity.stereoMode
        verify(mockSurfaceEntity).stereoMode

        surfaceEntity.canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(1.0f)
        verify(mockSurfaceEntity).canvasShape = any()

        // no equivalent test for getter - that just returns the Kotlin object for now.
    }

    @Test
    fun surfaceEntity_getPerceivedResolution_callsRuntimeAndConverts() {
        // Arrange
        val runtimePixelDimensions = RtPixelDimensions(100, 200)
        val runtimeResult = RtPerceivedResolutionResult.Success(runtimePixelDimensions)
        whenever(mockSurfaceEntity.getPerceivedResolution()).thenReturn(runtimeResult)

        val scenecoreResult = surfaceEntity.getPerceivedResolution()
        verify(mockSurfaceEntity).getPerceivedResolution()
        assertThat(scenecoreResult).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = scenecoreResult as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(100)
        assertThat(successResult.perceivedResolution.height).isEqualTo(200)
    }

    @Test
    fun setCornerRadius() {
        val radius = 2.0f
        panelEntity.cornerRadius = radius

        verify(mockPanelEntityImpl).cornerRadius = radius
    }

    @Test
    fun createGltfResourceAsync_callsRuntimeLoadGltf() {
        runBlocking {
            val mockGltfModelResource = mock<RtGltfModelResource>()
            whenever(mockEntityPlatformAdapter.loadGltfByAssetName(anyString()))
                .thenReturn(Futures.immediateFuture(mockGltfModelResource))
            @Suppress("UNUSED_VARIABLE", "NewApi")
            val unused = GltfModel.create(entitySession, Paths.get("test.glb"))

            verify(mockEntityPlatformAdapter).loadGltfByAssetName("test.glb")
        }
    }

    @Test
    fun createGltfEntity_callsRuntimeCreateGltfEntity() {
        runBlocking {
            whenever(mockEntityPlatformAdapter.loadGltfByAssetName(anyString()))
                .thenReturn(Futures.immediateFuture(mock()))
            whenever(mockEntityPlatformAdapter.createGltfEntity(any(), any(), any()))
                .thenReturn(mock())
            @Suppress("NewApi")
            val gltfModel = GltfModel.create(entitySession, Paths.get("test.glb"))
            @Suppress("UNUSED_VARIABLE")
            val unused = GltfModelEntity.create(entitySession, gltfModel)

            verify(mockEntityPlatformAdapter).loadGltfByAssetName(eq("test.glb"))
            verify(mockEntityPlatformAdapter).createGltfEntity(any(), any(), any())
        }
    }

    @Test
    fun createPanelEntity_callsRuntimeCreatePanelEntity() {
        val view = TextView(activity)
        whenever(
                mockEntityPlatformAdapter.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mock())
        @Suppress("UNUSED_VARIABLE")
        val unused = PanelEntity.create(entitySession, view, IntSize2d(720, 480), "test")

        verify(mockEntityPlatformAdapter)
            .createPanelEntity(
                any<Context>(),
                any<Pose>(),
                any<View>(),
                any<RtPixelDimensions>(),
                any<String>(),
                any<RtEntity>(),
            )
    }

    @Test
    fun createAnchorEntity_callsRuntimeCreateAnchorEntity() {
        whenever(mockEntityPlatformAdapter.createAnchorEntity(any(), any(), any(), anyOrNull()))
            .thenReturn(mockAnchorEntityImpl)
        @Suppress("UNUSED_VARIABLE")
        val unused =
            AnchorEntity.create(
                entitySession,
                FloatSize2d(),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
            )

        verify(mockEntityPlatformAdapter).createAnchorEntity(any(), any(), any(), anyOrNull())
    }

    @Test
    fun createActivityPanelEntity_callsRuntimeCreateActivityPanelEntity() {
        whenever(
                mockEntityPlatformAdapter.createActivityPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(mock())
        @Suppress("UNUSED_VARIABLE")
        val unused = ActivityPanelEntity.create(entitySession, IntSize2d(640, 480), "test")

        verify(mockEntityPlatformAdapter)
            .createActivityPanelEntity(any(), any(), any(), any(), any())
    }

    @Test
    fun surfaceEntity_useAferDisposeRaisesIllegalStateException() {
        surfaceEntity.dispose()
        verify(mockSurfaceEntity).dispose()
    }
}
