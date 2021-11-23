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

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.view.Surface

/**
 * Interface for accessing various EGL facilities independent of EGL versions.
 * That is each EGL version implements this specification.
 *
 * EglSpec is not thread safe and is up to the caller of these methods to guarantee thread safety.
 */
interface EglSpec {

    /**
     * Query for the capabilities associated with the given eglDisplay.
     * The result contains a space separated list of the capabilities.
     *
     * @param nameId identifier for the EGL string to query
     */
    fun eglQueryString(nameId: Int): String

    /**
     * Create a Pixel Buffer surface with the corresponding [EglConfigAttributes].
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
        configAttributes: EglConfigAttributes?
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
        configAttributes: EglConfigAttributes?
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
     * @return True if destruction of the EGLSurface was successful, false otherwise
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
     * implementation through [EglVersion]. If initialization fails, this returns
     * [EglVersion.Unknown]
     */
    fun eglInitialize(): EglVersion

    /**
     * Load a corresponding EGLConfig from the provided [EglConfigAttributes]
     * If the EGLConfig could not be loaded, null is returned
     * @param configAttributes Desired [EglConfigAttributes] to create an [EGLConfig]
     *
     * @return the [EGLConfig] with the provided [EglConfigAttributes] or null if
     * an [EGLConfig] could not be created with the specified attributes
     */
    fun loadConfig(configAttributes: EglConfigAttributes): EGLConfig?

    /**
     * Create an EGLContext with the default display. If createContext fails to create a
     * rendering context, EGL_NO_CONTEXT is returned
     *
     * @param config [EGLConfig] used to create the [EGLContext]
     */
    fun eglCreateContext(config: EGLConfig): EGLContext

    /**
     * Destroy the given EGLContext generated in [eglCreateContext]
     */
    fun eglDestroyContext(eglContext: EGLContext)

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

    companion object {

        @JvmField
        val Egl14 = object : EglSpec {

            // Tuples of attribute identifiers along with their corresponding values.
            // EGL_NONE is used as a termination value similar to a null terminated string
            private val contextAttributes = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, // GLES VERSION 2
                // HWUI provides the ability to configure a context priority as well but that only
                // seems to be configured on SystemUIApplication. This might be useful for
                // front buffer rendering situations for performance.
                EGL14.EGL_NONE
            )

            override fun eglInitialize(): EglVersion {
                // eglInitialize is destructive so create 2 separate arrays to store the major and
                // minor version
                val major = intArrayOf(1)
                val minor = intArrayOf(1)
                val initializeResult =
                    EGL14.eglInitialize(getDefaultDisplay(), major, 0, minor, 0)
                if (initializeResult) {
                    return EglVersion(major[0], minor[0])
                } else {
                    throw EglException(EGL14.eglGetError(), "Unable to initialize default display")
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
                configAttributes: EglConfigAttributes?
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
                configAttributes: EglConfigAttributes?,
            ): EGLSurface =
                EGL14.eglCreateWindowSurface(
                    getDefaultDisplay(),
                    config,
                    surface,
                    configAttributes?.attrs,
                    0
                )

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

            override fun loadConfig(configAttributes: EglConfigAttributes): EGLConfig? {
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
                    throw EglException(EGL14.eglGetError(), "Unable to destroy EGLContext")
                }
            }

            override fun eglGetError(): Int = EGL14.eglGetError()

            private fun getDefaultDisplay() = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
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
 * Convenience method to obtain the corresponding error string from the
 * error code obtained from [EglSpec.eglGetError]
 */
fun EglSpec.getErrorMessage(): String = EglSpec.getStatusString(eglGetError())

/**
 * Exception class for reporting errors with EGL
 *
 * @param error Error code reported via eglGetError
 * @param msg Optional message describing the exception being thrown
 */
class EglException(val error: Int, val msg: String = "") : RuntimeException() {

    override val message: String
        get() = "Error: ${EglSpec.getStatusString(error)}, $msg"
}

/**
 * Identifier for the current EGL implementation
 *
 * @param major Major version of the EGL implementation
 * @param minor Minor version of the EGL implementation
 */
data class EglVersion(
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
        val V14 = EglVersion(1, 4)

        /**
         * Constant that represents version 1.5 of the EGL spec
         */
        @JvmField
        val V15 = EglVersion(1, 5)

        /**
         * Sentinel EglVersion value returned in error situations
         */
        @JvmField
        val Unknown = EglVersion(-1, -1)
    }
}