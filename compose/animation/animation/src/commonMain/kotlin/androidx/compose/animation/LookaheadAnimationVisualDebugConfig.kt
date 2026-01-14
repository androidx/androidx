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

package androidx.compose.animation

import androidx.compose.ui.graphics.Color

/**
 * Animation debugging configuration that will apply to the entire animation visual debug scope.
 *
 * @param isEnabled Boolean specifying whether to enable animation debugging.
 * @param overlayColor The color of the translucent film covering everything underneath the lifted
 *   layer (where the shared elements and other elements rendered in overlay are rendered).
 * @param multipleMatchesColor The color to indicate a shared element key with multiple matches.
 * @param unmatchedElementColor The color to indicate a shared element key with no matches.
 * @param isShowKeyLabelEnabled Boolean specifying whether to print animated element keys.
 */
@ExperimentalLookaheadAnimationVisualDebugApi
internal class LookaheadAnimationVisualDebugConfig(
    val isEnabled: Boolean = true,
    val overlayColor: Color = Color(0x8034A853),
    val multipleMatchesColor: Color = Color(0xFFEA4335),
    val unmatchedElementColor: Color = Color(0xFF9AA0A6),
    val isShowKeyLabelEnabled: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LookaheadAnimationVisualDebugConfig) return false

        if (isEnabled != other.isEnabled) return false
        if (overlayColor != other.overlayColor) return false
        if (multipleMatchesColor != other.multipleMatchesColor) return false
        if (unmatchedElementColor != other.unmatchedElementColor) return false
        if (isShowKeyLabelEnabled != other.isShowKeyLabelEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isEnabled.hashCode()
        result = 31 * result + overlayColor.hashCode()
        result = 31 * result + multipleMatchesColor.hashCode()
        result = 31 * result + unmatchedElementColor.hashCode()
        result = 31 * result + isShowKeyLabelEnabled.hashCode()
        return result
    }

    override fun toString(): String {
        return "LookaheadAnimationVisualDebugConfig(isEnabled=$isEnabled, " +
            "overlayColor=$overlayColor, " +
            "multipleMatchesColor=$multipleMatchesColor, " +
            "unmatchedElementColor=$unmatchedElementColor, " +
            "isShowKeyLabelEnabled=$isShowKeyLabelEnabled)"
    }
}
