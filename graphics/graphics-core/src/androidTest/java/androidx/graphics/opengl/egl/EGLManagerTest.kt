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
import android.hardware.SyncFence
import android.media.ImageReader
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.hardware.SyncFenceCompat
import androidx.opengl.EGLBindings
import androidx.opengl.EGLExt
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_CLIENT_BUFFER
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_IMAGE_NATIVE_BUFFER
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_NATIVE_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_FOREVER_KHR
import androidx.opengl.EGLExt.Companion.EGL_KHR_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_KHR_IMAGE
import androidx.opengl.EGLExt.Companion.EGL_KHR_IMAGE_BASE
import androidx.opengl.EGLExt.Companion.EGL_KHR_SURFACELESS_CONTEXT
import androidx.opengl.EGLExt.Companion.EGL_SYNC_CONDITION_KHR
import androidx.opengl.EGLExt.Companion.EGL_SYNC_FENCE_KHR
import androidx.opengl.EGLExt.Companion.EGL_SYNC_FLUSH_COMMANDS_BIT_KHR
import androidx.opengl.EGLExt.Companion.EGL_SYNC_NATIVE_FENCE_ANDROID
import androidx.opengl.EGLExt.Companion.EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR
import androidx.opengl.EGLExt.Companion.EGL_SYNC_STATUS_KHR
import androidx.opengl.EGLExt.Companion.EGL_SYNC_TYPE_KHR
import androidx.opengl.EGLSyncKHR
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.TimeUnit
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
@Suppress("AcronymName")
class EGLManagerTest {

    @Test
    fun testInitializeAndRelease() {
        testEGLManager {
            initialize()
            val config = loadConfig(EGLConfigAttributes.RGBA_8888)?.also {
                createContext(it)
            }
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            // Even though EGL v 1.5 was introduced in API level 29 not all devices will advertise
            // support for it. However, all devices should at least support EGL v 1.4
            assertTrue(
                "Unexpected EGL version, received $eglVersion",
                eglVersion == EGLVersion.V14 || eglVersion == EGLVersion.V15
            )
            assertNotNull(eglContext)
            assertNotNull(eglConfig)
        }
    }

