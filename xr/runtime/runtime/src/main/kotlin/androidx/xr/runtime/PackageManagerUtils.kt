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

package androidx.xr.runtime

import android.content.Context
import androidx.annotation.VisibleForTesting

/** Internal utils class for [android.content.pm.PackageManager]-related operations. */
internal object PackageManagerUtils {
    @VisibleForTesting
    internal const val XR_PROJECTED_SYSTEM_FEATURE = "com.google.android.feature.XR_PROJECTED"

    /** Returns true if the [XR_PROJECTED_SYSTEM_FEATURE] is present. False otherwise. */
    internal fun hasXrProjectedSystemFeature(context: Context): Boolean =
        context.packageManager.hasSystemFeature(XR_PROJECTED_SYSTEM_FEATURE)
}
