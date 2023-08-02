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

import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.FrameBufferRenderer
import androidx.graphics.opengl.SyncStrategy
import androidx.graphics.opengl.egl.EGLSpec
import androidx.hardware.SyncFenceCompat

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
            SyncFenceCompat.createNativeSyncFence()
        } else if (supportsFrontBufferUsage) {
            GLES20.glFlush()
            return null
        } else {
            val fence = SyncFenceCompat.createNativeSyncFence()
            fence.close()
            return null
        }
    }
}
