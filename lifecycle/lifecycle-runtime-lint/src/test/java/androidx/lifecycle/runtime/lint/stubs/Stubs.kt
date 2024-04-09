/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle.runtime.lint.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles

internal val LIFECYCLE_STUB = TestFiles.kt(
    "androidx/lifecycle/PausingDispatcher.kt",
    """
        package androidx.lifecycle;

        import kotlinx.coroutines.CoroutineScope

        abstract class Lifecycle {
            enum class State { CREATED, STARTED }
            fun isAtLeast(state: State): Boolean {
                return true
            }
        }

        interface LifecycleOwner {
            val lifecycle: Lifecycle
        }

        suspend fun <T> Lifecycle.whenCreated(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }

        suspend fun <T> Lifecycle.whenStarted(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }

        suspend fun <T> Lifecycle.whenResumed(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }

        suspend fun <T> LifecycleOwner.whenCreated(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }

        suspend fun <T> LifecycleOwner.whenStarted(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }

        suspend fun <T> LifecycleOwner.whenResumed(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }
    """
).indented().within("src")

internal val COROUTINES_STUB = TestFiles.kt(
    "kotlinx/coroutines/GlobalScope.kt",
    """
        package kotlinx.coroutines;

        import kotlinx.coroutines.CoroutineScope

        interface CoroutineScope {}

        object GlobalScope {
            fun launch(block: suspend () -> Unit) {}
        }

    """
).indented().within("src")

internal val VIEW_STUB = TestFiles.kt(
    """
        package android.view

        class View {}

        class FooView: View() {
            fun foo() {}
        }
    """
).indented().within("src")

private val FRAGMENT_STUB = LintDetectorTest.java(
    """
    package androidx.fragment.app;

    import androidx.lifecycle.LifecycleOwner;

    public class Fragment implements LifecycleOwner {
        public LifecycleOwner getViewLifecycleOwner() {}
    }
"""
)

private val ACTIVITY_STUB = LintDetectorTest.java(
    """
    package androidx.core.app;

    import androidx.lifecycle.LifecycleOwner;

    public class ComponentActivity implements LifecycleOwner {}
"""
)

private val REPEAT_ON_LIFECYCLE_STUB = TestFiles.kt(
    "androidx/lifecycle/RepeatOnLifecycle.kt",
    """
        package androidx.lifecycle;

        import androidx.lifecycle.LifecycleOwner
        import kotlinx.coroutines.CoroutineScope

        abstract class Lifecycle {
            enum class State { CREATED, STARTED }
            fun isAtLeast(state: State): Boolean {
                return true
            }
        }

        public suspend fun Lifecycle.repeatOnLifecycle(
            state: Lifecycle.State,
            block: suspend CoroutineScope.() -> Unit
        ) {
            throw Error()
        }

        public suspend fun LifecycleOwner.repeatOnLifecycle(
            state: Lifecycle.State,
            block: suspend CoroutineScope.() -> Unit
        ) {
            throw Error()
        }
    """
).indented().within("src")

// stubs for testing calls to LiveData.observe calls
internal val REPEAT_ON_LIFECYCLE_STUBS = arrayOf(
    REPEAT_ON_LIFECYCLE_STUB, LIFECYCLE_STUB, FRAGMENT_STUB, ACTIVITY_STUB, COROUTINES_STUB
)
