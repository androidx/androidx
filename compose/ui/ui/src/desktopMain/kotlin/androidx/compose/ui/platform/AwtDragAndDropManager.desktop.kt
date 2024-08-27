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

package androidx.compose.ui.platform

import androidx.compose.ui.draganddrop.AwtDragAndDropTransferable
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Copy
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Link
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Move
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Image
import java.awt.MouseInfo
import java.awt.Point
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.TransferHandler
import kotlin.math.roundToInt

/**
 * Returns the AWT transfer action corresponding to the [DragAndDropTransferAction].
 */
private val DragAndDropTransferAction.awtAction: Int
    get() = when (this) {
        Copy -> TransferHandler.COPY
        Move -> TransferHandler.MOVE
        Link -> TransferHandler.LINK
        else -> TransferHandler.NONE
    }

/**
 * Returns the [DragAndDropTransferAction] corresponding to the given AWT transfer [action].
 */
internal fun DragAndDropTransferAction.Companion.fromAwtAction(
    action: Int
): DragAndDropTransferAction? = when (action) {
    TransferHandler.COPY -> Copy
    TransferHandler.MOVE -> Move
    TransferHandler.LINK -> Link
    else -> null
}

/**
 * A drag-and-drop implementation via the AWT drag-and-drop system.
 */
internal class AwtDragAndDropManager(
    private val rootContainer: JComponent,
    private val getScene: () -> ComposeScene
) {
    private val scene get() = getScene()

    private val density: Density
        get() = rootContainer.density

    private val scale: Float
        get() = density.density

    private fun Point.toOffset(): Offset {
        val scale = this@AwtDragAndDropManager.scale
        return Offset(
            x = x * scale,
            y = y * scale
        )
    }

    fun startDrag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean {
        // These should actually be the values in the local composition where dragAndDropSource was
        // used, but we don't currently have access to them, so we use the ones corresponding to
        // the root container.
        val density = this@AwtDragAndDropManager.density
        val layoutDirection = layoutDirectionFor(rootContainer)

        transferHandler.startOutgoingTransfer(
            transferData = transferData,
            dragImage = renderDragImage(
                size = decorationSize,
                density = density,
                layoutDirection = layoutDirection,
                drawDragDecoration = drawDragDecoration
            ),
            dragDecorationOffset = transferData.dragDecorationOffset
        )

        return true
    }

    /**
     * Renders the image to represent the dragged object for AWT.
     */
    private fun renderDragImage(
        size: Size,
        density: Density,
        layoutDirection: LayoutDirection,
        drawDragDecoration: DrawScope.() -> Unit
    ): Image {
        val imageBitmap = ImageBitmap(
            width = size.width.roundToInt(),
            height = size.height.roundToInt()
        )
        // This results in blurry text for some reason.
        val canvas = Canvas(imageBitmap)
        val canvasScope = CanvasDrawScope()
        canvasScope.draw(density, layoutDirection, canvas, size, drawDragDecoration)
        return PlatformAdaptations.dragImage(
            image = imageBitmap.toAwtImage(),
            density = density.density
        )
    }

    /**
     * Receives and processes events from the [DropTarget] installed in the root component.
     */
    private val dropTargetListener = object : DropTargetListener {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            // There's no drag-start event in AWT, so start in dragEnter, and stop in dragExit
            val accepted = scene.dropTarget.onEntered(DragAndDropEvent(dtde))
            if (!accepted) {
                dtde.rejectDrag()
            }
        }

        override fun dragExit(dte: DropTargetEvent) {
            scene.dropTarget.onExited(DragAndDropEvent(dte))
        }

        override fun dragOver(dtde: DropTargetDragEvent) {
            scene.dropTarget.onMoved(DragAndDropEvent(dtde))
        }

        override fun dropActionChanged(dtde: DropTargetDragEvent) {
            scene.dropTarget.onChanged(DragAndDropEvent(dtde))
        }

        override fun drop(dtde: DropTargetDropEvent) {
            val accepted = scene.dropTarget.onDrop(DragAndDropEvent(dtde))
            dtde.acceptDrop(dtde.dropAction)
            dtde.dropComplete(accepted)
        }

        private fun DragAndDropEvent(dragEvent: DropTargetDragEvent) = DragAndDropEvent(
            nativeEvent = dragEvent,
            action = DragAndDropTransferAction.fromAwtAction(dragEvent.dropAction),
            positionInRootImpl = dragEvent.location.toOffset()
        )

        private fun DragAndDropEvent(dropEvent: DropTargetDropEvent) = DragAndDropEvent(
            nativeEvent = dropEvent,
            action = DragAndDropTransferAction.fromAwtAction(dropEvent.dropAction),
            positionInRootImpl = dropEvent.location.toOffset()
        )

        private fun DragAndDropEvent(dropEvent: DropTargetEvent) = DragAndDropEvent(
            nativeEvent = dropEvent,
            action = null,
            positionInRootImpl = Offset.Zero
        )
    }

    /**
     * The [TransferHandler] installed as the root container's [JComponent.setTransferHandler] in
     * order to implement drop-source functionality.
     */
    val transferHandler = ComposeTransferHandler(rootContainer)

    /**
     * The AWT [DropTarget] installed as the root container's [JComponent.dropTarget] in order to
     * implement drop-target functionality.
     */
    val dropTarget = DropTarget(
        rootContainer,
        DnDConstants.ACTION_MOVE or DnDConstants.ACTION_COPY or DnDConstants.ACTION_LINK,
        dropTargetListener,
        true
    )
}

