/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Immutable

/**
 * This class collects window info that affects adaptation decisions. An adaptive layout is supposed
 * to use the info from this class to decide how the layout is supposed to be adapted.
 *
 * @constructor create an instance of [WindowAdaptiveInfo]
 * @param windowSizeClass [WindowSizeClass] of the current window.
 * @param posture [Posture] of the current window.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class WindowAdaptiveInfo(
    val windowSizeClass: WindowSizeClass,
    val posture: Posture
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WindowAdaptiveInfo) return false
        if (windowSizeClass != other.windowSizeClass) return false
        if (posture != other.posture) return false
        return true
    }

    override fun hashCode(): Int {
        var result = windowSizeClass.hashCode()
        result = 31 * result + posture.hashCode()
        return result
    }
}
