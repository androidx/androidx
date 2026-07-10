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

package androidx.glance.appwidget.testing.unit.componenttests

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.Action
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.Column
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.hasAnyDescendant
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.text.Text
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val arbitraryText = "some text"
private const val buttonTestTag = "button-under-test"
private const val columnTestTag = "column-under-test"

private val anImageProvider = ImageProvider(uri = Uri.parse("example.com"))
private const val contentDescription = "a content description"

/**
 * Tests that the unit testing framework runs correctly on Glance's components. The intent is that
 * changes to the components should not break existing unit tests.
 *
 * TODO: add more tests for more components 480199909. Also, for each button test case, ensure that
 *   the behavior is tested for the base button, TextButton, and IconButton
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ButtonsGlanceTest {

    /**
     * Test that the M3 buttons and the basic button behave in the same way. They have different
     * implementations internally.
     */
    @Test
    fun translateButton_hasCorrectText() = runGlanceAppWidgetUnitTest {
        val buttonText = "Button Text"
        val m3ButtonText = "M3 Btn Text"
        provideComposable {
            Column() {
                FilledButton(text = m3ButtonText, onClick = {})
                Button(buttonText, onClick = {})
            }
        }

        onNode(hasText(m3ButtonText)).assertExists()
        onNode(hasText(buttonText)).assertExists()
    }

    @Test
    fun button_hasClick() = runGlanceAppWidgetUnitTest {
        provideComposable { TestTextButton() }

        onNode(hasTestTag(buttonTestTag)).assertHasClickAction()
    }

    @Test
    fun m3FilledButton_hasClick() = runGlanceAppWidgetUnitTest {
        provideComposable { TestTextButton() }
        onNode(hasTestTag(buttonTestTag)).assertHasClickAction()
    }

    @Test
    fun m3CircleIconButton_hasClick() = runGlanceAppWidgetUnitTest {
        provideComposable { TestCircleIconButton() }
        onNode(hasTestTag(buttonTestTag)).assertHasClickAction()
    }

    @Test
    fun m3SquareIconButton_hasClick() = runGlanceAppWidgetUnitTest {
        provideComposable { TestSquareIconButton() }
        onNode(hasTestTag(buttonTestTag)).assertHasClickAction()
    }

    @Test
    fun m3FilledButton_hasStartActivityClickAction() = runGlanceAppWidgetUnitTest {
        val (action, intent) = startActivityAction()

        provideComposable { TestStartActivityButton(action) }

        onNode(hasText(arbitraryText)).assertExists()
        onAllNodes((hasText(arbitraryText)))
            .filter(hasStartActivityClickAction(intent))
            .assertCountEquals(1)
    }

    @Test
    fun m3IconButton_hasStartActivityClickAction() = runGlanceAppWidgetUnitTest {
        val (action, intent) = startActivityAction()
        provideComposable { TestStartActivityIconButton(action) }

        onAllNodes(hasTestTag(buttonTestTag))
            .filter(hasStartActivityClickAction(intent))
            .assertCountEquals(1)
    }

    @Test
    fun hasAnyDescendent_columnAndText() = runGlanceAppWidgetUnitTest {
        provideComposable {
            Column(GlanceModifier.semantics { testTag = columnTestTag }) { Text(arbitraryText) }
        }

        onAllNodes(hasAnyDescendant(hasText(arbitraryText)))
            .filter(hasTestTag(columnTestTag))
            .assertCountEquals(1)
    }

    @Test
    fun hasAnyDescendent_columnAndM3FilledButton() = runGlanceAppWidgetUnitTest {
        val (action, intent) = startActivityAction()

        provideComposable {
            Column(GlanceModifier.semantics { testTag = columnTestTag }) {
                FilledButton(arbitraryText, onClick = action)
            }
        }

        onAllNodes(hasAnyDescendant(hasText(arbitraryText)))
            .filter(hasTestTag(columnTestTag))
            .assertCountEquals(1)
    }

    @Test
    fun hasContentDescription_iconButton() = runGlanceAppWidgetUnitTest {
        provideComposable {
            CircleIconButton(
                imageProvider = anImageProvider,
                contentDescription = arbitraryText,
                onClick = {},
            )
        }

        onNode(hasContentDescription(arbitraryText)).assertExists()
    }

    @Test
    fun hasContentDescription_image() = runGlanceAppWidgetUnitTest {
        provideComposable { Image(provider = anImageProvider, contentDescription = arbitraryText) }

        onNode(hasContentDescription(arbitraryText)).assertExists()
    }
}

private fun startActivityAction(): Pair<Action, Intent> {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
    return actionStartActivity(intent = intent) to intent
}

@Composable
private fun TestTextButton() =
    FilledButton(
        arbitraryText,
        onClick = {},
        modifier = GlanceModifier.semantics { testTag = buttonTestTag },
    )

@Composable
private fun TestCircleIconButton() =
    CircleIconButton(
        imageProvider = anImageProvider,
        contentDescription = contentDescription,
        onClick = {},
        modifier = GlanceModifier.semantics { testTag = buttonTestTag },
    )

@Composable
private fun TestSquareIconButton() =
    SquareIconButton(
        imageProvider = anImageProvider,
        contentDescription = contentDescription,
        onClick = {},
        modifier = GlanceModifier.semantics { testTag = buttonTestTag },
    )

@Composable
private fun TestStartActivityButton(startActivityAction: Action) {
    Column {
        FilledButton(
            arbitraryText,
            onClick = startActivityAction,
            modifier = GlanceModifier.semantics { testTag = buttonTestTag },
        )
    }
}

@Composable
private fun TestStartActivityIconButton(startActivityAction: Action) {
    Column {
        CircleIconButton(
            imageProvider = anImageProvider,
            contentDescription = contentDescription,
            onClick = startActivityAction,
            modifier = GlanceModifier.semantics { testTag = buttonTestTag },
        )
    }
}
