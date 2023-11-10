/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test.junit4

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.SkiaRootForTest

/**
 * Registry where all views implementing [SkiaRootForTest] should be registered while they
 * are attached to the window. This registry is used by the testing library to query the roots'
 * state.
 */
@OptIn(InternalComposeUiApi::class)
internal class ComposeRootRegistry {
    private val lock = Any()
    private val roots = mutableSetOf<SkiaRootForTest>()

    /**
     * Returns if the registry is setup to receive registrations from [SkiaRootForTest]s
     */
    val isSetUp: Boolean
        get() = SkiaRootForTest.onRootCreatedCallback == ::onRootCreated

    /**
     * Sets up this registry to be notified of any [SkiaRootForTest] created
     */
    private fun setupRegistry() {
        SkiaRootForTest.onRootCreatedCallback = ::onRootCreated
        SkiaRootForTest.onRootDisposedCallback = ::onRootDisposed
    }

    /**
     * Cleans up the changes made by [setupRegistry]. Call this after your test has run.
     */
    private fun tearDownRegistry() {
        // Stop accepting new roots
        SkiaRootForTest.onRootCreatedCallback = null
        SkiaRootForTest.onRootDisposedCallback = null
        synchronized(lock) {
            // Clear all references
            roots.clear()
        }
    }

    private fun onRootCreated(root: SkiaRootForTest) {
        synchronized(lock) {
            if (isSetUp) {
                roots.add(root)
            }
        }
    }

    private fun onRootDisposed(root: SkiaRootForTest) {
        synchronized(lock) {
            if (isSetUp) {
                roots.remove(root)
            }
        }
    }

    /**
     * Returns a copy of the set of all registered [SkiaRootForTest]s that can be interacted with.
     */
    fun getComposeRoots(): Set<SkiaRootForTest> {
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
