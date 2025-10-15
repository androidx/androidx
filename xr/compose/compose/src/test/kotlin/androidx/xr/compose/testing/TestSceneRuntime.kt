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

package androidx.xr.compose.testing

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.AnchorPlacement
import androidx.xr.scenecore.runtime.CameraViewScenePose
import androidx.xr.scenecore.runtime.CameraViewScenePose.Fov
import androidx.xr.scenecore.runtime.HeadScenePose
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeRenderingRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowConfig
import com.google.common.util.concurrent.ListenableFuture
import java.lang.reflect.Method
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

private object SubspaceAndroidComposeTestRuleConstants {
    const val DEFAULT_DP_PER_METER = 1151.856f

    const val USE_REAL_RUNTIME = "androidx.xr.compose.testing.USE_REAL_RUNTIME"
}

/**
 * Create a fake [Session] for testing.
 *
 * A convenience method that creates a fake [Session] for testing. If runtime is not provided, a
 * fake [androidx.xr.scenecore.runtime.SceneRuntime] will be created by default.
 *
 * @param activity The [Activity] to use for the [Session].
 * @param sceneRuntime The [androidx.xr.scenecore.runtime.SceneRuntime] to use for the [Session].
 */
fun createFakeSession(
    activity: Activity,
    sceneRuntime: SceneRuntime = createFakeRuntime(activity),
): Session =
    Session(
        activity,
        runtimes =
            listOf(
                FakePerceptionRuntimeFactory().createRuntime(activity).apply {
                    lifecycleManager.create()
                },
                sceneRuntime,
                FakeRenderingRuntime(sceneRuntime),
            ),
    )

/**
 * Create a fake [androidx.xr.scenecore.runtime.SceneRuntime] for testing.
 *
 * A convenience method that creates a fake [androidx.xr.scenecore.runtime.SceneRuntime] for
 * testing.
 *
 * @param activity The [Activity] to use for the [androidx.xr.scenecore.runtime.SceneRuntime].
 */
fun createFakeRuntime(
    activity: Activity,
    defaultDpPerMeter: Float = SubspaceAndroidComposeTestRuleConstants.DEFAULT_DP_PER_METER,
): SceneRuntime {
    if (shouldUseRealRuntime(activity)) {
        ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
            .setDefaultDpPerMeter(defaultDpPerMeter)

        // Check version to pass lint rule, "BanUncheckedReflection".
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            try {
                // TODO (b/442359966): Use FakeSceneRuntime instead.
                val spatialSceneRuntimeClass =
                    Class.forName("androidx.xr.scenecore.spatial.core.SpatialSceneRuntime")

                val createMethod: Method? =
                    spatialSceneRuntimeClass.getDeclaredMethod(
                        "create",
                        Activity::class.java,
                        ScheduledExecutorService::class.java,
                    )
                createMethod!!.isAccessible = true
                return createMethod.invoke(null, activity, FakeScheduledExecutorService())
                    as SceneRuntime
            } catch (e: Exception) {
                throw e
            }
        } else {
            throw IllegalStateException(
                "This method is not available on this SDK version" + Build.VERSION.SDK_INT
            )
        }
    } else {
        // TODO(b/447211302) Remove once direct dependency on XrExtensions in Compose XR is removed.
        ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
            .setDefaultDpPerMeter(defaultDpPerMeter)

        return FakeSceneRuntimeFactory().create(activity).apply {
            deviceDpPerMeter = defaultDpPerMeter
        }
    }
}

/**
 * Check the AndroidManifest for a <meta-data> indicating that the real SceneRuntimeImpl should be
 * used instead of the FakeSceneRuntime. By default, we will use the fake adapter.
 */
private fun shouldUseRealRuntime(activity: Activity) =
    activity.packageManager
        .getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
        .metaData
        ?.run {
            containsKey(SubspaceAndroidComposeTestRuleConstants.USE_REAL_RUNTIME) &&
                getBoolean(SubspaceAndroidComposeTestRuleConstants.USE_REAL_RUNTIME)
        }
        ?: activity.packageManager
            .getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
            .metaData
            ?.run {
                containsKey(SubspaceAndroidComposeTestRuleConstants.USE_REAL_RUNTIME) &&
                    getBoolean(SubspaceAndroidComposeTestRuleConstants.USE_REAL_RUNTIME)
            }
        ?: false

