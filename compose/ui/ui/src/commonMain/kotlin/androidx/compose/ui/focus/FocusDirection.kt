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

package androidx.compose.ui.focus

import kotlin.jvm.JvmInline

/**
 * The [FocusDirection] is used to specify the direction for a [FocusManager.moveFocus] request.
 *
 * @sample androidx.compose.ui.samples.MoveFocusSample
 */
@JvmInline
public value class FocusDirection internal constructor(private val value: Int) {

    public override fun toString(): String {
        return when (this) {
            Next -> "Next"
            Previous -> "Previous"
            Left -> "Left"
            Right -> "Right"
            Up -> "Up"
            Down -> "Down"
            Enter -> "Enter"
            Exit -> "Exit"
            else -> "Invalid FocusDirection"
        }
    }

    public companion object {
        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * next focusable item.
         *
         * @sample androidx.compose.ui.samples.MoveFocusSample
         */
        public val Next: FocusDirection
            get() = FocusDirection(1)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * previous focusable item.
         *
         * @sample androidx.compose.ui.samples.MoveFocusSample
         */
        public val Previous: FocusDirection
            get() = FocusDirection(2)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * next focusable item to the left of the currently focused item.
         *
         * @sample androidx.compose.ui.samples.MoveFocusSample
         */
        public val Left: FocusDirection
            get() = FocusDirection(3)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * next focusable item to the right of the currently focused item.
         *
         * @sample androidx.compose.ui.samples.MoveFocusSample
         */
        public val Right: FocusDirection
            get() = FocusDirection(4)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * next focusable item that is above the currently focused item.
         *
         * @sample androidx.compose.ui.samples.MoveFocusSample
         */
        public val Up: FocusDirection
            get() = FocusDirection(5)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * next focusable item that is below the currently focused item.
         *
         * @sample androidx.compose.ui.samples.MoveFocusSample
         */
        public val Down: FocusDirection
            get() = FocusDirection(6)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         * next focusable item that is a child of the currently focused item.
         */
        public val Enter: FocusDirection
            get() = FocusDirection(7)

        /**
         * Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to the
         * parent of the currently focused item.
         */
        public val Exit: FocusDirection
            get() = FocusDirection(8)
    }
}
