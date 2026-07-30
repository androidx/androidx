/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation

import kotlin.jvm.JvmField

/**
 * This is a collection of flags which are used to guard against regressions in some of the
 * "riskier" refactors or new feature support that is added to this module. These flags are always
 * "on" in the published artifact of this module, however these flags allow end consumers of this
 * module to toggle them "off" in case this new path is causing a regression.
 *
 * These flags are considered temporary, and there should be no expectation for these flags be
 * around for an extended period of time. If you have a regression that one of these flags fixes, it
 * is strongly encouraged for you to file a bug ASAP.
 *
 * **Usage:**
 *
 * In order to turn a feature off in a debug environment, it is recommended to set this to false in
 * as close to the initial loading of the application as possible. Changing this value after compose
 * library code has already been loaded can result in undefined behavior.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              ComposeFoundationFlags.SomeFeatureEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.foundation.ComposeFoundationFlags {
 *          public static boolean SomeFeatureEnabled return false
 *      }
 */
@ExperimentalFoundationApi
public object ComposeFoundationFlags {

    /**
     * Whether to use the new context menu API and default implementations in
     * [SelectionContainer][androidx.compose.foundation.text.selection.SelectionContainer], and all
     * [BasicTextField][androidx.compose.foundation.text.BasicTextField]s. If false, the previous
     * context menu that has no public APIs will be used instead.
     */
    // TODO: b/455589857
    @field:Suppress("MutableBareField")
    @JvmField
    public var isNewContextMenuEnabled: Boolean = isNewContextMenuInitiallyEnabled

    /**
     * Whether to use the new smart selection feature in
     * [androidx.compose.foundation.text.selection.SelectionContainer] and all
     * [androidx.compose.foundation.text.BasicTextField]s.
     */
    // TODO: b/455592302
    @field:Suppress("MutableBareField") @JvmField public var isSmartSelectionEnabled: Boolean = true

    /**
     * Whether to support inherited text styles. If enabled, text styles set by the styles API will
     * be inherited by text composables contained in a style box.
     */
    // TODO: b/485968143
    @field:Suppress("MutableBareField")
    @JvmField
    public var isInheritedTextStyleEnabled: Boolean = false

    /**
     * Selecting flag to enable the use of new PausableComposition in lazy layout prefetch. This
     * change allows us to distribute work we need to do during the prefetch better, for example we
     * can only perform the composition for parts of the LazyColumn's next item during one ui frame,
     * and then continue composing the rest of it in the next frames.
     */
    // TODO: b/455589928
    @field:Suppress("MutableBareField")
    @JvmField
    public var isPausableCompositionInPrefetchEnabled: Boolean = true

    /**
     * With this flag on, Pager will use
     * [androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow] as the default prefetching
     * strategy, instead of 1 item in the direction of the scroll. The window used will be 1 view
     * port AFTER the currently composed items, this includes visible and items composed through
     * beyond bounds.
     */
    // TODO: b/485967807
    @field:Suppress("MutableBareField")
    @JvmField
    public var isCacheWindowForPagerEnabled: Boolean = true

    /**
     * With this flag on, [androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow] will
     * support multi-lane configurations.
     */
    // TODO: b/522643119
    @field:Suppress("MutableBareField")
    @JvmField
    public var isMultiLaneCacheWindowEnabled: Boolean = true

    /**
     * With this flag enabled, [androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGrid]
     * layouts will make use of a cache window, either a default cache window or the cache window
     * provided by the user via the composable function arguments.
     */
    // TODO: b/530894185
    @field:Suppress("MutableBareField")
    @JvmField
    public var isUsingCacheWindowInStaggeredGrids: Boolean = true

    /**
     * With this flag enabled,
     * [androidx.compose.foundation.gestures.AnchoredDraggableState.targetValue] correctly returns
     * the [androidx.compose.foundation.gestures.AnchoredDraggableState.currentValue] when no
     * [androidx.compose.foundation.gestures.AnchoredDraggableState.anchoredDrag] is in progress.
     * Previously, [androidx.compose.foundation.gestures.AnchoredDraggableState.targetValue]
     * incorrectly returned the last-inserted anchor when there were two or more anchors with the
     * same offset, for example: `DraggableAnchors { Expanded at 100f; HalfExpanded at 0f; Hidden at
     * 0f;}` Disabling the flag restores this previous behavior.
     */
    // TODO: b/485967318
    @field:Suppress("MutableBareField")
    @JvmField
    public var isAnchoredDraggableTargetValueCalculationFixEnabled: Boolean = true

    /**
     * This flag controls performance optimizations related to
     * [androidx.compose.foundation.text.BasicTextField]'s internal min height calculations.
     */
    // TODO: Remove this flag once it has soaked (b/487251541)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isBasicTextFieldMinSizeOptimizationEnabled: Boolean = true

    /**
     * This flag controls performance optimizations related to
     * [androidx.compose.foundation.text.BasicTextField]'s internal calculations of the minimum and
     * maximum height.
     */
    // TODO: Remove this flag once it has soaked (b/501503945)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isBasicTextFieldHeightInLinesOptimizationEnabled: Boolean = true

