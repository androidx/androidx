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

import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
@SmallTest
class EglManagerTest {

    val GL_TEXTURE_EXTERNAL_OES = 0x8D65

    @Test
    fun testInitializeAndRelease() {
        testEglManager {
            initialize()
            val config = loadConfig(EglConfigAttributes8888)?.also {
                createContext(it)
            }
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            // Even though EGL v 1.5 was introduced in API level 29 not all devices will advertise
            // support for it. However, all devices should at least support EGL v 1.4
            assertTrue(
                "Unexpected EGL version, received $eglVersion",
                eglVersion == EglVersion.V14 || eglVersion == EglVersion.V15
            )
            assertNotNull(eglContext)
            assertNotNull(eglConfig)
        }
    }

    @Test
    fun testMultipleInitializeCallsIgnored() {
        testEglManager {
            initialize()
            loadConfig(EglConfigAttributes8888)?.also {
                createContext(it)
            }
            val currentContext = eglContext
            val currentConfig = eglConfig
            assertNotEquals(EGL14.EGL_NO_CONTEXT, currentContext)
            // Subsequent calls to initialize should be ignored
            // and the current EglContext should be the same as the previous call
            initialize()
            assertTrue(currentContext === eglContext)
            assertTrue(currentConfig === eglConfig)
        }
    }

