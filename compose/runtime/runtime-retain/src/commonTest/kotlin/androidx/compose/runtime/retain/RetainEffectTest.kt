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

package androidx.compose.runtime.retain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertContentEquals

class RetainEffectTest {

    @Test
    fun testRetainEffect_retained() = compositionTest {
        var mount by mutableStateOf(false)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun RetainLogger(name: String) {
            RetainedEffect(Unit) {
                log("Retain:$name")
                onRetire { log("Retire:$name") }
            }
        }

        compose {
            RetainLogger("1")
            if (mount) {
                RetainLogger("2")
            }
        }

        assertContentEquals(
            message = "Initial RetainedEffect sequence didn't match",
            expected = listOf("Retain:1"),
            actual = logHistory,
        )
        mount = true
        log("recompose")
        expectChanges()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after recomposing",
            expected = listOf("Retain:1", "recompose", "Retain:2"),
            actual = logHistory,
        )
    }

    @Test
    fun testRetainEffect_keysChanged() = compositionTest {
        var keyDelta by mutableIntStateOf(0)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun RetainLogger(name: String) {
            RetainedEffect(name) {
                log("Retain:$name")
                onRetire { log("Retire:$name") }
            }
        }

        compose {
            RetainLogger((keyDelta * 2).toString())
            RetainLogger((keyDelta * 2 + 1).toString())
        }

        assertContentEquals(
            message = "Initial RetainedEffect sequence didn't match",
            expected = listOf("Retain:0", "Retain:1"),
            actual = logHistory,
        )
        keyDelta++
        log("recompose")
        expectChanges()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after recomposing",
            expected =
                listOf(
                    "Retain:0",
                    "Retain:1",
                    "recompose",
                    "Retire:1",
                    "Retire:0",
                    "Retain:2",
                    "Retain:3",
                ),
            actual = logHistory,
        )
    }

    @Test
    fun testRetainEffect_retireWhenRemovedNotKeepingExitedValues() = compositionTest {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun RetainLogger(name: String) {
            RetainedEffect(Unit) {
                log("Retain:$name")
                onRetire { log("Retire:$name") }
            }
        }

        compose {
            if (mount) {
                RetainLogger("1")
                RetainLogger("2")
            }
        }

        assertContentEquals(
            message = "Initial RetainedEffect sequence didn't match",
            expected = listOf("Retain:1", "Retain:2"),
            actual = logHistory,
        )
        mount = false
        log("recompose")
        expectChanges()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after recomposing",
            expected = listOf("Retain:1", "Retain:2", "recompose", "Retire:2", "Retire:1"),
            actual = logHistory,
        )
    }

    @Test
    fun testRetainEffect_retireWhenKeepExitedValuesEnds() = compositionTest {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun RetainLogger(name: String) {
            RetainedEffect(Unit) {
                log("Retain:$name")
                onRetire { log("Retire:$name") }
            }
        }

        val retainScope = ControlledRetainScope()
        compose {
            CompositionLocalProvider(LocalRetainScope provides retainScope) {
                RetainLogger("1")
                if (mount) {
                    RetainLogger("2")
                }
            }
        }

        assertContentEquals(
            message = "Initial RetainedEffect sequence didn't match",
            expected = listOf("Retain:1", "Retain:2"),
            actual = logHistory,
        )
        retainScope.startKeepingExitedValues()
        mount = false
        log("recompose")
        expectChanges()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after recomposing",
            expected = listOf("Retain:1", "Retain:2", "recompose"),
            actual = logHistory,
        )

        retainScope.stopKeepingExitedValues()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after ending retention",
            expected = listOf("Retain:1", "Retain:2", "recompose", "Retire:2"),
            actual = logHistory,
        )
    }

    @Test
    fun testRetainEffect_changeKeyWhenKeepingExitedValues() = compositionTest {
        var key by mutableStateOf("A")

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun RetainLogger(name: String) {
            RetainedEffect(name) {
                log("Retain:$name")
                onRetire { log("Retire:$name") }
            }
        }

        val retainScope = ControlledRetainScope()
        compose {
            CompositionLocalProvider(LocalRetainScope provides retainScope) { RetainLogger(key) }
        }

        assertContentEquals(
            message = "Initial RetainedEffect sequence didn't match",
            expected = listOf("Retain:A"),
            actual = logHistory,
        )
        retainScope.startKeepingExitedValues()
        key = "B"
        log("recompose")
        expectChanges()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after recomposing",
            expected = listOf("Retain:A", "recompose", "Retain:B"),
            actual = logHistory,
        )

        key = "A"
        log("recompose")
        expectChanges()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after recomposing",
            expected = listOf("Retain:A", "recompose", "Retain:B", "recompose"),
            actual = logHistory,
        )

        retainScope.stopKeepingExitedValues()
        assertContentEquals(
            message = "RetainedEffect sequence didn't match after ending retention",
            expected = listOf("Retain:A", "recompose", "Retain:B", "recompose", "Retire:B"),
            actual = logHistory,
        )
    }
}
