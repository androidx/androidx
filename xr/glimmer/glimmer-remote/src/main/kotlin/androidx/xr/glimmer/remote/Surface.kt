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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.drawOutline
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteTweenSpec
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.compositeOver
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle

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
            // TODO: b/572840539, b/553944853 - Update spec animations.
            animationSpec = RemoteTweenSpec(durationMillis = 800, easing = RemoteEasing.Standard),
        )

    return clip(shape).drawWithContent {
        val outline = shape.createOutline(size, layoutDirection, remoteDensity)

        // 1. Background.
        drawBackground(outline = outline, focusProgress = focusProgress)

        // 2. Surface border.
        // TODO: b/572836490 - Draw the border.

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

/** Draws disabled overlay if necessary. */
private fun RemoteContentDrawScope.disabledOverlay(
    outline: RemoteOutline,
    enabled: RemoteBoolean,
) {
    @SuppressLint("RestrictedApiAndroidX")
    drawConditionally(!enabled) { drawOutline(outline, DisabledOverlayPaint) }
}

private val BackgroundColor: RemoteColor = Color(0xFF303030).rc

// TODO: b/577056595 - Replace with tone change.
private val FocusedOverlayColor: RemoteColor = Color(0x29FFFFFF).rc

private val DisabledOverlayColor: RemoteColor = Color(0x8F191919).rc

private val DisabledOverlayPaint = RemotePaint {
    style = PaintingStyle.Fill
    color = DisabledOverlayColor
}
