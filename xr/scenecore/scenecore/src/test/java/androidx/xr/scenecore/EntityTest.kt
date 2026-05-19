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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import android.content.Intent
import android.os.Build
import android.os.Looper
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.test.filters.SdkSuppress
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeActivityPanelEntity
import androidx.xr.scenecore.testing.FakeAnchorEntity
import androidx.xr.scenecore.testing.FakeGltfModelResource
import androidx.xr.scenecore.testing.FakePanelEntity
import androidx.xr.scenecore.testing.GltfModelEntityTester
import androidx.xr.scenecore.testing.MemoryUtils
import androidx.xr.scenecore.testing.SceneCoreTestRule
import androidx.xr.scenecore.testing.SurfaceEntityTester
import androidx.xr.scenecore.testing.TestGltfAnimation
import com.android.extensions.xr.XrExtensions
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

// TODO: b/329902726 - Add a fake runtime and verify CPM integration.
// TODO: b/369199417 - Update EntityTest once createGltfResourceAsync is default.
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class EntityTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var extensions: XrExtensions
    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var renderingRuntime: RenderingRuntime
    private lateinit var entityRegistry: EntityRegistry
    private lateinit var session: Session
    private lateinit var renderViewpoint: RenderViewpoint
    private lateinit var activitySpace: ActivitySpace
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var panelEntity: PanelEntity
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private lateinit var entity: Entity
    private lateinit var surfaceEntity: SurfaceEntity
    private lateinit var surfaceEntityTester: SurfaceEntityTester

    private lateinit var gltfModelEntityTester: GltfModelEntityTester

    private class TestComponent(val canBeAttached: Boolean) : Component() {
        var onAttached: Int = 0
            private set

        var onDetached: Int = 0
            private set

        override fun onAttach(entity: Entity): Boolean {
            onAttached++
            return canBeAttached
        }

        override fun onDetach(entity: Entity) {
            onDetached++
        }
    }

    private open class FakeComponent(open val canBeAttached: Boolean) : Component() {
        var onAttached: Int = 0
            private set

        var onDetached: Int = 0
            private set

        override fun onAttach(entity: Entity): Boolean {
            onAttached++
            return canBeAttached
        }

        override fun onDetach(entity: Entity) {
            onDetached++
        }
    }

    private class SubtypeFakeComponent(override val canBeAttached: Boolean) :
        FakeComponent(canBeAttached)

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setUp() = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        extensions = XrExtensions()
        sceneRuntime = session.sceneRuntime
        renderingRuntime = session.renderingRuntime
        session.configure(
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                deviceTracking = DeviceTrackingMode.SPATIAL,
            )
        )
        renderViewpoint = RenderViewpoint.left(session)
        entityRegistry = session.scene.entityRegistry
        activitySpace = session.scene.activitySpace
        gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity =
            GltfModelEntity.create(
                sceneRuntime,
                renderingRuntime,
                entityRegistry,
                gltfModel,
                parent = session.scene.activitySpace,
            )
        gltfModelEntityTester = testRule.createTester<GltfModelEntityTester>(gltfModelEntity)
        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
                parent = session.scene.activitySpace,
            )
        anchorEntity =
            AnchorEntity.create(
                session,
                FloatSize2d(),
                PlaneOrientation.ALL,
                PlaneSemanticType.ALL,
                10.seconds.toJavaDuration(),
            )
        activityPanelEntity =
            ActivityPanelEntity.create(
                session,
                IntSize2d(640, 480),
                "test",
                parent = session.scene.activitySpace,
            )
        entity = Entity.create(session, "test")
        surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                parent = session.scene.activitySpace,
            )
        surfaceEntityTester = testRule.createTester<SurfaceEntityTester>(surfaceEntity)
    }

    @Test
    fun allEntity_disposeAndCreateWithNullParent_callsRuntimeEntityImplSetParent() {
        activityPanelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        entity.disposeInternal()

        panelEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        activityPanelEntity =
            ActivityPanelEntity.create(session, IntSize2d(640, 480), "test", parent = null)
        gltfModelEntity =
            GltfModelEntity.create(
                sceneRuntime,
                renderingRuntime,
                entityRegistry,
                gltfModel,
                parent = null,
            )
        entity = Entity.create(session, "test", parent = null)
        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
                parent = null,
            )
        surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                parent = null,
            )

        assertThat(activityPanelEntity.rtEntity.parent).isNull()
        assertThat(gltfModelEntity.rtEntity.parent).isNull()
        assertThat(entity.rtEntity.parent).isNull()
        assertThat(panelEntity.rtEntity.parent).isNull()
        assertThat(surfaceEntity.rtEntity.parent).isNull()

        panelEntity.disposeInternal()

        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                dimensions = FloatSize2d(1f, 1f),
                name = "test",
                parent = null,
            )
        assertThat(panelEntity.rtEntity.parent).isNull()
    }

    @Test
    fun allEntity_disposeAndCreateWithNullParent_getPoseInParentSpace() {
        activityPanelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        entity.disposeInternal()

        panelEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        activityPanelEntity =
            ActivityPanelEntity.create(session, IntSize2d(640, 480), "test", parent = null)
        gltfModelEntity =
            GltfModelEntity.create(
                sceneRuntime,
                renderingRuntime,
                entityRegistry,
                gltfModel,
                parent = null,
            )
        entity = Entity.create(session, "test", parent = null)
        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
                parent = null,
            )
        surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                parent = null,
            )

        activityPanelEntity.getPose(Space.PARENT)
        gltfModelEntity.getPose(Space.PARENT)
        entity.getPose(Space.PARENT)
        panelEntity.getPose(Space.PARENT)
        surfaceEntity.getPose(Space.PARENT)
    }

    @Test
    fun allEntity_disposeAndCreateWithNullParent_getPoseInActivitySpace() {
        activityPanelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        entity.disposeInternal()

        panelEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        activityPanelEntity =
            ActivityPanelEntity.create(session, IntSize2d(640, 480), "test", parent = null)
        gltfModelEntity =
            GltfModelEntity.create(
                sceneRuntime,
                renderingRuntime,
                entityRegistry,
                gltfModel,
                parent = null,
            )
        entity = Entity.create(session, "test", parent = null)
        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
                parent = null,
            )
        surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                parent = null,
            )

        assertFailsWith<IllegalStateException> { activityPanelEntity.getPose(Space.ACTIVITY) }
        assertFailsWith<IllegalStateException> { gltfModelEntity.getPose(Space.ACTIVITY) }
        assertFailsWith<IllegalStateException> { entity.getPose(Space.ACTIVITY) }
        assertFailsWith<IllegalStateException> { panelEntity.getPose(Space.ACTIVITY) }
        assertFailsWith<IllegalStateException> { surfaceEntity.getPose(Space.ACTIVITY) }
    }

    @Test
    fun anchorEntityCreateWithNullTimeout_passesNullToImpl() {
        anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)

        assertThat(anchorEntity).isNotNull()
    }

    @Test
    fun anchorEntity_planeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> {
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)
        }
    }

    @Test
    fun allEntitySetParent_callsRuntimeEntityImplSetParent() {
        panelEntity.parent = activitySpace
        gltfModelEntity.parent = activitySpace
        anchorEntity.parent = activitySpace
        activityPanelEntity.parent = activitySpace

        assertThat(panelEntity.parent).isEqualTo(activitySpace)
        assertThat(panelEntity.rtEntity.parent).isEqualTo(activitySpace.rtEntity)
        assertThat(gltfModelEntity.parent).isEqualTo(activitySpace)
        assertThat(gltfModelEntity.rtEntity.parent).isEqualTo(activitySpace.rtEntity)
        assertThat(anchorEntity.parent).isEqualTo(activitySpace)
        assertThat(anchorEntity.rtEntity.parent).isEqualTo(activitySpace.rtEntity)
        assertThat(activityPanelEntity.parent).isEqualTo(activitySpace)
        assertThat(activityPanelEntity.rtEntity.parent).isEqualTo(activitySpace.rtEntity)
    }

    @Test
    fun allEntityGetParent_callsRuntimeEntityImplGetParent() {
        panelEntity.parent = activityPanelEntity
        gltfModelEntity.parent = panelEntity
        entity.parent = gltfModelEntity
        anchorEntity.parent = entity

        assertThat(activityPanelEntity.parent).isEqualTo(activitySpace)
        assertThat(activityPanelEntity.rtEntity.parent).isEqualTo(activitySpace.rtEntity)
        assertThat(panelEntity.parent).isEqualTo(activityPanelEntity)
        assertThat(panelEntity.rtEntity.parent).isEqualTo(activityPanelEntity.rtEntity)
        assertThat(gltfModelEntity.parent).isEqualTo(panelEntity)
        assertThat(gltfModelEntity.rtEntity.parent).isEqualTo(panelEntity.rtEntity)
        assertThat(entity.parent).isEqualTo(gltfModelEntity)
        assertThat(entity.rtEntity.parent).isEqualTo(gltfModelEntity.rtEntity)
        assertThat(anchorEntity.parent).isEqualTo(entity)
        assertThat(anchorEntity.rtEntity.parent).isEqualTo(entity.rtEntity)
    }

    @Test
    fun allEntityGetParent_nullParent_callsRuntimeEntityImplGetParent() {
        activityPanelEntity.parent = null
        panelEntity.parent = null
        gltfModelEntity.parent = null
        entity.parent = null
        anchorEntity.parent = null

        assertThat(activityPanelEntity.parent).isNull()
        assertThat(activityPanelEntity.rtEntity.parent).isNull()
        assertThat(panelEntity.parent).isNull()
        assertThat(panelEntity.rtEntity.parent).isNull()
        assertThat(gltfModelEntity.parent).isNull()
        assertThat(gltfModelEntity.rtEntity.parent).isNull()
        assertThat(entity.parent).isNull()
        assertThat(entity.rtEntity.parent).isNull()
        assertThat(anchorEntity.parent).isNull()
        assertThat(anchorEntity.rtEntity.parent).isNull()
    }

    @Test
    fun allEntityAddChild_callsRuntimeEntityImplAddChild() {
        anchorEntity.addChild(panelEntity)
        panelEntity.addChild(gltfModelEntity)
        gltfModelEntity.addChild(activityPanelEntity)

        assertThat(panelEntity.parent).isEqualTo(anchorEntity)
        assertThat(panelEntity.rtEntity.parent).isEqualTo(anchorEntity.rtEntity)
        assertThat(gltfModelEntity.parent).isEqualTo(panelEntity)
        assertThat(gltfModelEntity.rtEntity.parent).isEqualTo(panelEntity.rtEntity)
        assertThat(activityPanelEntity.parent).isEqualTo(gltfModelEntity)
        assertThat(activityPanelEntity.rtEntity.parent).isEqualTo(gltfModelEntity.rtEntity)
    }

    @Test
    @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
    fun allEntitySetPose_allEntityGetPose_poseSetCorrectly() {
        val pose = Pose.Identity

        panelEntity.setPose(pose)
        gltfModelEntity.setPose(pose, Space.PARENT)
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setPose(pose, Space.ACTIVITY)
        }
        activityPanelEntity.setPose(pose, Space.REAL_WORLD)

        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(gltfModelEntity.getPose(Space.PARENT)).isEqualTo(pose)
        assertThat(anchorEntity.getPose(Space.ACTIVITY)).isEqualTo(pose)
        assertThat(activityPanelEntity.getPose(Space.REAL_WORLD)).isEqualTo(pose)
    }

    @Test
    fun allEntityGetActivitySpacePose_callsRuntimeEntityImplGetActivitySpacePose() {
        check(panelEntity.rtEntity.activitySpacePose == Pose.Identity)
        check(gltfModelEntity.rtEntity.activitySpacePose == Pose.Identity)
        check(anchorEntity.rtEntity.activitySpacePose == Pose.Identity)
        check(activityPanelEntity.rtEntity.activitySpacePose == Pose.Identity)
    }

    @Test
    fun allEntitySetAlpha_allEntityGetAlpha_alphaSetCorrectly() {
        val alpha = 0.1f

        panelEntity.setAlpha(alpha)
        gltfModelEntity.setAlpha(alpha)
        anchorEntity.setAlpha(alpha)
        activityPanelEntity.setAlpha(alpha)
        entity.setAlpha(alpha)
        activitySpace.setAlpha(alpha)

        assertThat(panelEntity.getAlpha()).isEqualTo(alpha)
        assertThat(gltfModelEntity.getAlpha()).isEqualTo(alpha)
        assertThat(anchorEntity.getAlpha()).isEqualTo(alpha)
        assertThat(activityPanelEntity.getAlpha()).isEqualTo(alpha)
        assertThat(entity.getAlpha()).isEqualTo(alpha)
        assertThat(activitySpace.getAlpha()).isEqualTo(alpha)
    }

    @Test
    fun getAlpha_inActivitySpace_isProductOfParentAndChildAlphas() {
        val parentAlpha1 = 1.0f
        val childAlphaInParentSpace = 0.8f
        val parent = entity
        val child = panelEntity
        parent.setAlpha(parentAlpha1)
        child.setAlpha(childAlphaInParentSpace)

        assertThat(parent.getAlpha(Space.ACTIVITY)).isEqualTo(parentAlpha1)
        assertThat(child.getAlpha(Space.ACTIVITY)).isEqualTo(childAlphaInParentSpace)

        parent.addChild(child)

        // Child's alpha in ACTIVITY space changes after attached to parent.
        assertThat(child.getAlpha(Space.ACTIVITY)).isEqualTo(parentAlpha1 * childAlphaInParentSpace)

        val parentAlpha2 = 0.5f
        parent.setAlpha(parentAlpha2)

        // Child's alpha changes after parent's alpha changed.
        assertThat(child.getAlpha(Space.ACTIVITY)).isEqualTo(parentAlpha2 * childAlphaInParentSpace)
    }

    @Test
    fun setAlpha_withValueGreaterThanOne_isClampedToOne() {
        val entity = entity
        entity.setAlpha(5.0f)
        assertThat(entity.getAlpha()).isEqualTo(1.0f)
    }

    @Test
    fun setAlpha_withNegativeValue_isClampedToZero() {
        val entity = panelEntity
        entity.setAlpha(-2.0f)
        assertThat(entity.getAlpha()).isEqualTo(0.0f)
    }

    @Test
    fun allEntitySetEnabled_callsRuntimeEntityImplSetHidden() {
        panelEntity.setEnabled(false)
        gltfModelEntity.setEnabled(false)
        anchorEntity.setEnabled(false)
        activityPanelEntity.setEnabled(true)
        entity.setEnabled(true)
        activitySpace.setEnabled(true)

        assertThat(panelEntity.rtEntity.isHidden(false)).isTrue()
        assertThat(gltfModelEntity.rtEntity.isHidden(false)).isTrue()
        assertThat(anchorEntity.rtEntity.isHidden(false)).isTrue()
        assertThat(activityPanelEntity.rtEntity.isHidden(false)).isFalse()
        assertThat(entity.isEnabled()).isTrue()
        assertThat(activitySpace.rtEntity.isHidden(false)).isFalse()
    }

    @Test
    @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
    fun allEntitySetScale_float_allEntityGetScale_scaleSetCorrectly() {
        val scale = 0.1f

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)

        // We expect this to raise an exception
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale, Space.ACTIVITY)
        }
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        entity.setScale(scale)
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.setScale(scale) }

        assertThat(panelEntity.getScale()).isEqualTo(scale)
        assertThat(gltfModelEntity.getScale(Space.PARENT)).isEqualTo(scale)
        assertThat(anchorEntity.getScale(Space.ACTIVITY)).isEqualTo(1f)
        assertThat(activityPanelEntity.getScale(Space.REAL_WORLD)).isEqualTo(scale)
        assertThat(entity.getScale()).isEqualTo(scale)
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getScale() }
    }

    @Test
    @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
    fun allEntitySetScale_vector_allEntityGetScale_scaleSetCorrectly() {
        val scale = Vector3(0.1f, 0.1f, 0.1f)
        val sdkScale = 0.1f

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)

        // We expect this to raise an exception
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale, Space.ACTIVITY)
        }
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        entity.setScale(scale)
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.setScale(scale) }

        assertThat(panelEntity.getScale()).isEqualTo(sdkScale)
        assertThat(gltfModelEntity.getScale(Space.PARENT)).isEqualTo(sdkScale)
        assertThat(anchorEntity.getScale(Space.ACTIVITY)).isEqualTo(1.0f)
        assertThat(activityPanelEntity.getScale(Space.REAL_WORLD)).isEqualTo(sdkScale)
        assertThat(entity.getScale()).isEqualTo(sdkScale)
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getScale() }
    }

    @Test
    @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
    fun allEntitySetScale_float_allEntityGetNonUniformScale_scaleSetCorrectly() {
        val scale = Vector3(0.1f, 0.1f, 0.1f)

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)

        // We expect this to raise an exception
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale, Space.ACTIVITY)
        }
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        entity.setScale(scale)
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.setScale(scale) }

        assertThat(panelEntity.getNonUniformScale()).isEqualTo(scale)
        assertThat(gltfModelEntity.getNonUniformScale(Space.PARENT)).isEqualTo(scale)
        assertThat(anchorEntity.getNonUniformScale(Space.ACTIVITY)).isEqualTo(Vector3.One)
        assertThat(activityPanelEntity.getNonUniformScale(Space.REAL_WORLD)).isEqualTo(scale)
        assertThat(entity.getNonUniformScale()).isEqualTo(scale)

        assertThrows(IllegalArgumentException::class.java) { activitySpace.getNonUniformScale() }
    }

    @Test
    @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
    fun allEntitySetScale_vector_allEntityGetNonUniformScale_scaleSetCorrectly() {
        val scale = Vector3(0.1f, 0.1f, 0.1f)

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)

        // We expect this to raise an exception
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale, Space.ACTIVITY)
        }
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        entity.setScale(scale)
        assertThrows(UnsupportedOperationException::class.java) { activitySpace.setScale(scale) }

        assertThat(panelEntity.getNonUniformScale()).isEqualTo(scale)
        assertThat(gltfModelEntity.getNonUniformScale(Space.PARENT)).isEqualTo(scale)
        assertThat(anchorEntity.getNonUniformScale(Space.ACTIVITY)).isEqualTo(Vector3.One)
        assertThat(activityPanelEntity.getNonUniformScale(Space.REAL_WORLD)).isEqualTo(scale)
        assertThat(entity.getNonUniformScale()).isEqualTo(scale)
        assertThrows(IllegalArgumentException::class.java) { activitySpace.getNonUniformScale() }
    }

    @Test
    fun allEntityTransformPoseTo_callsRuntimeEntityImplTransformPoseTo() {
        val pose = Pose.Identity

        assertThat(panelEntity.rtEntity.transformPoseTo(pose, panelEntity.rtScenePose))
            .isEqualTo(pose)
        assertThat(gltfModelEntity.rtEntity.transformPoseTo(pose, panelEntity.rtScenePose))
            .isEqualTo(pose)
        assertThat(anchorEntity.rtEntity.transformPoseTo(pose, panelEntity.rtScenePose))
            .isEqualTo(pose)
        assertThat(activityPanelEntity.rtEntity.transformPoseTo(pose, panelEntity.rtScenePose))
            .isEqualTo(pose)
        assertThat(entity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
    }

    @Test
    fun allPanelEntitySetSizeInPixels_allPanelEntityGetSizeInPixels_correctSize() {
        val dimensions = IntSize2d(320, 240)
        val pixelDimensions = RtPixelDimensions(320, 240)
        val expectedPixelDimensions = pixelDimensions.toIntSize2d()
        panelEntity.sizeInPixels = dimensions
        activityPanelEntity.sizeInPixels = dimensions

        assertThat(panelEntity.sizeInPixels).isEqualTo(expectedPixelDimensions)
        assertThat(activityPanelEntity.sizeInPixels).isEqualTo(expectedPixelDimensions)
    }

    @Test
    fun panelEntityGetPerceivedResolution_callsRuntimeAndConverts() {
        // Arrange
        val runtimePixelDimensions = RtPixelDimensions(100, 200)
        val runtimeResult = RtPerceivedResolutionResult.Success(runtimePixelDimensions)
        (panelEntity.rtEntity as FakePanelEntity).setPerceivedResolution(runtimeResult)

        val result = panelEntity.getPerceivedResolution(renderViewpoint)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(100)
        assertThat(successResult.perceivedResolution.height).isEqualTo(200)

        val runtimeResult2 = RtPerceivedResolutionResult.InvalidRenderViewpoint()
        (panelEntity.rtEntity as FakePanelEntity).setPerceivedResolution(runtimeResult2)
        assertThat(panelEntity.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.InvalidRenderViewpoint::class.java)

        val runtimeResult3 = RtPerceivedResolutionResult.EntityTooClose()
        (panelEntity.rtEntity as FakePanelEntity).setPerceivedResolution(runtimeResult3)
        assertThat(panelEntity.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun activityPanelEntityGetPerceivedResolution_callsRuntimeAndConverts() {
        // Arrange
        val runtimePixelDimensions = RtPixelDimensions(100, 200)
        val runtimeResult = RtPerceivedResolutionResult.Success(runtimePixelDimensions)
        (activityPanelEntity.rtEntity as FakeActivityPanelEntity).setPerceivedResolution(
            runtimeResult
        )

        val result = activityPanelEntity.getPerceivedResolution(renderViewpoint)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(100)
        assertThat(successResult.perceivedResolution.height).isEqualTo(200)
    }

    @Test
    fun allEntityDispose_callsRuntimeEntityImplDispose() {
        gltfModelEntity.disposeInternal()

        panelEntity.disposeInternal()

        anchorEntity.disposeInternal()

        activityPanelEntity.disposeInternal()

        assertThat(gltfModelEntity.isDisposed).isTrue()
        assertThat(panelEntity.isDisposed).isTrue()
        assertThat(anchorEntity.isDisposed).isTrue()
        assertThat(activityPanelEntity.isDisposed).isTrue()
    }

    @Test
    fun activityPanelEntityLaunchActivity_callsImplLaunchActivity() {
        val launchIntent = Intent(activity.applicationContext, ComponentActivity::class.java)
        val rtEntity = activityPanelEntity.rtEntity as FakeActivityPanelEntity
        activityPanelEntity.startActivity(launchIntent)

        assertThat(rtEntity.launchIntent).isEqualTo(launchIntent)
    }

    @Test
    fun activityPanelEntityTransferActivity_callsImplMoveActivity() {
        activityPanelEntity.transferActivity(activity)

        assertThat((activityPanelEntity.rtEntity as FakeActivityPanelEntity).movedActivity)
            .isEqualTo(activity)
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
    }

    @Test
    fun entity_isCreated() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()
    }

    @Test
    fun entity_canSetPose_canGetPose() {
        val entity = Entity.create(session, "test")
        val setPose = Pose(Vector3(1f, 2f, 3f))
        entity.setPose(setPose)
        val pose = entity.getPose()

        assertThat(pose.translation.x).isEqualTo(setPose.translation.x)
        assertThat(pose.translation.y).isEqualTo(setPose.translation.y)
        assertThat(pose.translation.z).isEqualTo(setPose.translation.z)
    }

    @Test
    fun entity_canGetPoseInActivitySpace() {
        val entity = Entity.create(session, "test")
        val pose = Pose.Identity

        assertThat(entity.poseInActivitySpace).isEqualTo(pose)
    }

    @Test
    fun allEntity_addComponentInvokesOnAttach() {
        val component = TestComponent(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        assertThat(anchorEntity.addComponent(component)).isTrue()
        assertThat(activityPanelEntity.addComponent(component)).isTrue()
    }

    @Test
    fun allEntity_addComponentFailsIfOnAttachFails() {
        val component = TestComponent(false)

        assertThat(panelEntity.addComponent(component)).isFalse()
        assertThat(gltfModelEntity.addComponent(component)).isFalse()
        assertThat(anchorEntity.addComponent(component)).isFalse()
        assertThat(activityPanelEntity.addComponent(component)).isFalse()
    }

    @Test
    fun allEntity_removeComponentInvokesOnDetach() {
        val component = TestComponent(true)

        assertThat(panelEntity.addComponent(component)).isTrue()

        panelEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(1)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()

        gltfModelEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(2)

        assertThat(anchorEntity.addComponent(component)).isTrue()

        anchorEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(3)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()

        activityPanelEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(4)
    }

    @Test
    fun allEntity_addingSameComponentTypeAgainSucceeds() {
        val component1 = TestComponent(true)
        val component2 = TestComponent(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
    }

    @Test
    fun allEntity_addDifferentComponentTypesInvokesOnAttachOnAll() {
        val component1 = TestComponent(true)
        val component2 = FakeComponent(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
    }

    @Test
    fun allEntity_removeAllComponentsInvokesOnDetachOnAll() {
        val component1 = TestComponent(true)
        val component2 = FakeComponent(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()

        panelEntity.removeAllComponents()

        assertThat(component1.onDetached).isEqualTo(1)
        assertThat(component2.onDetached).isEqualTo(1)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()

        gltfModelEntity.removeAllComponents()

        assertThat(component1.onDetached).isEqualTo(2)
        assertThat(component2.onDetached).isEqualTo(2)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()

        anchorEntity.removeAllComponents()

        assertThat(component1.onDetached).isEqualTo(3)
        assertThat(component2.onDetached).isEqualTo(3)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()

        activityPanelEntity.removeAllComponents()

        assertThat(component1.onDetached).isEqualTo(4)
        assertThat(component2.onDetached).isEqualTo(4)
    }

    @Test
    fun allEntity_addSameComponentMultipleTimesInvokesOnAttachMultipleTimes() {
        val component = TestComponent(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        assertThat(panelEntity.addComponent(component)).isTrue()
        assertThat(component.onAttached).isEqualTo(2)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        assertThat(component.onAttached).isEqualTo(4)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        assertThat(anchorEntity.addComponent(component)).isTrue()
        assertThat(component.onAttached).isEqualTo(6)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        assertThat(component.onAttached).isEqualTo(8)
    }

    @Test
    fun allEntity_removeSameComponentMultipleTimesInvokesOnDetachOnce() {
        val component = TestComponent(true)

        assertThat(panelEntity.addComponent(component)).isTrue()

        panelEntity.removeComponent(component)
        panelEntity.removeComponent(component)
        assertThat(component.onDetached).isEqualTo(1)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()

        gltfModelEntity.removeComponent(component)
        gltfModelEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(2)

        assertThat(anchorEntity.addComponent(component)).isTrue()

        anchorEntity.removeComponent(component)
        anchorEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(3)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()

        activityPanelEntity.removeComponent(component)
        activityPanelEntity.removeComponent(component)

        assertThat(component.onDetached).isEqualTo(4)
    }

    @Test
    fun allEntity_disposeRemovesAllComponents() {
        val component = TestComponent(true)

        assertThat(panelEntity.addComponent(component)).isTrue()

        panelEntity.disposeInternal()

        assertThat(component.onDetached).isEqualTo(1)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()

        gltfModelEntity.disposeInternal()

        assertThat(component.onDetached).isEqualTo(2)

        assertThat(anchorEntity.addComponent(component)).isTrue()

        anchorEntity.disposeInternal()

        assertThat(component.onDetached).isEqualTo(3)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()

        activityPanelEntity.disposeInternal()

        assertThat(component.onDetached).isEqualTo(4)
    }

    @Test
    fun allEntity_getComponentsReturnsAttachedComponents() {
        val component1 = TestComponent(true)
        val component2 = FakeComponent(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        assertThat(panelEntity.getComponents()).containsExactly(component1, component2)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        assertThat(gltfModelEntity.getComponents()).containsExactly(component1, component2)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        assertThat(anchorEntity.getComponents()).containsExactly(component1, component2)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        assertThat(activityPanelEntity.getComponents()).containsExactly(component1, component2)
    }

    @Test
    fun getComponentsOfType_returnsAttachedComponents() {
        val component1 = TestComponent(true)
        val component2 = FakeComponent(true)
        val component3 = SubtypeFakeComponent(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        assertThat(panelEntity.addComponent(component3)).isTrue()
        assertThat(panelEntity.getComponentsOfType(FakeComponent::class.java))
            .containsExactly(component2, component3)
    }

    @Test
    fun surfaceEntity_setShapeWithSphereRadius() {
        surfaceEntity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM

        assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)

        surfaceEntity.shape = SurfaceEntity.Shape.Sphere(1.0f)

        assertThat(surfaceEntity.shape).isInstanceOf(SurfaceEntity.Shape.Sphere::class.java)

        val shape = surfaceEntity.shape as SurfaceEntity.Shape.Sphere
        assertThat(shape.radius).isEqualTo(1.0f)
    }

    @Test
    fun surfaceEntity_getPerceivedResolution_callsRuntimeAndConverts() {
        // Arrange
        surfaceEntityTester.perceivedResolutionResult =
            PerceivedResolutionResult.Success(IntSize2d(100, 200))

        val scenecoreResult = surfaceEntity.getPerceivedResolution(renderViewpoint)
        assertThat(scenecoreResult).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = scenecoreResult as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(100)
        assertThat(successResult.perceivedResolution.height).isEqualTo(200)
    }

    @Test
    fun surfaceEntity_setShapeWithCornerRadius() {
        val quad = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f), 0.5f)
        surfaceEntity.shape = quad

        assertThat(surfaceEntity.shape).isInstanceOf(SurfaceEntity.Shape.Quad::class.java)

        val shape = surfaceEntity.shape as SurfaceEntity.Shape.Quad
        assertThat(shape.cornerRadius).isEqualTo(0.5f)
    }

    @Test
    fun surfaceEntity_setShapeWithInvalidCornerRadius_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f), -0.5f)
        }
    }

    @Test
    fun surfaceEntity_createQuadWithInvalidExtents_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SurfaceEntity.Shape.Quad(FloatSize2d(-1.0f, 1.0f))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, -1.0f))
        }
    }

    @Test
    fun surfaceEntity_createSphereWithInvalidRadius_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { SurfaceEntity.Shape.Sphere(-1.0f) }
    }

    @Test
    fun surfaceEntity_createHemisphereWithInvalidRadius_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { SurfaceEntity.Shape.Hemisphere(-1.0f) }
    }

    @Test
    fun setCornerRadius() {
        val radius = 2.0f
        panelEntity.cornerRadius = radius

        assertThat((panelEntity.rtEntity as FakePanelEntity).cornerRadius).isEqualTo(radius)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_callsRuntime() {
        val input = Vector2(100f, 100f)

        var sizeInPixels = (panelEntity.rtEntity as FakePanelEntity).sizeInPixels
        var size = (panelEntity.rtEntity as FakePanelEntity).size
        var u = input.x / sizeInPixels.width
        var v = input.y / sizeInPixels.height
        var coordinates = Vector2(u * 2 - 1, (1 - v) * 2 - 1)
        var xInLocal3DSpace = coordinates.x * size.width / 2f
        var yInLocal3DSpace = coordinates.y * size.height / 2f
        val expectedPosition = Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)
        val position = panelEntity.transformPixelCoordinatesToLocalPosition(input)

        assertThat(position).isEqualTo(expectedPosition)

        val input2 = Vector2(200f, 200f)

        sizeInPixels = (activityPanelEntity.rtEntity as FakeActivityPanelEntity).sizeInPixels
        size = (activityPanelEntity.rtEntity as FakeActivityPanelEntity).size
        u = input2.x / sizeInPixels.width
        v = input2.y / sizeInPixels.height
        coordinates = Vector2(u * 2 - 1, (1 - v) * 2 - 1)
        xInLocal3DSpace = coordinates.x * size.width / 2f
        yInLocal3DSpace = coordinates.y * size.height / 2f
        val expectedPosition2 = Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)
        val position2 = activityPanelEntity.transformPixelCoordinatesToLocalPosition(input2)

        assertThat(position2).isEqualTo(expectedPosition2)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_callsRuntime() {
        val input = Vector2(1f, 1f)

        var size = (panelEntity.rtEntity as FakePanelEntity).size
        var xInLocal3DSpace = input.x * size.width / 2f
        var yInLocal3DSpace = input.y * size.height / 2f
        val expectedPosition = Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)
        val position = panelEntity.transformNormalizedCoordinatesToLocalPosition(input)

        assertThat(position).isEqualTo(expectedPosition)

        val input2 = Vector2(2f, 2f)

        size = (activityPanelEntity.rtEntity as FakeActivityPanelEntity).size
        xInLocal3DSpace = input2.x * size.width / 2f
        yInLocal3DSpace = input2.y * size.height / 2f
        val expectedPosition2 = Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)
        val position2 = activityPanelEntity.transformNormalizedCoordinatesToLocalPosition(input2)

        assertThat(position2).isEqualTo(expectedPosition2)
    }

    @Test
    fun createGltfResourceAsync_callsRuntimeLoadGltf() {
        runBlocking {
            @Suppress("NewApi") val gltfModel = GltfModel.create(session, Paths.get("intest.glb"))

            assertThat((gltfModel.model as FakeGltfModelResource).assetName).isEqualTo("intest.glb")
        }
    }

    @Test
    fun createGltfEntity_callsRuntimeCreateGltfEntity() {
        runBlocking {
            @Suppress("NewApi") val gltfModel = GltfModel.create(session, Paths.get("intest.glb"))
            val gltfEntity =
                GltfModelEntity.create(session, gltfModel, parent = session.scene.activitySpace)
            val testData = testRule.createTester<GltfModelEntityTester>(gltfEntity)

            assertThat(testData.gltfModelBoundingBox)
                .isEqualTo(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        }
    }

    @Test
    fun createPanelEntity_callsRuntimeCreatePanelEntity() {
        val view = TextView(activity)
        @Suppress("UNUSED_VARIABLE")
        val panelEntity =
            PanelEntity.create(
                session,
                view,
                IntSize2d(640, 480),
                "test",
                parent = session.scene.activitySpace,
            )

        assertThat((panelEntity.rtEntity as FakePanelEntity).sizeInPixels)
            .isEqualTo(IntSize2d(640, 480).toRtPixelDimensions())
    }

    @Test
    fun createAnchorEntity_callsRuntimeCreateAnchorEntity() {
        val anchorEntity = sceneRuntime.createAnchorEntity()

        assertThat(anchorEntity).isInstanceOf(FakeAnchorEntity::class.java)
    }

    @Test
    fun createActivityPanelEntity_callsRuntimeCreateActivityPanelEntity() {
        @Suppress("UNUSED_VARIABLE")
        val activityPanelEntity =
            ActivityPanelEntity.create(
                session,
                IntSize2d(320, 240),
                "test",
                parent = session.scene.activitySpace,
            )

        assertThat((activityPanelEntity.rtEntity as FakeActivityPanelEntity).sizeInPixels)
            .isEqualTo(IntSize2d(320, 240).toRtPixelDimensions())
    }

    @Test
    fun anyEntity_useAfterDisposeRaisesDisposedException() {
        panelEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        anchorEntity.disposeInternal()

        entity.disposeInternal()

        activityPanelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        activitySpace.disposeInternal()

        assertFailsWith<Entity.DisposedException> { surfaceEntity.stereoMode }
        assertFailsWith<Entity.DisposedException> { panelEntity.sizeInPixels }
        assertFailsWith<Entity.DisposedException> { entity.getScale() }
        assertFailsWith<Entity.DisposedException> {
            activityPanelEntity.getPerceivedResolution(renderViewpoint)
        }

        assertFailsWith<Entity.DisposedException> { gltfModelEntity.getScale() }
        assertFailsWith<Entity.DisposedException> { gltfModelEntity.setPose(Pose.Identity) }
        assertFailsWith<Entity.DisposedException> { activitySpace.getAlpha() }

        val component = TestComponent(true)

        assertFailsWith<Entity.DisposedException> { panelEntity.addComponent(component) }
        assertFailsWith<Entity.DisposedException> { panelEntity.removeComponent(component) }
    }

    @Test
    fun allEntity_disposeTwiceDoesNotCrash() {
        panelEntity.disposeInternal()

        panelEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        anchorEntity.disposeInternal()

        anchorEntity.disposeInternal()

        entity.disposeInternal()

        entity.disposeInternal()

        activityPanelEntity.disposeInternal()

        activityPanelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        activitySpace.disposeInternal()

        activitySpace.disposeInternal()
    }

    @Test
    fun getChildren_noChildren_returnsEmptyList() {
        // Call the getChildren method on the wrapper entity.
        val children = entity.children

        // Verify the returned list is empty and the underlying property was accessed.
        assertThat(children).isEmpty()
    }

    @Test
    fun getChildren_withChildren_returnsAllChildren() {
        // Configure the parent to have two children.
        // The corresponding wrapper entities (panelEntity, gltfModelEntity) were created in setUp.
        panelEntity.parent = entity
        gltfModelEntity.parent = entity

        // Call getChildren on the parent entity.
        val children = entity.children

        // Verify the returned list contains the correct wrapper entities in order.
        assertThat(children).containsExactly(panelEntity, gltfModelEntity).inOrder()
    }

    @Test
    fun isDisposed_falseForNewEntity() {
        assertThat(panelEntity.isDisposed).isFalse()
        assertThat(surfaceEntity.isDisposed).isFalse()
        assertThat(anchorEntity.isDisposed).isFalse()
        assertThat(entity.isDisposed).isFalse()
        assertThat(activityPanelEntity.isDisposed).isFalse()
        assertThat(gltfModelEntity.isDisposed).isFalse()
        assertThat(activitySpace.isDisposed).isFalse()
    }

    @Test
    fun isDisposed_trueAfterDispose() {
        panelEntity.disposeInternal()

        surfaceEntity.disposeInternal()

        anchorEntity.disposeInternal()

        entity.disposeInternal()

        activityPanelEntity.disposeInternal()

        gltfModelEntity.disposeInternal()

        activitySpace.disposeInternal()

        assertThat(panelEntity.isDisposed).isTrue()
        assertThat(surfaceEntity.isDisposed).isTrue()
        assertThat(anchorEntity.isDisposed).isTrue()
        assertThat(entity.isDisposed).isTrue()
        assertThat(activityPanelEntity.isDisposed).isTrue()
        assertThat(gltfModelEntity.isDisposed).isTrue()
        assertThat(activitySpace.isDisposed).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_getAnimations_returnsAnimations() {
        val animation1 = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        val animation2 = TestGltfAnimation.Builder().setAnimationName("anim2").build()
        gltfModelEntityTester.addAnimation(animation1)
        gltfModelEntityTester.addAnimation(animation2)

        val animations = gltfModelEntity.animations

        assertThat(animations).hasSize(2)
        assertThat(animations[0].name).isEqualTo("anim1")
        assertThat(animations[1].name).isEqualTo("anim2")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_startAnimation_startsAnimation() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start()

        assertThat(gltfAnimation.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_startAnimation_withOptions_startsAnimationWithOptions() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start(
            GltfAnimationStartOptions(
                shouldLoop = true,
                speed = 2.0f,
                seekStartTime = 0.5.seconds.toJavaDuration(),
            )
        )

        assertThat(animation.shouldLoop).isTrue()
        assertThat(animation.speed).isEqualTo(2.0f)
        assertThat(animation.seekStartTime).isEqualTo(0.5f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfAnimation_startAnimation_negativeSeekTime_throwsException() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val gltfAnimation = gltfModelEntity.animations[0]

        assertThrows(IllegalArgumentException::class.java) {
            gltfAnimation.start(
                GltfAnimationStartOptions(seekStartTime = (-1).seconds.toJavaDuration())
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_stopAnimation_stopsAnimation() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start()
        gltfAnimation.stop()

        assertThat(gltfAnimation.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_pauseAnimation_pausesAnimation() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start()
        gltfAnimation.pause()

        assertThat(gltfAnimation.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_resumeAnimation_resumesAnimation() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start()
        gltfAnimation.pause()
        gltfAnimation.resume()

        assertThat(gltfAnimation.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_setSpeed_setsAnimationSpeed() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start()
        gltfAnimation.setSpeed(2.0f)

        assertThat(animation.speed).isEqualTo(2.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfModelEntity_seekTo_seeksAnimation() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val animations = gltfModelEntity.animations
        val gltfAnimation = animations[0]

        gltfAnimation.start()
        gltfAnimation.seekTo(0.5.seconds.toJavaDuration())

        assertThat(animation.seekStartTime).isEqualTo(0.5f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfAnimation_seekTo_negativeTime_throwsException() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val gltfAnimation = gltfModelEntity.animations[0]

        gltfAnimation.start()
        assertThrows(IllegalArgumentException::class.java) {
            gltfAnimation.seekTo((-0.5).seconds.toJavaDuration())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfAnimation_animationStateListener_receivesUpdates() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val gltfAnimation = gltfModelEntity.animations[0]

        var state: GltfAnimation.AnimationState? = null
        gltfAnimation.addAnimationStateListener { state = it }

        gltfAnimation.start()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(state).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        gltfAnimation.pause()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(state).isEqualTo(GltfAnimation.AnimationState.PAUSED)

        gltfAnimation.stop()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(state).isEqualTo(GltfAnimation.AnimationState.STOPPED)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gltfAnimation_removeAnimationStateListener_stopsUpdates() {
        val animation = TestGltfAnimation.Builder().setAnimationName("anim1").build()
        gltfModelEntityTester.addAnimation(animation)
        val gltfAnimation = gltfModelEntity.animations[0]

        var state: GltfAnimation.AnimationState? = null
        val listener = java.util.function.Consumer<GltfAnimation.AnimationState> { state = it }
        gltfAnimation.addAnimationStateListener(listener)

        gltfAnimation.start()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(state).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        gltfAnimation.removeAnimationStateListener(listener)
        gltfAnimation.pause()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // The listener should NOT have received the PAUSED update, so it remains PLAYING
        assertThat(state).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        // Verify the actual state is indeed PAUSED
        assertThat(gltfAnimation.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)
    }

    @Test
    fun panelEntity_garbageCollection_disposesEntity() {
        fun createPanelEntity(): WeakReference<PanelEntity> {
            val entity =
                PanelEntity.create(
                    session,
                    view = TextView(activity),
                    pixelDimensions = IntSize2d(720, 480),
                    name = "test",
                    parent = null,
                )
            return WeakReference(entity)
        }

        val entityRef = createPanelEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun surfaceEntity_garbageCollection_disposesEntity() {
        fun createSurfaceEntity(): WeakReference<SurfaceEntity> {
            val entity =
                SurfaceEntity.create(
                    session,
                    Pose.Identity,
                    SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                    SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                    parent = null,
                )
            return WeakReference(entity)
        }

        val entityRef = createSurfaceEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun entity_garbageCollection_disposesEntity() {
        fun createEntity(): WeakReference<Entity> {
            val entity = Entity.create(session, "test", parent = null)
            return WeakReference(entity)
        }

        val entityRef = createEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun gltfModelEntity_garbageCollection_disposesEntity() {
        fun createGltfModelEntity(): WeakReference<GltfModelEntity> {
            val entity =
                GltfModelEntity.create(
                    sceneRuntime,
                    renderingRuntime,
                    entityRegistry,
                    gltfModel,
                    parent = null,
                )
            return WeakReference(entity)
        }

        val entityRef = createGltfModelEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun activityPanelEntity_garbageCollection_disposesEntity() {
        fun createActivityPanelEntity(): WeakReference<ActivityPanelEntity> {
            val entity =
                ActivityPanelEntity.create(session, IntSize2d(320, 240), "test", parent = null)
            return WeakReference(entity)
        }

        val entityRef = createActivityPanelEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun groupEntity_garbageCollection_disposesEntity() {
        fun createGroupEntity(): WeakReference<GroupEntity> {
            @Suppress("DEPRECATION") val entity = GroupEntity.create(session, "test", parent = null)
            return WeakReference(entity)
        }

        val entityRef = createGroupEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun subspaceNodeEntity_garbageCollection_disposesEntity() {
        fun createSubspaceNodeEntity(): WeakReference<SubspaceNodeEntity> {
            val nodeHolder =
                NodeHolder(extensions.createNode(), com.android.extensions.xr.node.Node::class.java)
            val entity = SubspaceNodeEntity.create(session, nodeHolder, FloatSize3d(1f, 1f, 1f))
            return WeakReference(entity)
        }

        val entityRef = createSubspaceNodeEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun entityIsNotCleanedWhenStrongReferenceHeld() {
        val entity: Entity = Entity.create(session, "test")
        val weakRef = WeakReference(entity)

        // Force GC
        System.gc()
        System.runFinalization()

        assertThat(weakRef.get()).isNotNull()
        assertThat(entity).isNotNull()
    }

    @Test
    fun entityIsNotCleanedWhenPartOfScenegraph() {
        val parent = session.scene.activitySpace
        var child: Entity? = Entity.create(session, "child", parent = parent)
        val weakRef = WeakReference(child)

        // Nullify the local strong reference
        child = null

        // Force GC. The parent should still hold a strong reference to the child via its internal
        // children list.
        System.gc()
        System.runFinalization()

        assertThat(weakRef.get()).isNotNull()
    }

    @Test
    fun entityIsCleanedWhenUnreachable() {
        // We need a way to know if the RT entity was disposed.
        // Since we are using Robolectric and FakeRuntime, we can check the registry.
        var entity: Entity? = Entity.create(session, "test", parent = null)
        val rtEntity = entity!!.rtEntity
        val weakRef = WeakReference(entity)

        // Nullify the strong reference and ensure it's not in the scenegraph
        entity = null

        MemoryUtils.assertGarbageCollected(
            weakRef,
            onAttempt = { ShadowLooper.runUiThreadTasksIncludingDelayedTasks() },
            afterGcCondition = { !entityRegistry.containsRtEntity(rtEntity) },
        )
    }

    @Test
    fun entityIsRemovedFromRegistryWhenGc() {
        fun createEntity(): Pair<WeakReference<Entity>, RtEntity> {
            val entity = Entity.create(session, "test", parent = null)
            return Pair(WeakReference(entity), entity.rtEntity)
        }

        val (weakRef, rtEntity) = createEntity()
        assertThat(weakRef.get()).isNotNull()
        assertThat(entityRegistry.containsRtEntity(rtEntity)).isTrue()

        MemoryUtils.assertGarbageCollected(
            weakRef,
            onAttempt = { shadowOf(Looper.getMainLooper()).idle() },
            afterGcCondition = { !entityRegistry.containsRtEntity(rtEntity) },
        )
    }

    @Test
    fun componentsAreGarbageCollectedWithEntity() {
        fun createEntityWithComponent(): Pair<WeakReference<Entity>, WeakReference<Component>> {
            val entity = Entity.create(session, "test", parent = null)
            val component = TestComponent(true)
            entity.addComponent(component)
            return Pair(WeakReference(entity), WeakReference(component))
        }

        val (entityRef, componentRef) = createEntityWithComponent()
        assertThat(entityRef.get()).isNotNull()
        assertThat(componentRef.get()).isNotNull()

        // Nullify and GC
        MemoryUtils.assertGarbageCollected(entityRef)
        MemoryUtils.assertGarbageCollected(componentRef)
    }

    @Test
    fun sceneCloseDisposesRemainingEntities() {
        val entity1 = Entity.create(session, "entity1")
        val entity2 = Entity.create(session, "entity2")

        val rtEntity1 = entity1.rtEntity
        val rtEntity2 = entity2.rtEntity

        assertThat(entityRegistry.getEntityForRtEntity(rtEntity1)).isNotNull()
        assertThat(entityRegistry.getEntityForRtEntity(rtEntity2)).isNotNull()

        session.scene.close()

        assertThat(entity1.isDisposed).isTrue()
        assertThat(entity2.isDisposed).isTrue()
        assertThat(entityRegistry.getEntityForRtEntity(rtEntity1)).isNull()
        assertThat(entityRegistry.getEntityForRtEntity(rtEntity2)).isNull()
    }

    @Test
    fun disposeInternalIsIdempotent() {
        val entity = Entity.create(session, "test")

        assertThat(entity.isDisposed).isFalse()

        entity.disposeInternal()
        assertThat(entity.isDisposed).isTrue()

        // Calling again should not throw
        entity.disposeInternal()
        assertThat(entity.isDisposed).isTrue()
    }

    @Test
    fun sdkEntityIsGarbageCollectedAndDisposesRtEntity() {
        val mockRtEntity = mock<RtEntity>()

        var sdkEntity: Entity? = Entity(mockRtEntity, entityRegistry)
        val weakRef = WeakReference(sdkEntity)

        assertThat(entityRegistry.getEntityForRtEntity(mockRtEntity)).isEqualTo(sdkEntity)

        // Nullify and GC
        @Suppress("UNUSED_VALUE")
        sdkEntity = null
        MemoryUtils.assertGarbageCollected(
            weakRef,
            onAttempt = { shadowOf(Looper.getMainLooper()).idle() },
            afterGcCondition = { entityRegistry.getEntityForRtEntity(mockRtEntity) == null },
        )

        // Verify cleanup: rtEntity.dispose() should have been called
        verify(mockRtEntity).dispose()
    }

    @Test
    fun dispose_setsParentToNull() {
        val parentEntity = Entity.create(session, "parent")
        val childEntity = Entity.create(session, "child", parent = parentEntity)

        assertThat(childEntity.parent).isEqualTo(parentEntity)

        childEntity.dispose()

        assertThat(childEntity.parent).isNull()
    }
}
