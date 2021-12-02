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
import android.opengl.GLES20

/**
 * Class responsible for configuration of EGL related resources. This includes
 * initialization of the corresponding EGL Display as well as EGL Context, among
 * other EGL related facilities.
 */
class EglManager(val eglSpec: EglSpec = EglSpec.Egl14) {

    private val TAG = "EglManager"

    private var mEglConfig: EGLConfig? = null

    /**
     * Offscreen pixel buffer surface
     */
    private var mPBufferSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var mEglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var mWideColorGamutSupport = false
    private var mEglVersion = EglVersion.Unknown
    private var mEglExtensions: EglExtensions? = null
    private var mIsSingleBuffered: Boolean = false
    private var mQueryResult: IntArray? = null

    /**
     * Initialize the EGLManager. This initializes the default display as well
     * as queries the supported extensions
     */
    fun initialize() {
        mEglContext.let {
            if (it === EGL14.EGL_NO_CONTEXT) {
                mEglVersion = eglSpec.eglInitialize()
                mEglExtensions = EglExtensions.from(eglSpec.eglQueryString(EGL14.EGL_EXTENSIONS))
            }
        }
    }

    /**
     * Attempt to load an [EGLConfig] instance from the given
     * [EglConfigAttributes]. If the [EGLConfig] could not be loaded
     * this returns null
     */
    fun loadConfig(configAttributes: EglConfigAttributes): EGLConfig? =
        eglSpec.loadConfig(configAttributes)

    /**
     * Creates an [EGLContext] from the given [EGLConfig] returning
     * null if the context could not be created
     *
     * @throws EglException if the default surface could not be made current after context creation
     */
    fun createContext(config: EGLConfig): EGLContext {
        val eglContext = eglSpec.eglCreateContext(config)
        if (eglContext !== EGL14.EGL_NO_CONTEXT) {
            val pbBufferSurface: EGLSurface = if (isExtensionSupported(EglKhrSurfacelessContext)) {
                EGL14.EGL_NO_SURFACE
            } else {
                val configAttrs = EglConfigAttributes {
                    EGL14.EGL_WIDTH to 1
                    EGL14.EGL_HEIGHT to 1
                }
                eglSpec.eglCreatePBufferSurface(config, configAttrs)
            }
            if (!eglSpec.eglMakeCurrent(eglContext, pbBufferSurface, pbBufferSurface)) {
                throw EglException(eglSpec.eglGetError(), "Unable to make default surface current")
            }
            mPBufferSurface = pbBufferSurface
            mEglContext = eglContext
            mEglConfig = config
        } else {
            mPBufferSurface = EGL14.EGL_NO_SURFACE
            mEglContext = EGL14.EGL_NO_CONTEXT
            mEglConfig = null
        }
        return eglContext
    }

    /**
     * Release the resources allocated by EGLManager. This will destroy the corresponding
     * EGLContext instance if it was previously initialized.
     * The configured EGLVersion as well as EGLExtensions
     */
    fun release() {
        mEglContext.let {
            if (it != EGL14.EGL_NO_CONTEXT) {
                eglSpec.eglDestroyContext(it)
                mPBufferSurface.let { pbBufferSurface ->
                    if (pbBufferSurface != EGL14.EGL_NO_SURFACE) {
                        eglSpec.eglDestroySurface(pbBufferSurface)
                    }
                }
                mPBufferSurface = EGL14.EGL_NO_SURFACE
                eglSpec.eglMakeCurrent(
                    EGL14.EGL_NO_CONTEXT,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE
                )
                mEglVersion = EglVersion.Unknown
                mEglContext = EGL14.EGL_NO_CONTEXT
                mEglConfig = null
                mEglExtensions = null
            }
        }
    }

    /**
     * Returns the EGL version that is supported. This parameter is configured
     * after [initialize] is invoked.
     */
    val eglVersion: EglVersion
        get() = mEglVersion

    /**
     * Returns the current EGLContext. This parameter is configured after [initialize] is invoked
     */
    val eglContext: EGLContext?
        get() = mEglContext

    /**
     * Returns the [EGLConfig] used to load the current [EGLContext].
     * This is configured after [createContext] is invoked.
     */
    val eglConfig: EGLConfig?
        get() = mEglConfig

    /**
     * Determines whether the extension with the provided name is supported. The string
     * provided is expected to be one of the named extensions defined within the OpenGL
     * extension documentation.
     *
     * See [EglExtensions] for additional documentation for given extension name constants
     * and descriptions.
     *
     * The set of supported extensions is configured after [initialize] is invoked.
     * Attempts to query support for any extension beforehand will return false.
     */
    fun isExtensionSupported(extensionName: String): Boolean =
        mEglExtensions?.isExtensionSupported(extensionName) ?: false

    /**
     * Binds the current context to the given draw and read surfaces.
     * The draw surface is used for all operations except for any pixel data read back or
     * copy operations which are taken from the read surface.
     *
     * The same EGLSurface may be specified for both draw and read surfaces.
     *
     * If the context is not previously configured, the only valid parameters for the
     * draw and read surfaces is [EGL14.EGL_NO_SURFACE]. This is useful to make sure there is
     * always a surface specified and to release the current context without assigning a new one.
     *
     * See https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglMakeCurrent.xhtml
     *
     * @param drawSurface Surface used for all operations that involve writing pixel information
     * @param readSurface Surface used for pixel data read back or copy operations. By default this
     * is the same as [drawSurface]
     */
    @JvmOverloads
    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface = drawSurface): Boolean {
        val result = eglSpec.eglMakeCurrent(mEglContext, drawSurface, readSurface)
        if (result) {
            querySurface(drawSurface)
        }
        return result
    }

    /**
     * Post EGL surface color buffer to a native window. If the current drawing surface
     * is single buffered this will flush the buffer
     */
    fun swapAndFlushBuffers() {
        if (mIsSingleBuffered) {
            GLES20.glFlush()
        }
        eglSpec.eglSwapBuffers(currentDrawSurface)
    }

    /**
     * Returns the default surface. This can be an offscreen pixel buffer surface or
     * [EGL14.EGL_NO_SURFACE] if the surfaceless context extension is supported.
     */
    val defaultSurface: EGLSurface
        get() = mPBufferSurface

    /**
     * Returns the current surface used for drawing pixel content
     */
    val currentDrawSurface: EGLSurface
        get() = eglSpec.eglGetCurrentDrawSurface()

    /**
     * Returns the current surface used for reading back or copying pixels
     */
    val currentReadSurface: EGLSurface
        get() = eglSpec.eglGetCurrentReadSurface()

    /**
     * Helper method to query properties of the given surface
     */
    private fun querySurface(surface: EGLSurface) {
        val resultArray = mQueryResult ?: IntArray(1).also { mQueryResult = it }
        if (eglSpec.eglQuerySurface(surface, EGL14.EGL_RENDER_BUFFER, resultArray, 0)) {
            mIsSingleBuffered = resultArray[0] == EGL14.EGL_SINGLE_BUFFER
        }
    }
}