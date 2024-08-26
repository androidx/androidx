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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.SystemClock
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
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
class SurfaceControlWrapperTest {
    private var executor: Executor? = null

    @Before
    fun setup() {
        executor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
    }

    private abstract class SurfaceHolderCallback : SurfaceHolder.Callback {
        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

        override fun surfaceDestroyed(p0: SurfaceHolder) {}
    }

    @Test
    fun testCreateFromWindow() {
        val surfaceControl =
            SurfaceControl.Builder().setName("SurfaceControlCompact_createFromWindow").build()
        var scWrapper: SurfaceControlWrapper? = null
        try {
            scWrapper =
                SurfaceControlWrapper.Builder()
                    .setParent(Surface(surfaceControl))
                    .setDebugName("SurfaceControlWrapperTest")
                    .build()
        } catch (e: IllegalArgumentException) {
            fail()
        } finally {
            scWrapper?.release()
            surfaceControl.release()
        }
    }

    @Test
    fun testSurfaceControlWrapperBuilder_surfaceParent() {
        val surfaceControl =
            SurfaceControl.Builder().setName("SurfaceControlCompact_createFromWindow").build()
        var scWrapper: SurfaceControlWrapper? = null
        try {
            scWrapper =
                SurfaceControlWrapper.Builder()
                    .setParent(Surface(surfaceControl))
                    .setDebugName("SurfaceControlWrapperTest")
                    .build()
        } catch (e: IllegalArgumentException) {
            fail()
        } finally {
            scWrapper?.release()
            surfaceControl.release()
        }
    }

    @Test
    fun testSurfaceTransactionCreate() {
        var scWrapperTransaction: SurfaceControlWrapper.Transaction? = null
        try {
            scWrapperTransaction = SurfaceControlWrapper.Transaction()
        } catch (e: java.lang.IllegalArgumentException) {
            fail()
        } finally {
            scWrapperTransaction?.close()
        }
    }

    class TransactionOnCompleteListener : SurfaceControlCompat.TransactionCompletedListener {
        var mCallbackTime = -1L
        var mLatch = CountDownLatch(1)

