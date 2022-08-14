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

import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Region
import android.opengl.EGL14
import android.os.Build
import android.os.SystemClock
import android.view.SurfaceHolder
import androidx.graphics.lowlatency.SyncFenceCompat
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.EGLVersion
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.hardware.SyncFence
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun testSurfaceControlCompatBuilder_parent() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)

        try {
            scenario.onActivity {
                val callback = object : SurfaceHolderCallback() {
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
            scenario.moveToState(Lifecycle.State.RESUMED)
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

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
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

    @Test
    fun testSurfaceControlIsValid_valid() {
        val callbackLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
        try {
            scenario.onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat.Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        assertTrue(scCompat.isValid())
                        callbackLatch.countDown()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

            scenario.moveToState(Lifecycle.State.RESUMED)
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
            .moveToState(Lifecycle.State.CREATED)
        try {
            scenario.onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat.Builder()
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

            scenario.moveToState(Lifecycle.State.RESUMED)
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
            .moveToState(Lifecycle.State.CREATED)
        try {
            scenario.onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat.Builder()
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

            scenario.moveToState(Lifecycle.State.RESUMED)
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
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setVisibility(scCompat, true)
                            .reparent(scCompat, null)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.BLACK == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionReparent_childOfSibling() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        val buffer2 =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.GREEN
                            )
                        assertNotNull(buffer2)

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setBuffer(scCompat2, buffer2)
                            .setVisibility(scCompat, true)
                            .setVisibility(scCompat2, true)
                            .reparent(scCompat, scCompat2)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testExtractSyncFenceFd() {
        val fileDescriptor = 7
        val syncFence = SyncFence(7)
        assertEquals(fileDescriptor, JniBindings.nExtractFenceFd(syncFence))
    }

    @Test
    fun testTransactionSetBuffer_nullCallback() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        SurfaceControlCompat.Transaction()
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
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetBuffer_singleReleaseCallback() {
        val releaseLatch = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.GREEN
                            )
                        assertNotNull(buffer)

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer2 =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer2)

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer) {
                                releaseLatch.countDown()
                            }
                            .setVisibility(scCompat, true)
                            .commit()
                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer2)
                            .setVisibility(scCompat, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetBuffer_multipleReleaseCallbacksAndOverwrite() {
        val releaseLatch = CountDownLatch(1)
        val releaseLatch2 = CountDownLatch(1)
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.GREEN
                            )
                        assertNotNull(buffer)

                        val buffer2 =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.GREEN
                            )
                        assertNotNull(buffer2)

                        val buffer3 =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer3)

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer) {
                                releaseLatch.countDown()
                            }
                            .setBuffer(scCompat, buffer2) {
                                releaseLatch2.countDown()
                            }
                            .setVisibility(
                                scCompat,
                                true
                            ).commit()

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer3)
                            .setVisibility(scCompat, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
            assertTrue(releaseLatch2.await(3000, TimeUnit.MILLISECONDS))
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetBuffer_withSyncFence() {
        val releaseLatch = CountDownLatch(1)
        val egl = createAndSetupEGLManager(EGLSpec.V14)
        if (egl.supportsNativeAndroidFence()) {
            val syncFenceCompat = SyncFenceCompat.createNativeSyncFence(egl.eglSpec)
            val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
                .moveToState(
                    Lifecycle.State.CREATED
                ).onActivity {
                    val callback = object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            val scCompat = SurfaceControlCompat
                                .Builder()
                                .setParent(it.getSurfaceView())
                                .setName("SurfaceControlCompatTest")
                                .build()

                            val buffer =
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            assertNotNull(buffer)

                            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                            val buffer2 =
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            assertNotNull(buffer2)

                            SurfaceControlCompat.Transaction()
                                .setBuffer(
                                    scCompat,
                                    buffer,
                                    syncFenceCompat,
                                ) {
                                    releaseLatch.countDown()
                                }
                                .setVisibility(scCompat, true)
                                .commit()
                            SurfaceControlCompat.Transaction()
                                .setBuffer(scCompat, buffer2)
                                .setVisibility(scCompat, true)
                                .commit()
                        }
                    }

                    it.addSurface(it.mSurfaceView, callback)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                assertTrue(releaseLatch.await(3000, TimeUnit.MILLISECONDS))
                assertTrue(syncFenceCompat.await(3000))
                SurfaceControlUtils.validateOutput { bitmap ->
                    val coord = intArrayOf(0, 0)
                    it.mSurfaceView.getLocationOnScreen(coord)
                    Color.RED == bitmap.getPixel(coord[0], coord[1])
                }

                releaseEGLManager(egl)
            }
        }
    }

    @Test
    fun testTransactionSetVisibility_show() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        SurfaceControlCompat.Transaction()
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
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetVisibility_hide() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer =
                            SurfaceControlUtils.getSolidBuffer(
                                it.DEFAULT_WIDTH,
                                it.DEFAULT_HEIGHT,
                                Color.BLUE
                            )
                        assertNotNull(buffer)

                        SurfaceControlCompat.Transaction()
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
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.BLACK == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetLayer_zero() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat1 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .setLayer(scCompat1, 1)
                            .setBuffer(
                                scCompat1,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setVisibility(scCompat1, true)
                            .setLayer(scCompat2, 0)
                            .setBuffer(
                                scCompat2,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            )
                            .setVisibility(scCompat2, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetLayer_positive() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat1 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .setLayer(scCompat1, 1)
                            .setBuffer(
                                scCompat1,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            )
                            .setVisibility(scCompat1, true)
                            .setLayer(scCompat2, 24)
                            .setBuffer(
                                scCompat2,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setVisibility(scCompat2, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetLayer_negative() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat1 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()
                        val scCompat2 = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .setLayer(scCompat1, 1)
                            .setBuffer(
                                scCompat1,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setVisibility(scCompat1, true)
                            .setLayer(scCompat2, -7)
                            .setBuffer(
                                scCompat2,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.GREEN
                                )
                            )
                            .setVisibility(scCompat2, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetDamageRegion_all() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .setDamageRegion(
                                scCompat,
                                Region(0, 0, it.DEFAULT_WIDTH, it.DEFAULT_HEIGHT)
                            )
                            .setBuffer(
                                scCompat,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setVisibility(scCompat, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetDamageRegion_null() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        SurfaceControlCompat.Transaction()
                            .setDamageRegion(
                                scCompat,
                                null
                            )
                            .setBuffer(
                                scCompat,
                                SurfaceControlUtils.getSolidBuffer(
                                    it.DEFAULT_WIDTH,
                                    it.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )
                            )
                            .setVisibility(scCompat, true)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetBufferTransparency_opaque() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer = SurfaceControlUtils.getSolidBuffer(
                            it.DEFAULT_WIDTH,
                            it.DEFAULT_HEIGHT,
                            Color.BLUE
                        )

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setVisibility(scCompat, true)
                            .setOpaque(
                                scCompat,
                                true
                            )
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetAlpha_0_0() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer = SurfaceControlUtils.getSolidBuffer(
                            it.DEFAULT_WIDTH,
                            it.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setOpaque(
                                scCompat,
                                false
                            )
                            .setAlpha(scCompat, 0.0f)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.BLACK == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @Test
    fun testTransactionSetAlpha_0_5() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer = SurfaceControlUtils.getSolidBuffer(
                            it.DEFAULT_WIDTH,
                            it.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setVisibility(scCompat, true)
                            .setAlpha(scCompat, 0.5f)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)

                val fConnector: ColorSpace.Connector = ColorSpace.connect(
                    ColorSpace.get(ColorSpace.Named.SRGB),
                    bitmap.colorSpace!!
                )

                val red = fConnector.transform(1.0f, 0.0f, 0.0f)
                val black = fConnector.transform(0.0f, 0.0f, 0.0f)
                val expectedResult = Color.valueOf(red[0], red[1], red[2], 0.5f)
                    .compositeOver(Color.valueOf(black[0], black[1], black[2], 1.0f))

                (Math.abs(
                    expectedResult.red() - bitmap.getColor(coord[0], coord[1]).red()
                ) < 2.5e-3f) &&
                    (Math.abs(
                        expectedResult.green() - bitmap.getColor(coord[0], coord[1]).green()
                    ) < 2.5e-3f) &&
                    (Math.abs(
                        expectedResult.blue() - bitmap.getColor(coord[0], coord[1]).blue()
                    ) < 2.5e-3f)
            }
        }
    }

    @Test
    fun testTransactionSetAlpha_1_0() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer = SurfaceControlUtils.getSolidBuffer(
                            it.DEFAULT_WIDTH,
                            it.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setVisibility(scCompat, true)
                            .setAlpha(scCompat, 1.0f)
                            .commit()
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testSurfaceTransactionCommitOnDraw() {
        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)
            .moveToState(
                Lifecycle.State.CREATED
            ).onActivity {
                val callback = object : SurfaceHolderCallback() {
                    override fun surfaceCreated(sh: SurfaceHolder) {
                        val scCompat = SurfaceControlCompat
                            .Builder()
                            .setParent(it.getSurfaceView())
                            .setName("SurfaceControlCompatTest")
                            .build()

                        // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                        val buffer = SurfaceControlUtils.getSolidBuffer(
                            it.DEFAULT_WIDTH,
                            it.DEFAULT_HEIGHT,
                            Color.BLUE
                        )

                        SurfaceControlCompat.Transaction()
                            .setBuffer(scCompat, buffer)
                            .setVisibility(scCompat, true)
                            .setAlpha(scCompat, 1.0f)
                            .commitTransactionOnDraw(it.mSurfaceView.rootSurfaceControl!!)
                    }
                }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                Color.RED == bitmap.getPixel(coord[0], coord[1])
            }
        }
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