/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

actual fun Modifier.safeDrawingPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "safeDrawingPadding" }) {
        WindowInsets.safeDrawing
    }

actual fun Modifier.safeGesturesPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "safeGesturesPadding" }) {
        WindowInsets.safeGestures
    }

actual fun Modifier.safeContentPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "safeContentPadding" }) {
        WindowInsets.safeContent
    }

actual fun Modifier.systemBarsPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "systemBarsPadding" }) {
        WindowInsets.systemBars
    }

actual fun Modifier.displayCutoutPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "displayCutoutPadding" }) {
        WindowInsets.displayCutout
    }

actual fun Modifier.statusBarsPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "statusBarsPadding" }) {
        WindowInsets.statusBars
    }

actual fun Modifier.imePadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "imePadding" }) {
        WindowInsets.ime
    }

actual fun Modifier.navigationBarsPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "navigationBarsPadding" }) {
        WindowInsets.navigationBars
    }

actual fun Modifier.captionBarPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "captionBarPadding" }) {
        WindowInsets.captionBar
    }

actual fun Modifier.waterfallPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "waterfallPadding" }) {
        WindowInsets.waterfall
    }

actual fun Modifier.systemGesturesPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "systemGesturesPadding" }) {
        WindowInsets.systemGestures
    }

actual fun Modifier.mandatorySystemGesturesPadding(): Modifier =
    windowInsetsPadding(debugInspectorInfo { name = "mandatorySystemGesturesPadding" }) {
        WindowInsets.mandatorySystemGestures
    }

// FIXME: Should be replaced with non-composed InsetsPaddingModifierElement
//  https://youtrack.jetbrains.com/issue/CMP-8998
@Suppress("NOTHING_TO_INLINE")
@Stable
private inline fun Modifier.windowInsetsPadding(
    noinline inspectorInfo: InspectorInfo.() -> Unit,
    crossinline insetsCalculation: @Composable () -> WindowInsets
): Modifier = composed(inspectorInfo) {
    val insets = insetsCalculation()
    _InsetsPaddingModifier(insets)
}

private val ModifierLocalConsumedWindowInsets = modifierLocalOf { WindowInsets(0, 0, 0, 0) }
private class _InsetsPaddingModifier(private val insets: WindowInsets) :
    LayoutModifier, ModifierLocalConsumer, ModifierLocalProvider<WindowInsets> {
    private var unconsumedInsets: WindowInsets by mutableStateOf(insets)
    private var consumedInsets: WindowInsets by mutableStateOf(insets)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val left = unconsumedInsets.getLeft(this, layoutDirection)
        val top = unconsumedInsets.getTop(this)
        val right = unconsumedInsets.getRight(this, layoutDirection)
        val bottom = unconsumedInsets.getBottom(this)

        val horizontal = left + right
        val vertical = top + bottom

        val childConstraints = constraints.offset(-horizontal, -vertical)
        val placeable = measurable.measure(childConstraints)

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) { placeable.place(left, top) }
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) {
            val consumed = ModifierLocalConsumedWindowInsets.current
            unconsumedInsets = insets.exclude(consumed)
            consumedInsets = consumed.union(insets)
        }
    }

    override val key: ProvidableModifierLocal<WindowInsets>
        get() = ModifierLocalConsumedWindowInsets

    override val value: WindowInsets
        get() = consumedInsets

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is _InsetsPaddingModifier) {
            return false
        }

        return other.insets == insets
    }

    override fun hashCode(): Int = insets.hashCode()
}
