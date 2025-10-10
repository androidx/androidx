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
import androidx.navigationevent.NavigationEventHandler

/**
 * A test implementation of [NavigationEventDispatcherOwner] for verifying
 * [NavigationEventDispatcher] interactions.
 *
 * Use this class in tests to confirm that the `onBackCompletedFallback` action is invoked as
 * expected. It tracks the number of times this event occurs.
 *
 * @param onBackCompletedFallback An optional lambda to execute when the [NavigationEventDispatcher]
 *   back fallback is triggered. This is invoked *after* the internal invocation counter is
 *   incremented.
 */
public class TestNavigationEventDispatcherOwner(
    onBackCompletedFallback: TestNavigationEventDispatcherOwner.() -> Unit = {}
) : NavigationEventDispatcherOwner {

    /**
     * The number of times the dispatcher's `onBackCompletedFallback` lambda has been invoked.
     *
     * This counter is incremented when a back navigation event completes and no
     * [NavigationEventHandler] handles it.
     */
    public var onBackCompletedFallbackInvocations: Int = 0
        private set

    /**
     * The [NavigationEventDispatcher] instance managed by this owner.
     *
     * This dispatcher is created with the `onBackCompletedFallback` lambda provided to the
     * [TestNavigationEventDispatcherOwner]'s constructor, which increments
     * [onBackCompletedFallback].
     */
    override val navigationEventDispatcher: NavigationEventDispatcher =
        NavigationEventDispatcher(
            onBackCompletedFallback = {
                onBackCompletedFallbackInvocations++
                onBackCompletedFallback.invoke(this)
            }
        )
}
