/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.xr.arcore.runtime.QrCode as RuntimeQrCode
import androidx.xr.runtime.Config
import androidx.xr.runtime.QrCodeTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/** Describes the current best knowledge of a real-world QR code */
@SuppressWarnings("HiddenSuperclass")
public class QrCode internal constructor(internal val runtimeQrCode: RuntimeQrCode) :
    Trackable<QrCode.State>, Updatable() {

    public companion object {
        /**
         * Emits the QR codes that are currently being tracked in the [session].
         *
         * Only instances of [QrCode] that are [androidx.xr.arcore.TrackingState.TRACKING] will be
         * emitted in the [Collection]. Instances of the same [QrCode] will remain between
         * subsequent emits to the [StateFlow] as long as they remain tracking.
         *
         * @param session the [Session] to subscribe to
         * @return a [StateFlow] that emits a collection of QrCodes
         * @throws [IllegalStateException] if [Config.qrCodeTracking] is set to
         *   [QrCodeTrackingMode.DISABLED]
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<QrCode>> {
            check(session.perceptionRuntime.config.qrCodeTracking != QrCodeTrackingMode.DISABLED) {
                "Config.QrCodeTrackingMode is set to DISABLED."
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(
                            perceptionState.trackableStates.filterIsInstance<QrCode.State>().map {
                                it.owner
                            }
                        )
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.Eagerly,
                    session.state.value.perceptionState
                        ?.trackableStates
                        ?.filterIsInstance<QrCode.State>()
                        ?.map { it.owner } ?: emptyList(),
                )
        }
    }

    /**
     * The representation of the current state of a [QrCode]. The [centerPose] is the [Pose] of the
     * center of the detected QR code's bounding box in the world coordinate space. A [QrCode] is
     * represented as a finite 2D bounding box around a [centerPose].
     *
     * @property trackingState Whether this QR code is being tracked or not
     * @property centerPose The pose of the center of the QR code. The +Y axis relative to the
     *   [centerPose] is equivalent to the normal of the [QrCode]
     * @property extents The dimensions of the bounding box of the detected QR code
     * @property data The content of the detected QR code
     * @property owner self-reference to the object that owns this state
     */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        public val centerPose: Pose,
        public val extents: FloatSize2d,
        public val data: String,
        public val owner: QrCode,
    ) : Trackable.State {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                centerPose == other.centerPose &&
                extents == other.extents &&
                data == other.data &&
                owner == other.owner
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + centerPose.hashCode()
            result = 31 * result + extents.hashCode()
            result = 31 * result + data.hashCode()
            result = 31 * result + owner.hashCode()
            return result
        }
    }

    private val _state =
        MutableStateFlow(
            State(
                runtimeQrCode.trackingState.toTrackingState(),
                runtimeQrCode.centerPose,
                runtimeQrCode.extents,
                runtimeQrCode.data,
                owner = this,
            )
        )
    /** The current state of the [QrCode] */
    public override val state: StateFlow<State> = _state.asStateFlow()

    override suspend fun update() {
        _state.emit(
            State(
                trackingState = runtimeQrCode.trackingState.toTrackingState(),
                centerPose = runtimeQrCode.centerPose,
                extents = runtimeQrCode.extents,
                data = runtimeQrCode.data,
                owner = this,
            )
        )
    }
}
