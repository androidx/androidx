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

package androidx.graphics.opengl.egl

import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Utility class that provides some helper methods for interacting EGL Extension APIs
 */
@Suppress("AcronymName")
class EGLUtils private constructor() {

    companion object {

        /**
         * This extension enables the creation of EGL fence sync objects that are
         * associated with a native synchronization fence object that is referenced
         * using a file descriptor.  These EGL fence sync objects have nearly
         * identical semantics to those defined by the KHR_fence_sync extension,
         * except that they have an additional attribute storing the file descriptor
         * referring to the native fence object.
         *
         * This extension assumes the existence of a native fence synchronization
         * object that behaves similarly to an EGL fence sync object.  These native
         * objects must have a signal status like that of an EGLSyncKHR object that
         * indicates whether the fence has ever been signaled.  Once signaled the
         * native object's signal status may not change again.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_native_fence_sync.txt
         */
        const val EGL_SYNC_NATIVE_FENCE_ANDROID = 12612

        /**
         * Creates an EGLImage from the provided [HardwareBuffer]. This handles
         * internally creating an EGLClientBuffer and an EGLImage from the client buffer.
         *
         * @param eglDisplay EGLDisplay connection associated with the EGLImage to create
         * @param hardwareBuffer Backing [HardwareBuffer] for the generated EGLImage instance
         *
         * @return an EGLImageKHR instance representing the EGLImage created from the HardwareBuffer
         * Because this is created internally through EGL's eglCreateImageKR method, this has the
         * KHR suffix.
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        fun eglCreateImageFromHardwareBuffer(
            eglDisplay: EGLDisplay,
            hardwareBuffer: HardwareBuffer
        ): EGLImageKHR? {
            val handle = EGLUtilsBindings.nCreateImageFromHardwareBuffer(
                eglDisplay.obtainNativeHandle(), hardwareBuffer)
            return if (handle == 0L) {
                null
            } else {
                EGLImageKHR(handle)
            }
        }

        /**
         * Destroy the given EGLImageKHR instance. Once destroyed, the image may not be used to
         * create any additional EGLImage target resources within any client API contexts,
         * although existing EGLImage siblings may continue to be used. True is returned
         * if DestroyImageKHR succeeds, false indicates failure. This can return false if
         * the EGLImage is not associated with the default display.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_image_base.txt
         *
         * @param eglDisplay EGLDisplay that this EGLImage is connected to
         * @param image EGLImageKHR to be destroyed
         *
         * @return True if the destruction of the EGLImageKHR object was successful, false otherwise
         */
        @JvmStatic
        @Suppress("AcronymName")
        fun eglDestroyImageKHR(
            eglDisplay: EGLDisplay,
            image: EGLImageKHR
        ): Boolean = EGLUtilsBindings.nDestroyImageKHR(
            eglDisplay.obtainNativeHandle(),
            image.nativeHandle
        )

        /**
         * Upload a given EGLImage to the currently bound GLTexture
         *
         * This method requires either of the following EGL extensions to be supported:
         * EGL_KHR_image_base or EGL_KHR_image
         *
         * See: https://www.khronos.org/registry/OpenGL/extensions/OES/OES_EGL_image_external.txt
         */
        @JvmStatic
        @Suppress("AcronymName")
        fun glEGLImageTargetTexture2DOES(target: Int, image: EGLImageKHR) {
            EGLUtilsBindings.nImageTargetTexture2DOES(target, image.nativeHandle)
        }

        /**
         * Creates a sync object of the specified type associated with the
         * specified display, and returns a handle to the new object.
         * The configuration of the returned [EGLSyncKHR] object is specified by the provided
         * attributes.
         *
         * Consumers should ensure that the EGL_KHR_fence_sync EGL extension is supported before
         * invoking this method otherwise a null EGLSyncFenceKHR object is returned.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         *
         * @param eglDisplay EGLDisplay to associate the sync object with
         * @param type Indicates the type of sync object that is returned
         * @param attributes Specifies the configuration of the sync object returned
         *
         * @return the EGLSyncKHR object to be used as a fence or null if this extension
         * is not supported
         */
        @JvmStatic
        @Suppress("AcronymName")
        fun eglCreateSyncKHR(
            eglDisplay: EGLDisplay,
            type: Int,
            attributes: EglConfigAttributes?
        ): EGLSyncKHR? {
            val handle = EGLUtilsBindings.nCreateSyncKHR(
                eglDisplay.obtainNativeHandle(), type, attributes?.attrs)
            return if (handle == 0L) {
                null
            } else {
                EGLSyncKHR(handle)
            }
        }

        /**
         * Destroys the given sync object associated with the specified display
         *
         * Consumers should ensure that the EGL_KHR_fence_sync EGL extension is supported before
         * invoking this method otherwise a null EGLSyncFenceKHR object is returned.
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         *
         * @param eglDisplay EGLDisplay instance associated with the fence
         * @param eglSync Fence object to be destroyed
         *
         * @return true if the EGLSyncKHR object was destroyed successfully false otherwise. This
         * can return false if the sync object is not a valid sync object for the provided display
         * or if the display provided in this method does not match the display used to create this
         * sync in eglCreateSyncKHR.
         */
        @JvmStatic
        @Suppress("AcronymName")
        fun eglDestroySyncKHR(
            eglDisplay: EGLDisplay,
            eglSync: EGLSyncKHR
        ): Boolean = EGLUtilsBindings.nDestroySyncKHR(
            eglDisplay.obtainNativeHandle(),
            eglSync.nativeHandle
        )

        /**
         * Helper method to obtain the corresponding native handle. Newer versions of Android
         * represent the native pointer as a long instead of an integer to support 64 bits.
         * For OS levels that support the wider bit format, invoke it otherwise cast the int
         * to a long.
         *
         * This is internal to avoid synthetic accessors
         */
        internal fun EGLDisplay.obtainNativeHandle(): Long =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EGLDisplayVerificationHelper.getNativeHandle(this)
            } else {
                @Suppress("DEPRECATION")
                handle.toLong()
            }
    }
}

