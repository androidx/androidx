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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import java.lang.reflect.Field
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

/** Fully qualified name of Wear Compose's `ScalingLazyListState`. */
private const val WEAR_SCALING_LAZY_LIST_STATE_CLASS =
    "androidx.wear.compose.foundation.lazy.ScalingLazyListState"

/**
 * Returns Wear Compose's `ScalingLazyListState` class if it can be loaded from [classLoader], or
 * null otherwise.
 */
@VisibleForTesting
internal fun findWearScalingLazyListStateClass(classLoader: ClassLoader?): Class<*>? =
    try {
        Class.forName(WEAR_SCALING_LAZY_LIST_STATE_CLASS, false, classLoader)
    } catch (_: ReflectiveOperationException) {
        null
    } catch (_: LinkageError) {
        null
    } catch (_: SecurityException) {
        null
    }

/** Wear Compose's `ScalingLazyListState` class, or null if Wear Compose is not in the classpath. */
private val wearScalingLazyListStateClass: Class<*>? by lazy {
    findWearScalingLazyListStateClass(ComposeViewAdapter::class.java.classLoader)
}

/**
 * Reflective field handles for `ScalingLazyListState`, resolved lazily only when a
 * `ScalingLazyListState` instance is actually encountered in a vertically expanded preview.
 */
private val wearScalingLazyListStateSupport: WearScalingLazyListStateSupport? by lazy {
    wearScalingLazyListStateClass?.let(WearScalingLazyListStateSupport::forClass)
}

/**
 * Reflective handles used to collapse the auto-centering spacers of Wear Compose's
 * `ScalingLazyListState` when expanding a preview vertically.
 *
 * By default, `ScalingLazyColumn` enables `autoCentering`: it inserts a top and a bottom `Spacer`
 * whose heights (`topAutoCenteringItemSizePx` and `bottomAutoCenteringItemSizePx`) are ~half the
 * viewport height so the anchor item sits at the vertical center of a watch face. When a preview is
 * expanded vertically, `constraints.maxHeight` is the expanded canvas height, so those spacers
 * would leave the top half of the tall preview empty. Replacing the two spacer-height `State`
 * delegates with a constant `0` state and marking the state `initialized` collapses both spacers to
 * 0px while keeping `config.autoCentering` non-null (so `ScalingLazyListState.layoutInfo` still
 * excludes the spacer items from `visibleItemsInfo` and `totalItemsCount`).
 */
@VisibleForTesting
internal class WearScalingLazyListStateSupport
private constructor(
    private val topSpacerDelegateField: Field,
    private val bottomSpacerDelegateField: Field,
    private val initializedField: Field,
    private val configField: Field,
    private val configAutoCenteringField: Field,
) {
    private val zeroState: State<Int> =
        object : State<Int> {
            override val value: Int
                get() = 0
        }

    /**
     * Collapses the auto-centering spacers of [state] (`ScalingLazyListState`). Returns true if
     * [state] was updated.
     */
    @Suppress("BanUncheckedReflection")
    fun disableAutoCentering(state: Any): Boolean =
        try {
            @Suppress("UNCHECKED_CAST")
            val configState = configField.get(state) as? MutableState<Any?>
            val currentConfig = configState?.value ?: return false
            val oldAutoCentering = configAutoCenteringField.get(currentConfig) ?: return false
            topSpacerDelegateField.set(state, zeroState)
            bottomSpacerDelegateField.set(state, zeroState)
            configAutoCenteringField.set(currentConfig, null)
            @Suppress("UNCHECKED_CAST")
            val initializedState = initializedField.get(state) as? MutableState<Boolean>
            if (initializedState != null) {
                // Flipping `initialized` invalidates `layoutInfo` and the original spacer-height
                // `derivedStateOf` instances (which now see `config.autoCentering == null` and
                // evaluate to 0), marking both spacer layout nodes for re-measurement without ever
                // setting `config.value` to null.
                initializedState.value = !initializedState.value
                Snapshot.sendApplyNotifications()
                initializedState.value = true
            }
            // Restore `config.autoCentering` so `ScalingLazyListState.layoutInfo` still
            // excludes the two 0px spacer items from `visibleItemsInfo` and `totalItemsCount`.
            configAutoCenteringField.set(currentConfig, oldAutoCentering)
            true
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        } catch (_: SecurityException) {
            false
        }

    companion object {
        fun forClass(stateClass: Class<*>): WearScalingLazyListStateSupport? =
            try {
                val configClass =
                    Class.forName(
                        "$WEAR_SCALING_LAZY_LIST_STATE_CLASS\$Configuration",
                        false,
                        stateClass.classLoader,
                    )
                WearScalingLazyListStateSupport(
                    topSpacerDelegateField =
                        stateClass.getDeclaredField("topAutoCenteringItemSizePx\$delegate").apply {
                            isAccessible = true
                        },
                    bottomSpacerDelegateField =
                        stateClass
                            .getDeclaredField("bottomAutoCenteringItemSizePx\$delegate")
                            .apply { isAccessible = true },
                    initializedField =
                        stateClass.getDeclaredField("initialized").apply { isAccessible = true },
                    configField =
                        stateClass.getDeclaredField("config").apply { isAccessible = true },
                    configAutoCenteringField =
                        configClass.getDeclaredField("autoCentering").apply { isAccessible = true },
                )
            } catch (_: ReflectiveOperationException) {
                null
            } catch (_: LinkageError) {
                null
            } catch (_: SecurityException) {
                null
            }
    }
}

