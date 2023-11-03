/*
 * Copyright 2021 The Android Open Source Project
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
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.opengl.EGLExt
import androidx.opengl.EGLExt.Companion.EGLClientWaitResult
import androidx.opengl.EGLExt.Companion.EGLSyncAttribute
import androidx.opengl.EGLExt.Companion.EGL_CONDITION_SATISFIED_KHR
import androidx.opengl.EGLExt.Companion.EGL_FALSE
import androidx.opengl.EGLExt.Companion.EGL_FOREVER_KHR
import androidx.opengl.EGLExt.Companion.EGL_SYNC_FLUSH_COMMANDS_BIT_KHR
import androidx.opengl.EGLExt.Companion.EGL_TIMEOUT_EXPIRED_KHR
import androidx.opengl.EGLExt.Companion.eglClientWaitSyncKHR
import androidx.opengl.EGLExt.Companion.eglDestroyImageKHR
import androidx.opengl.EGLExt.Companion.eglDestroySyncKHR
import androidx.opengl.EGLImageKHR
import androidx.opengl.EGLSyncKHR

@JvmDefaultWithCompatibility
/**
 * Interface for accessing various EGL facilities independent of EGL versions.
 * That is each EGL version implements this specification.
 *
 * EGLSpec is not thread safe and is up to the caller of these methods to guarantee thread safety.
 */
@Suppress("AcronymName")
interface EGLSpec {

    /**
     * Query for the capabilities associated with the given eglDisplay.
     * The result contains a space separated list of the capabilities.
     *
     * @param nameId identifier for the EGL string to query
     */
    fun eglQueryString(nameId: Int): String

    /**
     * Create a Pixel Buffer surface with the corresponding [EGLConfigAttributes].
     * Accepted attributes are defined as part of the OpenGL specification here:
     * https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglCreatePbufferSurface.xhtml
     *
     * If a pixel buffer surface could not be created, [EGL14.EGL_NO_SURFACE] is returned.
     *
     * @param config Specifies the EGL Frame buffer configuration that defines the frame buffer
     * resource available to the surface
     * @param configAttributes Optional list of attributes for the pixel buffer surface
     */
    fun eglCreatePBufferSurface(
        config: EGLConfig,
        configAttributes: EGLConfigAttributes?
    ): EGLSurface

    /**
     * Creates an on screen EGL window surface from the given [Surface] and returns a handle to it.
     *
     * See https://khronos.org/registry/EGL/sdk/docs/man/html/eglCreateWindowSurface.xhtml
     *
     * @param config Specifies the EGL frame buffer configuration that defines the frame buffer
     * resource available to the surface
     * @param surface Android surface to consume rendered content
     * @param configAttributes Optional list of attributes for the specified surface
     */
    fun eglCreateWindowSurface(
        config: EGLConfig,
        surface: Surface,
        configAttributes: EGLConfigAttributes?
    ): EGLSurface

    /**
     * Destroys an EGL surface.
     *
     * If the EGL surface is not current to any thread, eglDestroySurface destroys
     * it immediately. Otherwise, surface is destroyed when it becomes not current to any thread.
     * Furthermore, resources associated with a pbuffer surface are not released until all color
     * buffers of that pbuffer bound to a texture object have been released. Deferral of
     * surface destruction would still return true as deferral does not indicate a failure condition
     *
     * @return `true` if destruction of the EGLSurface was successful, false otherwise
     */
    fun eglDestroySurface(surface: EGLSurface): Boolean

    /**
     * Binds the current context to the given draw and read surfaces.
     * The draw surface is used for all operations except for any pixel data read back or copy
     * operations which are taken from the read surface.
     *
     * The same EGLSurface may be specified for both draw and read surfaces.
     *
     * See https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglMakeCurrent.xhtml for more
     * information
     *
     * @param drawSurface EGLSurface to draw pixels into.
     * @param readSurface EGLSurface used for read/copy operations.
     */
    fun eglMakeCurrent(
        context: EGLContext,
        drawSurface: EGLSurface,
        readSurface: EGLSurface
    ): Boolean

