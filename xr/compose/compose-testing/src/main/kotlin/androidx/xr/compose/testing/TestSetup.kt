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

package androidx.xr.compose.testing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivityPose
import androidx.xr.runtime.internal.ActivitySpace
import androidx.xr.runtime.internal.CameraViewActivityPose
import androidx.xr.runtime.internal.CameraViewActivityPose.Fov
import androidx.xr.runtime.internal.Entity
import androidx.xr.runtime.internal.HeadActivityPose
import androidx.xr.runtime.internal.HitTestResult
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PanelEntity
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.SpatialCapabilities.Companion.SPATIAL_CAPABILITY_3D_CONTENT
import androidx.xr.scenecore.SpatialCapabilities.Companion.SPATIAL_CAPABILITY_APP_ENVIRONMENT
import androidx.xr.scenecore.SpatialCapabilities.Companion.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
import androidx.xr.scenecore.SpatialCapabilities.Companion.SPATIAL_CAPABILITY_SPATIAL_AUDIO
import androidx.xr.scenecore.SpatialCapabilities.Companion.SPATIAL_CAPABILITY_UI
import androidx.xr.scenecore.scene
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * A Test environment composable wrapper to support testing spatial components locally.
 *
 * This function simplifies the initializing and configuring Session for testing purposes. It
 * provides control over whether XR features are enabled and whether the application should operate
 * in "full space" mode or "home space" mode.
 *
 * The created fake Session is then provided down the Composable tree using [LocalSession]. If
 * [isXrEnabled] is false, the Session will not be created.
 *
 * @param isXrEnabled Whether the system XR Spatial feature should be enabled. If false, the Session
 *   will not be created.
 * @param runtime The [JxrPlatformAdapter] to use for the Session.
 * @param content The content block containing the compose content to be tested.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun TestSetup(
    isXrEnabled: Boolean = true,
    runtime: JxrPlatformAdapter? = null,
    onSessionCreated: (Session) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val activity = LocalContext.current.getActivity() as SubspaceTestingActivity

    activity.session =
        remember(isXrEnabled, activity, runtime) {
            if (isXrEnabled) {
                val actualRuntime: JxrPlatformAdapter = runtime ?: createFakeRuntime(activity)

                createFakeSession(activity, actualRuntime).apply {
                    // Use HeadTrackingMode.DISABLED by default to bypass FOV logic.
                    configure(Config(headTracking = HeadTrackingMode.DISABLED))
                    onSessionCreated(this)
                }
            } else {
                null
            }
        }

    CompositionLocalProvider(
        LocalSession provides activity.session,
        LocalSpatialConfiguration provides
            (activity.session?.let { TestSessionSpatialConfiguration(it) }
                ?: LocalSpatialConfiguration.current),
        LocalSpatialCapabilities provides
            (activity.session?.let { TestSessionSpatialCapabilities(it) }
                ?: SpatialCapabilities.NoCapabilities),
        content = content,
    )
}

private tailrec fun Context.getActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> error("Unexpected Context type when trying to resolve the context's Activity.")
    }

