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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class ToCharArrayTest {

    private val sources = listOf(
        "hello",
        TextFieldCharSequence("hello"),
        CustomCharSequence("hello"),
    )

    private val dest = CharArray(10)

    @Test
    fun toCharArray_invalidSourceStartIndex() {
        sources.forEach { source ->
            assertFails {
                source.toCharArray(dest, 0, source.length + 1, source.length + 2)
            }
        }
    }

    @Test
    fun toCharArray_invalidSourceEndIndex() {
        sources.forEach { source ->
            assertFails {
                source.toCharArray(dest, 0, 0, source.length + 1)
            }
        }
    }

    @Test
    fun toCharArray_invalidDestStartIndex() {
        sources.forEach { source ->
            assertFails {
                source.toCharArray(dest, dest.size + 1, 0, 1)
            }
        }
    }

    @Test
    fun toCharArray_invalidDestEndIndex() {
        sources.forEach { source ->
            assertFails {
                source.toCharArray(dest, dest.size, 0, 1)
            }
        }
    }

    @Test
    fun toCharArray_copiesChars_toStartOfDest() {
        sources.forEach { source ->
            dest.fill(Char(0))
            source.toCharArray(dest, 0, 0, source.length)
            assertThat(dest).asList().containsExactly(
                'h', 'e', 'l', 'l', 'o', Char(0), Char(0), Char(0), Char(0), Char(0)
            ).inOrder()
        }
    }

    @Test
    fun toCharArray_copiesChars_toEndOfDest() {
        sources.forEach { source ->
            dest.fill(Char(0))
            source.toCharArray(dest, 5, 0, source.length)
            assertThat(dest).asList().containsExactly(
                Char(0), Char(0), Char(0), Char(0), Char(0), 'h', 'e', 'l', 'l', 'o'
            ).inOrder()
        }
    }

    private class CustomCharSequence(text: String) : CharSequence by text
}