    /**
     * Return the current surface used for reading or copying pixels.
     * If no context is current, [EGL14.EGL_NO_SURFACE] is returned
     */
    fun eglGetCurrentReadSurface(): EGLSurface

    /**
     * Return the current surface used for drawing pixels.
     * If no context is current, [EGL14.EGL_NO_SURFACE] is returned.
     */
    fun eglGetCurrentDrawSurface(): EGLSurface

    /**
     * Initialize the EGL implementation and return the major and minor version of the EGL
     * implementation through [EGLVersion]. If initialization fails, this returns
     * [EGLVersion.Unknown]
     */
    fun eglInitialize(): EGLVersion

    /**
     * Load a corresponding EGLConfig from the provided [EGLConfigAttributes]
     * If the EGLConfig could not be loaded, null is returned
     * @param configAttributes Desired [EGLConfigAttributes] to create an [EGLConfig]
     *
     * @return the [EGLConfig] with the provided [EGLConfigAttributes] or null if
     * an [EGLConfig] could not be created with the specified attributes
     */
    fun loadConfig(configAttributes: EGLConfigAttributes): EGLConfig?

    /**
     * Create an EGLContext with the default display. If createContext fails to create a
     * rendering context, EGL_NO_CONTEXT is returned
     *
     * @param config [EGLConfig] used to create the [EGLContext]
     */
    fun eglCreateContext(config: EGLConfig): EGLContext

    /**
     * Destroy the given EGLContext generated in [eglCreateContext]
     *
     * See https://khronos.org/registry/EGL/sdk/docs/man/html/eglDestroyContext.xhtml
     *
     * @param eglContext EGL rendering context to be destroyed
     */
    fun eglDestroyContext(eglContext: EGLContext)

    /**
     * Post EGL surface color buffer to a native window
     *
     * See https://khronos.org/registry/EGL/sdk/docs/man/html/eglSwapBuffers.xhtml
     *
     * @param surface Specifies the EGL drawing surface whose buffers are to be swapped
     *
     * @return `true` if swapping of buffers succeeds, false otherwise
     */
    fun eglSwapBuffers(surface: EGLSurface): Boolean

    /**
     * Query the EGL attributes of the provided surface
     *
     * @param surface EGLSurface to be queried
     * @param attribute EGL attribute to query on the given EGL Surface
     * @param result Int array to store the result of the query
     * @param offset Index within [result] to store the value of the queried attribute
     *
     * @return `true` if the query was completed successfully, false otherwise. If the query
     * fails, [result] is unmodified
     */
    fun eglQuerySurface(surface: EGLSurface, attribute: Int, result: IntArray, offset: Int): Boolean

    /**
     * Returns the error of the last called EGL function in the current thread. Initially,
     * the error is set to EGL_SUCCESS. When an EGL function could potentially generate several
     * different errors (for example, when passed both a bad attribute name, and a bad attribute
     * value for a legal attribute name), the implementation may choose to generate any one of the
     * applicable errors.
     *
     * See https://khronos.org/registry/EGL/sdk/docs/man/html/eglGetError.xhtml for more information
     * and error codes that could potentially be returned
     */
    fun eglGetError(): Int

    /**
     * Convenience method to obtain the corresponding error string from the
     * error code obtained from [EGLSpec.eglGetError]
     */
    fun getErrorMessage(): String = getStatusString(eglGetError())

