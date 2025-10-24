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
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivityPanelEntity as RtActivityPanelEntity
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.AnchorEntity as RtAnchorEntity
import androidx.xr.scenecore.runtime.Component as RtComponent
import androidx.xr.scenecore.runtime.Dimensions as RtDimensions
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.GltfEntity as RtGltfEntity
import androidx.xr.scenecore.runtime.GltfModelResource as RtGltfModelResource
import androidx.xr.scenecore.runtime.HitTestResult as RtHitTestResult
import androidx.xr.scenecore.runtime.InputEventListener as RtInputEventListener
import androidx.xr.scenecore.runtime.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.ScenePose as RtScenePose
import androidx.xr.scenecore.runtime.ScenePose.HitTestFilterValue as RtHitTestFilterValue
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.Space as RtSpace
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.runtime.SurfaceEntity as RtSurfaceEntity
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Paths
import java.util.concurrent.Executor
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

// TODO: b/329902726 - Add a fake runtime and verify CPM integration.
// TODO: b/369199417 - Update EntityTest once createGltfResourceAsync is default.
@RunWith(RobolectricTestRunner::class)
class EntityTest {
    private val mFakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private val mockSceneRuntime = mock<SceneRuntime>()
    private val mockRenderingRuntime = mock<RenderingRuntime>()
    private val mockGltfModelEntityImpl = mock<RtGltfEntity>()
    private val mockPanelEntityImpl = mock<RtPanelEntity>()
    private val mockMainPanelEntity = mock<RtPanelEntity>()
    private val mockAnchorEntityImpl = mock<RtAnchorEntity>()
    private val mockActivityPanelEntity = mock<RtActivityPanelEntity>()
    private val mockGroupEntity = mock<RtEntity>()
    private val mockSurfaceEntity = mock<RtSurfaceEntity>()
    private lateinit var entityManager: EntityManager
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

        override fun getGravityAlignedPose(pose: Pose): Pose {
            return pose
        }

        override val activitySpacePose: Pose = Pose()

