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

import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHandler

/**
 * A test implementation of [NavigationEventDispatcherOwner] for verifying
 * [NavigationEventDispatcher] interactions.
 *
 * Use this class in tests to confirm that the `onBackCompletedFallback` and
 * `onForwardCompletedFallback` actions are invoked as expected. It tracks the number of times these
 * events occur.
 *
 * @param onBackCompletedFallback An optional lambda to execute when the [NavigationEventDispatcher]
 *   back fallback is triggered.
 */
public class TestNavigationEventDispatcherOwner(
    private val onBackCompletedFallback: TestNavigationEventDispatcherOwner.() -> Unit = {}
) : NavigationEventDispatcherOwner {

    /**
     * Backing property for the forward fallback action.
     *
     * This is mutated by the secondary constructor because it cannot be passed through the primary
     * constructor without breaking binary compatibility.
     */
    private var onForwardCompletedFallback: TestNavigationEventDispatcherOwner.() -> Unit = {}

    /**
     * Creates a [TestNavigationEventDispatcherOwner] with both forward and back fallbacks.
     *
     * @param onForwardCompletedFallback A lambda to execute when the [NavigationEventDispatcher]
     *   forward fallback is triggered.
     * @param onBackCompletedFallback An optional lambda to execute when the
     *   [NavigationEventDispatcher] back fallback is triggered.
     */
    public constructor(
        onForwardCompletedFallback: TestNavigationEventDispatcherOwner.() -> Unit,
        onBackCompletedFallback: TestNavigationEventDispatcherOwner.() -> Unit,
    ) : this(onBackCompletedFallback = onBackCompletedFallback) {
        // Secondary constructor required mutable property for binary compatibility.
        // Kotlin only generates the necessary 0-argument JVM constructor
        // when all primary constructor parameters have default values.
        // Inverting these alters the JVM <init> metadata and breaks the ABI.
        this.onForwardCompletedFallback = onForwardCompletedFallback
    }

    /**
     * The number of times the dispatcher's [onBackCompletedFallback] lambda has been invoked.
     *
     * This counter is incremented when a back navigation event completes and no
     * [NavigationEventHandler] handles it.
     */
    public var onBackCompletedFallbackInvocations: Int = 0
        private set

    /**
     * The number of times the dispatcher's [onForwardCompletedFallback] lambda has been invoked.
     *
     * This counter is incremented when a forward navigation event completes and no
     * [NavigationEventHandler] handles it.
     */
    public var onForwardCompletedFallbackInvocations: Int = 0
        private set

    /**
     * The [NavigationEventDispatcher] instance managed by this owner.
     *
     * This dispatcher is created with the `onBackCompletedFallback` and
     * `onForwardCompletedFallback` lambdas provided to the [TestNavigationEventDispatcherOwner]'s
     * constructor, which increments [onBackCompletedFallbackInvocations] and
     * [onForwardCompletedFallbackInvocations].
     */
    override val navigationEventDispatcher: NavigationEventDispatcher =
        NavigationEventDispatcher(
            onBackCompletedFallback = {
                onBackCompletedFallbackInvocations++
                onBackCompletedFallback.invoke(this)
            },
            onForwardCompletedFallback = {
                onForwardCompletedFallbackInvocations++
                onForwardCompletedFallback.invoke(this)
            },
        )

    /**
     * The [DirectNavigationEventInput] instance managed by this owner.
     *
     * This input is automatically added to the [navigationEventDispatcher] during initialization,
     * allowing for direct simulation of navigation events.
     */
    public val navigationEventInput: DirectNavigationEventInput = DirectNavigationEventInput()

    init {
        navigationEventDispatcher.addInput(navigationEventInput)
    }
}
