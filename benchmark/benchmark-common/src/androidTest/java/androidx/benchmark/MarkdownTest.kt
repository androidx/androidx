/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MarkdownTest {
    @Test
    fun test_file_link_basic() =
        test_file_link(label = "abc", path = "/d/e/f", expected = "[abc](file:///d/e/f)")

    @Test
    fun test_file_link_escaping() {
        val expected = "[abc \\[tag\\] (1)](file:///d/e \\(1\\)/f [2])"

        // needs escaping
        test_file_link(label = "abc [tag] (1)", path = "/d/e (1)/f [2]", expected = expected)

        // already escaped
        test_file_link(
            label = "abc \\[tag\\] (1)",
            path = "/d/e \\(1\\)/f [2]",
            expected = expected
        )
    }

    private fun test_file_link(label: String, path: String, expected: String) {
        val actual = Markdown.createFileLink(label, path)
        assertThat(actual).isEqualTo(expected)
    }
}
