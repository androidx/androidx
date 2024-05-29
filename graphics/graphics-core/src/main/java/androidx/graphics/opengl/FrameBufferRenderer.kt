/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.opengl

import android.annotation.SuppressLint
import android.hardware.HardwareBuffer
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.hardware.SyncFenceCompat
import androidx.opengl.EGLExt
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_NATIVE_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_KHR_FENCE_SYNC
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [GLRenderer.RenderCallback] implementation that renders content into a frame buffer object backed
 * by a [HardwareBuffer] object
 *
 * @param frameBufferRendererCallbacks Callbacks to provide a [FrameBuffer] instance to render into,
 *   draw method to render into the [FrameBuffer] as well as a subsequent callback to consume the
 *   contents of the [FrameBuffer]
 * @param syncStrategy [SyncStrategy] used to determine when a fence is to be created to gate on
 *   consumption of the [FrameBuffer] instance. This determines if a [SyncFenceCompat] instance is
 *   provided in the [RenderCallback.onDrawComplete] depending on the use case. For example for
 *   front buffered rendering scenarios, it is possible that no [SyncFenceCompat] is provided in
 *   order to reduce latency within the rendering pipeline.
 *
 * This API can be used to render content into a [HardwareBuffer] directly and convert that to a
 * bitmap with the following code snippet:
 * ```
 * val glRenderer = GLRenderer().apply { start() }
 * val callbacks = object : FrameBufferRenderer.RenderCallback {
 *
 *   override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer =
 *      FrameBuffer(
 *          egl,
 *          HardwareBuffer.create(
 *              width,
 *              height,
 *              HardwareBuffer.RGBA_8888,
 *              1,
 *              HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
 *          )
 *      )
 *
 *   override fun onDraw(eglManager: EGLManager) {
 *       // GL code
 *   }
 *
 *   override fun onDrawComplete(frameBuffer: FrameBuffer, syncFenceCompat: SyncFenceCompat?) {
 *       syncFenceCompat?.awaitForever()
 *       val bitmap = Bitmap.wrapHardwareBuffer(frameBuffer.hardwareBuffer,
 *              ColorSpace.get(ColorSpace.Named.LINEAR_SRGB))
 *       // bitmap operations
 *   }
 * }
 *
 * glRenderer.createRenderTarget(width,height, FrameBufferRenderer(callbacks)).requestRender()
 * ```
 */
