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

@file:JvmName("ManifestPermission")

package androidx.xr.runtime.manifest

/**
 * Allows an application to get approximate eye gaze.
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.EYE_TRACKING_COARSE"
 */
@JvmField public val EYE_TRACKING_COARSE: String = "android.permission.EYE_TRACKING_COARSE"

/**
 * Allows an application to get face tracking data.
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.FACE_TRACKING"
 */
@JvmField public val FACE_TRACKING: String = "android.permission.FACE_TRACKING"

/**
 * Allows an application to get hand tracking data.
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.HAND_TRACKING"
 */
@JvmField public val HAND_TRACKING: String = "android.permission.HAND_TRACKING"

/**
 * Allows an application to get data derived by sensing the user's environment.
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.SCENE_UNDERSTANDING_COARSE"
 */
@JvmField
public val SCENE_UNDERSTANDING_COARSE: String = "android.permission.SCENE_UNDERSTANDING_COARSE"

/**
 * Allows an application to get precise eye gaze data.
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.EYE_TRACKING_FINE"
 */
@JvmField public val EYE_TRACKING_FINE: String = "android.permission.EYE_TRACKING_FINE"

/**
 * Allows an application to get head tracking data. Unmanaged activities (OpenXR activities with the
 * manifest property "android.window.PROPERTY_XR_ACTIVITY_START_MODE" set to
 * "XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED") do not require this permission to get head
 * tracking data.
 *
 * {@see
 * https://developer.android.com/develop/xr/get-started#property_activity_xr_start_mode_property}
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.HEAD_TRACKING"
 */
@JvmField public val HEAD_TRACKING: String = "android.permission.HEAD_TRACKING"

/**
 * Allows an application to get highly precise data derived by sensing the user's environment, such
 * as a depth map.
 *
 * Protection level: dangerous
 *
 * Constant Value: "android.permission.SCENE_UNDERSTANDING_FINE"
 */
@JvmField
public val SCENE_UNDERSTANDING_FINE: String = "android.permission.SCENE_UNDERSTANDING_FINE"