/**
 * A test implementation of [androidx.xr.scenecore.runtime.SceneRuntime] that allows for setting
 * custom values for the ActivitySpace, HeadScenePose, and CameraViewScenePose.
 *
 * [fakeSceneRuntimeBase] is the base [androidx.xr.scenecore.runtime.SceneRuntime] to use for the
 * [androidx.xr.scenecore.runtime.SceneRuntime] implementation.
 *
 * @param activitySpace The [androidx.xr.scenecore.runtime.ActivitySpace] to use for the
 *   [androidx.xr.scenecore.runtime.SceneRuntime] implementation.
 * @param headActivityPose The [TestHeadScenePose] to use for the
 *   [androidx.xr.scenecore.runtime.SceneRuntime] implementation.
 * @param leftCameraViewPose The [TestCameraViewScenePose] to use for the
 *   [androidx.xr.scenecore.runtime.SceneRuntime] implementation for the left camera.
 * @param rightCameraViewPose The [TestCameraViewScenePose] to use for the
 *   [androidx.xr.scenecore.runtime.SceneRuntime] implementation for the right camera.
 * @param unknownCameraViewPose The [TestCameraViewScenePose] to use for the
 *   [androidx.xr.scenecore.runtime.SceneRuntime] implementation for the unknown camera.
 */
class TestSceneRuntime
private constructor(
    private val fakeSceneRuntimeBase: SceneRuntime,
    private val fakeRenderingEntityFactory: RenderingEntityFactory =
        fakeSceneRuntimeBase as RenderingEntityFactory,
    override var mainPanelEntity: PanelEntity = fakeSceneRuntimeBase.mainPanelEntity,
    override var activitySpace: ActivitySpace = fakeSceneRuntimeBase.activitySpace,
    override var headActivityPose: TestHeadScenePose? =
        TestHeadScenePose(activitySpacePose = Pose(translation = Vector3(1f, 0f, 0f))),
    var leftCameraViewPose: TestCameraViewScenePose? =
        TestCameraViewScenePose(
            cameraType = CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE,
            fov = Fov(angleLeft = -1.57f, angleRight = 1.00f, angleUp = 1.57f, angleDown = -1.57f),
        ),
    var rightCameraViewPose: TestCameraViewScenePose? =
        TestCameraViewScenePose(
            cameraType = CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE,
            fov = Fov(angleLeft = -1.00f, angleRight = 1.57f, angleUp = 1.57f, angleDown = -1.57f),
        ),
    var unknownCameraViewPose: TestCameraViewScenePose? =
        TestCameraViewScenePose(cameraType = CameraViewScenePose.CameraType.CAMERA_TYPE_UNKNOWN),
) : SceneRuntime by fakeSceneRuntimeBase, RenderingEntityFactory by fakeRenderingEntityFactory {
    override fun getCameraViewActivityPose(
        @CameraViewScenePose.CameraType cameraType: Int
    ): CameraViewScenePose? {
        return when (cameraType) {
            CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE -> leftCameraViewPose
            CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE -> rightCameraViewPose
            else -> unknownCameraViewPose
        }
    }

    val scalesInZ = mutableListOf<Boolean>()

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        anchorPlacement: Set<@JvmSuppressWildcards AnchorPlacement>,
        shouldDisposeParentAnchor: Boolean,
    ): MovableComponent {
        val component =
            fakeSceneRuntimeBase.createMovableComponent(
                systemMovable,
                scaleInZ,
                anchorPlacement,
                shouldDisposeParentAnchor,
            )
        scalesInZ.add(scaleInZ)
        return component
    }

    /**
     * Creates a [TestSceneRuntime] with the provided [fakeSceneRuntimeBase].
     *
     * @param fakeSceneRuntimeBase The base [androidx.xr.scenecore.runtime.SceneRuntime] to use for
     *   the [androidx.xr.scenecore.runtime.SceneRuntime] implementation.
     * @return The [TestSceneRuntime] with the provided [fakeSceneRuntimeBase].
     */
    companion object {
        fun create(fakeSceneRuntimeBase: SceneRuntime): TestSceneRuntime =
            TestSceneRuntime(fakeSceneRuntimeBase = fakeSceneRuntimeBase)

        fun create(activity: Activity): TestSceneRuntime =
            TestSceneRuntime(fakeSceneRuntimeBase = createFakeRuntime(activity))
    }
}

/**
 * A test implementation of [androidx.xr.scenecore.runtime.HeadScenePose] that allows for setting
 * custom values for the activity space pose and scales.
 *
 * @param activitySpacePose The pose of the head in ActivitySpace.
 * @param worldSpaceScale The scale of the head in WorldSpace.
 * @param activitySpaceScale The scale of the head in ActivitySpace.
 */
