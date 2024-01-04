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

import android.annotation.SuppressLint
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareBufferRenderer
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.graphics.BufferedRendererImpl.Companion.DefaultColorSpace
import androidx.graphics.lowlatency.BufferTransformHintResolver
import androidx.graphics.lowlatency.BufferTransformer
import androidx.hardware.BufferPool
import androidx.hardware.SyncFenceCompat
import androidx.hardware.SyncFenceV33
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(Build.VERSION_CODES.Q)
internal fun defaultBufferTransformer(width: Int, height: Int) =
    BufferTransformer().apply {
        computeTransform(width, height, BufferTransformHintResolver.UNKNOWN_TRANSFORM)
    }

/**
 * Helper class used to draw RenderNode content into a HardwareBuffer instance. The contents of the
 * HardwareBuffer are not persisted across renders.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class MultiBufferedCanvasRenderer(
    width: Int,
    height: Int,
    bufferTransformer: BufferTransformer = defaultBufferTransformer(width, height),
    format: Int = PixelFormat.RGBA_8888,
    usage: Long = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
    maxImages: Int = 3
) {

    private val mBufferRenderer: BufferedRendererImpl =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            BufferedRendererV34(
                width,
                height,
                bufferTransformer,
                format,
                usage,
                maxImages
            )
        } else {
            BufferedRendererV29(
                bufferTransformer,
                format,
                usage,
                maxImages
            )
        }

    private val mPreserveContents = AtomicBoolean(false)

    var preserveContents: Boolean
        get() = mPreserveContents.get()
        set(value) { mPreserveContents.set(value) }

    private var mIsReleased = false

    inline fun record(block: (canvas: Canvas) -> Unit) {
        val canvas = mBufferRenderer.beginRecording()
        if (!preserveContents) {
            canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
        }
        block(canvas)
        mBufferRenderer.endRecording()
    }

    var colorSpace: ColorSpace
        get() = mBufferRenderer.colorSpace
        set(value) { mBufferRenderer.colorSpace = value }

    fun renderFrame(
        executor: Executor,
        bufferAvailable: (HardwareBuffer, SyncFenceCompat?) -> Unit
    ) {
        if (!mIsReleased) {
            mBufferRenderer.renderFrame(executor, bufferAvailable)
        }
    }

    /**
     * Release the buffer and close the corresponding [Image] instance to allow for the buffer
     * to be re-used on a subsequent render
     */
    fun releaseBuffer(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?) {
        mBufferRenderer.releaseBuffer(hardwareBuffer, fence)
    }

    fun release() {
        if (!mIsReleased) {
            mBufferRenderer.release()
            mIsReleased = true
        }
    }

    internal companion object {
        const val TAG = "MultiBufferRenderer"
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal interface BufferedRendererImpl {

    fun beginRecording(): Canvas

    fun endRecording()

    fun renderFrame(executor: Executor, bufferAvailable: (HardwareBuffer, SyncFenceCompat?) -> Unit)

    fun releaseBuffer(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?)

    var colorSpace: ColorSpace
        get() = DefaultColorSpace
        set(_) {}

    fun release()

    val isReleased: Boolean

    companion object {
        val DefaultColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private class BufferedRendererV34(
    val width: Int,
    val height: Int,
    val bufferTransformer: BufferTransformer,
    val format: Int,
    val usage: Long,
    maxImages: Int
) : BufferedRendererImpl {

    private val mRenderNode = RenderNode("node").apply {
        setPosition(
            0,
            0,
            this@BufferedRendererV34.width,
            this@BufferedRendererV34.height
        )
    }

    private val mColorSpaceRef = AtomicReference(DefaultColorSpace)

    private data class HardwareBufferProvider(
        private val buffer: HardwareBuffer,
        val renderer: HardwareBufferRenderer
    ) : BufferPool.BufferProvider {
        override val hardwareBuffer: HardwareBuffer
            get() = buffer

        override fun release() {
            renderer.close()
            buffer.close()
        }
    }

    private val mInverseTransform =
        bufferTransformer.invertBufferTransform(bufferTransformer.computedTransform)

    private val mPool = BufferPool<HardwareBufferProvider>(maxImages)

    private fun obtainBufferEntry(): HardwareBufferProvider =
        mPool.obtain {
            val hardwareBuffer = HardwareBuffer.create(
                bufferTransformer.glWidth,
                bufferTransformer.glHeight,
                format,
                1,
                usage
            )
            val renderer = HardwareBufferRenderer(hardwareBuffer).apply {
                setContentRoot(mRenderNode)
            }
            HardwareBufferProvider(hardwareBuffer, renderer)
        }

    override fun beginRecording(): Canvas = mRenderNode.beginRecording()

    override fun endRecording() {
        mRenderNode.endRecording()
    }

    override val isReleased: Boolean
        get() = mPool.isClosed

    override fun renderFrame(
        executor: Executor,
        bufferAvailable: (HardwareBuffer, SyncFenceCompat?) -> Unit
    ) {
        executor.execute {
            val entry = obtainBufferEntry()
            entry.renderer.obtainRenderRequest().apply {
                setColorSpace(colorSpace)
                if (mInverseTransform != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                    setBufferTransform(mInverseTransform)
                }
            }.draw(executor) { result ->
                bufferAvailable(entry.hardwareBuffer, SyncFenceCompat(result.fence))
            }
        }
    }

    override var colorSpace: ColorSpace
        get() = mColorSpaceRef.get()
        set(value) { mColorSpaceRef.set(value) }

    override fun releaseBuffer(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?) {
        mPool.release(hardwareBuffer, fence)
    }

    override fun release() {
        mPool.close()
        mRenderNode.discardDisplayList()
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private class BufferedRendererV29(
    bufferTransformer: BufferTransformer,
    format: Int,
    usage: Long,
    maxImages: Int
) : BufferedRendererImpl {

    private val mRenderNode = RenderNode("renderNode").apply {
        setPosition(
            0,
            0,
            bufferTransformer.glWidth,
            bufferTransformer.glHeight
        )
    }

    private val mTransform = android.graphics.Matrix().apply {
        bufferTransformer.configureMatrix(this)
    }

    // PixelFormat.RGBA_8888 should be accepted here but Android Studio flags as a warning
    @SuppressLint("WrongConstant")
    private val mImageReader = ImageReader.newInstance(
        bufferTransformer.glWidth,
        bufferTransformer.glHeight,
        format,
        maxImages,
        usage
    )
    private var mHardwareRenderer: HardwareRenderer? = HardwareRenderer().apply {
        // HardwareRenderer will preserve contents of the buffers if the isOpaque flag is true
        // otherwise it will clear contents across subsequent renders
        isOpaque = true
        setContentRoot(mRenderNode)
        setSurface(mImageReader.surface)
        start()
    }

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

    override val isReleased: Boolean
        get() = mHardwareRenderer == null

    private var mCanvas: Canvas? = null

    override fun beginRecording(): Canvas {
        val canvas = mRenderNode.beginRecording()
        canvas.save()
        canvas.setMatrix(mTransform)
        mCanvas = canvas
        return canvas
    }

    override fun endRecording() {
        mCanvas?.restore()
        mRenderNode.endRecording()
    }

    override fun renderFrame(
        executor: Executor,
        bufferAvailable: (HardwareBuffer, SyncFenceCompat?) -> Unit
    ) {
        val renderer = mHardwareRenderer
        if (renderer != null && !isReleased) {
            with(renderer) {
                createRenderRequest()
                    .setFrameCommitCallback(executor) {
                        acquireBuffer { buffer, fence ->
                            executor.execute {
                                bufferAvailable(buffer, fence)
                            }
                        }
                    }
                    .syncAndDraw()
            }
        } else {
            Log.v(TAG, "mHardwareRenderer is null")
        }
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

    /**
     * Release the buffer and close the corresponding [Image] instance to allow for the buffer
     * to be re-used on a subsequent render
     */
    override fun releaseBuffer(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?) {
        mBufferLock.withLock {
            // Remove the mapping of HardwareBuffer to Image and close the Image associated with
            // this HardwareBuffer instance
            val image = mAllocatedBuffers.remove(hardwareBuffer)
            if (image != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ImageVerificationHelper.setFence(image, fence)
                    image.close()
                } else {
                    image.waitAndClose()
                }
            }
            mBufferSignal.signal()
        }
    }

    private fun closeBuffers() = mBufferLock.withLock {
        for (entry in mAllocatedBuffers) {
            entry.key.close() // HardwareBuffer
            entry.value.waitAndClose() // Image
        }
        mAllocatedBuffers.clear()
        mBufferSignal.signal()
    }

    override fun release() {
        closeBuffers()
        mImageReader.close()
        mHardwareRenderer?.let { renderer ->
            renderer.stop()
            renderer.destroy()
        }
        mHardwareRenderer = null
        mRenderNode.discardDisplayList()
    }

    private companion object {
        const val TAG = "BufferedRendererV29"
    }
}

/**
 * Helper class to avoid class verification failures
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class ImageVerificationHelper private constructor() {
    companion object {

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun getFence(image: Image): SyncFenceCompat = SyncFenceCompat(image.fence)

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun setFence(image: Image, fence: SyncFenceCompat?) {
            if (fence != null && fence.mImpl is SyncFenceV33) {
                image.fence = fence.mImpl.mSyncFence
            }
        }
    }
}
