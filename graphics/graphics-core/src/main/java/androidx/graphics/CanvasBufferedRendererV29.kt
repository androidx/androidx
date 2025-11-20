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
import android.graphics.Paint
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
import androidx.hardware.SyncFenceV33
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(Build.VERSION_CODES.Q)
internal class CanvasBufferedRendererV29(
    private val mWidth: Int,
    private val mHeight: Int,
    private val mFormat: Int,
    private val mUsage: Long,
    private val mMaxBuffers: Int,
    private val mPreservationConfig: Int,
) : CanvasBufferedRenderer.Impl {

    private var mPreservedRenderStrategy: PreservedRenderStrategy? = null

    private var mImageReader: ImageReader? = null

    private var mHardwareRenderer: HardwareRenderer? = null

    private fun createImageReader(preserveStrategy: PreservedRenderStrategy?): ImageReader =
        ImageReader.newInstance(
            mWidth,
            mHeight,
            mFormat,
            // If the device does not support preserving contents when we are rendering to a single
            // buffer, use the fallback of leveraging 2 but redrawing the contents from the previous
            // frame into the next frame
            if (mMaxBuffers == 1 && preserveStrategy != null) {
                preserveStrategy.maxImages
            } else {
                mMaxBuffers
            },
            mUsage,
        )

    private fun createHardwareRenderer(imageReader: ImageReader): HardwareRenderer =
        HardwareRenderer().apply {
            // HardwareRenderer may preserve contents of the buffers if the isOpaque flag is true
            // (see PreservedBufferContentsVerifier), otherwise it will clear contents across
            // subsequent renders.
            isOpaque = true
            setContentRoot(mRootRenderNode)
            setSurface(imageReader.surface)
            start()
        }

    private val mRootRenderNode =
        RenderNode("rootNode").apply {
            setPosition(0, 0, mWidth, mHeight)
            clipToBounds = false
        }

    private var mContentRoot: RenderNode? = null
    private var mLightX: Float = 0f
    private var mLightY: Float = 0f
    private var mLightZ: Float = 0f
    private var mLightRadius: Float = 0f

    private var mAmbientShadowAlpha: Float = 0f
    private var mSpotShadowAlpha: Float = 0f

    private var mBufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
    private val mTransform = Matrix()

    private var mPreserveContents = false

    /**
     * Lock used to provide thread safe access to the underlying pool that maps between outstanding
     * HardwareBuffer instances and the Image it is associated with
     */
    private val mBufferLock = ReentrantLock()

    /** Condition used to signal when an Image is available after it was previously released */
    private val mBufferSignal = mBufferLock.newCondition()

    /**
     * Mapping of [HardwareBuffer] instances to the corresponding [Image] they are associated with.
     * Because [ImageReader] allocates a new [Image] instance each time acquireNextImage is called,
     * we cannot rely on the fact that the [ImageReader] will cycle through the same [Image]
     * instances. So instead create a mapping of buffers to Images that will be added to and removed
     * on each render.
     */
    private val mAllocatedBuffers = HashMap<HardwareBuffer, Image>()

    private fun closeBuffers() =
        mBufferLock.withLock {
            for (entry in mAllocatedBuffers) {
                entry.key.close() // HardwareBuffer
                entry.value.waitAndClose() // Image
            }
            mAllocatedBuffers.clear()
            mBufferSignal.signal()
            mImageReader?.close()
            mImageReader = null
            mHardwareRenderer?.let { renderer ->
                renderer.stop()
                renderer.destroy()
            }
            mHardwareRenderer = null
        }

    private val mIsReleased = AtomicBoolean(false)

    override fun close() {
        closeBuffers()
        mRootRenderNode.discardDisplayList()
        mIsReleased.set(true)
    }

    override fun isClosed(): Boolean = mIsReleased.get()

    override fun draw(
        request: CanvasBufferedRenderer.RenderRequest,
        executor: Executor,
        callback: Consumer<CanvasBufferedRenderer.RenderResult>,
    ) {
        val transform = request.transform
        val content = mContentRoot
        // If we are redrawing contents from the previous scene then we must re-record the drawing
        // drawing instructions to draw the updated bitmap
        val forceRedraw = request.preserveContents || mPreserveContents
        val shouldRedraw =
            !mRootRenderNode.hasDisplayList() || transform != mBufferTransform || forceRedraw
        if (shouldRedraw && content != null) {
            recordContent(content, updateTransform(transform), request.preserveContents)
        }

        val lightX = mLightX
        val lightY = mLightY
        val lightZ = mLightZ
        val lightRadius = mLightRadius
        val ambientShadowAlpha = mAmbientShadowAlpha
        val spotShadowAlpha = mSpotShadowAlpha
        val preserveContents = request.preserveContents
        executor.execute {
            if (!isClosed()) {
                mBufferLock.withLock {
                    var preservedRenderStrategy = mPreservedRenderStrategy
                    if (preserveContents && mMaxBuffers == 1 && preservedRenderStrategy == null) {
                        closeBuffers()
                        preservedRenderStrategy = createPreservationStrategy(mPreservationConfig)
                        mPreservedRenderStrategy = preservedRenderStrategy
                    }
                    val renderer =
                        obtainHardwareRenderer(obtainImageReader(preservedRenderStrategy))
                    renderer.apply {
                        setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha)
                        setLightSourceGeometry(lightX, lightY, lightZ, lightRadius)
                    }
                    dispatchRender(executor, renderer, preservedRenderStrategy, callback)
                }
            }
        }
    }

    private fun obtainImageReader(preserveStrategy: PreservedRenderStrategy?): ImageReader =
        mImageReader ?: createImageReader(preserveStrategy).also { mImageReader = it }

    private fun obtainHardwareRenderer(imageReader: ImageReader): HardwareRenderer =
        mHardwareRenderer ?: createHardwareRenderer(imageReader).also { mHardwareRenderer = it }

    private fun dispatchRender(
        executor: Executor,
        renderer: HardwareRenderer,
        preservedRenderStrategy: PreservedRenderStrategy?,
        callback: Consumer<CanvasBufferedRenderer.RenderResult>,
    ) {
        with(renderer) {
            var result = 0
            val renderRequest =
                createRenderRequest().setFrameCommitCallback(executor) {
                    acquireBuffer { buffer, fence ->
                        preservedRenderStrategy?.onRenderComplete(buffer, fence)
                        callback.accept(
                            CanvasBufferedRenderer.RenderResult(
                                buffer,
                                fence,
                                if (isSuccess(result)) SUCCESS else ERROR_UNKNOWN,
                            )
                        )
                        if (mMaxBuffers == 1) {
                            releaseBuffer(buffer, fence)
                        }
                    }
                }
            result = renderRequest.syncAndDraw()
        }
    }

    /**
     * Helper method to determine if [HardwareRenderer.FrameRenderRequest.syncAndDraw] was
     * successful. In this case we wait for the next buffer even if we miss the vsync.
     */
    private fun isSuccess(result: Int) =
        result == HardwareRenderer.SYNC_OK || result == HardwareRenderer.SYNC_FRAME_DROPPED

    private fun updateTransform(transform: Int): Matrix {
        mBufferTransform = transform
        return BufferTransformHintResolver.configureTransformMatrix(
            mTransform,
            mWidth.toFloat(),
            mHeight.toFloat(),
            transform,
        )
    }

    private fun recordContent(
        contentNode: RenderNode,
        transform: Matrix,
        preserveContents: Boolean,
    ) {
        val canvas = mRootRenderNode.beginRecording()
        if (preserveContents) {
            mBufferLock.withLock { mPreservedRenderStrategy?.restoreContents(canvas) }
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
        mAmbientShadowAlpha = ambientShadowAlpha
        mSpotShadowAlpha = spotShadowAlpha
    }

    /**
     * Acquires the next [Image] from the [ImageReader]. This method will block until the number of
     * outstanding [Image]s acquired is below the maximum number of buffers specified by maxImages.
     * This is because [ImageReader] will throw exceptions if an additional [Image] is acquired
     * beyond the maximum amount of buffers.
     */
    private inline fun acquireBuffer(block: (HardwareBuffer, SyncFenceCompat?) -> Unit) {
        mBufferLock.withLock {
            // Block until the number of outstanding Images is less than the maximum specified
            val reader = mImageReader ?: return
            while (mAllocatedBuffers.size >= reader.maxImages) {
                mBufferSignal.await()
            }

            val image = reader.acquireNextImage()
            if (image != null) {
                // Be sure to call Image#getHardwareBuffer once as each call creates a new java
                // object
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
                    if (reader.maxImages == 1) {
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
        lightRadius: Float,
    ) {
        mLightX = lightX
        mLightY = lightY
        mLightZ = lightZ
        mLightRadius = lightRadius
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

        fun onRenderComplete(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?)
    }

    internal class SingleBufferedStrategy : PreservedRenderStrategy {
        override val maxImages = 1

        override fun restoreContents(canvas: Canvas) {
            // NO-OP HWUI preserves contents
        }

        override fun onRenderComplete(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?) {
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

        /**
         * Used for a call to [Canvas.drawBitmap] to overwrite the contents of one [HardwareBuffer]
         * with another. This operation should stay as simple as possible to avoid complications.
         */
        private val drawBitmapPaint =
            Paint().apply {
                blendMode = BlendMode.SRC

                // No need for AA. The zero-arg Paint constructor enables this by default on API 31
                // and above.
                isAntiAlias = false

                // Since we know this is always an unscaled blit, avoid any risk of subpixel
                // alignment causing filtering to slightly blur the re-rendered content.
                // The zero-arg Paint constructor enables this by default on API 29 and above.
                isFilterBitmap = false
            }

        override fun restoreContents(canvas: Canvas) {
            if (forceClear) {
                canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
            }
            mHardwareBuffer?.let { buffer ->
                mFence?.awaitForever()
                val bitmap =
                    Bitmap.wrapHardwareBuffer(buffer, CanvasBufferedRenderer.DefaultColorSpace)
                if (bitmap != null) {
                    canvas.save()
                    // Use blendMode=SRC to copy over every pixel from the old buffer, in case the
                    // newly obtained buffer was instantiated with garbage. If the
                    // RedrawBufferStrategy is needed, meaning the buffer contents are not preserved
                    // across renders, don't just assume that a fresh buffer will be cleared to all
                    // transparent pixels.
                    canvas.drawBitmap(bitmap, 0f, 0f, drawBitmapPaint)
                    canvas.restore()
                }
            }
        }

        override fun onRenderComplete(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?) {
            mHardwareBuffer = hardwareBuffer
            mFence = fence
        }
    }

    companion object {
        const val TAG = "BufferRendererV29"

        private val verifiedPreservation = AtomicBoolean(false)
        private val supportsPreservation = AtomicBoolean(false)

        internal fun createPreservationStrategy(
            preservationStrategy: Int
        ): PreservedRenderStrategy =
            when (preservationStrategy) {
                CanvasBufferedRenderer.USE_V29_IMPL_WITH_SINGLE_BUFFER -> {
                    Log.v(TAG, "Explicit usage of single buffered preservation strategy")
                    SingleBufferedStrategy()
                }
                CanvasBufferedRenderer.USE_V29_IMPL_WITH_REDRAW -> {
                    Log.v(
                        TAG,
                        "Explicit usage of double buffered redraw strategy " + "with force clear",
                    )
                    RedrawBufferStrategy(true)
                }
                else -> {
                    if (!verifiedPreservation.getAndSet(true)) {
                        val verifier = PreservedBufferContentsVerifier()
                        supportsPreservation.set(verifier.supportsPreservedRenderedContent())
                        verifier.release()
                    }

                    if (supportsPreservation.get()) {
                        Log.v(TAG, "Device supports persisted canvas optimizations")
                        SingleBufferedStrategy()
                    } else {
                        Log.w(
                            TAG,
                            "Warning, device DOES NOT support persisted canvas optimizations.",
                        )
                        RedrawBufferStrategy(false)
                    }
                }
            }
    }
}

/** Helper class to avoid class verification failures */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class ImageVerificationHelper private constructor() {
    companion object {

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun getFence(image: Image): SyncFenceCompat = SyncFenceCompat(image.fence)

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun setFence(image: Image, fence: SyncFenceCompat?) {
            if (fence != null && fence.mImpl is SyncFenceV33) {
                image.fence = fence.mImpl.mSyncFence
            }
        }
    }
}