/**
 * Helper class to configure JNI bindings to be invoked within the EGLUtils
 * public API. This class is provided to separate responsibilities of jni method registration
 * and helps to avoid synthetic accessor warnings
 */
internal class EGLUtilsBindings {
    companion object {
        external fun nCreateImageFromHardwareBuffer(
            eglDisplayPtr: Long,
            hardwareBuffer: HardwareBuffer
        ): Long

        // Note this API is explicitly a GL API and not an EGL API which is the reason
        // why this has the GL prefix vs EGL
        external fun nImageTargetTexture2DOES(target: Int, eglImagePtr: Long)
        external fun nCreateSyncKHR(eglDisplayPtr: Long, type: Int, attrs: IntArray?): Long
        external fun nDestroySyncKHR(eglDisplayPtr: Long, syncPtr: Long): Boolean
        external fun nDestroyImageKHR(eglDisplayPtr: Long, eglImagePtr: Long): Boolean
        external fun nSupportsEglGetNativeClientBufferAndroid(): Boolean
        external fun nSupportsEglCreateImageKHR(): Boolean
        external fun nSupportsEglDestroyImageKHR(): Boolean
        external fun nSupportsGlImageTargetTexture2DOES(): Boolean
        external fun nSupportsEglCreateSyncKHR(): Boolean
        external fun nSupportsEglDestroySyncKHR(): Boolean

        init {
            System.loadLibrary("graphics-core")
        }
    }
}

/**
 * Helper class to avoid class verification failures
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private class EGLDisplayVerificationHelper private constructor() {

    companion object {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @androidx.annotation.DoNotInline
        fun getNativeHandle(eglDisplay: EGLDisplay): Long = eglDisplay.nativeHandle
    }
}