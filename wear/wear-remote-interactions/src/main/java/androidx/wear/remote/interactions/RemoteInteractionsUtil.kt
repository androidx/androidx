/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.remote.interactions

import android.content.Context
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal object RemoteInteractionsUtil {
    internal const val SYSTEM_FEATURE_WATCH: String = "android.hardware.type.watch"

    internal fun isCurrentDeviceAWatch(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Api24Impl.hasSystemFeature(context)

    @RequiresApi(Build.VERSION_CODES.N)
    private object Api24Impl {
        @JvmStatic
        @DoNotInline
        fun hasSystemFeature(context: Context) =
            context.packageManager.hasSystemFeature(SYSTEM_FEATURE_WATCH)
    }
}
