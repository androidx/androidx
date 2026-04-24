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

package androidx.xr.arcore

import androidx.xr.arcore.runtime.TrackingState as RTTrackingState
import androidx.xr.runtime.TrackingState.Companion.PAUSED
import androidx.xr.runtime.TrackingState.Companion.STOPPED
import androidx.xr.runtime.TrackingState.Companion.TRACKING
import androidx.xr.runtime.TrackingState.Companion.TRACKING_DEGRADED

/** Describes the state of the tracking performed. */
@Suppress("TypealiasDefinition")
public typealias TrackingState = androidx.xr.runtime.TrackingState

internal fun TrackingState.toRuntimeTrackingState(): RTTrackingState =
    when (this) {
        TRACKING -> RTTrackingState.TRACKING
        PAUSED -> RTTrackingState.PAUSED
        STOPPED -> RTTrackingState.STOPPED
        TRACKING_DEGRADED -> RTTrackingState.TRACKING_DEGRADED
        else -> throw IllegalStateException()
    }

internal fun RTTrackingState.toTrackingState(): TrackingState =
    when (this) {
        RTTrackingState.TRACKING -> TrackingState.TRACKING
        RTTrackingState.PAUSED -> TrackingState.PAUSED
        RTTrackingState.STOPPED -> TrackingState.STOPPED
        RTTrackingState.TRACKING_DEGRADED -> TrackingState.TRACKING_DEGRADED
        else -> throw IllegalStateException()
    }
