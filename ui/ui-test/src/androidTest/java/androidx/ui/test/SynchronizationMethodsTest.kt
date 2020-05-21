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

package androidx.ui.test

import androidx.test.filters.MediumTest
import androidx.ui.test.android.AndroidOwnerRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SynchronizationMethodsTest {

    @Test
    fun runOnUiThread() {
        val result = runOnUiThread { "Hello" }
        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun runOnUiThread_void() {
        var called = false
        runOnUiThread { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun runOnUiThread_nullable() {
        val result: String? = runOnUiThread { null }
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun runOnIdleCompose() {
        withAndroidOwnerRegistry {
            val result = runOnIdleCompose { "Hello" }
            assertThat(result).isEqualTo("Hello")
        }
    }

    @Test
    fun runOnIdleCompose_void() {
        withAndroidOwnerRegistry {
            var called = false
            runOnIdleCompose { called = true }
            assertThat(called).isTrue()
        }
    }

    @Test
    fun runOnIdleCompose_nullable() {
        withAndroidOwnerRegistry {
            val result: String? = runOnIdleCompose { null }
            assertThat(result).isEqualTo(null)
        }
    }

    private fun withAndroidOwnerRegistry(block: () -> Unit) {
        AndroidOwnerRegistry.setupRegistry()
        try {
            block()
        } finally {
            AndroidOwnerRegistry.tearDownRegistry()
        }
    }
}