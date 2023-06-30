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

package androidx.wear.compose.material3

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ListHeaderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            ListHeader(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Header")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun listHeader_has_semantic_heading_property() {
        rule.setContentWithTheme {
            ListHeader(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Header")
            }
        }

        rule.onNode(isHeading())
    }

    @Test
    fun listSubheader_has_semantic_heading_property() {
        rule.setContentWithTheme {
            ListSubheader(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Subheader")
            }
        }

        rule.onNode(isHeading())
    }

    @Test
    fun gives_listHeader_correct_text_style() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.buttonMedium
            ListHeader {
                actualTextStyle = LocalTextStyle.current
            }
        }

        Assert.assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_listSubheader_correct_text_style() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.captionLarge
            ListSubheader {
                actualTextStyle = LocalTextStyle.current
            }
        }

        Assert.assertEquals(expectedTextStyle, actualTextStyle)
    }
}