    @Test
    fun testMultipleInitializeCallsIgnored() {
        testEGLManager {
            initialize()
            loadConfig(EGLConfigAttributes.RGBA_8888)?.also {
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
        testEGLManager {
            initialize()
            loadConfig(EGLConfigAttributes.RGBA_8888)?.also {
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
        testEGLManager {
            initialize()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EGLConfigAttributes.RGBA_8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }

            createContext(config!!)

            if (isExtensionSupported(EGL_KHR_SURFACELESS_CONTEXT)) {
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
        val wrappedEglSpec = object : EGLSpec by EGLSpec.V14 {
            override fun eglQueryString(nameId: Int): String {
                val queryString = EGLSpec.V14.eglQueryString(nameId)
                return if (nameId == EGL14.EGL_EXTENSIONS) {
                    // Parse the space separated string of EGL extensions into a set
                    val set = HashSet<String>().apply {
                        addAll(queryString.split(' '))
                    }
                    // Remove EglKhrSurfacelessContext if it exists
                    // and repack the set into a space separated string
                    set.remove(EGL_KHR_SURFACELESS_CONTEXT)
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

        testEGLManager(wrappedEglSpec) {
            initialize()

            // Verify that the wrapped EGL spec implementation in fact does not
            // advertise support for EglKhrSurfacelessContext
            assertFalse(isExtensionSupported(EGL_KHR_SURFACELESS_CONTEXT))

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EGLConfigAttributes.RGBA_8888)

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
        testEGLManager {
            initialize()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EGLConfigAttributes.RGBA_8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)

            val pBuffer = eglSpec.eglCreatePBufferSurface(
                config,
                EGLConfigAttributes {
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
        testEGLManager {
            initialize()

            val config = loadConfig(EGLConfigAttributes.RGBA_8888)
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

    private fun EGLSpec.isSingleBufferedSurface(surface: EGLSurface): Boolean {
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
        testEGLManager {
            initialize()
            val config = loadConfig(EGLConfigAttributes.RGBA_8888)
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
        testEGLManager {
            initialize()
            val config = loadConfig(EGLConfigAttributes.RGBA_8888)
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)
            val configAttributes = if (singleBuffered) {
                EGLConfigAttributes {
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
    fun testEGLGetNativeClientBufferANDROIDSupported() {
        testEGLManager {
            initializeWithDefaultConfig()
            val khrImageBaseSupported =
                isExtensionSupported(EGL_KHR_IMAGE_BASE)
            val androidImageNativeBufferSupported =
                isExtensionSupported(EGL_ANDROID_IMAGE_NATIVE_BUFFER)
            val eglClientBufferSupported =
                isExtensionSupported(EGL_ANDROID_CLIENT_BUFFER)
            // According to EGL spec both these extensions are required in order to support
            // eglGetNativeClientBufferAndroid
            if (khrImageBaseSupported &&
                androidImageNativeBufferSupported &&
                eglClientBufferSupported
            ) {
                assertTrue(EGLBindings.nSupportsEglGetNativeClientBufferAndroid())
            }
        }
    }

    @Test
    fun testEglFenceAPIsSupported() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_IMAGE_BASE)) {
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
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_IMAGE_BASE) && isExtensionSupported(
                    EGL_ANDROID_IMAGE_NATIVE_BUFFER)) {
                val hardwareBuffer = HardwareBuffer.create(
                    10,
                    10,
                    PixelFormat.RGBA_8888,
                    1,
                    HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                )
                val image = eglSpec.eglCreateImageFromHardwareBuffer(hardwareBuffer)
                assertNotNull(image)
                assertTrue(eglSpec.eglDestroyImageKHR(image!!))
            }
        }
    }

    @Test
    fun testGlImageTargetTexture2DOESSupported() {
        testEGLManager {
            initializeWithDefaultConfig()
            // According to EGL spec *EITHER* EGL_KHR_image_base or EGL_KHR_image
            // indicate that the eglImageTargetTexture2DOES method is supported on this device
            if (isExtensionSupported(EGL_KHR_IMAGE_BASE) || isExtensionSupported(EGL_KHR_IMAGE)) {
                assertTrue(EGLBindings.nSupportsGlImageTargetTexture2DOES())
            }
        }
    }

    @Test
    fun testEglCreateAndDestroySyncKHRSupported() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                assertTrue(EGLBindings.nSupportsEglCreateSyncKHR())
                assertTrue(EGLBindings.nSupportsEglDestroySyncKHR())
            }
        }
    }

    /**
     * Helper method to determine if both EGLSync fences are supported
     * along with Android platform specific EGLSync fence types
     */
    private fun EGLManager.supportsNativeAndroidFence(): Boolean =
        isExtensionSupported(EGL_KHR_FENCE_SYNC) &&
            isExtensionSupported(EGL_ANDROID_NATIVE_FENCE_SYNC)

    @Test
    fun testEglCreateAndDestroyAndroidFenceSyncKHR() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                assertTrue(
                    eglSpec.eglGetSyncAttribKHR(sync!!, EGL_SYNC_TYPE_KHR, syncAttr, 0))
                assertEquals(EGL_SYNC_NATIVE_FENCE_ANDROID, syncAttr[0])
                assertTrue(eglSpec.eglDestroySyncKHR(sync))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    fun testEGLDupNativeFenceFDMethodLinked() {
        verifyMethodLinked {
            EGLExt.eglDupNativeFenceFDANDROID(EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY),
                EGLSyncKHR(0))
        }
    }

    @Test
    fun testEglCreateSyncAndDestroyKHRMethodLinked() {
        verifyMethodLinked {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val sync = EGLExt.eglCreateSyncKHR(display, EGL_SYNC_FENCE_KHR, null)
            if (sync != null) {
                EGLExt.eglDestroySyncKHR(display, sync)
            }
        }
    }

    @Test
    fun testEglGetSyncAttribMethodLinked() {
        verifyMethodLinked {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val sync = EGLExt.eglCreateSyncKHR(display, EGL_SYNC_FENCE_KHR, null)
            if (sync != null) {
                EGLExt.eglGetSyncAttribKHR(
                    EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY),
                    sync,
                    EGL_SYNC_STATUS_KHR,
                    IntArray(1),
                    0
                )
                EGLExt.eglDestroySyncKHR(display, sync)
            }
        }
    }

    @Test
    fun testEglClientWaitSyncMethodLinked() {
        verifyMethodLinked {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val sync = EGLExt.eglCreateSyncKHR(display, EGL_SYNC_FENCE_KHR, null)
            if (sync != null) {
                EGLExt.eglClientWaitSyncKHR(
                    display,
                    sync,
                    EGL_SYNC_FLUSH_COMMANDS_BIT_KHR,
                    EGL_FOREVER_KHR
                )
                EGLExt.eglDestroySyncKHR(display, sync)
            }
        }
    }

    private inline fun verifyMethodLinked(crossinline block: () -> Unit) {
        testEGLManager {
            initializeWithDefaultConfig()
            try {
                block()
            } catch (exception: UnsatisfiedLinkError) {
                fail("Unable to resolve method: " + exception.message)
            } catch (exception: Exception) {
                // We only care about unsatisfied link errors. If the device does not support this
                // exception we do not care in this test case
            }
        }
    }

    @Test
    fun testEglDupNativeFenceFDANDROIDSupported() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                assertTrue(EGLBindings.nSupportsDupNativeFenceFDANDROID())
            }
        }
    }

    @Test
    fun testEglCreateAndDestroyFenceSyncKHR() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_FENCE_KHR, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                assertTrue(
                    eglSpec.eglGetSyncAttribKHR(sync!!, EGL_SYNC_TYPE_KHR, syncAttr, 0))
                assertEquals(EGL_SYNC_FENCE_KHR, syncAttr[0])
                assertTrue(
                    eglSpec.eglGetSyncAttribKHR(
                        sync,
                        EGL_SYNC_CONDITION_KHR,
                        syncAttr,
                        0
                    )
                )
                assertEquals(EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR, syncAttr[0])
                assertTrue(eglSpec.eglDestroySyncKHR(sync))
            }
        }
    }

