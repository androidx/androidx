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

package androidx.tv.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class, ExperimentalTvMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class CardContainerTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun standardCardContainer_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            StandardCardContainer(
                modifier =
                    Modifier.semantics(mergeDescendants = true) {}
                        .testTag(StandardCardContainerTag),
                imageCard = { interactionSource ->
                    Card(onClick = { count.value += 1 }, interactionSource = interactionSource) {
                        SampleImage()
                    }
                },
                title = { Text("${count.value}") }
            )
        }

        rule
            .onNodeWithTag(StandardCardContainerTag)
            .onChild()
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .requestFocus()
            .assertIsEnabled()

        rule
            .onNodeWithTag(StandardCardContainerTag)
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun standardCardContainer_clickAction() {
        val count = mutableStateOf(0f)
        rule.setContent {
            StandardCardContainer(
                modifier =
                    Modifier.semantics(mergeDescendants = true) {}
                        .testTag(StandardCardContainerTag),
                imageCard = { interactionSource ->
                    Card(onClick = { count.value += 1 }, interactionSource = interactionSource) {
                        SampleImage()
                    }
                },
                title = { Text("${count.value}") }
            )
        }

        rule.onNodeWithTag(StandardCardContainerTag).onChild().requestFocus().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        Truth.assertThat(count.value).isEqualTo(1)

        rule
            .onNodeWithTag(StandardCardContainerTag)
            .onChild()
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun wideCardContainer_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            WideCardContainer(
                modifier =
                    Modifier.semantics(mergeDescendants = true) {}.testTag(WideCardContainerTag),
                imageCard = { interactionSource ->
                    Card(onClick = { count.value += 1 }, interactionSource = interactionSource) {
                        SampleImage()
                    }
                },
                title = { Text("${count.value}") }
            )
        }

        rule
            .onNodeWithTag(WideCardContainerTag)
            .onChild()
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .requestFocus()
            .assertIsEnabled()

        rule
            .onNodeWithTag(WideCardContainerTag)
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun wideCardContainer_clickAction() {
        val count = mutableStateOf(0f)
        rule.setContent {
            WideCardContainer(
                modifier =
                    Modifier.semantics(mergeDescendants = true) {}.testTag(WideCardContainerTag),
                imageCard = { interactionSource ->
                    Card(onClick = { count.value += 1 }, interactionSource = interactionSource) {
                        SampleImage()
                    }
                },
                title = { Text("${count.value}") }
            )
        }

        rule.onNodeWithTag(WideCardContainerTag).onChild().requestFocus().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        Truth.assertThat(count.value).isEqualTo(1)

        rule
            .onNodeWithTag(WideCardContainerTag)
            .onChild()
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Composable
    fun SampleImage() {
        Box(Modifier.size(180.dp, 150.dp).testTag(SampleImageTag))
    }
}

private const val StandardCardContainerTag = "standard-card-container"
private const val WideCardContainerTag = "wide-card-container"

private const val SampleImageTag = "sample-image"
