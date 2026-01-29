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
import android.content.Context
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.InteractableComponent
import androidx.xr.scenecore.runtime.ResizableComponent
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ScheduledExecutorService
import kotlin.test.expect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class EntityTest {
    private var xrExtensions: XrExtensions? = XrExtensionsProvider.getXrExtensions()
    private val entityManager = EntityManager()
    private val fakeScheduledExecutorService = FakeScheduledExecutorService()
    private val testPose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)

    private lateinit var spatialSceneRuntime: SpatialSceneRuntime
    private lateinit var entity: TestEntity
    private lateinit var activity: Activity

    internal class TestEntity(
        context: Context,
        node: Node,
        extensions: XrExtensions,
        entityManager: EntityManager,
        executor: ScheduledExecutorService,
    ) : AndroidXrEntity(context, node, extensions, entityManager, executor)

    @Before
    fun setUp() {
        expect(true, "XrExtensions should not be null") { xrExtensions != null }

        FakeSpatialApiVersionProvider.testSpatialApiVersion = 1
        activity = Robolectric.buildActivity(Activity::class.java).create().start().get()

        spatialSceneRuntime =
            SpatialSceneRuntime.create(
                activity,
                fakeScheduledExecutorService,
                xrExtensions!!,
                entityManager,
                /* unscaledGravityAlignedActivitySpace= */ false,
            )
        entity =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        entity.parent = spatialSceneRuntime.activitySpace
    }

    @After
    fun tearDown() {
        spatialSceneRuntime.destroy()
        xrExtensions = null
        FakeSpatialApiVersionProvider.testSpatialApiVersion = null
    }

    @Test
    fun getPose_defaultsToPoseInParentSpace() {
        entity.setPose(testPose)
        assertPose(entity.getPose(Space.PARENT), testPose)
    }

    @Test
    fun getPose_parentSpace_returnsParentPose() {
        val activitySpace = spatialSceneRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(
                Vector3(5f, 6f, 7f),
                Quaternion.fromEulerAngles(22f, 33f, 44f),
                Vector3(2f, 2f, 2f),
            )
        )
        assertVector3(activitySpace.getScale(Space.REAL_WORLD), Vector3(2f, 2f, 2f))

        entity.setPose(testPose, Space.PARENT)
        assertPose(entity.getPose(Space.PARENT), testPose)
    }

    @Test
    fun getPose_activitySpace_returnsActivitySpacePose() {
        val activitySpace = spatialSceneRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(
                Vector3(5f, 6f, 7f),
                Quaternion.fromEulerAngles(22f, 33f, 44f),
                Vector3(2f, 2f, 2f),
            )
        )
        assertVector3(activitySpace.getScale(Space.REAL_WORLD), Vector3(2f, 2f, 2f))

        entity.parent = activitySpace
        entity.setPose(testPose, Space.PARENT)
        assertPose(entity.getPose(Space.PARENT), testPose)
        assertPose(entity.getPose(Space.ACTIVITY), testPose)
        assertPose(entity.activitySpacePose, testPose)
    }

    @Test
    fun getPose_worldSpace_returnsWorldSpacePose() {
        entity.setPose(testPose, Space.REAL_WORLD)

        assertPose(entity.getPose(Space.REAL_WORLD), testPose)
    }

    @Test
    fun getPose_invalidSpace_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { entity.getPose(999) }
    }

    @Test
    fun setPose_parentSpace_setsPoseInParentSpace() {
        entity.setPose(testPose, Space.PARENT)

        assertPose(entity.getPose(Space.PARENT), testPose)
    }

    @Test
    fun setPose_activitySpace_setsActivitySpacePose() {
        entity.setPose(testPose, Space.PARENT)
        val child =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        child.parent = entity
        child.setPose(testPose, Space.PARENT)

        assertPose(
            child.getPose(Space.ACTIVITY),
            Pose(Vector3(2.0f, 4.0f, 6.0f), Quaternion.Identity),
        )
        assertPose(child.activitySpacePose, Pose(Vector3(2.0f, 4.0f, 6.0f), Quaternion.Identity))
    }

    @Test
    fun setPose_worldSpace_setsWorldSpacePose() {
        entity.setPose(testPose, Space.REAL_WORLD)

        assertPose(entity.getPose(Space.REAL_WORLD), testPose)
    }

    @Test
    fun setPose_invalidSpace_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { entity.setPose(Pose(), 999) }
    }

    @Test
    fun getScale_parentSpace_returnsParentScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertVector3(entity.getScale(Space.PARENT), scale)
    }

    @Test
    fun getScale_activitySpace_returnsActivitySpaceScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertVector3(entity.getScale(Space.PARENT), scale)
        assertVector3(entity.getScale(Space.ACTIVITY), scale)
    }

    @Test
    fun getScale_worldSpace_returnsWorldSpaceScale() {
        val activitySpace = spatialSceneRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.mWorldSpaceScale = Vector3(2.0f, 2.0f, 2.0f)
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertVector3(
            entity.getScale(Space.REAL_WORLD),
            scale.scale(activitySpace.mWorldSpaceScale),
        )
    }

    @Test
    fun getScale_invalidSpace_throwsException() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertThrows(IllegalArgumentException::class.java) { entity.getScale(999) }
    }

    @Test
    fun setScaleActivitySpace_setsActivitySpaceScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)
        val child =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        child.parent = entity
        child.setScale(scale.scale(scale), Space.ACTIVITY)

        assertVector3(child.getScale(Space.ACTIVITY), scale.scale(scale))
    }

    @Test
    fun setScale_worldSpace_setsWorldSpaceScale() {
        val activitySpace = spatialSceneRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.mWorldSpaceScale = Vector3(2.0f, 2.0f, 2.0f)
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)
        val child =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        child.parent = entity
        child.setScale(scale.scale(scale.scale(activitySpace.mWorldSpaceScale)), Space.REAL_WORLD)

        assertVector3(
            child.getScale(Space.REAL_WORLD),
            scale.scale(scale.scale(activitySpace.mWorldSpaceScale)),
        )
    }

    @Test
    fun getPoseInActivitySpaceWithScale_returnsPose() {
        entity.setPose(testPose, Space.PARENT)
        entity.setScale(Vector3(2f, 2f, 2f), Space.PARENT)
        val child =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        child.setPose(testPose, Space.PARENT)
        child.parent = entity
        child.setScale(Vector3(3f, 3f, 3f), Space.PARENT)
        val grandchild =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        grandchild.setPose(testPose, Space.PARENT)
        grandchild.parent = child
        val activitySpace = spatialSceneRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(
                Vector3(5f, 6f, 7f),
                Quaternion.fromEulerAngles(22f, 33f, 44f),
                Vector3(2f, 2f, 2f),
            )
        )
        assertVector3(activitySpace.getScale(Space.REAL_WORLD), Vector3(2f, 2f, 2f))

        assertPose(entity.getPose(Space.PARENT), testPose)
        assertPose(entity.getPose(Space.ACTIVITY), testPose)

        assertPose(child.getPose(Space.PARENT), testPose)
        assertPose(child.getPose(Space.ACTIVITY), Pose(Vector3(3f, 6f, 9f), Quaternion.Identity))

        grandchild.setPose(testPose, Space.PARENT)
        assertPose(grandchild.getPose(Space.PARENT), testPose)
        assertPose(
            grandchild.getPose(Space.ACTIVITY),
            Pose(Vector3(9f, 18f, 27f), Quaternion.Identity),
        )
    }

    @Test
    fun setScale_invalidSpace_throwsException() {
        entity.setScale(Vector3(1.0f, 2.0f, 3.0f), Space.PARENT)

        assertThrows(IllegalArgumentException::class.java) { entity.setScale(Vector3(), 999) }
    }

    @Test
    fun getAlpha_parentSpace_returnsParentAlpha() {
        entity.setAlpha(0.5f)

        assertThat(entity.getAlpha(Space.PARENT)).isEqualTo(0.5f)
    }

    @Test
    fun getAlpha_activitySpace_returnsActivitySpaceAlpha() {
        entity.setAlpha(0.5f)

        assertThat(entity.getAlpha(Space.ACTIVITY)).isEqualTo(0.5f)
    }

    @Test
    fun getAlpha_worldSpace_returnsWorldSpaceAlpha() {
        entity.setAlpha(0.5f)

        assertThat(entity.getAlpha(Space.REAL_WORLD)).isEqualTo(0.5f)
    }

    @Test
    fun getAlpha_invalidSpace_throwsException() {
        entity.setAlpha(0.5f)
        assertThrows(IllegalArgumentException::class.java) { entity.getAlpha(999) }
    }

    @Test
    fun setAlpha_activitySpace_setsActivitySpaceAlpha() {
        entity.setAlpha(0.5f)
        val child =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        child.parent = entity
        child.setAlpha(0.5f)

        assertThat(child.getAlpha(Space.ACTIVITY)).isEqualTo(0.25f)
    }

    @Test
    fun setAlpha_worldSpace_setsWorldSpaceAlpha() {
        spatialSceneRuntime.activitySpace.setAlpha(4f)
        entity.setAlpha(0.5f)
        val child =
            TestEntity(
                activity,
                xrExtensions!!.createNode(),
                xrExtensions!!,
                entityManager,
                fakeScheduledExecutorService,
            )
        child.parent = entity
        child.setAlpha(0.5f)

        assertThat(child.getAlpha(Space.REAL_WORLD)).isEqualTo(0.25f)
    }

    @Test
    fun hitTest_returnsTransformedHitTest() = runBlocking {
        val distance = 2.0f
        val hitPosition = Vec3(1.0f, 2.0f, 3.0f)
        val surfaceNormal = Vec3(0.0f, 1.0f, 0.0f)
        val surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL
        val extensionsHitTestResult =
            com.android.extensions.xr.space.HitTestResult.Builder(
                    distance,
                    hitPosition,
                    true,
                    surfaceType,
                )
                .setSurfaceNormal(surfaceNormal)
                .build()
        entity.setPose(
            Pose(Vector3(1f, 1f, 1f), Quaternion.fromEulerAngles(Vector3(90f, 0f, 0f))),
            Space.ACTIVITY,
        )
        ShadowXrExtensions.extract(xrExtensions!!)
            .setHitTestResult(activity, extensionsHitTestResult)

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                entity.hitTest(
                    Vector3(1f, 1f, 1f),
                    Vector3(1f, 1f, 1f),
                    ScenePose.HitTestFilter.SELF_SCENE,
                )
            }

        fakeScheduledExecutorService.runAll()
        val hitTestResult = deferredHitTestResult.await()

        assertThat(hitTestResult).isNotNull()
        assertThat(hitTestResult.distance).isEqualTo(distance)
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated 90 degrees about the x axis.
        assertVector3(hitTestResult.hitPosition!!, Vector3(0f, 2f, -1f))
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(0f, 0f, -1f))
        assertThat(hitTestResult.surfaceType)
            .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE)
    }

    @Test
    fun hitTest_withScaledActivitySpace_returnsTransformedHitTest() = runBlocking {
        val distance = 2.0f
        val hitPosition = Vec3(0.5f, 1.0f, 1.5f)
        val surfaceNormal = Vec3(0.0f, 1.0f, 0.0f)
        val surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL
        (spatialSceneRuntime.activitySpace as ActivitySpaceImpl).setOpenXrReferenceSpaceTransform(
            Matrix4.fromScale(2f)
        )
        val extensionsHitTestResult =
            com.android.extensions.xr.space.HitTestResult.Builder(
                    distance,
                    hitPosition,
                    true,
                    surfaceType,
                )
                .setSurfaceNormal(surfaceNormal)
                .build()
        entity.setPose(
            Pose(Vector3(0.5f, 0.5f, 0.5f), Quaternion.fromEulerAngles(Vector3(90f, 0f, 0f))),
            Space.ACTIVITY,
        )
        ShadowXrExtensions.extract(xrExtensions!!)
            .setHitTestResult(activity, extensionsHitTestResult)

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                entity.hitTest(
                    Vector3(1f, 1f, 1f),
                    Vector3(1f, 1f, 1f),
                    ScenePose.HitTestFilter.SELF_SCENE,
                )
            }
        fakeScheduledExecutorService.runAll()
        val hitTestResult = deferredHitTestResult.await()

        assertThat(hitTestResult.distance).isEqualTo(distance)
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated 90 degrees about the x axis.
        assertVector3(hitTestResult.hitPosition!!, Vector3(0f, 1f, -0.5f))
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(0f, 0f, -1f))
        assertThat(hitTestResult.surfaceType)
            .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE)
    }

    @Test
    fun getParent_nullParent_returnsNull() {
        entity.parent = null
        assertThat(entity.parent).isEqualTo(null)
    }

    @Test
    fun getPoseInParentSpace_nullParent_returnsIdentity() {
        entity.parent = null
        entity.setPose(Pose.Identity)
        assertThat(entity.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPoseInActivitySpace_nullParent_throwsException() {
        entity.parent = null
        assertThrows(IllegalStateException::class.java) { entity.getPose(Space.ACTIVITY) }
    }

    @Test
    fun getPoseInRealWorldSpace_nullParent_throwsException() {
        entity.parent = null
        assertThrows(IllegalStateException::class.java) { entity.getPose(Space.REAL_WORLD) }
    }

    @Test
    fun addComponent_addsComponent() {
        val resizableComponent =
            ResizableComponentImpl(
                fakeScheduledExecutorService,
                xrExtensions!!,
                Dimensions(0f, 0f, 0f),
                Dimensions(1f, 1f, 1f),
            )
        entity.addComponent(resizableComponent)

        assertThat(entity.getComponents()).containsExactly(resizableComponent)
    }

    @Test
    fun removeComponent_removesComponent() {
        val resizableComponent =
            ResizableComponentImpl(
                fakeScheduledExecutorService,
                xrExtensions!!,
                Dimensions(0f, 0f, 0f),
                Dimensions(1f, 1f, 1f),
            )
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(fakeScheduledExecutorService) {}
        entity.addComponent(resizableComponent)
        entity.addComponent(interactableComponent)

        entity.removeComponent(resizableComponent)

        assertThat(entity.getComponents()).containsExactly(interactableComponent)
    }

    @Test
    fun getComponentsOfType_getsOnlyComponentOfType() {
        val resizableComponent =
            ResizableComponentImpl(
                fakeScheduledExecutorService,
                xrExtensions!!,
                Dimensions(0f, 0f, 0f),
                Dimensions(1f, 1f, 1f),
            )
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(fakeScheduledExecutorService) {}
        entity.addComponent(resizableComponent)
        entity.addComponent(interactableComponent)

        assertThat(entity.getComponentsOfType(InteractableComponent::class.java))
            .containsExactly(interactableComponent)
        assertThat(entity.getComponentsOfType(ResizableComponent::class.java))
            .containsExactly(resizableComponent)
    }
}
