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

package androidx.xr.projected

/** Constants used for requesting permissions on Projected devices. */
internal object ProjectedPermissionsConstants {
    const val EXTRA_RESULT_RECEIVER: String =
        "androidx.xr.projected.permissions.extra.RESULT_RECEIVER"
    const val EXTRA_PERMISSIONS: String = "androidx.xr.projected.permissions.extra.PERMISSIONS"
    const val EXTRA_PENDING_INTENT: String =
        "androidx.xr.projected.permissions.extra.EXTRA_PENDING_INTENT"
    const val RESULT_DATA_PERMISSIONS: String =
        "androidx.xr.projected.permissions.RESULT_DATA_PERMISSIONS"
    const val RESULT_DATA_GRANT_RESULTS: String =
        "androidx.xr.projected.permissions.RESULT_DATA_GRANT_RESULTS"
}
