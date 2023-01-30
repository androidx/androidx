/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials

import android.os.Build
import android.os.Bundle

/** True if the two Bundles contain the same elements, and false otherwise. */
@Suppress("DEPRECATION")
fun equals(a: Bundle, b: Bundle): Boolean {
    if (a.keySet().size != b.keySet().size) {
        return false
    }
    for (key in a.keySet()) {
        if (!b.keySet().contains(key)) {
            return false
        }

        val valA = a.get(key)
        val valB = b.get(key)
        if (valA is Bundle && valB is Bundle && !equals(valA, valB)) {
            return false
        } else {
            val isEqual = (valA?.equals(valB) ?: (valB == null))
            if (!isEqual) {
                return false
            }
        }
    }
    return true
}

/** Used to maintain compatibility across API levels. */
const val MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL = Build.VERSION_CODES.TIRAMISU

/** True if the device running the test is post framework api level,
 * false if pre framework api level. */
fun isPostFrameworkApiLevel(): Boolean {
    return !((Build.VERSION.SDK_INT <= MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL) &&
        !(Build.VERSION.SDK_INT == MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL &&
            Build.VERSION.PREVIEW_SDK_INT > 0))
}