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

package androidx.navigationevent

import kotlin.jvm.JvmInline

/**
 * A type-safe representation of the direction of a navigation gesture, such as backward or forward.
 *
 * This is used by the system to interpret the user's navigational intent, for example, whether they
 * intend to dismiss the current screen or advance to a new one.
 */
@JvmInline
internal value class NavigationEventDirection private constructor(private val value: Int) {
    companion object {
        /**
         * Represents a backward navigation event.
         *
         * This direction is typically associated with 'back' actions, such as dismissing the
         * current screen, closing an overlay like a menu or dialog, or navigating to the previous
         * destination in the history stack.
         */
        val Back = NavigationEventDirection(0)

        /**
         * Represents a forward navigation event.
         *
         * This direction is typically associated with actions that advance the user to a new screen
         * or deeper into the application's UI flow.
         */
        val Forward = NavigationEventDirection(1)
    }
}
