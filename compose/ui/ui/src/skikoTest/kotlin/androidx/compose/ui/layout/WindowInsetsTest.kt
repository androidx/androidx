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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class WindowInsetsTest {

    @Test
    fun composableDoesNotRecomposeOnWindowInsetsImeChange() = runSkikoComposeUiTest {
        var compositionCount = 0
        val imeInsets = mutableStateOf(PlatformInsets.Zero)
        var initialTextFieldBounds: Rect
        var textFieldBounds = Rect.Zero

        setContent {
            TestContent(
                TestWindowInsets(imeInsets),
                { compositionCount++ },
                { textFieldBounds = it }
            )
        }

        initialTextFieldBounds = textFieldBounds

        assertNotEquals(Rect.Zero, textFieldBounds)
        assertEquals(1, compositionCount)

        imeInsets.value = PlatformInsets(bottom = 100)
        waitForIdle()

        assertEquals(Rect(initialTextFieldBounds.left, initialTextFieldBounds.top - 100, initialTextFieldBounds.right, initialTextFieldBounds.bottom - 100), textFieldBounds)
        assertEquals(1, compositionCount)
    }

    @Test
    fun composableDoesNotRecomposeOnWindowInsetsImeConsecutiveChange() = runSkikoComposeUiTest {
        var compositionCount = 0
        val imeInsets = mutableStateOf(PlatformInsets.Zero)
        val maxBottomInset = 100
        var initialTextFieldBounds: Rect
        var textFieldBounds = Rect.Zero

        setContent {
            TestContent(
                TestWindowInsets(imeInsets),
                { compositionCount++ },
                { textFieldBounds = it }
            )
        }

        initialTextFieldBounds = textFieldBounds

        assertNotEquals(Rect.Zero, textFieldBounds)
        assertEquals(1, compositionCount)

        for (i in 1..maxBottomInset) {
            imeInsets.value = PlatformInsets(bottom = i)
            waitForIdle()
        }

        assertEquals(Rect(initialTextFieldBounds.left, initialTextFieldBounds.top - maxBottomInset, initialTextFieldBounds.right, initialTextFieldBounds.bottom - maxBottomInset), textFieldBounds)
        assertEquals(1, compositionCount)
    }

    @Test
    fun composableDoesNotRecomposeOnWindowInsetsImeChangeAndReset() = runSkikoComposeUiTest {
        var compositionCount = 0
        val imeInsets = mutableStateOf(PlatformInsets.Zero)
        val maxBottomInset = 100
        var initialTextFieldBounds: Rect
        var textFieldBounds = Rect.Zero

        setContent {
            TestContent(
                TestWindowInsets(imeInsets),
                { compositionCount++ },
                { textFieldBounds = it }
            )
        }

        initialTextFieldBounds = textFieldBounds

        assertNotEquals(Rect.Zero, textFieldBounds)
        assertEquals(1, compositionCount)

        for (i in 1..maxBottomInset) {
            imeInsets.value = PlatformInsets(bottom = i)
            waitForIdle()
        }

        for (i in 1..maxBottomInset) {
            imeInsets.value = PlatformInsets(bottom = maxBottomInset - i)
            waitForIdle()
        }

        assertEquals(initialTextFieldBounds, textFieldBounds)
        assertEquals(1, compositionCount)
    }

    /**
     * Demonstrates that a non-optimized implementation of PlatformWindowInsets causes
     * recomposition when inset values change. Uses a simple object implementation that directly returns the state value without
     * using lambda getters. This shows the performance impact of not using the optimized approach.
     */
    @Test
    fun testDirectStateAccessCausesRecompositionOnInsetsChange() = runSkikoComposeUiTest {
        var compositionCount = 0
        val imeInsets = mutableStateOf(PlatformInsets.Zero)
        var initialTextFieldBounds: Rect
        var textFieldBounds = Rect.Zero

        setContent {
            TestContent(
                object : PlatformWindowInsets {
                    override val ime: PlatformInsets get() = imeInsets.value
                },
                { compositionCount++ },
                { textFieldBounds = it }
            )
        }

        initialTextFieldBounds = textFieldBounds

        imeInsets.value = PlatformInsets(bottom = 100)
        waitForIdle()

        assertEquals(Rect(initialTextFieldBounds.left, initialTextFieldBounds.top - 100, initialTextFieldBounds.right, initialTextFieldBounds.bottom - 100), textFieldBounds)
        assertEquals(2, compositionCount)
    }
}

@Composable
private fun TestContent(
    insets: PlatformWindowInsets,
    onComposition: () -> Unit,
    onTextFieldPositioned: (Rect) -> Unit
) {
    CompositionLocalProvider(LocalPlatformWindowInsets provides insets) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()){
            Spacer(modifier = Modifier.weight(1f))
            TextField(
                value ="",
                onValueChange = {},
                modifier = Modifier.onGloballyPositioned { onTextFieldPositioned(it.boundsInRoot()) }
            )
            onComposition()
        }
    }
}

private fun TestWindowInsets(
    imeInsets: MutableState<PlatformInsets>
): PlatformWindowInsets = object : PlatformWindowInsets {
    override val ime: PlatformInsets get() = PlatformInsets(
        getBottom = { imeInsets.value.bottom },
        getTop = { imeInsets.value.top },
        getLeft = { imeInsets.value.left },
        getRight = { imeInsets.value.right }
    )
}