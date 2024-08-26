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

import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.SystemClock
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert

@SdkSuppress(minSdkVersion = 29)
internal class SurfaceControlUtils {
    companion object {

        @RequiresApi(Build.VERSION_CODES.Q)
        fun surfaceControlTestHelper(
            onSurfaceCreated: (SurfaceView, CountDownLatch) -> Unit,
            verifyOutput: (Bitmap, Rect) -> Boolean
        ) {
            val setupLatch = CountDownLatch(1)
            var surfaceView: SurfaceView? = null
            val destroyLatch = CountDownLatch(1)
            val scenario =
                ActivityScenario.launch(SurfaceControlWrapperTestActivity::class.java).onActivity {
                    it.setDestroyCallback { destroyLatch.countDown() }
                    val callback =
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(sh: SurfaceHolder) {
                                surfaceView = it.mSurfaceView
                                onSurfaceCreated(surfaceView!!, setupLatch)
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                // NO-OP
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                // NO-OP
                            }
                        }

                    it.addSurface(it.mSurfaceView, callback)
                    surfaceView = it.mSurfaceView
                }

            Assert.assertTrue(setupLatch.await(3000, TimeUnit.MILLISECONDS))
            val coords = intArrayOf(0, 0)
            surfaceView!!.getLocationOnScreen(coords)
            try {
                validateOutput { bitmap ->
                    verifyOutput(
                        bitmap,
                        Rect(
                            coords[0],
                            coords[1],
                            coords[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH,
                            coords[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT
                        )
                    )
                }
            } finally {
                scenario.moveToState(Lifecycle.State.DESTROYED)
                Assert.assertTrue(destroyLatch.await(3000, TimeUnit.MILLISECONDS))
            }
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun validateOutput(window: Window, block: (bitmap: Bitmap) -> Boolean) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val bitmap = uiAutomation.takeScreenshot(window)
            if (bitmap != null) {
                block(bitmap)
            } else {
                throw IllegalArgumentException("Unable to obtain bitmap from screenshot")
            }
        }

        private fun flushSurfaceFlinger() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var commitLatch: CountDownLatch? = null
                // Android S only requires 1 additional transaction
                val maxTransactions =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        1
                    } else {
                        3
                    }
                for (i in 0 until maxTransactions) {
                    val transaction = SurfaceControlCompat.Transaction()
                    // CommittedListener only added on Android S
                    if (
                        i == maxTransactions - 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        commitLatch = CountDownLatch(1)
                        val executor = Executors.newSingleThreadExecutor()
                        transaction.addTransactionCommittedListener(
                            executor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    executor.shutdownNow()
                                    commitLatch.countDown()
                                }
                            }
                        )
                    }
                    transaction.commit()
                }

                if (commitLatch != null) {
                    commitLatch.await(3000, TimeUnit.MILLISECONDS)
                } else {
                    // Wait for transactions to flush
                    SystemClock.sleep(maxTransactions * 16L)
                }
            }
        }

        fun validateOutput(block: (bitmap: Bitmap) -> Boolean) {
            flushSurfaceFlinger()
            var sleepDurationMillis = 1000L
            var success = false
            for (i in 0..3) {
                val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
                success = block(bitmap)
                if (!success) {
                    SystemClock.sleep(sleepDurationMillis)
                    sleepDurationMillis *= 2
                } else {
                    break
                }
            }
            Assert.assertTrue(success)
        }

        fun checkNullCrop(bitmap: Bitmap, coord: IntArray): Boolean {
            // check top left
            return Color.RED == bitmap.getPixel(coord[0], coord[1]) &&
                // check top right
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH - 1,
                        coord[1]
                    ) &&
                // check  bottom right
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH - 1,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT - 1
                    ) &&
                // check bottom left
                Color.RED ==
                    bitmap.getPixel(
                        coord[0],
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT - 1
                    ) &&
                // check center
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    )
        }

        fun checkStandardCrop(bitmap: Bitmap, coord: IntArray): Boolean {
            // check left crop
            return Color.BLACK ==
                bitmap.getPixel(
                    coord[0] + 19,
                    coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                ) &&
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + 20,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    ) &&
                // check top crop
                Color.BLACK ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        coord[1] + 29
                    ) &&
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        coord[1] + 30
                    ) &&
                // check right crop
                Color.BLACK ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH - 10,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    ) &&
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT - 11,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT / 2
                    ) &&
                // check bottom crop
                Color.BLACK ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT - 40
                    ) &&
                Color.RED ==
                    bitmap.getPixel(
                        coord[0] + SurfaceControlWrapperTestActivity.DEFAULT_WIDTH / 2,
                        coord[1] + SurfaceControlWrapperTestActivity.DEFAULT_HEIGHT - 41
                    )
        }

        fun getScreenshot(instrumentation: Instrumentation): Bitmap {
            val uiAutomation = instrumentation.uiAutomation
            val screenshot = uiAutomation.takeScreenshot()
            return screenshot
        }

        fun getSolidBuffer(width: Int, height: Int, color: Int): HardwareBuffer {
            return nGetSolidBuffer(width, height, color)
        }

        fun getQuadrantBuffer(
            width: Int,
            height: Int,
            colorTopLeft: Int,
            colorTopRight: Int,
            colorBottomRight: Int,
            colorBottomLeft: Int
        ): HardwareBuffer {
            return nGetQuadrantBuffer(
                width,
                height,
                colorTopLeft,
                colorTopRight,
                colorBottomRight,
                colorBottomLeft
            )
        }

        private external fun nGetSolidBuffer(width: Int, height: Int, color: Int): HardwareBuffer

        private external fun nGetQuadrantBuffer(
            width: Int,
            height: Int,
            colorTopLeft: Int,
            colorTopRight: Int,
            colorBottomRight: Int,
            colorBottomLeft: Int
        ): HardwareBuffer

        init {
            System.loadLibrary("graphics-core")
        }
    }
}
