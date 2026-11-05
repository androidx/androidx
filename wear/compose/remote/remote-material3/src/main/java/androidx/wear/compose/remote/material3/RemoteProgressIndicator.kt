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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Returns [progress] animated by [animationSpec] (by default the slow effects spec of
 * [RemoteMaterialTheme.motionScheme]) whenever it changes, like Wear Material3 progress indicators.
 * A constant [progress] can't change, so it is returned as is and adds nothing to the document.
 *
 * @return the animated [RemoteFloat] progress, or [progress] unchanged if constant
 */
@Composable
@RemoteComposable
internal fun animateProgress(
    progress: RemoteFloat,
    animationSpec: RemoteAnimationSpec = RemoteMaterialTheme.motionScheme.slowEffectsSpec(),
): RemoteFloat =
    if (progress.hasConstantValue) {
        progress
    } else {
        animateRemoteFloatAsState(targetValue = progress, animationSpec = animationSpec)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteProgressIndicatorDefaults {
    /** Diameter of the indicator circle for indeterminate progress. */
    internal val IndeterminateCircularIndicatorDiameter: RemoteDp = 24.rdp

    public val IndeterminateStrokeWidth: RemoteDp = 3.rdp

    /** Small screen breakpoint in Dp for circular progress indicator. */
    internal val SmallScreenBreakpoint: RemoteDp = 225.rdp

    public val DefaultStrokeWidth: RemoteDp = 12.rdp

    public fun largeStrokeWidth(screenSize: RemoteDp): RemoteDp =
        screenSize.isLessThan(SmallScreenBreakpoint).select(8.rdp, 12.rdp)

    public fun smallStrokeWidth(screenSize: RemoteDp): RemoteDp =
        screenSize.isLessThan(SmallScreenBreakpoint).select(5.rdp, 8.rdp)

    public val StartAngle: RemoteFloat = 270f.rf

    public val CurvedIndicatorStartAngle: RemoteFloat = 135f.rf

    public val CurvedIndicatorSweepAngle: RemoteFloat = 90f.rf

    public val CurvedIndicatorStrokeWidth: RemoteDp = 8.rdp

    public val CurvedIndicatorPadding: RemoteDp = 2.rdp

    public val CurvedIndicatorGapAngleDegrees: RemoteFloat = 2f.rf

    public val CurvedIndicatorDotFadeOutFraction: RemoteFloat = 0.5f.rf

    @Composable
    @RemoteComposable
    public fun colors(): RemoteProgressIndicatorColors = defaultProgressIndicatorColors()

    public fun calculateRecommendedGapSize(strokeWidth: RemoteDp): RemoteDp =
        max(0.rdp, strokeWidth) / 3f.rf

    @Composable
    @RemoteComposable
    public fun colors(
        indicatorColor: RemoteColor? = null,
        trackColor: RemoteColor? = null,
        overflowTrackColor: RemoteColor? = null,
        disabledIndicatorColor: RemoteColor? = null,
        disabledTrackColor: RemoteColor? = null,
        disabledOverflowTrackColor: RemoteColor? = null,
    ): RemoteProgressIndicatorColors {
        val defaults = defaultProgressIndicatorColors()
        return RemoteProgressIndicatorColors(
            indicatorBrush =
                indicatorColor?.let { RemoteBrush.solidColor(it) } ?: defaults.indicatorBrush,
            trackBrush = trackColor?.let { RemoteBrush.solidColor(it) } ?: defaults.trackBrush,
            overflowTrackBrush =
                overflowTrackColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.overflowTrackBrush,
            disabledIndicatorBrush =
                disabledIndicatorColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.disabledIndicatorBrush,
            disabledTrackBrush =
                disabledTrackColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.disabledTrackBrush,
            disabledOverflowTrackBrush =
                disabledOverflowTrackColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.disabledOverflowTrackBrush,
        )
    }

    @Composable
    @RemoteComposable
    public fun colors(
        indicatorBrush: RemoteBrush? = null,
        trackBrush: RemoteBrush? = null,
        overflowTrackBrush: RemoteBrush? = null,
        disabledIndicatorBrush: RemoteBrush? = null,
        disabledTrackBrush: RemoteBrush? = null,
        disabledOverflowTrackBrush: RemoteBrush? = null,
    ): RemoteProgressIndicatorColors {
        val defaults = defaultProgressIndicatorColors()
        return RemoteProgressIndicatorColors(
            indicatorBrush = indicatorBrush ?: defaults.indicatorBrush,
            trackBrush = trackBrush ?: defaults.trackBrush,
            overflowTrackBrush = overflowTrackBrush ?: defaults.overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush ?: defaults.disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush ?: defaults.disabledTrackBrush,
            disabledOverflowTrackBrush =
                disabledOverflowTrackBrush ?: defaults.disabledOverflowTrackBrush,
        )
    }

    @Composable
    @RemoteComposable
    private fun defaultProgressIndicatorColors(): RemoteProgressIndicatorColors {
        val colorScheme = RemoteMaterialTheme.colorScheme
        return RemoteProgressIndicatorColors(
            indicatorBrush = RemoteBrush.solidColor(colorScheme.primary),
            trackBrush = RemoteBrush.solidColor(colorScheme.surfaceContainer),
            overflowTrackBrush = RemoteBrush.solidColor(colorScheme.primary.copy(alpha = 0.6f.rf)),
            disabledIndicatorBrush =
                RemoteBrush.solidColor(colorScheme.onSurface.toDisabledColor(0.38f.rf)),
            disabledTrackBrush =
                RemoteBrush.solidColor(colorScheme.onSurface.toDisabledColor(0.12f.rf)),
            disabledOverflowTrackBrush =
                RemoteBrush.solidColor(
                    colorScheme.primary.copy(alpha = 0.6f.rf).toDisabledColor(0.12f.rf)
                ),
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteProgressIndicatorColors(
    public val indicatorBrush: RemoteBrush,
    public val trackBrush: RemoteBrush,
    public val overflowTrackBrush: RemoteBrush,
    public val disabledIndicatorBrush: RemoteBrush,
    public val disabledTrackBrush: RemoteBrush,
    public val disabledOverflowTrackBrush: RemoteBrush,
) {
    /**
     * Represents the indicator color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     * @return the [RemoteBrush] used to draw the indicator
     */
    internal fun indicatorBrush(enabled: RemoteBoolean): RemoteBrush =
        resolveRemoteBrush(enabled, indicatorBrush, disabledIndicatorBrush)

    /**
     * Represents the track color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     * @return the [RemoteBrush] used to draw the track
     */
    internal fun trackBrush(enabled: RemoteBoolean): RemoteBrush =
        resolveRemoteBrush(enabled, trackBrush, disabledTrackBrush)

    /**
     * Represents the animated overflow track color.
     *
     * @param enabled whether the component is enabled.
     * @return the [RemoteBrush] used to draw the overflow track
     */
    internal fun overflowTrackBrush(enabled: RemoteBoolean): RemoteBrush =
        resolveRemoteBrush(enabled, overflowTrackBrush, disabledOverflowTrackBrush)

    public fun copy(
        indicatorBrush: RemoteBrush = this.indicatorBrush,
        trackBrush: RemoteBrush = this.trackBrush,
        overflowTrackBrush: RemoteBrush = this.overflowTrackBrush,
        disabledIndicatorBrush: RemoteBrush = this.disabledIndicatorBrush,
        disabledTrackBrush: RemoteBrush = this.disabledTrackBrush,
        disabledOverflowTrackBrush: RemoteBrush = this.disabledOverflowTrackBrush,
    ): RemoteProgressIndicatorColors =
        RemoteProgressIndicatorColors(
            indicatorBrush = indicatorBrush,
            trackBrush = trackBrush,
            overflowTrackBrush = overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush,
            disabledOverflowTrackBrush = disabledOverflowTrackBrush,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteProgressIndicatorColors) return false

        if (indicatorBrush != other.indicatorBrush) return false
        if (trackBrush != other.trackBrush) return false
        if (overflowTrackBrush != other.overflowTrackBrush) return false
        if (disabledIndicatorBrush != other.disabledIndicatorBrush) return false
        if (disabledTrackBrush != other.disabledTrackBrush) return false
        if (disabledOverflowTrackBrush != other.disabledOverflowTrackBrush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indicatorBrush.hashCode()
        result = 31 * result + trackBrush.hashCode()
        result = 31 * result + overflowTrackBrush.hashCode()
        result = 31 * result + disabledIndicatorBrush.hashCode()
        result = 31 * result + disabledTrackBrush.hashCode()
        result = 31 * result + disabledOverflowTrackBrush.hashCode()
        return result
    }

    /** Returns the resolved brush for the given [enabled] state. */
    private fun resolveRemoteBrush(
        enabled: RemoteBoolean,
        enabledBrush: RemoteBrush,
        disabledBrush: RemoteBrush,
    ): RemoteBrush {
        // Always returns the enabled brush if enabled is not constant
        return enabled.constantValueOrNull?.let { isEnabled ->
            if (isEnabled) enabledBrush else disabledBrush
        } ?: enabledBrush
    }
}
