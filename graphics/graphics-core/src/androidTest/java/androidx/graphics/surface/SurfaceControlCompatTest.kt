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

package androidx.graphics.surface

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Rect
import android.graphics.Region
import android.hardware.DataSpace
import android.opengl.EGL14
import android.os.Build
import android.os.SystemClock
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.EGLVersion
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.graphics.surface.SurfaceControlUtils.Companion.getSolidBuffer
import androidx.hardware.SyncFenceCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 29)
class SurfaceControlCompatTest {
    var executor: Executor? = null

    @Before
    fun setup() {
        executor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
    }

    private abstract class SurfaceHolderCallback : SurfaceHolder.Callback {
        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

        override fun surfaceDestroyed(p0: SurfaceHolder) {}
    }

    @Test
    fun testSurfaceControlCompatBuilder_parent() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        try {
            scenario.onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            SurfaceControlCompat.Builder()
                                .setParent(it.mSurfaceView)
                                .setName("SurfaceControlCompatTest")
                                .build()

                            callbackLatch.countDown()
                        }
                    }

                it.addSurface(it.getSurfaceView(), callback)
            }

            assertTrue(callbackLatch.await(3000, TimeUnit.MILLISECONDS))
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testSurfaceControlCompatBuilder_parentSurfaceControl() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        try {
            scenario.onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            val parentSc =
                                SurfaceControlCompat.Builder()
                                    .setParent(it.mSurfaceView)
                                    .setName("ParentSurfaceControl")
                                    .build()

                            SurfaceControlCompat.Builder()
                                .setParent(parentSc)
                                .setName("ChildSurfaceControl")
                                .build()

                            callbackLatch.countDown()
                        }
                    }

                it.addSurface(it.getSurfaceView(), callback)
            }
            assertTrue(callbackLatch.await(3000, TimeUnit.MILLISECONDS))
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testSurfaceTransactionCreate() {
        try {
            SurfaceControlCompat.Transaction()
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        }
    }

    class TransactionOnCommitListener : SurfaceControlCompat.TransactionCommittedListener {
        var mCallbackTime = -1L
        var mLatch = CountDownLatch(1)

        override fun onTransactionCommitted() {
            mCallbackTime = SystemClock.elapsedRealtime()
            mLatch.countDown()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testSurfaceTransactionOnCommitCallback() {
        val listener = TransactionOnCommitListener()

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        try {
            scenario.onActivity {
                SurfaceControlCompat.Transaction()
                    .addTransactionCommittedListener(executor!!, listener)
                    .commit()
            }

            listener.mLatch.await(3, TimeUnit.SECONDS)
            assertEquals(0, listener.mLatch.count)
            assertTrue(listener.mCallbackTime > 0)
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testSurfaceTransactionOnCommitCallback_multiple() {
        val listener = TransactionOnCommitListener()
        val listener2 = TransactionOnCommitListener()

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        try {
            scenario.onActivity {
                SurfaceControlCompat.Transaction()
                    .addTransactionCommittedListener(executor!!, listener)
                    .addTransactionCommittedListener(executor!!, listener2)
                    .commit()
            }

            listener.mLatch.await(3, TimeUnit.SECONDS)
            listener2.mLatch.await(3, TimeUnit.SECONDS)

            assertEquals(0, listener.mLatch.count)
            assertEquals(0, listener2.mLatch.count)

            assertTrue(listener.mCallbackTime > 0)
            assertTrue(listener2.mCallbackTime > 0)
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testSurfaceControlIsValid_valid() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
        try {
            scenario.onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            val scCompat =
                                SurfaceControlCompat.Builder()
                                    .setParent(it.getSurfaceView())
                                    .setName("SurfaceControlCompatTest")
                                    .build()

                            assertTrue(scCompat.isValid())
                            callbackLatch.countDown()
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }

            assertTrue(callbackLatch.await(3000, TimeUnit.MILLISECONDS))
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testSurfaceControlIsValid_validNotValid() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
        try {
            scenario.onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            val scCompat =
                                SurfaceControlCompat.Builder()
                                    .setParent(it.getSurfaceView())
                                    .setName("SurfaceControlCompatTest")
                                    .build()

                            assertTrue(scCompat.isValid())
                            scCompat.release()
                            assertFalse(scCompat.isValid())

                            callbackLatch.countDown()
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }
            assertTrue(callbackLatch.await(3000, TimeUnit.MILLISECONDS))
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testSurfaceControlIsValid_multipleReleases() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
        try {
            scenario.onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            val scCompat =
                                SurfaceControlCompat.Builder()
                                    .setParent(it.getSurfaceView())
                                    .setName("SurfaceControlCompatTest")
                                    .build()

                            assertTrue(scCompat.isValid())
                            scCompat.release()
                            scCompat.release()
                            assertFalse(scCompat.isValid())

                            callbackLatch.countDown()
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }

            assertTrue(callbackLatch.await(3000, TimeUnit.MILLISECONDS))
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testTransactionReparent_null() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .reparent(scCompat, null)
        }) { bitmap, rect ->
            Color.BLACK == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testSetBufferInvokesPreviousReleaseCallback() {
        val buffer1 =
            SurfaceControlUtils.getSolidBuffer(
                SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                Color.BLUE
            )
        val buffer2 =
            SurfaceControlUtils.getSolidBuffer(
                SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                Color.RED
            )
        var releaseCallbackInvoked = false
        val setBuffer1Latch = CountDownLatch(1)

        try {
            verifySurfaceControlTest({ surfaceView ->
                val scCompat =
                    SurfaceControlCompat.Builder()
                        .setParent(surfaceView)
                        .setName("SurfaceControlCompatTest")
                        .build()
                SurfaceControlCompat.Transaction()
                    .setBuffer(scCompat, buffer1) {
                        releaseCallbackInvoked = true
                        setBuffer1Latch.countDown()
                    }
                    .commit()

                var visibility = false
                repeat(3) {
                    SurfaceControlCompat.Transaction().setVisibility(scCompat, visibility).commit()
                    visibility = !visibility
                }

                assertFalse(setBuffer1Latch.await(1000, TimeUnit.MILLISECONDS))
                assertFalse(releaseCallbackInvoked)

                SurfaceControlCompat.Transaction().setBuffer(scCompat, buffer2) {
                    // NO-OP
                }
            }) { _, _ ->
                setBuffer1Latch.await(3000, TimeUnit.MILLISECONDS) && releaseCallbackInvoked
            }
        } finally {
            buffer1.close()
            buffer2.close()
        }
    }

    @Test
    fun testTransactionReparent_childOfSibling() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()
            val scCompat2 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            val buffer2 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer2)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setBuffer(scCompat2, buffer2)
                .setVisibility(scCompat, true)
                .setVisibility(scCompat2, true)
                .apply { reparent(scCompat, scCompat2) }
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBuffer_nullFence() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer, null)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBuffer_simpleFence() {
        var eglManager: EGLManager? = null
        verifySurfaceControlTest({ surfaceView ->
            val manager =
                EGLManager().apply {
                    initialize()
                    val config = loadConfig(EGLConfigAttributes.RGBA_8888)
                    if (config == null) {
                        fail("Config 8888 should be supported")
                    }
                    createContext(config!!)
                }
            eglManager = manager

            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            val fence =
                if (manager.supportsNativeAndroidFence()) {
                    SyncFenceCompat.createNativeSyncFence()
                } else {
                    null
                }
            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer, fence)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
        eglManager?.release()
    }

    @Test
    fun testTransactionSetBuffer_nullCallback() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBuffer_singleReleaseCallback() {
        val releaseLatch = CountDownLatch(1)
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer)

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer2 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer2)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer) { releaseFence ->
                    releaseFence.awaitForever()
                    releaseLatch.countDown()
                }
                .setVisibility(scCompat, true)
                .commit()

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer2)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBuffer_multipleReleaseCallbacksAndOverwriteWithSingleSC() {
        val releaseLatch = CountDownLatch(1)
        val releaseLatch2 = CountDownLatch(1)

        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer)

            val buffer2 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer2)

            val buffer3 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer3)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer) { releaseFence ->
                    releaseFence.awaitForever()
                    releaseLatch.countDown()
                }
                .setBuffer(scCompat, buffer2) { releaseFence ->
                    releaseFence.awaitForever()
                    assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
                    releaseLatch2.countDown()
                }
                .setVisibility(scCompat, true)
                .commit()

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer3)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            assertTrue(releaseLatch2.await(3000, TimeUnit.MILLISECONDS))
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBuffer_multipleNullCallbacksWithOneNonNull() {
        val releaseLatch = CountDownLatch(1)
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer)

            val buffer2 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer2)

            val buffer3 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer3)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setBuffer(scCompat, buffer3) { releaseFence ->
                    releaseFence.awaitForever()
                    releaseLatch.countDown()
                }
                .setBuffer(scCompat, buffer2)
                .setVisibility(scCompat, true)
                .commit()

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer3)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBuffer_ReleaseCallbacksAndOverwriteWithMultipleSC() {
        val releaseLatch = CountDownLatch(1)
        val releaseLatch2 = CountDownLatch(1)
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()
            val scCompat2 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer)

            val buffer2 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.GREEN
                )
            assertNotNull(buffer2)

            val buffer3 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer3)
            val buffer4 =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer3)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer) { releaseFence ->
                    releaseFence.awaitForever()
                    releaseLatch.countDown()
                }
                .setBuffer(scCompat2, buffer2) { releaseFence ->
                    releaseFence.awaitForever()
                    releaseLatch2.countDown()
                }
                .setVisibility(scCompat, true)
                .setVisibility(scCompat2, true)
                .commit()

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer3)
                .setBuffer(scCompat2, buffer4)
                .setVisibility(scCompat, true)
                .setVisibility(scCompat2, true)
        }) { bitmap, rect ->
            assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
            assertTrue(releaseLatch2.await(3000, TimeUnit.MILLISECONDS))
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetVisibility_show() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetVisibility_hide() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            assertNotNull(buffer)

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, false)
        }) { bitmap, rect ->
            Color.BLACK == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetLayer_zero() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat1 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()
            val scCompat2 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            SurfaceControlCompat.Transaction()
                .setLayer(scCompat1, 1)
                .setBuffer(
                    scCompat1,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                )
                .setVisibility(scCompat1, true)
                .setLayer(scCompat2, 0)
                .setBuffer(
                    scCompat2,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.GREEN
                    )
                )
                .setVisibility(scCompat2, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetLayer_positive() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat1 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()
            val scCompat2 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            SurfaceControlCompat.Transaction()
                .setLayer(scCompat1, 1)
                .setBuffer(
                    scCompat1,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.GREEN
                    )
                )
                .setVisibility(scCompat1, true)
                .setLayer(scCompat2, 24)
                .setBuffer(
                    scCompat2,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                )
                .setVisibility(scCompat2, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetLayer_negative() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat1 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()
            val scCompat2 =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            SurfaceControlCompat.Transaction()
                .setLayer(scCompat1, 1)
                .setBuffer(
                    scCompat1,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                )
                .setVisibility(scCompat1, true)
                .setLayer(scCompat2, -7)
                .setBuffer(
                    scCompat2,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.GREEN
                    )
                )
                .setVisibility(scCompat2, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetDamageRegion_all() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            SurfaceControlCompat.Transaction()
                .setDamageRegion(
                    scCompat,
                    Region(
                        0,
                        0,
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT
                    )
                )
                .setBuffer(
                    scCompat,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                )
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetDamageRegion_null() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            SurfaceControlCompat.Transaction()
                .setDamageRegion(scCompat, null)
                .setBuffer(
                    scCompat,
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                )
                .setVisibility(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetBufferTransparency_opaque() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setOpaque(scCompat, true)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetNullBuffer() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setOpaque(scCompat, false)
                .commit()

            SurfaceControlCompat.Transaction().setBuffer(scCompat, null)
        }) { bitmap, rect ->
            Color.BLACK == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetAlpha_0_0() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setOpaque(scCompat, false)
                .setAlpha(scCompat, 0.0f)
        }) { bitmap, rect ->
            Color.BLACK == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    fun testTransactionSetAlpha_0_5() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setAlpha(scCompat, 0.5f)
        }) { bitmap, rect ->
            val left = rect.left
            val top = rect.top
            val fConnector: ColorSpace.Connector =
                ColorSpace.connect(ColorSpace.get(ColorSpace.Named.SRGB), bitmap.colorSpace!!)

            val red = fConnector.transform(1.0f, 0.0f, 0.0f)
            val black = fConnector.transform(0.0f, 0.0f, 0.0f)
            val expectedResult =
                Color.valueOf(red[0], red[1], red[2], 0.5f)
                    .compositeOver(Color.valueOf(black[0], black[1], black[2], 1.0f))

            (Math.abs(expectedResult.red() - bitmap.getColor(left, top).red()) < 2.5e-3f) &&
                (Math.abs(expectedResult.green() - bitmap.getColor(left, top).green()) < 2.5e-3f) &&
                (Math.abs(expectedResult.blue() - bitmap.getColor(left, top).blue()) < 2.5e-3f)
        }
    }

    @Test
    fun testTransactionSetAlpha_1_0() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )
            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setAlpha(scCompat, 1.0f)
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetCrop_null() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setCrop(scCompat, null)
        }) { bitmap, rect ->
            SurfaceControlUtils.checkNullCrop(bitmap, intArrayOf(rect.left, rect.top))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetCrop_standardCrop() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setCrop(scCompat, Rect(20, 30, 90, 60))
        }) { bitmap, rect ->
            SurfaceControlUtils.checkStandardCrop(bitmap, intArrayOf(rect.left, rect.top))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetCrop_standardThenNullCrop() {
        val destroyLatch = CountDownLatch(1)
        val listener = TransactionOnCommitListener()
        var scCompat: SurfaceControlCompat? = null
        var surfaceView: SurfaceView? = null
        val scenario =
            ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java).onActivity {
                it.setDestroyCallback { destroyLatch.countDown() }
                surfaceView = it.mSurfaceView
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            scCompat =
                                SurfaceControlCompat.Builder()
                                    .setParent(it.getSurfaceView())
                                    .setName("SurfaceControlCompatTest")
                                    .build()

                            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                            val buffer =
                                SurfaceControlUtils.getSolidBuffer(
                                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )

                            SurfaceControlCompat.Transaction()
                                .addTransactionCommittedListener(executor!!, listener)
                                .setBuffer(scCompat!!, buffer)
                                .setVisibility(scCompat!!, true)
                                .setCrop(scCompat!!, Rect(20, 30, 90, 60))
                                .commit()
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }

        assertTrue(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))

        try {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                surfaceView!!.getLocationOnScreen(coord)
                SurfaceControlUtils.checkStandardCrop(bitmap, coord)
            }

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            val cropLatchListener = TransactionOnCommitListener()
            SurfaceControlCompat.Transaction()
                .addTransactionCommittedListener(executor!!, cropLatchListener)
                .setBuffer(scCompat!!, buffer)
                .setVisibility(scCompat!!, true)
                .setCrop(scCompat!!, Rect(0, 0, 0, 0))
                .commit()

            assertTrue(cropLatchListener.mLatch.await(3000, TimeUnit.MILLISECONDS))

            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                surfaceView!!.getLocationOnScreen(coord)
                SurfaceControlUtils.checkNullCrop(bitmap, coord)
            }
        } finally {
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetPosition() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setPosition(scCompat, 30f, 30f)
        }) { bitmap, rect ->
            val left = rect.left
            val top = rect.top
            Color.BLACK ==
                bitmap.getPixel(
                    left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                    top + 29
                ) &&
                Color.BLACK ==
                    bitmap.getPixel(
                        left + 29,
                        top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    ) &&
                Color.RED == bitmap.getPixel(left + 30, top + 30)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetScale() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setScale(scCompat, 0.5f, 0.5f)
        }) { bitmap, rect ->
            val left = rect.left
            val top = rect.top
            Color.RED == bitmap.getPixel(left, top) &&
                Color.RED ==
                    bitmap.getPixel(
                        left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                        top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                    ) &&
                // Scale reduced by 50%, so should be black here
                Color.BLACK ==
                    bitmap.getPixel(
                        left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testTransactionSetBufferTransform_identity() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getQuadrantBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setBufferTransform(scCompat, SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
        }) { bitmap, rect ->
            val left = rect.left
            val top = rect.top
            // Check outer bounds of square to ensure its scaled correctly
            Color.RED == bitmap.getPixel(left, top) &&
                Color.RED ==
                    bitmap.getPixel(
                        left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                        top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                    ) &&
                Color.BLACK ==
                    bitmap.getPixel(
                        left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testTransactionSetBufferTransform_singleTransform() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getQuadrantBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setBufferTransform(
                    scCompat,
                    SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_HORIZONTAL
                )
        }) { bitmap, rect ->
            // Ensure it actually rotated by checking its outer bounds are black
            Color.BLACK ==
                bitmap.getPixel(
                    rect.left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                    rect.top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 4 - 1
                ) &&
                Color.BLACK ==
                    bitmap.getPixel(
                        rect.left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH * 3 / 4,
                        rect.top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    ) &&
                Color.RED ==
                    bitmap.getPixel(
                        rect.left + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 + 1,
                        rect.top + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                    )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testSurfaceTransactionCommitOnDraw() {
        verifySurfaceControlTest({ surfaceView ->
            val scCompat =
                SurfaceControlCompat.Builder()
                    .setParent(surfaceView)
                    .setName("SurfaceControlCompatTest")
                    .build()

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlCompat.Transaction()
                .setBuffer(scCompat, buffer)
                .setVisibility(scCompat, true)
                .setAlpha(scCompat, 1.0f)
                .apply { commitTransactionOnDraw(surfaceView.rootSurfaceControl!!) }
        }) { bitmap, rect ->
            Color.RED == bitmap.getPixel(rect.left, rect.top)
        }
    }

    @SuppressLint("NewApi")
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testSetExtendedRangeBrightnessThrowsOnUnsupportedPlatforms() {
        ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {

                            assertThrows(UnsupportedOperationException::class.java) {
                                val surfaceControl =
                                    SurfaceControlCompat.Builder()
                                        .setName("testSurfaceControl")
                                        .setParent(it.mSurfaceView)
                                        .build()
                                SurfaceControlCompat.Transaction()
                                    .setExtendedRangeBrightness(surfaceControl, 1.0f, 2.0f)
                                    .commit()
                            }
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetFrameRate120WithDefaultCompatibilityAndAlwaysChangeStrategy() {
        testFrameRate(
            120f,
            SurfaceControlCompat.FRAME_RATE_COMPATIBILITY_DEFAULT,
            SurfaceControlCompat.CHANGE_FRAME_RATE_ALWAYS
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetFrameRateNegativeWithDefaultCompatibilityAndAlwaysChangeStrategy() {
        testFrameRate(
            -50f,
            SurfaceControlCompat.FRAME_RATE_COMPATIBILITY_DEFAULT,
            SurfaceControlCompat.CHANGE_FRAME_RATE_ALWAYS
        )
    }

    @SuppressLint("NewApi")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetFrameRateZeroWithDefaultCompatibilityAndAlwaysChangeStrategy() {
        testFrameRate(
            0f,
            SurfaceControlCompat.FRAME_RATE_COMPATIBILITY_DEFAULT,
            SurfaceControlCompat.CHANGE_FRAME_RATE_ALWAYS
        )
    }

    @SuppressLint("NewApi")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetFrameRateInvalidCompatibility() {
        testFrameRate(120f, 42, SurfaceControlCompat.CHANGE_FRAME_RATE_ALWAYS)
    }

    @SuppressLint("NewApi")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSetFrameRateInvalidStrategy() {
        testFrameRate(120f, SurfaceControlCompat.FRAME_RATE_COMPATIBILITY_DEFAULT, 108)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testClearFrameRate() {
        ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java).onActivity {
            val callback =
                object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {

                        val surfaceControl =
                            SurfaceControlCompat.Builder()
                                .setName("testSurfaceControl")
                                .setParent(it.mSurfaceView)
                                .build()
                        SurfaceControlCompat.Transaction().clearFrameRate(surfaceControl).commit()
                    }
                }

            it.addSurface(it.mSurfaceView, callback)
        }
    }

    private fun testFrameRate(frameRate: Float, compatibility: Int, strategy: Int) {
        ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java).onActivity {
            val callback =
                object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {

                        val surfaceControl =
                            SurfaceControlCompat.Builder()
                                .setName("testSurfaceControl")
                                .setParent(it.mSurfaceView)
                                .build()
                        SurfaceControlCompat.Transaction()
                            .setFrameRate(surfaceControl, frameRate, compatibility, strategy)
                            .commit()
                    }
                }

            it.addSurface(it.mSurfaceView, callback)
        }
    }

    @SuppressLint("NewApi")
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.S_V2)
    @Test
    fun testSetDataSpaceThrowsOnUnsupportedPlatforms() {
        ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {

                            assertThrows(UnsupportedOperationException::class.java) {
                                val surfaceControl =
                                    SurfaceControlCompat.Builder()
                                        .setName("testSurfaceControl")
                                        .setParent(it.mSurfaceView)
                                        .build()

                                val extendedDataspace =
                                    DataSpace.pack(
                                        DataSpace.STANDARD_BT709,
                                        DataSpace.TRANSFER_SRGB,
                                        DataSpace.RANGE_EXTENDED
                                    )
                                SurfaceControlCompat.Transaction()
                                    .setDataSpace(surfaceControl, extendedDataspace)
                                    .commit()
                            }
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testSetExtendedRangeBrightness() {
        val destroyLatch = CountDownLatch(1)
        val scenario =
            ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java).onActivity {
                it.setDestroyCallback { destroyLatch.countDown() }
                val display = it.display
                assertNotNull(display)
                if (display!!.isHdrSdrRatioAvailable) {
                    assertEquals(1.0f, display.hdrSdrRatio, .0001f)
                }

                it.window.attributes.screenBrightness = 0.01f
                val hdrReady = CountDownLatch(1)
                val listenerErrors = arrayOfNulls<Exception>(1)
                if (display.isHdrSdrRatioAvailable) {
                    display.registerHdrSdrRatioChangedListener(
                        executor!!,
                        object : Consumer<Display?> {
                            var mIsRegistered = true

                            override fun accept(updatedDisplay: Display?) {
                                try {
                                    assertEquals(display.displayId, updatedDisplay!!.displayId)
                                    assertTrue(mIsRegistered)
                                    if (display.hdrSdrRatio > 2f) {
                                        hdrReady.countDown()
                                        display.unregisterHdrSdrRatioChangedListener(this)
                                        mIsRegistered = false
                                    }
                                } catch (e: Exception) {
                                    synchronized(it) {
                                        listenerErrors[0] = e
                                        hdrReady.countDown()
                                    }
                                }
                            }
                        }
                    )
                } else {
                    assertThrows(IllegalStateException::class.java) {
                        display.registerHdrSdrRatioChangedListener(
                            executor!!,
                            Consumer { _: Display? -> }
                        )
                    }
                }
                val extendedDataspace =
                    DataSpace.pack(
                        DataSpace.STANDARD_BT709,
                        DataSpace.TRANSFER_SRGB,
                        DataSpace.RANGE_EXTENDED
                    )
                val buffer =
                    getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.RED
                    )
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            val scCompat =
                                SurfaceControlCompat.Builder()
                                    .setParent(it.getSurfaceView())
                                    .setName("SurfaceControlCompatTest")
                                    .build()

                            SurfaceControlCompat.Transaction()
                                .setBuffer(scCompat, buffer)
                                .setDataSpace(scCompat, extendedDataspace)
                                .setExtendedRangeBrightness(scCompat, 1.0f, 3.0f)
                                .setVisibility(scCompat, true)
                                .commit()
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }

        try {
            scenario.onActivity {
                SurfaceControlUtils.validateOutput(it.window) { bitmap ->
                    val coord = intArrayOf(0, 0)
                    it.mSurfaceView.getLocationInWindow(coord)
                    val topLeft = bitmap.getPixel(coord[0] + 2, coord[1] + 2)
                    val topRight =
                        bitmap.getPixel(coord[0] + it.mSurfaceView.width - 2, coord[1] + 2)
                    val bottomLeft =
                        bitmap.getPixel(coord[0] + 2, coord[1] + it.mSurfaceView.height - 2)
                    val bottomRight =
                        bitmap.getPixel(
                            coord[0] + it.mSurfaceView.width - 2,
                            coord[1] + it.mSurfaceView.height - 2
                        )

                    Color.RED == topLeft &&
                        topLeft == topRight &&
                        bottomLeft == topRight &&
                        bottomLeft == bottomRight
                }
            }
        } finally {
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun verifySurfaceControlTest(
        createTransaction: (SurfaceView) -> SurfaceControlCompat.Transaction,
        verifyOutput: (Bitmap, Rect) -> Boolean = { _, _ -> true }
    ) {
        SurfaceControlUtils.surfaceControlTestHelper(
            { surfaceView, latch ->
                val transaction = createTransaction(surfaceView)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        executor!!,
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                latch.countDown()
                            }
                        }
                    )
                } else {
                    latch.countDown()
                }
                transaction.commit()
            },
            verifyOutput
        )
    }

    fun Color.compositeOver(background: Color): Color {
        val fg = this.convert(background.colorSpace)

        val bgA = background.alpha()
        val fgA = fg.alpha()
        val a = fgA + (bgA * (1f - fgA))

        val r = compositeComponent(fg.red(), background.red(), fgA, bgA, a)
        val g = compositeComponent(fg.green(), background.green(), fgA, bgA, a)
        val b = compositeComponent(fg.blue(), background.blue(), fgA, bgA, a)

        return Color.valueOf(r, g, b, a, background.colorSpace)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun compositeComponent(
        fgC: Float,
        bgC: Float,
        fgA: Float,
        bgA: Float,
        a: Float
    ) = if (a == 0f) 0f else ((fgC * fgA) + ((bgC * bgA) * (1f - fgA))) / a

    // Helper method to create and initialize an EGLManager
    fun createAndSetupEGLManager(eglSpec: EGLSpec = EGLSpec.V14): EGLManager {
        val egl = EGLManager(eglSpec)
        assertEquals(EGLVersion.Unknown, egl.eglVersion)
        assertEquals(EGL14.EGL_NO_CONTEXT, egl.eglContext)

        egl.initialize()

        val config = egl.loadConfig(EGLConfigAttributes.RGBA_8888)
        if (config == null) {
            fail("Config 888 should be supported")
        }

        egl.createContext(config!!)
        return egl
    }

    // Helper method to release EGLManager
    fun releaseEGLManager(egl: EGLManager) {
        egl.release()
        assertEquals(EGLVersion.Unknown, egl.eglVersion)
        assertEquals(EGL14.EGL_NO_CONTEXT, egl.eglContext)
    }
}
