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
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.opengl.EGL14
import android.opengl.EGL15
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLUtils.Companion.EGL_SYNC_CONDITION_KHR
import androidx.graphics.opengl.egl.EGLUtils.Companion.EGL_SYNC_FENCE_KHR
import androidx.graphics.opengl.egl.EGLUtils.Companion.EGL_SYNC_NATIVE_FENCE_ANDROID
import androidx.graphics.opengl.egl.EGLUtils.Companion.EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR
import androidx.graphics.opengl.egl.EGLUtils.Companion.EGL_SYNC_TYPE_KHR
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class EglManagerTest {

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

    private fun EglSpec.isSingleBufferedSurface(surface: EGLSurface): Boolean {
        return if (surface == EGL14.EGL_NO_SURFACE) {
            false
        } else {
            val result = IntArray(1)
            val queryResult = eglQuerySurface(
                surface, EGL14.EGL_RENDER_BUFFER, result, 0)
            queryResult && result[0] == EGL14.EGL_SINGLE_BUFFER
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    fun testSurfaceContentsWithBackBuffer() {
        verifySurfaceContentsWithWindowConfig()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    fun testSurfaceContentsWithFrontBuffer() {
        verifySurfaceContentsWithWindowConfig(true)
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun verifySurfaceContentsWithWindowConfig(
        singleBuffered: Boolean = false
    ) {
        testEglManager {
            initialize()
            val config = loadConfig(EglConfigAttributes8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)

            val width = 8
            val height = 5
            val targetColor = Color.RED
            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
            var canRender = false

            thread {
                canRender = drawSurface(imageReader.surface, targetColor, singleBuffered)
            }.join()

            try {
                if (canRender) {
                    val image = imageReader.acquireLatestImage()
                    val plane = image.planes[0]
                    assertEquals(4, plane.pixelStride)

                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    var offset = 0
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val red = plane.buffer[offset].toInt() and 0xff
                            val green = plane.buffer[offset + 1].toInt() and 0xff
                            val blue = plane.buffer[offset + 2].toInt() and 0xff
                            val alpha = plane.buffer[offset + 3].toInt() and 0xff
                            val packedColor = Color.argb(alpha, red, green, blue)
                            assertEquals("Index: " + x + ", " + y, targetColor, packedColor)
                            offset += pixelStride
                        }
                        offset += rowPadding
                    }
                }
            } finally {
                imageReader.close()
                release()
            }
        }
    }

    private fun drawSurface(
        surface: Surface,
        color: Int,
        singleBuffered: Boolean
    ): Boolean {
        var canRender = false
        testEglManager {
            initialize()
            val config = loadConfig(EglConfigAttributes8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)
            val configAttributes = if (singleBuffered) {
                EglConfigAttributes {
                    EGL14.EGL_RENDER_BUFFER to EGL14.EGL_SINGLE_BUFFER
                }
            } else {
                null
            }
            val eglSurface = eglSpec.eglCreateWindowSurface(config, surface, configAttributes)
            // Skip tests of the device does not support EGL_SINGLE_BUFFER
            canRender = !singleBuffered || eglSpec.isSingleBufferedSurface(eglSurface)
            if (canRender) {
                makeCurrent(eglSurface)
                assertEquals("Make current failed", EGL14.EGL_SUCCESS, eglSpec.eglGetError())
                GLES20.glClearColor(
                    Color.red(color) / 255f,
                    Color.green(color) / 255f,
                    Color.blue(color) / 255f,
                    Color.alpha(color) / 255f
                )
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                swapAndFlushBuffers()
                assertEquals("Swapbuffers failed", EGL14.EGL_SUCCESS, eglSpec.eglGetError())
            }

            eglSpec.eglDestroySurface(eglSurface)
            release()
        }
        return canRender
    }

    @Test
    fun testEglGetNativeClientBufferANDROIDSupported() {
        testEglManager {
            initializeWithDefaultConfig()
            val khrImageBaseSupported =
                isExtensionSupported(EglKhrImageBase)
            val androidImageNativeBufferSupported =
                isExtensionSupported(EglAndroidImageNativeBuffer)
            // According to EGL spec both these extensions are required in order to support
            // eglGetNativeClientBufferAndroid
            if (khrImageBaseSupported && androidImageNativeBufferSupported) {
                assertTrue(EGLBindings.nSupportsEglGetNativeClientBufferAndroid())
            }
        }
    }

    @Test
    fun testEglFenceAPIsSupported() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrImageBase)) {
                assertTrue(EGLBindings.nSupportsEglCreateImageKHR())
                assertTrue(EGLBindings.nSupportsEglClientWaitSyncKHR())
                assertTrue(EGLBindings.nSupportsEglGetSyncAttribKHR())
                assertTrue(EGLBindings.nSupportsEglDestroyImageKHR())
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testEglCreateAndDestroyImageKHR() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrImageBase)) {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val hardwareBuffer = HardwareBuffer.create(
                    10,
                    10,
                    PixelFormat.RGBA_8888,
                    1,
                    HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                )
                val image = EGLUtils.eglCreateImageFromHardwareBuffer(display, hardwareBuffer)
                assertNotNull(image)
                assertTrue(EGLUtils.eglDestroyImageKHR(display, image!!))
            }
        }
    }

    @Test
    fun testGlImageTargetTexture2DOESSupported() {
        testEglManager {
            initializeWithDefaultConfig()
            // According to EGL spec *EITHER* EGL_KHR_image_base or EGL_KHR_image
            // indicate that the eglImageTargetTexture2DOES method is supported on this device
            if (isExtensionSupported(EglKhrImageBase) || isExtensionSupported(EglKhrImage)) {
                assertTrue(EGLBindings.nSupportsGlImageTargetTexture2DOES())
            }
        }
    }

    @Test
    fun testEglCreateAndDestroySyncKHRSupported() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrFenceSync)) {
                assertTrue(EGLBindings.nSupportsEglCreateSyncKHR())
                assertTrue(EGLBindings.nSupportsEglDestroySyncKHR())
            }
        }
    }

    @Test
    fun testEglCreateAndDestroyAndroidFenceSyncKHR() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrFenceSync)) {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val sync = EGLUtils.eglCreateSyncKHR(display,
                    EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                assertTrue(
                    EGLUtils.eglGetSyncAttribKHR(display, sync!!, EGL_SYNC_TYPE_KHR, syncAttr, 0))
                assertEquals(EGL_SYNC_NATIVE_FENCE_ANDROID, syncAttr[0])
                assertTrue(EGLUtils.eglDestroySyncKHR(display, sync))
            }
        }
    }

    @Test
    fun testEglCreateAndDestroyFenceSyncKHR() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrFenceSync)) {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val sync = EGLUtils.eglCreateSyncKHR(display, EGL_SYNC_FENCE_KHR, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                assertTrue(
                    EGLUtils.eglGetSyncAttribKHR(display, sync!!, EGL_SYNC_TYPE_KHR, syncAttr, 0))
                assertEquals(EGL_SYNC_FENCE_KHR, syncAttr[0])
                assertTrue(
                    EGLUtils.eglGetSyncAttribKHR(
                        display,
                        sync,
                        EGL_SYNC_CONDITION_KHR,
                        syncAttr,
                        0
                    )
                )
                assertEquals(EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR, syncAttr[0])
                assertTrue(EGLUtils.eglDestroySyncKHR(display, sync))
            }
        }
    }

    @Test
    fun testEglGetSyncAttribKHROutOfBounds() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrFenceSync)) {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val sync = EGLUtils.eglCreateSyncKHR(display, EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                try {
                    assertFalse(
                        EGLUtils.eglGetSyncAttribKHR(
                            display,
                            sync!!,
                            EGL_SYNC_TYPE_KHR,
                            syncAttr,
                            1
                        )
                    )
                    fail("Should have thrown for array out of bounds exception")
                } catch (_: IllegalArgumentException) {
                    // NO-OP
                }

                if (sync != null) {
                    assertTrue(EGLUtils.eglDestroySyncKHR(display, sync))
                }
            }
        }
    }

    @Test
    fun testEglGetSyncAttribKHRNegativeOffset() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrFenceSync)) {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val sync = EGLUtils.eglCreateSyncKHR(display, EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                try {
                    assertFalse(
                        EGLUtils.eglGetSyncAttribKHR(
                            display,
                            sync!!,
                            EGL_SYNC_TYPE_KHR,
                            syncAttr,
                            -1
                        )
                    )
                    fail("Should have thrown for negative offset into attributes array")
                } catch (_: IllegalArgumentException) {
                    // NO-OP
                }

                if (sync != null) {
                    assertTrue(EGLUtils.eglDestroySyncKHR(display, sync))
                }
            }
        }
    }

    @Test
    fun testEglClientWaitSyncKHR() {
        testEglManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EglKhrFenceSync)) {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val sync = EGLUtils.eglCreateSyncKHR(display, EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)

                var error = eglSpec.eglGetError()
                if (error != EGL14.EGL_SUCCESS) {
                    throw RuntimeException("eglCreateSync failed")
                }

                GLES20.glFlush()
                error = GLES20.glGetError()
                if (error != GLES20.GL_NO_ERROR) {
                    throw RuntimeException("glFlush failed")
                }

                val status = EGLUtils.eglClientWaitSyncKHR(
                    display,
                    sync!!,
                    0,
                    EGLUtils.EGL_FOREVER_KHR
                )
                if (status != EGL15.EGL_CONDITION_SATISFIED) {
                    throw RuntimeException("eglClientWaitSync failed")
                }
                assertEquals("eglClientWaitSyncKHR failed", EGL14.EGL_SUCCESS, EGL14.eglGetError())
                assertTrue(EGLUtils.eglDestroySyncKHR(display, sync))
                assertEquals("eglDestroySyncKHR failed", EGL14.EGL_SUCCESS, EGL14.eglGetError())
            }
        }
    }

    @Test
    fun testSignedForeverConstantMatchesNDK() {
        assertTrue(EGLBindings.nEqualToNativeForeverTimeout(EGLUtils.EGL_FOREVER_KHR))
    }

    // Helper method used in testing to initialize EGL and default
    // EGLConfig to the ARGB8888 configuration
    private fun EglManager.initializeWithDefaultConfig() {
        initialize()
        val config = loadConfig(EglConfigAttributes8888)
        if (config == null) {
            fail("Config 8888 should be supported")
        }
        createContext(config!!)
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