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

package androidx.compose.ui.platform

import android.view.View
import android.view.Window
import androidx.annotation.FloatRange
import androidx.annotation.UiThread
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.ObservationTarget
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Connects a hosted Compose UI hierarchy to the Android [View] system.
 *
 * A host supplies the platform environment that a Compose hierarchy needs (its [View], [density],
 * [layoutDirection], coordinate transforms, graphics, and input method), and receives callbacks
 * from the hierarchy when it needs to be measured or drawn, when focus moves across its boundary,
 * when embedded Android [View]s are added or removed, and when its layout or semantics change.
 *
 * The host drives the hierarchy from the View system: it measures the hierarchy from
 * [View.onMeasure], lays it out from [View.onLayout] with the final size, and draws it from
 * [View.dispatchDraw]. Drawing flushes any pending measure and layout first.
 *
 * All members are called on the UI thread that owns [view], and implementations must call into the
 * hierarchy on that thread. This need not be the main thread.
 */
@ExperimentalComposeUiHostApi
@UiThread
public interface ComposeUiHost {
    /**
     * The Android [View] that represents this host in the View hierarchy.
     *
     * [view] must be the same instance for the lifetime of the host. It:
     * - receives input, input method (`View.onCheckIsTextEditor`, `View.onCreateInputConnection`,
     *   `InputMethodManager.restartInput`), and accessibility callbacks for the hosted content;
     * - is the ancestor that embedded Android [View]s are added under (see [addAndroidView]);
     * - supplies [LocalView] and [LocalContext] to the hosted content.
     *
     * [view] may be the host itself, or a [View] owned by a host that delegates to it.
     */
    public val view: View

    /**
     * The current [Density] of the host.
     *
     * Reads are snapshot-observed. Implementations must back this with snapshot state (for example
     * `mutableStateOf`) and assign a new [Density] when the font scale or density of [view]
     * changes.
     */
    public val density: Density

    /**
     * The current [LayoutDirection] of the host.
     *
     * Reads are snapshot-observed. Implementations must back this with snapshot state (for example
     * `mutableStateOf`) and update it from `View.onRtlPropertiesChanged`.
     */
    public val layoutDirection: LayoutDirection

    /** The [Window] that [view] is attached to, or `null` if it is not attached to a window. */
    public val window: Window?

    /**
     * The [GraphicsContext] used to create graphics layers for the hosted content.
     *
     * Typically `GraphicsContext(layerContainer)` with an attached `ViewGroup` in the host's View
     * tree, usually [view].
     */
    public val graphicsContext: GraphicsContext

    /**
     * Converts [relativeToLocal], a position relative to the hosted content's root, to a position
     * relative to the device's screen.
     */
    public fun localToScreen(relativeToLocal: Offset): Offset

    /**
     * Converts [relativeToScreen], a position relative to the device's screen, to a position
     * relative to the hosted content's root.
     */
    public fun screenToLocal(relativeToScreen: Offset): Offset

    /**
     * Modifies [matrix] to be a transform from the hosted content's root coordinates into screen
     * coordinates.
     *
     * [matrix] initially transforms some coordinate space into the root coordinates; on return it
     * transforms that coordinate space into screen coordinates.
     */
    public fun transformToScreen(matrix: Matrix)

    /**
     * Executes [block], observing the snapshot state it reads, and calls [onChanged] with [target]
     * when any of that state changes.
     *
     * Implementations typically delegate to a
     * [androidx.compose.runtime.snapshots.SnapshotStateObserver]. Pass [onChanged] through
     * unwrapped: the observer keys its scopes by callback identity, so wrapping it in a new lambda
     * on every call defeats that and allocates.
     *
     * The host owns the observer's lifecycle: start observing before the hosted content is attached
     * and keep observing while it is attached.
     *
     * Targets are never notified as valid after removal. Hosts may discard stale observations on
     * their own schedule, for example `observer.clearIf { !(it as
     * ObservationTarget).isObservationValid }`.
     */
    public fun <T : ObservationTarget> observeReads(
        target: T,
        onChanged: (T) -> Unit,
        block: () -> Unit,
    )

    /**
     * Called when the hosted content needs to be drawn again. View hosts typically call
     * [View.invalidate].
     */
    public fun invalidateDraw()

    /**
     * Called when the hosted content needs to be measured and laid out again. View hosts typically
     * call [View.requestLayout].
     */
    public fun invalidateMeasurement()

    /**
     * Votes for a preferred frame rate, in Hz, for the current frame.
     *
     * May be called multiple times per frame. Hosts keep the highest value, apply it when drawing
     * (for example with `View.setRequestedFrameRate` on API 35 and above), and reset it for the
     * next frame.
     *
     * @see voteFrameRateCategory
     */
    public fun voteFrameRate(@FloatRange(from = 0.0, to = 360.0) frameRate: Float)