        override fun onTransactionCompleted(transactionStats: Long) {
            mCallbackTime = SystemClock.elapsedRealtime()
            mLatch.countDown()
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

    @Test
    fun testSurfaceTransactionOnCompleteCallback() {
        val listener = TransactionOnCompleteListener()

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        val destroyLatch = CountDownLatch(1)
        try {
            scenario.onActivity {
                it.setDestroyCallback { destroyLatch.countDown() }
                SurfaceControlWrapper.Transaction()
                    .addTransactionCompletedListener(listener)
                    .commit()
            }

            listener.mLatch.await(3, TimeUnit.SECONDS)
            assertEquals(0, listener.mLatch.count)
            assertTrue(listener.mCallbackTime > 0)
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testSurfaceTransactionOnCommitCallback() {
        val listener = TransactionOnCommitListener()

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        val destroyLatch = CountDownLatch(1)
        try {
            scenario.onActivity {
                it.setDestroyCallback { destroyLatch.countDown() }
                SurfaceControlWrapper.Transaction()
                    .addTransactionCommittedListener(executor!!, listener)
                    .commit()
            }

            listener.mLatch.await(3, TimeUnit.SECONDS)
            assertEquals(0, listener.mLatch.count)
            assertTrue(listener.mCallbackTime > 0)
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testSurfaceTransactionOnCommitCallback_multiple() {
        val listener = TransactionOnCommitListener()
        val listener2 = TransactionOnCommitListener()

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        val destroyLatch = CountDownLatch(1)
        try {
            scenario.onActivity {
                it.setDestroyCallback { destroyLatch.countDown() }
                SurfaceControlWrapper.Transaction()
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
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testSurfaceTransactionOnCommitCallbackAndOnCompleteCallback() {
        val listener1 = TransactionOnCommitListener()
        val listener2 = TransactionOnCompleteListener()

        val scenario = ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java)

        val destroyLatch = CountDownLatch(1)
        try {
            scenario.onActivity {
                it.setDestroyCallback { destroyLatch.countDown() }
                SurfaceControlWrapper.Transaction()
                    .addTransactionCommittedListener(executor!!, listener1)
                    .addTransactionCompletedListener(listener2)
                    .commit()
            }

            listener1.mLatch.await(3, TimeUnit.SECONDS)
            listener2.mLatch.await(3, TimeUnit.SECONDS)

            assertEquals(0, listener1.mLatch.count)
            assertEquals(0, listener2.mLatch.count)
            assertTrue(listener1.mCallbackTime > 0)
            assertTrue(listener2.mCallbackTime > 0)
        } finally {
            // ensure activity is destroyed after any failures
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testSurfaceControlIsValid_valid() {
        val surfaceControl =
            SurfaceControl.Builder().setName("SurfaceControlCompact_createFromWindow").build()
        var scCompat: SurfaceControlWrapper? = null
        try {
            scCompat =
                SurfaceControlWrapper.Builder()
                    .setParent(Surface(surfaceControl))
                    .setDebugName("SurfaceControlWrapperTest")
                    .build()

            assertTrue(scCompat.isValid())
        } catch (e: IllegalArgumentException) {
            fail()
        } finally {
            scCompat?.release()
            surfaceControl.release()
        }
    }

    @Test
    fun testSurfaceControlIsValid_validNotValid() {
        val surfaceControl =
            SurfaceControl.Builder().setName("SurfaceControlCompact_createFromWindow").build()
        var scCompat: SurfaceControlWrapper? = null

        try {
            scCompat =
                SurfaceControlWrapper.Builder()
                    .setParent(Surface(surfaceControl))
                    .setDebugName("SurfaceControlWrapperTest")
                    .build()
            assertTrue(scCompat.isValid())

            scCompat.release()
            assertFalse(scCompat.isValid())
        } catch (e: IllegalArgumentException) {
            fail()
        } finally {
            if (scCompat != null && scCompat.isValid()) {
                scCompat.release()
            }
            surfaceControl.release()
        }
    }

    @Test
    fun testSurfaceControlIsValid_multipleRelease() {
        val surfaceControl =
            SurfaceControl.Builder().setName("SurfaceControlCompact_createFromWindow").build()
        var scCompat: SurfaceControlWrapper? = null

        try {
            scCompat =
                SurfaceControlWrapper.Builder()
                    .setParent(Surface(surfaceControl))
                    .setDebugName("SurfaceControlWrapperTest")
                    .build()
            assertTrue(scCompat.isValid())
            scCompat.release()
            scCompat.release()
            assertFalse(scCompat.isValid())
        } catch (e: IllegalArgumentException) {
            fail()
        } finally {
            if (scCompat != null && scCompat.isValid()) {
                scCompat.release()
            }
            surfaceControl.release()
        }
    }

    @Test
    fun testTransactionReparent_null() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                assertNotNull(buffer)

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .reparent(scCompat, null)
            },
            { bitmap, rect -> Color.BLACK == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionReparent_childOfSibling() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()
                val scCompat2 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapper")
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

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setBuffer(scCompat2, buffer2)
                    .reparent(scCompat, scCompat2)
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetVisibility_show() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                assertNotNull(buffer)

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetVisibility_hide() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                assertNotNull(buffer)

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, false)
            },
            { bitmap, rect -> Color.BLACK == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetLayer_zero() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat1 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()
                val scCompat2 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                SurfaceControlWrapper.Transaction()
                    .setLayer(scCompat1, 1)
                    .setBuffer(
                        scCompat1,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                    )
                    .setLayer(scCompat2, 0)
                    .setBuffer(
                        scCompat2,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.GREEN
                        )
                    )
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetLayer_positive() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat1 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()
                val scCompat2 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                SurfaceControlWrapper.Transaction()
                    .setLayer(scCompat1, 1)
                    .setBuffer(
                        scCompat1,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.GREEN
                        )
                    )
                    .setLayer(scCompat2, 24)
                    .setBuffer(
                        scCompat2,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                    )
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetLayer_negative() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat1 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()
                val scCompat2 =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                SurfaceControlWrapper.Transaction()
                    .setLayer(scCompat1, 1)
                    .setBuffer(
                        scCompat1,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                    )
                    .setLayer(scCompat2, -7)
                    .setBuffer(
                        scCompat2,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.GREEN
                        )
                    )
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetDamageRegion_all() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                SurfaceControlWrapper.Transaction()
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
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetDamageRegion_null() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                SurfaceControlWrapper.Transaction()
                    .setDamageRegion(scCompat, null)
                    .setBuffer(
                        scCompat,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                    )
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetDesiredPresentTime_now() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                SurfaceControlWrapper.Transaction()
                    .setBuffer(
                        scCompat,
                        SurfaceControlUtils.getSolidBuffer(
                            SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                            Color.BLUE
                        )
                    )
                    .setDesiredPresentTime(0)
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetBufferTransparency_opaque() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setOpaque(scCompat, true)
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetAlpha_0_0() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setOpaque(scCompat, false)
                    .setAlpha(scCompat, 0.0f)
            },
            { bitmap, rect -> Color.BLACK == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    fun testTransactionSetAlpha_0_5() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setAlpha(scCompat, 0.5f)
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                val fConnector: ColorSpace.Connector =
                    ColorSpace.connect(ColorSpace.get(ColorSpace.Named.SRGB), bitmap.colorSpace!!)

                val red = fConnector.transform(1.0f, 0.0f, 0.0f)
                val black = fConnector.transform(0.0f, 0.0f, 0.0f)
                val expectedResult =
                    Color.valueOf(red[0], red[1], red[2], 0.5f)
                        .compositeOver(Color.valueOf(black[0], black[1], black[2], 1.0f))

                (Math.abs(expectedResult.red() - bitmap.getColor(coord[0], coord[1]).red()) <
                    2.5e-3f) &&
                    (Math.abs(
                        expectedResult.green() - bitmap.getColor(coord[0], coord[1]).green()
                    ) < 2.5e-3f) &&
                    (Math.abs(expectedResult.blue() - bitmap.getColor(coord[0], coord[1]).blue()) <
                        2.5e-3f)
            }
        )
    }

    @Test
    fun testTransactionSetAlpha_1_0() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )
                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setAlpha(scCompat, 1.0f)
            },
            { bitmap, rect -> Color.RED == bitmap.getPixel(rect.left, rect.top) }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetCrop_null() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlWrapperTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setCrop(scCompat, null)
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                SurfaceControlUtils.checkNullCrop(bitmap, coord)
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetCrop_standardCrop() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setCrop(scCompat, Rect(20, 30, 90, 60))
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                SurfaceControlUtils.checkStandardCrop(bitmap, coord)
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetCrop_standardThenNullCrop() {
        var scCompat: SurfaceControlWrapper? = null
        val listener = TransactionOnCompleteListener()
        val scenario =
            ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java).onActivity {
                val callback =
                    object : SurfaceHolderCallback() {
                        override fun surfaceCreated(sh: SurfaceHolder) {
                            scCompat =
                                SurfaceControlWrapper.Builder()
                                    .setParent(it.getSurfaceView().holder.surface)
                                    .setDebugName("SurfaceControlCompatTest")
                                    .build()

                            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                            val buffer =
                                SurfaceControlUtils.getSolidBuffer(
                                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                                    Color.BLUE
                                )

                            SurfaceControlWrapper.Transaction()
                                .addTransactionCompletedListener(listener)
                                .setBuffer(scCompat!!, buffer)
                                .setVisibility(scCompat!!, true)
                                .setCrop(scCompat!!, Rect(20, 30, 90, 60))
                                .commit()
                        }
                    }

                it.addSurface(it.mSurfaceView, callback)
            }

