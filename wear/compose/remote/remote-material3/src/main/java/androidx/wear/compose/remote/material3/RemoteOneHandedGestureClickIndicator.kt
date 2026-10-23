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
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.wear.compose.remote.material3.internal.RemotePrimaryGestureIndicator
import java.util.Collections
import java.util.WeakHashMap

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

    /**
     * The name of the named float, in the [RemoteState.Domain.User] domain, that counts the primary
     * gesture hints played in a document. The host plays a hint by reading this variable and
     * setting it to its value + 1.
     */
    public const val PrimaryHintCountName: String = "OneHandedGestureHintCount"
}

/**
 * State of a [RemoteOneHandedGestureClickIndicator] whose hint is played by the host.
 *
 * The state records the named float [RemoteOneHandedGestureDefaults.PrimaryHintCountName] into the
 * document. Each time the host adds 1 to it, the document plays one full gesture hint sequence,
 * without any further updates from the host.
 *
 * Create it with [rememberRemoteOneHandedGestureClickIndicatorState]. A document can contain only
 * one such state, because every state reads the same named float.
 *
 * @property hintProgress The normalized progress of the gesture hint sequence, `0f` when no hint is
 *   playing and `0f..1f` while one is.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public class RemoteOneHandedGestureClickIndicatorState
internal constructor(public val hintProgress: RemoteFloat)

/**
 * Creates and remembers a [RemoteOneHandedGestureClickIndicatorState] for the primary (double
 * pinch) gesture.
 *
 * The hint count is animated with a linear tween of
 * [RemoteOneHandedGestureDefaults.IndicatorDurationMillis]. While the tween runs from the previous
 * count to the new one, [RemoteOneHandedGestureClickIndicatorState.hintProgress] goes from `0f` to
 * `1f`. At rest, the animated count equals the count and the progress is `0f`.
 *
 * Only one state is supported per document.
 *
 * @throws IllegalStateException if the document already contains a primary gesture hint state.
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public fun rememberRemoteOneHandedGestureClickIndicatorState():
    RemoteOneHandedGestureClickIndicatorState {
    val creationState = LocalRemoteComposeCreationState.current
    remember(creationState) { PrimaryHintRegistration(creationState) }
    return remember {
        val count =
            RemoteFloat.createNamedRemoteFloat(
                RemoteOneHandedGestureDefaults.PrimaryHintCountName,
                0f,
            )
        val animatedCount =
            animateRemoteFloatAsState(
                targetValue = count,
                animationSpec =
                    remoteTween(
                        durationMillis = RemoteOneHandedGestureDefaults.IndicatorDurationMillis,
                        easing = RemoteEasing.Linear,
                    ),
            )
        RemoteOneHandedGestureClickIndicatorState(
            hintProgress =
                animatedCount.isLessThan(count).select(animatedCount - count + 1f.rf, 0f.rf)
        )
    }
}

/**
 * A remote composable container that wraps a button's [content] and plays a one-handed gesture hint
 * each time the host adds 1 to the hint count of [state].
 *
 * @param state The state that drives the hint, see
 *   [rememberRemoteOneHandedGestureClickIndicatorState].
 * @param modifier The [RemoteModifier] to be applied to the container layout.
 * @param gestureIndicatorSize The size constraints for the gesture indicator icon.
 * @param gestureIndicatorTint The color used to tint the gesture animation.
 * @param content The original component content (e.g. Text or Icon) to be displayed when no
 *   indicator is active.
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteOneHandedGestureClickIndicator(
    state: RemoteOneHandedGestureClickIndicatorState,
    modifier: RemoteModifier = RemoteModifier,
    gestureIndicatorSize: RemoteDp = RemoteOneHandedGestureDefaults.IndicatorSize,
    gestureIndicatorTint: RemoteColor = RemoteOneHandedGestureDefaults.indicatorTint,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteOneHandedGestureClickIndicator(
        modifier = modifier,
        hintProgress = state.hintProgress,
        gestureIndicatorSize = gestureIndicatorSize,
        gestureIndicatorTint = gestureIndicatorTint,
        content = content,
    )
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

/**
 * Registers a primary gesture hint state in the document of [creationState] for as long as it is
 * remembered, so that a second one fails when the document is built instead of playing in sync with
 * the first one.
 *
 * @param creationState The creation state of the document being built.
 */
@Suppress("RestrictedApiAndroidX")
private class PrimaryHintRegistration(private val creationState: RemoteComposeCreationState) :
    RememberObserver {
    init {
        synchronized(documentsWithPrimaryHint) {
            check(documentsWithPrimaryHint.add(creationState)) {
                "Only one Primary gesture hint per document"
            }
        }
    }

    override fun onRemembered() {}

    override fun onForgotten() = unregister()

    override fun onAbandoned() = unregister()

    private fun unregister() {
        synchronized(documentsWithPrimaryHint) { documentsWithPrimaryHint.remove(creationState) }
    }

    private companion object {
        // One entry per document being built, because several documents can be built at the same
        // time (on different threads). Weak keys, so a document that is never disposed can still
        // be garbage collected.
        val documentsWithPrimaryHint: MutableSet<RemoteComposeCreationState> =
            Collections.newSetFromMap(WeakHashMap())
    }
}

/** Phase timings for the 1.82s gesture hint sequence. */
private object HintTimeline {
    private const val MillisPerSecond = 1000f

    /** Duration of the initial content fade-out and indicator scale-in (450 ms). */
    const val EnterDurationMillis = RemoteMotionTokens.DurationLong1

    /** Start time of the double-pinch gesture animation (450 ms). */
    const val GestureStartMillis = EnterDurationMillis

    /** Duration of the double-pinch gesture animation (617 ms). */
    const val GestureDurationMillis = 617

    /** Hold duration after the double-pinch gesture finishes before exiting (200 ms). */
    const val PostGestureHoldMillis = RemoteMotionTokens.DurationShort4

    /** Start time of the indicator scale-out and content fade-in (1267 ms). */
    const val ExitStartMillis = GestureStartMillis + GestureDurationMillis + PostGestureHoldMillis

    /** Duration of the indicator scale-out and content fade-in (553 ms). */
    const val ExitDurationMillis = 553

    /** Total sequence duration (1820 ms). */
    const val TotalMillis = ExitStartMillis + ExitDurationMillis

    // The same timings in seconds, derived from the integer milliseconds above so that they don't
    // accumulate float rounding errors.
    const val EnterDurationSeconds = EnterDurationMillis / MillisPerSecond
    const val GestureStartSeconds = GestureStartMillis / MillisPerSecond
    const val GestureDurationSeconds = GestureDurationMillis / MillisPerSecond
    const val ExitStartSeconds = ExitStartMillis / MillisPerSecond
    const val ExitDurationSeconds = ExitDurationMillis / MillisPerSecond
    const val TotalSeconds = TotalMillis / MillisPerSecond
}
