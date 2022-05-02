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

import android.os.Build
import android.os.SystemClock
import android.view.Surface
import android.view.SurfaceControl
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(Build.VERSION_CODES.Q)
@SdkSuppress(minSdkVersion = 29)
class SurfaceControlCompatTest {

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

        override fun onComplete(latchTime: Long, presentTime: Long) {
            mCallbackTime = SystemClock.elapsedRealtime()
            mLatchTime = latchTime
            mPresentTime = presentTime
            mLatch.countDown()
        }
    }

    @Test
    fun testSurfaceTransactionOnCompleteCallback() {
        val transaction = SurfaceControlCompat.Transaction()
        val listener = TransactionOnCompleteListener()
        transaction.addTransactionCompletedListener(listener)
        transaction.commit()

        listener.mLatch.await(3, TimeUnit.SECONDS)

        assertEquals(0, listener.mLatch.count)
        assertTrue(listener.mCallbackTime > 0)
    }
}