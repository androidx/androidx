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

package androidx.graphics.surface

import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Region
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.SystemClock
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceHolder
import androidx.hardware.SyncFenceCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
        }
    }

    @Test
    fun testCreateFromWindow() {
        var surfaceControl = SurfaceControl.Builder()
            .setName("SurfaceControlCompact_createFromWindow")
            .build()
        try {
            SurfaceControlCompat.Builder(Surface(surfaceControl))
                .setDebugName("SurfaceControlCompatTest")
                .build()
        } catch (e: IllegalArgumentException) {
            fail()
        }
    }

    @Test
    fun testSurfaceControlCompatBuilder_surfaceControlParent() {
        val surfaceControl = SurfaceControl.Builder()
            .setName("SurfaceControlCompact_createFromWindow")
            .build()
        try {
            SurfaceControlCompat.Builder(
                SurfaceControlCompat(
                    Surface(surfaceControl),
                    null,
                    "SurfaceControlCompatTest"
                )
            )
                .setDebugName("SurfaceControlCompatTest")
                .build()
        } catch (e: IllegalArgumentException) {
            fail()
        }
    }

    @Test
    fun testSurfaceControlCompatBuilder_surfaceParent() {
        val surfaceControl = SurfaceControl.Builder()
            .setName("SurfaceControlCompact_createFromWindow")
            .build()
        try {
            SurfaceControlCompat.Builder(Surface(surfaceControl))
                .setDebugName("SurfaceControlCompatTest")
                .build()
        } catch (e: IllegalArgumentException) {
            fail()
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

    class TransactionOnCompleteListener : SurfaceControlCompat.TransactionCompletedListener {
        var mCallbackTime = -1L
        var mLatchTime = -1L
        var mPresentTime = -1L
        var mLatch = CountDownLatch(1)

        override fun onComplete(latchTimeNanos: Long, presentTimeNanos: Long) {
            mCallbackTime = SystemClock.elapsedRealtime()
            mLatchTime = latchTimeNanos
            mPresentTime = presentTimeNanos
            mLatch.countDown()
        }
    }

    class TransactionOnCommitListener : SurfaceControlCompat.TransactionCommittedListener {
        var mCallbackTime = -1L
        var mLatchTime = -1L
        var mPresentTime = -1L
        var mLatch = CountDownLatch(1)

        override fun onCommit(latchTimeNanos: Long, presentTimeNanos: Long) {
            mCallbackTime = SystemClock.elapsedRealtime()
            mLatchTime = latchTimeNanos
            mPresentTime = presentTimeNanos
            mLatch.countDown()
        }
    }

    @Test
    fun testSurfaceTransactionOnCompleteCallback() {
        val listener = TransactionOnCompleteListener()

        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)

        try {
            scenario.onActivity {
                SurfaceControlCompat.Transaction()
                    .addTransactionCompletedListener(executor!!, listener)
                    .commit()
            }

            scenario.moveToState(Lifecycle.State.RESUMED)

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
    fun testSurfaceTransactionOnCommitCallback() {
        val listener = TransactionOnCommitListener()

        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)

        try {
            scenario.onActivity {
                SurfaceControlCompat.Transaction()
                    .addTransactionCommittedListener(executor!!, listener)
                    .commit()
            }
            scenario.moveToState(Lifecycle.State.RESUMED)

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

        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)

        try {
            scenario.onActivity {
                SurfaceControlCompat.Transaction()
                    .addTransactionCommittedListener(executor!!, listener)
                    .addTransactionCommittedListener(executor!!, listener2)
                    .commit()
            }

            scenario.moveToState(Lifecycle.State.RESUMED)

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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testSurfaceTransactionOnCommitCallbackAndOnCompleteCallback() {
        val listener1 = TransactionOnCommitListener()
        val listener2 = TransactionOnCompleteListener()

        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)

        try {
            scenario.onActivity {
                SurfaceControlCompat.Transaction()
                    .addTransactionCommittedListener(executor!!, listener1)
                    .addTransactionCompletedListener(executor!!, listener2)
                    .commit()
            }

            scenario.moveToState(Lifecycle.State.RESUMED)

            listener1.mLatch.await(3, TimeUnit.SECONDS)
            listener2.mLatch.await(3, TimeUnit.SECONDS)

            assertEquals(0, listener1.mLatch.count)
            assertEquals(0, listener2.mLatch.count)
            assertTrue(listener1.mCallbackTime > 0)
            assertTrue(listener2.mCallbackTime > 0)
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testExtractSyncFenceFd() {
        val fileDescriptor = 7
        val syncFence = SyncFenceCompat(7)
        assertEquals(fileDescriptor, JniBindings.nExtractFenceFd(syncFence))
    }

    @Test
    fun testTransactionSetVisibility_show() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setBuffer(scCompat, buffer)
                            .setVisibility(
                                scCompat,
                                true
                            ).commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assertTrue(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetVisibility_hide() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setBuffer(scCompat, buffer)
                            .setVisibility(
                                scCompat,
                                false
                            ).commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assertTrue(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.BLACK, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetLayer_zero() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat1 = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setLayer(scCompat1, 1)
                            .setBuffer(
                                scCompat1,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setLayer(scCompat2, 0)
                            .setBuffer(
                                scCompat2,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            )
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetLayer_positive() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat1 = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setLayer(scCompat1, 1)
                            .setBuffer(
                                scCompat1,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            )
                            .setLayer(scCompat2, 24)
                            .setBuffer(
                                scCompat2,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetLayer_negative() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat1 = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setLayer(scCompat1, 1)
                            .setBuffer(
                                scCompat1,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setLayer(scCompat2, -7)
                            .setBuffer(
                                scCompat2,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            )
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetDamageRegion_all() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setDamageRegion(
                                scCompat,
                                Region(0, 0, it.DEFAULT_WIDTH, it.DEFAULT_HEIGHT)
                            )
                            .setBuffer(
                                scCompat,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetDamageRegion_null() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setDamageRegion(
                                scCompat,
                                null
                            )
                            .setBuffer(
                                scCompat,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    @Test
    fun testTransactionSetDesiredPresentTime_now() {
        val listener = TransactionOnCompleteListener()
        val scenario = ActivityScenario.launch(SurfaceControlCompatTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder(it.getSurfaceView().holder.surface)
                            .setDebugName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .addTransactionCompletedListener(executor!!, listener)
                            .setBuffer(
                                scCompat,
                                getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setDesiredPresentTime(0)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
            val coord = intArrayOf(0, 0)
            it.mSurfaceView.getLocationOnScreen(coord)
            assertEquals(Color.RED, bitmap.getPixel(coord[0], coord[1]))
        }
    }

    private fun getSolidBuffer(width: Int, height: Int, color: Int): HardwareBuffer {
        return nGetSolidBuffer(width, height, color)
    }

    private fun getScreenshot(instrumentation: Instrumentation): Bitmap {
        val uiAutomation = instrumentation.uiAutomation
        val screenshot = uiAutomation.takeScreenshot()
        return screenshot
    }

    companion object {
        private external fun nGetSolidBuffer(width: Int, height: Int, color: Int): HardwareBuffer

        init {
            System.loadLibrary("sc-compat-test")
        }
    }
}