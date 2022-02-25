/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.opengl

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.media.Image
import android.media.ImageReader
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EglManager
import androidx.graphics.opengl.egl.EglSpec
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GLRendererTest {

    @Test
    fun testStartAfterStop() {
        with(GLRenderer()) {
            start("thread1")
            stop(true)
            start("thread2")
            stop(true)
        }
    }

    @Test
    fun testAttachBeforeStartThrows() {
        try {
            with(GLRenderer()) {
                attach(
                    Surface(SurfaceTexture(17)),
                    10,
                    10,
                    object : GLRenderer.RenderCallback {
                    override fun onDrawFrame(eglManager: EglManager) {
                        // NO-OP
                    }
                })
            }
            fail("Start should be called first")
        } catch (exception: IllegalStateException) {
            // Success, attach before call to start should fail
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    fun testRender() {
        val latch = CountDownLatch(1)
        val renderer = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EglManager) {
                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val width = 5
        val height = 8
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        val glRenderer = GLRenderer()
        glRenderer.start()

        val target = glRenderer.attach(reader.surface, width, height, renderer)
        target.requestRender {
            latch.countDown()
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        val plane = reader.acquireLatestImage().planes[0]
        assertEquals(4, plane.pixelStride)

        val targetColor = Color.argb(255, 255, 0, 255)
        Api19Helpers.verifyPlaneContent(width, height, plane, targetColor)

        target.detach(true)

        glRenderer.stop(true)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    fun testDetachExecutesPendingRequests() {
        val latch = CountDownLatch(1)
        val renderer = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EglManager) {
                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val width = 5
        val height = 8
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        val glRenderer = GLRenderer()
        glRenderer.start()

        val target = glRenderer.attach(reader.surface, width, height, renderer)
        target.requestRender {
            latch.countDown()
        }
        target.detach(false) // RequestRender Call should still execute

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        val plane = reader.acquireLatestImage().planes[0]
        assertEquals(4, plane.pixelStride)

        val targetColor = Color.argb(255, 255, 0, 255)
        Api19Helpers.verifyPlaneContent(width, height, plane, targetColor)

        glRenderer.stop(true)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    fun testStopExecutesPendingRequests() {
        val latch = CountDownLatch(1)
        val surfaceWidth = 5
        val surfaceHeight = 8
        val renderer = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EglManager) {
                val size = eglManager.eglSpec.querySurfaceSize(eglManager.currentDrawSurface)
                assertEquals(surfaceWidth, size.width)
                assertEquals(surfaceHeight, size.height)
                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val reader = ImageReader.newInstance(surfaceWidth, surfaceHeight, PixelFormat.RGBA_8888, 1)
        val glRenderer = GLRenderer()
        glRenderer.start()

        val target = glRenderer.attach(reader.surface, surfaceWidth, surfaceHeight, renderer)
        target.requestRender {
            latch.countDown()
        }
        glRenderer.stop(false) // RequestRender call should still execute

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        val plane = reader.acquireLatestImage().planes[0]
        assertEquals(4, plane.pixelStride)

        val targetColor = Color.argb(255, 255, 0, 255)
        Api19Helpers.verifyPlaneContent(surfaceWidth, surfaceHeight, plane, targetColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    fun testDetachExecutesMultiplePendingRequests() {
        val numRenders = 4
        val latch = CountDownLatch(numRenders)
        val renderCount = AtomicInteger(0)
        val renderer = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EglManager) {
                var red: Float = 0f
                var green: Float = 0f
                var blue: Float = 0f
                when (renderCount.get()) {
                    1 -> {
                        red = 1f
                    }
                    2 -> {
                        green = 1f
                    }
                    3 -> {
                        blue = 1f
                    }
                }
                GLES20.glClearColor(red, green, blue, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val width = 5
        val height = 8
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        val glRenderer = GLRenderer()
        glRenderer.start()

        val target = glRenderer.attach(reader.surface, width, height, renderer)
        // Issuing multiple requestRender calls to ensure each of them are
        // executed even when a detach call is made
        repeat(numRenders) {
            target.requestRender {
                renderCount.incrementAndGet()
                latch.countDown()
            }
        }

        target.detach(false) // RequestRender calls should still execute

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(numRenders, renderCount.get())
        val plane = reader.acquireLatestImage().planes[0]
        assertEquals(4, plane.pixelStride)

        val targetColor = Color.argb(255, 0, 0, 255)
        Api19Helpers.verifyPlaneContent(width, height, plane, targetColor)

        glRenderer.stop(true)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    fun testDetachCancelsPendingRequests() {
        val latch = CountDownLatch(1)
        val renderer = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EglManager) {
                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val width = 5
        val height = 8
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        val glRenderer = GLRenderer()
        glRenderer.start()

        val target = glRenderer.attach(reader.surface, width, height, renderer)
        target.requestRender {
            latch.countDown()
        }
        target.detach(false) // RequestRender Call should be cancelled

        glRenderer.stop(true)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    fun testMultipleAttachedSurfaces() {
        val latch = CountDownLatch(2)
        val renderer1 = object : GLRenderer.RenderCallback {

            override fun onDrawFrame(eglManager: EglManager) {
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val renderer2 = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EglManager) {
                GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val width1 = 6
        val height1 = 7

        val width2 = 11
        val height2 = 23
        val reader1 = ImageReader.newInstance(width1, height1, PixelFormat.RGBA_8888, 1)

        val reader2 = ImageReader.newInstance(width2, height2, PixelFormat.RGBA_8888, 1)

        val glRenderer = GLRenderer()
        glRenderer.start()

        val target1 = glRenderer.attach(reader1.surface, width1, height1, renderer1)
        val target2 = glRenderer.attach(reader2.surface, width2, height2, renderer2)
        target1.requestRender {
            latch.countDown()
        }
        target2.requestRender {
            latch.countDown()
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        val plane1 = reader1.acquireLatestImage().planes[0]
        val plane2 = reader2.acquireLatestImage().planes[0]

        Api19Helpers.verifyPlaneContent(width1, height1, plane1, Color.argb(255, 255, 0, 0))
        Api19Helpers.verifyPlaneContent(width2, height2, plane2, Color.argb(255, 0, 0, 255))

        target1.detach(true)
        target2.detach(true)

        val attachLatch = CountDownLatch(1)
        glRenderer.stop(true) {
            attachLatch.countDown()
        }

        assertTrue(attachLatch.await(3000, TimeUnit.MILLISECONDS))
    }

    /**
     * Helper class for test methods that refer to APIs that may not exist on earlier API levels.
     * This must be broken out into a separate class instead of being defined within the
     * test class as the test runner will inspect all methods + parameter types in advance.
     * If a parameter type does not exist on a particular API level, it will crash even if
     * there are corresponding @SdkSuppress and @RequiresApi
     * See https://b.corp.google.com/issues/221485597
     */
    class Api19Helpers private constructor() {
        companion object {
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            fun verifyPlaneContent(width: Int, height: Int, plane: Image.Plane, targetColor: Int) {
                val rowPadding = plane.rowStride - plane.pixelStride * width
                var offset = 0
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val red = plane.buffer[offset].toInt() and 0xff
                        val green = plane.buffer[offset + 1].toInt() and 0xff
                        val blue = plane.buffer[offset + 2].toInt() and 0xff
                        val alpha = plane.buffer[offset + 3].toInt() and 0xff
                        val packedColor = Color.argb(alpha, red, green, blue)
                        assertEquals("Index: $x, $y", targetColor, packedColor)
                        offset += plane.pixelStride
                    }
                    offset += rowPadding
                }
            }
        }
    }

    @Test
    fun testNonStartedGLRendererIsNotRunning() {
        assertFalse(GLRenderer().isRunning())
    }

    @Test
    fun testRepeatedStartAndStopRunningState() {
        val glRenderer = GLRenderer()
        assertFalse(glRenderer.isRunning())
        glRenderer.start()
        assertTrue(glRenderer.isRunning())
        glRenderer.stop(true)
        assertFalse(glRenderer.isRunning())
        glRenderer.start()
        assertTrue(glRenderer.isRunning())
        glRenderer.stop(true)
        assertFalse(glRenderer.isRunning())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun testSurfaceViewAttach() {
        withGLTestActivity {
            assertNotNull(surfaceView)

            val latch = CountDownLatch(1)
            val glRenderer = GLRenderer().apply { start() }
            val target = glRenderer.attach(surfaceView, ColorRenderCallback(Color.BLUE))

            target.requestRender {
                latch.countDown()
            }

            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))

            val bitmap = Bitmap.createBitmap(
                GLTestActivity.TARGET_WIDTH,
                GLTestActivity.TARGET_HEIGHT,
                Bitmap.Config.ARGB_8888
            )

            blockingPixelCopy(bitmap) { surfaceView.holder.surface }

            assertTrue(bitmap.isAllColor(Color.BLUE))

            val stopLatch = CountDownLatch(1)
            glRenderer.stop(true) {
                stopLatch.countDown()
            }

            assertTrue(stopLatch.await(3000, TimeUnit.MILLISECONDS))
            // Assert that targets are detached when the GLRenderer is stopped
            assertFalse(target.isAttached())
        }
    }

    @Test
    fun testTextureViewOnResizeCalled() {
        withGLTestActivity {
            assertNotNull(textureView)
            val glRenderer = GLRenderer().apply { start() }

            val resizeLatch = CountDownLatch(1)
            val target = glRenderer.attach(textureView, object : GLRenderer.RenderCallback {
                override fun onDrawFrame(eglManager: EglManager) {
                    val size = eglManager.eglSpec.querySurfaceSize(eglManager.currentDrawSurface)
                    assertTrue(size.width > 0)
                    assertTrue(size.height > 0)
                    resizeLatch.countDown()
                }
            })
            target.requestRender()

            assertTrue(resizeLatch.await(3000, TimeUnit.MILLISECONDS))

            val detachLatch = CountDownLatch(1)
            target.detach(false) {
                detachLatch.countDown()
            }
            assertTrue(detachLatch.await(3000, TimeUnit.MILLISECONDS))
            glRenderer.stop(true)
        }
    }

    @Test
    fun testSurfaceViewOnResizeCalled() {
        withGLTestActivity {
            assertNotNull(surfaceView)
            val glRenderer = GLRenderer().apply { start() }

            val resizeLatch = CountDownLatch(1)
            val target = glRenderer.attach(surfaceView, object : GLRenderer.RenderCallback {
                override fun onDrawFrame(eglManager: EglManager) {
                    val size = eglManager.eglSpec.querySurfaceSize(eglManager.currentDrawSurface)
                    assertTrue(size.width > 0)
                    assertTrue(size.height > 0)
                    resizeLatch.countDown()
                }
            })
            target.requestRender()

            assertTrue(resizeLatch.await(3000, TimeUnit.MILLISECONDS))

            val detachLatch = CountDownLatch(1)
            target.detach(false) {
                detachLatch.countDown()
            }
            assertTrue(detachLatch.await(3000, TimeUnit.MILLISECONDS))
            glRenderer.stop(true)
        }
    }

    data class Size(val width: Int, val height: Int)

    fun EglSpec.querySurfaceSize(eglSurface: EGLSurface): Size {
        val result = IntArray(1)
        eglQuerySurface(
            eglSurface, EGL14.EGL_WIDTH, result, 0)
        val width = result[0]
        eglQuerySurface(
            eglSurface, EGL14.EGL_HEIGHT, result, 0)
        val height = result[0]
        return Size(width, height)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun testTextureViewAttach() {
        withGLTestActivity {
            assertNotNull(textureView)

            val latch = CountDownLatch(1)
            val glRenderer = GLRenderer().apply { start() }
            val target = glRenderer.attach(textureView, ColorRenderCallback(Color.BLUE))
            target.requestRender {
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))

            val bitmap = Bitmap.createBitmap(
                GLTestActivity.TARGET_WIDTH,
                GLTestActivity.TARGET_HEIGHT,
                Bitmap.Config.ARGB_8888
            )

            blockingPixelCopy(bitmap) { Surface(textureView.surfaceTexture) }
            assertTrue(bitmap.isAllColor(Color.BLUE))

            val stopLatch = CountDownLatch(1)
            glRenderer.stop(true) {
                stopLatch.countDown()
            }

            assertTrue(stopLatch.await(3000, TimeUnit.MILLISECONDS))
            // Assert that targets are detached when the GLRenderer is stopped
            assertFalse(target.isAttached())
        }
    }

    @Test
    fun testEglContextCallbackInvoked() {
        val createdLatch = CountDownLatch(1)
        val destroyedLatch = CountDownLatch(1)
        val createCount = AtomicInteger()
        val destroyCount = AtomicInteger()
        val callback = object : GLRenderer.EglContextCallback {

            override fun onEglContextCreated(eglManager: EglManager) {
                createCount.incrementAndGet()
                createdLatch.countDown()
            }

            override fun onEglContextDestroyed(eglManager: EglManager) {
                destroyCount.incrementAndGet()
                destroyedLatch.countDown()
            }
        }

        val glRenderer = GLRenderer().apply { start() }
        glRenderer.registerEglContextCallback(callback)

        glRenderer.attach(
            Surface(SurfaceTexture(12)),
            10,
            10,
            ColorRenderCallback(Color.RED)
        ).requestRender()

        assertTrue(createdLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(1, createCount.get())

        glRenderer.stop(true)

        assertTrue(destroyedLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(1, destroyCount.get())
    }

    @Test
    fun testEglContextCallbackInvokedBeforeStart() {
        val createdLatch = CountDownLatch(1)
        val destroyedLatch = CountDownLatch(1)
        val createCount = AtomicInteger()
        val destroyCount = AtomicInteger()
        val callback = object : GLRenderer.EglContextCallback {

            override fun onEglContextCreated(eglManager: EglManager) {
                createCount.incrementAndGet()
                createdLatch.countDown()
            }

            override fun onEglContextDestroyed(eglManager: EglManager) {
                destroyCount.incrementAndGet()
                destroyedLatch.countDown()
            }
        }

        val glRenderer = GLRenderer()
        // Adding a callback before the glRenderer is started should still
        // deliver onEglRendererCreated callbacks
        glRenderer.registerEglContextCallback(callback)
        glRenderer.start()

        glRenderer.attach(
            Surface(SurfaceTexture(12)),
            10,
            10,
            ColorRenderCallback(Color.CYAN)
        ).requestRender()

        assertTrue(createdLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(1, createCount.get())

        glRenderer.stop(true)

        assertTrue(destroyedLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(1, destroyCount.get())
    }

    @Test
    fun testEglContextCallbackRemove() {
        val createdLatch = CountDownLatch(1)
        val destroyedLatch = CountDownLatch(1)
        val createCount = AtomicInteger()
        val destroyCount = AtomicInteger()
        val callback = object : GLRenderer.EglContextCallback {

            override fun onEglContextCreated(eglManager: EglManager) {
                createCount.incrementAndGet()
                createdLatch.countDown()
            }

            override fun onEglContextDestroyed(eglManager: EglManager) {
                destroyCount.incrementAndGet()
            }
        }

        val glRenderer = GLRenderer()
        // Adding a callback before the glRenderer is started should still
        // deliver onEglRendererCreated callbacks
        glRenderer.registerEglContextCallback(callback)
        glRenderer.start()

        glRenderer.attach(
            Surface(SurfaceTexture(12)),
            10,
            10,
            ColorRenderCallback(Color.CYAN)
        ).requestRender()

        assertTrue(createdLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(1, createCount.get())

        glRenderer.unregisterEglContextCallback(callback)

        glRenderer.stop(false) {
            destroyedLatch.countDown()
        }

        assertTrue(destroyedLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(0, destroyCount.get())
    }

    /**
     * Helper method to create a GLTestActivity instance and progress it through the Activity
     * lifecycle to the resumed state so we can issue rendering commands into the corresponding
     * SurfaceView/TextureView
     */
    private fun withGLTestActivity(block: GLTestActivity.() -> Unit) {
        ActivityScenario.launch(GLTestActivity::class.java).moveToState(State.RESUMED).onActivity {
            block(it!!)
        }
    }

    /**
     * Helper RenderCallback that renders a solid color and invokes the provided CountdownLatch
     * when rendering is complete
     */
    private class ColorRenderCallback(
        val targetColor: Int
    ) : GLRenderer.RenderCallback {

        override fun onDrawFrame(eglManager: EglManager) {
            GLES20.glClearColor(
                Color.red(targetColor) / 255f,
                Color.green(targetColor) / 255f,
                Color.blue(targetColor) / 255f,
                Color.alpha(targetColor) / 255f,
            )
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }
    }

    /**
     * Helper method that synchronously blocks until the PixelCopy operation is complete
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun blockingPixelCopy(
        destBitmap: Bitmap,
        surfaceProvider: () -> Surface
    ) {
        val copyLatch = CountDownLatch(1)
        val copyThread = HandlerThread("copyThread").apply { start() }
        val copyHandler = Handler(copyThread.looper)
        PixelCopy.request(surfaceProvider.invoke(),
            destBitmap,
            { copyResult ->
                assertEquals(PixelCopy.SUCCESS, copyResult)
                copyLatch.countDown()
                copyThread.quit()
            },
            copyHandler
        )
        assertTrue(copyLatch.await(3000, TimeUnit.MILLISECONDS))
    }

    private fun Bitmap.isAllColor(targetColor: Int): Boolean {
        for (i in 0 until width) {
            for (j in 0 until height) {
                if (getPixel(i, j) != targetColor) {
                    return false
                }
            }
        }
        return true
    }
}