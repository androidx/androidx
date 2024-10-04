/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * Constants to be used to perform haptic feedback effects via
 * [HapticFeedback.performHapticFeedback].
 */
@kotlin.jvm.JvmInline
value class HapticFeedbackType(internal val value: Int) {

    override fun toString(): String {
        return when (this) {
            Confirm -> "Confirm"
            ContextClick -> "ContextClick"
            GestureEnd -> "GestureEnd"
            GestureThresholdActivate -> "GestureThresholdActivate"
            LongPress -> "LongPress"
            Reject -> "Reject"
            SegmentFrequentTick -> "SegmentFrequentTick"
            SegmentTick -> "SegmentTick"
            TextHandleMove -> "TextHandleMove"
            ToggleOff -> "ToggleOff"
            ToggleOn -> "ToggleOn"
            VirtualKey -> "VirtualKey"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * A haptic effect to signal the confirmation or successful completion of a user
         * interaction..
         */
        val Confirm
            get() = PlatformHapticFeedbackType.Confirm

        /** The user has performed a context click on an object. */
        val ContextClick
            get() = PlatformHapticFeedbackType.ContextClick

        /** The user has finished a gesture (e.g. on the soft keyboard). */
        val GestureEnd
            get() = PlatformHapticFeedbackType.GestureEnd

        /**
         * The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the
         * gesture action is eligible at a certain threshold of movement, and can be cancelled by
         * moving back past the threshold.
         */
        val GestureThresholdActivate
            get() = PlatformHapticFeedbackType.GestureThresholdActivate

        /**
         * The user has performed a long press on an object that is resulting in an action being
         * performed.
         */
        val LongPress
            get() = PlatformHapticFeedbackType.LongPress

        /** A haptic effect to signal the rejection or failure of a user interaction. */
        val Reject
            get() = PlatformHapticFeedbackType.Reject

        /**
         * The user is switching between a series of many potential choices, for example minutes on
         * a clock face, or individual percentages.
         */
        val SegmentFrequentTick
            get() = PlatformHapticFeedbackType.SegmentFrequentTick

        /**
         * The user is switching between a series of potential choices, for example items in a list
         * or discrete points on a slider.
         */
        val SegmentTick
            get() = PlatformHapticFeedbackType.SegmentTick

        /** The user has performed a selection/insertion handle move on text field. */
        val TextHandleMove
            get() = PlatformHapticFeedbackType.TextHandleMove

        /** The user has toggled a switch or button into the off position. */
        val ToggleOff
            get() = PlatformHapticFeedbackType.ToggleOff

        /** The user has toggled a switch or button into the on position. */
        val ToggleOn
            get() = PlatformHapticFeedbackType.ToggleOn

        /** The user has pressed on a virtual on-screen key. */
        val VirtualKey
            get() = PlatformHapticFeedbackType.VirtualKey

        /** Returns a list of possible values of [HapticFeedbackType]. */
        fun values(): List<HapticFeedbackType> =
            listOf(
                Confirm,
                ContextClick,
                GestureEnd,
                GestureThresholdActivate,
                LongPress,
                Reject,
                SegmentFrequentTick,
                SegmentTick,
                TextHandleMove,
                ToggleOff,
                ToggleOn,
                VirtualKey,
            )
    }
}

internal expect object PlatformHapticFeedbackType {
    val Confirm: HapticFeedbackType
    val ContextClick: HapticFeedbackType
    val GestureEnd: HapticFeedbackType
    val GestureThresholdActivate: HapticFeedbackType
    val LongPress: HapticFeedbackType
    val Reject: HapticFeedbackType
    val SegmentFrequentTick: HapticFeedbackType
    val SegmentTick: HapticFeedbackType
    val TextHandleMove: HapticFeedbackType
    val ToggleOn: HapticFeedbackType
    val ToggleOff: HapticFeedbackType
    val VirtualKey: HapticFeedbackType
}
