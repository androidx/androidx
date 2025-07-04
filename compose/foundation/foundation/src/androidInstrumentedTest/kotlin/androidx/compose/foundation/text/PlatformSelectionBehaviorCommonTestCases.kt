/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.contextmenu.ProcessTextApi23Impl
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.PlatformSelectionBehaviors
import androidx.compose.foundation.text.selection.PlatformSelectionBehaviorsFactory
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@SdkSuppress(minSdkVersion = 28)
@OptIn(ExperimentalFoundationApi::class)
abstract class PlatformSelectionBehaviorCommonTestCases : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val platformSelectionBehaviorsRule = PlatformSelectionBehaviorsRule()
    internal val TAG = "SelectableText"

    internal val fontSize = 10.sp

    internal val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    internal abstract val testLongPress: Boolean

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            ComposeFoundationFlags.isSmartSelectionEnabled = true
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            ComposeFoundationFlags.isSmartSelectionEnabled = false
        }
    }

    /** The composable component to be tested, which should be BTF1, BTF2 or SelectionContainer. */
    @Composable abstract fun Content(text: String, textStyle: TextStyle, modifier: Modifier)

    abstract val selection: TextRange

    @Test
    fun singleLine_callsSuggestSelectionForLongPressOrDoubleClick() {
        rule.setTextFieldTestContent {
            Content(
                text = "abc def ghi",
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        performLongPressOrDoubleClick { Offset(x = 5 * fontSize.toPx(), y = fontSize.toPx() / 2) }

        rule.waitForIdle()

        assertThat(selection).isEqualTo(TextRange(4, 7))
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc def ghi",
            TextRange(4, 7),
        )
        expectOnShowContextMenu("abc def ghi", TextRange(4, 7))
    }

    @Test
    fun multiline_callsSuggestSelectionForLongPressOrDoubleClick() {
        rule.setTextFieldTestContent {
            Content(
                text = "abc def ghi",
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        performLongPressOrDoubleClick { Offset(x = 5 * fontSize.toPx(), y = fontSize.toPx() / 2) }

        rule.waitForIdle()

        assertThat(selection).isEqualTo(TextRange(4, 7))
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc def ghi",
            TextRange(4, 7),
        )
        expectOnShowContextMenu("abc def ghi", TextRange(4, 7))
    }

    @Test
    fun dragToChangeSelection_notCallSuggestSelectionForLongPressOrDoubleClick() {
        rule.setTextFieldTestContent {
            Content(
                text = "abc def ghi",
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        // Select "abc" first and then drag to select " def",
        performLongPressOrDoubleClickThenDrag(
            position = { Offset(x = fontSize.toPx() * 2, y = fontSize.toPx() / 2) },
            moveTo = { Offset(x = fontSize.toPx() * 5, y = fontSize.toPx() / 2) },
        )

        rule.waitForIdle()

        assertThat(selection).isEqualTo(TextRange(0, 7))
        expectOnShowContextMenu("abc def ghi", TextRange(0, 7))
        platformSelectionBehaviorsRule.assertNoMoreCalls()
    }

    @Test
    fun dragButNotChangeSelection_callSuggestSelectionForLongPressOrDoubleClick() {
        rule.setTextFieldTestContent {
            Content(
                text = "abc def ghi",
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        // Select "abc" first and then drag to the offset after character "c", the
        // selection shouldn't update.
        performLongPressOrDoubleClickThenDrag(
            position = { Offset(x = fontSize.toPx() * 2, y = fontSize.toPx() / 2) },
            moveTo = { Offset(x = fontSize.toPx() * 3, y = fontSize.toPx() / 2) },
        )

        rule.waitForIdle()

        assertThat(selection).isEqualTo(TextRange(0, 3))
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc def ghi",
            TextRange(0, 3),
        )
        expectOnShowContextMenu("abc def ghi", TextRange(0, 3))
    }

    @Test
    fun doesApplySuggestedRange() {
        val state = TextFieldState("abc def ghi")
        val suggestedSelection = TextRange(1, 5)
        platformSelectionBehaviorsRule.suggestedSelection = suggestedSelection

        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        // select "abc".
        performLongPressOrDoubleClick { Offset(x = fontSize.toPx() * 2, y = fontSize.toPx() / 2) }
        rule.waitForIdle()

        assertThat(state.selection).isEqualTo(suggestedSelection)
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc def ghi",
            TextRange(0, 3),
        )
        expectOnShowContextMenu("abc def ghi", TextRange(0, 3))
    }

    internal fun performLongPressOrDoubleClick(position: InjectionScope.() -> Offset) {
        if (testLongPress) {
            rule.onNodeWithTag(TAG).performTouchInput {
                longPress(position.invoke(this))
                up()
            }
        } else {
            rule.onNodeWithTag(TAG).performMouseInput { doubleClick(position.invoke(this)) }
        }
    }

    internal fun performLongPressOrDoubleClickThenDrag(
        position: InjectionScope.() -> Offset,
        moveTo: InjectionScope.() -> Offset,
    ) {
        if (testLongPress) {
            rule.onNodeWithTag(TAG).performTouchInput {
                longPressAndDrag(position = position.invoke(this), moveTo = moveTo.invoke(this))
            }
        } else {
            rule.onNodeWithTag(TAG).performMouseInput {
                doubleClickAndDrag(position = position.invoke(this), moveTo = moveTo.invoke(this))
            }
        }
    }

    internal fun expectOnShowContextMenu(text: CharSequence, selection: TextRange) {
        // Mouse double click won't bring up the context menu.
        if (testLongPress) {
            platformSelectionBehaviorsRule.expectOnShowContextMenu(text, selection)
        }
    }
}

private fun TouchInjectionScope.longPressAndDrag(position: Offset, moveTo: Offset) {
    longPress(position)
    moveTo(moveTo)
    up()
}

private fun MouseInjectionScope.doubleClickAndDrag(position: Offset = center, moveTo: Offset) {
    click(position, MouseButton.Primary)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    press(MouseButton.Primary)
    advanceEventTime(SingleClickDelayMillis)
    moveTo(moveTo)
    release(MouseButton.Primary)
}

private val ViewConfiguration.defaultDoubleTapDelayMillis: Long
    get() = (doubleTapMinTimeMillis + doubleTapTimeoutMillis) / 2

private const val SingleClickDelayMillis = 60L

class PlatformSelectionBehaviorsRule : TestRule {
    private var testPlatformSelectionBehaviors: TestPlatformSelectionBehaviors? = null

    override fun apply(base: Statement?, description: Description?): Statement? {

        return if (Build.VERSION.SDK_INT >= 28) {
            object : Statement() {
                @SuppressLint("VisibleForTests")
                override fun evaluate() {
                    val oldQueryLambda = ProcessTextApi23Impl.processTextActivitiesQuery

                    val platformSelectionBehaviors =
                        TestPlatformSelectionBehaviors().also {
                            testPlatformSelectionBehaviors = it
                        }
                    PlatformSelectionBehaviorsFactory = { _, _, _, _ ->
                        platformSelectionBehaviors as PlatformSelectionBehaviors
                    }
                    base?.evaluate()

                    ProcessTextApi23Impl.processTextActivitiesQuery = oldQueryLambda
                }
            }
        } else {
            base
        }
    }

    /**
     * The returned suggested selection range if
     * [PlatformSelectionBehaviors.suggestSelectionForLongPressOrDoubleClick] is called.
     */
    var suggestedSelection: TextRange?
        set(value) {
            testPlatformSelectionBehaviors!!.suggestedSelection = value
        }
        get() = testPlatformSelectionBehaviors!!.suggestedSelection

    fun expectSuggestSelectionForLongPressOrDoubleClick(text: CharSequence, selection: TextRange) {
        testPlatformSelectionBehaviors!!.expectSuggestSelectionForLongPressOrDoubleClick(
            text,
            selection,
        )
    }

    fun expectOnShowContextMenu(text: CharSequence, selection: TextRange) {
        testPlatformSelectionBehaviors!!.expectOnShowContextMenu(text, selection)
    }

    fun assertNoMoreCalls() {
        testPlatformSelectionBehaviors!!.assertNoMoreCalls()
    }
}

internal class TestPlatformSelectionBehaviors : PlatformSelectionBehaviors {
    var suggestedSelection: TextRange? = null

    val suggestSelectionForLongPressOrDoubleClickCalls =
        mutableListOf<Pair<CharSequence, TextRange>>()

    val onShowContextMenuCalls = mutableListOf<Pair<CharSequence, TextRange>>()

    fun expectSuggestSelectionForLongPressOrDoubleClick(text: CharSequence, selection: TextRange) {
        val firstCall = suggestSelectionForLongPressOrDoubleClickCalls.first()
        assertThat(firstCall.first.toString()).isEqualTo(text.toString())
        assertThat(firstCall.second).isEqualTo(selection)
        suggestSelectionForLongPressOrDoubleClickCalls.removeAt(0)
    }

    fun expectOnShowContextMenu(text: CharSequence, selection: TextRange) {
        val firstCall = onShowContextMenuCalls.first()
        assertThat(firstCall.first.toString()).isEqualTo(text.toString())
        assertThat(firstCall.second).isEqualTo(selection)
        onShowContextMenuCalls.removeAt(0)
    }

    fun assertNoMoreCalls() {
        assertThat(suggestSelectionForLongPressOrDoubleClickCalls).isEmpty()
        assertThat(onShowContextMenuCalls).isEmpty()
    }

    override suspend fun suggestSelectionForLongPressOrDoubleClick(
        text: CharSequence,
        selection: TextRange,
    ): TextRange? {
        suggestSelectionForLongPressOrDoubleClickCalls.add(text to selection)
        return suggestedSelection
    }

    override suspend fun onShowContextMenu(text: CharSequence, selection: TextRange) {
        onShowContextMenuCalls.add(text to selection)
    }
}
