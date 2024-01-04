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

import android.graphics.Color
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.lowlatency.Rectangle
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlUtils
import androidx.hardware.SyncFenceCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GLFrameBufferRendererTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetSyncStrategy() {
        val latch = CountDownLatch(1)

        val syncStrategy = object : SyncStrategy {
            override fun createSyncFence(eglSpec: EGLSpec): SyncFenceCompat? {
                return SyncStrategy.ALWAYS.createSyncFence(eglSpec)
            }
        }
        var fence: SyncFenceCompat? = null
        var supportsNativeFence = false
        val callbacks = object : GLFrameBufferRenderer.Callback {
            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                supportsNativeFence = eglManager.supportsNativeAndroidFence()
            }

            override fun onDrawComplete(
                targetSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction,
                frameBuffer: FrameBuffer,
                syncFence: SyncFenceCompat?
            ) {
                fence = syncFence
                latch.countDown()
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks)
                        .setSyncStrategy(syncStrategy)
                        .build().also { fbr ->
                            fbr.render()
                        }
                }
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(renderer)
            assertTrue(renderer!!.isValid())
            assertTrue(syncStrategy === renderer?.syncStrategy)
            if (supportsNativeFence) {
                assertNotNull(fence)
            } else {
                assertNull(fence)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetGLRenderer() {
        val glRenderer = GLRenderer().apply { start() }
        val callbacks = object : GLFrameBufferRenderer.Callback {
            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                // NO-OP
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val createLatch = CountDownLatch(1)
            ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks)
                        .setGLRenderer(glRenderer)
                        .build()
                    createLatch.countDown()
                }

            assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(renderer)
            assertTrue(renderer!!.isValid())
            assertTrue(glRenderer == renderer?.glRenderer)
        } finally {
            renderer.blockingRelease()
            glRenderer.stop(true)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetMaxBuffers() {
        val callbacks = object : GLFrameBufferRenderer.Callback {
            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                // NO-OP
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val createLatch = CountDownLatch(1)
            ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks)
                        .setMaxBuffers(5)
                        .build()
                    createLatch.countDown()
                }

            assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(renderer)
            assertTrue(renderer!!.isValid())
            assertEquals(5, renderer?.maxBuffers)
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetPixelFormat() {
        val flags = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
            HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        // First verify if another format other than RGBA_8888 is supported
        if (!HardwareBuffer.isSupported(
                1, // width
                1, // height
                HardwareBuffer.RGBA_FP16, // format
                1, // layers
                flags // flags
            )) {
            return
        }
        var pixelFormat = -1
        val latch = CountDownLatch(1)
        val callbacks = object : GLFrameBufferRenderer.Callback {
            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                // NO-OP
            }

            override fun onDrawComplete(
                targetSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction,
                frameBuffer: FrameBuffer,
                syncFence: SyncFenceCompat?
            ) {
                pixelFormat = frameBuffer.hardwareBuffer.format
                latch.countDown()
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks)
                        .setBufferFormat(HardwareBuffer.RGBA_FP16)
                        .build()
                }
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(renderer)
            assertTrue(renderer!!.isValid())
            assertEquals(HardwareBuffer.RGBA_FP16, renderer?.bufferFormat)
            assertEquals(HardwareBuffer.RGBA_FP16, pixelFormat)
        } finally {
            renderer.blockingRelease()
        }
    }

    @Ignore // b/288580549
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetUsageFlags() {
        val latch = CountDownLatch(1)
        var actualUsageFlags = -1L
        val callbacks = object : GLFrameBufferRenderer.Callback {
            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                // NO-OP
            }

            override fun onDrawComplete(
                targetSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction,
                frameBuffer: FrameBuffer,
                syncFence: SyncFenceCompat?
            ) {
                actualUsageFlags = frameBuffer.hardwareBuffer.usage
                latch.countDown()
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val createLatch = CountDownLatch(1)
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks)
                        .setUsageFlags(
                            HardwareBuffer.USAGE_GPU_DATA_BUFFER or
                                HardwareBuffer.USAGE_CPU_READ_RARELY)
                        .build()
                    createLatch.countDown()
                }

            scenario.moveToState(Lifecycle.State.RESUMED)

            assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(renderer)
            val usageFlags = renderer?.usageFlags ?: 0
            assertTrue(usageFlags and HardwareBuffer.USAGE_GPU_DATA_BUFFER != 0L)
            assertTrue(usageFlags and HardwareBuffer.USAGE_CPU_READ_RARELY != 0L)
            assertTrue(actualUsageFlags and HardwareBuffer.USAGE_GPU_DATA_BUFFER != 0L)
            assertTrue(actualUsageFlags and HardwareBuffer.USAGE_CPU_READ_RARELY != 0L)
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testExecute() {
        val executeLatch = CountDownLatch(1)
        val callbacks = object : GLFrameBufferRenderer.Callback {

            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray,
            ) {
                // NO-OP
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks).build()
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.execute {
                    executeLatch.countDown()
                }
            }

            assertTrue(executeLatch.await(3000, TimeUnit.MILLISECONDS))
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderFrameBuffer() {
        val renderLatch = CountDownLatch(1)
        var surfaceWidth = 0
        var surfaceHeight = 0
        val callbacks = object : GLFrameBufferRenderer.Callback {

            val mProjectionMatrix = FloatArray(16)
            val mOrthoMatrix = FloatArray(16)

            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                assertEquals(surfaceWidth, width)
                assertEquals(surfaceHeight, height)
                GLES20.glViewport(0, 0, bufferInfo.width, bufferInfo.height)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferInfo.width.toFloat(),
                    0f,
                    bufferInfo.height.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
            }

            override fun onDrawComplete(
                targetSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction,
                frameBuffer: FrameBuffer,
                syncFence: SyncFenceCompat?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView? = null

        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView().apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                // no-op
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                surfaceWidth = width
                                surfaceHeight = height
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                // no-op
                            }
                        })
                    }
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks).build()
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.render()
            }
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED ==
                    bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderedOnSurfaceRedraw() {
        val renderLatch = CountDownLatch(1)
        val callbacks = object : GLFrameBufferRenderer.Callback {

            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                renderLatch.countDown()
            }
        }
        var activity: SurfaceViewTestActivity? = null
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceView?
        try {
            val scenario = ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    activity = it
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callbacks).build()
                }

            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val destroyLatch = CountDownLatch(1)
            activity?.setOnDestroyCallback {
                destroyLatch.countDown()
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testReleaseRemovedSurfaceCallbacks() {
        val callback = object : GLFrameBufferRenderer.Callback {
            override fun onDrawFrame(
                eglManager: EGLManager,
                width: Int,
                height: Int,
                bufferInfo: BufferInfo,
                transform: FloatArray
            ) {
                // NO-OP
            }
        }
        var renderer: GLFrameBufferRenderer? = null
        var surfaceView: SurfaceViewTestActivity.TestSurfaceView? = null
        val createLatch = CountDownLatch(1)
        ActivityScenario.launch(SurfaceViewTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity {
                surfaceView = it.getSurfaceView()
                renderer = GLFrameBufferRenderer.Builder(surfaceView!!, callback).build()
                createLatch.countDown()
            }

        assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))

        val resolvedSurfaceView = surfaceView
        try {
            if (resolvedSurfaceView != null) {
                assertEquals(1, resolvedSurfaceView.getCallbackCount())
                val releaseLatch = CountDownLatch(1)
                renderer!!.release(true) {
                    releaseLatch.countDown()
                }
                assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
                assertEquals(0, resolvedSurfaceView.getCallbackCount())
                renderer = null
            } else {
                fail("Unable to resolve SurfaceView, was the test Activity created?")
            }
        } finally {
            renderer?.blockingRelease()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun GLFrameBufferRenderer?.blockingRelease(timeoutMillis: Long = 3000) {
        if (this != null) {
            val destroyLatch = CountDownLatch(1)
            release(false) {
                destroyLatch.countDown()
            }
            assertTrue(destroyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS))
            assertFalse(isValid())
        } else {
            fail("GLFrameBufferRenderer is not initialized")
        }
    }
}
