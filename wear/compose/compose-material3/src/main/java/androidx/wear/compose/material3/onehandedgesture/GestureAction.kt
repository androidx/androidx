/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.runtime.Immutable

/**
 * Defines the distinct actions of one-handed gestures supported by the system.
 *
 * When a gesture is performed, the system first identifies all handlers registered for that
 * specific [GestureAction]. From that set, it identifies the handlers with the highest assigned
 * priority.
 *
 * Handlers with the highest priority take precedence. It is not recommended to register multiple
 * gestures for the same action and priority (but if that is the case, all of them will be actioned)
 */
@Immutable
@JvmInline
public value class GestureAction internal constructor(internal val value: Int) {
    public companion object {
        /**
         * The primary gesture action.
         *
         * Depending on the current UI context, this could be a click, selection or scroll action.
         */
        public val Primary: GestureAction = GestureAction(1)

        /**
         * The dismiss gesture action.
         *
         * Typically used to perform a "go back" action, close a dialog or exit the current screen.
         */
        public val Dismiss: GestureAction = GestureAction(2)
    }
}
