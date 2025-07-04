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

package androidx.compose.runtime

import androidx.compose.runtime.mock.compositionTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestResult

// This test duplicates the common CompositionTest, but makes it run properly on web.
// This test requires that compositionTest is called in the test body.
// On Web, compositionTest calls returns a Promise, and it's ignored when not returned from the Test
// method.
// Here we make sure it runs (not ignored) by wrapping it in another Promise returned from the Test
// method.
class CompositionTestWeb {

    @Test // https://youtrack.jetbrains.com/issue/CMP-7453
    fun testRememberObserver_Abandon_Recompose() = wrapTestWithCoroutine {
        val abandonedObjects = mutableListOf<RememberObserver>()
        val observed =
            object : RememberObserver {
                override fun onAbandoned() {
                    abandonedObjects.add(this)
                }

                override fun onForgotten() {
                    error("Unexpected call to onForgotten")
                }

                override fun onRemembered() {
                    error("Unexpected call to onRemembered")
                }
            }

        var promiseStarted = false
        var promiseCompleted = false

        assertFailsWith(IllegalStateException::class, message = "Throw") {
            promiseStarted = true
            compositionTest {
                    val rememberObject = mutableStateOf(false)

                    compose {
                        if (rememberObject.value) {
                            @Suppress("UNUSED_EXPRESSION") remember { observed }
                            error("Throw")
                        }
                    }

                    assertTrue(abandonedObjects.isEmpty())

                    rememberObject.value = true

                    advance(ignorePendingWork = true)
                }
                .awaitCompletion()

            promiseCompleted = true
        }

        assertTrue(promiseStarted)
        assertFalse(promiseCompleted)
        assertArrayEquals(listOf(observed), abandonedObjects)
    }
}

internal expect suspend fun TestResult.awaitCompletion()

internal expect fun wrapTestWithCoroutine(block: suspend () -> Unit): TestResult

class TestWrapTest {

    @Test
    fun t() = wrapTestWithCoroutine {
        var result = false

        kotlinx.coroutines.test.runTest { result = true }.awaitCompletion()

        assertTrue(result)
        println("Completed\n")
    }
}
