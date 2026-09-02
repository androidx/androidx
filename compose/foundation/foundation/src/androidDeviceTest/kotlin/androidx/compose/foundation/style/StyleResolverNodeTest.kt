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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class StyleResolverNodeTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun nodeReuse_recompositionSwapsResolvers() {
        val customColor = stylePropertyOf("customColor") { Color.Black }
        val resolverA = StyleResolver({ customColor.provide(Color.Red) })
        val resolverB = StyleResolver({ customColor.provide(Color.Blue) })

        var resolver by mutableStateOf(resolverA)
        var layoutResolvedColor: Color? = null

        rule.setContent {
            Box(
                Modifier.styleResolver(resolver).layout { measurable, constraints ->
                    layoutResolvedColor = resolver.resolve { customColor.value }
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            )
        }

        rule.runOnIdle {
            assertEquals(Color.Red, layoutResolvedColor)
            assertEquals(Color.Red, resolverA.resolve { customColor.value })
        }

        // Swap out the style resolver for a new instance on the attached node
        resolver = resolverB

        rule.runOnIdle {
            // resolverB should now be bound and resolved
            assertEquals(Color.Blue, layoutResolvedColor)
            assertEquals(Color.Blue, resolverB.resolve { customColor.value })

            // resolverA should have been disposed and unbound
            assertFailsWith<IllegalStateException> { resolverA.resolve { customColor.value } }
        }
    }

    @Test
    fun nodeReuse_styleableModifierUpdatesLayout() {
        val resolverA = StyleResolver(Style { size(50.dp) }.toCommonStyle())
        val resolverB = StyleResolver(Style { size(100.dp) }.toCommonStyle())

        var resolver by mutableStateOf(resolverA)

        rule.setContent { Box(Modifier.testTag("box").styleable(resolver)) }

        rule.onNodeWithTag("box").assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(50.dp)

        // Update to resolverB
        resolver = resolverB

        rule.onNodeWithTag("box").assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(100.dp)
        rule.runOnIdle {
            // resolverA should be disposed
            assertFailsWith<IllegalStateException> { resolverA.resolve {} }
        }
    }

    @Test
    fun nodeReuse_multipleSuccessiveUpdates() {
        val testProp = stylePropertyOf("testProp") { 0 }
        val resolver1 = StyleResolver({ testProp.provide(1) })
        val resolver2 = StyleResolver({ testProp.provide(2) })
        val resolver3 = StyleResolver({ testProp.provide(3) })

        var resolver by mutableStateOf(resolver1)
        var resolvedValue: Int? = null

        rule.setContent {
            Box(
                Modifier.styleResolver(resolver).layout { measurable, constraints ->
                    resolvedValue = resolver.resolve { testProp.value }
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            )
        }

        rule.runOnIdle {
            assertEquals(1, resolvedValue)
            assertEquals(1, resolver1.resolve { testProp.value })
        }

        // 1 -> 2
        resolver = resolver2
        rule.runOnIdle {
            assertEquals(2, resolvedValue)
            assertEquals(2, resolver2.resolve { testProp.value })
            assertFailsWith<IllegalStateException> { resolver1.resolve { testProp.value } }
        }

        // 2 -> 3
        resolver = resolver3
        rule.runOnIdle {
            assertEquals(3, resolvedValue)
            assertEquals(3, resolver3.resolve { testProp.value })
            assertFailsWith<IllegalStateException> { resolver2.resolve { testProp.value } }
        }
    }

    @Test
    fun nodeReuse_sameResolverInstancePreservedOnRecomposition() {
        val testProp = stylePropertyOf("testProp") { "default" }
        val resolver = StyleResolver({ testProp.provide("hello") })

        var counter by mutableStateOf(0)
        var resolvedValue: String? = null

        rule.setContent {
            // Trigger recomposition by reading counter
            @Suppress("UNUSED_VARIABLE") val c = counter
            Box(
                Modifier.styleResolver(resolver).layout { measurable, constraints ->
                    resolvedValue = resolver.resolve { testProp.value }
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            )
        }

        rule.runOnIdle {
            assertEquals("hello", resolvedValue)
            assertEquals("hello", resolver.resolve { testProp.value })
        }

        // Recompose with the same resolver instance
        counter++

        rule.runOnIdle {
            assertEquals("hello", resolvedValue)
            assertEquals("hello", resolver.resolve { testProp.value })
        }
    }

    @Test
    fun nodeReuse_detachingDisposesActiveResolver() {
        val testProp = stylePropertyOf("testProp") { 10 }
        val resolverA = StyleResolver({ testProp.provide(20) })
        val resolverB = StyleResolver({ testProp.provide(30) })

        var resolver by mutableStateOf(resolverA)
        var showContent by mutableStateOf(true)

        rule.setContent {
            if (showContent) {
                Box(Modifier.styleResolver(resolver))
            }
        }

        rule.runOnIdle { assertEquals(20, resolverA.resolve { testProp.value }) }

        // Update to resolverB while attached
        resolver = resolverB

        rule.runOnIdle {
            assertEquals(30, resolverB.resolve { testProp.value })
            assertFailsWith<IllegalStateException> { resolverA.resolve { testProp.value } }
        }

        // Detach node by removing from composition
        showContent = false

        rule.runOnIdle {
            // resolverB should now also be disposed
            assertFailsWith<IllegalStateException> { resolverB.resolve { testProp.value } }
        }
    }
}
