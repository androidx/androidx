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
object ComposeFoundationFlags {

    /**
     * Whether to use the new context menu API and default implementations in
     * [SelectionContainer][androidx.compose.foundation.text.selection.SelectionContainer], and all
     * [BasicTextField][androidx.compose.foundation.text.BasicTextField]s. If false, the previous
     * context menu that has no public APIs will be used instead.
     */
    // TODO: b/455589857
    @field:Suppress("MutableBareField") @JvmField var isNewContextMenuEnabled = true

    /**
     * Whether to use the new smart selection feature in
     * [androidx.compose.foundation.text.selection.SelectionContainer] and all
     * [androidx.compose.foundation.text.BasicTextField]s.
     */
    // TODO: b/455592302
    @field:Suppress("MutableBareField") @JvmField var isSmartSelectionEnabled = true

    /**
     * Whether to support inherited text styles. If enabled, text styles set by the styles API will
     * be inherited by text composables contained in a style box.
     */
    // TODO: b/485968143
    @field:Suppress("MutableBareField") @JvmField var isInheritedTextStyleEnabled = true

    /**
     * Selecting flag to enable the use of new PausableComposition in lazy layout prefetch. This
     * change allows us to distribute work we need to do during the prefetch better, for example we
     * can only perform the composition for parts of the LazyColumn's next item during one ui frame,
     * and then continue composing the rest of it in the next frames.
     */
    // TODO: b/455589928
    @field:Suppress("MutableBareField") @JvmField var isPausableCompositionInPrefetchEnabled = true

    /**
     * With this flag on, Pager will use Cache Window as the default prefetching strategy, instead
     * of 1 item in the direction of the scroll. The window used will be 1 view port AFTER the
     * currently composed items, this includes visible and items composed through beyond bounds.
     */
    // TODO: b/485967807
    @field:Suppress("MutableBareField") @JvmField var isCacheWindowForPagerEnabled = true

    /**
     * When Pager was used with a keyboard in RTL the pages would bounce indefinitely due to the
     * bring into view animation. If this flag is off the fix for that behavior will be disabled.
     */
    // TODO: b/485967682
    @field:Suppress("MutableBareField")
    @JvmField
    var isBringIntoViewRltBouncyBehaviorInPagerFixEnabled: Boolean = true

    /**
     * If this flag is enabled, for lazy layout implementations that use
     * [androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow], if the dataset changes, the
     * window mechanism will understand that it needs to re-fill the window from scratch. This is
     * because there is no good way for the window to know that a possible non-visible item has
     * changed. For instance, if C and D are 2 items in the cache window and later they're removed
     * from the dataset, the cache window won't know it until it tries to prefetch them.
     */
    // TODO: b/485967875
    @field:Suppress("MutableBareField") @JvmField var isCacheWindowRefillFixEnabled = true

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
    var isAnchoredDraggableTargetValueCalculationFixEnabled = true

    /**
     * If this flag is enabled, Clickable will detect if it should delay press by using the new
     * GestureNode structure where nodes can indicate if they're interested in a given
     * PointerInputEvent. Moreover, all containers where a drag gesture happens (e.g. scrollable,
     * draggable, anchored draggable) will cause the presses to be delayed.
     */
    // TODO: b/485966702
    @field:Suppress("MutableBareField")
    @JvmField
    var isDelayPressesUsingGestureConsumptionEnabled = true

    /**
     * Enables support of trackpad gesture events in foundation components.
     *
     * This uses the additional trackpad gesture information enabled by
     * `ComposeUiFlags.isTrackpadGestureHandlingEnabled`
     */
    // TODO: b/475634969 remove the temporary flag
    @field:Suppress("MutableBareField")
    @JvmField
    var isTrackpadGestureHandlingEnabled: Boolean = true

    /**
     * With this flag on, nested draggable components (e.g. Lists, Pagers, Grids) will handle
     * conflicting gestures by deciding which has a higher priority.
     */
    // TODO: b/485966180
    @field:Suppress("MutableBareField")
    @JvmField
    var isNestedDraggablesTouchConflictFixEnabled = true

    /**
     * With this flag on we don't use suspend pointer input as part of Modifier.combinedClickable
     * implementation as an optimization.
     */
    // TODO: b/485966320
    @field:Suppress("MutableBareField")
    @JvmField
    var isNonSuspendingPointerInputInCombinedClickableEnabled = true
}
