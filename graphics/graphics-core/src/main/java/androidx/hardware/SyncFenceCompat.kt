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

package androidx.hardware

import android.opengl.EGL14
import android.opengl.EGL15
import android.opengl.GLES20
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.FrontBufferUtils
import androidx.opengl.EGLExt
import androidx.graphics.surface.SurfaceControlCompat
import androidx.opengl.EGLSyncKHR

/**
 * A synchronization primitive which signals when hardware units have completed work on a
 * particular resource. They initially start in an unsignaled state and make a one-time
 * transaction to either a signaled or error state.
 *
 * [SyncFenceCompat] is a presentation fence used in combination with
 * [SurfaceControlCompat.Transaction.setBuffer]. Note that depending on API level, this will
 * utilize either [android.hardware.SyncFence] or a compatibility implementation.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class SyncFenceCompat : AutoCloseable {
    internal val mImpl: SyncFenceImpl

    companion object {
        /**
         * Creates a native synchronization fence from an EGLSync object.
         *
         * @throws IllegalStateException if EGL dependencies cannot be resolved
         */
        @JvmStatic
        fun createNativeSyncFence(): SyncFenceCompat {
            val usePlatformSyncFence = !FrontBufferUtils.UseCompatSurfaceControl &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            return if (usePlatformSyncFence) {
                SyncFenceCompatVerificationHelper.createSyncFenceCompatV33()
            } else {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                    ?: throw IllegalStateException("No EGL Display available")
                val eglSync: EGLSyncKHR =
                    EGLExt.eglCreateSyncKHR(display, EGLExt.EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                        ?: throw IllegalStateException("Unable to create sync object")
                GLES20.glFlush()

                val syncFenceCompat = EGLExt.eglDupNativeFenceFDANDROID(display, eglSync)
                EGLExt.eglDestroySyncKHR(display, eglSync)

                syncFenceCompat
            }
        }

        /**
         * An invalid signal time. Represents either the signal time for a SyncFence that isn't
         * valid (that is, [isValid] is `false`), or if an error occurred while attempting to
         * retrieve the signal time.
         */
        const val SIGNAL_TIME_INVALID: Long = -1L

        /**
         * A pending signal time. This is equivalent to the max value of a long, representing an
         * infinitely far point in the future.
         */
        const val SIGNAL_TIME_PENDING: Long = Long.MAX_VALUE
    }

    internal constructor(syncFence: SyncFenceV19) {
        mImpl = syncFence
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal constructor(syncFence: android.hardware.SyncFence) {
        mImpl = SyncFenceV33(syncFence)
    }

    /**
     * Waits for a [SyncFenceCompat] to signal for up to the timeout duration
     *
     * @param timeoutNanos time in nanoseconds to wait for before timing out.
     */
    fun await(timeoutNanos: Long): Boolean =
        mImpl.await(timeoutNanos)

    /**
     * Waits forever for a [SyncFenceImpl] to signal
     */
    fun awaitForever(): Boolean =
        mImpl.awaitForever()

    /**
     * Close the [SyncFenceImpl]
     */
    override fun close() {
        mImpl.close()
    }

    /**
     * Returns the time that the fence signaled in the [CLOCK_MONOTONIC] time domain.
     * This returns an instant, [SyncFenceCompat.SIGNAL_TIME_INVALID] if the SyncFence is invalid, and
     * if the fence hasn't yet signaled, then [SyncFenceCompat.SIGNAL_TIME_PENDING] is returned.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSignalTimeNanos(): Long {
        return mImpl.getSignalTimeNanos()
    }

    /**
     * Checks if the SyncFence object is valid.
     * @return `true` if it is valid, `false` otherwise
     */
    fun isValid() = mImpl.isValid()
}

/**
 * Helper class to avoid class verification failures
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class SyncFenceCompatVerificationHelper private constructor() {
    companion object {
        private val mEmptyAttributes = longArrayOf(EGL14.EGL_NONE.toLong())

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun createSyncFenceCompatV33(): SyncFenceCompat {
            val display = EGL15.eglGetPlatformDisplay(
                EGL15.EGL_PLATFORM_ANDROID_KHR,
                EGL14.EGL_DEFAULT_DISPLAY.toLong(),
                mEmptyAttributes,
                0
            )
            if (display == EGL15.EGL_NO_DISPLAY) {
                throw RuntimeException("no EGL display")
            }
            val error = EGL14.eglGetError()
            if (error != EGL14.EGL_SUCCESS) {
                throw RuntimeException("eglGetPlatformDisplay failed")
            }

            val eglSync = EGL15.eglCreateSync(
                display,
                android.opengl.EGLExt.EGL_SYNC_NATIVE_FENCE_ANDROID,
                mEmptyAttributes,
                0
            )
            GLES20.glFlush()
            val syncFenceCompat = SyncFenceCompat(
                android.opengl.EGLExt.eglDupNativeFenceFDANDROID(
                    display,
                    eglSync
                )
            )
            EGL15.eglDestroySync(display, eglSync)

            return syncFenceCompat
        }
    }
}