    /**
     * Votes for a preferred [FrameRateCategory] for the current frame.
     *
     * May be called multiple times per frame. Hosts keep the highest-priority category, where
     * [FrameRateCategory.High] > [FrameRateCategory.Normal] > [FrameRateCategory.Default], apply it
     * when drawing, and reset it for the next frame.
     *
     * @see voteFrameRate
     */
    public fun voteFrameRateCategory(category: FrameRateCategory)

    /**
     * Called when Compose content requests focus while [view] does not have framework focus.
     *
     * Hosts typically call `view.requestFocus(direction, previouslyFocusedRect)`, mapping
     * [focusDirection] to the matching `View.FOCUS_*` direction. [FocusDirection.Enter] means the
     * request is not directional.
     *
     * @param focusDirection The direction focus is moving in.
     * @param previouslyFocusedRect The bounds of the previously focused element, in the hosted
     *   content's root coordinates, or `null` if unknown.
     * @return `true` if [view] holds or obtained framework focus.
     */
    public fun requestHostFocus(
        focusDirection: FocusDirection,
        previouslyFocusedRect: Rect?,
    ): Boolean

    /**
     * Called when Compose focus was cleared. Clear Android framework focus from [view] if [view]
     * itself is focused. Don't clear focus held by an embedded Android [View].
     */
    public fun clearHostFocus()

    /**
     * Adds an embedded Android [View] to the host.
     *
     * [view] is a wrapper created by Compose around the [View] supplied to `AndroidView`. The host
     * adds it to its View tree under the host's View, and draws it only from [drawAndroidView].
     * When the host performs a forced layout, it lays out [view] again at its current bounds.
     *
     * The semantics node of the embedded view is identified by [semanticsId]. The id can change
     * while [view] is attached, when its layout node is reused; the host is then notified with
     * [onNodeReused] and must re-key any state it stores by id.
     *
     * @param view The wrapper [View] to add.
     * @param semanticsId The semantics id of the layout node that contains [view].
     * @param invalidateMeasurement Called when [view] requests a layout (`View.requestLayout`), so
     *   that Compose measures it again.
     */
    public fun addAndroidView(view: View, semanticsId: Int, invalidateMeasurement: () -> Unit)

    /**
     * Removes an embedded Android [View] previously added with [addAndroidView].
     *
     * @param view The wrapper [View] to remove.
     * @param semanticsId The current semantics id of the layout node that contains [view].
     */
    public fun removeAndroidView(view: View, semanticsId: Int)

    /**
     * Draws an embedded Android [View] previously added with [addAndroidView] into [canvas].
     *
     * @param view The wrapper [View] to draw.
     * @param canvas The [Canvas] to draw [view] into.
     */
    public fun drawAndroidView(view: View, canvas: Canvas)

    /**
     * Starts a platform text input session for [request] and suspends until the session is
     * cancelled.
     *
     * Hosts make [request] available to [view]'s `onCheckIsTextEditor` and
     * `onCreateInputConnection` before the first suspension point, and stop using it when the call
     * is cancelled. There is never more than one concurrent call: the previous session is cancelled
     * and joined before a new one starts.
     */
    public suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing

    /**
     * Called when the size, position, or layer of [node] changed.
     *
     * Hosts that report node bounds outside Compose use this to refresh them, for example
     * accessibility virtual view bounds, content capture, or autofill bounds.
     * - Called on the UI thread during measure and layout, and when a `graphicsLayer` changes its
     *   transform, clip, or outline.
     * - Can be called many times per frame, and several times for the same [node]. Keep it cheap:
     *   record [node] and process the batch later, for example on the next frame.
     * - Don't measure, lay out, or change the tree from this call.
     */
    public fun onLayoutChanged(node: LayoutInfo)

    /**
     * Called when the semantics of [node] changed.
     *
     * Hosts that mirror semantics outside Compose use this to keep the copy current, for example to
     * send accessibility events or report new text values to autofill. Compare [previousConfig]
     * with the current semantics of [node] to find what changed.
     * - Called on the UI thread right after Compose recalculates the semantics of [node]: when a
     *   semantics modifier is added, removed, or updated, when [node] with semantics is attached,
     *   and when it is detached.
     * - Reports the unmerged semantics of [node] only. The change can also affect the merged
     *   semantics of its ancestors, which aren't reported separately.
     * - Can be called many times per frame. Keep it cheap and batch work, for example until
     *   [onChangesApplied].
     *
     * @param node the node whose semantics changed. Its semantics are empty after it is detached.
     * @param previousConfig the semantics of [node] before the change, or `null` if [node] had no
     *   semantics
     */
    public fun onSemanticsChanged(node: LayoutInfo, previousConfig: SemanticsConfiguration?)

