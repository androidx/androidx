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

@file:JvmName("ManifestPermissionGroup")

package androidx.xr.runtime.manifest

/**
 * Used for permissions that are associated with accessing XR tracked information about the person
 * using the device and the environment around them.
 *
 * Constant Value: "android.permission-group.XR_TRACKING"
 */
@JvmField public val XR_TRACKING: String = "android.permission-group.XR_TRACKING"

/**
 * Used for permissions that are associated with accessing particularly sensitive XR tracking data.
 *
 * Constant Value: "android.permission-group.XR_TRACKING_SENSITIVE"
 */
@JvmField
public val XR_TRACKING_SENSITIVE: String = "android.permission-group.XR_TRACKING_SENSITIVE"
