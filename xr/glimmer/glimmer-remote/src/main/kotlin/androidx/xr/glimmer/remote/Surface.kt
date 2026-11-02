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

package androidx.xr.glimmer.remote

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteContentDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.shaders.RemoteSweepGradient
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.drawOutline
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.compositeOver
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.util.fastForEachReversed

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A [surface] implements shared
 * visual decoration for Remote Compose Glimmer components:
 * 1) Clipping: a surface clips its children to the shape specified by [shape].
 * 2) Background: a surface draws a background that is highlighted when [focused] is `true`.
 * 3) Interaction states: when [focused] or [pressed] changes, a surface animates between its idle,
 *    focused and pressed visuals.
 * 4) Disabled state: when [enabled] is `false`, a surface draws a disabled overlay on top of its
 *    content.
 *
 * Use constant values (for example `false.rb`) for decorative surfaces that cannot be interacted
 * with by a user:
 *
 * @sample androidx.xr.glimmer.remote.samples.StaticSurfaceSample
 *
 * State that is controlled by the host, such as focus, can be provided as a named remote boolean.
 * The host can update its value at runtime, and the surface animates to the new state:
 *
 * @sample androidx.xr.glimmer.remote.samples.HostStateSurfaceSample
 *
 * State that is controlled by the document itself can be provided as a mutable remote boolean,
 * updated from actions. For example, to show the pressed state while the surface is being touched:
 *
 * @sample androidx.xr.glimmer.remote.samples.PlayerStateSurfaceSample
 *
 * The pressed state can be combined with a host action, so that the visual feedback is shown by the
 * player while the click itself is handled by the host:
 *
 * @sample androidx.xr.glimmer.remote.samples.HostActionSurfaceSample
 * @param enabled controls the enabled state of this surface. When `false`, a disabled overlay
 *   visual will be drawn on top of the surface. Note that this only affects the visual decoration;
 *   it does not intercept input.
 * @param focused whether this surface is currently focused. When `true`, the surface changes its
 *   appearance to the focused state.
 * @param pressed whether this surface is currently pressed. When `true`, the surface changes its
 *   appearance to the pressed state.
 * @param shape the [RemoteShape] used to clip this surface, and also used to draw the background
 *   and border.
 */
@Composable
public fun RemoteModifier.surface(
    enabled: RemoteBoolean = true.rb,
    // TODO: b/560199673 - Replace focused with RC version of InteractionSource once it's available.
    focused: RemoteBoolean = false.rb,
    // TODO: b/560199673 - Replace pressed with RC version of InteractionSource once it's available.
    pressed: RemoteBoolean = false.rb,
    // TODO: b/560206798 - Replace the default value with a value from RemoteGlimmerTheme.
    shape: RemoteShape = RemoteRoundedCornerShape(36.rdp),
): RemoteModifier {
    val focusProgress =
        animateRemoteFloatAsState(
            targetValue = focused.select(1f.rf, 0f.rf),
            // TODO: b/553944853 - Update spec animations.
            animationSpec = remoteTween(durationMillis = 650, easing = RemoteEasing.Decelerate),
        )

    val pressedProgress =
        animateRemoteFloatAsState(
            targetValue = pressed.select(1f.rf, 0f.rf),
            // TODO: b/553944853 - Update spec animations.
            animationSpec = remoteSpring(stiffness = 400f, dampingRatio = 0.85f),
        )

    return clip(shape).drawWithContent {
        val outline = shape.createOutline(size, layoutDirection, remoteDensity)

        // 1. Background.
        drawBackground(outline = outline, focusProgress = focusProgress)

        // 2. Surface borders.
        drawBorders(
            outline = outline,
            focusProgress = focusProgress,
            pressedProgress = pressedProgress,
        )

        // 3. Content.
        drawContent()

        // 4. Disabled overlay.
        disabledOverlay(outline, enabled)
    }
}

