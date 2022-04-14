/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.text

import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.runTestingComposition
import androidx.glance.unit.Dimension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class TextTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun createComposableText() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Text("text")
        }

        assertThat(root.children).hasSize(1)
        val text = assertIs<EmittableText>(root.children[0])
        assertThat(text.text).isEqualTo("text")
    }

    @Test
    fun createComposableTextWithStyle() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Text(
                "text",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        assertThat(root.children).hasSize(1)
        val text = assertIs<EmittableText>(root.children[0])
        assertThat(text.text).isEqualTo("text")
        assertThat(text.style)
            .isEqualTo(
                TextStyle(
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )
            )
    }

    @Test
    fun createComposableTextWithModifiers() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Text("text", modifier = GlanceModifier.fillMaxWidth())
        }

        assertThat(root.children).hasSize(1)
        val text = assertIs<EmittableText>(root.children[0])
        assertThat(text.modifier.findModifier<WidthModifier>()?.width)
            .isEqualTo(Dimension.Fill)
    }

    @Test
    fun createComposableTextWithMaxLines() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
          Text("text", maxLines = 3)
        }

        assertThat(root.children).hasSize(1)
        val text = assertIs<EmittableText>(root.children[0])
        assertThat(text.maxLines).isEqualTo(3)
    }

    @Test
    fun textDecoration_plus() {
        val combined = TextDecoration.LineThrough + TextDecoration.Underline
        assertThat(TextDecoration.LineThrough in combined).isTrue()
        assertThat(TextDecoration.Underline in combined).isTrue()
    }

    @Test
    fun textDecoration_combine() {
        val combined =
            TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
        assertThat(TextDecoration.LineThrough in combined).isTrue()
        assertThat(TextDecoration.Underline in combined).isTrue()
    }

    @Test
    fun textAlign() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            Text("text", style = TextStyle(textAlign = TextAlign.Center))
        }

        assertThat(root.children).hasSize(1)
        val child = assertIs<EmittableText>(root.children[0])
        assertThat(child.style?.textAlign).isEqualTo(TextAlign.Center)
    }
}