        override fun transformPoseTo(pose: Pose, destination: RtScenePose): Pose {
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
            scenePose: RtScenePose,
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

        override fun setHidden(hidden: Boolean) {
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

        override var contentDescription: CharSequence = ""

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
            BoundingBox.fromMinMax(
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
        whenever(mockAnchorEntity.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        whenever(mockSceneRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockSceneRuntime.activitySpace).thenReturn(testActivitySpace)
        whenever(mockSceneRuntime.headActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockSceneRuntime.mainPanelEntity).thenReturn(mockMainPanelEntity)
        val mockGltfModelResource = mock<RtGltfModelResource>()
        whenever(mockRenderingRuntime.loadGltfByAssetName(anyString()))
            .thenReturn(Futures.immediateFuture(mockGltfModelResource))
        whenever(mockRenderingRuntime.createGltfEntity(any(), any(), any()))
            .thenReturn(mockGltfModelEntityImpl)
        whenever(
                mockSceneRuntime.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockPanelEntityImpl)
        whenever(mockSceneRuntime.createAnchorEntity()).thenReturn(mockAnchorEntityImpl)
        whenever(mockAnchorEntityImpl.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        whenever(mockSceneRuntime.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        whenever(mockSceneRuntime.createGroupEntity(any(), any(), any()))
            .thenReturn(mockGroupEntity)
        whenever(mockRenderingRuntime.createSurfaceEntity(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockSurfaceEntity)
        session =
            Session(
                activity,
                runtimes =
                    listOf(
                        mFakePerceptionRuntimeFactory.createRuntime(activity),
                        mockSceneRuntime,
                        mockRenderingRuntime,
                    ),
            )
        lifecycleManager = session.perceptionRuntime.lifecycleManager
        session.configure(
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                headTracking = Config.HeadTrackingMode.LAST_KNOWN,
            )
        )
        entityManager = session.scene.entityManager
        activitySpace = ActivitySpace.create(mockSceneRuntime, entityManager)
        gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity =
            GltfModelEntity.create(mockSceneRuntime, mockRenderingRuntime, entityManager, gltfModel)
        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
            )
        anchorEntity =
            AnchorEntity.create(
                session,
                FloatSize2d(),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
                10.seconds.toJavaDuration(),
            )
        activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test")
        groupEntity = GroupEntity.create(session, "test")
        surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            )
    }

    @Test
    fun anchorEntityCreateWithNullTimeout_passesNullToImpl() {
        anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

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

        verify(mockPanelEntityImpl).parent = mockSceneRuntime.activitySpace
        verify(mockGltfModelEntityImpl).parent = mockSceneRuntime.activitySpace
        verify(mockAnchorEntityImpl).parent = mockSceneRuntime.activitySpace
        verify(mockActivityPanelEntity).parent = mockSceneRuntime.activitySpace
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
        val rtActivitySpace = mockSceneRuntime.activitySpace
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
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setPose(pose, Space.ACTIVITY)
        }
        activityPanelEntity.setPose(pose, Space.REAL_WORLD)

        verify(mockPanelEntityImpl).setPose(any(), eq(RtSpace.PARENT))
        verify(mockGltfModelEntityImpl).setPose(any(), eq(RtSpace.PARENT))
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
    fun allEntityGetGravityAlignedPose_callsRuntimeEntityImplGetGravityAlignedPose() {
        val pose = Pose.Identity
        whenever(mockPanelEntityImpl.getGravityAlignedPose(pose)).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.getGravityAlignedPose(pose)).thenReturn(Pose())
        whenever(mockAnchorEntityImpl.getGravityAlignedPose(pose)).thenReturn(Pose())
        whenever(mockActivityPanelEntity.getGravityAlignedPose(pose)).thenReturn(Pose())

        assertThat(panelEntity.getGravityAlignedPose()).isEqualTo(pose)
        assertThat(gltfModelEntity.getGravityAlignedPose()).isEqualTo(pose)
        assertThat(anchorEntity.getGravityAlignedPose()).isEqualTo(pose)
        assertThat(activityPanelEntity.getGravityAlignedPose()).isEqualTo(pose)

        verify(mockPanelEntityImpl).getGravityAlignedPose(any())
        verify(mockGltfModelEntityImpl).getGravityAlignedPose(any())
        verify(mockAnchorEntityImpl).getGravityAlignedPose(any())
        verify(mockActivityPanelEntity).getGravityAlignedPose(any())
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
    fun allEntitySetEnabled_callsRuntimeEntityImplSetHidden() {
        panelEntity.setEnabled(false)
        gltfModelEntity.setEnabled(false)
        anchorEntity.setEnabled(false)
        activityPanelEntity.setEnabled(true)
        groupEntity.setEnabled(true)
        activitySpace.setEnabled(true)

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
    fun allEntitySetScale_float_callsRuntimeEntityImplSetScale() {
        val scale = 0.1f

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)

        // We expect this to raise an exception
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale, Space.ACTIVITY)
        }
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        groupEntity.setScale(scale)
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.setScale(scale) }

        verify(mockPanelEntityImpl).setScale(any(), eq(RtSpace.PARENT))
        verify(mockGltfModelEntityImpl).setScale(any(), eq(RtSpace.PARENT))
        verify(mockActivityPanelEntity).setScale(any(), eq(RtSpace.REAL_WORLD))
        verify(mockGroupEntity).setScale(any(), eq(RtSpace.PARENT))
    }

    @Test
    fun allEntitySetScale_vector_callsRuntimeEntityImplSetScale() {
        val scale = Vector3(0.1f, 0.1f, 0.1f)

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)

