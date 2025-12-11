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

package androidx.compose.ui.draganddrop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformDragAndDropSource
import androidx.compose.ui.platform.toUIImage
import androidx.compose.ui.scene.ComposeSceneDragAndDropNode
import androidx.compose.ui.uikit.utils.CMPDragInteractionProxy
import androidx.compose.ui.uikit.utils.CMPDropInteractionProxy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.OverlayInputView
import kotlin.math.roundToInt
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIDropInteractionDelegateProtocol
import platform.UIKit.UIDropOperation
import platform.UIKit.UIDropOperationCopy
import platform.UIKit.UIDropOperationForbidden
import platform.UIKit.UIDropProposal
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIImageView
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIPreviewParameters
import platform.UIKit.UIPreviewTarget
import platform.UIKit.UITargetedDragPreview
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode
import platform.UIKit.addInteraction

/**
 * Context of a drag session initiated from Compose.
 */
private class DragSessionContext(
    val transferData: DragAndDropTransferData,
    val decorationSize: Size,
    val drawDragDecoration: DrawScope.() -> Unit
) {
    private var preview: UITargetedDragPreview? = null

    fun getCachedPreviewOrCreate(view: UIView, session: UIDragSessionProtocol): UITargetedDragPreview {
        return preview ?: createPreview(view, session).also {
            preview = it
        }
    }

    fun createPreview(view: UIView, session: UIDragSessionProtocol): UITargetedDragPreview {
        val window = checkNotNull(view.window) {
            "Can't generate UITargetedDragPreview for a view, detached from the window"
        }

        val density = Density(density = window.screen.scale.toFloat())

        val decorationCgRect = decorationSize
            .toRect()
            .toDpRect(density)
            .asCGRect()

        val decorationView = UIImageView(frame = decorationCgRect)

        val imageBitmap = ImageBitmap(
            width = decorationSize.width.roundToInt(),
            height = decorationSize.height.roundToInt(),
            hasAlpha = false
        )

        val canvas = Canvas(imageBitmap)
        val canvasScope = CanvasDrawScope()

        canvasScope.draw(density, LayoutDirection.Ltr, canvas, decorationSize) {
            drawRect(
                color = Color.Transparent,
                topLeft = Offset.Zero,
                size = decorationSize,
                alpha = 1f,
                style = Fill
            )

            drawDragDecoration()
        }

        imageBitmap
            .toUIImage()
            ?.let { uiImage ->
                decorationView.image = uiImage
                decorationView.setOpaque(false)
                decorationView.backgroundColor = UIColor.clearColor
                decorationView.contentMode = UIViewContentMode.UIViewContentModeScaleToFill
            }

        val parameters = UIPreviewParameters()
        val cornerRadius = decorationCgRect.useContents {
            minOf(size.width / 2.0, size.height  / 2.0, 4.0)
        }
        val path = UIBezierPath.bezierPathWithRoundedRect(decorationCgRect.useContents {
            CGRectMake(0.0, 0.0, size.width, size.height)
        }, cornerRadius = cornerRadius)
        parameters.backgroundColor = UIColor.clearColor
        parameters.visiblePath = path

        val preview = UITargetedDragPreview(
            view = decorationView,
            parameters = parameters,
            target = UIPreviewTarget(
                container = view,
                center = session.locationInView(view),
                transform = CGAffineTransformIdentity.readValue()
            )
        )
        return preview
    }
}

/**
 * Context of a drop session, tracked by [UIDropInteraction] managed by Compose.
 */
internal class DropSessionContext(
    val view: UIView,
    val session: UIDropSessionProtocol
) {
    val event = DragAndDropEvent(this)
}

/**
 * The [DragAndDropManager] implementation
 * for UIKit.
 *
 * This class is responsible for managing the drag and drop interactions on the UIKit platform.
 * It is responsible for setting up the drag and drop interactions on the [view] and bridging the
 * context between iOS and Compose.
 */
