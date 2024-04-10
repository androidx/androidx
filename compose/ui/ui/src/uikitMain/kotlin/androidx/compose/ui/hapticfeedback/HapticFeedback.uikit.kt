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

package androidx.compose.ui.hapticfeedback

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UISelectionFeedbackGenerator

// TODO: minor UX improvement, add `prepare()` calls when internal APIs are likely to use HapticFeedback
//  (e.g. pan started during the text selection) to reduce haptic feedback latency
//  see https://developer.apple.com/documentation/uikit/uifeedbackgenerator
internal class CupertinoHapticFeedback : HapticFeedback {
    private val impactGenerator = UIImpactFeedbackGenerator()
    private val selectionGenerator = UISelectionFeedbackGenerator()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        when (hapticFeedbackType) {
            HapticFeedbackType.LongPress -> impactGenerator.impactOccurred()
            HapticFeedbackType.TextHandleMove -> selectionGenerator.selectionChanged()
        }
    }
}