class TestHeadScenePose(
    override var activitySpacePose: Pose = Pose.Identity,
    override var worldSpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override var activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f),
) : HeadScenePose {
    override fun transformPoseTo(pose: Pose, destination: ScenePose): Pose {
        throw NotImplementedError("Intentionally left unimplemented for these test scenarios")
    }

    @Suppress("AsyncSuffixFuture")
    override fun hitTest(
        origin: Vector3,
        direction: Vector3,
        hitTestFilter: Int,
    ): ListenableFuture<HitTestResult> {
        throw NotImplementedError("Intentionally left unimplemented for these test scenarios")
    }
}

/**
 * A test implementation of [androidx.xr.scenecore.runtime.CameraViewScenePose] that allows for
 * setting custom values for the cameraType, field of view, ActivitySpace pose, and scales.
 *
 * @param cameraType The type of camera.
 * @param fov The field of view of the camera.
 * @param activitySpacePose The pose of the camera in ActivitySpace.
 * @param activitySpaceScale The scale of the camera in ActivitySpace.
 * @param worldSpaceScale The scale of the camera in WorldSpace.
 * @param displayResolutionInPixels The pixel dimensions of the camera view.
 */
class TestCameraViewScenePose(
    override val cameraType: Int,
    override var fov: Fov =
        Fov(angleLeft = -1.57f, angleRight = 1.57f, angleUp = 1.57f, angleDown = -1.57f),
    override var activitySpacePose: Pose = Pose.Identity,
    override var activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override val worldSpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override val displayResolutionInPixels: PixelDimensions = PixelDimensions(0, 0),
) : CameraViewScenePose {
    override fun transformPoseTo(pose: Pose, destination: ScenePose): Pose {
        throw NotImplementedError("Intentionally left unimplemented for these test scenarios")
    }

    @Suppress("AsyncSuffixFuture")
    override fun hitTest(
        origin: Vector3,
        direction: Vector3,
        hitTestFilter: Int,
    ): ListenableFuture<HitTestResult> {
        throw NotImplementedError("Intentionally left unimplemented for these test scenarios")
    }
}

/**
 * A test implementation of a SceneCore [androidx.xr.scenecore.runtime.ActivitySpace] that allows
 * for setting custom values.
 *
 * This class delegates non-overridden functionality to a base ActivitySpace instance but provides
 * direct control over key properties like [activitySpacePose] and [activitySpaceScale] (via the
 * overridden [getScale] method).
 *
 * @param fakeRuntimeActivitySpaceBase The base [androidx.xr.scenecore.runtime.ActivitySpace] to use
 *   for the [androidx.xr.scenecore.runtime.ActivitySpace] implementation.
 * @param activitySpacePose The pose of the ActivitySpace. Defaults to [Pose.Identity].
 * @param activitySpaceScale The scale of the ActivitySpace. Defaults to one.
 */
class TestActivitySpace(
    private val fakeRuntimeActivitySpaceBase: ActivitySpace,
    override var activitySpacePose: Pose = Pose.Identity,
    override var activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override val recommendedContentBoxInFullSpace: BoundingBox =
        BoundingBox.fromMinMax(
            min = Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
            max = Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
        ),
) : ActivitySpace by fakeRuntimeActivitySpaceBase {

    override fun getScale(relativeTo: Int): Vector3 {
        return activitySpaceScale
    }

    private var spaceUpdateListener: Runnable? = null

    override fun setOnSpaceUpdatedListener(listener: Runnable?, executor: Executor?) {
        this.spaceUpdateListener = listener
    }

    /**
     * Manually triggers the listener that was captured via [setOnSpaceUpdatedListener].
     *
     * This method is primarily intended for use in unit tests. It simulates the runtime invoking
     * the listener, which is necessary for testing code that uses suspending functions like
     * `ActivitySpace.awaitUpdate()` or otherwise waits on the listener callback.
     *
     * Call this method in your test after simulating the condition that should cause the space
     * update (e.g., changing `activitySpaceScale` from zero to non-zero) to manually resume any
     * coroutine that might be suspended waiting for the `onSpaceUpdated()` callback.
     *
     * The listener's `onSpaceUpdated()` method is invoked directly on the calling thread.
     */
    fun triggerOnSpaceUpdatedListener() {
        spaceUpdateListener?.run()
    }
}
