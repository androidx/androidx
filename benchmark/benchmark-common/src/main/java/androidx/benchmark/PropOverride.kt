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

package androidx.benchmark

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * PropOverride provides temporary overriding of a platform setprop value, with resetting to
 * avoid polluting device state.
 *
 * It's recommended to use a try/finally to ensure that resetIfOverridden isn't missed.
 *
 * Barring that, using a single static instance enables reset to happen later if one reset is
 * missed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public class PropOverride(
    private val propName: String,
    private val overrideValue: String
) {
    private var resetValue: String? = null

    fun forceValue() {
        if (resetValue != null) {
            // Value was left overridden, skip
            return
        }

        val currentPropVal = Shell.executeCommand("getprop $propName").trim()
        if (currentPropVal != overrideValue) {
            resetValue = currentPropVal
            Log.d(BenchmarkState.TAG, "setting $propName to $overrideValue (was $currentPropVal)")
            Shell.executeCommand("setprop $propName $overrideValue")
        }
    }

    fun resetIfOverridden() {
        if (resetValue != null) {
            Log.d(BenchmarkState.TAG, "resetting $propName to $resetValue")
        }
    }
}