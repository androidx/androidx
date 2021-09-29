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

package androidx.glance.layout

import androidx.glance.Modifier
import androidx.glance.findModifier
import androidx.glance.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TextTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createComposableText() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Text("text")
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableText::class.java)
        assertThat((root.children[0] as EmittableText).text).isEqualTo("text")
    }

    @Test
    fun createComposableTextWithStyle() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Text(
                "text",
                style = TextStyle(
                    20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableText::class.java)
        val text = root.children[0] as EmittableText
        assertThat(text.text).isEqualTo("text")
        assertThat(text.style)
            .isEqualTo(
                TextStyle(
                    20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )
            )
    }

    @Test
    fun createComposableTextWithModifiers() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            Text("text", modifier = Modifier.expandWidth())
        }

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(EmittableText::class.java)
        assertThat(root.children[0].modifier.findModifier<WidthModifier>()?.width)
            .isEqualTo(Dimension.Expand)
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
}