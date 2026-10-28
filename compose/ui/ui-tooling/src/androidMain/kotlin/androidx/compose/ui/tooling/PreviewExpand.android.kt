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

package androidx.compose.ui.tooling

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import kotlin.math.roundToInt

// Spec parameter keys (normalized lowercase for lookup)
private const val PARAM_EXPAND = "expand"
private const val PARAM_MAX_HEIGHT = "maxheight"
private const val PARAM_MAX_WIDTH = "maxwidth"

// Spec parameter values
private const val EXPAND_VERTICAL = "vertical"
private const val EXPAND_HORIZONTAL = "horizontal"

/**
 * Default maximum size, in pixels, of the expanded axis of a preview, used when the device spec
 * does not specify a valid `maxHeight` / `maxWidth`.
 */
internal const val DEFAULT_EXPAND_MAX_SIZE_PX = 4096

/** Remaining scroll range, in pixels, below which a scrollable container is considered to fit. */
private const val SCROLL_OVERFLOW_TOLERANCE_PX = 0.5f

/** Kotlin file class declaring Wear Compose's `LocalReduceMotion`. */
private const val WEAR_COMPOSITION_LOCALS_CLASS =
    "androidx.wear.compose.foundation.CompositionLocalsKt"

/**
 * Returns Wear Compose's `LocalReduceMotion` if Wear Compose can be loaded from [classLoader], or
 * null otherwise.
 */
@Suppress("BanUncheckedReflection")
@VisibleForTesting
internal fun findWearLocalReduceMotion(
    classLoader: ClassLoader?
): ProvidableCompositionLocal<Boolean>? =
    try {
        @Suppress("UNCHECKED_CAST")
        Class.forName(WEAR_COMPOSITION_LOCALS_CLASS, false, classLoader)
            .getMethod("getLocalReduceMotion")
            .invoke(null) as? ProvidableCompositionLocal<Boolean>
    } catch (_: ReflectiveOperationException) {
        null
    } catch (_: LinkageError) {
        null
    } catch (_: SecurityException) {
        null
    }

/**
 * Wear Compose's `LocalReduceMotion`, or null if Wear Compose is not in the classpath. Resolved
 * reflectively since ui-tooling can not depend on Wear Compose.
 */
internal val wearLocalReduceMotion: ProvidableCompositionLocal<Boolean>? by lazy {
    findWearLocalReduceMotion(ComposeViewAdapter::class.java.classLoader)
}

/** Axis along which a preview grows to display all its scrollable content. */
internal enum class PreviewExpandAxis {
    VERTICAL,
    HORIZONTAL,
}

/**
 * Expansion settings of a preview, read from its device spec.
 *
 * @param axis the axis along which the preview grows to display all its scrollable content.
 * @param maxSizePx the maximum size, in pixels, of the preview along [axis].
 */
internal data class PreviewExpandSettings(val axis: PreviewExpandAxis, val maxSizePx: Int)

/**
 * Parses the expansion settings from a `spec:` device string, e.g.
 * `spec:parent=pixel_7,expand=vertical,maxHeight=1200dp`:
 * - `expand`: `vertical` or `horizontal` (case-insensitive), the axis along which the preview grows
 *   to display all its scrollable content.
 * - `maxHeight` (for `vertical`) / `maxWidth` (for `horizontal`): the maximum size of the expanded
 *   axis, as a number with a `dp` or `px` suffix. `dp` values are converted to pixels with
 *   [density]. The key that does not match the axis is ignored.
 *
 * Returns null when [deviceSpec] is not a `spec:` string or when `expand` is absent or invalid. An
 * absent or invalid maximum size falls back to [DEFAULT_EXPAND_MAX_SIZE_PX].
 */
internal fun parsePreviewExpandSettings(
    deviceSpec: String?,
    density: Float,
): PreviewExpandSettings? {
    val params = parseDeviceSpecParams(deviceSpec) ?: return null
    val axis =
        when (params[PARAM_EXPAND]) {
            EXPAND_VERTICAL -> PreviewExpandAxis.VERTICAL
            EXPAND_HORIZONTAL -> PreviewExpandAxis.HORIZONTAL
            else -> return null
        }
    val maxSizeKey =
        when (axis) {
            PreviewExpandAxis.VERTICAL -> PARAM_MAX_HEIGHT
            PreviewExpandAxis.HORIZONTAL -> PARAM_MAX_WIDTH
        }
    val maxSizePx = parseSizePx(params[maxSizeKey], density) ?: DEFAULT_EXPAND_MAX_SIZE_PX
    return PreviewExpandSettings(axis, maxSizePx)
}

/**
 * Parses a size with a required `dp` or `px` suffix (e.g. `1200dp`, `3000px`) into pixels. Returns
 * null if [value] is null, has no valid unit or number, or is smaller than one pixel.
 */
