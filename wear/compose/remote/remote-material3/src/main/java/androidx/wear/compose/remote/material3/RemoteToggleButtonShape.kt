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

import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Resolves the resting and pressed [RemoteShape]s of a toggle button for the given [checked] state,
 * to be passed as `shape` and `pressedShape` to [RemoteRoundButton].
 *
 * When the pressed shapes are equal to the resting shapes, the same instance is returned for both,
 * so that [RemoteRoundButton] doesn't add a press animation.
 */
internal fun resolveToggleButtonShapes(
    checked: RemoteBoolean,
    uncheckedShape: RemoteShape,
    checkedShape: RemoteShape,
    uncheckedPressedShape: RemoteShape,
    checkedPressedShape: RemoteShape,
): Pair<RemoteShape, RemoteShape> {
    val shape = resolveToggleButtonShape(checked, uncheckedShape, checkedShape)
    // When checked isn't constant, each resolved shape is a new instance, so compare the input
    // shapes instead, and reuse the resting shape to skip the press animation.
    val pressedShape =
        if (uncheckedPressedShape == uncheckedShape && checkedPressedShape == checkedShape) {
            shape
        } else {
            resolveToggleButtonShape(checked, uncheckedPressedShape, checkedPressedShape)
        }
    return shape to pressedShape
}

/**
 * Resolves the [RemoteShape] of a toggle button for the given [checked] state.
 *
 * If [checked] is a constant, the matching shape is returned directly. Otherwise, if both shapes
 * are [RemoteRoundedCornerShape]s, a [RemoteRoundedCornerShape] whose corner sizes are selected at
 * playback time from [checked] is returned, so that the shape follows state changes on the player.
 * For any other combination, [uncheckedShape] is used.
 */
// TODO(b/571080867): Move shape selection and interpolation helpers to
//  androidx.compose.remote.foundation.
internal fun resolveToggleButtonShape(
    checked: RemoteBoolean,
    uncheckedShape: RemoteShape,
    checkedShape: RemoteShape,
): RemoteShape {
    if (uncheckedShape === checkedShape) return uncheckedShape
    checked.constantValueOrNull?.let { isChecked ->
        return if (isChecked) checkedShape else uncheckedShape
    }
    if (uncheckedShape is RemoteRoundedCornerShape && checkedShape is RemoteRoundedCornerShape) {
        return RemoteRoundedCornerShape(
            topStart = CheckedCornerSize(checked, checkedShape.topStart, uncheckedShape.topStart),
            topEnd = CheckedCornerSize(checked, checkedShape.topEnd, uncheckedShape.topEnd),
            bottomEnd =
                CheckedCornerSize(checked, checkedShape.bottomEnd, uncheckedShape.bottomEnd),
            bottomStart =
                CheckedCornerSize(checked, checkedShape.bottomStart, uncheckedShape.bottomStart),
        )
    }
    return uncheckedShape
}

/** A [RemoteCornerSize] that picks between two corner sizes based on a [RemoteBoolean]. */
private data class CheckedCornerSize(
    val checked: RemoteBoolean,
    val checkedSize: RemoteCornerSize,
    val uncheckedSize: RemoteCornerSize,
) : RemoteCornerSize {
    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat =
        checked.select(
            ifTrue = checkedSize.toPx(shapeSize, density),
            ifFalse = uncheckedSize.toPx(shapeSize, density),
        )
}

/**
 * Returns a copy of this shape with each corner size scaled by [fraction], used to derive the
 * pressed shapes of the variant animated toggle button shapes.
 */
internal fun RemoteCornerBasedShape.scaleCorners(fraction: Float): RemoteCornerBasedShape =
    copy(
        topStart = FractionalCornerSize(topStart, fraction),
        topEnd = FractionalCornerSize(topEnd, fraction),
        bottomEnd = FractionalCornerSize(bottomEnd, fraction),
        bottomStart = FractionalCornerSize(bottomStart, fraction),
    )

/** A [RemoteCornerSize] that is [fraction] of [size]. */
private data class FractionalCornerSize(val size: RemoteCornerSize, val fraction: Float) :
    RemoteCornerSize {
    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat =
        size.toPx(shapeSize, density) * fraction.rf
}

/**
 * Returns the progress of the animation between the unchecked (`0`) and checked (`1`) states of a
 * toggle button, used to animate its colors like Wear Material3. Returns `null` when [checked] is a
 * constant, in which case there is nothing to animate.
 */
@Composable
internal fun animatedCheckedProgress(
    checked: RemoteBoolean,
    animationSpec: RemoteAnimationSpec = CheckedAnimationSpec,
): RemoteFloat? {
    if (checked.hasConstantValue) return null
    return remember(checked, animationSpec) {
        animateRemoteFloatAsState(checked.select(1f.rf, 0f.rf), animationSpec)
    }
}

/**
 * Returns [checkedColor] or [uncheckedColor] for [checked], interpolated by [checkedProgress] when
 * it is not `null`.
 */
internal fun checkedColor(
    checked: RemoteBoolean,
    checkedProgress: RemoteFloat?,
    checkedColor: RemoteColor,
    uncheckedColor: RemoteColor,
): RemoteColor =
    if (checkedProgress == null) {
        checked.select(checkedColor, uncheckedColor)
    } else {
        lerp(uncheckedColor, checkedColor, checkedProgress)
    }

/**
 * Default [RemoteAnimationSpec] for the color animation between checked and unchecked states,
 * matching `COLOR_ANIMATION_SPEC` of Wear Material3 toggle buttons.
 */
internal val CheckedAnimationSpec: RemoteAnimationSpec =
    remoteTween(RemoteMotionTokens.DurationMedium1, RemoteMotionTokens.EasingStandardDecelerate)

/**
 * Fraction of the corner sizes of a variant toggle button shape used for its pressed shape. See
 * `PressedShapeCornerSizeFraction` in Wear Compose Material3.
 */
internal const val PressedShapeCornerSizeFraction = 0.66f
