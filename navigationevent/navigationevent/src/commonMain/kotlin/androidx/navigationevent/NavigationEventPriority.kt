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
 * Priority to be provided to the [NavigationEventCallback] when it is being initialized to
 * determine when the callback should be triggered.
 */
@JvmInline
public value class NavigationEventPriority private constructor(internal val value: Int) {
    public companion object {
        /**
         * Priority level of [NavigationEventCallback]s for overlays such as menus and navigation
         * drawers that should receive event dispatch before non-overlays.
         */
        public val Overlay: NavigationEventPriority = NavigationEventPriority(0)
        /** Default priority level of [NavigationEventCallback]s. */
        public val Default: NavigationEventPriority = NavigationEventPriority(1)
    }
}
