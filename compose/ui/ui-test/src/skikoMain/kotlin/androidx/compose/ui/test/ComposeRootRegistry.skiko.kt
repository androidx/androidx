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

package androidx.compose.ui.test

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformRootForTest

/**
 * Registry where all views implementing [PlatformRootForTest] should be registered while they
 * are attached to the window. This registry is used by the testing library to query the roots'
 * state.
 */
@OptIn(InternalComposeUiApi::class)
internal class ComposeRootRegistry : PlatformContext.RootForTestListener {
    private val lock = Any()
    private val roots = mutableSetOf<PlatformRootForTest>()

    /**
     * Returns if the registry is set up to receive registrations from [PlatformRootForTest]s
     */
    private var isTracking = false

    /**
     * Sets up this registry to be notified of any [PlatformRootForTest] created
     */
    private fun setupRegistry() {
        isTracking = true
    }

    /**
     * Cleans up the changes made by [setupRegistry]. Call this after your test has run.
     */
    private fun tearDownRegistry() {
        // Stop accepting new roots
        isTracking = false
        synchronized(lock) {
            // Clear all references
            roots.clear()
        }
    }

    override fun onRootForTestCreated(root: PlatformRootForTest) {
        synchronized(lock) {
            if (isTracking) {
                roots.add(root)
            }
        }
    }

    override fun onRootForTestDisposed(root: PlatformRootForTest) {
        synchronized(lock) {
            if (isTracking) {
                roots.remove(root)
            }
        }
    }

    /**
     * Returns a copy of the set of all registered [PlatformRootForTest]s that can be interacted with.
     */
    fun getComposeRoots(): Set<PlatformRootForTest> {
        return synchronized(lock) { roots.toSet() }
    }

    fun <R> withRegistry(block: () -> R): R {
        try {
            setupRegistry()
            return block()
        } finally {
            tearDownRegistry()
        }
    }
}
