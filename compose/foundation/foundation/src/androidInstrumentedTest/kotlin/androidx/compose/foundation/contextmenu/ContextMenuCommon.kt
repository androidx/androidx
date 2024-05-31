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

package androidx.compose.foundation.contextmenu

import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Correspondence
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.IterableSubject
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat

internal fun ContextMenuState.open(offset: Offset = Offset.Zero) {
    status = Status.Open(offset)
}

internal fun ContextMenuScope.testItem(
    label: String = "Item",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable ((iconColor: Color) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    item(
        label = { label },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        onClick = onClick,
    )
}

internal fun assertThatContextMenuState(state: ContextMenuState): ContextMenuStateSubject =
    assertAbout(ContextMenuStateSubject.SUBJECT_FACTORY).that(state)!!

internal class ContextMenuStateSubject
internal constructor(failureMetadata: FailureMetadata?, private val subject: ContextMenuState) :
    Subject(failureMetadata, subject) {
    companion object {
        internal val SUBJECT_FACTORY: Factory<ContextMenuStateSubject?, ContextMenuState> =
            Factory { failureMetadata, subject ->
                ContextMenuStateSubject(failureMetadata, subject)
            }
    }

    fun statusIsOpen() {
        assertThat(subject.status).isInstanceOf(Status.Open::class.java)
    }

    fun statusIsClosed() {
        assertThat(subject.status).isInstanceOf(Status.Closed::class.java)
    }
}

internal fun assertThatColors(
    colors: Set<Color>,
    tolerance: Double = 0.02,
): IterableSubject.UsingCorrespondence<Color, Color> =
    assertThat(colors).comparingElementsUsing(colorCorrespondence(tolerance))

internal fun colorCorrespondence(tolerance: Double = 0.02): Correspondence<Color, Color> {
    val floatingPointCorrespondence = Correspondence.tolerance(tolerance)
    return Correspondence.from(
        { actual: Color?, expected: Color? ->
            if (expected == null || actual == null) return@from actual == expected
            floatingPointCorrespondence.compare(actual.red, expected.red) &&
                floatingPointCorrespondence.compare(actual.green, expected.green) &&
                floatingPointCorrespondence.compare(actual.blue, expected.blue) &&
                floatingPointCorrespondence.compare(actual.alpha, expected.alpha)
        },
        /* description = */ "equals",
    )
}

internal fun assertThatColor(actual: Color): ColorSubject =
    assertAbout(ColorSubject.INSTANCE).that(actual)

internal class ColorSubject(
    failureMetadata: FailureMetadata?,
    private val subject: Color,
) : Subject(failureMetadata, subject) {
    companion object {
        val INSTANCE =
            Factory<ColorSubject, Color> { failureMetadata, subject ->
                ColorSubject(failureMetadata, subject)
            }
    }

    fun isFuzzyEqualTo(expected: Color, tolerance: Float = 0.001f) {
        try {
            assertThat(subject.red).isWithin(tolerance).of(expected.red)
            assertThat(subject.green).isWithin(tolerance).of(expected.green)
            assertThat(subject.blue).isWithin(tolerance).of(expected.blue)
            assertThat(subject.alpha).isWithin(tolerance).of(expected.alpha)
        } catch (e: AssertionError) {
            failWithActual(
                Fact.simpleFact("Colors are not equal."),
                Fact.fact("expected", expected.toString()),
                Fact.fact("with tolerance", tolerance),
            )
        }
    }
}

/**
 * In order to trigger `Popup.onDismiss`, the click has to come from above compose's test framework.
 * This method will send the click in this way.
 *
 * @param offsetPicker Given the root rect bounds, select an offset to click at.
 */
internal fun ComposeTestRule.clickOffPopup(offsetPicker: (IntRect) -> IntOffset) {
    // Need the click to register above Compose's test framework,
    // else it won't be directed to the popup properly. So,
    // we use a different way of dispatching the click.
    val rootRect =
        with(density) { onAllNodes(isRoot()).onFirst().getBoundsInRoot().toRect().roundToIntRect() }
    val offset = offsetPicker(rootRect)
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(offset.x, offset.y)
    waitForIdle()
}

internal object ContextMenuItemLabels {
    internal const val CUT = "Cut"
    internal const val COPY = "Copy"
    internal const val PASTE = "Paste"
    internal const val SELECT_ALL = "Select all"
}

internal fun ComposeTestRule.contextMenuItemInteraction(
    label: String,
): SemanticsNodeInteraction = onNode(matcher = hasAnyAncestor(isPopup()) and hasText(label))

internal enum class ContextMenuItemState {
    ENABLED,
    DISABLED,
    DOES_NOT_EXIST
}

/**
 * Various asserts for checking enable/disable status of the context menu. Always checks that the
 * popup exists and that all the items exist. Each boolean parameter represents whether the item is
 * expected to be enabled or not.
 */
internal fun ComposeTestRule.assertContextMenuItems(
    cutState: ContextMenuItemState,
    copyState: ContextMenuItemState,
    pasteState: ContextMenuItemState,
    selectAllState: ContextMenuItemState,
) {
    val contextMenuInteraction = onNode(isPopup())
    contextMenuInteraction.assertExists("Context Menu should exist.")

    assertContextMenuItem(label = ContextMenuItemLabels.CUT, state = cutState)
    assertContextMenuItem(label = ContextMenuItemLabels.COPY, state = copyState)
    assertContextMenuItem(label = ContextMenuItemLabels.PASTE, state = pasteState)
    assertContextMenuItem(label = ContextMenuItemLabels.SELECT_ALL, state = selectAllState)
}

private fun ComposeTestRule.assertContextMenuItem(label: String, state: ContextMenuItemState) {
    // Note: this test assumes the text and the row have been merged in the semantics tree.
    contextMenuItemInteraction(label).run {
        if (state == ContextMenuItemState.DOES_NOT_EXIST) {
            assertDoesNotExist()
        } else {
            assertExists(errorMessageOnFail = """Couldn't find label "$label"""")
            assertIsDisplayed()
            assertHasClickAction()
            if (state == ContextMenuItemState.ENABLED) assertIsEnabled() else assertIsNotEnabled()
        }
    }
}
