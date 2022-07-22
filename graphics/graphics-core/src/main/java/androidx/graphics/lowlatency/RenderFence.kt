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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLSpec
import androidx.hardware.SyncFenceCompat
import androidx.opengl.EGLExt
import androidx.opengl.EGLSyncKHR

/**
 * Synchronization mechanism for inserting an [EGLSyncKHR] sync object
 * within the list of GL Commands and associating it with a native
 * Android fence represented by [SyncFenceCompat]
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class RenderFence(private val egl: EGLSpec) : AutoCloseable {

    private val eglSync: EGLSyncKHR =
        egl.eglCreateSyncKHR(EGLExt.EGL_SYNC_NATIVE_FENCE_ANDROID, null)
            ?: throw IllegalArgumentException("Unable to create sync object")

    private val syncFence: SyncFenceCompat = egl.eglDupNativeFenceFDANDROID(eglSync)

    fun await(timeoutNanos: Long): Boolean =
        syncFence.await(timeoutNanos)

    fun awaitForever(): Boolean =
        syncFence.awaitForever()

    override fun close() {
        egl.eglDestroySyncKHR(eglSync)
        syncFence.close()
    }
}