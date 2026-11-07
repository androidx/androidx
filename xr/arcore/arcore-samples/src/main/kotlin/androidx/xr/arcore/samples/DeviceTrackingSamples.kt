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

package androidx.xr.arcore.samples

import androidx.annotation.Sampled
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Session
import kotlin.math.asin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * @param session the active [Session] configured with device tracking enabled (not
 *   [androidx.xr.runtime.DeviceTrackingMode.DISABLED])
 * @param lifecycle the [Lifecycle] to scope the coroutine to
 */
@Sampled
fun detectHeadTilt(session: Session, lifecycle: Lifecycle) {
    // Requires Session.config.deviceTracking != DeviceTrackingMode.DISABLED.
    val arDevice = ArDevice.getInstance(session)

    // Downward/upward hysteresis thresholds in [0°, 180°] (0° = straight down, 90° = level,
    // 180° = straight up).
    val tiltDownThresholdDegrees = 60f
    val tiltUpThresholdDegrees = 85f

    yourCoroutineScope.launch {
        var isTiltedDown = false

        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            arDevice.state.collect { deviceState ->
                if (deviceState.trackingState != TrackingState.TRACKING) return@collect

                // Project the device's forward (-Z) vector onto the world up (+Y) axis to obtain
                // sin(pitch) independent of horizontal yaw, then map [-90°, 90°] to [0°, 180°].
                val sinPitch = deviceState.devicePose.forward.y.coerceIn(-1f, 1f)
                val tiltAngleDegrees = Math.toDegrees(asin(sinPitch).toDouble()).toFloat() + 90f

                if (isTiltedDown) {
                    if (tiltAngleDegrees >= tiltUpThresholdDegrees) {
                        isTiltedDown = false
                        // Update your application UI or state when the confirmed tilt state flips.
                        onHeadTiltChanged(isTiltedDown = false)
                    }
                } else if (tiltAngleDegrees <= tiltDownThresholdDegrees) {
                    isTiltedDown = true
                    // Update your application UI or state when the confirmed tilt state flips.
                    onHeadTiltChanged(isTiltedDown = true)
                }
            }
        }
    }
}

private fun onHeadTiltChanged(isTiltedDown: Boolean) {}

private val yourCoroutineScope = MainScope()
