package androidx.compose.ui.draganddrop

import androidx.compose.ui.events.EventTargetListener
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformDragAndDropSource
import androidx.compose.ui.scene.ComposeSceneDragAndDropNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node

internal abstract class WebDragAndDropManager(private val rootNode: Node, eventListener: EventTargetListener, globalEventsListener: EventTargetListener, private val density: Density) :
    PlatformDragAndDropManager {
    override val isRequestDragAndDropTransferRequired: Boolean
        get() = false

    abstract val rootDragAndDropNode: ComposeSceneDragAndDropNode

    private var startTransferScope: InternalStartTransferScope? = null

    init {
        initEvents(eventListener, globalEventsListener)
    }

    private fun DragEvent.setAsDragImage(ghostImage: HTMLElement) {
        with (ghostImage.style) {
            position = "absolute"

            top = "0"
            left = "0"


            setProperty("pointer-events", "none")
            setProperty("z-index", "-1")
        }

        // non-image elements passed to setDragImage should be present in the document
        // the only browser the only browser not burdened with this limitation is Firefox
        rootNode.appendChild(ghostImage)

        dataTransfer?.setDragImage(ghostImage, 0, 0)

        // After browser made a snapshot we can safely remove ghostImage from document
        // But it should be done in a different frame
        window.requestAnimationFrame {
            ghostImage.remove()
        }
    }

    private fun InternalStartTransferScope.startTransfer(dragEvent: DragEvent): Boolean {
        with (rootDragAndDropNode) {
            startDragAndDropTransfer(dragEvent.offset) {
                isActive()
            }

            if (isActive()) {
                return acceptTransfer(dragEvent)
            }
        }

        dragEvent.preventDefault()
        return false
    }


    private fun acceptTransfer(dragEvent: DragEvent): Boolean {
        var acceptedTransfer = false

        with (rootDragAndDropNode) {
            val evt = DragAndDropEvent(dragEvent.offset, transferData = DragAndDropTransferData(dragEvent.dataTransfer))
            acceptedTransfer = acceptDragAndDropTransfer(evt)

            if (acceptedTransfer) {
                onStarted(evt)
                onEntered(evt)
            }
        }

        return acceptedTransfer
    }

    private fun initEvents(eventListener: EventTargetListener, globalEventsListener: EventTargetListener) {
        var previousDragEventIsStart = false

        eventListener.addDisposableEvent("dragstart") { event ->
            // Both internal (starting from within the application)
            // and external (triggered by dragging something for the outer world)
            // trigger the "dragenter" event, but we cannot set drag image anywhere apart dragstart
            previousDragEventIsStart = true
            event as DragEvent

            val scope = InternalStartTransferScope(density)

            if (scope.startTransfer(event)) {
                // without setting any kind of data in data transfer Safari on iOS won't proceed with drag action
                // luckily, the data is mutable and we can reset it in dragAndDropSource
                // see https://youtrack.jetbrains.com/issue/CMP-7292/Drag-and-Drop-is-not-working-in-mobile-browsers
                event.dataTransfer?.setData("text/plain", "")
                scope.ghostImage?.let { ghostImage ->
                    event.setAsDragImage(ghostImage)
                }
                startTransferScope = scope
            }
        }

        eventListener.addDisposableEvent("dragenter") { event ->
            // We have to ignore dragenter if last drag event was "dragstart"
            if (!previousDragEventIsStart) {
                event as DragEvent
                acceptTransfer(event)
            }
            previousDragEventIsStart = false
        }

        eventListener.addDisposableEvent("dragover") { event ->
            // This event always should be prevent-defaulted whenever we rely on the "drop" event
            // see https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/drop_event
            event.preventDefault()
            event as DragEvent
            rootDragAndDropNode.onMoved(DragAndDropEvent(event.offset, null))
        }

        eventListener.addDisposableEvent("drop") { event ->
            event.preventDefault()
            event as DragEvent

            val dragAndDropEvent = DragAndDropEvent(
                event.offset,
            startTransferScope?.transferData ?: DragAndDropTransferData(event.dataTransfer)
            )

            rootDragAndDropNode.onDrop(dragAndDropEvent)
            rootDragAndDropNode.onEnded(dragAndDropEvent)

            startTransferScope = null
        }

        globalEventsListener.addDisposableEvent("dragover") { event ->
            event as DragEvent
            event.preventDefault()
            event.dataTransfer?.dropEffect = "move"
        }
    }

    private val DragEvent.offset get() = Offset(
        x = offsetX.toFloat(),
        y = offsetY.toFloat()
    ) * density.density
}

@Suppress("UNUSED_PARAMETER")
private fun setMethodImplForUint8ClampedArray(obj: Uint8ClampedArray, index: Int, value: Int) { js("obj[index] = value;") }
private operator fun Uint8ClampedArray.set(index: Int, value: Int) = setMethodImplForUint8ClampedArray(this, index, value)

private fun IntArray.toUint8ClampedArray(): Uint8ClampedArray {
    val uint8ClampedArray = Uint8ClampedArray(size * 4)

    forEachIndexed { index, intValue ->
        val offset = index * 4

        // red
        uint8ClampedArray[offset] = (intValue shr 16) and 0xFF

        // green
        uint8ClampedArray[offset + 1] = (intValue shr 8) and 0xFF

        // blue
        uint8ClampedArray[offset + 2] = intValue and 0xFF

        // alpha
        uint8ClampedArray[offset + 3] = (intValue shr 24) and 0xFF
    }

    return uint8ClampedArray
}

private class InternalStartTransferScope(
    private val density: Density
) : PlatformDragAndDropSource.StartTransferScope {
    /**
     * Context for an ongoing drag session initiated from Compose.
     */
    var ghostImage: HTMLCanvasElement? = null
    var transferData: DragAndDropTransferData? = null

    fun isActive(): Boolean = transferData != null

    private fun captureAsImageData(
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): ImageData {
        val imageBitmap = ImageBitmap(
            width = decorationSize.width.roundToInt(),
            height = decorationSize.height.roundToInt()
        )

        val canvas = Canvas(imageBitmap)
        val canvasScope = CanvasDrawScope()

        canvasScope.draw(density, LayoutDirection.Ltr, canvas, decorationSize, drawDragDecoration)

        val intArray = IntArray(imageBitmap.width * imageBitmap.height)
        imageBitmap.readPixels(intArray)

        val uint8ClampedArray = intArray.toUint8ClampedArray()

        return ImageData(uint8ClampedArray, imageBitmap.width, imageBitmap.height)
    }

    private fun ImageData.asHtmlCanvas(): HTMLCanvasElement {
        val canvasConverter = document.createElement("canvas") as HTMLCanvasElement

        canvasConverter.width = width
        canvasConverter.height = height

        val scale = density.density
        require(scale > 0f)

        val widthNormalized = (width / scale).toInt()
        val heightNormalized = (height / scale).toInt()

        canvasConverter.style.width = "${widthNormalized}px"
        canvasConverter.style.height = "${heightNormalized}px"

        val canvasConverterContext = canvasConverter.getContext("2d") as CanvasRenderingContext2D
        canvasConverterContext.putImageData(this, 0.0, 0.0)

        return canvasConverter
    }

    override fun startDragAndDropTransfer(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean {
        this.transferData = transferData

        val imageData = captureAsImageData(decorationSize, drawDragDecoration)
        ghostImage = imageData.asHtmlCanvas()

        return true
    }
}