/**
 * A test implementation of [HeadActivityPose] that allows for setting custom values for the
 * activity space pose and scales.
 *
 * @param activitySpacePose The pose of the head in ActivitySpace.
 * @param worldSpaceScale The scale of the head in WorldSpace.
 * @param activitySpaceScale The scale of the head in ActivitySpace.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TestHeadActivityPose(
    override var activitySpacePose: Pose = Pose.Identity,
    override var worldSpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override var activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f),
) : HeadActivityPose {
    override fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose {
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
 * A test implementation of [CameraViewActivityPose] that allows for setting custom values for the
 * cameraType, field of view, ActivitySpace pose, and scales.
 *
 * @param cameraType The type of camera.
 * @param fov The field of view of the camera.
 * @param activitySpacePose The pose of the camera in ActivitySpace.
 * @param activitySpaceScale The scale of the camera in ActivitySpace.
 * @param worldSpaceScale The scale of the camera in WorldSpace.
 * @param displayResolutionInPixels The pixel dimensions of the camera view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TestCameraViewActivityPose(
    override val cameraType: Int,
    override var fov: CameraViewActivityPose.Fov =
        Fov(angleLeft = -1.57f, angleRight = 1.57f, angleUp = 1.57f, angleDown = -1.57f),
    override var activitySpacePose: Pose = Pose.Identity,
    override var activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override val worldSpaceScale: Vector3 = Vector3(1f, 1f, 1f),
    override val displayResolutionInPixels: PixelDimensions = PixelDimensions(0, 0),
) : CameraViewActivityPose {
    override fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose {
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
 * A test implementation of a SceneCore [ActivitySpace] that allows for setting custom values.
 *
 * This class delegates non-overridden functionality to a base ActivitySpace instance but provides
 * direct control over key properties like [activitySpacePose] and [activitySpaceScale] (via the
 * overridden [getScale] method).
 *
 * @param fakeRuntimeActivitySpaceBase The base [ActivitySpace] to use for the [ActivitySpace]
 *   implementation.
 * @param activitySpacePose The pose of the ActivitySpace. Defaults to [Pose.Identity].
 * @param activitySpaceScale The scale of the ActivitySpace. Defaults to one.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TestActivitySpace(
    private val fakeRuntimeActivitySpaceBase: ActivitySpace,
    override var activitySpacePose: Pose = Pose.Identity,
    override var activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f),
) : ActivitySpace by fakeRuntimeActivitySpaceBase {

    override fun getScale(relativeTo: Int): Vector3 {
        return activitySpaceScale
    }

    private var spaceUpdateListener: Runnable? = null

    @Suppress("ExecutorRegistration")
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
    public fun triggerOnSpaceUpdatedListener() {
        spaceUpdateListener?.run()
    }
}

/**
 * A test implementation of [JxrPlatformAdapter] that allows for setting custom values for the
 * ActivitySpace, HeadActivityPose, and CameraViewActivityPose.
 *
 * [fakeRuntimeBase] is the base [JxrPlatformAdapter] to use for the [JxrPlatformAdapter]
 * implementation.
 *
 * @param activitySpace The [ActivitySpace] to use for the [JxrPlatformAdapter] implementation.
 * @param headActivityPose The [TestHeadActivityPose] to use for the [JxrPlatformAdapter]
 *   implementation.
 * @param leftCameraViewPose The [TestCameraViewActivityPose] to use for the [JxrPlatformAdapter]
 *   implementation for the left camera.
 * @param rightCameraViewPose The [TestCameraViewActivityPose] to use for the [JxrPlatformAdapter]
 *   implementation for the right camera.
 * @param unknownCameraViewPose The [TestCameraViewActivityPose] to use for the [JxrPlatformAdapter]
 *   implementation for the unknown camera.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TestJxrPlatformAdapter
private constructor(
    private val fakeRuntimeBase: JxrPlatformAdapter,
    override var mainPanelEntity: PanelEntity = fakeRuntimeBase.mainPanelEntity,
    override var activitySpace: ActivitySpace = fakeRuntimeBase.activitySpace,
    override var headActivityPose: TestHeadActivityPose? =
        TestHeadActivityPose(activitySpacePose = Pose(translation = Vector3(1f, 0f, 0f))),
    public var leftCameraViewPose: TestCameraViewActivityPose? =
        TestCameraViewActivityPose(
            cameraType = CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE,
            fov =
                CameraViewActivityPose.Fov(
                    angleLeft = -1.57f,
                    angleRight = 1.00f,
                    angleUp = 1.57f,
                    angleDown = -1.57f,
                ),
        ),
    public var rightCameraViewPose: TestCameraViewActivityPose? =
        TestCameraViewActivityPose(
            cameraType = CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE,
            fov =
                CameraViewActivityPose.Fov(
                    angleLeft = -1.00f,
                    angleRight = 1.57f,
                    angleUp = 1.57f,
                    angleDown = -1.57f,
                ),
        ),
    public var unknownCameraViewPose: TestCameraViewActivityPose? =
        TestCameraViewActivityPose(
            cameraType = CameraViewActivityPose.CameraType.CAMERA_TYPE_UNKNOWN
        ),
) : JxrPlatformAdapter by fakeRuntimeBase {
    override val activitySpaceRootImpl: Entity
        get() = activitySpace

    override fun getCameraViewActivityPose(
        @CameraViewActivityPose.CameraType cameraType: Int
    ): CameraViewActivityPose? {
        return when (cameraType) {
            CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE -> leftCameraViewPose
            CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE -> rightCameraViewPose
            else -> unknownCameraViewPose
        }
    }

    /**
     * Creates a [TestJxrPlatformAdapter] with the provided [fakeRuntimeBase].
     *
     * @param fakeRuntimeBase The base [JxrPlatformAdapter] to use for the [JxrPlatformAdapter]
     *   implementation.
     * @return The [TestJxrPlatformAdapter] with the provided [fakeRuntimeBase].
     */
    public companion object {
        public fun create(fakeRuntimeBase: JxrPlatformAdapter): TestJxrPlatformAdapter =
            TestJxrPlatformAdapter(fakeRuntimeBase = fakeRuntimeBase)

        public fun create(activity: Activity): TestJxrPlatformAdapter =
            TestJxrPlatformAdapter(fakeRuntimeBase = createFakeRuntime(activity))
    }
}

private class TestSessionSpatialCapabilities(session: Session) : SpatialCapabilities {
    private var capabilities by
        mutableStateOf(session.scene.spatialCapabilities).apply {
            session.scene.addSpatialCapabilitiesChangedListener { value = it }
        }

    override val isSpatialUiEnabled: Boolean
        get() = capabilities.hasCapability(SPATIAL_CAPABILITY_UI)

    override val isContent3dEnabled: Boolean
        get() = capabilities.hasCapability(SPATIAL_CAPABILITY_3D_CONTENT)

    override val isAppEnvironmentEnabled: Boolean
        get() = capabilities.hasCapability(SPATIAL_CAPABILITY_APP_ENVIRONMENT)

    override val isPassthroughControlEnabled: Boolean
        get() = capabilities.hasCapability(SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)

    override val isSpatialAudioEnabled: Boolean
        get() = capabilities.hasCapability(SPATIAL_CAPABILITY_SPATIAL_AUDIO)
}

/** A [SpatialConfiguration] that is attached to the current [Session]. */
private class TestSessionSpatialConfiguration(private val session: Session) : SpatialConfiguration {
    override val hasXrSpatialFeature: Boolean = true

    override val bounds: DpVolumeSize by
        mutableStateOf(session.scene.activitySpace.bounds.toDpVolumeSize()).apply {
            session.scene.activitySpace.addOnBoundsChangedListener { value = it.toDpVolumeSize() }
        }

    override fun requestHomeSpaceMode() {
        session.scene.requestHomeSpaceMode()
    }

    override fun requestFullSpaceMode() {
        session.scene.requestFullSpaceMode()
    }
}

/**
 * Creates a [DpVolumeSize] from a [FloatSize3d] object in meters.
 *
 * @return a [DpVolumeSize] object representing the same volume size in Dp.
 */
private fun FloatSize3d.toDpVolumeSize(): DpVolumeSize =
    DpVolumeSize(Meter(width).toDp(), Meter(height).toDp(), Meter(depth).toDp())
