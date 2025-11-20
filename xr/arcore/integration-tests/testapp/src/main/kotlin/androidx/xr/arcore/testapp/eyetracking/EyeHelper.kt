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

package androidx.xr.arcore.testapp.eyetracking

import android.graphics.Color
import androidx.xr.arcore.Eye
import androidx.xr.runtime.Config
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose

fun getEyeIsOpen(config: Config, eye: Eye?): Boolean? = getEyeIsOpen(config, eye?.state?.value)

fun getEyeIsOpen(config: Config, eye: Eye.State?): Boolean? = eye?.isOpen

fun getEyePose(config: Config, eye: Eye?): Pose? = getEyePose(config, eye?.state?.value)

fun getEyePose(config: Config, eye: Eye.State?): Pose? = eye?.pose

const val GAZE_LEFT = Color.GREEN
const val GAZE_RIGHT = Color.RED
const val SHUT_LEFT = Color.BLUE
const val SHUT_RIGHT = Color.YELLOW
const val INVALID = Color.WHITE

fun getEyeTrackingState(config: Config, eye: Eye?): TrackingState? =
    getEyeTrackingState(config, eye?.state?.value)

fun getEyeTrackingState(config: Config, eye: Eye.State?): TrackingState? = eye?.trackingState
