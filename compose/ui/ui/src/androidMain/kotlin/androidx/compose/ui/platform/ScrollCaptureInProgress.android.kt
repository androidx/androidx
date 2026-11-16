/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.view.View
import androidx.annotation.UiThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.R

/**
 * Whether a scroll capture session (long screenshot) that includes this [View]'s Compose content is
 * in progress.
 *
 * A host that runs scroll capture itself (for example a custom [ComposeUiHost]) sets this on its
 * [View] while a session is active. Compose views nested anywhere below that [View] report `true`
 * from [LocalScrollCaptureInProgress] while it is set.
 *
 * Reads are snapshot-observable, so compositions that read [LocalScrollCaptureInProgress] are
 * invalidated when this value changes. Defaults to `false`.
 */
@ExperimentalComposeUiHostApi
public var View.isComposeScrollCaptureInProgress: Boolean
    @UiThread
    get() {
        val state = scrollCaptureInProgressState
        if (state == null) {
            // Observe creation of the first state on any view so that readers are invalidated
            // when this view (or one of its ancestors) starts reporting a value.
            scrollCaptureInProgressStateCreations.intValue
            return false
        }
        return state.value
    }
    @UiThread
    set(value) {
        val state = scrollCaptureInProgressState
        if (state != null) {
            state.value = value
        } else if (value) {
            setTag(R.id.compose_scroll_capture_in_progress, mutableStateOf(true))
            scrollCaptureInProgressStateCreations.intValue++
        }
    }

/**
 * `true` if any [View] has ever had [isComposeScrollCaptureInProgress] set to `true`. Reading this
 * is snapshot-observable, so readers are invalidated the first time a [View] gets a state, and
 * every time another [View] gets its first state.
 *
 * Lets callers that walk many ancestors skip the per-[View] tag lookups when no host uses
 * [isComposeScrollCaptureInProgress].
 */
internal val isAnyComposeScrollCaptureInProgressStateCreated: Boolean
    get() = scrollCaptureInProgressStateCreations.intValue != 0

/**
 * The value of [isComposeScrollCaptureInProgress] for this [View], without observing creation of
 * states on other views. Callers must observe [isAnyComposeScrollCaptureInProgressStateCreated]
 * instead.
 */
internal val View.isComposeScrollCaptureInProgressSet: Boolean
    get() = scrollCaptureInProgressState?.value == true

private val View.scrollCaptureInProgressState: MutableState<Boolean>?
    @Suppress("UNCHECKED_CAST")
    get() = getTag(R.id.compose_scroll_capture_in_progress) as? MutableState<Boolean>

/**
 * Incremented whenever a [View] gets its first scroll-capture-in-progress state, so readers of
 * views without a state are invalidated.
 */
private val scrollCaptureInProgressStateCreations = mutableIntStateOf(0)
