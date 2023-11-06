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

package androidx.camera.core.imagecapture

import androidx.annotation.RequiresApi
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

/**
 * Fake [ImageCaptureControl] that records method calls.
 */
@RequiresApi(21)
class FakeImageCaptureControl : ImageCaptureControl {

    companion object {
        // By default, this fake object returns an immediate successful result.
        private val IMMEDIATE_RESULT: ListenableFuture<Void> = Futures.immediateFuture(null)
    }

    val actions = arrayListOf<Action>()
    var latestCaptureConfigs: List<CaptureConfig?> = arrayListOf()

    // Flip this flag to return a custom result using pendingResultCompleter.
    var shouldUsePendingResult = false
    lateinit var pendingResultCompleter: CallbackToFutureAdapter.Completer<Void>
    var pendingResult = createPendingResult()

    private fun createPendingResult() = CallbackToFutureAdapter.getFuture { completer ->
        pendingResultCompleter = completer
        "FakeImageCaptureControl's pendingResult"
    }

    fun resetPendingResult() {
        pendingResult = createPendingResult()
    }

    override fun lockFlashMode() {
        actions.add(Action.LOCK_FLASH)
    }

    override fun unlockFlashMode() {
        actions.add(Action.UNLOCK_FLASH)
    }

    override fun submitStillCaptureRequests(
        captureConfigs: MutableList<CaptureConfig>
    ): ListenableFuture<Void> {
        actions.add(Action.SUBMIT_REQUESTS)
        latestCaptureConfigs = captureConfigs
        if (shouldUsePendingResult) {
            return pendingResult
        }
        return IMMEDIATE_RESULT
    }

    fun clear() {
        // Cancel pending futures.
        pendingResult.cancel(true)
    }

    enum class Action {
        LOCK_FLASH,
        UNLOCK_FLASH,
        SUBMIT_REQUESTS
    }
}
