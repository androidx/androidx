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
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

// TODO: minor UX improvement, add `prepare()` calls when internal APIs are likely to use HapticFeedback
//  (e.g. pan started during the text selection) to reduce haptic feedback latency
//  see https://developer.apple.com/documentation/uikit/uifeedbackgenerator
internal class CupertinoHapticFeedback : HapticFeedback {

    private val mediumImpactGenerator = UIImpactFeedbackGenerator()
    private val lightImpactGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val selectionGenerator = UISelectionFeedbackGenerator()
    private val notificationGenerator = UINotificationFeedbackGenerator()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        when (hapticFeedbackType) {
            HapticFeedbackType.Confirm -> notificationGenerator
                .notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            HapticFeedbackType.Reject -> notificationGenerator
                .notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
            HapticFeedbackType.ContextClick,
            HapticFeedbackType.LongPress -> mediumImpactGenerator.impactOccurred()
            HapticFeedbackType.GestureEnd,
            HapticFeedbackType.GestureThresholdActivate,
            HapticFeedbackType.ToggleOff,
            HapticFeedbackType.ToggleOn,
            HapticFeedbackType.VirtualKey -> lightImpactGenerator.impactOccurred()
            HapticFeedbackType.SegmentFrequentTick,
            HapticFeedbackType.SegmentTick,
            HapticFeedbackType.TextHandleMove -> selectionGenerator.selectionChanged()
        }
    }
}