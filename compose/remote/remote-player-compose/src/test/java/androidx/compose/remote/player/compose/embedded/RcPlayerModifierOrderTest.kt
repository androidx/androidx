/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerModifierOrderTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private lateinit var remoteContext: AndroidRemoteContext

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        remoteContext = AndroidRemoteContext().apply { setAndroidContext(context) }
    }

    @Test
    fun testDrawBeforePadding() {
        val componentModifiers = ComponentModifiers()
        // Draw content BEFORE padding in the input list
        componentModifiers.add(DrawContentOperation())
        componentModifiers.add(PaddingModifierOperation(10f, 10f, 10f, 10f))

        var resolvedModifier: Modifier = Modifier

        rule.setContent {
            CompositionLocalProvider(LocalRemoteContext provides remoteContext) {
                resolvedModifier = componentModifiers.toModifier(drawOpsList = emptyList())
            }
        }

        val elements = resolvedModifier.toElementList()

        // We expect DrawWithContentElement (or similar) to be before PaddingElement
        assertEquals(2, elements.size)
        assert(elements[0].javaClass.name.contains("DrawWithContent")) {
            "Expected DrawWithContent modifier first, got ${elements[0].javaClass.name}"
        }
        assert(elements[1].javaClass.name.contains("Padding")) {
            "Expected Padding modifier second, got ${elements[1].javaClass.name}"
        }
    }

    @Test
    fun testPaddingBeforeDraw() {
        val componentModifiers = ComponentModifiers()
        // Padding BEFORE draw content in the input list
        componentModifiers.add(PaddingModifierOperation(10f, 10f, 10f, 10f))
        componentModifiers.add(DrawContentOperation())

        var resolvedModifier: Modifier = Modifier

        rule.setContent {
            CompositionLocalProvider(LocalRemoteContext provides remoteContext) {
                resolvedModifier = componentModifiers.toModifier(drawOpsList = emptyList())
            }
        }

        val elements = resolvedModifier.toElementList()

        // We expect PaddingElement to be before DrawWithContentElement
        assertEquals(2, elements.size)
        assert(elements[0].javaClass.name.contains("Padding")) {
            "Expected Padding modifier first, got ${elements[0].javaClass.name}"
        }
        assert(elements[1].javaClass.name.contains("DrawWithContent")) {
            "Expected DrawWithContent modifier second, got ${elements[1].javaClass.name}"
        }
    }

    @Test
    fun testDrawContentProcessedOnce() {
        val componentModifiers = ComponentModifiers()
        componentModifiers.add(PaddingModifierOperation(10f, 10f, 10f, 10f))

        var resolvedModifier: Modifier = Modifier

        rule.setContent {
            CompositionLocalProvider(LocalRemoteContext provides remoteContext) {
                resolvedModifier = componentModifiers.toModifier(drawOpsList = listOf())
            }
        }

        val elements = resolvedModifier.toElementList()
        val drawElements = elements.filter { it.javaClass.name.contains("DrawWithContent") }
        assertEquals(1, drawElements.size)
    }

    @Test
    fun testRoundedClipHoistedBeforeDrawContent() {
        val componentModifiers = ComponentModifiers()
        // DrawContent precedes RoundedClipRect in wire modifier list
        componentModifiers.add(DrawContentOperation())
        componentModifiers.add(RoundedClipRectModifierOperation(10f, 10f, 10f, 10f))

        var resolvedModifier: Modifier = Modifier

        rule.setContent {
            CompositionLocalProvider(
                LocalRemoteContext provides remoteContext,
                LocalCoreDocument provides CoreDocument(),
            ) {
                resolvedModifier = componentModifiers.toModifier(drawOpsList = emptyList())
            }
        }

        val elements = resolvedModifier.toElementList()
        // Clip modifier must be before DrawWithContent modifier so it clips the background
        assertEquals(2, elements.size)
        assert(
            elements[0].javaClass.name.contains("GraphicsLayer") ||
                elements[0].javaClass.name.contains("Clip")
        ) {
            "Expected Clip/GraphicsLayer modifier first, got ${elements[0].javaClass.name}"
        }
        assert(elements[1].javaClass.name.contains("DrawWithContent")) {
            "Expected DrawWithContent modifier second, got ${elements[1].javaClass.name}"
        }
    }

    private fun Modifier.toElementList(): List<Modifier.Element> {
        val list = mutableListOf<Modifier.Element>()
        foldIn(list) { acc, element ->
            acc.add(element)
            acc
        }
        return list
    }
}
