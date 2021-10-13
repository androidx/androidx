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
 * Class responsible for configuration of EGL related resources. This includes
 * initialization of the corresponding EGL Display as well as EGL Context, among
 * other EGL related facilities.
 */
class EglManager(val eglSpec: EglSpec = EglSpec.Egl14) {

    private val TAG = "EglManager"

    private var mEglConfig: EGLConfig? = null
    private var mEglContext: EGLContext? = null
    private var mWideColorGamutSupport = false
    private var mEglVersion = EglVersion.Unknown
    private var mEglExtensions: EglExtensions? = null

    /**
     * Initialize the EGLManager. This initializes the default display as well
     * as queries the supported extensions
     */
    fun initialize() {
        mEglContext.let {
            if (it == null) {
                mEglVersion = eglSpec.initialize()
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
     */
    fun createContext(config: EGLConfig): EGLContext? {
        val eglContext = eglSpec.createContext(config)
        if (eglContext != null) {
            mEglContext = eglContext
            mEglConfig = config
        } else {
            mEglContext = null
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
        mEglContext?.let {
            eglSpec.destroyContext(it)
            mEglVersion = EglVersion.Unknown
            mEglContext = null
            mEglConfig = null
            mEglExtensions = null
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
}