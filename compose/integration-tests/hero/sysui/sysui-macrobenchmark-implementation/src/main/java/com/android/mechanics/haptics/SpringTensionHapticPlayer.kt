/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.haptics

import android.Manifest
import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
@HapticsExperimentalApi
fun SpringTensionHapticPlayerProvider(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val hapticPlayer =
        remember(density, context) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            SpringTensionHapticPlayer(density, vibratorManager)
        }

    CompositionLocalProvider(LocalHapticPlayer provides hapticPlayer) { content() }
}

@HapticsExperimentalApi
class SpringTensionHapticPlayer(private val density: Density, vibratorManager: VibratorManager) :
    HapticPlayer {

    // TODO(b/443090261): We should use the MSDLPlayer to play haptics here
    private val vibrator = vibratorManager.defaultVibrator
    private val executor: Executor = Executors.newSingleThreadExecutor()

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun playSegmentHaptics(
        segmentHaptics: SegmentHaptics,
        spatialInput: Float,
        spatialVelocity: Float,
    ) {
        // TODO: Maybe this player can extend to handle other forms of haptics
        if (segmentHaptics !is SegmentHaptics.SpringTension) return

        // 1. Convert the inputs in pixels to metric units
        val distance = density.pxToMeters(abs(spatialInput - segmentHaptics.anchorPointPx))
        val velocity =
            density.pxPerSecToMetersPerSec(spatialVelocity.coerceAtMost(MAX_VELOCITY_PX_PER_SEC))

        // 2. Derive a force in Newton from the spring tension model and the metric inputs
        val damperConstant =
            2f *
                segmentHaptics.attachedMassKg *
                segmentHaptics.dampingRatio *
                sqrt(segmentHaptics.stiffness / segmentHaptics.attachedMassKg)
        val force =
            segmentHaptics.stiffness * distance.value +
                damperConstant * velocity.absoluteValue().value

        // 3. Divide the force by MAX_FORCE to map the values in Newtons to the 0..1 range
        // 4. Multiply the proportion by MaX_INPUT_VIBRATION_SCALE to cap the scale
        // 5. Apply a power function to compensate for the logarithmic human perception.
        val vibrationScale =
            (force * MAX_INPUT_VIBRATION_SCALE / MAX_FORCE).pow(VIBRATION_SCALE_EXPONENT)
        val compensatedScale =
            vibrationScale.pow(VIBRATION_PERCEPTION_EXPONENT).coerceAtMost(maximumValue = 1f)

        // Play the texture.
        // TODO(b/443090261): We should play MSDLToken.DRAG_INDICATOR_CONTINUOUS
        val composition = VibrationEffect.startComposition()
        repeat(5) {
            composition.addPrimitive(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                compensatedScale,
            )
        }
        vibrate(composition.compose())
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun playBreakpointHaptics(
        breakpointHaptics: BreakpointHaptics,
        spatialInput: Float,
        spatialVelocity: Float,
    ) {
        if (breakpointHaptics != BreakpointHaptics.GenericThreshold) return
        // TODO: This could be more expressive by using the inputs

        // TODO(b/443090261): We should play MSDLToken.SWIPE_THRESHOLD_INDICATOR
        val effect =
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, 0)
                .compose()
        vibrate(effect)
    }

    // Use 60 ms because, in theory, this is how long the DRAG_INDICATOR_CONTINUOUS token takes
    override fun getPlaybackIntervalNanos(): Long = 60_000L

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate(vibrationEffect: VibrationEffect) =
        executor.execute { vibrator.vibrate(vibrationEffect) }

    companion object {
        private const val MAX_FORCE = 4f // In Newtons
        private const val MAX_INPUT_VIBRATION_SCALE = 0.2f
        private const val VIBRATION_SCALE_EXPONENT = 1.5f
        private const val VIBRATION_PERCEPTION_EXPONENT = 1 / 0.89f
        private const val MAX_VELOCITY_PX_PER_SEC = 2000f
    }
}
