/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.internal

import android.os.Build
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TAG = "FakeFocusMeteringControl"

/** A fake implementations of {@link ScreenFlash} for testing purpose. */
internal class FakeFocusMeteringControl(
    fakeCamera2CameraControlImpl: ScreenFlashTaskTest.FakeCamera2CameraControlImpl,
    quirks: Quirks
) :
    FocusMeteringControl(
        fakeCamera2CameraControlImpl,
        CameraXExecutors.myLooperExecutor(),
        MoreExecutors.directExecutor(),
        quirks,
    ) {
    // This class needs to be in androidx.camera.camera2.internal path since FocusMeteringControl
    // constructor is package-private

    // TODO: Fake out all the methods so real implementation is not called in test

    private val lock = Any()

    @GuardedBy("mLock") var externalFlashAeModeEnabled = false
    private var enableExternalFlashAeModeLatch: CountDownLatch? = null

    @GuardedBy("mLock") var triggerAePrecaptureCount = 0
    @GuardedBy("mLock") private var triggerAePrecaptureLatch: CountDownLatch? = null

    @GuardedBy("mLock") var cancelAfAeTriggerCount = 0
    @GuardedBy("mLock") private var cancelAfAeTriggerLatch: CountDownLatch? = null

    override fun triggerAePrecapture(): ListenableFuture<Void> {
        synchronized(lock) {
            triggerAePrecaptureCount++
            triggerAePrecaptureLatch?.countDown()
        }

        Log.d(TAG, "triggerAePrecaptureCount = $triggerAePrecaptureCount")

        return Futures.immediateFuture(null)
    }

    /** Waits for a new [triggerAePrecapture] API invocation. */
    fun awaitTriggerAePrecapture(timeoutMillis: Long = 3000) {
        synchronized(lock) { triggerAePrecaptureLatch = CountDownLatch(1) }
        triggerAePrecaptureLatch?.await(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    override fun cancelAfAeTrigger(cancelAfTrigger: Boolean, cancelAePrecaptureTrigger: Boolean) {
        synchronized(lock) {
            cancelAfAeTriggerCount++
            cancelAfAeTriggerLatch?.countDown()
        }
    }

    /** Waits for a new [cancelAfAeTrigger] API invocation. */
    fun awaitCancelAfAeTrigger(timeoutMillis: Long = 3000) {
        synchronized(lock) { cancelAfAeTriggerLatch = CountDownLatch(1) }
        cancelAfAeTriggerLatch?.await(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    override fun enableExternalFlashAeMode(enable: Boolean): ListenableFuture<Void> {
        synchronized(lock) {
            if (Build.VERSION.SDK_INT >= 28) {
                externalFlashAeModeEnabled = enable
            }
            enableExternalFlashAeModeLatch?.countDown()
        }

        Log.d(TAG, "externalFlashAeModeEnabled = $externalFlashAeModeEnabled")

        return Futures.immediateFuture(null)
    }

    /** Waits for a new [enableExternalFlashAeMode] API invocation. */
    fun awaitEnableExternalFlashAeMode(timeoutMillis: Long = 3000) {
        synchronized(lock) { enableExternalFlashAeModeLatch = CountDownLatch(1) }
        enableExternalFlashAeModeLatch?.await(timeoutMillis, TimeUnit.MILLISECONDS)
    }
}
