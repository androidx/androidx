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

package androidx.navigationevent.testing

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

/**
 * A test implementation of [NavigationEventDispatcherOwner] for verifying
 * [NavigationEventDispatcher] interactions.
 *
 * Use this class in tests to confirm that back-press fallbacks and callback status changes are
 * invoked as expected. It tracks the number of times these events occur via public counters.
 *
 * @param fallbackOnBackPressed A lambda invoked by the [NavigationEventDispatcher] when a back
 *   press occurs and no other callbacks handle it.
 */
public class TestNavigationEventDispatcherOwner(
    fallbackOnBackPressed: TestNavigationEventDispatcherOwner.() -> Unit = {}
) : NavigationEventDispatcherOwner {

    /** The number of times [NavigationEventDispatcher.fallbackOnBackPressed] has been invoked. */
    public var fallbackOnBackPressedInvocations: Int = 0
        private set

    override val navigationEventDispatcher: NavigationEventDispatcher =
        NavigationEventDispatcher(
            fallbackOnBackPressed = {
                fallbackOnBackPressedInvocations++
                fallbackOnBackPressed.invoke(this)
            }
        )
}
