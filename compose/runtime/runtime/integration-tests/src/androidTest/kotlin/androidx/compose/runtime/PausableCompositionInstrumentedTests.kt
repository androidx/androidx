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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PausableCompositionInstrumentedTests {
    @get:Rule val rule = createComposeRule()

    @Test
    fun changeTheKeyUsedInPrecomposition() {
        val state = SubcomposeLayoutState()
        var precompositionKey by mutableStateOf("1")
        var addSlot by mutableStateOf(false)
        lateinit var holder: SaveableStateHolder

        rule.setContent {
            holder = rememberSaveableStateHolder()
            SubcomposeLayout(state) {
                if (addSlot) {
                    subcompose("measurepass") { holder.SaveableStateProvider("1") {} }
                }
                layout(10, 10) {}
            }
        }

        val precomposition =
            rule.runOnIdle {
                val precomposition =
                    state.createPausedPrecomposition("precomposition") {
                        holder.SaveableStateProvider(precompositionKey) {}
                    }

                while (!precomposition.isComplete) {
                    precomposition.resume { false }
                }

                precompositionKey = "2"

                addSlot = true

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }
            precomposition.apply()
        }
    }

    @Test
    fun pausedPrecompositionIsNotRecomposedOnItsOwn() {
        val state = SubcomposeLayoutState()
        var key by mutableStateOf("1")
        val composed = mutableListOf<String>()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        val precomposition =
            rule.runOnIdle {
                val precomposition = state.createPausedPrecomposition(Unit) { composed.add(key) }
                while (!precomposition.isComplete) {
                    precomposition.resume { false }
                }

                assertThat(composed).isEqualTo(listOf("1"))
                composed.clear()

                // recompose just composed composable (which wasn't yet applied)
                key = "2"

                precomposition
            }

        rule.runOnIdle {
            // check that recomposition didn't happen on its own.
            // we don't expect that for non applied compositions.
            assertThat(composed).isEqualTo(emptyList<String>())

            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }
            precomposition.apply()

            // recomposition happened
            assertThat(composed).isEqualTo(listOf("2"))
        }
    }

    @Test // b/404058957
    fun test_for_404058957() {
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var active by mutableStateOf(true)
        var modifier by mutableStateOf<Modifier>(Modifier)

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        val precomposition =
            rule.runOnIdle {
                active = true
                val precomposition =
                    state.createPausedPrecomposition(Unit) {
                        ReusableContentHost(active) {
                            Layout(modifier) { _, _ -> layout(10, 10) {} }
                        }
                    }
                precomposition.resume { false }

                active = false

                precomposition
            }

        rule.runOnIdle {
            precomposition.resume { false }
            active = true
        }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }
            precomposition.apply()

            // update modifier on just applied composition
            modifier = Modifier.drawBehind {}
        }

        rule.waitForIdle()
    }

    @Test
    fun movableContentDisposed() {
        val state = SubcomposeLayoutState()
        var something by mutableStateOf("")
        var emitMovable by mutableStateOf(true)
        var activeMovableContentsCount = 0
        val movableContent = movableContentOf {
            Box(Modifier.size(10.dp))
            DisposableEffect(Unit) {
                activeMovableContentsCount++
                onDispose { activeMovableContentsCount-- }
            }
        }

        rule.setContent {
            something
            SubcomposeLayout(state) { layout(10, 10) {} }
        }
        val precomposition =
            rule.runOnIdle {
                val precomposition =
                    state.createPausedPrecomposition(Unit) {
                        if (emitMovable) {
                            movableContent()
                        }
                    }
                precomposition.resume { false }

                emitMovable = false

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }
            precomposition.apply()
        }

        rule.runOnIdle { assertThat(activeMovableContentsCount).isEqualTo(0) }
    }

    @Test
    fun movableContent_virtual_node() {
        val state = SubcomposeLayoutState()
        var emitMovable by mutableStateOf(true)
        val movableContent =
            movableContentOf<Boolean> { flag ->
                Box(if (flag) Modifier.background(Color.Red) else Modifier)
            }
        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        val precomposition =
            rule.runOnIdle {
                val precomposition =
                    state.createPausedPrecomposition(Unit) {
                        Box {}
                        if (emitMovable) {
                            Box {}
                            movableContent(emitMovable)
                        } else {
                            Row {
                                Box {}
                                movableContent(emitMovable)
                            }
                        }
                    }
                precomposition.resume { false }

                emitMovable = false

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }
            precomposition.apply()
        }

        rule.runOnIdle {}
    }

    @Test
    fun precomposingWithPauseAndExtraRecomposition_effectAppliedOnlyOnce() {
        // initialize SubcomposeLayout with one composition in a reuse pool
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var addSlot by mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot) {
                    subcompose("for-reuse") {}
                }
                layout(10, 10) {}
            }
        }
        rule.runOnIdle { addSlot = false }

        // do pausable precomposition
        var outerCompositionHappened = false
        var applyCalls = 0
        var recompositionTrigger by mutableStateOf(Unit, neverEqualPolicy())

        val precomposition =
            rule.runOnIdle {
                val precomposition =
                    state.createPausedPrecomposition(Unit) {
                        outerCompositionHappened = true
                        DisposableEffectWrapper(
                            onComposed = { recompositionTrigger },
                            onApplied = { applyCalls++ },
                        )
                    }

                // resume and pause before composing DisposableEffectWrapper
                precomposition.resume { outerCompositionHappened }

                // continue after the pause
                precomposition.resume { false }

                // trigger recomposition
                recompositionTrigger = Unit

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }

            precomposition.apply()

            assertThat(applyCalls).isEqualTo(1)
        }
    }

    @Test
    fun precomposingWithPauseAndExtraRecomposition_effectAppliedOnlyOnce2() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        // do pausable precomposition
        var outerCompositionHappened = false
        var applyCalls = 0
        var key by mutableStateOf("A")

        val precomposition =
            rule.runOnIdle {
                val precomposition =
                    state.createPausedPrecomposition(Unit) {
                        outerCompositionHappened = true
                        ReusableContent(key) {
                            DisposableEffectWrapper(onComposed = {}, onApplied = { applyCalls++ })
                        }
                    }
                // resume and pause before composing DisposableEffectWrapper
                precomposition.resume { outerCompositionHappened }
                // continue after the pause
                precomposition.resume { false }

                // trigger recomposition
                key = "B"

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }
            precomposition.apply()

            assertThat(applyCalls).isEqualTo(1)
        }
    }

    @Test
    fun precomposingWithPauseAndExtraRecomposition_rememberedValueNotRecreated() {
        // initialize SubcomposeLayout with one composition in a reuse pool
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var addSlot by mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot) {
                    subcompose("for-reuse") {}
                }
                layout(10, 10) {}
            }
        }
        rule.runOnIdle { addSlot = false }

        // do pausable precomposition
        var outerCompositionHappened = false
        var recompositionTrigger by mutableStateOf(Unit, neverEqualPolicy())
        var rememberCalls = 0

        val precomposition =
            rule.runOnIdle {
                val precomposition =
                    state.createPausedPrecomposition(Unit) {
                        outerCompositionHappened = true
                        RememberWrapper(
                            onComposed = { recompositionTrigger },
                            onRemembered = { rememberCalls++ },
                        )
                    }

                // resume and pause before composing DisposableEffectWrapper
                precomposition.resume { outerCompositionHappened }

                // continue after the pause
                precomposition.resume { false }

                // trigger recomposition
                recompositionTrigger = Unit

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }

            assertThat(rememberCalls).isEqualTo(1)
        }
    }

    @Test
    fun pausedPrecompositionIsNotCrashingWhenRecompositionIsTriggeredAndIgnoredMultipleTimes() {
        val state = SubcomposeLayoutState()
        var key by mutableStateOf("1")
        lateinit var scope: RecomposeScope

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) {
                    key
                    scope = currentRecomposeScope
                }
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }

            // first recomposition could be triggered by a state change
            key = "2"
        }

        rule.runOnIdle {
            // second recomposition should happen not because of the state change
            scope.invalidate()
        }
        rule.waitForIdle()
    }

    @Test
    fun precomposingWithPauseAndExtraRecomposition_updateModifierDuringRecompositionNotCrashing() {
        // initialize SubcomposeLayout with one composition in a reuse pool
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var addSlot by mutableStateOf(true)

        var modifier by mutableStateOf(Modifier.drawBehind {})
        val content = @Composable { LayoutWrapper(modifier) }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot) {
                    subcompose("for-reuse", content)
                }
                layout(10, 10) {}
            }
        }
        rule.runOnIdle { addSlot = false }

        val precomposition =
            rule.runOnIdle {
                val precomposition = state.createPausedPrecomposition(Unit, content)

                // resume and pause before composing Wrapper
                precomposition.resume { true }

                // trigger recomposition
                modifier = Modifier

                precomposition
            }

        rule.runOnIdle {
            while (!precomposition.isComplete) {
                precomposition.resume { false }
            }

            precomposition.apply()
        }
    }
}

@Composable
fun DisposableEffectWrapper(onComposed: () -> Unit, onApplied: () -> Unit) {
    onComposed()
    DisposableEffect(Unit) {
        onApplied()
        onDispose {}
    }
}

@Composable
fun RememberWrapper(onComposed: () -> Unit, onRemembered: () -> Unit) {
    onComposed()
    val a = remember {
        onRemembered()
        object {}
    }
    println(a)
}

@Composable
fun LayoutWrapper(modifier: Modifier) {
    Layout(modifier) { measurables, constraints -> layout(10, 10) {} }
}
