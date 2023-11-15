/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Matrix
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.graphics.CanvasBufferedRenderer.RenderResult.Companion.ERROR_UNKNOWN
import androidx.graphics.CanvasBufferedRenderer.RenderResult.Companion.SUCCESS
import androidx.graphics.lowlatency.BufferTransformHintResolver
import androidx.graphics.lowlatency.PreservedBufferContentsVerifier
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(Build.VERSION_CODES.Q)
internal class CanvasBufferedRendererV29(
    private val mWidth: Int,
    private val mHeight: Int,
    format: Int,
    usage: Long,
    private val mMaxBuffers: Int,
    forceRedrawContentsStrategy: Boolean = false
) : CanvasBufferedRenderer.Impl {

    private val mPreservedRenderStrategy = createPreservationStrategy(forceRedrawContentsStrategy)

    private val mImageReader = ImageReader.newInstance(
        mWidth,
        mHeight,
        format,
        // If the device does not support preserving contents when we are rendering to a single
        // buffer, use the fallback of leveraging 2 but redrawing the contents from the previous
        // frame into the next frame
        if (mMaxBuffers == 1) mPreservedRenderStrategy.maxImages else mMaxBuffers,
        usage
    )

    private val mRootRenderNode = RenderNode("rootNode").apply {
        setPosition(0, 0, mWidth, mHeight)
        clipToBounds = false
    }

    private var mContentRoot: RenderNode? = null

    private var mBufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
    private val mTransform = Matrix()

    private var mPreserveContents = false

    /**
     * Lock used to provide thread safe access to the underlying pool that maps between outstanding
     * HardwareBuffer instances and the Image it is associated with
     */
    private val mBufferLock = ReentrantLock()

    /**
     * Condition used to signal when an Image is available after it was previously released
     */
    private val mBufferSignal = mBufferLock.newCondition()

    /**
     * Mapping of [HardwareBuffer] instances to the corresponding [Image] they are associated with.
     * Because [ImageReader] allocates a new [Image] instance each time acquireNextImage is called,
     * we cannot rely on the fact that the [ImageReader] will cycle through the same [Image]
     * instances. So instead create a mapping of buffers to Images that will be added to and removed
     * on each render.
     */
    private val mAllocatedBuffers = HashMap<HardwareBuffer, Image>()

    private var mHardwareRenderer: HardwareRenderer? = HardwareRenderer().apply {
        // HardwareRenderer will preserve contents of the buffers if the isOpaque flag is true
        // otherwise it will clear contents across subsequent renders
        isOpaque = true
        setContentRoot(mRootRenderNode)
        setSurface(mImageReader.surface)
        start()
    }

    private fun closeBuffers() = mBufferLock.withLock {
        for (entry in mAllocatedBuffers) {
            entry.key.close() // HardwareBuffer
            entry.value.waitAndClose() // Image
        }
        mAllocatedBuffers.clear()
        mBufferSignal.signal()
    }

    override fun close() {
        closeBuffers()
        mImageReader.close()
        mHardwareRenderer?.let { renderer ->
            renderer.stop()
            renderer.destroy()
        }
        mHardwareRenderer = null
        mRootRenderNode.discardDisplayList()
    }

    override fun isClosed(): Boolean = mHardwareRenderer == null

    override fun draw(
        request: CanvasBufferedRenderer.RenderRequest,
        executor: Executor,
        callback: Consumer<CanvasBufferedRenderer.RenderResult>
    ) {
        val transform = request.transform
        val content = mContentRoot
        // If we are redrawing contents from the previous scene then we must re-record the drawing
        // drawing instructions to draw the updated bitmap
        val forceRedraw = request.preserveContents || mPreserveContents
        val shouldRedraw = !mRootRenderNode.hasDisplayList() || transform != mBufferTransform ||
            forceRedraw
        if (shouldRedraw && content != null) {
            recordContent(content, updateTransform(transform), request.preserveContents)
        }

        val renderer = mHardwareRenderer
        if (renderer != null && !isClosed()) {
            with(renderer) {
                var result = 0
                val renderRequest = createRenderRequest()
                    .setFrameCommitCallback(executor) {
                        acquireBuffer { buffer, fence ->
                            executor.execute {
                                mPreservedRenderStrategy.onRenderComplete(buffer, fence)
                                callback.accept(
                                    CanvasBufferedRenderer.RenderResult(
                                        buffer,
                                        fence,
                                        if (result != 0) ERROR_UNKNOWN else SUCCESS
                                    )
                                )
                                if (mMaxBuffers == 1) {
                                    releaseBuffer(buffer, fence)
                                }
                            }
                        }
                    }
                result = renderRequest.syncAndDraw()
            }
        } else {
            Log.v(TAG, "mHardwareRenderer is null")
        }
    }

    private fun updateTransform(transform: Int): Matrix {
        mBufferTransform = transform
        return BufferTransformHintResolver.configureTransformMatrix(
            mTransform,
            mWidth.toFloat(),
            mHeight.toFloat(),
            transform
        )
    }

    private fun recordContent(
        contentNode: RenderNode,
        transform: Matrix,
        preserveContents: Boolean
    ) {
        val canvas = mRootRenderNode.beginRecording()
        if (preserveContents) {
            mPreservedRenderStrategy.restoreContents(canvas)
        } else {
            canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
        }
        canvas.save()
        canvas.concat(transform)
        canvas.drawRenderNode(contentNode)
        canvas.restore()
        mRootRenderNode.endRecording()
        mPreserveContents = preserveContents
    }

    override fun setContentRoot(renderNode: RenderNode) {
        mContentRoot = renderNode
        mRootRenderNode.discardDisplayList()
    }

    override fun setLightSourceAlpha(ambientShadowAlpha: Float, spotShadowAlpha: Float) {
        mHardwareRenderer?.setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha)
    }

    /**
     * Acquires the next [Image] from the [ImageReader]. This method will block until the
     * number of outstanding [Image]s acquired is below the maximum number of buffers specified
     * by maxImages. This is because [ImageReader] will throw exceptions if an additional
     * [Image] is acquired beyond the maximum amount of buffers.
     */
    private inline fun acquireBuffer(block: (HardwareBuffer, SyncFenceCompat?) -> Unit) {
        mBufferLock.withLock {
            // Block until the number of outstanding Images is less than the maximum specified
            while (mAllocatedBuffers.size >= mImageReader.maxImages) {
                mBufferSignal.await()
            }
            val image = mImageReader.acquireNextImage()
            if (image != null) {
                // Be sure to call Image#getHardwareBuffer once as each call creates a new java object
                // and we are relying on referential equality to map the HardwareBuffer back to the
                // Image that it came from in order to close the Image when the buffer is released
                val buffer = image.hardwareBuffer
                if (buffer != null) {
                    // Insert a new mapping of hardware buffer to Image, closing any previous Image
                    // that maybe inserted for the hardware buffer
                    mAllocatedBuffers.put(buffer, image)?.waitAndClose()
                    val fence = image.getFenceCompat()
                    block(buffer, fence)
                    // If we are leveraging single buffered rendering, release the buffer right away
                    if (mImageReader.maxImages == 1) {
                        releaseBuffer(buffer, fence)
                    }
                } else {
                    // If we do not have a HardwareBuffer associated with this Image, close it
                    // and return null
                    image.waitAndClose()
                }
            }
        }
    }

    override fun releaseBuffer(hardwareBuffer: HardwareBuffer, syncFence: SyncFenceCompat?) {
        mBufferLock.withLock {
            // Remove the mapping of HardwareBuffer to Image and close the Image associated with
            // this HardwareBuffer instance
            val image = mAllocatedBuffers.remove(hardwareBuffer)
            if (image != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ImageVerificationHelper.setFence(image, syncFence)
                    image.close()
                } else {
                    image.waitAndClose()
                }
            }
            mBufferSignal.signal()
        }
    }

    override fun setLightSourceGeometry(
        lightX: Float,
        lightY: Float,
        lightZ: Float,
        lightRadius: Float
    ) {
        mHardwareRenderer?.setLightSourceGeometry(lightX, lightY, lightZ, lightRadius)
    }

    private fun Image.getFenceCompat(): SyncFenceCompat? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ImageVerificationHelper.getFence(this)
        } else {
            null
        }

    private fun Image.waitAndClose() {
        getFenceCompat()?.let { fence ->
            fence.awaitForever()
            fence.close()
        }
        close()
    }

    internal interface PreservedRenderStrategy {
        val maxImages: Int

        fun restoreContents(canvas: Canvas)

        fun onRenderComplete(
            hardwareBuffer: HardwareBuffer,
            fence: SyncFenceCompat?
        )
    }

    internal class SingleBufferedStrategy : PreservedRenderStrategy {
        override val maxImages = 1

        override fun restoreContents(canvas: Canvas) {
            // NO-OP HWUI preserves contents
        }

        override fun onRenderComplete(
            hardwareBuffer: HardwareBuffer,
            fence: SyncFenceCompat?
        ) {
            // NO-OP
        }
    }

    internal class RedrawBufferStrategy(
        // debugging flag used to simulate clearing of the canvas before
        // restoring the contents
        private val forceClear: Boolean = false
    ) : PreservedRenderStrategy {

        override val maxImages: Int = 2

        private var mHardwareBuffer: HardwareBuffer? = null
        private var mFence: SyncFenceCompat? = null

        override fun restoreContents(canvas: Canvas) {
            if (forceClear) {
                canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
            }
            mHardwareBuffer?.let { buffer ->
                mFence?.awaitForever()
                val bitmap = Bitmap.wrapHardwareBuffer(
                    buffer,
                    BufferedRendererImpl.DefaultColorSpace
                )
                if (bitmap != null) {
                    canvas.save()
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.restore()
                }
            }
        }

        override fun onRenderComplete(
            hardwareBuffer: HardwareBuffer,
            fence: SyncFenceCompat?
        ) {
            mHardwareBuffer = hardwareBuffer
            mFence = fence
        }
    }

    companion object {
        const val TAG = "BufferRendererV29"

        internal fun createPreservationStrategy(
            forceRedrawContentsStrategy: Boolean
        ): PreservedRenderStrategy {
            val supportsBufferPreservation = if (forceRedrawContentsStrategy) {
                false
            } else {
                val verifier = PreservedBufferContentsVerifier()
                val preserveContents = verifier.supportsPreservedRenderedContent()
                verifier.release()
                preserveContents
            }

            return if (supportsBufferPreservation) {
                Log.v(TAG, "Device supports persisted canvas optimizations")
                SingleBufferedStrategy()
            } else {
                Log.w(
                    TAG,
                    "Warning, device DOES NOT support persisted canvas optimizations."
                )
                RedrawBufferStrategy(forceRedrawContentsStrategy)
            }
        }
    }
}