    /**
     * Called when Compose focus moved from [previous] to [current].
     *
     * Hosts use this to follow the focused node outside Compose, for example to tell autofill which
     * field the user entered and left.
     * - Called on the UI thread whenever Compose sets its focused node, so [previous] and [current]
     *   can be the same node.
     * - [previous] and [current] are the layout nodes of the focus targets.
     * - Either is `null` when no node held focus, or when focus left the hosted content.
     */
    public fun onFocusChanged(previous: LayoutInfo?, current: LayoutInfo?)

    /**
     * Called after [node] joins the hosted content.
     *
     * Hosts use this to start tracking nodes that they mirror outside Compose, for example to
     * register autofillable nodes with an autofill session.
     * - Called once for each node of an added subtree, children before their parent.
     * - Called on the UI thread while composition changes are applied. For content composed during
     *   measure, such as the items of lazy lists, this is during the measure pass. Don't measure,
     *   lay out, or change the tree from this call; batch work until [onChangesApplied].
     * - [node] isn't measured or placed yet; its size and coordinates aren't valid.
     * - [LayoutInfo.semanticsId] identifies [node] until [onNodeReused] or [onNodeDetached].
     */
    public fun onNodeAttached(node: LayoutInfo)

    /**
     * Called after [node] leaves the hosted content.
     *
     * Hosts must release everything they hold for [node]: state keyed by its
     * [LayoutInfo.semanticsId], accessibility or autofill records, and references to [node] itself,
     * which would otherwise keep the removed content in memory.
     * - Called once for each node of a removed subtree, children before their parent. Disposing the
     *   hosted content removes every node except the root.
     * - Called on the UI thread while composition changes are applied, like [onNodeAttached].
     * - The modifiers of [node] are already detached. Don't read its semantics or coordinates.
     * - Observations of [node] made through [observeReads] are no longer needed; see
     *   [androidx.compose.ui.node.ObservationTarget.isObservationValid].
     * - Not called for nodes that are only deactivated; see [onNodeDeactivated].
     */
    public fun onNodeDetached(node: LayoutInfo)

    /**
     * Called when [node] is deactivated: kept in the hosted content for later reuse, without
     * content.
     *
     * [androidx.compose.ui.layout.SubcomposeLayout], which lazy lists use, and
     * [androidx.compose.runtime.ReusableContentHost] deactivate nodes instead of removing them.
     * Treat [node] as removed for accessibility, autofill, and content capture, and drop state
     * keyed by its [LayoutInfo.semanticsId].
     * - [node] stays attached, so [onNodeDetached] isn't called until it is discarded.
     * - The modifiers of [node] are reset and its semantics are cleared.
     * - Next, [node] is either reused, see [onNodeReused], or detached, see [onNodeDetached].
     * - Called on the UI thread, often during the measure pass of a lazy layout.
     */
    public fun onNodeDeactivated(node: LayoutInfo)

    /**
     * Called when [node] is reused for different content.
     *
     * A node is reused after [onNodeDeactivated], or when the key of
     * [androidx.compose.runtime.ReusableContent] changes. Treat [node] as newly added content:
     * re-key state stored by [previousSemanticsId] to the new [LayoutInfo.semanticsId], for example
     * embedded Views added with [addAndroidView], and track it again as in [onNodeAttached].
     * - Called after the modifiers of [node] are attached again and its semantics recalculated.
     * - [node] stays attached throughout, so [onNodeAttached] isn't called.
     * - Called on the UI thread, often during the measure pass of a lazy layout.
     *
     * @param node the reused node, with its new [LayoutInfo.semanticsId]
     * @param previousSemanticsId the semantics id of [node] before it was reused
     */
    public fun onNodeReused(node: LayoutInfo, previousSemanticsId: Int)

    /**
     * Called when the content of [node] asks to show autofill suggestions.
     *
     * Content asks with [androidx.compose.ui.node.requestAutofill], for example when the user picks
     * the autofill action of a text field's context menu. Hosts that support autofill show the
     * suggestions for [node], for example with
     * [android.view.autofill.AutofillManager.requestAutofill] and the bounds of [node]. Hosts
     * without autofill ignore the call.
     * - Called on the UI thread.
     */
    public fun onAutofillRequested(node: LayoutInfo)

    /**
     * Called after a batch of composition changes was applied to the hosted content.
     *
     * Hosts use this to flush work they batched during [onNodeAttached], [onNodeDetached],
     * [onNodeDeactivated], [onNodeReused], and [onSemanticsChanged], for example to commit autofill
     * changes or to discard observations of removed nodes.
     * - Called on the UI thread once per batch: for the main composition, and for each
     *   subcomposition, such as the items of lazy lists, which apply changes during measure.
     * - Can be called several times per frame. Keep it cheap.
     */
    public fun onChangesApplied()
}
