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
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.SurfaceTextureRenderer
import androidx.graphics.isAllColor
import androidx.graphics.lowlatency.LineRenderer
import androidx.graphics.lowlatency.Rectangle
import androidx.hardware.SyncFenceCompat
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.graphics.verifyQuadrants
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
                        override fun onDrawFrame(eglManager: EGLManager) {
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
            override fun onDrawFrame(eglManager: EGLManager) {
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
            override fun onDrawFrame(eglManager: EGLManager) {
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
            override fun onDrawFrame(eglManager: EGLManager) {
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
            override fun onDrawFrame(eglManager: EGLManager) {
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
            override fun onDrawFrame(eglManager: EGLManager) {
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

            override fun onDrawFrame(eglManager: EGLManager) {
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }

        val renderer2 = object : GLRenderer.RenderCallback {
            override fun onDrawFrame(eglManager: EGLManager) {
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
    fun testExecute() {
        val countDownLatch = CountDownLatch(1)
        GLRenderer().apply {
            start()
            execute {
                countDownLatch.countDown()
            }
        }
        assertTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS))
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
    fun testMultipleSurfaceHolderDestroyCallbacks() {
        val destroyLatch = CountDownLatch(1)
        val renderer = GLRenderer().apply { start() }
        val scenario = withGLTestActivity {
            assertNotNull(surfaceView)

            var renderTarget: GLRenderer.RenderTarget? = null
            val callbacks = object : SurfaceHolder.Callback {
                override fun surfaceCreated(p0: SurfaceHolder) {
                    // no-op
                }

                override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                    // no-op
                }

                override fun surfaceDestroyed(p0: SurfaceHolder) {
                    renderTarget?.detach(true)
                    destroyLatch.countDown()
                }
            }
            surfaceView.holder.addCallback(callbacks)
            renderTarget = renderer.attach(surfaceView, ColorRenderCallback(Color.RED))
        }

        val tearDownLatch = CountDownLatch(1)
        scenario.moveToState(State.DESTROYED)
        assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        renderer.stop(true) {
            tearDownLatch.countDown()
        }
        assertTrue(tearDownLatch.await(3000, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testMultipleTextureViewDestroyCallbacks() {
        val destroyLatch = CountDownLatch(1)
        val renderer = GLRenderer().apply { start() }
        val scenario = withGLTestActivity {
            assertNotNull(textureView)

            val renderTarget = renderer.attach(textureView, ColorRenderCallback(Color.RED))
            val listener = textureView.surfaceTextureListener
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    listener?.onSurfaceTextureAvailable(p0, p1, p2)
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                    listener?.onSurfaceTextureSizeChanged(p0, p1, p2)
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    renderTarget.detach(true)
                    listener?.onSurfaceTextureDestroyed(p0)
                    destroyLatch.countDown()
                    return true
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                    listener?.onSurfaceTextureUpdated(p0)
                }
            }
        }

        val tearDownLatch = CountDownLatch(1)
        scenario.moveToState(State.DESTROYED)
        assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        renderer.stop(true) {
            tearDownLatch.countDown()
        }
        assertTrue(tearDownLatch.await(3000, TimeUnit.MILLISECONDS))
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
                override fun onDrawFrame(eglManager: EGLManager) {
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
                override fun onDrawFrame(eglManager: EGLManager) {
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

    fun EGLSpec.querySurfaceSize(eglSurface: EGLSurface): Size {
        val result = IntArray(1)
        eglQuerySurface(
            eglSurface, EGL14.EGL_WIDTH, result, 0
        )
        val width = result[0]
        eglQuerySurface(
            eglSurface, EGL14.EGL_HEIGHT, result, 0
        )
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
    fun testEGLContextCallbackInvoked() {
        val createdLatch = CountDownLatch(1)
        val destroyedLatch = CountDownLatch(1)
        val createCount = AtomicInteger()
        val destroyCount = AtomicInteger()
        val callback = object : GLRenderer.EGLContextCallback {

            override fun onEGLContextCreated(eglManager: EGLManager) {
                createCount.incrementAndGet()
                createdLatch.countDown()
            }

            override fun onEGLContextDestroyed(eglManager: EGLManager) {
                destroyCount.incrementAndGet()
                destroyedLatch.countDown()
            }
        }

        val glRenderer = GLRenderer().apply { start() }
        glRenderer.registerEGLContextCallback(callback)

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
    fun testEGLContextCallbackInvokedBeforeStart() {
        val createdLatch = CountDownLatch(1)
        val destroyedLatch = CountDownLatch(1)
        val createCount = AtomicInteger()
        val destroyCount = AtomicInteger()
        val callback = object : GLRenderer.EGLContextCallback {

            override fun onEGLContextCreated(eglManager: EGLManager) {
                createCount.incrementAndGet()
                createdLatch.countDown()
            }

            override fun onEGLContextDestroyed(eglManager: EGLManager) {
                destroyCount.incrementAndGet()
                destroyedLatch.countDown()
            }
        }

        val glRenderer = GLRenderer()
        // Adding a callback before the glRenderer is started should still
        // deliver onEGLRendererCreated callbacks
        glRenderer.registerEGLContextCallback(callback)
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
    fun testEGLContextCallbackRemove() {
        val createdLatch = CountDownLatch(1)
        val destroyedLatch = CountDownLatch(1)
        val createCount = AtomicInteger()
        val destroyCount = AtomicInteger()
        val callback = object : GLRenderer.EGLContextCallback {

            override fun onEGLContextCreated(eglManager: EGLManager) {
                createCount.incrementAndGet()
                createdLatch.countDown()
            }

            override fun onEGLContextDestroyed(eglManager: EGLManager) {
                destroyCount.incrementAndGet()
            }
        }

        val glRenderer = GLRenderer()
        // Adding a callback before the glRenderer is started should still
        // deliver onEGLRendererCreated callbacks
        glRenderer.registerEGLContextCallback(callback)
        glRenderer.start()

        glRenderer.attach(
            Surface(SurfaceTexture(12)),
            10,
            10,
            ColorRenderCallback(Color.CYAN)
        ).requestRender()

        assertTrue(createdLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(1, createCount.get())

        glRenderer.unregisterEGLContextCallback(callback)

        glRenderer.stop(false) {
            destroyedLatch.countDown()
        }

        assertTrue(destroyedLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(0, destroyCount.get())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testRenderBufferTarget() {
        val width = 10
        val height = 10
        val renderLatch = CountDownLatch(1)
        val teardownLatch = CountDownLatch(1)
        val glRenderer = GLRenderer().apply { start() }
        var frameBuffer: FrameBuffer? = null

        val supportsNativeFence = AtomicBoolean(false)
        glRenderer.createRenderTarget(width, height, object : GLRenderer.RenderCallback {

            @WorkerThread
            override fun onDrawFrame(eglManager: EGLManager) {
                if (eglManager.supportsNativeAndroidFence()) {
                    supportsNativeFence.set(true)
                    var syncFenceCompat: SyncFenceCompat? = null
                    try {
                        val egl = eglManager.eglSpec
                        val buffer = FrameBuffer(
                            egl,
                            HardwareBuffer.create(
                                width,
                                height,
                                HardwareBuffer.RGBA_8888,
                                1,
                                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
                            )
                        ).also { frameBuffer = it }
                        buffer.makeCurrent()
                        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                        GLES20.glFlush()
                        syncFenceCompat = SyncFenceCompat.createNativeSyncFence()
                        syncFenceCompat.await(TimeUnit.SECONDS.toNanos(3))
                    } finally {
                        syncFenceCompat?.close()
                    }
                }
                renderLatch.countDown()
            }
        }).requestRender()

        var hardwareBuffer: HardwareBuffer? = null
        try {
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            if (supportsNativeFence.get()) {
                hardwareBuffer = frameBuffer?.hardwareBuffer
                if (hardwareBuffer != null) {
                    val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
                    // Copy to non hardware bitmap to be able to sample pixels
                    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        ?.copy(Bitmap.Config.ARGB_8888, false)
                    if (bitmap != null) {
                        assertTrue(bitmap.isAllColor(Color.RED))
                    } else {
                        fail("Unable to obtain Bitmap from hardware buffer")
                    }
                } else {
                    fail("Unable to obtain hardwarebuffer from FrameBuffer")
                }
            }
        } finally {
            hardwareBuffer?.close()
            glRenderer.stop(true) {
                teardownLatch.countDown()
            }
            assertTrue(teardownLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q, maxSdkVersion = 32) // b/268117579
    fun testFrontBufferedRenderer() {
        val width = 10
        val height = 10
        val renderLatch = CountDownLatch(1)
        val teardownLatch = CountDownLatch(1)
        val glRenderer = GLRenderer().apply { start() }
        var frameBuffer: FrameBuffer? = null
        var status = false
        var supportsFence = false

        val callbacks = object : FrameBufferRenderer.RenderCallback {

            private val mOrthoMatrix = FloatArray(16)

            override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer =
                FrameBuffer(
                    egl,
                    HardwareBuffer.create(
                        width,
                        height,
                        HardwareBuffer.RGBA_8888,
                        1,
                        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
                    )
                ).also { frameBuffer = it }

            override fun onDraw(eglManager: EGLManager) {
                GLES20.glViewport(0, 0, width, height)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    width.toFloat(),
                    0f,
                    height.toFloat(),
                    -1f,
                    1f
                )
                Rectangle().draw(
                    mOrthoMatrix,
                    Color.RED,
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat()
                )
                supportsFence = eglManager.supportsNativeAndroidFence()
            }

            override fun onDrawComplete(
                frameBuffer: FrameBuffer,
                syncFenceCompat: SyncFenceCompat?
            ) {
                status = syncFenceCompat?.await(3000) ?: true
                renderLatch.countDown()
            }
        }

        glRenderer.createRenderTarget(
            width,
            height,
            FrameBufferRenderer(callbacks)
        ).requestRender()

        var hardwareBuffer: HardwareBuffer? = null
        try {
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            if (supportsFence) {
                assert(status)
            }

            hardwareBuffer = frameBuffer?.hardwareBuffer
            if (hardwareBuffer != null) {
                val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
                // Copy to non hardware bitmap to be able to sample pixels
                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                if (bitmap != null) {
                    assertTrue(bitmap.isAllColor(Color.RED))
                } else {
                    fail("Unable to obtain Bitmap from hardware buffer")
                }
            } else {
                fail("Unable to obtain hardwarebuffer from FrameBuffer")
            }
        } finally {
            hardwareBuffer?.close()
            glRenderer.stop(true) {
                teardownLatch.countDown()
            }
            assertTrue(teardownLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testQuadTextureRenderer() {
        val width = 10
        val height = 10
        val renderLatch = CountDownLatch(1)
        val teardownLatch = CountDownLatch(1)
        val glRenderer = GLRenderer().apply { start() }
        var frameBuffer: FrameBuffer? = null
        var status = false
        var supportsFence = false
        val frameHandlerThread = HandlerThread("frameAvailable").apply { start() }
        val frameHandler = Handler(frameHandlerThread.looper)
        val callbacks = object : FrameBufferRenderer.RenderCallback {

            private val mOrthoMatrix = FloatArray(16)

            override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer =
                FrameBuffer(
                    egl,
                    HardwareBuffer.create(
                        width,
                        height,
                        HardwareBuffer.RGBA_8888,
                        1,
                        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
                    )
                ).also { frameBuffer = it }

            override fun onDraw(eglManager: EGLManager) {
                val texId = genTexture()
                val frameAvailableLatch = CountDownLatch(1)
                val surfaceTexture = SurfaceTexture(texId).apply {
                    setDefaultBufferSize(width, height)
                    setOnFrameAvailableListener({
                        frameAvailableLatch.countDown()
                    }, frameHandler)
                }

                val surface = Surface(surfaceTexture)
                val canvas = surface.lockCanvas(null)
                canvas.save()
                val paint = Paint()
                // top left
                canvas.drawRect(0f, 0f, width / 2f, height / 2f,
                    paint.apply { color = Color.RED })
                // top right
                canvas.drawRect(width / 2f, 0f, width.toFloat(), height / 2f,
                    paint.apply { color = Color.BLUE })
                // bottom left
                canvas.drawRect(0f, height / 2f, width / 2f, height.toFloat(),
                    paint.apply { color = Color.YELLOW })
                // bottom right
                canvas.drawRect(width / 2f, height / 2f, width.toFloat(), height.toFloat(),
                    paint.apply { color = Color.GREEN })
                canvas.restore()
                surface.unlockCanvasAndPost(canvas)

                assertTrue(frameAvailableLatch.await(3000, TimeUnit.MILLISECONDS))

                GLES20.glViewport(0, 0, width, height)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    width.toFloat(),
                    0f,
                    height.toFloat(),
                    -1f,
                    1f
                )
                val quadRenderer = QuadTextureRenderer().apply {
                    setSurfaceTexture(surfaceTexture)
                }
                quadRenderer.draw(
                    mOrthoMatrix,
                    width.toFloat(),
                    height.toFloat()
                )
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                GLES20.glFinish()
                supportsFence = eglManager.supportsNativeAndroidFence()
                quadRenderer.release()
                surface.release()
                surfaceTexture.release()
                deleteTexture(texId)
            }

            override fun onDrawComplete(
                frameBuffer: FrameBuffer,
                syncFenceCompat: SyncFenceCompat?
            ) {
                status = syncFenceCompat?.await(3000) ?: true
                renderLatch.countDown()
            }
        }

        glRenderer.createRenderTarget(
            width,
            height,
            FrameBufferRenderer(callbacks)
        ).requestRender()

        var hardwareBuffer: HardwareBuffer? = null
        try {
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            if (supportsFence) {
                assertTrue(status)
            }

            hardwareBuffer = frameBuffer?.hardwareBuffer
            if (hardwareBuffer != null) {
                val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
                // Copy to non hardware bitmap to be able to sample pixels
                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                if (bitmap != null) {
                    bitmap.verifyQuadrants(Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN)
                } else {
                    fail("Unable to obtain Bitmap from hardware buffer")
                }
            } else {
                fail("Unable to obtain hardwarebuffer from FrameBuffer")
            }
        } finally {
            hardwareBuffer?.close()
            glRenderer.stop(true) {
                teardownLatch.countDown()
            }
            frameHandlerThread.quit()
            assertTrue(teardownLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testRenderSurfaceTextureFromSurfaceTextureRenderer() {
        val width = 10
        val height = 10
        val renderLatch = CountDownLatch(1)
        val teardownLatch = CountDownLatch(1)
        val glRenderer = GLRenderer().apply { start() }
        var frameBuffer: FrameBuffer? = null
        var status = false
        var supportsFence = false
        val frameHandlerThread = HandlerThread("frameAvailable").apply { start() }
        val frameHandler = Handler(frameHandlerThread.looper)
        val renderNode = RenderNode("node").apply {
            setPosition(0, 0, width, height)
            val canvas = beginRecording()
            val paint = Paint()
            // top left
            canvas.drawRect(0f, 0f, width / 2f, height / 2f,
                paint.apply { color = Color.RED })
            // top right
            canvas.drawRect(width / 2f, 0f, width.toFloat(), height / 2f,
                paint.apply { color = Color.BLUE })
            // bottom left
            canvas.drawRect(0f, height / 2f, width / 2f, height.toFloat(),
                paint.apply { color = Color.YELLOW })
            // bottom right
            canvas.drawRect(width / 2f, height / 2f, width.toFloat(), height.toFloat(),
                paint.apply { color = Color.GREEN })
            endRecording()
        }
        val frameAvailableLatch = CountDownLatch(1)
        var surfaceTexture: SurfaceTexture? = null
        val surfaceTextureRenderer = SurfaceTextureRenderer(
            renderNode,
            width,
            height,
            frameHandler
        ) {
            surfaceTexture = it
            frameAvailableLatch.countDown()
        }
        surfaceTextureRenderer.renderFrame()

        val callbacks = object : FrameBufferRenderer.RenderCallback {

            private val mOrthoMatrix = FloatArray(16)

            override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer =
                FrameBuffer(
                    egl,
                    HardwareBuffer.create(
                        width,
                        height,
                        HardwareBuffer.RGBA_8888,
                        1,
                        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
                    )
                ).also { frameBuffer = it }

            override fun onDraw(eglManager: EGLManager) {
                val texId = genTexture()
                assertTrue(frameAvailableLatch.await(3000, TimeUnit.MILLISECONDS))
                assertNotNull(surfaceTexture)
                surfaceTexture!!.attachToGLContext(texId)

                GLES20.glViewport(0, 0, width, height)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    width.toFloat(),
                    0f,
                    height.toFloat(),
                    -1f,
                    1f
                )
                val quadRenderer = QuadTextureRenderer().apply {
                    setSurfaceTexture(surfaceTexture!!)
                }
                quadRenderer.draw(
                    mOrthoMatrix,
                    width.toFloat(),
                    height.toFloat()
                )
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                GLES20.glFinish()
                supportsFence = eglManager.supportsNativeAndroidFence()
                quadRenderer.release()
                deleteTexture(texId)
            }

            override fun onDrawComplete(
                frameBuffer: FrameBuffer,
                syncFenceCompat: SyncFenceCompat?
            ) {
                status = syncFenceCompat?.await(3000) ?: true
                renderLatch.countDown()
            }
        }

        glRenderer.createRenderTarget(
            width,
            height,
            FrameBufferRenderer(callbacks)
        ).requestRender()

        var hardwareBuffer: HardwareBuffer? = null
        try {
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            if (supportsFence) {
                assertTrue(status)
            }

            hardwareBuffer = frameBuffer?.hardwareBuffer
            if (hardwareBuffer != null) {
                val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
                // Copy to non hardware bitmap to be able to sample pixels
                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                if (bitmap != null) {
                    bitmap.verifyQuadrants(Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN)
                } else {
                    fail("Unable to obtain Bitmap from hardware buffer")
                }
            } else {
                fail("Unable to obtain hardwarebuffer from FrameBuffer")
            }
        } finally {
            surfaceTextureRenderer.release()
            hardwareBuffer?.close()
            glRenderer.stop(true) {
                teardownLatch.countDown()
            }
            frameHandlerThread.quit()
            assertTrue(teardownLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testFrameBufferRendererWithSyncFence() {

        val width = 10
        val height = 10
        val renderLatch = CountDownLatch(1)
        val teardownLatch = CountDownLatch(1)

        val glRenderer = GLRenderer().apply { start() }
        var startTime = Long.MAX_VALUE
        var signalTime = 0L

        var supportsFence = false
        val renderer =
            object : FrameBufferRenderer.RenderCallback, GLRenderer.EGLContextCallback {
                private val mMVPMatrix = FloatArray(16)
                private val mLines = FloatArray(4)
                private val mLineRenderer = LineRenderer()
                var mFrameBuffer: FrameBuffer? = null

                @WorkerThread
                override fun onEGLContextCreated(eglManager: EGLManager) {
                    mLineRenderer.initialize()
                }

                @WorkerThread
                override fun onEGLContextDestroyed(eglManager: EGLManager) {
                    mLineRenderer.release()
                }

                @WorkerThread
                override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer {
                    return if (mFrameBuffer != null) {
                        mFrameBuffer!!
                    } else {
                        FrameBuffer(
                            egl,
                            HardwareBuffer.create(
                                width,
                                height,
                                HardwareBuffer.RGBA_8888,
                                1,
                                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
                            )
                        ).also { mFrameBuffer = it }
                    }
                }

                @WorkerThread
                override fun onDraw(eglManager: EGLManager) {
                    startTime = System.nanoTime()
                    GLES20.glViewport(0, 0, width, height)
                    assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
                    Matrix.orthoM(mMVPMatrix, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)
                    mLines[0] = 0f
                    mLines[1] = 0f
                    mLines[2] = 5f
                    mLines[3] = 5f
                    mLineRenderer.drawLines(mMVPMatrix, mLines)
                    assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())

                    supportsFence = eglManager.supportsNativeAndroidFence()
                }

                @WorkerThread
                override fun onDrawComplete(
                    frameBuffer: FrameBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    if (supportsFence) {
                        assertNotNull(syncFenceCompat)
                        assertTrue(syncFenceCompat!!.isValid())
                        assertTrue(syncFenceCompat.await(3000))
                        signalTime = syncFenceCompat.getSignalTimeNanos()

                        assertTrue(syncFenceCompat.getSignalTimeNanos() < System.nanoTime())
                        assertTrue(syncFenceCompat.getSignalTimeNanos() > startTime)
                    }
                    renderLatch.countDown()

                    assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
                }
            }

        glRenderer.registerEGLContextCallback(renderer)
        val hwBufferRenderer = FrameBufferRenderer(renderer)
        val renderTarget =
            glRenderer.createRenderTarget(width, height, hwBufferRenderer)

        renderTarget.requestRender()
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())

        try {
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            if (supportsFence) {
                assertTrue(startTime < signalTime)
                assertTrue(signalTime < System.nanoTime())
            }
        } finally {
            glRenderer.stop(true) {
                teardownLatch.countDown()
            }
            assertTrue(teardownLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    /**
     * Helper method to create a GLTestActivity instance and progress it through the Activity
     * lifecycle to the resumed state so we can issue rendering commands into the corresponding
     * SurfaceView/TextureView
     */
    private fun withGLTestActivity(
        block: GLTestActivity.() -> Unit
    ): ActivityScenario<GLTestActivity> =
        ActivityScenario.launch(GLTestActivity::class.java).moveToState(State.RESUMED).onActivity {
            block(it!!)
        }

    /**
     * Helper RenderCallback that renders a solid color and invokes the provided CountdownLatch
     * when rendering is complete
     */
    private class ColorRenderCallback(
        val targetColor: Int
    ) : GLRenderer.RenderCallback {

        override fun onDrawFrame(eglManager: EGLManager) {
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
        PixelCopy.request(
            surfaceProvider.invoke(),
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

    private fun genTexture(): Int {
        val buffer = IntArray(1)
        GLES20.glGenTextures(1, buffer, 0)
        return buffer[0]
    }

    private fun deleteTexture(texId: Int) {
        val buffer = IntArray(1)
        buffer[0] = texId
        GLES20.glDeleteTextures(1, buffer, 0)
    }
}