internal class UIKitDragAndDropManager(
    private val view: OverlayInputView,
    private val getComposeRootDragAndDropNode: () -> ComposeSceneDragAndDropNode
) : PlatformDragAndDropManager {
    private val rootNode: ComposeSceneDragAndDropNode
        get() = getComposeRootDragAndDropNode()

    /**
     * Context for an ongoing drag session initiated from Compose.
     */
    private var dragSessionContext: DragSessionContext? = null

    /**
     * Context from an ongoing drop session could possibly be initiated from outside Compose.
     */
    private var dropSessionContext: DropSessionContext? = null

    /**
     * The [CMPDragInteractionProxy] that handles the serves as [UIDragInteractionDelegateProtocol] for the
     * interaction added to [view]
     */
    private val dragInteractionProxy = object : CMPDragInteractionProxy() {
        override fun isSessionRestrictedToDraggingApplication(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): Boolean {
            return false
        }

        override fun itemsForBeginningSession(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): List<*> {
            val startTransferScope = object : PlatformDragAndDropSource.StartTransferScope {
                override fun startDragAndDropTransfer(
                    transferData: DragAndDropTransferData,
                    decorationSize: Size,
                    drawDragDecoration: DrawScope.() -> Unit
                ): Boolean {
                    dragSessionContext = DragSessionContext(
                        transferData = transferData,
                        decorationSize = decorationSize,
                        drawDragDecoration = drawDragDecoration
                    )
                    return true
                }
            }

            val density = Density(density = view.window?.screen?.scale?.toFloat() ?: 1f)
            val offset = session
                .locationInView(view)
                .useContents { asDpOffset() }
                .toOffset(density)

            with(rootNode) {
                startTransferScope.startDragAndDropTransfer(
                    offset = offset,
                    isTransferStarted = { dragSessionContext != null }
                )
            }

            return dragSessionContext?.transferData?.items ?: emptyList<UIDragItem>()
        }

        override fun previewForLiftingItemInSession(
            session: UIDragSessionProtocol,
            item: UIDragItem,
            interaction: UIDragInteraction
        ): UITargetedDragPreview? = withDragSessionContext {
            getCachedPreviewOrCreate(view, session)
        }

        override fun doesSessionAllowMoveOperation(
            session: UIDragSessionProtocol, interaction: UIDragInteraction
        ): Boolean {
            return false
        }

        override fun sessionDidEndWithOperation(
            session: UIDragSessionProtocol,
            interaction: UIDragInteraction,
            operation: UIDropOperation
        ) {
            dragSessionContext = null
        }
    }

    /**
     * A proxy that bridges the [DragAndDropTarget] with a subclass of [CMPDropInteractionProxy]
     * serving as a Kotlin-friendly facade for methods of [UIDropInteractionDelegateProtocol]
     */
    private val dropInteractionProxy = object : CMPDropInteractionProxy() {
        override fun canHandleSession(
            session: UIDropSessionProtocol, interaction: UIDropInteraction
        ): Boolean {
            // Can't handle multiple drop sessions at the same time
            if (dropSessionContext != null) return false

            val context = DropSessionContext(view, session)
            val accepts = rootNode.acceptDragAndDropTransfer(context.event)
            if (accepts) {
                dropSessionContext = context
            }

            return accepts
        }


        override fun performDropFromSession(
            session: UIDropSessionProtocol, interaction: UIDropInteraction
        ) {
            withDropSessionContext {
                rootNode.onDrop(event)
            }
        }

        override fun proposalForSessionUpdate(
            session: UIDropSessionProtocol, interaction: UIDropInteraction
        ): UIDropProposal = withDropSessionContext {
            rootNode.onMoved(event)
            if (rootNode.hasEligibleDropTarget) {
                UIDropProposal(UIDropOperationCopy)
            } else {
                UIDropProposal(UIDropOperationForbidden)
            }
        } ?: UIDropProposal(UIDropOperationForbidden)

        override fun sessionDidEnter(
            session: UIDropSessionProtocol,
            interaction: UIDropInteraction
        ) {
            withDropSessionContext {
                rootNode.onEntered(event)
            }
        }

        override fun sessionDidExit(
            session: UIDropSessionProtocol,
            interaction: UIDropInteraction
        ) {
            withDropSessionContext {
                rootNode.onExited(event)
            }
        }

        override fun sessionDidEnd(session: UIDropSessionProtocol, interaction: UIDropInteraction) {
            withDropSessionContext {
                rootNode.onEnded(event)
            }

            dropSessionContext = null
        }
    }

    init {
        view.addInteraction(UIDragInteraction(delegate = dragInteractionProxy))
        view.addInteraction(UIDropInteraction(delegate = dropInteractionProxy))

        // UIDragInteraction adds its own gesture recogniser that can cancel the gesture that is
        // responsive to touch handling. Ignore this gesture if the drag session is not started
        // or empty.
        view.canIgnoreDragGesture = { gestureRecognizer ->
            (gestureRecognizer is UILongPressGestureRecognizer &&
                gestureRecognizer.view == view &&
                dragSessionContext?.transferData?.items?.isNotEmpty() != true)
        }
    }

    private fun <R> withDragSessionContext(block: DragSessionContext.() -> R): R? =
        dragSessionContext?.block()

    private fun <R> withDropSessionContext(block: DropSessionContext.() -> R): R? =
        dropSessionContext?.block()
}