    @Test
    fun testEglGetSyncAttribKHROutOfBounds() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_FENCE_KHR, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                try {
                    assertFalse(
                        eglSpec.eglGetSyncAttribKHR(
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
                    assertTrue(eglSpec.eglDestroySyncKHR(sync))
                }
            }
        }
    }

    @Test
    fun testEglGetSyncAttribKHRNegativeOffset() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_FENCE_KHR, null)
                assertNotNull(sync)
                val syncAttr = IntArray(1)
                try {
                    assertFalse(
                        eglSpec.eglGetSyncAttribKHR(
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
                    assertTrue(eglSpec.eglDestroySyncKHR(sync))
                }
            }
        }
    }

    @Test
    fun testEglClientWaitSyncKHR() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (isExtensionSupported(EGL_KHR_FENCE_SYNC)) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_FENCE_KHR, null)
                assertNotNull(sync)

                assertEquals("eglCreateSync failed", EGL14.EGL_SUCCESS, eglSpec.eglGetError())

                GLES20.glFlush()
                assertEquals("glFlush failed", GLES20.GL_NO_ERROR, GLES20.glGetError())

                val status = eglSpec.eglClientWaitSyncKHR(
                    sync!!,
                    0,
                    EGL_FOREVER_KHR
                )
                assertEquals("eglClientWaitSync failed",
                    EGLExt.EGL_CONDITION_SATISFIED_KHR, status)
                assertEquals("eglClientWaitSyncKHR failed", EGL14.EGL_SUCCESS, EGL14.eglGetError())
                assertTrue(eglSpec.eglDestroySyncKHR(sync))
                assertEquals("eglDestroySyncKHR failed", EGL14.EGL_SUCCESS, EGL14.eglGetError())
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    fun testEglDupNativeFenceFDANDROID() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)

                assertEquals("eglCreateSyncFailed", EGL14.EGL_SUCCESS, eglSpec.eglGetError())

                GLES20.glFlush()
                assertEquals("glFlush failed", GLES20.GL_NO_ERROR, GLES20.glGetError())

                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val syncFence = EGLExt.eglDupNativeFenceFDANDROID(display, sync!!)
                assertTrue(syncFence.isValid())
                assertTrue(syncFence.await(TimeUnit.MILLISECONDS.toNanos(3000)))

                assertTrue(eglSpec.eglDestroySyncKHR(sync))
                assertEquals("eglDestroySyncKHR failed", EGL14.EGL_SUCCESS, EGL14.eglGetError())
                syncFence.close()
                assertFalse(syncFence.isValid())
            }
        }
    }
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testEglDupNativeFenceFDANDROIDawaitForever() {
        testEGLManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                val sync = eglSpec.eglCreateSyncKHR(EGL_SYNC_NATIVE_FENCE_ANDROID, null)
                assertNotNull(sync)

                assertEquals("eglCreateSync failed", EGL14.EGL_SUCCESS, eglSpec.eglGetError())

                GLES20.glFlush()
                assertEquals("glFlush failed", GLES20.GL_NO_ERROR, GLES20.glGetError())

                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val syncFence = EGLExt.eglDupNativeFenceFDANDROID(display, sync!!)
                assertTrue(syncFence.isValid())
                assertNotEquals(SyncFenceCompat.SIGNAL_TIME_INVALID, syncFence.getSignalTimeNanos())
                assertTrue(syncFence.awaitForever())

                assertTrue(eglSpec.eglDestroySyncKHR(sync))
                assertEquals("eglDestroySyncKHR failed", EGL14.EGL_SUCCESS, EGL14.eglGetError())
                syncFence.close()
                assertFalse(syncFence.isValid())
                assertEquals(SyncFence.SIGNAL_TIME_INVALID, syncFence.getSignalTimeNanos())
            }
        }
    }

    @Test
    fun testSignedForeverConstantMatchesNDK() {
        assertTrue(EGLBindings.nEqualToNativeForeverTimeout(EGL_FOREVER_KHR))
    }

    // Helper method used in testing to initialize EGL and default
    // EGLConfig to the ARGB8888 configuration
    private fun EGLManager.initializeWithDefaultConfig() {
        initialize()
        val config = loadConfig(EGLConfigAttributes.RGBA_8888)
        if (config == null) {
            fail("Config 8888 should be supported")
        }
        createContext(config!!)
    }

    /**
     * Helper method to ensure EglManager has the corresponding release calls
     * made to it and verifies that no exceptions were thrown as part of the test.
     */
    private fun testEGLManager(
        eglSpec: EGLSpec = EGLSpec.V14,
        block: EGLManager.() -> Unit = {}
    ) {
        with(EGLManager(eglSpec)) {
            assertEquals(EGLVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
            block()
            release()
            assertEquals(EGLVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
        }
    }
}
