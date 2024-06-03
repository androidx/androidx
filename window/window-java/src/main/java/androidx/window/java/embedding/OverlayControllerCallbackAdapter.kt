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

package androidx.window.java.embedding

import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.OverlayController
import androidx.window.embedding.OverlayCreateParams
import androidx.window.embedding.OverlayInfo
import androidx.window.java.core.CallbackToFlowAdapter
import java.util.concurrent.Executor

/**
 * An adapted interface for [OverlayController] that provides callback shaped APIs to report the
 * latest [OverlayInfo].
 *
 * It should only be used if [OverlayController.overlayInfo] is not available. For example, an app
 * is written in Java and cannot use Flow APIs.
 *
 * @param controller an [OverlayController] that can be obtained by [OverlayController.getInstance].
 * @constructor creates a callback adapter of [OverlayController.overlayInfo] flow API.
 */
class OverlayControllerCallbackAdapter(private val controller: OverlayController) {

    private val callbackToFlowAdapter = CallbackToFlowAdapter()

    /**
     * Registers a listener for updates of [OverlayInfo] that [overlayTag] is associated with.
     *
     * If there is no active overlay [ActivityStack], the reported [OverlayInfo.activityStack] and
     * [OverlayInfo.currentOverlayAttributes] will be `null`.
     *
     * Note that launching an overlay [ActivityStack] only supports on the device with
     * [WindowSdkExtensions.extensionVersion] equal to or larger than 5. If
     * [WindowSdkExtensions.extensionVersion] is less than 5, this flow will always report
     * [OverlayInfo] without associated [OverlayInfo.activityStack].
     *
     * @param overlayTag the overlay [ActivityStack]'s tag which is set through
     *   [OverlayCreateParams]
     * @param executor the [Executor] to dispatch the [OverlayInfo] change
     * @param consumer the [Consumer] that will be invoked on the [executor] when there is an update
     *   to [OverlayInfo].
     */
    @RequiresWindowSdkExtension(5)
    fun addOverlayInfoListener(
        overlayTag: String,
        executor: Executor,
        consumer: Consumer<OverlayInfo>
    ) {
        callbackToFlowAdapter.connect(executor, consumer, controller.overlayInfo(overlayTag))
    }

    /**
     * Unregisters a listener that was previously registered via [addOverlayInfoListener].
     *
     * @param consumer the previously registered [Consumer] to unregister.
     */
    @RequiresWindowSdkExtension(5)
    fun removeOverlayInfoListener(consumer: Consumer<OverlayInfo>) {
        callbackToFlowAdapter.disconnect(consumer)
    }
}
