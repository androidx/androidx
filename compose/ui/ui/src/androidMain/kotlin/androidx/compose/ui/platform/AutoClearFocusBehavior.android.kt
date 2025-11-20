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

package androidx.compose.ui.platform

/**
 * A behavior for auto-clearing focus for [ComposeView].
 *
 * @see ComposeView.autoClearFocusBehavior
 */
// Note to maintainers: changing the behavior of existing constants is a behavior change, which
// is unexpected if a developer is explicitly using one of these constants.
// If the behavior for a new constant supersedes an existing constant, it is appropriate to
// deprecate an existing constant, pointing to the new constant.
// The value of Default can change without deprecation, but should still be done mindfully as it is
// also a behavior change.
@JvmInline
value class AutoClearFocusBehavior private constructor(private val value: Int) {
    companion object {
        /** Interacting with this [ComposeView] won't automatically clear focus. */
        val None = AutoClearFocusBehavior(0)

        /**
         * When interacting with this [ComposeView] with a cursor-based input device, a pointer down
         * event that occurs outside the currently focused item will result in focus being cleared.
         *
         * This results in a UX experience where clicking with a mouse or touchpad outside of the
         * currently focused item will clear focus, even if that item is focusable in touch mode.
         * This does _not_ apply to stylus input.
         */
        val CursorBased = AutoClearFocusBehavior(1)

        /**
         * The default [AutoClearFocusBehavior]. This value is currently [CursorBased], but this is
         * subject to change.
         */
        val Default
            get() = CursorBased
    }
}
