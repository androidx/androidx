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

/**
 * Interface for accessing various EGL facilities independent of EGL versions.
 * That is each EGL version implements this specification
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
     * Initialize the EGL implementation and return the major and minor version of the EGL
     * implementation through [EglVersion]. If initialization fails, this returns
     * [EglVersion.Unknown]
     */
    fun initialize(): EglVersion

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
    fun createContext(config: EGLConfig): EGLContext?

    /**
     * Destroy the given EGLContext generated in [createContext]
     */
    fun destroyContext(eglContext: EGLContext)

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

            override fun initialize(): EglVersion {
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

            override fun eglQueryString(nameId: Int): String =
                EGL14.eglQueryString(getDefaultDisplay(), nameId)

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

            override fun createContext(config: EGLConfig): EGLContext {
                return EGL14.eglCreateContext(
                    getDefaultDisplay(),
                    config,
                    EGL14.EGL_NO_CONTEXT, // not creating from a shared context
                    contextAttributes,
                    0
                )
            }

            override fun destroyContext(eglContext: EGLContext) {
                if (!EGL14.eglDestroyContext(getDefaultDisplay(), eglContext)) {
                    throw EglException(EGL14.eglGetError(), "Unable to destroy EGLContext")
                }
            }

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