    /**
     * This flag controls performance optimizations related to squashing multiple modifiers
     * responsible for providing default measurement of the
     * [androidx.compose.foundation.text.BasicTextField] into one.
     */
    // TODO: Remove this flag after 1.12 (b/507967106)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isBasicTextFieldSizeOptimizationEnabled: Boolean = false

    /**
     * This flag controls the fix where item placement animation in
     * [androidx.compose.foundation.lazy.LazyColumn] and [androidx.compose.foundation.lazy.LazyRow]
     * is disabled when animated scroll happens.
     */
    // TODO: Remove this flag once it has soaked (b/493183465)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isSkipItemPlacementAnimationFixEnabled: Boolean = true

    /**
     * This flag controls the fix where we correctly dispatch deltas in pager's default
     * pageNestedScrollConnection by considering the role of reverse layout.
     */
    // TODO: Remove this flag once it has soaked (b/493462428)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isReverseLayoutNestedScrollConnectionInPagerFixEnabled: Boolean = true

    /**
     * This flag controls whether [androidx.compose.foundation.text.BasicTextField]'s formatted text
     * features are enabled.
     */
    // TODO: Remove this flag once it has soaked (b/494340211)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isBasicTextFieldStyledTextEnabled: Boolean = true

    /**
     * This flag controls whether the legacy nodeOffset logic in DragGestureNode and
     * IndirectPointerInputDragCycleDetector is disabled. Previously, VelocityTracker used local
     * coordinates, necessitating manual point adjustment tracking.
     */
    // TODO: Remove this flag once it has soaked (b/457672200)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isDragNodeOffsetDoubleCountingFixEnabled: Boolean = true

    /**
     * Enables fix where coroutine scope lambda and scope are cleared on node detachment to prevent
     * reference leaking.
     */
    // TODO: b/506963276
    @field:Suppress("MutableBareField")
    @JvmField
    public var isClearNestedScrollCoroutineScopeFixEnabled: Boolean = true

    /**
     * This flag controls whether selecting text in
     * [androidx.compose.foundation.text.selection.SelectionContainer] causes scrollable ancestors
     * of the text to be scrolled when the selecting pointer is dragged outside the scrollable's
     * viewport.
     */
    // TODO: Remove this flag once it has soaked (b/504914051)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isSelectionAutoScrollEnabled: Boolean = true

    /**
     * This flag controls whether the fix for velocity tracker usage in Draggable and related
     * classes is enabled to a) properly track velocity per pointer and b) make sure to also take
     * the pointer events into account that don't move at the beginning of the gesture in order to
     * increase the stability of the computed velocity.
     */
    // TODO: Remove this flag once it has soaked (b/501080937)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isDraggableVelocityTrackerFixEnabled: Boolean = false

    /**
     * This flag controls whether it's possible to start selecting (via the mouse) text in a
     * [androidx.compose.foundation.text.selection.SelectionContainer] by dragging from the areas
     * between the text selectables.
     */
    // TODO: Remove this flag once it has soaked (b/521973612)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isMouseSelectionBetweenTextEnabled: Boolean = true

    /**
     * Disable minimum touch target expansion for inline links. Touch target expansion for
     * multi-line links causes incorrect clicks on plain text on the same lines because it uses
     * layout bounds (bounding box of multi-line path) instead of clipped shape.
     */
    // TODO: b/522377028
    @field:Suppress("MutableBareField")
    @JvmField
    public var isLinkMinimumTouchTargetSizeZeroEnabled: Boolean = false

    /**
     * This flag controls the fix where draggable was ignoring and not consuming zero delta events.
     * This caused issues with the gesture pickup feature. When this flag is enabled, zero delta
     * events will be consumed (after a drag gesture has started).
     */
    // TODO: Remove this flag once it has soaked (b/524219039)
    @field:Suppress("MutableBareField")
    @JvmField
    public var isDraggableZeroDeltaConsumptionEnabled: Boolean = true

    /**
     * This flag controls a fix in the lazy layout prefetch scheduler's idle detection. When
     * enabled, it prevents the scheduler from incorrectly identifying an idle state when a frame is
     * delayed longer than our idle detection threshold. If disabled, only the idle detection
     * threshold will be used to determine if a frame is idle, which could lead to janky frames when
     * scrolling.
     */
    // TODO: b/531649461
    @field:Suppress("MutableBareField")
    @JvmField
    public var isPrefetchSchedulerLateFrameDetectionEnabled: Boolean = true

    /**
     * This flag controls whether [androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow]
     * checks if the number of visible items has changed across iterations without scroll deltas
     * (such as when changing lookahead window sizes) and refills the cache window if needed.
     */
    // TODO: b/535884139
    @field:Suppress("MutableBareField")
    @JvmField
    public var isCacheWindowLookaheadCheckEnabled: Boolean = true

    /**
     * This flag controls whether [androidx.compose.foundation.lazy.grid.LazyGrid] prefers using the
     * default [androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow] over
     * [androidx.compose.foundation.lazy.grid.LazyGridPrefetchStrategy].
     */
    // TODO: b/536884365
    @field:Suppress("MutableBareField")
    @JvmField
    public var isPreferDefaultCacheWindowOverPrefetchStrategy: Boolean = true
}

/** The initial value of [ComposeFoundationFlags.isNewContextMenuEnabled] */
internal expect val isNewContextMenuInitiallyEnabled: Boolean
