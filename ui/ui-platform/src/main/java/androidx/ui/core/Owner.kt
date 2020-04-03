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
package androidx.ui.core

import androidx.annotation.RestrictTo
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.semantics.SemanticsOwner
import androidx.ui.graphics.Canvas
import androidx.ui.input.TextInputService
import androidx.ui.savedinstancestate.UiSavedStateRegistry
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxSize
import org.jetbrains.annotations.TestOnly

/**
 * Owner implements the connection to the underlying view system. On Android, this connects
 * to Android [android.view.View]s and all layout, draw, input, and accessibility is hooked
 * through them.
 */
interface Owner {

    /**
     * The root layout node in the component tree.
     */
    val root: LayoutNode

    /**
     * Used for updating the ConfigurationAmbient when configuration changes - consume the
     * configuration ambient instead of changing this observer if you are writing a component
     * that adapts to configuration changes.
     */
    var configurationChangeObserver: () -> Unit

    /**
     * Provide haptic feedback to the user. Use the Android version of haptic feedback.
     */
    val hapticFeedBack: HapticFeedback

    /**
     * Provide clipboard manager to the user. Use the Android version of clipboard manager.
     */
    val clipboardManager: ClipboardManager

    /**
     *  A data structure used to store autofill information. It is used by components that want to
     *  provide autofill semantics.
     *  TODO(ralu): Replace with SemanticsTree. This is a temporary hack until we have a semantics
     *  tree implemented.
     */
    val autofillTree: AutofillTree

    /**
     * The [Autofill] class can be used to perform autofill operations. It is used as an ambient.
     */
    val autofill: Autofill?

    /**
     * The current instance of [UiSavedStateRegistry]. If it's null you can wait for it to became
     * available using [setOnSavedStateRegistryAvailable].
     */
    val savedStateRegistry: UiSavedStateRegistry?

    /**
     * Allows other components to be notified when the [UiSavedStateRegistry] became available.
     */
    fun setOnSavedStateRegistryAvailable(callback: (UiSavedStateRegistry) -> Unit)

    val density: Density

    val semanticsOwner: SemanticsOwner

    val textInputService: TextInputService

    val fontLoader: Font.ResourceLoader

    /**
     * `true` when layout should draw debug bounds.
     */
    var showLayoutBounds: Boolean
        /** @suppress */
        @TestOnly
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        set

    /**
     * Called from a [DrawNode], this registers with the underlying view system that a
     * redraw of the given [drawNode] is required. It may cause other nodes to redraw, if
     * necessary.
     */
    fun onInvalidate(drawNode: DrawNode)

    /**
     * Called from a [LayoutNode], this registers with the underlying view system that a
     * redraw of the given [layoutNode] is required. It may cause other nodes to redraw, if
     * necessary. Note that [LayoutNode]s are able to draw due to draw modifiers applied to them.
     */
    fun onInvalidate(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] to indicate the new size of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onSizeChange(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] to indicate the new position of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onPositionChange(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] to request the Owner a new measurement+layout.
     */
    fun onRequestMeasure(layoutNode: LayoutNode)

    /**
     * Called by [ComponentNode] when it is attached to the view system and now has an owner.
     * This is used by [Owner] to update [ComponentNode.ownerData] and track which nodes are
     * associated with it. It will only be called when [node] is not already attached to an
     * owner.
     */
    fun onAttach(node: ComponentNode)

    /**
     * Called by [ComponentNode] when it is detached from the view system, such as during
     * [ComponentNode.removeAt]. This will only be called for [node]s that are already
     * [ComponentNode.attach]ed.
     */
    fun onDetach(node: ComponentNode)

    /**
     * Returns the most global position of the owner that Compose can access (such as the device
     * screen).
     */
    fun calculatePosition(): IntPxPosition

    /**
     * Ask the system to provide focus to this owner.
     *
     * @return true if the system granted focus to this owner. False otherwise.
     */
    fun requestFocus(): Boolean

    /**
     * Observing the model reads are temporary disabled during the [block] execution.
     * For example if we are currently within the measure stage and we want some code block to
     * be skipped from the observing we disable if before calling the block, execute block and
     * then enable it again.
     */
    fun pauseModelReadObserveration(block: () -> Unit)

    /**
     * Observe model reads during layout of [node], executed in [block].
     */
    fun observeLayoutModelReads(node: LayoutNode, block: () -> Unit)

    /**
     * Observe model reads during measure of [node], executed in [block].
     */
    fun observeMeasureModelReads(node: LayoutNode, block: () -> Unit)

    /**
     * Causes the [node] to draw into [canvas].
     */
    fun callDraw(canvas: Canvas, node: ComponentNode, parentSize: PxSize)

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    fun measureAndLayout()

    /**
     * Creates and returns an [OwnedLayer] for the given [drawLayerModifier].
     */
    fun createLayer(
        drawLayerModifier: DrawLayerModifier,
        drawBlock: (Canvas, Density) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer

    val measureIteration: Long

    companion object {
        /**
         * Enables additional (and expensive to do in production) assertions. Useful to be set
         * to true during the tests covering our core logic.
         */
        var enableExtraAssertions: Boolean = false
    }
}
