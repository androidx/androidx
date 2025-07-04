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

package androidx.credentials.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
object FormFactorHelper {
    /** Determines whether the device is a TV. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmStatic
    fun isTV(ctx: Context): Boolean {
        val pm = ctx.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    /** Determines whether the device is a Wearable. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmStatic
    fun isWear(ctx: Context): Boolean {
        val pm = ctx.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }

    /** Determines whether the device is a Auto. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmStatic
    fun isAuto(ctx: Context): Boolean {
        val pm = ctx.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
}
