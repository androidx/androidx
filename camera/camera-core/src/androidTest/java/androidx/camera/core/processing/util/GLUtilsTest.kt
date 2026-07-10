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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GLUtilsTest {

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
    fun createFloatBuffer_returnsCorrectBuffer() {
        val coords = floatArrayOf(1.0f, 2.0f, 3.0f)
        val buffer = GLUtils.createFloatBuffer(coords)
        assertThat(buffer.capacity()).isEqualTo(3)
        assertThat(buffer.get(0)).isEqualTo(1.0f)
        assertThat(buffer.get(1)).isEqualTo(2.0f)
        assertThat(buffer.get(2)).isEqualTo(3.0f)
    }

    @Test
    fun chooseSurfaceAttrib_returnsHlgAttribs_whenHlgSupported() {
        val eglExtensions = "EGL_EXT_gl_colorspace_bt2020_hlg"
        val attribs = GLUtils.chooseSurfaceAttrib(eglExtensions, DynamicRange.HLG_10_BIT)
        assertThat(attribs).isEqualTo(GLUtils.HLG_SURFACE_ATTRIBS)
    }

    @Test
    fun chooseSurfaceAttrib_returnsEmptyAttribs_whenHlgNotSupported() {
        val eglExtensions = ""
        val attribs = GLUtils.chooseSurfaceAttrib(eglExtensions, DynamicRange.HLG_10_BIT)
        assertThat(attribs).isEqualTo(GLUtils.EMPTY_ATTRIBS)
    }

    @Test
    fun getGlVersionNumber_returnsValidVersion() {
        runBlocking(glHandler.asCoroutineDispatcher()) {
            val version = GLUtils.getGlVersionNumber()
            assertThat(version).isNotEqualTo(GLUtils.VERSION_UNKNOWN)
            assertThat(version).matches("\\d+\\.\\d+")
        }
    }

    @Test
    fun checkLocationOrThrow_throws_whenLocationIsNegative() {
        assertThrows(IllegalStateException::class.java) {
            GLUtils.checkLocationOrThrow(-1, "testLabel")
        }
    }

    @Test
    fun checkLocationOrThrow_doesNotThrow_whenLocationIsNonNegative() {
        GLUtils.checkLocationOrThrow(0, "testLabel")
    }

    @Test
    fun loadShader_compilesValidShader() {
        runBlocking(glHandler.asCoroutineDispatcher()) {
            val source = "void main() { gl_FragColor = vec4(1.0); }"
            val shader = GLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, source)
            assertThat(shader).isNotEqualTo(0)
            GLES20.glDeleteShader(shader)
        }
    }

    @Test
    fun loadShader_throwsOnInvalidShader() {
        runBlocking(glHandler.asCoroutineDispatcher()) {
            val source = "invalid shader code"
            assertThrows(IllegalStateException::class.java) {
                GLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, source)
            }
        }
    }
}
