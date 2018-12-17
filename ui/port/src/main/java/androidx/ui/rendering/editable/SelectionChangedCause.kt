/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.editable

/**
 * Indicates what triggered the change in selected text (including changes to the cursor location).
 */
enum class SelectionChangedCause {
    /**
     * The user tapped on the text and that caused the selection (or the location of the cursor) to
     * change.
     */
    TOP,

    /**
     * The user tapped twice in quick succession on the text and that caused the selection (or the
     * location of the cursor) to change.
     */
    DOUBLE_TAP,

    /**
     * The user long-pressed the text and that caused the selection (or the location of the cursor)
     * to change.
     */
    LONG_PRESS,

    /**
     * The user used the keyboard to change the selection or the location of the cursor.
     *
     * Keyboard-triggered selection changes may be caused by the IME as well as by accessibility
     * tools (e.g. TalkBack on Android).
     */
    KEYBOARD,
}