/** Draws background color and focused overlay. */
private fun RemoteContentDrawScope.drawBackground(
    outline: RemoteOutline,
    focusProgress: RemoteFloat,
) {
    val focusedOverlay = FocusedOverlayColor.copy(alpha = focusProgress * FocusedOverlayColor.alpha)
    @SuppressLint("RestrictedApiAndroidX")
    val backgroundColor = focusedOverlay.compositeOver(BackgroundColor)
    val paint = RemotePaint {
        style = PaintingStyle.Fill
        color = backgroundColor
    }
    drawOutline(outline, paint)
}

/** Draws the border that highlights the surface edges and reacts to focus and press. */
private fun RemoteContentDrawScope.drawBorders(
    outline: RemoteOutline,
    focusProgress: RemoteFloat,
    pressedProgress: RemoteFloat,
) {
    val paints =
        getBorderPaints(
            focusProgress = focusProgress,
            pressProgress = pressedProgress,
        )
    // Reverse, as layers should be drawn from the bottom up (the lower a border, the wider it is).
    paints.fastForEachReversed { paint ->
        drawOutline(outline, paint)
    }
}

/**
 * Dims the surface when it is disabled. Drawn conditionally, so an enabled surface costs nothing
 * extra at runtime.
 */
private fun RemoteContentDrawScope.disabledOverlay(
    outline: RemoteOutline,
    enabled: RemoteBoolean,
) {
    @SuppressLint("RestrictedApiAndroidX")
    drawConditionally(!enabled) { drawOutline(outline, DisabledOverlayPaint) }
}

/**
 * Remote Compose has no blur, RC Glimmer uses [LayersCount] stacked strokes to mimic this effect.
 * Returns their paints from the top (narrowest) to the bottom (widest) layer.
 */
@SuppressLint("RestrictedApiAndroidX")
private fun RemoteDrawScope.getBorderPaints(
    focusProgress: RemoteFloat,
    pressProgress: RemoteFloat,
): List<RemotePaint> {
    val transformation = transformationMatrix(focusProgress)
    return List(LayersCount) { layer ->
        val colors = colorStops(layer, focusProgress, pressProgress)
        val width = width(layer, focusProgress, pressProgress)

        val brush = RemoteSweepGradient(colors = colors, stops = SweepGradientStops)

        RemotePaint {
            style = PaintingStyle.Stroke
            // The stroke is centered on the outline and its outer half is clipped away.
            strokeWidth = width * 2f
            with(brush) { applyTo(this@RemotePaint, size, transformation) }
        }
    }
}

/**
 * Creates a matrix that rotates the sweep gradient and then stretches it into an ellipse that fits
 * the canvas bounds.
 */
@SuppressLint("RestrictedApiAndroidX")
private fun RemoteDrawScope.transformationMatrix(focusProgress: RemoteFloat): RemoteMatrix3x3 {
    val angle = lerp(from = IdleAngleDegree, to = FocusedAngleDegree, tween = focusProgress)
    // Matrices are applied from right to left, so the steps are listed in reverse order.
    // 4. Move the center back, otherwise the gradient will be centered in the top-left corner.
    return RemoteMatrix3x3.createTranslateXy(center.x, center.y) *
        // 3. Stretch into an ellipse.
        RemoteMatrix3x3.createScaleX(size.width / size.height) *
        // 2. Rotate around the origin.
        RemoteMatrix3x3.createRotate(angle) *
        // 1. Move the center to the origin, otherwise the gradient will be rotated and stretched
        // around the top-left corner instead of its center.
        RemoteMatrix3x3.createTranslateXy(-center.x, -center.y)
}

/** Animates the gradient colors of the border [layer] between idle, focused and pressed states. */
private fun colorStops(
    layer: Int,
    focusProgress: RemoteFloat,
    pressProgress: RemoteFloat,
): List<RemoteColor> {
    val colors =
        List(GradientStopsCount) { stopIndex ->
            interpolateColor(
                idle = IdleColors[layer][stopIndex],
                focused = FocusedColors[layer][stopIndex],
                pressed = PressedColors[layer][stopIndex],
                focusProgress = focusProgress,
                pressProgress = pressProgress,
            )
        }
    // There are 8 gradient stops. The last 4 stops are mirrored.
    return colors + colors.reversed()
}

