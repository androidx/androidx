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

@file:JvmName("ManifestFeature")

package androidx.xr.runtime.manifest

/**
 * Feature for [android.content.pm.PackageManager.getSystemAvailableFeatures] and
 * [android.content.pm.PackageManager.hasSystemFeature]: This device supports XR input from XR
 * controllers.
 *
 * Constant Value: "android.hardware.xr.input.controller"
 */
@JvmField public val FEATURE_XR_INPUT_CONTROLLER: String = "android.hardware.xr.input.controller"

/**
 * Feature for [android.content.pm.PackageManager.getSystemAvailableFeatures] and
 * [android.content.pm.PackageManager.hasSystemFeature]: This device supports XR input from the
 * user's hands.
 *
 * Constant Value: "android.hardware.xr.input.hand_tracking"
 */
@JvmField
public val FEATURE_XR_INPUT_HAND_TRACKING: String = "android.hardware.xr.input.hand_tracking"

/**
 * Feature for [android.content.pm.PackageManager.getSystemAvailableFeatures] and
 * [android.content.pm.PackageManager.hasSystemFeature]: This device supports XR input from the
 * user's eye gaze.
 *
 * Constant Value: "android.hardware.xr.input.eye_tracking"
 */
@JvmField
public val FEATURE_XR_INPUT_EYE_TRACKING: String = "android.hardware.xr.input.eye_tracking"

/**
 * Feature for [android.content.pm.PackageManager.getSystemAvailableFeatures] and
 * [android.content.pm.PackageManager.hasSystemFeature]: This device supports <a
 * href="https://www.khronos.org/openxr/">OpenXR</a>. The feature version indicates the highest
 * version of OpenXR supported by the device using the following encoding:
 * - Major version in bits 31-16
 * - Minor version in bits 15-0
 *
 * This is the same encoding as the top 32 bits of an `XrVersion`.
 *
 * Example: OpenXR 1.1 support is encoded as 0x00010001.
 *
 * Constant Value: "android.software.xr.api.openxr"
 */
@JvmField public val FEATURE_XR_API_OPENXR: String = "android.software.xr.api.openxr"

/**
 * Feature for [android.content.pm.PackageManager.getSystemAvailableFeatures] and
 * [android.content.pm.PackageManager.hasSystemFeature]: This device supports the Android XR Spatial
 * APIs. The feature version indicates the highest version of the Android XR Spatial APIs supported
 * by the device.
 *
 * Also see <a href="https://developer.android.com/develop/xr">Develop with the Android XR SDK</a>.
 *
 * Constant Value: "android.software.xr.api.spatial"
 */
@JvmField public val FEATURE_XR_API_SPATIAL: String = "android.software.xr.api.spatial"
