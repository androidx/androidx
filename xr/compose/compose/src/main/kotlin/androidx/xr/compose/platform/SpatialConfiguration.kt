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
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.toDpVolumeSize
import androidx.xr.scenecore.Session

/**
 * The name of the system feature that indicates whether the system supports XR Spatial features.
 */
internal const val XR_IMMERSIVE_FEATURE = "android.software.xr.immersive"

/** CompositionLocal indicating whether the system XR Spatial feature is enabled. */
public val LocalHasXrSpatialFeature: ProvidableCompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        SpatialConfiguration.hasXrSpatialFeature(LocalContext.currentValue)
    }

public val LocalSpatialConfiguration: CompositionLocal<SpatialConfiguration> =
    compositionLocalWithComputedDefaultOf {
        if (LocalHasXrSpatialFeature.currentValue) {
            SpatialConfiguration.getOrCreate(
                checkNotNull(LocalSession.currentValue) { "session must be initialized" },
                LocalHasXrSpatialFeature.currentValue,
            )
        } else {
            ContextOnlySpatialConfiguration(LocalContext.currentValue)
        }
    }

/**
 * Provides information and functionality related to the spatial configuration of the application.
 */
public interface SpatialConfiguration {
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
        get() = DpVolumeSize.Zero

    /**
     * XR Spatial APIs are supported for this system. This is equivalent to
     * PackageManager.hasSystemFeature(FEATURE_XR_SPATIAL, version) where version is the minimum
     * version for features available in the XR Compose library used.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("hasXrSpatialFeature")
    public val hasXrSpatialFeature: Boolean
        get() = false

    /**
     * Request that the system places the application into home space mode. This will execute
     * asynchronously. If it completes successfully then [bounds] will change. This method will
     * throw an [UnsupportedOperationException] if the application is not in an XR environment.
     *
     * In home space, the visible space may be shared with other applications; however, applications
     * in home space will have their spatial capabilities and physical bounds limited.
     */
    public fun requestHomeSpaceMode() {
        throw UnsupportedOperationException(
            "Cannot request mode changes when not in an Android XR environment."
        )
    }

    /**
     * Request that the system places the application into full space mode. This will execute
     * asynchronously. If it completes successfully then [bounds] will change. This method will
     * throw an [UnsupportedOperationException] if the application is not in an XR environment.
     *
     * In full space, this application will be the only application in the visible space, its
     * spatial capabilities will be expanded, and its physical bounds will expand to fill the entire
     * virtual space.
     */
    public fun requestFullSpaceMode() {
        throw UnsupportedOperationException(
            "Cannot request mode changes when not in an Android XR environment."
        )
    }

    public companion object {
        /**
         * XR Spatial APIs are supported for this system. This is equivalent to
         * PackageManager.hasSystemFeature(FEATURE_XR_SPATIAL, version) where version is the minimum
         * version for features available in the XR Compose library used.
         */
        public fun hasXrSpatialFeature(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(XR_IMMERSIVE_FEATURE)
        }

        private val sessionInstances: MutableMap<Session, SpatialConfiguration> = mutableMapOf()

        public fun getOrCreate(
            session: Session,
            hasXrSpatialFeature: Boolean
        ): SpatialConfiguration =
            sessionInstances.getOrPut(session) {
                SessionSpatialConfiguration(session, hasXrSpatialFeature)
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
                context.getActivity().resources.configuration.screenWidthDp.dp,
                context.getActivity().resources.configuration.screenHeightDp.dp,
                0.dp,
            )
}

/** A [SpatialConfiguration] that is attached to the current [Session]. */
private class SessionSpatialConfiguration(
    private val session: Session,
    override val hasXrSpatialFeature: Boolean,
) : SpatialConfiguration {
    private var boundsState by
        mutableStateOf(session.activitySpace.getBounds()).apply {
            session.activitySpace.addBoundsChangedListener { value = it }
        }

    override val bounds: DpVolumeSize
        get() = boundsState.toDpVolumeSize()

    override fun requestHomeSpaceMode() {
        session.spatialEnvironment.requestHomeSpaceMode()
    }

    override fun requestFullSpaceMode() {
        session.spatialEnvironment.requestFullSpaceMode()
    }
}
