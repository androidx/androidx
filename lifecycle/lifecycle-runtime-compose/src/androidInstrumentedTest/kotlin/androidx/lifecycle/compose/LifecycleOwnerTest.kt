/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LifecycleOwnerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun rememberLifecycleOwner_whenComposed_thenReturnsNewLifecycleOwner() = runTest {
        val parentLifecycleOwner = TestLifecycleOwner()
        lateinit var childLifecycleOwner: LifecycleOwner

        rule.setContent {
            childLifecycleOwner = rememberLifecycleOwner(parent = parentLifecycleOwner)
        }

        rule.awaitIdle()

        assertThat(childLifecycleOwner).isNotSameInstanceAs(parentLifecycleOwner)
        assertThat(childLifecycleOwner.lifecycle.currentState).isEqualTo(State.STARTED)
    }

    @Test
    fun rememberLifecycleOwner_whenParentLifecycleChanges_thenChildLifecycleFollows() = runTest {
        val parentLifecycleOwner = TestLifecycleOwner()
        lateinit var childLifecycle: Lifecycle

        rule.setContent {
            val owner = rememberLifecycleOwner(parent = parentLifecycleOwner)
            childLifecycle = owner.lifecycle
        }
        rule.awaitIdle()

        parentLifecycleOwner.currentState = State.CREATED
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.CREATED)

        parentLifecycleOwner.currentState = State.RESUMED
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.RESUMED)

        parentLifecycleOwner.currentState = State.DESTROYED
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.DESTROYED)
    }

    @Test
    fun rememberLifecycleOwner_whenParentStateExceedsMaxLifecycle_thenChildStateIsCapped() =
        runTest {
            val parentLifecycleOwner = TestLifecycleOwner(State.RESUMED, Dispatchers.Main)
            lateinit var childLifecycle: Lifecycle

            rule.setContent {
                val owner =
                    rememberLifecycleOwner(
                        maxLifecycle = State.STARTED,
                        parent = parentLifecycleOwner,
                    )
                childLifecycle = owner.lifecycle
            }
            rule.awaitIdle()

            assertThat(childLifecycle.currentState).isEqualTo(State.STARTED)
        }

    @Test
    fun rememberLifecycleOwner_whenMaxLifecycleParameterChanges_thenChildStateUpdates() = runTest {
        val parentLifecycleOwner = TestLifecycleOwner(State.RESUMED, Dispatchers.Main)
        lateinit var childLifecycle: Lifecycle
        var maxLifecycle by mutableStateOf(State.STARTED)

        rule.setContent {
            val owner =
                rememberLifecycleOwner(maxLifecycle = maxLifecycle, parent = parentLifecycleOwner)
            childLifecycle = owner.lifecycle
        }

        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.STARTED)

        maxLifecycle = State.RESUMED
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.RESUMED)
    }

    @Test
    fun rememberLifecycleOwner_whenDisposed_thenObserverIsRemoved() = runTest {
        val parentLifecycleOwner = TestLifecycleOwner()
        assertThat(parentLifecycleOwner.observerCount).isEqualTo(0)

        var showContent by mutableStateOf(true)

        rule.setContent {
            if (showContent) {
                rememberLifecycleOwner(parent = parentLifecycleOwner)
            }
        }

        rule.awaitIdle()
        assertThat(parentLifecycleOwner.observerCount).isEqualTo(1)

        showContent = false
        rule.awaitIdle()
        assertThat(parentLifecycleOwner.observerCount).isEqualTo(0)
    }

    @Test
    fun rememberLifecycleOwner_whenDisposed_thenChildIsDestroyed() = runTest {
        // Keep the parent alive to ensure the child's destruction is not just
        // a result of the parent being destroyed.
        val parentLifecycleOwner = TestLifecycleOwner(State.RESUMED)
        lateinit var childLifecycle: Lifecycle
        var showContent by mutableStateOf(true)

        rule.setContent {
            if (showContent) {
                val owner = rememberLifecycleOwner(parent = parentLifecycleOwner)
                childLifecycle = owner.lifecycle
            }
        }

        rule.awaitIdle()
        // At this point, the child should be active.
        assertThat(childLifecycle.currentState).isEqualTo(State.RESUMED)

        // Now, remove the composable from the composition.
        showContent = false
        rule.awaitIdle()

        // Verify that the child lifecycle was explicitly moved to DESTROYED,
        // even though the parent is still RESUMED.
        assertThat(childLifecycle.currentState).isEqualTo(State.DESTROYED)
        assertThat(parentLifecycleOwner.lifecycle.currentState).isEqualTo(State.RESUMED)
    }

    @Test
    fun rememberLifecycleOwner_whenParentIsNull_thenStartsItself() = runTest {
        lateinit var rootLifecycle: Lifecycle

        rule.setContent {
            val owner = rememberLifecycleOwner(parent = null)
            rootLifecycle = owner.lifecycle
        }

        rule.awaitIdle()

        // When there is no parent, the lifecycle should start itself as RESUMED by default.
        assertThat(rootLifecycle.currentState).isEqualTo(State.RESUMED)
    }

    @Test
    fun rememberLifecycleOwner_whenParentIsNullAndMaxLifecycle_thenIsCapped() = runTest {
        lateinit var rootLifecycle: Lifecycle

        rule.setContent {
            val owner = rememberLifecycleOwner(parent = null, maxLifecycle = State.STARTED)
            rootLifecycle = owner.lifecycle
        }

        rule.awaitIdle()

        // The root lifecycle should respect the maxLifecycle cap.
        assertThat(rootLifecycle.currentState).isEqualTo(State.STARTED)
    }

    @Test
    fun rememberLifecycleOwner_whenParentIsNull_thenDestroyedOnDispose() = runTest {
        lateinit var rootLifecycle: Lifecycle
        var showContent by mutableStateOf(true)

        rule.setContent {
            if (showContent) {
                val owner = rememberLifecycleOwner(parent = null)
                rootLifecycle = owner.lifecycle
            }
        }

        rule.awaitIdle()
        assertThat(rootLifecycle.currentState).isEqualTo(State.RESUMED)

        // Remove the composable — root should destroy itself.
        showContent = false
        rule.awaitIdle()

        assertThat(rootLifecycle.currentState).isEqualTo(State.DESTROYED)
    }

    @Test
    fun rememberLifecycleOwner_withDefaultParent_followsLocalLifecycle() = runTest {
        // This parent will be provided via CompositionLocal
        val parentLifecycleOwner = TestLifecycleOwner(State.CREATED, Dispatchers.Main)
        lateinit var childLifecycle: Lifecycle

        rule.setContent {
            // Provide the parent as the LocalLifecycleOwner
            CompositionLocalProvider(LocalLifecycleOwner provides parentLifecycleOwner) {
                // Call rememberLifecycleOwner without an explicit parent.
                // It should pick up parentLifecycleOwner from the composition local.
                val childOwner = rememberLifecycleOwner()
                childLifecycle = childOwner.lifecycle
            }
        }

        // Verify the child's initial state matches the parent's
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.CREATED)

        // Verify the child follows the parent's state changes
        parentLifecycleOwner.currentState = State.RESUMED
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.RESUMED)

        parentLifecycleOwner.currentState = State.DESTROYED
        rule.awaitIdle()
        assertThat(childLifecycle.currentState).isEqualTo(State.DESTROYED)
    }
}
