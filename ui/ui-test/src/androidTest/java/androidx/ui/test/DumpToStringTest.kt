/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.semantics.Semantics
import androidx.ui.test.util.obfuscateNodesInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DumpToStringTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun dumpToString_nothingFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        val result = findByText("Oops")
            .dumpToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "There were 0 nodes found!")
    }

    @Test
    fun dumpToString_one() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        val result = findByText("Hello")
            .dumpToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- AccessibilityLabel = 'Hello'\n" +
                "- Boundary = 'true'")
    }

    @Test
    fun dumpToString_many() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        val result = findAll(SemanticsPredicate.any)
            .dumpToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "1) Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- Boundary = 'true'\n" +
                "2) Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- AccessibilityLabel = 'Hello'\n" +
                "- Boundary = 'true'\n" +
                "3) Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- AccessibilityLabel = 'World'\n" +
                "- Boundary = 'true'")
    }

    @Composable
    fun ComposeSimpleCase() {
        MaterialTheme {
            Column {
                Semantics(container = true) {
                    Text("Hello")
                }
                Semantics(container = true) {
                    Text("World")
                }
            }
        }
    }
}