/**
 * The AWT [TransferHandler] we install in the root container in order to implement drag-source
 * functionality.
 */
internal class ComposeTransferHandler(private val rootContainer: JComponent) : TransferHandler() {

    private val scale: Float
        get() = rootContainer.density.density

    private var outgoingTransfer: OutgoingTransfer? = null

    fun startOutgoingTransfer(
        transferData: DragAndDropTransferData,
        dragImage: Image,
        dragDecorationOffset: Offset,
    ) {
        outgoingTransfer = OutgoingTransfer(
            transferData = transferData,
            dragImage = dragImage,
            dragImageOffset = PlatformAdaptations.dragImageOffset(dragDecorationOffset, scale)
        )

        val rootContainerLocation = rootContainer.locationOnScreen
        val mouseLocation = MouseInfo.getPointerInfo().location?.let {
            IntOffset(
                x = it.x - rootContainerLocation.x,
                y = it.y - rootContainerLocation.y
            )
        } ?: rootContainerLocation.let { IntOffset(it.x, it.y) }
        exportAsDrag(
            rootContainer,
            MouseEvent(
                rootContainer,
                MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(),
                0,
                mouseLocation.x,
                mouseLocation.y,
                0,
                false
            ),
            // This seems to be ignored, and the initial action is MOVE regardless
            DnDConstants.ACTION_MOVE
        )
    }

    override fun createTransferable(c: JComponent?): Transferable? {
        return (outgoingTransfer?.transferData?.transferable as? AwtDragAndDropTransferable)
            ?.toAwtTransferable()
    }

    override fun getSourceActions(c: JComponent?): Int {
        val actions = outgoingTransfer?.transferData?.supportedActions ?: emptyList()
        return actions.fold(
            initial = NONE,
            operation = { acc, action -> acc or action.awtAction },
        )
    }

    override fun getDragImage() = outgoingTransfer?.dragImage

    override fun getDragImageOffset() = outgoingTransfer?.dragImageOffset

    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        super.exportDone(source, data, action)

        val transferAction = DragAndDropTransferAction.fromAwtAction(action)
        outgoingTransfer?.transferData?.onTransferCompleted?.invoke(transferAction)
        outgoingTransfer = null
    }
}

private class OutgoingTransfer(
    val transferData: DragAndDropTransferData,
    val dragImage: Image,
    val dragImageOffset: Point
)

/**
 * The AWT drag-and-drop seems to have some differences between the various OSes. This
 * interface encapsulates the adaptations to these differences for each OS.
 */
private interface PlatformAdaptations {
    /**
     * Given a Compose offset and density, returns the AWT [Point] representing the offset of the
     * pointer inside the drag image we return from [TransferHandler.dragImage].
     */
    fun dragImageOffset(decorationOffset: Offset, density: Float): Point

    /**
     * Returns the image to represent the dragged object, given its rendering at the size specified
     * by the `decorationSize` argument passed to [DragAndDropManager.drag].
     */
    fun dragImage(image: BufferedImage, density: Float): Image

    /**
     * The adaptations for macOS.
     */
    private object MacOs : PlatformAdaptations {
        override fun dragImageOffset(decorationOffset: Offset, density: Float) = Point(
            -(decorationOffset.x / density).roundToInt(),
            -(decorationOffset.y / density).roundToInt()
        )

        override fun dragImage(image: BufferedImage, density: Float): Image {
            return image.getScaledInstance(
                (image.width / density).roundToInt(),
                (image.height / density).roundToInt(),
                Image.SCALE_SMOOTH
            )
        }
    }

    /**
     * The adaptations for Windows.
     */
    private object Windows : PlatformAdaptations {
        override fun dragImageOffset(decorationOffset: Offset, density: Float) =
            Point(decorationOffset.x.roundToInt(), decorationOffset.y.roundToInt())

        override fun dragImage(image: BufferedImage, density: Float) = image
    }

    /**
     * The adaptations for other OSes.
     */
    private object Other : PlatformAdaptations {

        override fun dragImageOffset(decorationOffset: Offset, density: Float) =
            Point(decorationOffset.x.roundToInt(), decorationOffset.y.roundToInt())

        override fun dragImage(image: BufferedImage, density: Float) = image
    }

    /**
     * The adaptations for the OS this application is running on.
     */
    companion object : PlatformAdaptations by when (DesktopPlatform.Current) {
        DesktopPlatform.Windows -> Windows
        DesktopPlatform.MacOS -> MacOs
        else -> Other
    }
}


