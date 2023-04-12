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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalTvMaterial3Api::class
)
@LargeTest
@RunWith(AndroidJUnit4::class)
class CardTest {
    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun card_customShapeAndColorIsUsed() {
        val shape = CutCornerShape(8.dp)
        val background = Color.Yellow
        val cardColor = Color.Blue
        rule.setContent {
            Box(modifier = Modifier.background(background)) {
                Card(
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {}
                        .testTag(CardTag),
                    shape = CardDefaults.shape(shape = shape),
                    colors = CardDefaults.colors(containerColor = cardColor),
                    onClick = {}
                ) {
                    Box(Modifier.size(50.dp, 50.dp))
                }
            }
        }

        rule
            .onNodeWithTag(CardTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = cardColor,
                backgroundColor = background,
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun card_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            Card(
                modifier = Modifier.testTag(CardTag),
                onClick = { count.value += 1 },
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.dp))
            }
        }

        rule.onNodeWithTag(CardTag)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsEnabled()
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun card_clickAction() {
        val count = mutableStateOf(0f)
        rule.setContent {
            Card(
                modifier = Modifier.testTag(CardTag),
                onClick = { count.value += 1 },
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.dp))
            }
        }

        rule.onNodeWithTag(CardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag(CardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun card_interactionSource() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Card(
                onClick = {},
                modifier = Modifier.testTag(CardTag),
                interactionSource = interactionSource
            ) {
                Spacer(Modifier.size(30.dp))
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { Truth.assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(CardTag).performSemanticsAction(SemanticsActions.RequestFocus)

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.onNodeWithTag(CardTag).performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(3)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            Truth.assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun classicCard_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            ClassicCard(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag(ClassicCardTag),
                image = { SampleImage() },
                title = { Text("${count.value}") },
                onClick = { count.value += 1 }
            )
        }

        rule.onNodeWithTag(ClassicCardTag)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsEnabled()
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun classicCard_clickAction() {
        val count = mutableStateOf(0f)
        rule.setContent {
            ClassicCard(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag(ClassicCardTag),
                image = { SampleImage() },
                title = { Text("${count.value}") },
                onClick = { count.value += 1 }
            )
        }

        rule.onNodeWithTag(ClassicCardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag(ClassicCardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun classicCard_contentPadding() {
        val contentPadding = PaddingValues(8.dp, 10.dp, 12.dp, 14.dp)
        val cardTitleTag = "classic_card_title"

        rule.setContent {
            ClassicCard(
                modifier = Modifier.testTag(ClassicCardTag),
                image = { SampleImage() },
                title = {
                    Text(
                        text = "Classic Card",
                        modifier = Modifier.testTag(cardTitleTag)
                    )
                },
                onClick = {},
                contentPadding = contentPadding
            )
        }

        val cardBounds = rule
            .onNodeWithTag(ClassicCardTag)
            .getUnclippedBoundsInRoot()

        val imageBounds = rule
            .onNodeWithTag(SampleImageTag, true)
            .getUnclippedBoundsInRoot()

        val titleBounds = rule
            .onNodeWithTag(cardTitleTag, true)
            .getUnclippedBoundsInRoot()

        // Check top padding
        (imageBounds.top - cardBounds.top).assertIsEqualTo(
            10.dp,
            "padding between top of the image and top of the card."
        )

        // Check bottom padding
        (cardBounds.bottom - titleBounds.bottom).assertIsEqualTo(
            14.dp,
            "padding between bottom of the text and bottom of the card."
        )

        // Check start padding
        (imageBounds.left - cardBounds.left).assertIsEqualTo(
            8.dp,
            "padding between left of the image and left of the card."
        )

        // Check end padding
        (cardBounds.right - imageBounds.right).assertIsEqualTo(
            12.dp,
            "padding between right of the text and right of the card."
        )
    }

    @Test
    fun compactCard_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            CompactCard(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag(CompactCardTag),
                image = { SampleImage() },
                title = { Text("${count.value}") },
                onClick = { count.value += 1 }
            )
        }

        rule.onNodeWithTag(CompactCardTag)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsEnabled()
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun compactCard_clickAction() {
        val count = mutableStateOf(0f)
        rule.setContent {
            CompactCard(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag(CompactCardTag),
                image = { SampleImage() },
                title = { Text("${count.value}") },
                onClick = { count.value += 1 }
            )
        }

        rule.onNodeWithTag(CompactCardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag(CompactCardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun wideClassicCard_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            WideClassicCard(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag(WideClassicCardTag),
                image = { SampleImage() },
                title = { Text("${count.value}") },
                onClick = { count.value += 1 }
            )
        }

        rule.onNodeWithTag(WideClassicCardTag)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsEnabled()
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun wideClassicCard_clickAction() {
        val count = mutableStateOf(0f)
        rule.setContent {
            WideClassicCard(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag(WideClassicCardTag),
                image = { SampleImage() },
                title = { Text("${count.value}") },
                onClick = { count.value += 1 }
            )
        }

        rule.onNodeWithTag(WideClassicCardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag(WideClassicCardTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun wideClassicCard_contentPadding() {
        val contentPadding = PaddingValues(8.dp, 10.dp, 12.dp, 14.dp)
        val cardTitleTag = "wide_classic_card_title"

        rule.setContent {
            WideClassicCard(
                modifier = Modifier.testTag(WideClassicCardTag),
                image = { SampleImage() },
                title = {
                    Text(
                        text = "Wide Classic Card",
                        modifier = Modifier.testTag(cardTitleTag)
                    )
                },
                onClick = {},
                contentPadding = contentPadding
            )
        }

        val cardBounds = rule
            .onNodeWithTag(WideClassicCardTag)
            .getUnclippedBoundsInRoot()

        val imageBounds = rule
            .onNodeWithTag(SampleImageTag, true)
            .getUnclippedBoundsInRoot()

        val titleBounds = rule
            .onNodeWithTag(cardTitleTag, true)
            .getUnclippedBoundsInRoot()

        // Check top padding
        (imageBounds.top - cardBounds.top).assertIsEqualTo(
            10.dp,
            "padding between top of the image and top of the card."
        )

        // Check bottom padding
        (cardBounds.bottom - imageBounds.bottom).assertIsEqualTo(
            14.dp,
            "padding between bottom of the text and bottom of the card."
        )

        // Check start padding
        (imageBounds.left - cardBounds.left).assertIsEqualTo(
            8.dp,
            "padding between left of the image and left of the card."
        )

        // Check end padding
        (cardBounds.right - titleBounds.right).assertIsEqualTo(
            12.dp,
            "padding between right of the text and right of the card."
        )
    }

    @Composable
    fun SampleImage() {
        Box(
            Modifier
                .size(180.dp, 150.dp)
                .testTag(SampleImageTag)
        )
    }
}

private const val CardTag = "card"
private const val CompactCardTag = "compact-card"
private const val ClassicCardTag = "classic-card"
private const val WideClassicCardTag = "wide-classic-card"

private const val SampleImageTag = "sample-image"