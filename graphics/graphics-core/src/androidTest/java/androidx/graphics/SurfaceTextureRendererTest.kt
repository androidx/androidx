/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics

import android.graphics.Color
import android.graphics.RenderNode
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.graphics.SurfaceTextureRendererTest.TestHelpers.Companion.createSurfaceTextureRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SurfaceTextureRendererTest {

    companion object {
        const val TEST_WIDTH = 20
        const val TEST_HEIGHT = 20
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderFrameInvokesCallback() {
        withHandlerThread { handler ->
            val renderLatch = CountDownLatch(1)
            val renderer = createSurfaceTextureRenderer(handler = handler) {
                renderLatch.countDown()
            }
            renderer.renderFrame()
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            renderer.release()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderAfterReleaseDoesNotRender() {
        withHandlerThread { handler ->
            val renderNode = RenderNode("renderNode")
            val renderLatch = CountDownLatch(1)
            val renderer = SurfaceTextureRenderer(renderNode, 100, 100, handler) {
                renderLatch.countDown()
            }
            renderer.release()
            renderer.renderFrame()
            assertFalse(renderLatch.await(1000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiReleasesDoesNotCrash() {
        withHandlerThread { handler ->
            val renderer = createSurfaceTextureRenderer(handler = handler) {
                // NO-OP
            }
            renderer.release()
            renderer.release()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultipleRenderFramesRequests() {
        withHandlerThread { handler ->
            var renderLatch = CountDownLatch(1)
            val glRenderer = GLRenderer().apply { start() }
            val node = RenderNode("node").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
            }
            var attached = false
            var texture: SurfaceTexture? = null
            val target = glRenderer.createRenderTarget(
                TEST_WIDTH,
                TEST_HEIGHT,
                object : GLRenderer.RenderCallback {
                    override fun onDrawFrame(eglManager: EGLManager) {
                        if (!attached) {
                            val tex = IntArray(1)
                            GLES20.glGenTextures(1, tex, 0)
                            texture!!.attachToGLContext(tex[0])
                            attached = true
                        }

                        texture!!.updateTexImage()
                        renderLatch.countDown()
                    }
                })
            val renderer = createSurfaceTextureRenderer(renderNode = node, handler = handler) {
                    surfaceTexture ->
                texture = surfaceTexture
                target.requestRender()
            }

            var canvas = node.beginRecording()
            canvas.drawColor(Color.RED)
            node.endRecording()

            renderer.renderFrame() // 1

            canvas = node.beginRecording()
            canvas.drawColor(Color.YELLOW)
            node.endRecording()

            renderer.renderFrame() // 2

            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            canvas = node.beginRecording()
            canvas.drawColor(Color.BLUE)
            node.endRecording()

            renderLatch = CountDownLatch(1)

            renderer.renderFrame() // 3

            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            renderer.release()
        }
    }

    /**
     * Static inner class to enclose helper methods that rely on APIs that were introduced in newer
     * versions of Android. Even though the individual tests are gated with SdkSuppress annotations,
     * the test runners reflectively iterate through every function of a test class. As a result if
     * a function refers to new APIs in the method signatures they can crash. So move these methods
     * into a separate class that will only be inspected upon first use with an entry point of the
     * test methods defined above, on the appropriate API level.
     */
    internal class TestHelpers {
        companion object {
            @RequiresApi(Build.VERSION_CODES.Q)
            fun createTestRenderNode(): RenderNode =
                RenderNode("node").apply {
                    setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                    val canvas = beginRecording()
                    canvas.drawColor(Color.RED)
                    endRecording()
                }

            @RequiresApi(Build.VERSION_CODES.Q)
            fun createSurfaceTextureRenderer(
                renderNode: RenderNode = createTestRenderNode(),
                width: Int = TEST_WIDTH,
                height: Int = TEST_WIDTH,
                handler: Handler = Handler(Looper.getMainLooper()),
                block: (SurfaceTexture) -> Unit = {}
            ) = SurfaceTextureRenderer(renderNode, width, height, handler, block)
        }
    }

    private fun withHandlerThread(block: (Handler) -> Unit) {
        val handlerThread = HandlerThread("rendererCallbackThread").apply { start() }
        val handler = Handler(handlerThread.looper)
        try {
            block(handler)
        } catch (exception: Exception) {
            fail("Exception thrown in test case: " + exception.message)
        } finally {
            handlerThread.quit()
        }
    }
}
