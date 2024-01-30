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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue


/**
 * Tests related to [OwnerSnapshotObserver].
 */
@OptIn(ExperimentalTestApi::class)
class OwnerSnapshotObserverTest {

    /**
     * Verifies that [ObserverModifierNode.onObservedReadsChanged] is called before `onDispose` of
     * a [DisposableEffect] when the read change and the disposal of the effect happen in the same
     * recomposition.
     *
     * This behavior is relied upon by [LazyLayoutPinnableItem] and possibly others.
     *
     * [The issue where the bug was originally discovered](https://youtrack.jetbrains.com/issue/COMPOSE-595).
     * Also see [Slack discussion](https://kotlinlang.slack.com/archives/G010KHY484C/p1700136697942499).
     */
    @Test
    fun onObserveReadsChangedCalledBeforeOnDispose() = runComposeUiTest {
        var value by mutableIntStateOf(0)
        var observedReadsChanged = false
        var onDisposeCalled: Boolean
        setContent {
            // Use a local state so that the "change" we expect to be notified about in
            // observedReadsChanged happens during recomposition and not before.
            val localValue by rememberUpdatedState(value)
            Box(
                Modifier.then(
                    ObserverTestElement(
                        readValues = { localValue },
                        observedReadsChanged = { observedReadsChanged = true }
                    )
                )
            )
            DisposableEffect(localValue) {
                onDispose {
                    assertTrue(
                        actual = observedReadsChanged,
                        message = "onDispose called before onObservedReadsChanged"
                    )
                    onDisposeCalled = true
                }
            }
        }

        observedReadsChanged = false
        onDisposeCalled = false
        value = 1
        waitForIdle()
        assertTrue(observedReadsChanged, "onObservedReadsChanged not called")
        assertTrue(onDisposeCalled, "onDispose not called")
    }


    private class ObserverTestElement(
        private var readValues: () -> Int,
        private val observedReadsChanged: () -> Unit
    ) : ModifierNodeElement<ObserverTestNode>() {
        override fun create(): ObserverTestNode =
            ObserverTestNode(readValues, observedReadsChanged)

        override fun update(node: ObserverTestNode) {
            node.update(readValues, observedReadsChanged)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ObserverTestElement) return false

            if (readValues != other.readValues) return false
            if (observedReadsChanged != other.observedReadsChanged) return false
            return true
        }

        override fun hashCode(): Int {
            return readValues.hashCode()*31 + observedReadsChanged.hashCode()
        }
    }

    private class ObserverTestNode(
        var readValues: () -> Int,
        var observedReadsChanged: () -> Unit
    ) : Modifier.Node(), ObserverModifierNode {
        override fun onAttach() {
            observeReads {
                readValues()
            }
        }

        fun update(readValues: () -> Int, observedReadsChanged: () -> Unit) {
            this.readValues = readValues
            this.observedReadsChanged = observedReadsChanged

            observeReads {
                readValues()
            }
        }

        override fun onObservedReadsChanged() {
            observedReadsChanged()
        }
    }

}