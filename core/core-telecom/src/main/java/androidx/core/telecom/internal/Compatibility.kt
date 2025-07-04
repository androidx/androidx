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

package androidx.core.telecom.internal

import android.os.Build
import android.telecom.Call
import androidx.annotation.RequiresApi

internal object Compatibility {
    @RequiresApi(Build.VERSION_CODES.M)
    @Suppress("DEPRECATION")
    @JvmStatic
    fun getCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Impl.getCallState(call)
        } else {
            call.state
        }
    }
}

/** Ensure compatibility for [Call] APIs for API level 31+ */
@RequiresApi(Build.VERSION_CODES.S)
internal object Api31Impl {
    @JvmStatic
    fun getCallState(call: Call): Int {
        return call.details.state
    }
}
