/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.modifier

import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val ToolbarRequesterNotInitialized = "ToolbarRequester is not initialized."

/**
 * Use in conjunction with
 * [Modifier.textContextMenuToolbarHandler(...)][textContextMenuToolbarHandler] to request the
 * toolbar to be shown or hidden.
 */
internal abstract class ToolbarRequester {
    internal var toolbarHandlerNode: TextContextMenuToolbarHandlerNode? = null

    internal fun requireNode(): TextContextMenuToolbarHandlerNode =
        checkPreconditionNotNull(toolbarHandlerNode) { ToolbarRequesterNotInitialized }

    /** Shows the toolbar. */
    abstract fun show()

    /** Hides the toolbar. */
    abstract fun hide()
}

/**
 * Use in conjunction with
 * [Modifier.textContextMenuToolbarHandler(...)][textContextMenuToolbarHandler] to request the
 * toolbar to be shown or hidden.
 */
internal class ToolbarRequesterImpl : ToolbarRequester() {

    override fun show() {
        requireNode().show()
    }

    override fun hide() {
        toolbarHandlerNode?.hide()
    }
}

/**
 * Add this modifier to a component to be able to show or hide the text toolbar using the input
 * [requester]. `suspend` [onShow]/[onHide] callbacks are available if you need to run any
 * setup/cleanup before showing the toolbar. The modifier will use this point in the hierarchy to
 * visit ancestors in search for
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents] and
 * [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents] and then provide the
 * results to the [LocalTextContextMenuToolbarProvider]'s [currentValueOf]'s
 * [showTextContextMenu][TextContextMenuProvider.showTextContextMenu].
 *
 * @param requester The [ToolbarRequester] to attach to this point in the hierarchy.
 * @param onShow A `suspend` lambda called right before showing the toolbar.
 * @param onHide A `suspend` lambda called when the toolbar is hidden.
 * @param computeContentBounds Lambda that tells the toolbar where to position around. This is used
 *   for computing [TextContextMenuDataProvider.contentBounds]. The resulting bounds should be
 *   relative to the input [LayoutCoordinates]. If null is returned, then the previous bound results
 *   will be used. If null is returned on the first invocation, then [Rect.Zero] will be used.
 */
internal fun Modifier.textContextMenuToolbarHandler(
    requester: ToolbarRequester,
    onShow: (suspend () -> Unit)? = null,
    onHide: (suspend () -> Unit)? = null,
    computeContentBounds: (destinationCoordinates: LayoutCoordinates) -> Rect?,
): Modifier =
    this then TextContextMenuToolbarHandlerElement(requester, onShow, onHide, computeContentBounds)

private class TextContextMenuToolbarHandlerElement(
    private val requester: ToolbarRequester,
    private val onShow: (suspend () -> Unit)?,
    private val onHide: (suspend () -> Unit)?,
    private val computeContentBounds: (LayoutCoordinates) -> Rect?,
) : ModifierNodeElement<TextContextMenuToolbarHandlerNode>() {
    override fun create(): TextContextMenuToolbarHandlerNode =
        TextContextMenuToolbarHandlerNode(requester, onShow, onHide, computeContentBounds)

    override fun update(node: TextContextMenuToolbarHandlerNode) {
        node.requester.toolbarHandlerNode = null
        node.requester = requester
        node.requester.toolbarHandlerNode = node

        node.onShow = onShow
        node.onHide = onHide
        node.computeContentBounds = computeContentBounds
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector info
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextContextMenuToolbarHandlerElement) return false

        if (requester !== other.requester) return false
        if (onShow !== other.onShow) return false
        if (onHide !== other.onHide) return false
        if (computeContentBounds !== other.computeContentBounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + requester.hashCode()
        result = 31 * result + onShow.hashCode()
        result = 31 * result + onHide.hashCode()
        result = 31 * result + computeContentBounds.hashCode()
        return result
    }
}

internal class TextContextMenuToolbarHandlerNode(
    var requester: ToolbarRequester,
    var onShow: (suspend () -> Unit)?,
    var onHide: (suspend () -> Unit)?,
    var computeContentBounds: (LayoutCoordinates) -> Rect?,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, TextContextMenuDataProvider {

    /** [Job] that is showing the text toolbar. */
    private var textToolbarJob: Job? = null

    private val derivedData: TextContextMenuData by derivedStateOf {
        // This can update as the modifier is getting disposed,
        // so return empty if we aren't attached to avoid crashing.
        if (isAttached) collectTextContextMenuData() else TextContextMenuData.Empty
    }

    private var previousContentBounds: Rect = Rect.Zero

    override fun onAttach() {
        super.onAttach()
        requester.toolbarHandlerNode = this
    }

    override fun onDetach() {
        requester.toolbarHandlerNode = null
        super.onDetach()
    }

    fun show() {
        if (!isAttached || textToolbarJob?.isActive == true) return
        val provider = currentValueOf(LocalTextContextMenuToolbarProvider) ?: return

        textToolbarJob =
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    onShow?.invoke()
                    provider.showTextContextMenu(this@TextContextMenuToolbarHandlerNode)
                } finally {
                    onHide?.invoke()
                }
            }
    }

    fun hide() {
        val job = textToolbarJob ?: return
        job.cancel()
        textToolbarJob = null
    }

    override fun position(destinationCoordinates: LayoutCoordinates): Offset =
        contentBounds(destinationCoordinates).topLeft

    // This can update as the modifier is getting disposed,
    // so return zero if we aren't attached to avoid crashing.
    override fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect {
        if (!isAttached) return previousContentBounds

        val computedContentBounds = computeContentBounds(destinationCoordinates)
        if (computedContentBounds == null) return previousContentBounds

        previousContentBounds = computedContentBounds
        return computedContentBounds
    }

    override fun data(): TextContextMenuData = derivedData
}

/**
 * Translates the [rootContentBounds] computed from the [localCoordinates] to the
 * [destinationCoordinates].
 */
internal fun translateRootToDestination(
    rootContentBounds: Rect,
    localCoordinates: LayoutCoordinates,
    destinationCoordinates: LayoutCoordinates,
): Rect {
    if (!localCoordinates.isAttached || !destinationCoordinates.isAttached) return Rect.Zero
    val rootContentPosition = rootContentBounds.topLeft
    val rootCoordinates = localCoordinates.findRootCoordinates()
    val destinationContentPosition =
        destinationCoordinates.localPositionOf(
            sourceCoordinates = rootCoordinates,
            relativeToSource = rootContentPosition,
        )
    return Rect(destinationContentPosition, rootContentBounds.size)
}
