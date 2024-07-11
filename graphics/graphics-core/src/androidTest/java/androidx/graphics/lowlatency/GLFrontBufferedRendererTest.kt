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

package androidx.graphics.lowlatency

import android.app.UiAutomation
import android.graphics.Color
import android.hardware.HardwareBuffer
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.SurfaceViewTestActivity
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlUtils
import androidx.lifecycle.Lifecycle
import androidx.opengl.EGLExt
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class GLFrontBufferedRendererTest {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    companion object {
        const val TAG = "GLFrontBufferedRenderer"
    }

    @Test
    fun testFrontBufferedLayerRender() {
        val renderLatch = CountDownLatch(1)
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                val mProjectionMatrix = FloatArray(16)
                val mOrthoMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
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

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
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
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, surfaceView ->
            renderer.renderFrontBufferedLayer(Any())
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED == bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        }
    }

    @Test
    fun testInvalidWidth() {
        testRenderWithDimension(0, 200)
    }

    @Test
    fun testInvalidHeight() {
        testRenderWithDimension(100, 0)
    }

    @Test
    fun testNegativeWidth() {
        testRenderWithDimension(-19, 200)
    }

    @Test
    fun testNegativeHeight() {
        testRenderWithDimension(100, -30)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun testRenderWithDimension(width: Int, height: Int) {
        val renderLatch = CountDownLatch(1)
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                val mProjectionMatrix = FloatArray(16)
                val mOrthoMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
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

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
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
        verifyGLFrontBufferedRenderer(
            callbacks,
            createSurfaceView = { activity ->
                val surfaceView = SurfaceView(activity)
                activity.setContentView(surfaceView, FrameLayout.LayoutParams(width, height))
                surfaceView
            },
            assertFirstRender = { latch ->
                // invalid dimension should not render
                assertFalse(latch.await(500, TimeUnit.MILLISECONDS))
            }
        ) { _, renderer, _ ->
            renderer.renderFrontBufferedLayer(Any())
        }
    }

    @Test
    fun testDoubleBufferedLayerRender() {
        val commitLatch = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                private val mOrthoMatrix = FloatArray(16)
                private val mProjectionMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
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

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    commitLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        commitLatch.get()?.countDown()
                    }
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { scenario, renderer, surfaceView ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            renderer.renderFrontBufferedLayer(Any())
            commitLatch.set(CountDownLatch(1))
            renderer.commit()

            assertTrue(commitLatch.get()!!.await(300, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (abs(
                    Color.red(Color.BLUE) -
                        Color.red(bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2))
                ) < 2) &&
                    (abs(
                        Color.green(Color.BLUE) -
                            Color.green(
                                bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
                            )
                    ) < 2) &&
                    (abs(
                        Color.blue(Color.BLUE) -
                            Color.blue(
                                bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
                            )
                    ) < 2)
            }
        }
    }

    @Test
    fun testRenderDoubleBufferLayer() {
        val squareSize = 100f
        val renderLatch = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Int> {

                private val mOrthoMatrix = FloatArray(16)
                private val mProjectionMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Int
                ) {
                    // NO-OP we do not render to the front buffered layer in this test case
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Int>
                ) {

                    GLES20.glViewport(0, 0, bufferInfo.width, bufferInfo.height)
                    if (params.isEmpty()) {
                        // We will receive no inputs on the first render so just render black.
                        // This will be in response to surfaceRedrawNeeded
                        GLES20.glClearColor(0f, 0f, 0f, 1f)
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                        GLES20.glFlush()
                        return
                    }
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
                    assertEquals(params.size, 4)
                    with(Rectangle()) {
                        draw(
                            mProjectionMatrix,
                            params.elementAt(0),
                            0f,
                            0f,
                            squareSize / 2f,
                            squareSize / 2f
                        )
                        draw(
                            mProjectionMatrix,
                            params.elementAt(1),
                            squareSize / 2f,
                            0f,
                            squareSize,
                            squareSize / 2f
                        )
                        draw(
                            mProjectionMatrix,
                            params.elementAt(2),
                            0f,
                            squareSize / 2f,
                            squareSize / 2f,
                            squareSize
                        )
                        draw(
                            mProjectionMatrix,
                            params.elementAt(3),
                            squareSize / 2f,
                            squareSize / 2f,
                            squareSize,
                            squareSize
                        )
                    }
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    renderLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        renderLatch.get()?.countDown()
                    }
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, surfaceView ->
            renderLatch.set(CountDownLatch(1))
            val colors = listOf(Color.RED, Color.BLACK, Color.YELLOW, Color.BLUE)
            renderer.renderMultiBufferedLayer(colors)

            assertTrue(renderLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            surfaceView.getLocationOnScreen(coords)

            SurfaceControlUtils.validateOutput { bitmap ->
                val topLeft =
                    bitmap.getPixel(
                        coords[0] + (squareSize / 4).toInt(),
                        coords[1] + (squareSize / 4).toInt()
                    )
                val topRight =
                    bitmap.getPixel(
                        coords[0] + (squareSize * 3f / 4f).roundToInt(),
                        coords[1] + (squareSize / 4).toInt()
                    )
                val bottomLeft =
                    bitmap.getPixel(
                        coords[0] + (squareSize / 4f).toInt(),
                        coords[1] + (squareSize * 3f / 4f).roundToInt()
                    )
                val bottomRight =
                    bitmap.getPixel(
                        coords[0] + (squareSize * 3f / 4f).roundToInt(),
                        coords[1] + (squareSize * 3f / 4f).roundToInt()
                    )
                Color.RED == topLeft &&
                    Color.BLACK == topRight &&
                    Color.YELLOW == bottomLeft &&
                    Color.BLUE == bottomRight
            }
        }
    }

    @Test
    fun testBufferRetargetingFrontBufferLayer() {
        val squareSize = 100f
        val renderLatch = CountDownLatch(1)
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Int> {

                private val mOrthoMatrix = FloatArray(16)
                private val mProjectionMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Int
                ) {
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
                    val buffer = IntArray(1)
                    GLES20.glGenTextures(1, buffer, 0)
                    val textureId = buffer[0]
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

                    GLES20.glTexImage2D(
                        GLES20.GL_TEXTURE_2D,
                        0,
                        GLES20.GL_RGBA,
                        width,
                        height,
                        0,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        null
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_NEAREST
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE
                    )

                    GLES20.glGenFramebuffers(1, buffer, 0)
                    val frameBufferId = buffer[0]
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId)
                    GLES20.glFramebufferTexture2D(
                        GLES20.GL_FRAMEBUFFER,
                        GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D,
                        textureId,
                        0
                    )

                    val framebufferStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
                    assertEquals(
                        "Invalid framebufferstatus: $framebufferStatus",
                        GLES20.GL_FRAMEBUFFER_COMPLETE,
                        framebufferStatus
                    )
                    Rectangle().draw(transform, Color.RED, 0f, 0f, squareSize, squareSize)

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferInfo.frameBufferId)
                    Rectangle().draw(mProjectionMatrix, param, 0f, 0f, squareSize, squareSize)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
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

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Int>
                ) {
                    // NO-OP
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, surfaceView ->
            renderer.renderFrontBufferedLayer(Color.BLUE)
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            surfaceView.getLocationOnScreen(coords)

            SurfaceControlUtils.validateOutput { bitmap ->
                val center =
                    bitmap.getPixel(
                        coords[0] + (squareSize / 2).toInt(),
                        coords[1] + (squareSize / 2).toInt()
                    )
                Color.BLUE == center
            }
        }
    }

    @Test
    fun testBufferRetargetingDoubleBufferedLayer() {
        val commitLatch = AtomicReference<CountDownLatch?>()
        val squareSize = 100f
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Int> {

                private val mOrthoMatrix = FloatArray(16)
                private val mProjectionMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Int
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Int>
                ) {
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
                    val buffer = IntArray(1)

                    GLES20.glGenTextures(1, buffer, 0)
                    val textureId = buffer[0]
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

                    GLES20.glTexImage2D(
                        GLES20.GL_TEXTURE_2D,
                        0,
                        GLES20.GL_RGBA,
                        width,
                        height,
                        0,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        null
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_NEAREST
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE
                    )

                    GLES20.glGenFramebuffers(1, buffer, 0)
                    val frameBufferId = buffer[0]
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId)
                    GLES20.glFramebufferTexture2D(
                        GLES20.GL_FRAMEBUFFER,
                        GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D,
                        textureId,
                        0
                    )

                    val framebufferStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
                    assertEquals(
                        "Invalid framebufferstatus: $framebufferStatus",
                        GLES20.GL_FRAMEBUFFER_COMPLETE,
                        framebufferStatus
                    )

                    Rectangle().draw(transform, Color.RED, 0f, 0f, squareSize, squareSize)

                    val eglSpec = eglManager.eglSpec
                    val sync = eglSpec.eglCreateSyncKHR(EGLExt.EGL_SYNC_FENCE_KHR, null)
                    Assert.assertNotNull(sync)

                    assertEquals(
                        "eglCreateSync failed: " + GLES20.GL_INVALID_OPERATION,
                        EGL14.EGL_SUCCESS,
                        eglSpec.eglGetError()
                    )

                    GLES20.glFlush()
                    assertEquals("glFlush failed", GLES20.GL_NO_ERROR, GLES20.glGetError())

                    val status = eglSpec.eglClientWaitSyncKHR(sync!!, 0, EGLExt.EGL_FOREVER_KHR)

                    assertEquals(
                        "eglClientWaitSync failed",
                        EGLExt.EGL_CONDITION_SATISFIED_KHR,
                        status
                    )

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferInfo.frameBufferId)
                    for (param in params) {
                        Rectangle().draw(mProjectionMatrix, param, 0f, 0f, squareSize, squareSize)
                    }
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    commitLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        commitLatch.get()?.countDown()
                    }
                }
            }

        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, surfaceView ->
            renderer.renderFrontBufferedLayer(Color.BLUE)
            commitLatch.set(CountDownLatch(1))
            renderer.commit()

            assertTrue(commitLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            surfaceView.getLocationOnScreen(coords)

            SurfaceControlUtils.validateOutput { bitmap ->
                val center =
                    bitmap.getPixel(
                        coords[0] + (squareSize / 2).toInt(),
                        coords[1] + (squareSize / 2).toInt()
                    )
                Color.BLUE == center
            }
        }
    }

    @Test
    fun testCancelFrontBufferLayerRender() {
        val squareSize = 100f
        val commitLatch = AtomicReference<CountDownLatch?>()
        val cancelLatch = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Int> {

                private val mOrthoMatrix = FloatArray(16)
                private val mProjectionMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Int
                ) {
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
                    Rectangle().draw(mProjectionMatrix, param, 0f, 0f, squareSize, squareSize)
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Int>
                ) {

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
                    for (p in params) {
                        Rectangle().draw(mProjectionMatrix, p, 0f, 0f, squareSize, squareSize)
                    }
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    cancelLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        cancelLatch.get()?.countDown()
                    }
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    commitLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        commitLatch.get()?.countDown()
                    }
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, surfaceView ->
            renderer.renderFrontBufferedLayer(Color.BLUE)
            commitLatch.set(CountDownLatch(1))
            renderer.commit()

            assertTrue(commitLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            cancelLatch.set(CountDownLatch(1))
            renderer.renderFrontBufferedLayer(Color.RED)
            renderer.cancel()

            val coords = IntArray(2)
            surfaceView.getLocationOnScreen(coords)

            SurfaceControlUtils.validateOutput { bitmap ->
                val pixel =
                    bitmap.getPixel(
                        coords[0] + (squareSize / 2).toInt(),
                        coords[1] + (squareSize / 2).toInt()
                    )
                // After cancel is invoked the front buffered layer should not be visible
                Color.BLUE == pixel
            }
        }
    }

    @Test
    fun testExecute() {
        val executeLatch = CountDownLatch(1)
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Int> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Int
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Int>
                ) {
                    // NO-OP
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, _ ->
            renderer.execute { executeLatch.countDown() }

            assertTrue(executeLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun testUsageFlagContainsFrontBufferUsage() {
        val usageFlags = FrontBufferUtils.obtainHardwareBufferUsageFlags()
        if (UsageFlagsVerificationHelper.isSupported(HardwareBuffer.USAGE_FRONT_BUFFER)) {
            assertNotEquals(0, usageFlags and HardwareBuffer.USAGE_FRONT_BUFFER)
        } else {
            assertEquals(0, usageFlags and HardwareBuffer.USAGE_FRONT_BUFFER)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun testUsageFlagContainsComposerOverlay() {
        val usageFlags = FrontBufferUtils.obtainHardwareBufferUsageFlags()
        if (UsageFlagsVerificationHelper.isSupported(HardwareBuffer.USAGE_COMPOSER_OVERLAY)) {
            assertNotEquals(0, usageFlags and HardwareBuffer.USAGE_COMPOSER_OVERLAY)
        } else {
            assertEquals(0, usageFlags and HardwareBuffer.USAGE_COMPOSER_OVERLAY)
        }
    }

    @Test
    fun testBaseFlags() {
        assertNotEquals(0, FrontBufferUtils.BaseFlags and HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
        assertNotEquals(0, FrontBufferUtils.BaseFlags and HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
    }

    @Ignore("b/324920812")
    @Test
    fun testRenderFrontBufferSeveralTimes() {
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                var red = 1f
                var blue = 0f
                val mOrthoMatrix = FloatArray(16)
                val mProjectionMatrix = FloatArray(16)
                var mRectangle: Rectangle? = null

                private fun getSquare(): Rectangle =
                    mRectangle ?: Rectangle().also { mRectangle = it }

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
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
                    val color = Color.argb(1f, red, 0f, blue)
                    Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                    getSquare().draw(mProjectionMatrix, color, 0f, 0f, 100f, 100f)

                    val tmp = red
                    red = blue
                    blue = tmp
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    val color = Color.argb(1f, red, 0f, blue)
                    Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                    getSquare().draw(mProjectionMatrix, color, 0f, 0f, 100f, 100f)
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, _ ->
            val param = Any()
            repeat(500) { renderer.renderFrontBufferedLayer(param) }
        }
    }

    @Test
    fun testDoubleBufferedContentsNotPersisted() {
        val mOrthoMatrix = FloatArray(16)
        val mProjectionMatrix = FloatArray(16)
        val screenWidth = SurfaceViewTestActivity.WIDTH
        val rectWidth = 10f

        val renderLatch = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {
                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    for (param in params) {
                        val left = screenWidth / 4 + (param as Float) - rectWidth / 2
                        val top = 0f
                        val right = left + rectWidth / 2
                        val bottom = 100f

                        Rectangle().draw(mProjectionMatrix, Color.RED, left, top, right, bottom)
                        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
                    }
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    renderLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        renderLatch.get()?.countDown()
                    }
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
            configureSurfaceView = { surfaceView -> surfaceView.setZOrderOnTop(true) }
        ) { _, renderer, surfaceView ->
            renderer.renderFrontBufferedLayer(0f)
            renderLatch.set(CountDownLatch(1))
            renderer.commit()
            assertTrue(renderLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            renderer.renderFrontBufferedLayer(screenWidth / 2f)
            renderLatch.set(CountDownLatch(1))
            renderer.commit()
            assertTrue(renderLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (bitmap.getPixel(coords[0] + width / 4, coords[1] + height / 2) == Color.WHITE) &&
                    (bitmap.getPixel(coords[0] + 3 * width / 4 - 1, coords[1] + height / 2) ==
                        Color.RED)
            }
        }
    }

    @Test
    fun testRenderAfterPauseAndResume() {
        val renderLatch = CountDownLatch(2)
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                val mProjectionMatrix = FloatArray(16)
                val mOrthoMatrix = FloatArray(16)

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
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

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
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
        verifyGLFrontBufferedRenderer(callbacks) { scenario, renderer, surfaceView ->
            renderer.renderFrontBufferedLayer(Any())
            // Navigate to stopped and resumed state to simulate returning to the application
            scenario
                .moveToState(Lifecycle.State.CREATED)
                .moveToState(Lifecycle.State.RESUMED)
                .onActivity { renderer.renderFrontBufferedLayer(Any()) }
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED == bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        }
    }

    @Test
    fun test180DegreeRotationBufferTransform() {
        val initialFrontBufferLatch = CountDownLatch(1)
        val secondFrontBufferLatch = CountDownLatch(1)
        var bufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
        var surfaceView: SurfaceView? = null
        var surfaceWidth = 0
        var surfaceHeight = 0
        val surfaceHolderCallbacks =
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(p0: SurfaceHolder) {
                    // NO-OP
                }

                override fun surfaceChanged(
                    p0: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    bufferTransform =
                        BufferTransformHintResolver().getBufferTransformHint(surfaceView!!)
                    surfaceWidth = width
                    surfaceHeight = height
                }

                override fun surfaceDestroyed(p0: SurfaceHolder) {
                    // NO-OP
                }
            }
        var configuredBufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                val mOrthoMatrix = FloatArray(16)
                val mProjectionMatrix = FloatArray(16)
                var mRectangle: Rectangle? = null

                private fun getSquare(): Rectangle =
                    mRectangle ?: Rectangle().also { mRectangle = it }

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
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
                    getSquare().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
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
                    getSquare().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    configuredBufferTransform =
                        transaction.mBufferTransforms[frontBufferedLayerSurfaceControl]
                            ?: BufferTransformHintResolver.UNKNOWN_TRANSFORM
                    if (initialFrontBufferLatch.count == 0L) {
                        secondFrontBufferLatch.countDown()
                    }
                    initialFrontBufferLatch.countDown()
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
            configureSurfaceView = { targetSurfaceView ->
                targetSurfaceView.holder.addCallback(surfaceHolderCallbacks)
                surfaceView = targetSurfaceView
            }
        ) { _, renderer, _ ->
            renderer.renderFrontBufferedLayer(Any())

            assertTrue(initialFrontBufferLatch.await(3000, TimeUnit.MILLISECONDS))

            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            assertTrue(automation.setRotation(UiAutomation.ROTATION_FREEZE_180))
            automation.waitForIdle(1000, 3000)

            renderer.renderFrontBufferedLayer(Any())

            assertTrue(secondFrontBufferLatch.await(3000, TimeUnit.MILLISECONDS))

            assertEquals(
                BufferTransformer().invertBufferTransform(bufferTransform),
                configuredBufferTransform
            )
        }
    }

    @Test
    fun testReleaseRemovedSurfaceCallbacks() {
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {
                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
                    // NO-OP
                }
            }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceViewTestActivity.TestSurfaceView? = null
        val createLatch = CountDownLatch(1)
        val destroyLatch = CountDownLatch(1)
        val scenario =
            ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrontBufferedRenderer(surfaceView!!, callbacks)
                    it.setOnDestroyCallback { destroyLatch.countDown() }
                    createLatch.countDown()
                }

        assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))
        // Capture surfaceView with local val to avoid Kotlin warnings regarding the surfaceView
        // parameter changing potentially
        val resolvedSurfaceView = surfaceView
        try {
            if (resolvedSurfaceView != null) {
                assertEquals(1, resolvedSurfaceView.getCallbackCount())
                val releaseLatch = CountDownLatch(1)
                renderer!!.release(true) { releaseLatch.countDown() }
                assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
                assertEquals(0, resolvedSurfaceView.getCallbackCount())
                renderer = null
            } else {
                fail("Unable to resolve SurfaceView, was the test Activity created?")
            }
        } finally {
            renderer?.blockingRelease()
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testSurfaceCallbackPreservedAfterResume() {
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {
                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
                    // NO-OP
                }
            }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceViewTestActivity.TestSurfaceView? = null
        val createLatch = CountDownLatch(1)
        val destroyLatch = CountDownLatch(1)
        val scenario =
            ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrontBufferedRenderer(surfaceView!!, callbacks)
                    it.setOnDestroyCallback { destroyLatch.countDown() }
                    createLatch.countDown()
                }
        assertTrue(createLatch.await(3000, TimeUnit.MILLISECONDS))

        val resumeLatch = CountDownLatch(1)
        var callbackCount = 0
        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            callbackCount = it.getSurfaceView().getCallbackCount()
            resumeLatch.countDown()
        }
        assertTrue(resumeLatch.await(3000, TimeUnit.MILLISECONDS))

        val pauseLatch = CountDownLatch(1)
        scenario.moveToState(Lifecycle.State.CREATED).onActivity { pauseLatch.countDown() }

        val returnToResumeLatch = CountDownLatch(1)
        scenario.moveToState(Lifecycle.State.RESUMED).onActivity { returnToResumeLatch.countDown() }
        assertTrue(returnToResumeLatch.await(3000, TimeUnit.MILLISECONDS))

        // Capture surfaceView with local val to avoid Kotlin warnings regarding the surfaceView
        // parameter changing potentially
        val resolvedSurfaceView = surfaceView
        try {
            if (resolvedSurfaceView != null) {

                assertEquals(callbackCount, resolvedSurfaceView.getCallbackCount())
                val releaseLatch = CountDownLatch(1)
                renderer!!.release(true) { releaseLatch.countDown() }
                assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
                assertEquals(0, resolvedSurfaceView.getCallbackCount())
                renderer = null
            } else {
                fail("Unable to resolve SurfaceView, was the test Activity created?")
            }
        } finally {
            renderer?.blockingRelease()
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testGLFrontBufferedRendererCreationFromUnstartedGLRenderer() {
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {
                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
                    // NO-OP
                }
            }
        val destroyLatch = CountDownLatch(1)
        val scenario =
            ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    assertThrows(IllegalStateException::class.java) {
                        GLFrontBufferedRenderer(it.getSurfaceView(), callbacks, GLRenderer())
                    }
                    it.setOnDestroyCallback { destroyLatch.countDown() }
                }
        scenario.moveToState(Lifecycle.State.DESTROYED)
        assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testMultiBufferedLayerRenderedOnSurfaceRedraw() {
        val renderLatch = CountDownLatch(1)
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    // NO-OP
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
                    renderLatch.countDown()
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { scenario, _, _ ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testFrontBufferClearAfterRender() {
        val frontLatch = AtomicReference<CountDownLatch?>()
        val commitLatch = AtomicReference<CountDownLatch?>()
        verifyGLFrontBufferedRenderer(
            object : GLFrontBufferedRenderer.Callback<Any> {

                private val mOrthoMatrix = FloatArray(16)
                private val mProjectionMatrix = FloatArray(16)

                // Should only render once
                private var mShouldRender = true

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    if (mShouldRender) {
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
                        mShouldRender = false
                    }
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
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
                    Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    commitLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        commitLatch.get()?.countDown()
                    }
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    frontLatch.get()?.countDown()
                                }
                            }
                        )
                    } else {
                        frontLatch.get()?.countDown()
                    }
                }
            }
        ) { _, renderer, surfaceView ->
            frontLatch.set(CountDownLatch(1))

            renderer.renderFrontBufferedLayer(1)

            assertTrue(frontLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            commitLatch.set(CountDownLatch(1))

            renderer.commit()

            assertTrue(commitLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            // Contents should be cleared and the front buffer should be visible but transparent
            frontLatch.set(CountDownLatch(1))

            renderer.renderFrontBufferedLayer(2)

            assertTrue(frontLatch.get()!!.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (abs(
                    Color.red(Color.BLUE) -
                        Color.red(bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2))
                ) < 2) &&
                    (abs(
                        Color.green(Color.BLUE) -
                            Color.green(
                                bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
                            )
                    ) < 2) &&
                    (abs(
                        Color.blue(Color.BLUE) -
                            Color.blue(
                                bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
                            )
                    ) < 2)
            }
        }
    }

    @Test
    fun testSetPixelFormat() {
        val flags = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        // First verify if another format other than RGBA_8888 is supported
        if (
            !HardwareBuffer.isSupported(
                1, // width
                1, // height
                HardwareBuffer.RGBA_FP16, // format
                1, // layers
                flags // flags
            )
        ) {
            return
        }

        var configLoaded = false
        val configLatch = CountDownLatch(1)
        val glRenderer =
            GLRenderer(
                    eglConfigFactory = {
                        val config = loadConfig(EGLConfigAttributes.RGBA_F16)
                        configLoaded = config != null
                        configLatch.countDown()
                        config ?: loadConfig(EGLConfigAttributes.RGBA_8888)!!
                    }
                )
                .apply { start("glRenderer") }

        data class ColorDepths(val red: Int, val green: Int, val blue: Int, val alpha: Int)

        fun obtainColorDepths(): ColorDepths {
            val result = IntArray(1)
            GLES20.glGetFramebufferAttachmentParameteriv(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES30.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE,
                result,
                0
            )
            val alpha = result[0]
            GLES20.glGetFramebufferAttachmentParameteriv(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES30.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE,
                result,
                0
            )
            val red = result[0]
            GLES20.glGetFramebufferAttachmentParameteriv(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES30.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE,
                result,
                0
            )
            val green = result[0]
            GLES20.glGetFramebufferAttachmentParameteriv(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES30.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE,
                result,
                0
            )
            val blue = result[0]
            return ColorDepths(red, green, blue, alpha)
        }

        var frontBufferColorDepth: ColorDepths? = null
        var multiBufferedColorDepth: ColorDepths? = null
        val latch = CountDownLatch(2)

        val callbacks =
            object : GLFrontBufferedRenderer.Callback<Any> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: Any
                ) {
                    frontBufferColorDepth = obtainColorDepths()
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<Any>
                ) {
                    multiBufferedColorDepth = obtainColorDepths()
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    latch.countDown()
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    latch.countDown()
                }
            }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView?
        val rendererCreatedLatch = CountDownLatch(1)
        val destroyLatch = CountDownLatch(1)
        var scenario: ActivityScenario<SurfaceViewTestActivity>? = null
        try {
            scenario =
                ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                    .moveToState(Lifecycle.State.CREATED)
                    .onActivity {
                        surfaceView = it.getSurfaceView()
                        renderer =
                            GLFrontBufferedRenderer(
                                surfaceView!!,
                                callbacks,
                                glRenderer,
                                bufferFormat = HardwareBuffer.RGBA_FP16
                            )
                        it.setOnDestroyCallback { destroyLatch.countDown() }
                        rendererCreatedLatch.countDown()
                    }
            scenario.moveToState(Lifecycle.State.RESUMED)

            configLatch.await(3000, TimeUnit.MILLISECONDS)
            if (!configLoaded) {
                // RBGAF16 not supported
                return
            }

            assertTrue(rendererCreatedLatch.await(3000, TimeUnit.MILLISECONDS))
            renderer!!.renderFrontBufferedLayer(Any())

            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
            Assert.assertNotNull(renderer)
            assertTrue(renderer!!.isValid())
            assertEquals(HardwareBuffer.RGBA_FP16, renderer?.bufferFormat)
            val rgb16 = ColorDepths(16, 16, 16, 16)
            assertEquals(rgb16, frontBufferColorDepth)
            assertEquals(rgb16, multiBufferedColorDepth)
        } finally {
            renderer.blockingRelease()
            glRenderer.stop(true)
            if (scenario != null) {
                scenario.moveToState(Lifecycle.State.DESTROYED)
                assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
            }
        }
    }

    @Test
    fun stillOneDrawOnSurfaceCreationAfterDestroySurfaceWithPendingCommit() {
        val multiBufferDraws: ConcurrentLinkedQueue<List<String>> = ConcurrentLinkedQueue()
        var afterSurfaceDestroyedMultiBufferDraws: List<List<String>>? = null
        val latchToCountDownOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val latchToAwaitOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {}

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    multiBufferDraws.add(params.toList())
                    latchToCountDownOnMultiLayerDraw.get()?.countDown()
                    latchToAwaitOnMultiLayerDraw.get()?.await(3, TimeUnit.SECONDS)
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
        ) { _, renderer, surfaceView ->
            val untilSurfaceDestroyed = CountDownLatch(1)
            val untilSurfaceCreated = CountDownLatch(1)
            val untilFirstDrawAfterSurfaceCreated = CountDownLatch(1)
            surfaceView.holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        untilSurfaceCreated.countDown()
                        latchToCountDownOnMultiLayerDraw.set(untilFirstDrawAfterSurfaceCreated)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        untilSurfaceDestroyed.countDown()
                    }
                }
            )

            // This draw is still ongoing when the view is hidden and the surface destroyed.
            val untilInitialDraw = CountDownLatch(1)
            latchToCountDownOnMultiLayerDraw.set(untilInitialDraw)
            latchToAwaitOnMultiLayerDraw.set(untilSurfaceCreated)
            renderer.renderFrontBufferedLayer("FIRST")
            renderer.commit()

            // This draw doesn't happen because pending callbacks are cancelled when the surface
            // is destroyed.
            renderer.renderFrontBufferedLayer("SECOND")
            renderer.commit()
            untilInitialDraw.await(3, TimeUnit.SECONDS)

            // When the view is hidden and restored, the initial draw of the surface should call
            // onDrawMultiBufferedLayer once, passing an empty list of params.
            surfaceView.post { surfaceView.setVisibility(View.INVISIBLE) }
            untilSurfaceDestroyed.await(3, TimeUnit.SECONDS)
            afterSurfaceDestroyedMultiBufferDraws = multiBufferDraws.toList()

            surfaceView.post { surfaceView.setVisibility(View.VISIBLE) }
            untilFirstDrawAfterSurfaceCreated.await(3, TimeUnit.SECONDS)

            // At the end of this block, renderer.release(false) is called to complete
            // in-progress renders.
        }
        assertEquals(listOf(listOf(), listOf("FIRST")), afterSurfaceDestroyedMultiBufferDraws)
        assertEquals(
            // The draw with empty parameter list at the end happens when the surface is created.
            // Before this fix, that incorrectly happened a second time.
            listOf(listOf(), listOf("FIRST"), listOf()),
            multiBufferDraws.toList()
        )
    }

    @Test
    fun noFrontDrawOnSurfaceCreationAfterDestroySurfaceWithPendingCommit() {
        val frontDrawParams = ConcurrentLinkedQueue<String>()
        val frontDrawCompleteCount = AtomicInteger(0)
        var afterSurfaceDestroyedFrontDrawParams: List<String>? = null
        var afterSurfaceDestroyedFrontDrawCount: Int? = null
        val latchToCountDownOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val latchToAwaitOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {
                    frontDrawParams.add(param)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    // This one is a little more awkward to record because the
                    // onDrawFrontBufferedLayer
                    // is in a block passed to ParamQueue.next, which doesn't run when the
                    // ParamQueue
                    // is actually empty. onFrontBufferedLayerRenderComplete gets called
                    // unconditionally
                    // so examine that instead.
                    frontDrawCompleteCount.incrementAndGet()
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    latchToCountDownOnMultiLayerDraw.get()?.countDown()
                    latchToAwaitOnMultiLayerDraw.get()?.await(3, TimeUnit.SECONDS)
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
        ) { _, renderer, surfaceView ->
            val untilSurfaceDestroyed = CountDownLatch(1)
            val untilSurfaceCreated = CountDownLatch(1)
            val untilFirstDrawAfterSurfaceCreated = CountDownLatch(1)
            surfaceView.holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        untilSurfaceCreated.countDown()
                        latchToCountDownOnMultiLayerDraw.set(untilFirstDrawAfterSurfaceCreated)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        untilSurfaceDestroyed.countDown()
                    }
                }
            )

            // This draw is still ongoing when the view is hidden and the surface destroyed.
            val untilInitialDraw = CountDownLatch(1)
            latchToCountDownOnMultiLayerDraw.set(untilInitialDraw)
            latchToAwaitOnMultiLayerDraw.set(untilSurfaceCreated)
            renderer.commit()

            // This draw doesn't happen because pending callbacks are cancelled when the surface
            // is destroyed.
            renderer.renderFrontBufferedLayer("TOSSED")
            untilInitialDraw.await(3, TimeUnit.SECONDS)

            // When the view is hidden and restored, the initial draw of the surface should call
            // onDrawMultiBufferedLayer once, passing an empty list of params.
            surfaceView.post { surfaceView.setVisibility(View.INVISIBLE) }
            untilSurfaceDestroyed.await(3, TimeUnit.SECONDS)
            afterSurfaceDestroyedFrontDrawParams = frontDrawParams.toList()
            afterSurfaceDestroyedFrontDrawCount = frontDrawCompleteCount.get()

            surfaceView.post { surfaceView.setVisibility(View.VISIBLE) }
            untilFirstDrawAfterSurfaceCreated.await(3, TimeUnit.SECONDS)

            // At the end of this block, renderer.release(false) is called to complete
            // in-progress renders.
        }
        assertTrue(afterSurfaceDestroyedFrontDrawParams!!.isEmpty())
        assertEquals(0, afterSurfaceDestroyedFrontDrawCount)
        assertTrue(frontDrawParams.isEmpty())
        // Before this fix, this would be 1, as the underlying render request would happen
        // (even though the user onDrawFrontBufferedLayer callback would not).
        assertEquals(0, frontDrawCompleteCount.get())
    }

    @Test
    fun stillOneDrawAfterClearWithPendingCommit() {
        val multiBufferDraws: ConcurrentLinkedQueue<List<String>> = ConcurrentLinkedQueue()
        var beforeClearMultiBufferDraws: List<List<String>>? = null
        val latchToCountDownOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val latchToAwaitOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {}

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    multiBufferDraws.add(params.toList())
                    latchToCountDownOnMultiLayerDraw.get()?.countDown()
                    latchToAwaitOnMultiLayerDraw.get()?.await(3, TimeUnit.SECONDS)
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, _ ->
            val untilInitialDraw = CountDownLatch(1)
            val untilAfterClear = CountDownLatch(1)
            latchToCountDownOnMultiLayerDraw.set(untilInitialDraw)
            latchToAwaitOnMultiLayerDraw.set(untilAfterClear)
            renderer.renderFrontBufferedLayer("STALLED")
            renderer.commit() // This draw is still in progress on clear
            renderer.renderFrontBufferedLayer("TOSSED")
            renderer.commit() // This draw is deferred on clear and never happens

            untilInitialDraw.await(3, TimeUnit.SECONDS)
            beforeClearMultiBufferDraws = multiBufferDraws.toList()
            renderer.clear()
            untilAfterClear.countDown()

            // At the end of this block, renderer.release(false) is called to complete
            // in-progress renders.
        }
        assertEquals(listOf(listOf(), listOf("STALLED")), beforeClearMultiBufferDraws)
        assertEquals(
            // The draw with empty parameter list at the end happens on clear.
            // Before this fix, that incorrectly happened a second time.
            listOf(listOf(), listOf("STALLED"), listOf()),
            multiBufferDraws.toList()
        )
    }

    @Test
    fun noFrontDrawAfterClearWithPendingCommit() {
        val frontDrawParams = ConcurrentLinkedQueue<String>()
        val frontDrawCompleteCount = AtomicInteger(0)
        var beforeClearFrontDrawParams: List<String>? = null
        var beforeClearFrontDrawCompleteCount: Int? = null
        val latchToCountDownOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val latchToAwaitOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {
                    frontDrawParams.add(param)
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    // This one is a little more awkward to record because the
                    // onDrawFrontBufferedLayer
                    // is in a block passed to ParamQueue.next, which doesn't run when the
                    // ParamQueue
                    // is actually empty. onFrontBufferedLayerRenderComplete gets called
                    // unconditionally
                    // so examine that instead.
                    frontDrawCompleteCount.incrementAndGet()
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    latchToCountDownOnMultiLayerDraw.get()?.countDown()
                    latchToAwaitOnMultiLayerDraw.get()?.await(3, TimeUnit.SECONDS)
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
        ) { _, renderer, _ ->
            val untilInitialDraw = CountDownLatch(1)
            val untilAfterClear = CountDownLatch(1)
            latchToCountDownOnMultiLayerDraw.set(untilInitialDraw)
            latchToAwaitOnMultiLayerDraw.set(untilAfterClear)
            renderer.commit() // This draw is still in progress on clear

            // This draw doesn't happen because pending draws are cancelled on clear.
            renderer.renderFrontBufferedLayer("TOSSED")
            untilInitialDraw.await(3, TimeUnit.SECONDS)
            beforeClearFrontDrawParams = frontDrawParams.toList()
            beforeClearFrontDrawCompleteCount = frontDrawCompleteCount.get()
            renderer.clear()
            untilAfterClear.countDown()

            // At the end of this block, renderer.release(false) is called to complete
            // in-progress renders.
        }
        assertTrue(beforeClearFrontDrawParams!!.isEmpty())
        assertEquals(0, beforeClearFrontDrawCompleteCount)
        assertTrue(frontDrawParams.isEmpty())
        // Before this fix, this would be 1, as the underlying render request would happen
        // (even though the user onDrawFrontBufferedLayer callback would not).
        assertEquals(0, frontDrawCompleteCount.get())
    }

    @Test
    fun deferredCommitsAreCompletedOnReleaseIfCancelPendingIsFalse() {
        val multiBufferDraws: ConcurrentLinkedQueue<List<String>> = ConcurrentLinkedQueue()
        var beforeStallMultiBufferDraws: List<List<String>>? = null
        val latchToCountDownOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val latchToAwaitOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {}

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    multiBufferDraws.add(params.toList())
                    latchToCountDownOnMultiLayerDraw.get()?.countDown()
                    latchToAwaitOnMultiLayerDraw.get()?.await(3, TimeUnit.SECONDS)
                }
            }
        verifyGLFrontBufferedRenderer(callbacks) { _, renderer, _ ->
            val untilInitialDraw = CountDownLatch(1)
            val untilAllRendersRequested = CountDownLatch(1)
            latchToCountDownOnMultiLayerDraw.set(untilInitialDraw)
            latchToAwaitOnMultiLayerDraw.set(untilAllRendersRequested)
            renderer.renderFrontBufferedLayer("STALLED")
            renderer.commit() // This draw gets stuck until the end of the block.
            untilInitialDraw.await(3, TimeUnit.SECONDS)
            renderer.renderFrontBufferedLayer("DEFERRED")
            renderer.commit() // This draw is deferred.

            // At the end of this block, renderer.release(false) is called to complete
            // in-progress renders.
            beforeStallMultiBufferDraws = multiBufferDraws.toList()
            untilAllRendersRequested.countDown()
        }
        assertEquals(listOf(listOf(), listOf("STALLED")), beforeStallMultiBufferDraws)
        assertEquals(
            listOf(listOf(), listOf("STALLED"), listOf("DEFERRED")),
            multiBufferDraws.toList()
        )
    }

    @Test
    fun deferredFrontBufferedRendersAreCompletedOnReleaseIfCancelPendingIsFalse() {
        val frontBufferDraws: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
        val latchToCountDownOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        val latchToAwaitOnMultiLayerDraw = AtomicReference<CountDownLatch?>()
        var beforeStallEndsFrontDraws: List<String>? = null
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {
                    frontBufferDraws.add(param)
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    latchToCountDownOnMultiLayerDraw.get()?.countDown()
                    latchToAwaitOnMultiLayerDraw.get()?.await(3, TimeUnit.SECONDS)
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
        ) { _, renderer, _ ->
            val untilAllRendersRequested = CountDownLatch(1)
            val untilInitialDraw = CountDownLatch(1)
            latchToCountDownOnMultiLayerDraw.set(untilInitialDraw)
            latchToAwaitOnMultiLayerDraw.set(untilAllRendersRequested)
            renderer.commit() // This draw gets stuck until the end of the block.
            untilInitialDraw.await(3, TimeUnit.SECONDS)
            renderer.renderFrontBufferedLayer("FIRST")
            renderer.renderFrontBufferedLayer("SECOND")

            // At the end of this block, renderer.release(false) is called to complete
            // in-progress renders.
            beforeStallEndsFrontDraws = frontBufferDraws.toList()
            untilAllRendersRequested.countDown()
        }
        // The assertion is here instead of above because if assert leaves the GL Thread stalled
        // the error is confusing.
        assertTrue(beforeStallEndsFrontDraws!!.isEmpty())
        assertEquals(listOf("FIRST", "SECOND"), frontBufferDraws.toList())
    }

    @Test
    fun multiBufferedLayerDrawDoesNotAllocateEmptySegment() {
        val callbacks =
            object : GLFrontBufferedRenderer.Callback<String> {

                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: String
                ) {}

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<String>
                ) {
                    // emptyList always returns the same singleton instance, which we use both for
                    // empty commits and any time a multi-layer draw is implcitly requested with no
                    // active segment in the queue (e.g. on surface creation).
                    assertSame(emptyList<String>(), params)
                }
            }
        verifyGLFrontBufferedRenderer(
            callbacks,
        ) { _, renderer, _ ->
            // commit with the current segment empty (no front draws).
            renderer.commit()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun GLFrontBufferedRenderer<*>?.blockingRelease(timeoutMillis: Long = 3000) {
        if (this != null) {
            val destroyLatch = CountDownLatch(1)
            release(false) { destroyLatch.countDown() }
            assertTrue(destroyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS))
        } else {
            fail("GLFrontBufferedRenderer is not initialized")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun <T> verifyGLFrontBufferedRenderer(
        callbacks: GLFrontBufferedRenderer.Callback<T>,
        createSurfaceView: (SurfaceViewTestActivity) -> SurfaceView = { it.getSurfaceView() },
        configureSurfaceView: (SurfaceView) -> Unit = {},
        assertFirstRender: (CountDownLatch) -> Unit = { latch ->
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        },
        block: FrontBufferTestCallback<T>
    ) {
        val firstRenderLatch = CountDownLatch(1)
        val wrappedCallbacks =
            object : GLFrontBufferedRenderer.Callback<T> {
                override fun onDrawFrontBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    param: T
                ) {
                    callbacks.onDrawFrontBufferedLayer(
                        eglManager,
                        width,
                        height,
                        bufferInfo,
                        transform,
                        param
                    )
                }

                override fun onDrawMultiBufferedLayer(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                    params: Collection<T>
                ) {
                    callbacks.onDrawMultiBufferedLayer(
                        eglManager,
                        width,
                        height,
                        bufferInfo,
                        transform,
                        params
                    )
                }

                override fun onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    callbacks.onFrontBufferedLayerRenderComplete(
                        frontBufferedLayerSurfaceControl,
                        transaction
                    )
                }

                override fun onMultiBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                    multiBufferedLayerSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction
                ) {
                    callbacks.onMultiBufferedLayerRenderComplete(
                        frontBufferedLayerSurfaceControl,
                        multiBufferedLayerSurfaceControl,
                        transaction
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    firstRenderLatch.countDown()
                                }
                            }
                        )
                    } else {
                        firstRenderLatch.countDown()
                    }
                }
            }
        var renderer: GLFrontBufferedRenderer<T>? = null
        var surfaceView: SurfaceView? = null
        val destroyLatch = CountDownLatch(1)
        var scenario: ActivityScenario<SurfaceViewTestActivity>? = null
        try {
            scenario =
                ActivityScenario.launch(SurfaceViewTestActivity::class.java)
                    .moveToState(Lifecycle.State.CREATED)
                    .onActivity {
                        surfaceView = createSurfaceView(it)
                        configureSurfaceView(surfaceView!!)
                        renderer = GLFrontBufferedRenderer(surfaceView!!, wrappedCallbacks)
                        it.setOnDestroyCallback { destroyLatch.countDown() }
                    }

            scenario.moveToState(Lifecycle.State.RESUMED)
            assertFirstRender(firstRenderLatch)

            block(scenario, renderer!!, surfaceView!!)
        } finally {
            renderer.blockingRelease()
            if (scenario != null) {
                scenario.moveToState(Lifecycle.State.DESTROYED)
                assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
            }
        }
    }
}

typealias FrontBufferTestCallback<T> =
    (
        scenario: ActivityScenario<SurfaceViewTestActivity>,
        renderer: GLFrontBufferedRenderer<T>,
        surfaceView: SurfaceView
    ) -> Unit
