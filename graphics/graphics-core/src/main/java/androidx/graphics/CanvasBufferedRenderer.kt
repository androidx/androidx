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

import android.graphics.ColorSpace
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import android.view.SurfaceControl
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlCompat.Companion.BufferTransform
import androidx.hardware.DefaultFlags
import androidx.hardware.DefaultNumBuffers
import androidx.hardware.HardwareBufferFormat
import androidx.hardware.HardwareBufferUsage
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Creates an instance of a hardware-accelerated renderer. This is used to render a scene built
 * from [RenderNode]s to an output [HardwareBuffer]. There can be as many
 * [CanvasBufferedRenderer] instances as desired.
 *
 * Resources & lifecycle
 *
 * All [CanvasBufferedRenderer] instances share a common render
 * thread. Therefore [CanvasBufferedRenderer] will share common resources and GPU utilization
 * with hardware accelerated rendering initiated by the UI thread of an application.
 * The render thread contains the GPU context & resources necessary to do GPU-accelerated
 * rendering. As such, the first [CanvasBufferedRenderer] created comes with the cost of also
 * creating the associated GPU contexts, however each incremental [CanvasBufferedRenderer]
 * thereafter is fairly cheap.
 *
 * This is useful in situations where a scene built with [RenderNode]
 * [SurfaceControlCompat.Transaction.setBuffer].
 *
 * [CanvasBufferedRenderer] can optionally persist contents before each draw invocation so
 * previous contents in the [HardwareBuffer] target will be preserved across renders. This is
 * determined by the argument provided to
 * [CanvasBufferedRenderer.RenderRequest.preserveContents] which is set to `false` by default.
