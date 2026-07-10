/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.core.processing.util

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.core.DynamicRange
import androidx.camera.core.processing.util.GLUtils.InputFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ShaderProvidersTest {

    private lateinit var glThread: HandlerThread
    private lateinit var glHandler: Handler
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    @Before
    fun setUp() {
        glThread = HandlerThread("GLThread").apply { start() }
        glHandler = Handler(glThread.looper)

        runBlocking(glHandler.asCoroutineDispatcher()) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs =
                intArrayOf(
                    EGL14.EGL_RED_SIZE,
                    8,
                    EGL14.EGL_GREEN_SIZE,
                    8,
                    EGL14.EGL_BLUE_SIZE,
                    8,
                    EGL14.EGL_ALPHA_SIZE,
                    8,
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE,
                )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val config = configs[0]

            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext =
                EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttribs, 0)
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }
    }

    @After
    fun tearDown() {
        runBlocking(glHandler.asCoroutineDispatcher()) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }
        glThread.quitSafely()
    }

    @Test
    fun sdrDefaultFragmentShader_containsExpectedKeywords() {
        val shader =
            ShaderProviders.createFragmentShaderInternal(
                samplerVarName = "sTexture",
                fragCoordsVarName = "vTextureCoord",
                isHdr = false,
                hasAdvancedStyling = false,
            )
        assertThat(shader).contains("samplerExternalOES sTexture")
        assertThat(shader).contains("varying vec2 vTextureCoord")
        assertThat(shader).contains("texture2D")
        assertThat(shader).doesNotContain("uCornerRadiusRatio")
    }

    @Test
    fun sdrCompositionFragmentShader_containsExpectedKeywords() {
        val shader =
            ShaderProviders.createFragmentShaderInternal(
                samplerVarName = "sTexture",
                fragCoordsVarName = "vTextureCoord",
                isHdr = false,
                hasAdvancedStyling = true,
            )
        assertThat(shader).contains("samplerExternalOES sTexture")
        assertThat(shader).contains("uCornerRadiusRatio")
        assertThat(shader).contains("uBorderWidth")
        assertThat(shader).contains("vPosition")
    }

    @Test
    fun hdrYuvFragmentShader_containsExpectedKeywords() {
        val shader =
            ShaderProviders.createFragmentShaderInternal(
                samplerVarName = "sTexture",
                fragCoordsVarName = "vTextureCoord",
                isHdr = true,
                isYuvHdr = true,
                hasAdvancedStyling = true,
            )
        assertThat(shader).contains("__samplerExternal2DY2YEXT sTexture")
        assertThat(shader).contains("#extension GL_EXT_YUV_target : require")
        assertThat(shader).contains("yuvToRgb")
        assertThat(shader).contains("uCornerRadiusRatio")
    }

    @Test
    fun shadersCompileSuccessfully() {
        runBlocking(glHandler.asCoroutineDispatcher()) {
            // Test a few combinations
            val sdrFragment =
                ShaderProviders.createFragmentShaderInternal("s", "v", false, false, false)
            val shader1 = GLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, sdrFragment)
            GLES20.glDeleteShader(shader1)

            val sdrCompFragment =
                ShaderProviders.createFragmentShaderInternal("s", "v", false, false, true)
            val shader2 = GLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, sdrCompFragment)
            GLES20.glDeleteShader(shader2)
        }
    }

    @Test
    fun resolveDefaultShaderProvider_returnsCorrectProvider() {
        val provider =
            ShaderProviders.resolveDefaultShaderProvider(
                DynamicRange.SDR,
                InputFormat.DEFAULT,
                false,
            )
        val shader = provider.createFragmentShader("s", "v")
        assertThat(shader).contains("samplerExternalOES s")
    }
}
