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
import android.util.Size
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Matrix4.Companion.fromPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.environment.EnvironmentVisibilityState
import com.android.extensions.xr.environment.PassthroughVisibilityState
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState
import com.android.extensions.xr.node.Box3
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.android.extensions.xr.node.Vec3
import com.android.extensions.xr.space.Bounds
import com.android.extensions.xr.space.ShadowSpatialCapabilities
import com.android.extensions.xr.space.ShadowSpatialState
import com.android.extensions.xr.space.SpatialCapabilities
import com.android.extensions.xr.space.SpatialState
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.DefaultAsserter.fail
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ActivitySpaceImplTest : SystemSpaceEntityImplTest() {
    // TODO(b/329902726): Move this boilerplate for creating a TestSceneRuntime into a test util
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    override val activity: Activity = activityController.create().start().get()
    override val fakeExecutor = FakeScheduledExecutorService()
    private val nodeRepository = NodeRepository.getInstance()
    override lateinit var xrExtensions: XrExtensions
    private lateinit var testRuntime: SceneRuntime
    private lateinit var activitySpace: ActivitySpaceImpl

    private fun createTestSceneRuntime(): SceneRuntime {
        return SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, SceneNodeRegistry())
    }

    /**
     * Creates a new [AndroidXrEntity] for testing.
     *
     * @param node The [Node] to use for the entity.
     * @return The created [AndroidXrEntity].
     */
    override fun createEntity(node: Node): AndroidXrEntity {
        return ActivitySpaceImpl(
            node,
            activity,
            xrExtensions,
            SceneNodeRegistry(),
            { xrExtensions.getSpatialState(activity) },
            fakeExecutor,
        )
    }

    @Before
    fun setUp() {
        xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy

        testRuntime = createTestSceneRuntime()

        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl

        // This is slightly hacky. We're grabbing the singleton instance of the ActivitySpaceImpl
        // that was created by the RuntimeImpl. Ideally we'd have an interface to inject the
        // ActivitySpace for testing.  For now this is fine since there isn't an interface
        // difference (yet).
        assertThat(activitySpace).isNotNull()
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        testRuntime.destroy()
    }

    override val systemSpaceEntityImpl: SystemSpaceEntityImpl
        get() = activitySpace

    override val defaultFakeExecutor: FakeScheduledExecutorService
        get() = fakeExecutor

    override fun createChildAndroidXrEntity(): AndroidXrEntity {
        return testRuntime.createEntity(Pose(), "child", activitySpace) as AndroidXrEntity
    }

    override val activitySpaceEntity: ActivitySpaceImpl
        get() = activitySpace

    private fun createSpatialState(bounds: Bounds): SpatialState {
        val isUnbounded =
            bounds.width == Float.POSITIVE_INFINITY &&
                bounds.height == Float.POSITIVE_INFINITY &&
                bounds.depth == Float.POSITIVE_INFINITY
        val capabilities: SpatialCapabilities =
            if (isUnbounded) {
                ShadowSpatialCapabilities.createAll()
            } else {
                ShadowSpatialCapabilities.create(0)
            }
        return ShadowSpatialState.create(
            /* bounds= */ bounds,
            /* capabilities= */ capabilities,
            /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                /* state= */ EnvironmentVisibilityState.INVISIBLE
            ),
            /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                /* state= */ PassthroughVisibilityState.DISABLED,
                /* opacity= */ 0.0f,
            ),
            /* isEnvironmentInherited= */ false,
            /* mainWindowSize= */ Size(100, 100),
            /* preferredAspectRatio= */ 1.0f,
            /* sceneParentTransform= */ null,
        )
    }

    @Test
    fun getBounds_returnsBounds() {
        assertThat(activitySpace.bounds.width).isPositiveInfinity()
        assertThat(activitySpace.bounds.height).isPositiveInfinity()
        assertThat(activitySpace.bounds.depth).isPositiveInfinity()

        val spatialState = createSpatialState(/* bounds= */ Bounds(100.0f, 200.0f, 300.0f))
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, spatialState)

        assertThat(activitySpace.bounds.width).isEqualTo(100f)
        assertThat(activitySpace.bounds.height).isEqualTo(200f)
        assertThat(activitySpace.bounds.depth).isEqualTo(300f)
    }

    @Test
    fun addBoundsChangedListener_happyPath() {
        val listener = Mockito.mock(ActivitySpace.OnBoundsChangedListener::class.java)

        val spatialState = createSpatialState(/* bounds= */ Bounds(100.0f, 200.0f, 300.0f))
        activitySpace.addOnBoundsChangedListener(listener)
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, spatialState)

        verify(listener).onBoundsChanged(eq(Dimensions(100.0f, 200.0f, 300.0f)))
    }

    @Test
    fun removeOnBoundsChangedListener_happyPath() {
        val listener = Mockito.mock(ActivitySpace.OnBoundsChangedListener::class.java)

        activitySpace.addOnBoundsChangedListener(listener)
        activitySpace.removeOnBoundsChangedListener(listener)
        val spatialState = createSpatialState(/* bounds= */ Bounds(100.0f, 200.0f, 300.0f))
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, spatialState)

        verify(listener, Mockito.never()).onBoundsChanged(any())
    }

    @Test
    fun getActivitySpacePose_returnsIdentity() {
        val activitySpaceImpl = activitySpace

        assertPose(activitySpaceImpl.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpaceScale_returnsUnitScale() {
        val activitySpaceImpl = activitySpace
        activitySpaceImpl.setPlatformReferenceSpaceTransform(Matrix4.fromScale(5f))
        assertVector3(activitySpaceImpl.activitySpaceScale, Vector3(1f, 1f, 1f))
    }

    @Test
    @Throws(Exception::class)
    fun setScale_throwsException() {
        val scale = Vector3(1f, 1f, 9999f)

        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setScale(scale, Space.PARENT)
        }
    }

    @Test
    @Throws(Exception::class)
    fun hitTest_returnsHitTest() = runBlocking {
        val distance = 2.0f
        val hitPosition = Vec3(1.0f, 2.0f, 3.0f)
        val surfaceNormal = Vec3(4.0f, 5.0f, 6.0f)
        val surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL
        @ScenePose.HitTestFilterValue val hitTestFilter = ScenePose.HitTestFilter.SELF_SCENE

        val hitTestResultBuilder =
            com.android.extensions.xr.space.HitTestResult.Builder(
                distance,
                hitPosition,
                true,
                surfaceType,
            )
        val extensionsHitTestResult = hitTestResultBuilder.setSurfaceNormal(surfaceNormal).build()
        ShadowXrExtensions.extract(xrExtensions).setHitTestResult(activity, extensionsHitTestResult)

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                activitySpace.hitTest(Vector3(1f, 1f, 1f), Vector3(1f, 1f, 1f), hitTestFilter)
            }
        fakeExecutor.runAll()
        val hitTestResult = deferredHitTestResult.await()

        assertThat(hitTestResult.distance).isEqualTo(distance)
        assertVector3(hitTestResult.hitPosition!!, Vector3(1f, 2f, 3f))
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(4f, 5f, 6f).toNormalized())
        assertThat(hitTestResult.surfaceType)
            .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE)
    }

    @Test
    fun hitTest_jobCancelled_throwsCancellationException() = runBlocking {
        val extensionsHitTestResult =
            com.android.extensions.xr.space.HitTestResult.Builder(
                    2.0f,
                    Vec3(1.0f, 2.0f, 3.0f),
                    true,
                    com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL,
                )
                .build()
        ShadowXrExtensions.extract(xrExtensions).setHitTestResult(activity, extensionsHitTestResult)
        @ScenePose.HitTestFilterValue val hitTestFilter = ScenePose.HitTestFilter.SELF_SCENE

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                activitySpace.hitTest(Vector3(1f, 1f, 1f), Vector3(1f, 1f, 1f), hitTestFilter)
            }

        deferredHitTestResult.cancel()
        fakeExecutor.runAll()

        try {
            deferredHitTestResult.await()
            fail("Excepted CancellationException was not thrown")
        } catch (e: CancellationException) {
            // expected, this is not reached
        }
    }

    private class TestSpatialModeChangeListener : SpatialModeChangeListener {
        var lastRecommendedPose: Pose? = null
        var lastRecommendedScale: Vector3? = null
        var updateCount = 0

        override fun onSpatialModeChanged(recommendedPose: Pose, recommendedScale: Vector3) {
            lastRecommendedPose = recommendedPose
            lastRecommendedScale = recommendedScale
            updateCount++
        }
    }

    @Test
    fun handleOriginUpdate_unscaledGravityAlignedFalse_handlerCalled() {
        val handler = TestSpatialModeChangeListener()
        activitySpace.setSpatialModeChangeListener(handler)

        val initialRotation = Quaternion.fromEulerAngles(30f, 0f, 0f)
        val initialScale = Vector3(2.0f, 2.0f, 2.0f)
        val newTransform = Matrix4.fromTrs(Vector3.Zero, initialRotation, initialScale)

        activitySpace.handleOriginUpdate(newTransform)

        assertPose(handler.lastRecommendedPose!!, Pose(Vector3.Zero, initialRotation))
        assertVector3(handler.lastRecommendedScale!!, initialScale)
        assertThat(handler.updateCount).isEqualTo(1)
    }

    @Test
    fun handleOriginUpdate_unscaledGravityAlignedTrue_scaleAndRotationApplied_handlerCalled() {
        val handler = TestSpatialModeChangeListener()
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setSpatialModeChangeListener(handler)

        val initialRotation = Quaternion.fromEulerAngles(45f, 0f, 0f)
        val initialScale = Vector3(2.0f, 2.0f, 2.0f)
        val newTransform = Matrix4.fromTrs(Vector3.One, initialRotation, initialScale)

        activitySpace.handleOriginUpdate(newTransform)

        val activitySpaceScale =
            RuntimeUtils.getVector3(nodeRepository.getScale(activitySpace.getNode()))
        assertVector3(
            activitySpaceScale,
            Vector3(1f / initialScale.x, 1f / initialScale.y, 1f / initialScale.z),
        )
        val activitySpaceRotation =
            RuntimeUtils.getQuaternion(nodeRepository.getOrientation(activitySpace.getNode()))
        val expectedRotation = initialRotation.inverse
        assertThat(activitySpaceRotation.x).isWithin(0.001f).of(expectedRotation.x)
        assertThat(activitySpaceRotation.y).isWithin(0.001f).of(expectedRotation.y)
        assertThat(activitySpaceRotation.z).isWithin(0.001f).of(expectedRotation.z)
        assertThat(activitySpaceRotation.w).isWithin(0.001f).of(expectedRotation.w)

        val expectedPose = Pose(Vector3.Zero, initialRotation)
        assertThat(handler.lastRecommendedPose).isEqualTo(expectedPose)
        assertVector3(handler.lastRecommendedScale!!, initialScale)
        assertThat(handler.updateCount).isEqualTo(1)
    }

    @Test
    fun handleOriginUpdate_unscaledGravityAlignedTrue_preservesYaw() {
        val handler = TestSpatialModeChangeListener()
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setSpatialModeChangeListener(handler)

        // Rotation with Pitch=45, Yaw=90, Roll=30
        val initialRotation = Quaternion.fromEulerAngles(45f, 90f, 30f)
        val initialScale = Vector3(1.0f, 1.0f, 1.0f)
        val newTransform = Matrix4.fromTrs(Vector3.One, initialRotation, initialScale)

        activitySpace.handleOriginUpdate(newTransform)

        val activitySpaceRotation =
            RuntimeUtils.getQuaternion(nodeRepository.getOrientation(activitySpace.getNode()))

        // Expected: inverse of full rotation * yaw rotation
        // This ensures the net global rotation has the original yaw but 0 pitch/roll (gravity
        // aligned).
        val yawRotation = Quaternion.fromEulerAngles(0f, 90f, 0f)
        val expectedRotation = initialRotation.inverse * yawRotation

        assertThat(activitySpaceRotation.x).isWithin(0.001f).of(expectedRotation.x)
        assertThat(activitySpaceRotation.y).isWithin(0.001f).of(expectedRotation.y)
        assertThat(activitySpaceRotation.z).isWithin(0.001f).of(expectedRotation.z)
        assertThat(activitySpaceRotation.w).isWithin(0.001f).of(expectedRotation.w)

        // Ensure the handler receives the rotation with identity yaw (Pitch and Roll only)
        // Original rotation: Pitch=45, Yaw=90, Roll=30
        // Expected rotation: Pitch=45, Yaw=0, Roll=30
        val expectedCallbackRotation = Quaternion.fromEulerAngles(45f, 0f, 30f)
        val expectedPose = Pose(Vector3.Zero, expectedCallbackRotation)

        val actualPose = handler.lastRecommendedPose!!
        assertThat(actualPose.rotation.x).isWithin(0.001f).of(expectedPose.rotation.x)
        assertThat(actualPose.rotation.y).isWithin(0.001f).of(expectedPose.rotation.y)
        assertThat(actualPose.rotation.z).isWithin(0.001f).of(expectedPose.rotation.z)
        assertThat(actualPose.rotation.w).isWithin(0.001f).of(expectedPose.rotation.w)

        assertThat(handler.updateCount).isEqualTo(1)
    }

    @Test
    fun handleOriginUpdate_noHandler_doesNotCallHandler() {
        val handler = TestSpatialModeChangeListener()
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setSpatialModeChangeListener(null)

        val initialRotation = Quaternion.fromEulerAngles(0f, 0f, 90f)
        val initialScale = Vector3(3.0f, 3.0f, 3.0f)
        val newTransform = Matrix4.fromTrs(Vector3.Zero, initialRotation, initialScale)

        activitySpace.handleOriginUpdate(newTransform)

        val activitySpaceScale =
            RuntimeUtils.getVector3(nodeRepository.getScale(activitySpace.getNode()))
        assertVector3(
            activitySpaceScale,
            Vector3(1.0f / initialScale.x, 1.0f / initialScale.y, 1.0f / initialScale.z),
        )
        val activitySpaceRotation =
            RuntimeUtils.getQuaternion(nodeRepository.getOrientation(activitySpace.getNode()))

        // Even without handler, the physics correction applies.
        // Rotation was 90 roll. Yaw=0.
        // Expected: Inverse(90 roll) * Yaw(0) = Inverse(90 roll)
        val expectedRotation = initialRotation.inverse

        assertThat(activitySpaceRotation.x).isWithin(0.001f).of(expectedRotation.x)
        assertThat(activitySpaceRotation.y).isWithin(0.001f).of(expectedRotation.y)
        assertThat(activitySpaceRotation.z).isWithin(0.001f).of(expectedRotation.z)
        assertThat(activitySpaceRotation.w).isWithin(0.001f).of(expectedRotation.w)

        assertThat(handler.updateCount).isEqualTo(0)
    }

    @Test
    fun getRecommendedContentBoxInFullSpace_returnsCorrectlyConvertedBox() {
        val box = Box3(-1.73f / 2, -1.61f / 2, -0.5f / 2, 1.73f / 2, 1.61f / 2, 0.5f / 2)
        ShadowXrExtensions.extract(xrExtensions).setRecommendedContentBoxInFullSpace(box)

        val resultBox = activitySpace.recommendedContentBoxInFullSpace

        assertThat(resultBox).isNotNull()
        val expectedBox =
            BoundingBox.fromMinMax(
                Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
            )
        assertThat(resultBox).isEqualTo(expectedBox)
    }

    @Test
    @Throws(Exception::class)
    fun activitySpaceSetPose_throwsException() {
        val pose = Pose()

        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.setPose(pose, Space.ACTIVITY)
        }
    }

    @Test
    @Throws(Exception::class)
    fun getPoseRelativeToParentSpace_throwsException() {
        assertThrows(UnsupportedOperationException::class.java) {
            activitySpace.getPose(Space.PARENT)
        }
    }

    @Test
    fun getPoseRelativeToActivitySpace_returnsIdentity() {
        val activitySpaceImpl = activitySpace

        assertPose(activitySpaceImpl.getPose(Space.ACTIVITY), activitySpaceImpl.activitySpacePose)
    }

    @Test
    fun getPoseRelativeToRealWorldSpace_returnsPerceptionSpacePose() {
        val activitySpaceImpl = activitySpace

        assertPose(
            activitySpaceImpl.getPose(Space.REAL_WORLD),
            activitySpaceImpl.poseInPerceptionSpace,
        )
    }

    @Test
    @Throws(Exception::class)
    fun getScaleRelativeToParentSpace_throwsException() {
        val activitySpaceImpl = activitySpace

        assertThrows(UnsupportedOperationException::class.java) {
            activitySpaceImpl.getScale(Space.PARENT)
        }
    }

    @Test
    fun getScaleRelativeToActivitySpace_returnsActivitySpaceScale() {
        val activitySpaceImpl = activitySpace

        assertVector3(
            activitySpaceImpl.getScale(Space.ACTIVITY),
            activitySpaceImpl.activitySpaceScale,
        )
    }

    @Test
    fun getScaleRelativeToRealWorldSpace_returnsVector3One() {
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        val newTransform =
            Matrix4.fromTrs(
                Vector3.One,
                Quaternion.fromEulerAngles(45f, 0f, 0f),
                Vector3(2f, 2f, 2f),
            )
        activitySpace.handleOriginUpdate(newTransform)
        assertVector3(activitySpace.worldSpaceScale, Vector3(1f, 1f, 1f))
    }

    @Test
    fun getPoseInPlatformReferenceSpace_returnsUnscaledPose() {
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        val initialRotation = Quaternion.fromEulerAngles(45f, 0f, 0f)
        val newTransform = Matrix4.fromTrs(Vector3.One, initialRotation, Vector3(2f, 2f, 2f))
        activitySpace.setPlatformReferenceSpaceTransform(newTransform)
        val pose = activitySpace.poseInPlatformReferenceSpace
        assertThat(pose).isNotNull()
        assertPose(pose!!, Pose(Vector3.One, initialRotation))
    }

    @Test
    override fun zeroTransform_doesNotUpdatePoseOrScaleOrCallOnOriginChanged() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val listener = Mockito.mock(Runnable::class.java)
        val executor = FakeScheduledExecutorService()
        val expectedPose = Pose(Vector3.One, Quaternion.Identity)
        val expectedScale = Vector3(4f, 5f, 6f)

        systemSpaceEntity.platformReferenceSpaceTransform.set(fromPose(expectedPose))
        systemSpaceEntity._worldSpaceScale = expectedScale
        systemSpaceEntity.setOnOriginChangedListener(listener, executor)
        systemSpaceEntity.setPlatformReferenceSpaceTransform(Matrix4.Zero)
        executor.runAll()

        assertThat(systemSpaceEntity.poseInPlatformReferenceSpace).isEqualTo(expectedPose)
        // ActivitySpace always returns Vector3.One
        assertThat(systemSpaceEntity.worldSpaceScale).isEqualTo(Vector3.One)
        verify(listener, never()).run()
    }

    @Test
    override fun setPlatformReferenceSpaceTransform_updatesScale() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val matrix = Matrix4.fromScale(3.3f)
        systemSpaceEntity.setPlatformReferenceSpaceTransform(matrix)
        assertVector3(systemSpaceEntity.activitySpaceScale, Vector3.One)
        // ActivitySpace always returns Vector3.One
        assertVector3(systemSpaceEntity.worldSpaceScale, Vector3.One)
        assertVector3(systemSpaceEntity.getScale(Space.ACTIVITY), Vector3.One)
    }

    @Test
    fun handleOriginUpdate_sameTransform_doesNotNotifyAgain() {
        val handler = TestSpatialModeChangeListener()
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setSpatialModeChangeListener(handler)

        val newTransform = Matrix4.fromTrs(Vector3.One, Quaternion.Identity, Vector3.One)

        activitySpace.handleOriginUpdate(newTransform)
        assertThat(handler.updateCount).isEqualTo(1)

        activitySpace.handleOriginUpdate(newTransform)
        assertThat(handler.updateCount).isEqualTo(1)
    }

    @Test
    fun handleOriginUpdate_handlesIndependentlyOfPlatformReferenceSpaceTransform() {
        val handler = TestSpatialModeChangeListener()
        testRuntime = createTestSceneRuntime()
        activitySpace = testRuntime.activitySpace as ActivitySpaceImpl
        activitySpace.setSpatialModeChangeListener(handler)

        val newTransform = Matrix4.fromTrs(Vector3.One, Quaternion.Identity, Vector3.One)
        val anotherTransform =
            Matrix4.fromTrs(Vector3.One, Quaternion.Identity, Vector3(2f, 2f, 2f))

        activitySpace.handleOriginUpdate(newTransform)
        assertThat(handler.updateCount).isEqualTo(1)

        // Asynchronously the underlying platform reference space transform might be updated
        // by the node transform listener. This should NOT affect handleOriginUpdate's debouncing.
        activitySpace.setPlatformReferenceSpaceTransform(anotherTransform)

        // Applying the same transform via handleOriginUpdate should be debounced.
        activitySpace.handleOriginUpdate(newTransform)
        assertThat(handler.updateCount).isEqualTo(1)
    }

    @Test
    fun isValidSceneParentTransform_acceptsPositiveUniformScale() {
        val valid = Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, Vector3(2f, 2f, 2f))
        assertThat(isValidSceneParentTransform(valid)).isTrue()
    }

    @Test
    fun isValidSceneParentTransform_rejectsInvalidTransforms() {
        assertThat(isValidSceneParentTransform(Matrix4.Zero)).isFalse()

        val negativeScale =
            Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, Vector3(-2f, -2f, -2f))
        assertThat(isValidSceneParentTransform(negativeScale)).isFalse()

        val subEpsilonScale =
            Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, Vector3(1e-5f, 1e-5f, 1e-5f))
        assertThat(isValidSceneParentTransform(subEpsilonScale)).isFalse()

        val nonUniformScale =
            Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, Vector3(1f, 1.05f, 1f))
        assertThat(isValidSceneParentTransform(nonUniformScale)).isFalse()

        val nanData = FloatArray(16) { Float.NaN }
        val nanMatrix = Matrix4(nanData)
        assertThat(isValidSceneParentTransform(nanMatrix)).isFalse()

        val partialNanData = Matrix4.Identity.data.clone()
        partialNanData[12] = Float.NaN
        val partialNanMatrix = Matrix4(partialNanData)
        assertThat(isValidSceneParentTransform(partialNanMatrix)).isFalse()
    }

    @Test
    fun originCompensation_convertsVectorsDirectionsAndNormals() {
        val compensation = OriginCompensation(2f, Quaternion.fromEulerAngles(0f, 90f, 0f))

        val position = Vector3(2f, 0f, 0f)
        val convertedPos = compensation.toActivitySpace(position)
        assertVector3(convertedPos, Vector3(0f, 0f, 4f))

        val sceneParentPos = compensation.toSceneParentSpace(convertedPos)
        assertVector3(sceneParentPos, position)

        val direction = Vector3(2f, 0f, 0f)
        val convertedDirection = compensation.toActivitySpaceDirection(direction)
        assertVector3(convertedDirection, Vector3(0f, 0f, 1f))

        val sceneParentDirection = compensation.toSceneParentSpaceDirection(convertedDirection)
        assertVector3(sceneParentDirection, Vector3(1f, 0f, 0f))

        val normal = Vector3(1f, 0f, 0f)
        val convertedNormal = compensation.toActivitySpaceNormal(normal)
        assertVector3(convertedNormal, Vector3(0f, 0f, 1f))
    }

    @Test
    fun hitTest_withOriginCompensation_compensatesRayAndResult() = runBlocking {
        val sceneParentScale = Vector3(2f, 2f, 2f)
        val sceneParentRotation = Quaternion.fromEulerAngles(0f, 90f, 0f)
        activitySpace.handleOriginUpdate(
            Matrix4.fromTrs(Vector3.Zero, sceneParentRotation, sceneParentScale)
        )

        val rawDistance = 2.5f
        val rawHitPosition = Vec3(2f, 0f, 0f)
        val rawNormal = Vec3(1f, 0f, 0f)
        val extensionsHitTestResult =
            com.android.extensions.xr.space.HitTestResult.Builder(
                    rawDistance,
                    rawHitPosition,
                    true,
                    com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL,
                )
                .setSurfaceNormal(rawNormal)
                .build()
        ShadowXrExtensions.extract(xrExtensions).setHitTestResult(activity, extensionsHitTestResult)

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                activitySpace.hitTest(
                    Vector3(2f, 4f, 6f),
                    Vector3(0f, 0f, -1f),
                    ScenePose.HitTestFilter.SELF_SCENE,
                )
            }
        fakeExecutor.runAll()
        val hitTestResult = deferredHitTestResult.await()

        assertThat(hitTestResult.distance).isWithin(1e-5f).of(5.0f)
        assertThat(hitTestResult.hitPosition).isNotNull()
        assertVector3(hitTestResult.hitPosition!!, Vector3(4f, 0f, 0f))
        assertThat(hitTestResult.surfaceNormal).isNotNull()
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(1f, 0f, 0f))
    }

    @Test
    fun hitTest_withPitchAndScale_compensatesRayAndResult() = runBlocking {
        // 45 degree pitch + 2x uniform scale: gravityAlignedRotation is -45 degree pitch.
        val sceneParentScale = Vector3(2f, 2f, 2f)
        val sceneParentRotation = Quaternion.fromEulerAngles(45f, 0f, 0f)
        activitySpace.handleOriginUpdate(
            Matrix4.fromTrs(Vector3.Zero, sceneParentRotation, sceneParentScale)
        )

        val rawDistance = 2.0f
        val rawHitPosition = Vec3(0f, 2f, 0f)
        val rawNormal = Vec3(0f, 1f, 0f)
        val extensionsHitTestResult =
            com.android.extensions.xr.space.HitTestResult.Builder(
                    rawDistance,
                    rawHitPosition,
                    true,
                    com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL,
                )
                .setSurfaceNormal(rawNormal)
                .build()
        ShadowXrExtensions.extract(xrExtensions).setHitTestResult(activity, extensionsHitTestResult)

        val deferredResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                activitySpace.hitTest(
                    Vector3(1f, 0f, 0f),
                    Vector3(0f, 0f, 1f),
                    ScenePose.HitTestFilter.SELF_SCENE,
                )
            }
        fakeExecutor.runAll()
        val hitTestResult = deferredResult.await()

        assertThat(hitTestResult.distance).isWithin(1e-5f).of(4.0f)
        assertThat(hitTestResult.hitPosition).isNotNull()
        val expectedY = 2f * kotlin.math.sqrt(2.0).toFloat()
        val expectedZ = 2f * kotlin.math.sqrt(2.0).toFloat()
        assertVector3(hitTestResult.hitPosition!!, Vector3(0f, expectedY, expectedZ))
        assertThat(hitTestResult.surfaceNormal).isNotNull()
        val expectedNormalY = kotlin.math.sqrt(0.5).toFloat()
        val expectedNormalZ = kotlin.math.sqrt(0.5).toFloat()
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(0f, expectedNormalY, expectedNormalZ))
    }

    @Test
    fun gravityAlignedYawRotation_cardinalYawAngles_returnsExactYaw() {
        val testAngles = listOf(0f, 45f, 90f, -90f, 180f)
        for (expectedYaw in testAngles) {
            val input = Quaternion.fromEulerAngles(0f, expectedYaw, 0f)
            val result = gravityAlignedYawRotation(input)

            // Verifying forward facing direction matches the expected yaw rotation precisely
            val expectedForward = input * Vector3(0f, 0f, -1f)
            val actualForward = result * Vector3(0f, 0f, -1f)
            assertVector3(actualForward, expectedForward)
        }
    }

    @Test
    fun gravityAlignedYawRotation_withPitchAndRoll_preservesFacingYaw() {
        val expectedYaw = 45.0f
        // Apply +30 deg pitch and -20 deg roll alongside +45 deg yaw
        val input = Quaternion.fromEulerAngles(30f, expectedYaw, -20f)
        val result = gravityAlignedYawRotation(input)

        // The extracted gravity-aligned yaw must remain purely around Y without tilt distortion
        val resultEuler = result.eulerAngles
        assertThat(resultEuler.x).isWithin(1e-3f).of(0f)
        assertThat(resultEuler.z).isWithin(1e-3f).of(0f)
        assertThat(resultEuler.y).isWithin(0.1f).of(expectedYaw)
    }

    @Test
    fun gravityAlignedYawRotation_atExactVerticalPoles_returnsValidRotation() {
        val yawDeg = 60.0f
        val northPole = Quaternion.fromEulerAngles(90f, yawDeg, 0f)
        val southPole = Quaternion.fromEulerAngles(-90f, yawDeg, 0f)

        val resultNorth = gravityAlignedYawRotation(northPole)
        val resultSouth = gravityAlignedYawRotation(southPole)

        // Verify neither extreme vertical orientation produces NaN values in the quaternion
        assertThat(
                resultNorth.x.isFinite() &&
                    resultNorth.y.isFinite() &&
                    resultNorth.z.isFinite() &&
                    resultNorth.w.isFinite()
            )
            .isTrue()
        assertThat(
                resultSouth.x.isFinite() &&
                    resultSouth.y.isFinite() &&
                    resultSouth.z.isFinite() &&
                    resultSouth.w.isFinite()
            )
            .isTrue()
        // Proves Up-vector fallback accurately extracts exact facing yaw at vertical poles
        assertThat(resultNorth.eulerAngles.y).isWithin(0.1f).of(yawDeg)
        assertThat(resultSouth.eulerAngles.y).isWithin(0.1f).of(yawDeg)
        // Verify that zero tilt (0 pitch and 0 roll) is cleanly preserved in the output
        assertThat(resultNorth.eulerAngles.x).isWithin(1e-3f).of(0f)
        assertThat(resultSouth.eulerAngles.x).isWithin(1e-3f).of(0f)
    }

    @Test
    fun gravityAlignedYawRotation_nearPoleTransition_blendsSmoothlyWithoutDiscontinuity() {
        val yawDeg = 45.0f
        val rollDeg = 20.0f
        var previousForward =
            gravityAlignedYawRotation(Quaternion.fromEulerAngles(80f, yawDeg, rollDeg)) *
                Vector3.Forward

        for (step in 1..20) {
            val pitch = 80f + step * 0.5f // 80.5 to 90.0 degrees
            val currentRotation =
                gravityAlignedYawRotation(Quaternion.fromEulerAngles(pitch, yawDeg, rollDeg))
            val currentForward = currentRotation * Vector3.Forward
            assertThat(currentForward.dot(previousForward)).isGreaterThan(0.99f)
            previousForward = currentForward
        }
    }
}