private fun parseSizePx(value: String?, density: Float): Int? {
    val px =
        when {
            value == null -> null
            value.endsWith(DP_SUFFIX) ->
                value.removeSuffix(DP_SUFFIX).toFloatOrNull()?.let { it * density }
            value.endsWith(PX_SUFFIX) -> value.removeSuffix(PX_SUFFIX).toFloatOrNull()
            else -> null
        } ?: return null
    if (!px.isFinite()) return null
    return px.roundToInt().takeIf { it >= 1 }
}

/**
 * Updates `view.resources.configuration.windowConfiguration` and `ViewRootImpl`'s merged
 * configurations to have bounds `[0, 0, width, height]` when running under LayoutLib, so
 * `ViewRootImpl.relayoutWindow` takes the async relayout path with the expanded window bounds
 * instead of shrinking `mWinFrame` back to the initial screen bounds or triggering a full
 * configuration change during `performTraversals()`.
 */
internal fun updateWindowConfigurationBounds(view: View, width: Int, height: Int) {
    WindowConfigurationBoundsSupport.instance?.updateBounds(
        view,
        view.resources.configuration,
        width,
        height,
    )
}

/**
 * Marks ancestors above [View.getParent] as needing layout when measured with `UNSPECIFIED`. In
 * LayoutLib's `RenderSessionImpl.measureLayout`, the first pass measures `mViewRoot` (clearing its
 * `PFLAG_FORCE_LAYOUT` flag) and the second pass only measures `mContentRoot` (the direct parent,
 * whose `measure()` is currently executing and will clear its own flag on return). Marking the
 * ancestors above `parent` ensures `ViewRootImpl.performTraversals()` re-measures and lays out the
 * full hierarchy from `mViewRoot` down at the expanded size.
 */
internal fun forceAncestorLayouts(view: View) {
    var ancestor = view.parent?.parent
    while (ancestor is View) {
        ancestor.forceLayout()
        ancestor = ancestor.parent
    }
}

internal fun layoutComposeView(composeView: ComposeView) {
    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
}

/**
 * Composes the preview from scratch, then measures and lays it out at exactly [height], so the
 * positions and scroll ranges of its content are those of a device that is [height] tall.
 *
 * Re-composing is needed since measuring the same composition at different heights leaves state
 * behind. For example, a lazy list measured at a small height and scrolled to its end stays
 * scrolled when measured again at a larger height: it reports it can not scroll forward although
 * the start of its content is not visible anymore, and the end of its content padding (e.g. the
 * space reserved for Wear's `EdgeButton`) may still be cut off.
 */
private fun composeAndLayoutAt(
    composeView: ComposeView,
    height: Int,
    measureExactly: (Int) -> Unit,
    recreateComposition: () -> Unit,
) {
    recreateComposition()
    measureExactly(height)
    layoutComposeView(composeView)
}

/**
 * Returns the smallest height in `[minHeight, maxHeight]` at which the content of every vertically
 * scrollable container in the composition is visible, leaving the preview freshly composed,
 * measured at that exact height and laid out. If the content still overflows at [maxHeight],
 * [maxHeight] is returned. If everything fits at [minHeight], [minHeight] is returned.
 */
internal fun findExpandedHeight(
    composeView: ComposeView,
    minHeight: Int,
    maxHeight: Int,
    measureExactly: (Int) -> Unit,
    recreateComposition: () -> Unit,
): Int {
    measureExactly(maxHeight)
    layoutComposeView(composeView)
    if (minHeight >= maxHeight || hasVerticalScrollOverflow(composeView)) return maxHeight

    composeAndLayoutAt(composeView, minHeight, measureExactly, recreateComposition)
    if (!hasVerticalScrollOverflow(composeView)) return minHeight

    // Invariant: overflow at lo, no overflow at hi.
    var lo = minHeight
    var hi = maxHeight
    var lastProbedHeight = minHeight
    while (hi - lo > 1) {
        val mid = lo + (hi - lo) / 2
        composeAndLayoutAt(composeView, mid, measureExactly, recreateComposition)
        lastProbedHeight = mid
        if (hasVerticalScrollOverflow(composeView)) lo = mid else hi = mid
    }
    if (lastProbedHeight != hi) {
        composeAndLayoutAt(composeView, hi, measureExactly, recreateComposition)
    }
    return hi
}

private fun SemanticsNode.canScrollForward(): Boolean {
    val range = config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) ?: return false
    return range.maxValue() - range.value() > SCROLL_OVERFLOW_TOLERANCE_PX
}

/**
 * Returns true if any vertically scrollable container in the composition can still scroll forward
 * according to its [SemanticsProperties.VerticalScrollAxisRange] (exposed by `verticalScroll`,
 * `LazyColumn`, Wear `ScalingLazyColumn` and `TransformingLazyColumn`).
 */
private fun hasVerticalScrollOverflow(composeView: ComposeView): Boolean {
    val root = composeView.getChildAt(0) as? RootForTest ?: return false
    return root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = false).any {
        it.canScrollForward()
    }
}