@RequiresApi(Build.VERSION_CODES.O)
class FrameBufferRenderer(
    private val frameBufferRendererCallbacks: RenderCallback,
    @SuppressLint("ListenerLast") private val syncStrategy: SyncStrategy = SyncStrategy.ALWAYS
) : GLRenderer.RenderCallback {

    private val mClear = AtomicBoolean(false)

    override fun onSurfaceCreated(
        spec: EGLSpec,
        config: EGLConfig,
        surface: Surface,
        width: Int,
        height: Int
    ): EGLSurface? = null

    fun clear() {
        mClear.set(true)
    }

    override fun onDrawFrame(eglManager: EGLManager) {
        val egl = eglManager.eglSpec
        val buffer = frameBufferRendererCallbacks.obtainFrameBuffer(egl)
        var syncFenceCompat: SyncFenceCompat? = null
        try {
            buffer.makeCurrent()
            if (mClear.getAndSet(false)) {
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            } else {
                frameBufferRendererCallbacks.onDraw(eglManager)
            }

            syncFenceCompat =
                if (eglManager.supportsNativeAndroidFence()) {
                    syncStrategy.createSyncFence(egl)
                } else if (eglManager.isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                    // In this case the device only supports EGL sync objects but not creation
                    // of native SyncFence objects from an EGLSync.
                    // This usually occurs in emulator/cuttlefish instances as well as ChromeOS
                    // devices
                    // running ARC++. In this case fallback onto creating a sync object and waiting
                    // on it instead.
                    // TODO b/256217036 block on another thread instead of waiting here
                    val syncKhr = egl.eglCreateSyncKHR(EGLExt.EGL_SYNC_FENCE_KHR, null)
                    if (syncKhr != null) {
                        GLES20.glFlush()
                        val status =
                            egl.eglClientWaitSyncKHR(
                                syncKhr,
                                EGLExt.EGL_SYNC_FLUSH_COMMANDS_BIT_KHR,
                                EGLExt.EGL_FOREVER_KHR
                            )
                        if (status != EGLExt.EGL_CONDITION_SATISFIED_KHR) {
                            Log.w(TAG, "warning waiting on sync object: $status")
                        }
                    } else {
                        Log.w(TAG, "Unable to create EGLSync")
                        GLES20.glFinish()
                    }
                    null
                } else {
                    Log.w(TAG, "Device does not support creation of any fences")
                    GLES20.glFinish()
                    null
                }
        } catch (exception: Exception) {
            Log.w(TAG, "Error attempting to render to frame buffer: ${exception.message}")
        } finally {
            // At this point the HardwareBuffer has the contents of the GL rendering
            // Create a surface Control transaction to dispatch this request
            frameBufferRendererCallbacks.onDrawComplete(buffer, syncFenceCompat)
        }
    }

    private fun EGLManager.supportsNativeAndroidFence(): Boolean =
        isExtensionSupported(EGL_KHR_FENCE_SYNC) &&
            isExtensionSupported(EGL_ANDROID_NATIVE_FENCE_SYNC)

    /** Callbacks invoked to render content leveraging a [FrameBufferRenderer] */
    interface RenderCallback {

        /**
         * Obtain a [FrameBuffer] to render content into. The [FrameBuffer] obtained here is
         * expected to be managed by the consumer of [FrameBufferRenderer]. That is implementations
         * of this API are expected to be maintaining a reference to the returned [FrameBuffer] here
         * and calling [FrameBuffer.close] where appropriate as the instance will not be released by
         * [FrameBufferRenderer].
         *
         * @param egl EGLSpec that is utilized within creation of the [FrameBuffer] object
         */
        @SuppressLint("CallbackMethodName") fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer

        /**
         * Draw contents into the [HardwareBuffer]. Before this method is invoked the [FrameBuffer]
         * instance returned in [obtainFrameBuffer] is made current
         */
        fun onDraw(eglManager: EGLManager)

        /**
         * Callback when [onDraw] is complete and the contents of the draw are reflected in the
         * corresponding [HardwareBuffer].
         *
         * @param frameBuffer [FrameBuffer] that content is rendered into. The frameBuffer should
         *   not be consumed unless the syncFenceCompat is signalled or the fence is null. This is
         *   the same [FrameBuffer] instance returned in [obtainFrameBuffer]
         * @param syncFenceCompat [SyncFenceCompat] is used to determine when rendering is done in
         *   [onDraw] and reflected within the given frameBuffer.
         */
        fun onDrawComplete(frameBuffer: FrameBuffer, syncFenceCompat: SyncFenceCompat?)
    }

    private companion object {
        private const val TAG = "FrameBufferRenderer"
    }
}

/**
 * A strategy class for deciding how to utilize [SyncFenceCompat] within
 * [FrameBufferRenderer.RenderCallback]. SyncStrategy provides default strategies for usage:
 *
 * [SyncStrategy.ALWAYS] will always create a [SyncFenceCompat] to pass into the render callbacks
 * for [FrameBufferRenderer]
 */
interface SyncStrategy {
    /**
     * Conditionally generates a [SyncFenceCompat] based upon implementation.
     *
     * @param eglSpec an [EGLSpec] object to dictate the version of EGL and make EGL calls.
     */
    fun createSyncFence(eglSpec: EGLSpec): SyncFenceCompat?

    companion object {
        /** [SyncStrategy] that will always create a [SyncFenceCompat] object */
        @JvmField
        val ALWAYS =
            object : SyncStrategy {
                override fun createSyncFence(eglSpec: EGLSpec): SyncFenceCompat? {
                    return SyncFenceCompat.createNativeSyncFence()
                }
            }
    }
}
