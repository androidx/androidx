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

package androidx.compose.ui.hapticfeedback

import androidx.compose.ui.implementedInJetBrainsFork

internal actual object PlatformHapticFeedbackType {
    actual val Confirm: HapticFeedbackType = implementedInJetBrainsFork()
    actual val ContextClick: HapticFeedbackType = implementedInJetBrainsFork()
    actual val GestureEnd: HapticFeedbackType = implementedInJetBrainsFork()
    actual val GestureThresholdActivate: HapticFeedbackType = implementedInJetBrainsFork()
    actual val LongPress: HapticFeedbackType = implementedInJetBrainsFork()
    actual val Reject: HapticFeedbackType = implementedInJetBrainsFork()
    actual val SegmentFrequentTick: HapticFeedbackType = implementedInJetBrainsFork()
    actual val SegmentTick: HapticFeedbackType = implementedInJetBrainsFork()
    actual val TextHandleMove: HapticFeedbackType = implementedInJetBrainsFork()
    actual val ToggleOn: HapticFeedbackType = implementedInJetBrainsFork()
    actual val ToggleOff: HapticFeedbackType = implementedInJetBrainsFork()
    actual val VirtualKey: HapticFeedbackType = implementedInJetBrainsFork()
}