        // We expect this to raise an exception
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale, Space.ACTIVITY)
        }
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        groupEntity.setScale(scale)
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.setScale(scale) }

        verify(mockPanelEntityImpl).setScale(any(), eq(RtSpace.PARENT))
        verify(mockGltfModelEntityImpl).setScale(any(), eq(RtSpace.PARENT))
        verify(mockActivityPanelEntity).setScale(any(), eq(RtSpace.REAL_WORLD))
        verify(mockGroupEntity).setScale(any(), eq(RtSpace.PARENT))
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

        assertThrows(IllegalArgumentException::class.java) { activitySpace.getScale() }

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
    fun panelEntity_getPerceivedResolution_deviceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(deviceTracking = Config.DeviceTrackingMode.DISABLED))

        val exception =
            assertFailsWith<IllegalStateException> { panelEntity.getPerceivedResolution() }
        assertThat(exception.message)
            .isEqualTo("Config.DeviceTrackingMode is not set to LastKnown.")
    }

    @Test
    fun surfaceEntity_getPerceivedResolution_deviceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(deviceTracking = Config.DeviceTrackingMode.DISABLED))

        val exception =
            assertFailsWith<IllegalStateException> { surfaceEntity.getPerceivedResolution() }
        assertThat(exception.message)
            .isEqualTo("Config.DeviceTrackingMode is not set to LastKnown.")
    }

    @Test
    fun activityPanelEntity_getPerceivedResolution_deviceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(deviceTracking = Config.DeviceTrackingMode.DISABLED))

        val exception =
            assertFailsWith<IllegalStateException> { activityPanelEntity.getPerceivedResolution() }
        assertThat(exception.message)
            .isEqualTo("Config.DeviceTrackingMode is not set to LastKnown.")
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
        activityPanelEntity.startActivity(launchIntent)

        verify(mockActivityPanelEntity).launchActivity(launchIntent, null)
    }

    @Test
    fun activityPanelEntityTransferActivity_callsImplMoveActivity() {
        activityPanelEntity.transferActivity(activity)

        verify(mockActivityPanelEntity).moveActivity(any())
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
        verify(mockSceneRuntime, times(1)).mainPanelEntity
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
    fun groupEntity_canGetGravityAlignedPose() {
        val pose = Pose.Identity
        whenever(mockGroupEntity.getGravityAlignedPose(pose)).thenReturn(Pose())

        val entity = GroupEntity.create(session, "test")

        assertThat(entity.getGravityAlignedPose()).isEqualTo(pose)
        verify(mockGroupEntity).getGravityAlignedPose(any())
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
    fun surfaceEntity_redirectsCallsToRtEntity() {
        surfaceEntity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
        verify(mockSurfaceEntity).stereoMode = RtSurfaceEntity.StereoMode.TOP_BOTTOM

        @Suppress("UNUSED_VARIABLE") val unusedMode = surfaceEntity.stereoMode
        verify(mockSurfaceEntity).stereoMode

        surfaceEntity.shape = SurfaceEntity.Shape.Sphere(1.0f)
        verify(mockSurfaceEntity).shape = any()

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
            @Suppress("NewApi") val unused = GltfModel.create(session, Paths.get("intest.glb"))

            verify(mockRenderingRuntime).loadGltfByAssetName("intest.glb")
        }
    }

    @Test
    fun createGltfEntity_callsRuntimeCreateGltfEntity() {
        runBlocking {
            val mockInTestglTFModelResource = mock<RtGltfModelResource>()
            whenever(mockRenderingRuntime.loadGltfByAssetName("intest.glb"))
                .thenReturn(Futures.immediateFuture(mockInTestglTFModelResource))

            @Suppress("NewApi") val gltfModel = GltfModel.create(session, Paths.get("intest.glb"))
            val unused = GltfModelEntity.create(session, gltfModel)

            verify(mockRenderingRuntime).loadGltfByAssetName(eq("intest.glb"))
            verify(mockRenderingRuntime)
                .createGltfEntity(any(), eq(mockInTestglTFModelResource), any())
        }
    }

    @Test
    fun createPanelEntity_callsRuntimeCreatePanelEntity() {
        val view = TextView(activity)
        whenever(
                mockSceneRuntime.createPanelEntity(
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
        val unused = PanelEntity.create(session, view, IntSize2d(640, 480), "test")

        verify(mockSceneRuntime)
            .createPanelEntity(
                any<Context>(),
                any<Pose>(),
                any<View>(),
                eq(RtPixelDimensions(640, 480)),
                any<String>(),
                any<RtEntity>(),
            )
    }

    @Test
    fun createAnchorEntity_callsRuntimeCreateAnchorEntity() {
        // setUp creates the anchor entity, make sure it calls the runtime factory.
        verify(mockSceneRuntime).createAnchorEntity()
    }

    @Test
    fun createActivityPanelEntity_callsRuntimeCreateActivityPanelEntity() {
        whenever(mockSceneRuntime.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mock())
        @Suppress("UNUSED_VARIABLE")
        val unused = ActivityPanelEntity.create(session, IntSize2d(320, 240), "test")

        verify(mockSceneRuntime)
            .createActivityPanelEntity(any(), eq(RtPixelDimensions(320, 240)), any(), any(), any())
    }

    @Test
    fun anyEntity_useAfterDisposeRaisesIllegalStateException() {
        panelEntity.dispose()
        surfaceEntity.dispose()
        anchorEntity.dispose()
        groupEntity.dispose()
        activityPanelEntity.dispose()
        gltfModelEntity.dispose()
        activitySpace.dispose()

        assertFailsWith<IllegalStateException> { surfaceEntity.stereoMode }
        assertFailsWith<IllegalStateException> { panelEntity.size }
        assertFailsWith<IllegalStateException> { groupEntity.getScale() }
        assertFailsWith<IllegalStateException> { activityPanelEntity.getPerceivedResolution() }

        assertFailsWith<IllegalStateException> { gltfModelEntity.getGltfModelBoundingBox() }
        assertFailsWith<IllegalStateException> { gltfModelEntity.stopAnimation() }
        assertFailsWith<IllegalStateException> { activitySpace.bounds }

        val component = mock<Component>()

        assertFailsWith<IllegalStateException> { panelEntity.addComponent(component) }
        assertFailsWith<IllegalStateException> { panelEntity.removeComponent(component) }
    }

    @Test
    fun allEntity_disposeTwiceDoesNotCrash() {
        panelEntity.dispose()
        panelEntity.dispose()
        surfaceEntity.dispose()
        surfaceEntity.dispose()
        anchorEntity.dispose()
        anchorEntity.dispose()
        groupEntity.dispose()
        groupEntity.dispose()
        activityPanelEntity.dispose()
        activityPanelEntity.dispose()
        gltfModelEntity.dispose()
        gltfModelEntity.dispose()
        activitySpace.dispose()
        activitySpace.dispose()
    }

    @Test
    fun getChildren_noChildren_returnsEmptyList() {
        // Configure the mock RtEntity to have no children.
        whenever(mockGroupEntity.children).thenReturn(emptyList())

        // Call the getChildren method on the wrapper entity.
        val children = groupEntity.children

        // Verify the returned list is empty and the underlying property was accessed.
        assertThat(children).isEmpty()
        verify(mockGroupEntity).children
    }

    @Test
    fun getChildren_withChildren_returnsAllChildren() {
        // Configure the mock parent (mockGroupEntity) to have two children.
        // The corresponding wrapper entities (panelEntity, gltfModelEntity) were created in setUp.
        val rtChildren = listOf(mockPanelEntityImpl, mockGltfModelEntityImpl)
        whenever(mockGroupEntity.children).thenReturn(rtChildren)

        // Call getChildren on the parent entity.
        val children = groupEntity.children

        // Verify the returned list contains the correct wrapper entities in order.
        assertThat(children).containsExactly(panelEntity, gltfModelEntity).inOrder()
        verify(mockGroupEntity).children
    }

    @Test
    fun dispose_recursivelyDisposesChildren() {
        // Set up a parent (groupEntity) with a child (panelEntity).
        // Mock getChildren() to return the child entity.
        whenever(mockGroupEntity.children).thenReturn(listOf(mockPanelEntityImpl))

        // Dispose of the parent entity.
        groupEntity.dispose()

        // Verify that dispose() was called on both the parent's and the child's runtime
        // entities.
        verify(mockGroupEntity).dispose()
        verify(mockPanelEntityImpl).dispose()
    }

    @Test
    fun dispose_doesNotDisposeMainPanelEntityIfChild() {
        // Get the mainPanelEntity. In the mock setup, this corresponds to mockPanelEntityImpl.
        val mainPanel = session.scene.mainPanelEntity

        // Set the mainPanel as a child of the group.
        whenever(mockGroupEntity.children).thenReturn(listOf(mockMainPanelEntity))
        mainPanel.parent = groupEntity

        // Dispose of the group entity.
        groupEntity.dispose()

        // Verify that the group entity's runtime was disposed.
        verify(mockGroupEntity).dispose()
        // Verify that the mainPanelEntity's runtime was NOT disposed, even though it was a child.
        verify(mockMainPanelEntity, never()).dispose()
    }

    @Test
    fun dispose_doesNotDisposeMainPanelEntityButDisposesItsChildren() {
        // Get the mainPanelEntity. In the mock setup, this corresponds to mockPanelEntityImpl.
        val mainPanel = session.scene.mainPanelEntity

        // Set the mainPanel as a child of the group.
        whenever(mockGroupEntity.children).thenReturn(listOf(mockMainPanelEntity))
        mainPanel.parent = groupEntity

        // Set childOfMainPanel as a child of mainPanel.
        whenever(mockMainPanelEntity.children).thenReturn(listOf(mockPanelEntityImpl))
        panelEntity.parent = mainPanel

        // Dispose of the group entity.
        groupEntity.dispose()

        // Verify that the group entity's runtime was disposed.
        verify(mockGroupEntity).dispose()
        // Verify that the mainPanelEntity's runtime was NOT disposed.
        verify(mockMainPanelEntity, never()).dispose()
        // Verify that the child of the mainPanelEntity WAS disposed.
        verify(mockPanelEntityImpl).dispose()

        // Verify that the mainPanelEntity's parent is now null.
        assertThat(mainPanel.parent).isNull()
    }
}