*/
@RequiresApi(Build.VERSION_CODES.Q)
class CanvasBufferedRenderer internal constructor(
    width: Int,
    height: Int,
    private val mFormat: Int,
    private val mUsage: Long,
    private val mMaxBuffers: Int,
    useImpl: Int = DEFAULT_IMPL,
) : AutoCloseable {

    private val mImpl: Impl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        useImpl == DEFAULT_IMPL
    ) {
        CanvasBufferedRendererV34(
            width,
            height,
            mFormat,
            mUsage,
            mMaxBuffers
        )
    } else {
        CanvasBufferedRendererV29(
            width,
            height,
            mFormat,
            mUsage,
            mMaxBuffers,
            useImpl
        )
    }

    private val mRenderRequest = RenderRequest()

    /**
     * Returns the number of buffers within the swap chain used for rendering with this
     * [CanvasBufferedRenderer]
     */
    val maxBuffers: Int
        get() = mMaxBuffers

    /**
     * Returns the [HardwareBufferFormat] of the buffers that are being rendered into by this
     * [CanvasBufferedRenderer]
     */
    @HardwareBufferFormat
    val bufferFormat: Int
        get() = mFormat

    /**
     * Returns the current usage flag hints of the buffers that are being rendered into by this
     * [CanvasBufferedRenderer]
     */
    @HardwareBufferUsage
    val usageFlags: Long
        get() = mUsage

    /**
     * Releases the resources associated with this [CanvasBufferedRenderer] instance.
     * **Note** this does not call [HardwareBuffer.close] on the provided [HardwareBuffer] instance.
     */
    override fun close() {
        mImpl.close()
    }

    /**
     * Returns if the [CanvasBufferedRenderer] has already been closed. That is
     * [CanvasBufferedRenderer.close] has been invoked.
     */
    val isClosed: Boolean
        get() = mImpl.isClosed()

    /**
     * Returns a [RenderRequest] that can be used to render into the provided HardwareBuffer.
     * This is used to synchronize the RenderNode content provided by [setContentRoot].
     */
    fun obtainRenderRequest(): RenderRequest {
        mRenderRequest.reset()
        return mRenderRequest
    }

    /**
     * Sets the content root to render. It is not necessary to call this whenever the content
     * recording changes. Any mutations to the [RenderNode] content, or any of the [RenderNode]s
     * contained within the content node, will be applied whenever a new [RenderRequest] is issued
     * via [obtainRenderRequest] and [RenderRequest.drawAsync].
     */
    fun setContentRoot(renderNode: RenderNode) {
        mImpl.setContentRoot(renderNode)
    }

    /**
     * Configures the ambient & spot shadow alphas. This is the alpha used when the shadow has
     * max alpha, and ramps down from the values provided to zero.
     *
     * These values are typically provided by the current theme, see R.attr.spotShadowAlpha and
     * R.attr.ambientShadowAlpha.
     *
     * This must be set at least once along with [setLightSourceGeometry] before shadows will work.
     */
    fun setLightSourceAlpha(
        ambientShadowAlpha: Float,
        spotShadowAlpha: Float,
    ) {
        mImpl.setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha)
    }

    /**
     * Sets the center of the light source. The light source point controls the directionality and
     * shape of shadows rendered by [RenderNode] Z & elevation.
     *
     * The light source should be setup both as part of initial configuration, and whenever the
     * window moves to ensure the light source stays anchored in display space instead of in
     * window space.
     *
     * This must be set at least once along with [setLightSourceAlpha] before shadows will work.
     */
    fun setLightSourceGeometry(
        lightX: Float,
        lightY: Float,
        lightZ: Float,
        lightRadius: Float
    ) {
        mImpl.setLightSourceGeometry(lightX, lightY, lightZ, lightRadius)
    }

    /**
     * Builder used to construct a [CanvasBufferedRenderer] instance.
     * @param width Width of the buffers created by the [CanvasBufferedRenderer] instance
     * @param height Height of the buffers created by the [CanvasBufferedRenderer] instance
     */
    class Builder(private val width: Int, private val height: Int) {

        private var mBufferFormat = HardwareBuffer.RGBA_8888
        private var mMaxBuffers = DefaultNumBuffers
        private var mUsageFlags = DefaultFlags
        private var mImpl = DEFAULT_IMPL

        init {
            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException(
                    "Invalid dimensions provided, width and height must be > 0. " +
                    "width: $width height: $height"
                )
            }
        }

        /**
         * Specify the buffer format of the underlying buffers being rendered into by the created
         * [CanvasBufferedRenderer]. The set of valid formats is implementation-specific.
         * The particular valid combinations for a given Android version and implementation should
         * be documented by that version.
         *
         * [HardwareBuffer.RGBA_8888] and [HardwareBuffer.RGBX_8888] are guaranteed to be supported.
         * However, consumers are recommended to query the desired [HardwareBuffer] configuration
         * using [HardwareBuffer.isSupported].
         *
         * @param format Pixel format of the buffers to be rendered into. The default is RGBA_8888.
         *
         * @return The builder instance
         */
        fun setBufferFormat(@HardwareBufferFormat format: Int): Builder {
            mBufferFormat = format
            return this
        }

        /**
         * Specify the maximum number of buffers used within the swap chain of the
         * [CanvasBufferedRenderer].
         * If 1 is specified, then the created [CanvasBufferedRenderer] is running in
         * "single buffer mode". In this case consumption of the buffer content would need to be
         * coordinated with the [SyncFenceCompat] returned by the callback of [RenderRequest.drawAsync].
         * @see CanvasBufferedRenderer.RenderRequest.drawAsync
         *
         * @param numBuffers The number of buffers within the swap chain to be consumed by the
         * created [CanvasBufferedRenderer]. This must be greater than zero. The default
         * number of buffers used is 3.
         *
         * @return The builder instance
         */
        fun setMaxBuffers(@IntRange(from = 1, to = 64) numBuffers: Int): Builder {
            require(numBuffers > 0) { "Must have at least 1 buffer" }
            mMaxBuffers = numBuffers
            return this
        }

        /**
         * Specify the usage flags to be configured on the underlying [HardwareBuffer] instances
         * created by the [CanvasBufferedRenderer].
         *
         * @param usageFlags Usage flags to be configured on the created [HardwareBuffer] instances
         * that the [CanvasBufferedRenderer] will render into. Must be one of
         * [HardwareBufferUsage]. Note that the provided flags here are combined with the following
         * mandatory default flags,
         * [HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE], [HardwareBuffer.USAGE_GPU_COLOR_OUTPUT] and
         * [HardwareBuffer.USAGE_COMPOSER_OVERLAY]
         *
         * @return The builder instance
         */
        fun setUsageFlags(@HardwareBufferUsage usageFlags: Long): Builder {
            mUsageFlags = usageFlags or DefaultFlags
            return this
        }

        /**
         * Internal test method use to verify alternative implementations of
         * HardwareBufferRenderer.Impl as well as internal algorithms for
         * persisting rendered content
         */
        internal fun setImpl(impl: Int): Builder {
            mImpl = impl
            return this
        }

        /**
         * Create the [CanvasBufferedRenderer] with the specified parameters on
         * this [Builder] instance.
         *
         * @return The newly created [CanvasBufferedRenderer] instance.
         */
        fun build(): CanvasBufferedRenderer {
            return CanvasBufferedRenderer(
                width,
                height,
                mBufferFormat,
                mUsageFlags,
                mMaxBuffers,
                mImpl
            )
        }
    }

    /**
     * Sets the parameters that can be used to control a render request for a
     * [CanvasBufferedRenderer]. This is not thread-safe and must not be held on to for longer
     * than a single request.
     */
    inner class RenderRequest internal constructor() {

        private var mColorSpace = DefaultColorSpace
        private var mTransform = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
        private var mPreserveContents = false

        internal val preserveContents: Boolean
            get() = mPreserveContents

        internal val colorSpace: ColorSpace
            get() = mColorSpace

        internal val transform: Int
            get() = mTransform

        internal fun reset() {
            mColorSpace = DefaultColorSpace
            mTransform = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
            mPreserveContents = false
        }

        /**
         * Syncs the [RenderNode] tree to the render thread and requests content to be drawn.
         * This [RenderRequest] instance should no longer be used after calling this method.
         * The system internally may reuse instances of [RenderRequest] to reduce allocation churn.
         *
         * @throws IllegalStateException if this method is invoked after the
         * [CanvasBufferedRenderer] has been closed.
         */
        fun drawAsync(executor: Executor, callback: Consumer<RenderResult>) {
            if (isClosed) {
                throw IllegalStateException("Attempt to draw after renderer has been closed")
            }
            mImpl.draw(this, executor, callback)
        }

        /**
         * Syncs the [RenderNode] tree to the render thread and requests content to be drawn
         * synchronously.
         * This [RenderRequest] instance should no longer be used after calling this method.
         * The system internally may reuse instances of [RenderRequest] to reduce allocation churn.
         *
         * @param waitForFence Optional flag to determine if the [SyncFenceCompat] is also waited
         * upon before returning as a convenience in order to enable callers to consume the
         * [HardwareBuffer] returned in the [RenderResult] immediately after this method returns.
         * Passing `false` here on Android T and below is a no-op as the graphics rendering pipeline
         * internally blocks on the fence before returning.
         */
        suspend fun draw(waitForFence: Boolean = true): RenderResult {
            check(!isClosed) { "Attempt to draw after renderer has been closed" }

            return suspendCancellableCoroutine { continuation ->
                drawAsync(Runnable::run) { result ->
                    if (waitForFence) {
                        result.fence?.apply {
                            awaitForever()
                            close()
                        }
                    }
                    continuation.resume(result)
                }
            }
        }

        /**
         * Specifies a transform to be applied before content is rendered. This is useful
         * for pre-rotating content for the current display orientation to increase performance
         * of displaying the associated buffer. This transformation will also adjust the light
         * source position for the specified rotation.
         * @see SurfaceControl.Transaction#setBufferTransform(SurfaceControl, int)
         *
         * @throws IllegalArgumentException if [bufferTransform] is not one of:
         * [SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180], or
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270]
         */
        fun setBufferTransform(@BufferTransform bufferTransform: Int): RenderRequest {
            val validTransform =
                bufferTransform == SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY ||
                    bufferTransform == SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 ||
                    bufferTransform == SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 ||
                    bufferTransform == SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
            if (validTransform) {
                mTransform = bufferTransform
            } else {
                throw IllegalArgumentException(
                    "Invalid transform provided, must be one of the " +
                        "SurfaceControlCompat.BufferTransform values received: " + bufferTransform
                )
            }
            return this
        }

        /**
         * Configures the color space which the content should be rendered in. This affects how the
         * framework will interpret the color at each pixel. The color space provided here must be
         * non-null, RGB based and leverage an ICC parametric curve. The min/max values of the
         * components should not reduce the numerical range compared to the previously assigned
         * color space. If left unspecified, the default color space of SRGB will be used.
         *
         * **NOTE** this method is only supported on Android U and above and is ignored on older
         * Android versions
         */
        fun setColorSpace(colorSpace: ColorSpace?): RenderRequest {
            mColorSpace = colorSpace ?: DefaultColorSpace
            return this
        }

        /**
         * Determines whether or not previous buffer contents will be persisted across render
         * requests. If false then contents are cleared before issuing drawing instructions,
         * otherwise contents will remain.
         *
         * If contents are known in advance to be completely opaque and cover all pixels within the
         * buffer, setting this flag to true will slightly improve performance as the clear
         * operation will be skipped.
         *
         * For low latency use cases (ex applications that support drawing with a stylus), setting
         * this value to true alongside single buffered rendering by configuring
         * [CanvasBufferedRenderer.Builder.setMaxBuffers] to 1 allows for reduced latency by allowing
         * consumers to only render the deltas across frames as the previous content would be
         * persisted.
         *
         * The default setting is false.
         */
        fun preserveContents(preserve: Boolean): RenderRequest {
            mPreserveContents = preserve
            return this
        }
    }

    /**
     * Releases the [HardwareBuffer] back into the allocation pool to be reused in subsequent
     * renders. The [HardwareBuffer] instance released here must be one that was originally obtained
     * from this [CanvasBufferedRenderer] instance. This method also takes in an optional
     * [SyncFenceCompat] instance that will be internally waited upon before re-using the buffer.
     * This is useful in conjunction with [SurfaceControlCompat.Transaction.setBuffer] where the
     * system will return a release fence that should be waited upon before the corresponding buffer
     * can be re-used.
     *
     * @param hardwareBuffer [HardwareBuffer] to return back to the allocation pool
     * @param fence Optional [SyncFenceCompat] that should be waited upon before the buffer is
     * reused.
     */
    @JvmOverloads
    fun releaseBuffer(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat? = null) {
        mImpl.releaseBuffer(hardwareBuffer, fence)
    }

    /**
     * Class that contains data regarding the result of the render request. Consumers are to wait
     * on the provided [SyncFenceCompat] if it is non null before consuming the [HardwareBuffer]
     * provided to as well as verify that the status returned by [RenderResult.status] returns
     * [RenderResult.SUCCESS].
     *
     * For example:
     * ```
     *  fun handleRenderResult(result: RenderResult) {
     *      // block on the fence if it is non-null
     *      result.fence?.let { fence ->
     *          fence.awaitForever()
     *          fence.close()
     *      }
     *      // consume contents of RenderResult.hardwareBuffer
     *  }
     * ```
     */
    class RenderResult(
        private val buffer: HardwareBuffer,
        private val mFence: SyncFenceCompat?,
        private val mStatus: Int
    ) {

        /**
         * [HardwareBuffer] that contains the result of the render request.
         * Consumers should be sure to block on the [SyncFenceCompat] instance
         * provided in [fence] if it is non-null before consuming the contents of this buffer.
         * If [fence] returns null then this [HardwareBuffer] can be consumed immediately.
         */
        val hardwareBuffer: HardwareBuffer
            get() = buffer

        /**
         * Optional fence that should be waited upon before consuming [hardwareBuffer].
         * On Android U and above, requests to render will return sooner and include this fence
         * as a way to signal that the result of the render request is reflected in the contents
         * of the buffer. This is done to reduce latency and provide opportunities for other systems
         * to block on the fence on the behalf of the application.
         * For example, [SurfaceControlCompat.Transaction.setBuffer] can be invoked with
         * [RenderResult.hardwareBuffer] and [RenderResult.fence] respectively without the
         * application having to explicitly block on this fence.
         * For older Android versions, the rendering pipeline will automatically block on this fence
         * and this value will return null.
         */
        val fence: SyncFenceCompat?
            get() = mFence

        /**
         * Status code for the [RenderResult] either [SUCCESS] if rendering completed or
         * [ERROR_UNKNOWN] if the rendering could not be completed.
         */
        val status: Int
            get() = mStatus

        companion object {
            /**
             * Render request was completed successfully
             */
            const val SUCCESS = 0

            /**
             * Render request failed with an unknown error
             */
            const val ERROR_UNKNOWN = 1
        }
    }

    internal interface Impl : AutoCloseable {

        override fun close()

        fun isClosed(): Boolean

        fun draw(
            request: RenderRequest,
            executor: Executor,
            callback: Consumer<RenderResult>
        )

        fun releaseBuffer(hardwareBuffer: HardwareBuffer, syncFence: SyncFenceCompat?)

        fun setContentRoot(renderNode: RenderNode)

        fun setLightSourceAlpha(
            ambientShadowAlpha: Float,
            spotShadowAlpha: Float,
        )

        fun setLightSourceGeometry(
            lightX: Float,
            lightY: Float,
            lightZ: Float,
            lightRadius: Float
        )
    }

    internal companion object {

        val DefaultColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)

        /**
         * Test flag to use the optimal implementation for the corresponding
         * Android platform version
         */
        internal const val DEFAULT_IMPL = 0

        /**
         * Test flag used to verify the V29 implementation that leverages the
         * redraw strategy on devices that do not persist contents of opaque renders
         */
        internal const val USE_V29_IMPL_WITH_REDRAW = 1

        /**
         * Test flag used to verify the V29 implementation that leverages the default
         * single buffered restoration strategy
         */
        internal const val USE_V29_IMPL_WITH_SINGLE_BUFFER = 2
    }
}
