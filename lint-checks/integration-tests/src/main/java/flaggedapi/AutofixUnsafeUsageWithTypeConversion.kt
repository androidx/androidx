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

package flaggedapi

import android.flagging.FlaggedApiContainer
import androidx.core.flagging.Flags

object AutofixUnsafeUsageWithTypeConversion {
    fun usafeUsages() {
        if (Flags.Companion.getBooleanFlagValue("flaggedapi", "myFlag")) {
            val resultA = FlaggedApiContainer.apiWithTypeArgument(null)
            val resultB = FlaggedApiContainer.apiWithGenericType<Int?, Float>(null)
            val resultC = FlaggedApiContainer.apiWithTwoDimensionalArray(null)
        }
    }
}