/** Animates the width of the border [layer] between idle, focused and pressed states. */
@SuppressLint("RestrictedApiAndroidX")
private fun RemoteDrawScope.width(
    layer: Int,
    focusProgress: RemoteFloat,
    pressProgress: RemoteFloat,
): RemoteFloat {
    val idleWidthPx = IdleWidth[layer].toPx()
    val focusedWidthPx = FocusedWidth[layer].toPx()
    val pressedWidthPx = (min(width, height) * PressedWidthFraction[layer])

    val focusedWidthProgress = lerp(idleWidthPx, focusedWidthPx, focusProgress)
    return lerp(focusedWidthProgress, pressedWidthPx, pressProgress)
}

/** Provides smooth animation of color between different states. */
@SuppressLint("RestrictedApiAndroidX")
private fun interpolateColor(
    idle: RemoteColor,
    focused: RemoteColor,
    pressed: RemoteColor,
    focusProgress: RemoteFloat,
    pressProgress: RemoteFloat,
): RemoteColor {
    val red = lerp(lerp(idle.red, focused.red, focusProgress), pressed.red, pressProgress)
    val green = lerp(lerp(idle.green, focused.green, focusProgress), pressed.green, pressProgress)
    val blue = lerp(lerp(idle.blue, focused.blue, focusProgress), pressed.blue, pressProgress)
    val alpha = lerp(lerp(idle.alpha, focused.alpha, focusProgress), pressed.alpha, pressProgress)
    return RemoteColor.rgb(red = red, green = green, blue = blue, alpha = alpha)
}

private val BackgroundColor: RemoteColor = Color(0xFF303030).rc

// TODO: b/577056595 - Replace with tone change.
/** Lightens the background when the surface is focused. */
private val FocusedOverlayColor: RemoteColor = Color(0x29FFFFFF).rc

/** Dims the whole surface, including its content, when it is disabled. */
private val DisabledOverlayColor: RemoteColor = Color(0x8F191919).rc

private val DisabledOverlayPaint = RemotePaint {
    style = PaintingStyle.Fill
    color = DisabledOverlayColor
}

/** Number of stacked border strokes that approximate the blurred border. */
internal const val LayersCount = 6

/** Number of unique gradient colors per layer. The other half of the gradient mirrors them. */
internal const val GradientStopsCount = 4

/** Rotation of the border gradient for a square surface, in degrees clockwise from 3 o'clock. */
private const val IdleAngleDegree = 225f
private const val FocusedAngleDegree = 315f

/** Gradient stop positions, symmetric around the middle to match the mirrored colors. */
internal val SweepGradientStops: List<RemoteFloat> =
    listOf(0.00f.rf, 0.10f.rf, 0.25f.rf, 0.40f.rf, 0.60f.rf, 0.75f.rf, 0.90f.rf, 1.00f.rf)

/** Width of each border layer, from top to bottom, when the surface is idle. */
internal val IdleWidth: Array<RemoteDp> =
    arrayOf(1.5f.rdp, 2.8f.rdp, 4.1f.rdp, 5.4f.rdp, 6.7f.rdp, 8.0f.rdp)

/** Width of each border layer, from top to bottom, when the surface is focused, but not pressed. */
internal val FocusedWidth: Array<RemoteDp> =
    arrayOf(2.0f.rdp, 2.2f.rdp, 2.4f.rdp, 2.6f.rdp, 2.8f.rdp, 3.0f.rdp)

/** Width of each border layer when pressed, as a fraction of the smaller surface side. */
internal val PressedWidthFraction: Array<RemoteFloat> =
    @SuppressLint("RestrictedApiAndroidX") // operator fun div
    arrayOf(3.rf / 48f, 4.rf / 48f, 5.rf / 48f, 6.rf / 48f, 7.rf / 48f, 8.rf / 48f)