    @Test
    fun testMultipleReleaseCallsIgnored() {
        testEglManager {
            initialize()
            loadConfig(EglConfigAttributes8888)?.also {
                createContext(it)
            }
            // Multiple attempts to release should act as no-ops, i.e. we should not crash
            // and the corresponding context should be nulled out
            release()
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)

            release()
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
        }
    }

    @Test
    fun testDefaultSurface() {
        testEglManager {
            initialize()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EglConfigAttributes8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }

            createContext(config!!)

            if (isExtensionSupported(EglKhrSurfacelessContext)) {
                assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            } else {
                assertNotEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            }

            assertEquals(currentDrawSurface, defaultSurface)
            assertEquals(currentReadSurface, defaultSurface)

            release()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)
        }
    }

    @Test
    fun testDefaultSurfaceWithoutSurfacelessContext() {
        // Create a new EGL Spec instance that does not support the
        // EglKhrSurfacelessContext extension in order to verify
        // the fallback support of initializing the current surface
        // to a PBuffer instead of EGL14.EGL_NO_SURFACE
        val wrappedEglSpec = object : EglSpec by EglSpec.Egl14 {
            override fun eglQueryString(nameId: Int): String {
                val queryString = EglSpec.Egl14.eglQueryString(nameId)
                return if (nameId == EGL14.EGL_EXTENSIONS) {
                    // Parse the space separated string of EGL extensions into a set
                    val set = HashSet<String>().apply {
                        addAll(queryString.split(' '))
                    }
                    // Remove EglKhrSurfacelessContext if it exists
                    // and repack the set into a space separated string
                    set.remove(EglKhrSurfacelessContext)
                    StringBuilder().let {
                        for (entry in set) {
                            it.append(entry)
                            it.append(' ')
                        }
                        it.toString()
                    }
                } else {
                    queryString
                }
            }
        }

        testEglManager(wrappedEglSpec) {
            initialize()

            // Verify that the wrapped EGL spec implementation in fact does not
            // advertise support for EglKhrSurfacelessContext
            assertFalse(isExtensionSupported(EglKhrSurfacelessContext))

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EglConfigAttributes8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }

            // Create context at this point should fallback of eglCreatePBufferSurface
            // instead of EGL_NO_SURFACE as a result of no longer advertising support
            // for EglKhrSurfacelessContext
            createContext(config!!)

            assertNotEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, defaultSurface)
            assertEquals(currentReadSurface, defaultSurface)

            release()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)
        }
    }

    @Test
    fun testCreatePBufferSurface() {
        testEglManager {
            initialize()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EglConfigAttributes8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)

            val pBuffer = eglSpec.eglCreatePBufferSurface(
                config,
                EglConfigAttributes {
                    EGL14.EGL_WIDTH to 1
                    EGL14.EGL_HEIGHT to 1
                })

            makeCurrent(pBuffer)

            assertNotEquals(EGL14.EGL_NO_SURFACE, currentReadSurface)
            assertNotEquals(EGL14.EGL_NO_SURFACE, currentDrawSurface)
            assertNotEquals(EGL14.EGL_NO_SURFACE, pBuffer)

            assertEquals(pBuffer, currentReadSurface)
            assertEquals(pBuffer, currentDrawSurface)

            eglSpec.eglDestroySurface(pBuffer)
            release()
        }
    }

    @Test
    fun testCreateWindowSurfaceDefault() {
        testEglManager {
            initialize()

            val config = loadConfig(EglConfigAttributes8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }

            createContext(config!!)

            val surface = Surface(SurfaceTexture(42))
            // Create a window surface with the default attributes
            val eglSurface = eglSpec.eglCreateWindowSurface(config, surface, null)
            assertNotEquals(EGL14.EGL_NO_SURFACE, eglSurface)
            eglSpec.eglDestroySurface(eglSurface)

            release()
        }
    }

    // Requires single buffered SurfaceTexture
    // Attempts to create an EGLSurface that is single buffered
    // always will return a double buffered EGLSurface instead
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    fun testCreateWindowSurfaceWithFrontBuffer() {
        testEglManager {
            initialize()

            val config = loadConfig(EglConfigAttributes8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }

            createContext(config!!)

            val surface = Surface(SurfaceTexture(42, true))

            val attrs = EglConfigAttributes {
                EGL14.EGL_RENDER_BUFFER to EGL14.EGL_SINGLE_BUFFER
            }
            // Create a window surface with the default attributes
            val eglSurface = eglSpec.eglCreateWindowSurface(config, surface, attrs)
            assertNotEquals(EGL14.EGL_NO_SURFACE, eglSurface)
            val result = IntArray(1)
            val queryResult = eglSpec.eglQuerySurface(
                eglSurface, EGL14.EGL_RENDER_BUFFER, result, 0)

            assertTrue(queryResult)
            assertEquals(EGL14.EGL_SINGLE_BUFFER, result[0])

            eglSpec.eglDestroySurface(eglSurface)

            release()
        }
    }

    @Test
    fun testSurfaceContentsWithBackBuffer() {
        verifySurfaceContentsWithWindowConfig()
    }

    @Test
    fun testSurfaceContentsWithFrontBuffer() {
        verifySurfaceContentsWithWindowConfig(
            EglConfigAttributes {
                EGL14.EGL_RENDER_BUFFER to EGL14.EGL_SINGLE_BUFFER
            }
        )
    }

    private fun verifySurfaceContentsWithWindowConfig(
        eglConfigAttributes: EglConfigAttributes? = null
    ) {
        testEglManager {
            initialize()
            val config = loadConfig(EglConfigAttributes8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)
            val srcTexName = IntArray(1).let {
                GLES20.glGenTextures(1, it, 0)
                it[0]
            }

            val fboName = IntArray(1).let {
                GLES20.glGenFramebuffers(1, it, 0)
                it[0]
            }

            val width = 8
            val height = 5
            val targetColor = Color.RED
            val surfaceTexture = SurfaceTexture(srcTexName).apply {
                setDefaultBufferSize(width, height)
            }
            val surface = Surface(surfaceTexture)
            thread {
                drawSurface(surface, targetColor, eglConfigAttributes)
            }.join()

            surfaceTexture.updateTexImage()

            // Android API level 17 emulators do not support GL_TEXTURE_EXTERNAL_OES
            // so fall back on GL_TEXTURE_2D instead
            val extensionSupported = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1
            val texType = if (extensionSupported) GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D

            createFbo(srcTexName, fboName, texType)
            verifyPixels(width, height, targetColor)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            release()
        }
    }

    private fun createFbo(texName: Int, fboName: Int, texType: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboName)
        verify("glBindFrame buffer success")
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            texType, texName, 0)
        verify("glFrameBufferTexture2d success")
        val frameBufferResult = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        assertEquals(GLES20.GL_FRAMEBUFFER_COMPLETE, frameBufferResult)
    }

    private fun verify(msg: String) {
        val error = GLES20.glGetError()
        assertEquals("$msg error: $error", GLES20.GL_NO_ERROR, GLES20.glGetError())
    }

    private fun verifyPixels(width: Int, height: Int, targetColor: Int) {
        val capacity = width * height * 4

        val buffer = ByteBuffer.allocateDirect(capacity)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)

        for (i in 0 until width * height step 4) {
            val red = buffer[i].toInt() and 0xff
            val green = buffer[i + 1].toInt() and 0xff
            val blue = buffer[i + 2].toInt() and 0xff
            val alpha = buffer[i + 3].toInt() and 0xff
            assertEquals(targetColor, Color.argb(alpha, red, green, blue))
        }
    }

    private fun drawSurface(
        surface: Surface,
        color: Int,
        configAttributes: EglConfigAttributes? = null
    ) {
        testEglManager {
            initialize()
            val config = loadConfig(EglConfigAttributes8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)

            val eglSurface = eglSpec.eglCreateWindowSurface(config, surface, configAttributes)
            makeCurrent(eglSurface)
            GLES20.glClearColor(
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                Color.alpha(color) / 255f
            )
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            swapAndFlushBuffers()

            eglSpec.eglDestroySurface(eglSurface)
            release()
        }
    }

    /**
     * Helper method to ensure EglManager has the corresponding release calls
     * made to it and verifies that no exceptions were thrown as part of the test.
     */
    private fun testEglManager(
        eglSpec: EglSpec = EglSpec.Egl14,
        block: EglManager.() -> Unit = {}
    ) {
        with(EglManager(eglSpec)) {
            assertEquals(EglVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
            block()
            release()
            assertEquals(EglVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
        }
    }
}