    /**
     * Creates an EGLImage from the provided [HardwareBuffer]. This handles
     * internally creating an EGLClientBuffer and an [EGLImageKHR] from the client buffer.
     *
     * When this [EGLImageKHR] instance is no longer necessary, consumers should be sure to
     * call the corresponding method [eglDestroyImageKHR] to deallocate the resource.
     *
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
    @Suppress("AcronymName")
    @RequiresApi(Build.VERSION_CODES.O)
    fun eglCreateImageFromHardwareBuffer(hardwareBuffer: HardwareBuffer): EGLImageKHR?

    /**
     * Destroy the given [EGLImageKHR] instance. Once destroyed, the image may not be used to
     * create any additional [EGLImageKHR] target resources within any client API contexts,
     * although existing [EGLImageKHR] siblings may continue to be used. `true` is returned
     * if DestroyImageKHR succeeds, `false` indicates failure. This can return `false` if the
     * corresponding [EGLContext] is not valid.
     *
     * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_image_base.txt
     *
     * @param image EGLImageKHR to be destroyed
     *
     * @return `true` if the destruction of the EGLImageKHR object was successful, `false` otherwise
     */
    @Suppress("AcronymName")
    fun eglDestroyImageKHR(image: EGLImageKHR): Boolean

    /**
     * Creates a sync object of the specified type associated with the
     * specified display, and returns a handle to the new object.
     * The configuration of the returned [EGLSyncKHR] object is specified by the provided
     * attributes.
     *
     * Consumers should ensure that the EGL_KHR_fence_sync EGL extension is supported before
     * invoking this method otherwise a null EGLSyncFenceKHR object is returned.
     *
     * When the [EGLSyncKHR] instance is no longer necessary, consumers are encouraged to call
     * [eglDestroySyncKHR] to deallocate this resource.
     *
     * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
     *
     * @param type Indicates the type of sync object that is returned
     * @param attributes Specifies the configuration of the sync object returned
     *
     * @return the [EGLSyncKHR] object to be used as a fence or null if this extension
     * is not supported
     */
    @Suppress("AcronymName")
    fun eglCreateSyncKHR(type: Int, attributes: EGLConfigAttributes?): EGLSyncKHR?

    /**
     * Query attributes of the provided sync object. Accepted attributes to query depend
     * on the type of sync object. If no errors are generated, this returns true and the
     * value of the queried attribute is stored in the value array at the offset position.
     * If this method returns false, the provided value array is unmodified.
     *
     * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
     *
     * @param sync EGLSyncKHR object to query attributes
     * @param attribute Corresponding EGLSyncKHR attribute to query on [sync]
     * @param value Integer array used to store the result of the query
     * @param offset Index within the value array to store the result of the attribute query
     *
     * @return `true` if the attribute was queried successfully, false otherwise. Failure cases
     * include attempting to call this method on an invalid sync object, or the display provided
     * not matching the display that was used to create this sync object. Additionally if the
     * queried attribute is not supported for the sync object, false is returned.
     */
    @Suppress("AcronymName")
    fun eglGetSyncAttribKHR(
        sync: EGLSyncKHR,
        @EGLSyncAttribute attribute: Int,
        value: IntArray,
        offset: Int
    ): Boolean

    /**
     * Destroys the given sync object associated with the specified display
     *
     * Consumers should ensure that the EGL_KHR_fence_sync EGL extension is supported before
     * invoking this method otherwise a null EGLSyncFenceKHR object is returned.
     * See: https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
     *
     * @param sync Fence object to be destroyed
     *
     * @return `true` if the [EGLSyncKHR] object was destroyed successfully `false` otherwise. This
     * can return `false` if the sync object is not a valid sync object for the provided display
     * or if the display provided in this method does not match the display used to create this
     * sync in [eglCreateSyncKHR].
     */
    @Suppress("AcronymName")
    fun eglDestroySyncKHR(sync: EGLSyncKHR): Boolean

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
    @Suppress("AcronymName")
    fun eglClientWaitSyncKHR(
        sync: EGLSyncKHR,
        flags: Int,
        timeoutNanos: Long
    ): @EGLClientWaitResult Int

