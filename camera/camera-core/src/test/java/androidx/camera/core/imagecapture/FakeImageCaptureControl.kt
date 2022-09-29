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

import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Fake [ImageCaptureControl] that records method calls.
 */
class FakeImageCaptureControl : ImageCaptureControl {

    val actions = arrayListOf<Action>()
    var latestCaptureConfigs: List<CaptureConfig?> = arrayListOf()
    var response: ListenableFuture<Void> = Futures.immediateFuture(null)

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
        return response
    }

    enum class Action {
        LOCK_FLASH,
        UNLOCK_FLASH,
        SUBMIT_REQUESTS
    }
}