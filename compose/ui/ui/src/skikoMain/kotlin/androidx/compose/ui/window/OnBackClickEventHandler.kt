/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo

/**
 * A specialized implementation of [NavigationEventHandler] that handles
 * only instant back navigation events, namely, clicks on the system back button.
 * If there were any progress events, they will be consumed,
 * but the [onBack] callback will not be invoked.
 */
internal class OnBackClickEventHandler(
    compositeKey: CompositeKeyHashCode,
    private val onBack: () -> Unit,
) : NavigationEventHandler<NavigationEventInfo>(
    initialInfo = BackClickHandlerInfo(compositeKey),
    isBackEnabled = true
) {
    private var isProgressEvent = false
    var backClickIsEnabled: Boolean = true

    override fun onBackProgressed(event: NavigationEvent) {
        isProgressEvent = true
    }

    override fun onBackCompleted() {
        if (!isProgressEvent && backClickIsEnabled) {
            onBack()
        }
        isProgressEvent = false
    }

    override fun onBackCancelled() {
        isProgressEvent = false
    }
}

private data class BackClickHandlerInfo(
    val compositeKey: CompositeKeyHashCode,
) : NavigationEventInfo()
