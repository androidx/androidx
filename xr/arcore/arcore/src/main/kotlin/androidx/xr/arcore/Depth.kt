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

package androidx.xr.arcore

import androidx.xr.arcore.runtime.Depth as RuntimeDepth
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.Session
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contains the depth information corresponding to a specific [RenderViewpoint].
 *
 * The availability of depth data depends on the [DepthEstimationMode] set in [Session.config],
 * which also allows it to be turned off by setting it to [DepthEstimationMode.DISABLED].
 *
 * Update frequency and the dimensions of the maps provided are system-defined. The buffer
 * lifecycles are controlled by the runtime so if the data will not be used upon receiving, a copy
 * should be made.
 *
 * @property state the current [State] of the depth data.
 */
@SuppressWarnings("HiddenSuperclass")
public class Depth internal constructor(internal val runtimeDepth: RuntimeDepth) : Updatable() {
    public companion object {
        /**
         * Returns the Depth data associated with the left display depending on the
         * [DepthEstimationMode] set in [Session.config].
         *
         * @param session the currently active [Session]
         * @note Supported only on devices that use stereo displays for rendering.
         */
        @JvmStatic
        public fun left(session: Session): Depth? {
            val perceptionStateExtender = Depth.Companion.getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.leftDepth
        }

        /**
         * Returns the Depth data associated with the right display depending on the
         * [DepthEstimationMode] set in [Session.config].
         *
         * @param session the currently active [Session]
         * @note Supported only on devices that use stereo displays for rendering.
         */
        @JvmStatic
        public fun right(session: Session): Depth? {
            val perceptionStateExtender = Depth.Companion.getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.rightDepth
        }

        /**
         * Returns the Depth associated with the single device display depending on the
         * [DepthEstimationMode] set in [Session.config].
         *
         * @param session the currently active [Session]
         * @note Supported only on devices that use a monocular display for rendering.
         */
        @JvmStatic
        public fun mono(session: Session): Depth? {
            val perceptionStateExtender = Depth.Companion.getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.monoDepth
        }

        // TODO(b/421240554): Combine getPerceptionStateExtender in different classes.
        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * Contains the current state of depth tracking.
     *
     * @property width the width of the depth maps
     * @property height the height of the depth maps
     * @property rawDepthMap a buffer of size [width] x [height] representing raw depth in meters
     *   from the image plane, with both row and pixel stride equal to 0
     * @property rawConfidenceMap a buffer of confidence values for each pixel in [rawDepthMap],
     *   with 0 representing the lowest confidence and 255 representing the highest confidence
     * @property smoothDepthMap a buffer of size [width] x [height] representing smooth depth in
     *   meters from the image plane, with both row and pixel stride equal to 0
     * @property smoothConfidenceMap a buffer of confidence values for each pixel in
     *   [smoothDepthMap], with 0 representing the lowest confidence and 255 representing the
     *   highest confidence
     * @property owner self-reference to the object that owns this state.
     */
    public class State
    internal constructor(
        public val width: Int,
        public val height: Int,
        public val rawDepthMap: FloatBuffer?,
        public val rawConfidenceMap: ByteBuffer?,
        public val smoothDepthMap: FloatBuffer?,
        public val smoothConfidenceMap: ByteBuffer?,
        public val owner: Depth,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is androidx.xr.arcore.Depth.State) return false
            return width == other.width &&
                height == other.height &&
                rawDepthMap == other.rawDepthMap &&
                rawConfidenceMap == other.rawConfidenceMap &&
                smoothDepthMap == other.smoothDepthMap &&
                smoothConfidenceMap == other.smoothConfidenceMap &&
                owner == other.owner
        }

        override fun hashCode(): Int {
            var result = width.hashCode()
            result = 31 * result + height.hashCode()
            result = 31 * result + (rawDepthMap?.hashCode() ?: 0)
            result = 31 * result + (rawConfidenceMap?.hashCode() ?: 0)
            result = 31 * result + (smoothDepthMap?.hashCode() ?: 0)
            result = 31 * result + (smoothConfidenceMap?.hashCode() ?: 0)
            result = 31 * result + owner.hashCode()
            return result
        }
    }

    private val _state =
        MutableStateFlow<State>(
            State(
                width = 0,
                height = 0,
                rawDepthMap = null,
                rawConfidenceMap = null,
                smoothDepthMap = null,
                smoothConfidenceMap = null,
                owner = this,
            )
        )

    public val state: StateFlow<Depth.State> = _state.asStateFlow()

    override suspend fun update() {
        _state.emit(
            State(
                width = runtimeDepth.width,
                height = runtimeDepth.height,
                rawDepthMap = runtimeDepth.rawDepthMap,
                rawConfidenceMap = runtimeDepth.rawConfidenceMap,
                smoothDepthMap = runtimeDepth.smoothDepthMap,
                smoothConfidenceMap = runtimeDepth.smoothConfidenceMap,
                owner = this,
            )
        )
    }
}