/** Axis along which a preview grows to display all its scrollable content. */
internal enum class PreviewExpandAxis {
    VERTICAL,
    HORIZONTAL,
}

/** The semantics property exposing the scroll range of containers scrollable along this axis. */
private val PreviewExpandAxis.scrollAxisRange: SemanticsPropertyKey<ScrollAxisRange>
    get() =
        when (this) {
            PreviewExpandAxis.VERTICAL -> SemanticsProperties.VerticalScrollAxisRange
            PreviewExpandAxis.HORIZONTAL -> SemanticsProperties.HorizontalScrollAxisRange
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
 * Composes the preview from scratch, then measures it with [measureExactly] at [size] and lays it
 * out, so the positions and scroll ranges of its content are those of a device of that size.
 *
 * Re-composing is needed since measuring the same composition at different sizes leaves state
 * behind. For example, a lazy list measured at a small height and scrolled to its end stays
 * scrolled when measured again at a larger height: it reports it can not scroll forward although
 * the start of its content is not visible anymore, and the end of its content padding (e.g. the
 * space reserved for Wear's `EdgeButton`) may still be cut off.
 */
private fun composeAndLayoutAt(
    composeView: ComposeView,
    slotTableRecord: CompositionDataRecord,
    size: Int,
    measureExactly: (Int) -> Unit,
    recreateComposition: () -> Unit,
    disableScalingLazyColumnAutoCentering: Boolean = false,
) {
    recreateComposition()
    measureExactly(size)
    layoutComposeView(composeView)
    if (disableScalingLazyColumnAutoCentering) {
        disableWearScalingLazyColumnAutoCentering(composeView, slotTableRecord) {
            measureExactly(size)
            layoutComposeView(composeView)
        }
    }
}

/**
 * Returns the smallest size along [axis] in `[minSize, maxSize]` at which the content of every
 * container in the composition scrollable along [axis] is visible, leaving the preview freshly
 * composed, measured at that exact size with [measureExactly] and laid out (with any Wear
 * `ScalingLazyColumn` auto-centering spacers collapsed when expanding vertically). If the content
 * still overflows at [maxSize], [maxSize] is returned. If everything fits at [minSize], [minSize]
 * is returned.
 */
internal fun findExpandedSize(
    composeView: ComposeView,
    slotTableRecord: CompositionDataRecord,
    axis: PreviewExpandAxis,
    minSize: Int,
    maxSize: Int,
    measureExactly: (Int) -> Unit,
    recreateComposition: () -> Unit,
): Int {
    measureExactly(maxSize)
    layoutComposeView(composeView)
    // A Wear `ScalingLazyColumn` with auto-centering adds top and bottom spacers that grow with
    // the viewport (~half the viewport each), so it would either still scroll at `maxSize` or
    // (for 1- or 2-item lists whose spacers + items equal `maxSize`) render with the top half
    // of the expanded preview empty. `disableWearScalingLazyColumnAutoCentering` only uses
    // reflection when a `ScalingLazyListState` instance is present in `slotTableRecord`, and is
    // checked once at `maxSize` to avoid walking the slot table on every binary-search probe
    // when no `ScalingLazyColumn` is present.
    val hasScalingLazyColumn =
        axis == PreviewExpandAxis.VERTICAL &&
            disableWearScalingLazyColumnAutoCentering(composeView, slotTableRecord) {
                measureExactly(maxSize)
                layoutComposeView(composeView)
            }
    if (minSize >= maxSize || hasScrollOverflow(composeView, axis)) return maxSize

    composeAndLayoutAt(
        composeView,
        slotTableRecord,
        minSize,
        measureExactly,
        recreateComposition,
        hasScalingLazyColumn,
    )
    if (!hasScrollOverflow(composeView, axis)) return minSize

    // Invariant: overflow at lo, no overflow at hi.
    var lo = minSize
    var hi = maxSize
    var lastProbedSize = minSize
    while (hi - lo > 1) {
        val mid = lo + (hi - lo) / 2
        composeAndLayoutAt(
            composeView,
            slotTableRecord,
            mid,
            measureExactly,
            recreateComposition,
            hasScalingLazyColumn,
        )
        lastProbedSize = mid
        if (hasScrollOverflow(composeView, axis)) lo = mid else hi = mid
    }
    if (lastProbedSize != hi) {
        composeAndLayoutAt(
            composeView,
            slotTableRecord,
            hi,
            measureExactly,
            recreateComposition,
            hasScalingLazyColumn,
        )
    }
    return hi
}

/** Returns true if this node, scrollable along [axis], can scroll forward along [axis]. */
private fun SemanticsNode.canScrollForward(axis: PreviewExpandAxis): Boolean {
    val range = config.getOrNull(axis.scrollAxisRange) ?: return false
    return range.maxValue() - range.value() > SCROLL_OVERFLOW_TOLERANCE_PX
}

/**
 * Returns true if any container in the composition scrollable along [axis] can still scroll forward
 * according to its [SemanticsProperties.VerticalScrollAxisRange] (exposed by `verticalScroll`,
 * `LazyColumn`, Wear `ScalingLazyColumn` and `TransformingLazyColumn`) or
 * [SemanticsProperties.HorizontalScrollAxisRange] (exposed by `horizontalScroll` and `LazyRow`).
 */
private fun hasScrollOverflow(composeView: ComposeView, axis: PreviewExpandAxis): Boolean {
    val root = composeView.getChildAt(0) as? RootForTest ?: return false
    return root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = false).any {
        it.canScrollForward(axis)
    }
}

/**
 * Collapses the auto-centering spacers of every Wear `ScalingLazyListState` in the current
 * composition, and runs [remeasure] so the `ScalingLazyColumn` re-measures with 0px spacers.
 * Returns true if at least one `ScalingLazyListState` was found and updated.
 *
 * Reflection (`getDeclaredField` / `Field.set`) only runs once a `ScalingLazyListState` instance is
 * actually found in [slotTableRecord].
 */
@OptIn(UiToolingDataApi::class)
private fun disableWearScalingLazyColumnAutoCentering(
    composeView: ComposeView,
    slotTableRecord: CompositionDataRecord,
    remeasure: () -> Unit,
): Boolean {
    val stateClass = wearScalingLazyListStateClass ?: return false
    val states = mutableSetOf<Any>()
    fun visit(group: Group) {
        for (item in group.data) {
            if (item != null && stateClass.isInstance(item)) {
                states.add(item)
            }
        }
        group.children.forEach(::visit)
    }
    slotTableRecord.store.forEach { visit(it.asTree()) }
    if (states.isEmpty()) return false
    val support = wearScalingLazyListStateSupport ?: return false
    var updated = false
    for (state in states) {
        if (support.disableAutoCentering(state)) updated = true
    }
    if (updated) {
        Snapshot.sendApplyNotifications()
        composeView.forceLayout()
        composeView.getChildAt(0)?.forceLayout()
        remeasure()
    }
    return updated
}
