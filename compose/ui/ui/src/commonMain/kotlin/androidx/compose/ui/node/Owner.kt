/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.annotation.RestrictTo
import androidx.collection.IntObjectMap
import androidx.compose.runtime.Applier
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.PlacementScope
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job

/**
 * Owner implements the connection to the underlying view system. On Android, this connects to
 * Android [views][android.view.View] and all layout, draw, input, and accessibility is hooked
 * through them.
 */
internal interface Owner : PositionCalculator {

    /** The root layout node in the component tree. */
    val root: LayoutNode

    /** A mapping of semantic id to LayoutNode. */
    val layoutNodes: IntObjectMap<LayoutNode>

    /** Draw scope reused for drawing speed up. */
    val sharedDrawScope: LayoutNodeDrawScope

    val rootForTest: RootForTest

    /** Provide haptic feedback to the user. Use the Android version of haptic feedback. */
    val hapticFeedBack: HapticFeedback

    /**
     * Provide information about the current input mode, and a way to programmatically change the
     * input mode.
     */
    val inputModeManager: InputModeManager

    /** Provide clipboard manager to the user. Use the Android version of clipboard manager. */
    val clipboardManager: @Suppress("Deprecation") androidx.compose.ui.platform.ClipboardManager

    /**
     * Provide clipboard manager with suspend function to the user. Use the Android version of
     * clipboard manager.
     */
    val clipboard: Clipboard

    /**
     * Provide accessibility manager to the user. Use the Android version of accessibility manager.
     */
    val accessibilityManager: AccessibilityManager

    /**
     * Provide access to a GraphicsContext instance used to create GraphicsLayers for providing
     * isolation boundaries for rendering portions of a Composition hierarchy as well as for
     * achieving certain visual effects like masks and blurs
     */
    val graphicsContext: GraphicsContext

    /** Provide toolbar for text-related actions, such as copy, paste, cut etc. */
    val textToolbar: TextToolbar

    /**
     * A data structure used to store autofill information. It is used by components that want to
     * provide autofill semantics.
     */
    val autofillTree: @Suppress("Deprecation") androidx.compose.ui.autofill.AutofillTree

    /**
     * The [Autofill][androidx.compose.ui.autofill.Autofill] class can be used to perform autofill
     * operations. It is used as a CompositionLocal.
     */
    val autofill: @Suppress("Deprecation") androidx.compose.ui.autofill.Autofill?

    /**
     * The [AutofillManager] class can be used to perform autofill operations. It is used as a
     * CompositionLocal.
     */
    val autofillManager: AutofillManager?

    val density: Density

    val textInputService: @Suppress("Deprecation") androidx.compose.ui.text.input.TextInputService

    val softwareKeyboardController: SoftwareKeyboardController

    val pointerIconService: PointerIconService

    /**
     * Semantics owner that provides access to
     * [SemanticsInfo][androidx.compose.ui.semantics.SemanticsInfo] and
     * [SemanticListeners][androidx.compose.ui.semantics.SemanticsListener].
     */
    val semanticsOwner: SemanticsOwner

    /** Provide a focus owner that controls focus within Compose. */
    val focusOwner: FocusOwner

    /** Provide information about the window that hosts this [Owner]. */
    val windowInfo: WindowInfo