    companion object {

        @JvmField
        val V14 = object : EGLSpec {

            // Tuples of attribute identifiers along with their corresponding values.
            // EGL_NONE is used as a termination value similar to a null terminated string
            private val contextAttributes = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, // GLES VERSION 2
                // HWUI provides the ability to configure a context priority as well but that only
                // seems to be configured on SystemUIApplication. This might be useful for
                // front buffer rendering situations for performance.
                EGL14.EGL_NONE
            )

            override fun eglInitialize(): EGLVersion {
                // eglInitialize is destructive so create 2 separate arrays to store the major and
                // minor version
                val major = intArrayOf(1)
                val minor = intArrayOf(1)
                val initializeResult =
                    EGL14.eglInitialize(getDefaultDisplay(), major, 0, minor, 0)
                if (initializeResult) {
                    return EGLVersion(major[0], minor[0])
                } else {
                    throw EGLException(EGL14.eglGetError(), "Unable to initialize default display")
                }
            }

            override fun eglGetCurrentReadSurface(): EGLSurface =
                EGL14.eglGetCurrentSurface(EGL14.EGL_READ)

            override fun eglGetCurrentDrawSurface(): EGLSurface =
                EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)

            override fun eglQueryString(nameId: Int): String =
                EGL14.eglQueryString(getDefaultDisplay(), nameId)

            override fun eglCreatePBufferSurface(
                config: EGLConfig,
                configAttributes: EGLConfigAttributes?
            ): EGLSurface =
                EGL14.eglCreatePbufferSurface(
                    getDefaultDisplay(),
                    config,
                    configAttributes?.attrs,
                    0
                )

            override fun eglCreateWindowSurface(
                config: EGLConfig,
                surface: Surface,
                configAttributes: EGLConfigAttributes?,
            ): EGLSurface =
                EGL14.eglCreateWindowSurface(
                    getDefaultDisplay(),
                    config,
                    surface,
                    configAttributes?.attrs ?: DefaultWindowSurfaceConfig.attrs,
                    0
                )

            override fun eglSwapBuffers(surface: EGLSurface): Boolean =
                EGL14.eglSwapBuffers(getDefaultDisplay(), surface)

            override fun eglQuerySurface(
                surface: EGLSurface,
                attribute: Int,
                result: IntArray,
                offset: Int
            ): Boolean =
                EGL14.eglQuerySurface(getDefaultDisplay(), surface, attribute, result, offset)

            override fun eglDestroySurface(surface: EGLSurface) =
                EGL14.eglDestroySurface(getDefaultDisplay(), surface)

            override fun eglMakeCurrent(
                context: EGLContext,
                drawSurface: EGLSurface,
                readSurface: EGLSurface
            ): Boolean =
                EGL14.eglMakeCurrent(
                    getDefaultDisplay(),
                    drawSurface,
                    readSurface,
                    context
                )

            override fun loadConfig(configAttributes: EGLConfigAttributes): EGLConfig? {
                val configs = arrayOfNulls<EGLConfig?>(1)
                return if (EGL14.eglChooseConfig(
                    getDefaultDisplay(),
                    configAttributes.attrs,
                    0,
                    configs,
                    0,
                    1,
                    intArrayOf(1),
                    0
                )) {
                    configs[0]
                } else {
                    null
                }
            }

            override fun eglCreateContext(config: EGLConfig): EGLContext {
                return EGL14.eglCreateContext(
                    getDefaultDisplay(),
                    config,
                    EGL14.EGL_NO_CONTEXT, // not creating from a shared context
                    contextAttributes,
                    0
                )
            }

