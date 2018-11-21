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

package androidx.savedstate.bundle

import android.os.Bundle
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class BundlableSavedStateRegistryTest {

    @Test
    fun saveRestoreFlow() {
        val registry = BundleSavedStateRegistry()
        registry.registerSavedStateProvider("a") { bundleOf("foo", 1) }
        registry.registerSavedStateProvider("b") { bundleOf("foo", 2) }
        val state = Bundle()
        registry.performSave(state)

        val newRegistry = BundleSavedStateRegistry()
        newRegistry.performRestore(state)

        assertThat(newRegistry.isRestored, `is`(true))
        val bundleForA = newRegistry.consumeRestoredStateForKey("a")
        val bundleForB = newRegistry.consumeRestoredStateForKey("b")

        assertThat(bundleForA.isSame(bundleOf("foo", 1)), `is`(true))
        assertThat(bundleForB.isSame(bundleOf("foo", 2)), `is`(true))
    }
}

private fun bundleOf(key: String, value: Int): Bundle {
    val result = Bundle()
    result.putInt(key, value)
    return result
}

private fun Bundle?.isSame(other: Bundle): Boolean {
    if (this == null) {
        return false
    }
    if (keySet() != other.keySet()) {
        return false
    }
    for (key in keySet()) {
        if (get(key) != other.get(key)) {
            return false
        }
    }
    return true
}