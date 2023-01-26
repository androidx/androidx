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

package androidx.graphics.lowlatency

import android.annotation.SuppressLint
import android.hardware.HardwareBuffer
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.opengl.EGLExt
import java.util.concurrent.atomic.AtomicBoolean
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_NATIVE_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_KHR_FENCE_SYNC

/**
 * [GLRenderer.RenderCallback] implementation that renders content into a frame buffer object
 * backed by a [HardwareBuffer] object
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

            syncFenceCompat = if (eglManager.supportsNativeAndroidFence()) {
                syncStrategy.createSyncFence(egl)
            } else if (eglManager.isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                // In this case the device only supports EGL sync objects but not creation
                // of native SyncFence objects from an EGLSync.
                // This usually occurs in emulator/cuttlefish instances as well as ChromeOS devices
                // running ARC++. In this case fallback onto creating a sync object and waiting
                // on it instead.
                // TODO b/256217036 block on another thread instead of waiting here
                val syncKhr = egl.eglCreateSyncKHR(EGLExt.EGL_SYNC_FENCE_KHR, null)
                if (syncKhr != null) {
                    GLES20.glFlush()
                    val status = egl.eglClientWaitSyncKHR(
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

    /**
     * Callbacks invoked to render content leveraging a [FrameBufferRenderer]
     */
    interface RenderCallback {

        /**
         * Obtain a [FrameBuffer] to render content into. The [FrameBuffer] obtained here
         * is expected to be managed by the consumer of [FrameBufferRenderer]. That is
         * callers of this API are expected to be maintaining a reference to the returned
         * [FrameBuffer] here and calling [FrameBuffer.close] where appropriate as the instance
         * will not be released by [FrameBufferRenderer].
         *
         * @param egl EGLSpec that is utilized within creation of the [FrameBuffer] object
         */
        @SuppressLint("CallbackMethodName")
        fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer

        /**
         * Draw contents into the [HardwareBuffer]
         */
        fun onDraw(eglManager: EGLManager)

        /**
         * Callback when [onDraw] is complete and the contents of the draw
         * are reflected in the corresponding [HardwareBuffer].
         *
         * @param frameBuffer [FrameBuffer] that content is rendered into. The frameBuffer
         * should not be consumed unless the syncFenceCompat is signalled or the fence is null.
         * @param syncFenceCompat [SyncFenceCompat] is used to determine when rendering
         * is done in [onDraw] and reflected within the given frameBuffer.
         */
        fun onDrawComplete(frameBuffer: FrameBuffer, syncFenceCompat: SyncFenceCompat?)
    }

    private companion object {
        private const val TAG = "FrameBufferRenderer"
    }
}

/**
 * A strategy class for deciding how to utilize [SyncFenceCompat] within
 * [FrameBufferRenderer.RenderCallback]. SyncStrategy provides default strategies for
 * usage:
 *
 * [SyncStrategy.ALWAYS] will always create a [SyncFenceCompat] to pass into the render
 * callbacks for [FrameBufferRenderer]
 */
interface SyncStrategy {
    /**
     * Conditionally generates a [SyncFenceCompat] based upon implementation.
     *
     * @param eglSpec an [EGLSpec] object to dictate the version of EGL and make EGL calls.
     */
    fun createSyncFence(eglSpec: EGLSpec): SyncFenceCompat?

    companion object {
        /**
         * [SyncStrategy] that will always create a [SyncFenceCompat] object
         */
        @JvmField
        val ALWAYS = object : SyncStrategy {
            override fun createSyncFence(eglSpec: EGLSpec): SyncFenceCompat? {
                return eglSpec.createNativeSyncFence()
            }
        }
    }
}

/**
 * [SyncStrategy] implementation that optimizes for front buffered rendering use cases.
 * More specifically this attempts to avoid unnecessary synchronization overhead
 * wherever possible.
 *
 * This will always provide a fence if the corresponding layer transitions from
 * an invisible to a visible state. If the layer is already visible and front
 * buffer usage flags are support on the device, then no fence is provided. If this
 * flag is not supported, then a fence is created to ensure contents
 * are flushed to the single buffer.
 *
 * @param usageFlags usage flags that describe the [HardwareBuffer] that is used as the destination
 * for rendering content within [FrameBufferRenderer]. The usage flags can be obtained via
 * [HardwareBuffer.getUsage] or by passing in the same flags from [HardwareBuffer.create]
 */
class FrontBufferSyncStrategy(
    usageFlags: Long
) : SyncStrategy {
    private val supportsFrontBufferUsage = (usageFlags and HardwareBuffer.USAGE_FRONT_BUFFER) != 0L
    private var mFrontBufferVisible: Boolean = false

    /**
     * Tells whether the corresponding front buffer layer is visible in its current state or not.
     * Utilize this to dictate when a [SyncFenceCompat] will be created when using
     * [createSyncFence].
     */
    var isVisible
        get() = mFrontBufferVisible
        set(visibility) {
            mFrontBufferVisible = visibility
        }

    /**
     * Creates a [SyncFenceCompat] based on various conditions.
     * If the layer is changing from invisible to visible, a fence is provided.
     * If the layer is already visible and front buffer usage flag is supported on the device, then
     * no fence is provided.
     * If front buffer usage is not supported, then a fence is created and destroyed to flush
     * contents to screen.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun createSyncFence(eglSpec: EGLSpec): SyncFenceCompat? {
        return if (!isVisible) {
            eglSpec.createNativeSyncFence()
        } else if (supportsFrontBufferUsage) {
            GLES20.glFlush()
            return null
        } else {
            val fence = eglSpec.createNativeSyncFence()
            fence.close()
            return null
        }
    }
}