            override fun eglDestroyContext(eglContext: EGLContext) {
                if (!EGL14.eglDestroyContext(getDefaultDisplay(), eglContext)) {
                    throw EGLException(EGL14.eglGetError(), "Unable to destroy EGLContext")
                }
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun eglCreateImageFromHardwareBuffer(
                hardwareBuffer: HardwareBuffer
            ): EGLImageKHR? =
                EGLExt.eglCreateImageFromHardwareBuffer(getDefaultDisplay(), hardwareBuffer)

            override fun eglDestroyImageKHR(image: EGLImageKHR): Boolean =
                EGLExt.eglDestroyImageKHR(getDefaultDisplay(), image)

            override fun eglCreateSyncKHR(
                type: Int,
                attributes: EGLConfigAttributes?
            ): EGLSyncKHR? =
                EGLExt.eglCreateSyncKHR(getDefaultDisplay(), type, attributes)

            override fun eglGetSyncAttribKHR(
                sync: EGLSyncKHR,
                attribute: Int,
                value: IntArray,
                offset: Int
            ): Boolean =
                EGLExt.eglGetSyncAttribKHR(getDefaultDisplay(), sync, attribute, value, offset)

            override fun eglDestroySyncKHR(sync: EGLSyncKHR): Boolean =
                EGLExt.eglDestroySyncKHR(getDefaultDisplay(), sync)

            override fun eglGetError(): Int = EGL14.eglGetError()

            override fun eglClientWaitSyncKHR(
                sync: EGLSyncKHR,
                flags: Int,
                timeoutNanos: Long
            ): Int =
                EGLExt.eglClientWaitSyncKHR(getDefaultDisplay(), sync, flags, timeoutNanos)

            private fun getDefaultDisplay() = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

            /**
             * EglConfigAttribute that provides the default attributes for an EGL window surface
             */
            private val DefaultWindowSurfaceConfig = EGLConfigAttributes {}
        }

        /**
         * Return a string representation of the corresponding EGL status code.
         * If the provided error value is not an EGL status code, the hex representation
         * is returned instead
         */
        @JvmStatic
        fun getStatusString(error: Int): String =
            when (error) {
                EGL14.EGL_SUCCESS -> "EGL_SUCCESS"
                EGL14.EGL_NOT_INITIALIZED -> "EGL_NOT_INITIALIZED"
                EGL14.EGL_BAD_ACCESS -> "EGL_BAD_ACCESS"
                EGL14.EGL_BAD_ALLOC -> "EGL_BAD_ALLOC"
                EGL14.EGL_BAD_ATTRIBUTE -> "EGL_BAD_ATTRIBUTE"
                EGL14.EGL_BAD_CONFIG -> "EGL_BAD_CONFIG"
                EGL14.EGL_BAD_CONTEXT -> "EGL_BAD_CONTEXT"
                EGL14.EGL_BAD_CURRENT_SURFACE -> "EGL_BAD_CURRENT_SURFACE"
                EGL14.EGL_BAD_DISPLAY -> "EGL_BAD_DISPLAY"
                EGL14.EGL_BAD_MATCH -> "EGL_BAD_MATCH"
                EGL14.EGL_BAD_NATIVE_PIXMAP -> "EGL_BAD_NATIVE_PIXMAP"
                EGL14.EGL_BAD_NATIVE_WINDOW -> "EGL_BAD_NATIVE_WINDOW"
                EGL14.EGL_BAD_PARAMETER -> "EGL_BAD_PARAMETER"
                EGL14.EGL_BAD_SURFACE -> "EGL_BAD_SURFACE"
                EGL14.EGL_CONTEXT_LOST -> "EGL_CONTEXT_LOST"
                else -> Integer.toHexString(error)
            }
    }
}

/**
 * Exception class for reporting errors with EGL
 *
 * @param error Error code reported via eglGetError
 * @param msg Optional message describing the exception being thrown
 */
@Suppress("AcronymName")
class EGLException(val error: Int, val msg: String = "") : RuntimeException() {

    override val message: String
        get() = "Error: ${EGLSpec.getStatusString(error)}, $msg"
}

/**
 * Identifier for the current EGL implementation
 *
 * @param major Major version of the EGL implementation
 * @param minor Minor version of the EGL implementation
 */
@Suppress("AcronymName")
data class EGLVersion(
    val major: Int,
    val minor: Int
) {

    override fun toString(): String {
        return "EGL version $major.$minor"
    }

    companion object {
        /**
         * Constant that represents version 1.4 of the EGL spec
         */
        @JvmField
        val V14 = EGLVersion(1, 4)

        /**
         * Constant that represents version 1.5 of the EGL spec
         */
        @JvmField
        val V15 = EGLVersion(1, 5)

        /**
         * Sentinel EglVersion value returned in error situations
         */
        @JvmField
        val Unknown = EGLVersion(-1, -1)
    }
}