/** Gradient colors of the idle border. Layers share them and differ only in alpha. */
internal val IdleColorsWithoutAlpha: Array<RemoteColor> =
    arrayOf(
        Color(0xFFE5E5E5).rc,
        Color(0xFF808080).rc,
        Color(0xFF666666).rc,
        Color(0xFFB2B2B2).rc,
    )

/** Gradient colors of the focused and pressed border. Layers differ only in alpha. */
internal val NonIdleColorsWithoutAlpha: Array<RemoteColor> =
    arrayOf(
        Color(0xFFFFFFFF).rc,
        Color(0xFFD6D6D6).rc,
        Color(0xFFACACAC).rc,
        Color(0xFFC1C1C1).rc,
    )

/** Alpha of each gradient color per layer (rows from top to bottom) in the idle state. */
internal val IdleLayerStopAlphas: Array<Array<Float>> =
    arrayOf(
        arrayOf(0.40f, 0.10f, 0.08f, 0.04f),
        arrayOf(0.16f, 0.06f, 0.04f, 0.03f),
        arrayOf(0.06f, 0.04f, 0.02f, 0.02f),
        arrayOf(0.03f, 0.02f, 0.01f, 0.01f),
        arrayOf(0.01f, 0.01f, 0.00f, 0.01f),
        arrayOf(0.00f, 0.00f, 0.00f, 0.01f),
    )

/**
 * Same as [IdleLayerStopAlphas], for the focused state. Each layer divides the opacity of the
 * previous layer by half.
 */
internal val FocusedLayerStopAlphas: Array<Array<Float>> =
    arrayOf(
        arrayOf(0.50f, 0.40f, 0.30f, 0.35f),
        arrayOf(0.25f, 0.20f, 0.15f, 0.17f),
        arrayOf(0.13f, 0.10f, 0.07f, 0.09f),
        arrayOf(0.06f, 0.05f, 0.04f, 0.04f),
        arrayOf(0.03f, 0.03f, 0.02f, 0.02f),
        arrayOf(0.02f, 0.01f, 0.01f, 0.01f),
    )

/** Same as [IdleLayerStopAlphas], for the pressed state. */
internal val PressedLayerStopAlphas: Array<Array<Float>> =
    arrayOf(
        arrayOf(0.10f, 0.08f, 0.06f, 0.07f),
        arrayOf(0.08f, 0.06f, 0.05f, 0.04f),
        arrayOf(0.06f, 0.05f, 0.04f, 0.02f),
        arrayOf(0.05f, 0.04f, 0.03f, 0.01f),
        arrayOf(0.04f, 0.03f, 0.02f, 0.01f),
        arrayOf(0.03f, 0.02f, 0.02f, 0.01f),
    )

/** Idle border colors, indexed as `[layer][stop]`. */
internal val IdleColors: List<Array<RemoteColor>> =
    mergeColorsAndAlphas(IdleColorsWithoutAlpha, IdleLayerStopAlphas)

/** Focused border colors, indexed as `[layer][stop]`. */
internal val FocusedColors: List<Array<RemoteColor>> =
    mergeColorsAndAlphas(NonIdleColorsWithoutAlpha, FocusedLayerStopAlphas)

/** Pressed border colors, indexed as `[layer][stop]`. */
internal val PressedColors: List<Array<RemoteColor>> =
    mergeColorsAndAlphas(NonIdleColorsWithoutAlpha, PressedLayerStopAlphas)

private fun mergeColorsAndAlphas(
    colors: Array<RemoteColor>,
    alphas: Array<Array<Float>>,
): List<Array<RemoteColor>> {
    return alphas.map { layerAlphas ->
        Array(layerAlphas.size) { index ->
            // Layers have the same color but different opacity.
            colors[index].copy(alpha = layerAlphas[index].rf)
        }
    }
}