        scenario.onActivity {
            assert(listener.mLatch.await(3000, TimeUnit.MILLISECONDS))
            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                SurfaceControlUtils.checkStandardCrop(bitmap, coord)
            }

            // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
            val buffer =
                SurfaceControlUtils.getSolidBuffer(
                    SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                    SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                    Color.BLUE
                )

            SurfaceControlWrapper.Transaction()
                .setBuffer(scCompat!!, buffer)
                .setVisibility(scCompat!!, true)
                .setCrop(scCompat!!, Rect(0, 0, 0, 0))
                .commit()

            SurfaceControlUtils.validateOutput { bitmap ->
                val coord = intArrayOf(0, 0)
                it.mSurfaceView.getLocationOnScreen(coord)
                SurfaceControlUtils.checkNullCrop(bitmap, coord)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetPosition() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setPosition(scCompat, 30f, 30f)
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                Color.BLACK ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        coord[1] + 29
                    ) &&
                    Color.BLACK ==
                        bitmap.getPixel(
                            coord[0] + 29,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                        ) &&
                    Color.RED == bitmap.getPixel(coord[0] + 30, coord[1] + 30)
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetScale() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
                        .build()

                // Buffer colorspace is RGBA, so Color.BLUE will be visually Red
                val buffer =
                    SurfaceControlUtils.getSolidBuffer(
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        Color.BLUE
                    )

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setScale(scCompat, 0.5f, 0.5f)
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                // Check outer bounds of square to ensure its scaled correctly
                Color.RED == bitmap.getPixel(coord[0], coord[1]) &&
                    Color.RED ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                        ) &&
                    // Scale reduced by 50%, so should be black here
                    Color.BLACK ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                        )
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetBufferTransform_identity() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
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

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setBufferTransform(scCompat, SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)

                // Check outer bounds of square to ensure its scaled correctly
                Color.RED == bitmap.getPixel(coord[0], coord[1]) &&
                    Color.RED ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                        ) &&
                    Color.BLACK ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                        )
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testTransactionSetGeometry_identity() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
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

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setGeometry(
                        scCompat,
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
                    )
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)

                // Check outer bounds of square to ensure its scaled correctly
                Color.RED == bitmap.getPixel(coord[0], coord[1]) &&
                    Color.RED ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                        ) &&
                    Color.BLACK ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                        )
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTransactionSetBufferTransform_singleTransform() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
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

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setBufferTransform(
                        scCompat,
                        SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_HORIZONTAL
                    )
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                // Ensure it actually rotated by checking its outer bounds are black
                Color.BLACK ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 4 - 1
                    ) &&
                    Color.BLACK ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH * 3 / 4,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                        ) &&
                    Color.RED ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 + 1,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                        )
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testTransactionSetGeometry_singleTransform() {
        verifySurfaceControlWrapperTest(
            { surfaceView ->
                val scCompat =
                    SurfaceControlWrapper.Builder()
                        .setParent(surfaceView.holder.surface)
                        .setDebugName("SurfaceControlCompatTest")
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

                SurfaceControlWrapper.Transaction()
                    .setBuffer(scCompat, buffer)
                    .setVisibility(scCompat, true)
                    .setGeometry(
                        scCompat,
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                        SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT,
                        SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_HORIZONTAL
                    )
            },
            { bitmap, rect ->
                val coord = intArrayOf(rect.left, rect.top)
                // Ensure it actually rotated by checking its outer bounds are black
                Color.BLACK ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 - 1,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 4 - 1
                    ) &&
                    Color.BLACK ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH * 3 / 4,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                        ) &&
                    Color.RED ==
                        bitmap.getPixel(
                            coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2 + 1,
                            coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2 - 1
                        )
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun verifySurfaceControlWrapperTest(
        createTransaction: (SurfaceView) -> SurfaceControlWrapper.Transaction,
        verifyOutput: (Bitmap, Rect) -> Boolean
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
}
