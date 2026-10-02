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

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.internal.RemotePrimaryGestureIndicator

/** Contains the default values used by [RemoteOneHandedGestureClickIndicator]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteOneHandedGestureDefaults {
    /** The default size of the gesture indicator icon (36dp). */
    public val IndicatorSize: RemoteDp = 36.rdp

    /** The recommended size of the gesture indicator icon inside small content (< 48dp). */
    public val SmallIndicatorSize: RemoteDp = 28.rdp

    /** The tint color used for the gesture animation. */
    public val indicatorTint: RemoteColor
        @Composable @RemoteComposable get() = LocalRemoteContentColor.current

    /**
     * The total duration in milliseconds of the full gesture hint sequence (content fade-out and
     * indicator scale-in, gesture animation, post-animation hold, and indicator scale-out with
     * content fade-in).
     */
    public const val IndicatorDurationMillis: Int = HintTimeline.TotalMillis
}

/**
 * A remote composable container that wraps a button's [content] and coordinates visual feedback for
 * a one-handed gesture hint without causing layout shifts.
 *
 * This component is the Remote Compose counterpart to
 * `androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator`. It drives the
 * full 1.82s gesture hint sequence across [hintProgress] (`0f..1f`):
 * - **0–450 ms**: Base [content] fades out while the indicator scales in (`0f -> 1f`).
 * - **450–1067 ms**: [RemotePrimaryGestureIndicator] plays its 617 ms double-pinch animation.
 * - **1067–1267 ms**: Indicator holds at rest for 200 ms.
 * - **1267–1820 ms**: Indicator scales out (`1f -> 0f`) while base [content] fades back in.
 * - **1820 ms (`1f`)**: Idle state is restored (`contentAlpha = 1f`, `indicatorScale = 0f`).
 *
 * @param modifier The [RemoteModifier] to be applied to the container layout.
 * @param hintProgress The normalized progress of the 1.82s gesture hint sequence in the range
 *   `0f..1f`. Defaults to `0f` (at rest).
 * @param gestureIndicatorSize The size constraints for the gesture indicator icon.
 * @param gestureIndicatorTint The color used to tint the gesture animation.
 * @param content The original component content (e.g. Text or Icon) to be displayed when no
 *   indicator is active.
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public fun RemoteOneHandedGestureClickIndicator(
    modifier: RemoteModifier = RemoteModifier,
    hintProgress: RemoteFloat = 0f.rf,
    gestureIndicatorSize: RemoteDp = RemoteOneHandedGestureDefaults.IndicatorSize,
    gestureIndicatorTint: RemoteColor = RemoteOneHandedGestureDefaults.indicatorTint,
    content: @Composable @RemoteComposable () -> Unit,
) {
    // Hoisted via createReference() so the player evaluates `t` and `indicatorScale` once per
    // frame instead of duplicating their expressions at each use site.
    val t = (clamp(hintProgress, 0f, 1f) * HintTimeline.TotalSeconds).createReference()

    val enterProgress = clamp(t / HintTimeline.EnterDurationSeconds, 0f.rf, 1f.rf)
    val exitProgress =
        clamp(
            (t - HintTimeline.ExitStartSeconds) / HintTimeline.ExitDurationSeconds,
            0f.rf,
            1f.rf,
        )

    val indicatorScale = (enterProgress - exitProgress).createReference()
    val contentAlpha = 1f.rf - indicatorScale
    val gestureProgress =
        clamp(
            (t - HintTimeline.GestureStartSeconds) / HintTimeline.GestureDurationSeconds,
            0f.rf,
            1f.rf,
        )

    RemoteBox(modifier = modifier, contentAlignment = RemoteAlignment.Center) {
        RemoteBox(
            modifier = RemoteModifier.graphicsLayer(alpha = contentAlpha),
            content = content,
        )

        RemotePrimaryGestureIndicator(
            progress = gestureProgress,
            modifier =
                RemoteModifier.size(gestureIndicatorSize)
                    .graphicsLayer(scaleX = indicatorScale, scaleY = indicatorScale),
            tint = gestureIndicatorTint,
        )
    }
}

/** Phase timings for the 1.82s gesture hint sequence. */
private object HintTimeline {
    /** Duration of the initial content fade-out and indicator scale-in (450 ms). */
    const val EnterDurationSeconds = 0.450f

    /** Start time of the double-pinch gesture animation (450 ms). */
    const val GestureStartSeconds = EnterDurationSeconds

    /** Duration of the double-pinch gesture animation (617 ms). */
    const val GestureDurationSeconds = 0.617f

    /** Hold duration after the double-pinch gesture finishes before exiting (200 ms). */
    const val PostGestureHoldSeconds = 0.200f

    /** Start time of the indicator scale-out and content fade-in (1267 ms). */
    const val ExitStartSeconds =
        GestureStartSeconds + GestureDurationSeconds + PostGestureHoldSeconds

    /** Duration of the indicator scale-out and content fade-in (553 ms). */
    const val ExitDurationSeconds = 0.553f

    /** Total sequence duration in milliseconds (1820 ms) and seconds (1.82 s). */
    const val TotalMillis = 1820
    const val TotalSeconds = 1.820f
}
