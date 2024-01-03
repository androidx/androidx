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
package androidx.opengl

import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.utils.JniVisible
import androidx.hardware.SyncFenceCompat
import androidx.hardware.SyncFenceV19
import androidx.opengl.EGLExt.Companion.eglCreateSyncKHR

/**
 * Utility class that provides some helper methods for interacting EGL Extension APIs
 */
@Suppress("AcronymName")
class EGLExt private constructor() {

    companion object {

        /**
         * Determines if applications can query the age of the back buffer contents for an
         * EGL surface as the number of frames elapsed since the contents were recently defined
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_buffer_age.txt
         */
        const val EGL_EXT_BUFFER_AGE = "EGL_EXT_buffer_age"

        /**
         * Allows for efficient partial updates to an area of a **buffer** that has changed since
         * the last time the buffer was used
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_partial_update.txt
         */
        const val EGL_KHR_PARTIAL_UPDATE = "EGL_KHR_partial_update"

        /**
         * Allows for efficient partial updates to an area of a **surface** that changes between
         * frames for the surface. This relates to the differences between two buffers, the current
         * back buffer and the current front buffer.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_swap_buffers_with_damage.txt
         */
        const val EGL_KHR_SWAP_BUFFERS_WITH_DAMAGE = "EGL_KHR_swap_buffers_with_damage"

        /**
         * Determines whether to use sRGB format default framebuffers to render sRGB
         * content to display devices. Supports creation of EGLSurfaces which will be rendered to in
         * sRGB by OpenGL contexts supporting that capability.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_gl_colorspace.txt
         */
        const val EGL_KHR_GL_COLORSPACE = "EGL_KHR_gl_colorspace"

        /**
         * Determines whether creation of GL and ES contexts without an EGLConfig is allowed
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_no_config_context.txt
         */
        const val EGL_KHR_NO_CONFIG_CONTEXT = "EGL_KHR_no_config_context"

        /**
         * Determines whether floating point RGBA components are supported
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_pixel_format_float.txt
         */
        const val EGL_EXT_PIXEL_FORMAT_FLOAT = "EGL_EXT_pixel_format_float"

        /**
         * Determines whether extended sRGB color spaces are supported options for EGL Surfaces
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_scrgb.txt
         */
        const val EGL_EXT_GL_COLORSPACE_SCRGB = "EGL_EXT_gl_colorspace_scrgb"

        /**
         * Determines whether the underlying platform can support rendering framebuffers in the
         * non-linear Display-P3 color space
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_display_p3_passthrough.txt
         */
        const val EGL_EXT_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH =
            "EGL_EXT_gl_colorspace_display_p3_passthrough"

        /**
         * Determines whether the platform framebuffers support rendering in a larger color gamut
         * specified in the BT.2020 color space
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_bt2020_linear.txt
         */
        const val EGL_EXT_GL_COLORSPACE_BT2020_PQ = "EGL_EXT_gl_colorspace_bt2020_pq"

        /**
         * Determines whether an EGLContext can be created with a priority hint. Not all
         * implementations are guaranteed to honor the hint.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/IMG/EGL_IMG_context_priority.txt
         */
        const val EGL_IMG_CONTEXT_PRIORITY = "EGL_IMG_context_priority"

        /**
         * Determines whether creation of an EGL Context without a surface is supported.
         * This is useful for applications that only want to render to client API targets (such as
         * OpenGL framebuffer objects) and avoid the need to a throw-away EGL surface just to get
         * a current context.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_surfaceless_context.txt
         */
        const val EGL_KHR_SURFACELESS_CONTEXT = "EGL_KHR_surfaceless_context"

        /**
         * Determines whether sync objects are supported. Sync objects are synchronization
         * primitives that represent events whose completion can be tested or waited upon.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_KHR_FENCE_SYNC = "EGL_KHR_fence_sync"

        /**
         * Determines whether waiting for signaling of sync objects is supported. This form of wait
         * does not necessarily block the application thread which issued the wait. Therefore
         * applications may continue to issue commands to the client API or perform other work
         * in parallel leading to increased performance.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_wait_sync.txt
         */
        const val EGL_KHR_WAIT_SYNC = "EGL_KHR_wait_sync"

        /**
         * Determines whether creation of platform specific sync objects are supported. These
         * objects that are associated with a native synchronization fence object using a file
         * descriptor.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_native_fence_sync.txt
         */
        const val EGL_ANDROID_NATIVE_FENCE_SYNC = "EGL_ANDROID_native_fence_sync"

        /**
         * Enables using an Android window buffer (struct ANativeWindowBuffer) as an EGLImage source
         *
         * See: https://www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_image_native_buffer.txt
         */
        const val EGL_ANDROID_IMAGE_NATIVE_BUFFER = "EGL_ANDROID_image_native_buffer"

        /**
         * Extension for supporting a new EGL resource type that is suitable for
         * sharing 2D arrays of image data between client APIs, the EGLImage.
         * Although the intended purpose is sharing 2D image data, the
         * underlying interface makes no assumptions about the format or
         * purpose of the resource being shared, leaving those decisions to
         * the application and associated client APIs.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_image_base.txt
         */
        const val EGL_KHR_IMAGE_BASE = "EGL_KHR_image_base"

        /**
         * Extension that allows creating an EGLClientBuffer from an Android [HardwareBuffer]
         * object which can later be used to create an [EGLImageKHR] instance.
         * See:
         * https://registry.khronos.org/EGL/extensions/ANDROID/EGL_ANDROID_get_native_client_buffer.txt
         */
        const val EGL_ANDROID_CLIENT_BUFFER = "EGL_ANDROID_get_native_client_buffer"

        /**
         * Extension that defines a new EGL resource type that is suitable for
         * sharing 2D arrays of image data between client APIs, the EGLImage,
         * and allows creating EGLImages from EGL native pixmaps.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_image.txt
         */
        const val EGL_KHR_IMAGE = "EGL_KHR_image"

        /**
         * Specifies the types of attributes that can be queried in [eglGetSyncAttribKHR]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("AcronymName")
        @IntDef(value = [EGL_SYNC_TYPE_KHR, EGL_SYNC_STATUS_KHR, EGL_SYNC_CONDITION_KHR])
        annotation class EGLSyncAttribute

        /**
         * Attribute that can be queried in [eglGetSyncAttribKHR].
         * The results can be either [EGL_SYNC_FENCE_KHR] or [EGL_SYNC_NATIVE_FENCE_ANDROID].
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_SYNC_TYPE_KHR = 0x30F7

        /**
         * Attribute that can be queried in [eglGetSyncAttribKHR].
         * The results can be either [EGL_SIGNALED_KHR] or [EGL_UNSIGNALED_KHR] representing
         * whether or not the sync object has been signalled or not.
         * This can be queried on all sync object types.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_SYNC_STATUS_KHR = 0x30F1

        /**
         * Attribute that can be queried in [eglGetSyncAttribKHR].
         * This attribute can only be queried on sync objects of the type [EGL_SYNC_FENCE_KHR].
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_SYNC_CONDITION_KHR = 0x30F8

        /**
         * Return value when [eglGetSyncAttribKHR] is called with [EGL_SYNC_STATUS_KHR] indicating
         * that the sync object has already been signalled.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_SIGNALED_KHR = 0x30F2

        /**
         * Return value when [eglGetSyncAttribKHR] is called with [EGL_SYNC_STATUS_KHR] indicating
         * that the sync object has not yet been signalled.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_UNSIGNALED_KHR = 0x30F3

        /**
         * Return value when [eglGetSyncAttribKHR] is called with [EGL_SYNC_CONDITION_KHR].
         * This indicates that the sync object will signal on the condition of the completion
         * of the fence command on the corresponding sync object and all preceding commands
         * in th EGL client API's command stream.
         */
        const val EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR = 0x30F0

        /**
         * Specifies the type of fence to create in [eglCreateSyncKHR]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("AcronymName")
        @IntDef(value = [EGL_SYNC_FENCE_KHR, EGL_SYNC_NATIVE_FENCE_ANDROID])
        annotation class EGLFenceType

        /**
         * Create an EGL fence sync object for signalling one time events. The fence object
         * created is not associated with the Android Sync fence object and is not recommended
         * for waiting for events in a portable manner across Android/EGL boundaries but rather
         * other EGL primitives.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         */
        const val EGL_SYNC_FENCE_KHR = 0x30F9

        /**
         * This extension enables the creation of EGL fence sync objects that are
         * associated with a native synchronization fence object that is referenced
         * using a file descriptor.  These EGL fence sync objects have nearly
         * identical semantics to those defined by the KHR_fence_sync extension,
         * except that they have an additional attribute storing the file descriptor
         * referring to the native fence object. This differs from EGL_SYNC_FENCE_KHR
         * as the fence sync object is associated with an Android Sync HAL fence object.
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
        const val EGL_SYNC_NATIVE_FENCE_ANDROID = 0x3144

        /**
         * Value that can be sent as the timeoutNanos parameter of [eglClientWaitSyncKHR]
         * indicating that waiting on the sync object to signal will never time out.
         */
        // Note EGL has EGL_FOREVER_KHR defined as 0xFFFFFFFFFFFFFFFFuL. However, Java does not
        // support unsigned long types. So use -1 as the constant value here as it will be casted
        // as an EGLTimeKHR type which is uint64 in the corresponding JNI method
        const val EGL_FOREVER_KHR = -1L

        /**
         * Specifies various return values for the [eglClientWaitSyncKHR] method
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Target(AnnotationTarget.TYPE)
        @Suppress("AcronymName")
        @IntDef(value = [EGL_CONDITION_SATISFIED_KHR, EGL_TIMEOUT_EXPIRED_KHR, EGL_FALSE])
        annotation class EGLClientWaitResult

        /**
         * Return value used in [eglClientWaitSyncKHR] to indicate that the specified timeout period
         * had expired before a sync object was signalled.
         */
        const val EGL_TIMEOUT_EXPIRED_KHR = 0x30F5

        /**
         * Return value used in [eglClientWaitSyncKHR] to indicate that the sync object had
         * signalled before the timeout expired. This includes the case where the sync object had
         * already signalled before [eglClientWaitSyncKHR] was called.
         */
        const val EGL_CONDITION_SATISFIED_KHR = 0x30F6

        /**
         * Accepted in the flags parameter of [eglClientWaitSyncKHR]. This will implicitly ensure
         * pending commands are flushed to prevent [eglClientWaitSyncKHR] from potentially blocking
         * forever. See [eglClientWaitSyncKHR] for details.
         */
        const val EGL_SYNC_FLUSH_COMMANDS_BIT_KHR = 0x0001

        /**
         * Constant indicating true within EGL. This is often returned in success cases.
         */
        const val EGL_TRUE = 1

        /**
         * Constant indicating false within EGL. This is often returned in failure cases.
         */
        const val EGL_FALSE = 0

        /**
         * Creates an EGLImage from the provided [HardwareBuffer]. This handles
         * internally creating an EGLClientBuffer and an [EGLImageKHR] from the client buffer.
         *
         * When this [EGLImageKHR] instance is no longer necessary, consumers should be sure to
         * call the corresponding method [eglDestroyImageKHR] to deallocate the resource.
         *
         * @param eglDisplay EGLDisplay connection associated with the EGLImage to create
         * @param hardwareBuffer Backing [HardwareBuffer] for the generated EGLImage instance
         *
         * @return an [EGLImageKHR] instance representing the [EGLImageKHR] created from the
         * HardwareBuffer. Because this is created internally through EGL's eglCreateImageKR method,
         * this has the KHR suffix.
         *
         * This can return null if the EGL_ANDROID_image_native_buffer and EGL_KHR_image_base
         * extensions are not supported or if allocation of the buffer fails.
         *
         * See
         * www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_get_native_client_buffer.txt
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        fun eglCreateImageFromHardwareBuffer(
            eglDisplay: EGLDisplay,
            hardwareBuffer: HardwareBuffer
        ): EGLImageKHR? {
            val handle = EGLBindings.nCreateImageFromHardwareBuffer(
                eglDisplay.obtainNativeHandle(), hardwareBuffer
            )
            return if (handle == 0L) {
                null
            } else {
                EGLImageKHR(handle)
            }
        }

        /**
         * Destroy the given [EGLImageKHR] instance. Once destroyed, the image may not be used to
         * create any additional [EGLImageKHR] target resources within any client API contexts,
         * although existing [EGLImageKHR] siblings may continue to be used. `True` is returned
         * if DestroyImageKHR succeeds, `false` indicates failure. This can return `false` if
         * the [EGLImageKHR] is not associated with the default display.
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
        ): Boolean = EGLBindings.nDestroyImageKHR(
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
            EGLBindings.nImageTargetTexture2DOES(target, image.nativeHandle)
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
         * Additionally when the [EGLSyncKHR] instance is no longer necessary, consumers are
         * encouraged to call [eglDestroySyncKHR] to deallocate this resource.
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
            @EGLFenceType type: Int,
            attributes: EGLConfigAttributes?
        ): EGLSyncKHR? {
            val handle = EGLBindings.nCreateSyncKHR(
                eglDisplay.obtainNativeHandle(), type, attributes?.attrs
            )
            return if (handle == 0L) {
                null
            } else {
                EGLSyncKHR(handle)
            }
        }

        /**
         * Query attributes of the provided sync object. Accepted attributes to query depend
         * on the type of sync object. If no errors are generated, this returns true and the
         * value of the queried attribute is stored in the value array at the offset position.
         * If this method returns false, the provided value array is unmodified.
         *
         * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
         *
         * @param eglDisplay EGLDisplay to associate the sync object with
         * @param sync EGLSyncKHR object to query attributes
         * @param attribute Corresponding EGLSyncKHR attribute to query on [sync]
         * @param value Integer array used to store the result of the query
         * @param offset Index within the value array to store the result of the attribute query
         *
         * @return True if the attribute was queried successfully, false otherwise. Failure cases
         * include attempting to call this method on an invalid sync object, or the display provided
         * not matching the display that was used to create this sync object. Additionally if the
         * queried attribute is not supported for the sync object, false is returned.
         */
        @JvmStatic
        @Suppress("AcronymName")
        fun eglGetSyncAttribKHR(
            eglDisplay: EGLDisplay,
            sync: EGLSyncKHR,
            @EGLSyncAttribute attribute: Int,
            value: IntArray,
            offset: Int
        ): Boolean =
            EGLBindings.nGetSyncAttribKHR(
                eglDisplay.obtainNativeHandle(),
                sync.nativeHandle,
                attribute,
                value,
                offset
            )

        /**
         * Blocks the calling thread until the specified sync object is signalled or until
         * [timeoutNanos] nanoseconds have passed.
         * More than one [eglClientWaitSyncKHR] may be outstanding on the same [sync] at any given
         * time. When there are multiple threads blocked on the same [sync] and the [sync] object
         * has signalled, all such threads are released, but the order in which they are released is
         * not defined.
         *
         * If the value of [timeoutNanos] is zero, then [eglClientWaitSyncKHR] simply tests the
         * current status of sync. If the value of [timeoutNanos] is the special value
         * [EGL_FOREVER_KHR], then [eglClientWaitSyncKHR] does not time out. For all other values,
         * [timeoutNanos] is adjusted to the closest value allowed by the implementation-dependent
         * timeout accuracy, which may be substantially longer than one nanosecond.
         *
         * [eglClientWaitSyncKHR] returns one of three status values describing the reason for
         * returning. A return value of [EGL_TIMEOUT_EXPIRED_KHR] indicates that the specified
         * timeout period expired before [sync] was signalled, or if [timeoutNanos] is zero,
         * indicates that [sync] is not signaled. A return value of [EGL_CONDITION_SATISFIED_KHR]
         * indicates that [sync] was signaled before the timeout expired, which includes the case
         * when [sync] was already signaled when [eglClientWaitSyncKHR] was called. If an error
         * occurs then an error is generated and [EGL_FALSE] is returned.
         *
         * If the sync object being blocked upon will not be signaled in finite time (for example
         * by an associated fence command issued previously, but not yet flushed to the graphics
         * pipeline), then [eglClientWaitSyncKHR] may wait forever. To help prevent this behavior,
         * if the [EGL_SYNC_FLUSH_COMMANDS_BIT_KHR] is set on the flags parameter and the [sync] is
         * unsignaled when [eglClientWaitSyncKHR] is called, then the equivalent flush will be
         * performed for the current EGL context before blocking on sync. If no context is
         * current bound for the API, the [EGL_SYNC_FLUSH_COMMANDS_BIT_KHR] bit is ignored.
         *
         * @param eglDisplay EGLDisplay to associate the sync object with
         * @param sync EGLSyncKHR object to wait on
         * @param flags Optional flags to provide to handle flushing of pending commands
         * @param timeoutNanos Optional timeout value to wait before this method returns, measured
         * in nanoseconds. This value is always consumed as an unsigned long value so even negative
         * values will be converted to their unsigned equivalent.
         *
         * @return Result code indicating the status of the wait request. Either
         * [EGL_CONDITION_SATISFIED_KHR], if the sync did signal within the specified timeout,
         * [EGL_TIMEOUT_EXPIRED_KHR] if the sync did not signal within the specified timeout,
         * or [EGL_FALSE] if an error occurs.
         */
        @JvmStatic
        @Suppress("AcronymName")
        fun eglClientWaitSyncKHR(
            eglDisplay: EGLDisplay,
            sync: EGLSyncKHR,
            flags: Int,
            timeoutNanos: Long
        ): @EGLClientWaitResult Int =
            EGLBindings.nClientWaitSyncKHR(
                eglDisplay.obtainNativeHandle(),
                sync.nativeHandle,
                flags,
                timeoutNanos
            )

        /**
         * Creates a native synchronization fence referenced through a file descriptor
         * that is associated with an EGL fence sync object.
         *
         * See:
         * https://www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_native_fence_sync.txt
         *
         * @param display The EGLDisplay connection
         * @param sync The EGLSyncKHR to fetch the [SyncFenceCompat] from
         * @return A [SyncFenceCompat] representing the native fence.
         *  If [sync] is not a valid sync object for [display], an invalid [SyncFenceCompat]
         *  instance is returned and an EGL_BAD_PARAMETER error is generated.
         *  If the EGL_SYNC_NATIVE_FENCE_FD_ANDROID attribute of [sync] is
         *  EGL_NO_NATIVE_FENCE_FD_ANDROID, an invalid [SyncFenceCompat] is
         *  returned and an EGL_BAD_PARAMETER error is generated.
         *  If [display] does not match the display passed to [eglCreateSyncKHR]
         *  when [sync] was created, the behavior is undefined.
         */
        @JvmStatic
        @Suppress("AcronymName")
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        internal fun eglDupNativeFenceFDANDROID(
            display: EGLDisplay,
            sync: EGLSyncKHR
        ): SyncFenceCompat {
            val fd = EGLBindings.nDupNativeFenceFDANDROID(
                display.obtainNativeHandle(),
                sync.nativeHandle
            )
            return if (fd >= 0) {
                SyncFenceCompat(SyncFenceV19(fd))
            } else {
                SyncFenceCompat(SyncFenceV19(-1))
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
        ): Boolean = EGLBindings.nDestroySyncKHR(
            eglDisplay.obtainNativeHandle(),
            eglSync.nativeHandle
        )

        /**
         * Returns a set of supported supported extensions from a space separated string
         * that represents the set of OpenGL extensions supported
         */
        @JvmStatic
        fun parseExtensions(queryString: String): Set<String> =
            HashSet<String>().apply { addAll(queryString.split(' ')) }

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
@JniVisible
internal class EGLBindings {
    companion object {
        @JvmStatic
        @JniVisible
        external fun nCreateImageFromHardwareBuffer(
            eglDisplayPtr: Long,
            hardwareBuffer: HardwareBuffer
        ): Long

        // Note this API is explicitly a GL API and not an EGL API which is the reason
        // why this has the GL prefix vs EGL
        @JvmStatic
        @JniVisible
        external fun nImageTargetTexture2DOES(target: Int, eglImagePtr: Long)

        @JvmStatic
        @JniVisible
        external fun nDupNativeFenceFDANDROID(eglDisplayPtr: Long, syncPtr: Long): Int

        @JvmStatic
        @JniVisible
        external fun nCreateSyncKHR(eglDisplayPtr: Long, type: Int, attrs: IntArray?): Long

        @JvmStatic
        @JniVisible
        external fun nGetSyncAttribKHR(
            eglDisplayPtr: Long,
            syncPtr: Long,
            attrib: Int,
            result: IntArray,
            offset: Int
        ): Boolean

        @JvmStatic
        @JniVisible
        external fun nClientWaitSyncKHR(
            eglDisplayPtr: Long,
            syncPtr: Long,
            flags: Int,
            timeout: Long
        ): Int

        @JvmStatic
        @JniVisible
        external fun nDestroySyncKHR(eglDisplayPtr: Long, syncPtr: Long): Boolean
        @JvmStatic
        @JniVisible
        external fun nDestroyImageKHR(eglDisplayPtr: Long, eglImagePtr: Long): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglGetNativeClientBufferAndroid(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsDupNativeFenceFDANDROID(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglCreateImageKHR(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglDestroyImageKHR(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsGlImageTargetTexture2DOES(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglCreateSyncKHR(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglGetSyncAttribKHR(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglClientWaitSyncKHR(): Boolean

        @JvmStatic
        @JniVisible
        external fun nSupportsEglDestroySyncKHR(): Boolean

        @JvmStatic
        @JniVisible
        external fun nEqualToNativeForeverTimeout(timeoutNanos: Long): Boolean

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