    /** Provides a queryable and observable index of nodes' bounding rectangles */
    val rectManager: RectManager

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver"),
    )
    @Suppress("DEPRECATION")
    val fontLoader: Font.ResourceLoader

    val fontFamilyResolver: FontFamily.Resolver

    val layoutDirection: LayoutDirection

    /** `true` when layout should draw debug bounds. */
    var showLayoutBounds: Boolean
        @RestrictTo(RestrictTo.Scope.LIBRARY) @InternalCoreApi set

    /**
     * Called by [LayoutNode] to request the Owner a new measurement+layout. [forceRequest] defines
     * whether the node should bypass the logic that would reject measure requests, and therefore
     * force the measure request to be evaluated even when it's already pending measure.
     *
     * [affectsLookahead] specifies whether this measure request is for the lookahead pass.
     */
    fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean = false,
        forceRequest: Boolean = false,
        scheduleMeasureAndLayout: Boolean = true,
    )

    /**
     * Called by [LayoutNode] to request the Owner a new layout. [forceRequest] defines whether the
     * node should bypass the logic that would reject relayout requests, and therefore force the
     * relayout request to be evaluated even when it's already pending measure/layout.
     *
     * [affectsLookahead] specifies whether this relayout request is for the lookahead pass pass.
     */
    fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean = false,
        forceRequest: Boolean = false,
    )

    /**
     * Called when graphics layers have changed the position of children and the
     * OnGloballyPositionedModifiers must be called.
     */
    fun requestOnPositionedCallback(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] when it is attached to the view system and now has an owner. This is
     * used by [Owner] to track which nodes are associated with it. It will only be called when
     * [node] is not already attached to an owner.
     */
    fun onPreAttach(node: LayoutNode)

    /**
     * Called by [LayoutNode] when all children have been attached, and the modifier node's attach
     * lifecycles have been run. It will only be called after [onPreAttach].
     */
    fun onPostAttach(node: LayoutNode)

    /**
     * Called by [LayoutNode] when it is detached from the view system, such as during
     * [LayoutNode.removeAt]. This will only be called for [node]s that are already
     * [LayoutNode.attach]ed.
     */
    fun onDetach(node: LayoutNode)

    /**
     * Returns the position relative to the containing window of the [localPosition], the position
     * relative to the [Owner]. If the [Owner] is rotated, scaled, or otherwise transformed relative
     * to the window, this will not be a simple translation.
     */
    fun calculatePositionInWindow(localPosition: Offset): Offset

    /**
     * Returns the position relative to the [Owner] of the [positionInWindow], the position relative
     * to the window. If the [Owner] is rotated, scaled, or otherwise transformed relative to the
     * window, this will not be a simple translation.
     */
    fun calculateLocalPosition(positionInWindow: Offset): Offset

    /** Ask the system to request autofill values to this owner. */
    fun requestAutofill(node: LayoutNode)

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out.
     * If [sendPointerUpdate] is `true` then a simulated PointerEvent may be sent to update pointer
     * input handlers.
     *
     * This method can dispatch ViewTreeObserver events during its execution. Do not call it during
     * a view's onLayout as an associated listener may invoke side effects that may requestLayout
     * during layout, potentially putting the view hierarchy into an invalid state.
     */
    fun measureAndLayout(sendPointerUpdate: Boolean = true)

    /**
     * Measures and lays out only the passed [layoutNode]. It will be remeasured with the passed
     * [constraints].
     *
     * This method can dispatch ViewTreeObserver events during its execution. Do not call it during
     * a view's onLayout as an associated listener may invoke side effects that may requestLayout
     * during layout, potentially putting the view hierarchy into an invalid state.
     */
    fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints)

    /** Makes sure the passed [layoutNode] and its subtree is remeasured and has the final sizes. */
    fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean = false)

    /** Creates an [OwnedLayer] which will be drawing the passed [drawBlock]. */
    fun createLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer? = null,
    ): OwnedLayer

    /**
     * The semantics have changed. This function will be called when a SemanticsNode is added to or
     * deleted from the Semantics tree. It will also be called when a SemanticsNode in the Semantics
     * tree has some property change.
     */
    fun onSemanticsChange()

    /** The position and/or size of the [layoutNode] changed. */
    fun onLayoutChange(layoutNode: LayoutNode)

    fun onLayoutNodeDeactivated(layoutNode: LayoutNode)

    /**
     * Called to do internal upkeep when a [layoutNode] is reused. The modifier nodes of this layout
     * node have not been attached by the time this method finishes running.
     */
    fun onPreLayoutNodeReused(layoutNode: LayoutNode, oldSemanticsId: Int) {}

    /**
     * Called to do internal upkeep when a [layoutNode] is reused. This is only called after
     * [onPreLayoutNodeReused], at which point the modifier nodes of this layout node have been
     * attached.
     */
    fun onPostLayoutNodeReused(layoutNode: LayoutNode, oldSemanticsId: Int) {}

    /**
     * The position and/or size of an interop view (typically, an android.view.View) has changed. On
     * Android, this schedules view tree layout observer callback to be invoked for the underlying
     * platform view hierarchy.
     */
    @InternalComposeUiApi fun onInteropViewLayoutChange(view: InteropView)

    val measureIteration: Long

    /** The [ViewConfiguration] to use in the application. */
    val viewConfiguration: ViewConfiguration

    /**
     * Performs snapshot observation for blocks like draw and layout which should be re-invoked
     * automatically when the snapshot value has been changed.
     */
    val snapshotObserver: OwnerSnapshotObserver

    val modifierLocalManager: ModifierLocalManager

    /** CoroutineContext for launching coroutines in Modifier Nodes. */
    val coroutineContext: CoroutineContext

    /** The scope used to place the outermost layout. */
    val placementScope: Placeable.PlacementScope
        get() = PlacementScope(this) // default implementation for test owners

    /**
     * Registers a call to be made when the [Applier.onEndChanges] is called. [listener] should be
     * called in [onEndApplyChanges] and then removed after being called.
     */
    fun registerOnEndApplyChangesListener(listener: () -> Unit)

    /**
     * Called when [Applier.onEndChanges] executes. This must call all listeners registered in
     * [registerOnEndApplyChangesListener] and then remove them so that they are not called again.
     */
    fun onEndApplyChanges()

    /** [listener] will be notified after the current or next layout has finished. */
    fun registerOnLayoutCompletedListener(listener: OnLayoutCompletedListener)

    val dragAndDropManager: DragAndDropManager

    /**
     * Starts a new text input session and suspends until it's closed. For more information see
     * [PlatformTextInputModifierNode.establishTextInputSession].
     *
     * Implementations must ensure that new requests cancel any active request. They must also
     * ensure that the previous request is finished running all cancellation tasks before starting
     * the new session, to ensure that no session code overlaps (e.g. using [Job.cancelAndJoin]).
     */
    suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing

    /**
     * Tracks sensitive content on the screen to protect user privacy. Increment sensitive component
     * count by 1. Implementation may protect user privacy by not showing sensitive content
     * (username, password etc) to remote viewer during screen share.
     */
    fun incrementSensitiveComponentCount() {}

    /**
     * Tracks sensitive content on the screen to protect user privacy. Decrement sensitive component
     * count by 1. Implementation may protect user privacy by not showing sensitive content
     * (username, password etc) to remote viewer during screen share.
     */
    fun decrementSensitiveComponentCount() {}

    /** Increments count of modifiers requesting to stop the screen from going to sleep */
    fun incrementKeepScreenOnCount() {}

    /** Decrements count of modifiers requesting to stop the screen from going to sleep */
    fun decrementKeepScreenOnCount() {}

    /** On Android it is only available when the view is attached. */
    val outOfFrameExecutor: OutOfFrameExecutor?
        get() = null

    /** This can be used to Vote for a preferred frame rate. */
    fun voteFrameRate(frameRate: Float) {}

    /**
     * Dispatches a callback when something in this hierarchy scrolls.
     *
     * @param offset Delta scrolled.
     */
    fun dispatchOnScrollChanged(delta: Offset) {}

    companion object {
        /**
         * Enables additional (and expensive to do in production) assertions. Useful to be set to
         * true during the tests covering our core logic.
         */
        var enableExtraAssertions: Boolean = false
    }

    interface OnLayoutCompletedListener {
        fun onLayoutComplete()
    }
}
