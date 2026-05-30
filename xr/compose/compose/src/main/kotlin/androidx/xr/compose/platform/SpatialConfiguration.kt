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

package androidx.xr.compose.platform

import android.content.Context
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.toDpVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene

/**
 * Provides the current [SpatialConfiguration].
 *
 * The behavior of the configuration object will depend on whether the system XR Spatial feature is
 * enabled.
 */
public val LocalSpatialConfiguration: ProvidableCompositionLocal<SpatialConfiguration> =
    compositionLocalWithComputedDefaultOf {
        LocalComposeXrOwners.currentValue?.spatialConfiguration
            ?: ContextOnlySpatialConfiguration(LocalContext.currentValue)
    }

/**
 * Provides information and functionality related to the spatial configuration of the application.
 */
public sealed interface SpatialConfiguration {
    /**
     * A volume whose width, height, and depth represent the space available to the application.
     *
     * In XR, an application's available space is not related to the display or window dimensions;
     * instead, it will always be some subset of the virtual 3D space. The app bounds will change
     * when switching between home space or full space modes.
     *
     * In non-XR environments, the width and height will represent the screen width and height
     * available to the application (see [android.content.res.Configuration.screenWidthDp] and
     * [android.content.res.Configuration.screenHeightDp]) and the depth will be zero.
     *
     * This is a state-based value that will trigger recomposition.
     */
    public val bounds: DpVolumeSize

    /**
     * XR Spatial APIs are supported for this system. This is equivalent to
     * PackageManager.hasSystemFeature(FEATURE_XR_SPATIAL, version) where version is the minimum
     * version for features available in the XR Compose library used.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("hasXrSpatialFeature")
    public val hasXrSpatialFeature: Boolean

    public companion object {
        /**
         * XR Spatial APIs are supported for this system. This is equivalent to
         * PackageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL). When this feature is available,
         * it is safe to assume we are in an XR environment.
         *
         * @param context The application or activity context used to check for the system feature.
         */
        public fun hasXrSpatialFeature(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)
        }
    }
}

/** A [SpatialConfiguration] that only has access to the current activity context. */
private class ContextOnlySpatialConfiguration(private val context: Context) : SpatialConfiguration {
    override val hasXrSpatialFeature: Boolean
        get() = SpatialConfiguration.hasXrSpatialFeature(context)

    override val bounds: DpVolumeSize
        get() =
            DpVolumeSize(
                context.requireActivity().resources.configuration.screenWidthDp.dp,
                context.requireActivity().resources.configuration.screenHeightDp.dp,
                0.dp,
            )
}

/** A [SpatialConfiguration] that is attached to the current [Session]. */
internal class SessionSpatialConfiguration(
    private val session: Session,
    private val subspaceRootNode: Entity,
) : SpatialConfiguration {
    private var boundsState by
        mutableStateOf(session.scene.activitySpace.bounds).apply {
            session.scene.activitySpace.addBoundsChangedListener { value = it }
        }

    private val recommendedPoseState =
        mutableStateOf(
            try {
                session.scene.keyEntity?.getPose(relativeTo = Space.ACTIVITY) ?: Pose.Identity
            } catch (_: RuntimeException) {
                Pose.Identity
            }
        )

    private val recommendedScaleState =
        mutableFloatStateOf(
            try {
                session.scene.keyEntity?.getScale(relativeTo = Space.ACTIVITY) ?: 1f
            } catch (_: RuntimeException) {
                1f
            }
        )

    init {
        session.scene.setSpaceChangedListener { event ->
            recommendedPoseState.value = event.recommendedPose
            recommendedScaleState.floatValue = event.recommendedScale
            subspaceRootNode.setPose(pose = recommendedPoseState.value, relativeTo = Space.ACTIVITY)
            subspaceRootNode.setScale(
                scale = recommendedScaleState.floatValue,
                relativeTo = Space.ACTIVITY,
            )
        }
    }

    override val hasXrSpatialFeature: Boolean = true

    override val bounds: DpVolumeSize
        get() = boundsState.toDpVolumeSize()

    /**
     * The recommended pose for the application, provided by the system.
     *
     * This value may change when the system adjusts, such as during a Spatial Mode transition
     * between Home Space Mode and Full Space Mode or recentering or launching a new activity.
     * Reading this property inside a Composable will trigger recomposition when the pose changes.
     */
    internal val recommendedPose: Pose
        get() = recommendedPoseState.value

    /**
     * The recommended scale for the application, provided by the system.
     *
     * This value may change when the system adjusts, such as during a Spatial Mode transition
     * between Home Space Mode and Full Space Mode or recentering or launching a new activity.
     * Reading this property inside a Composable will trigger recomposition when the scale changes.
     */
    internal val recommendedScale: Float
        get() = recommendedScaleState.floatValue
}
