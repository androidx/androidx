/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ParameterizedComposeTestRule
import androidx.compose.testutils.createParameterizedComposeTestRule
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyListFocusMoveTest(param: FocusDirectionWrapper) {

    @get:Rule
    val rule = createParameterizedComposeTestRule<Param>()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    data class FocusDirectionWrapper(val direction: FocusDirection)

    class Param(
        val reverseLayout: Boolean,
        val layoutDirection: LayoutDirection
    ) {
        override fun toString() =
            "reverseLayout=$reverseLayout " +
                "layoutDirection=$layoutDirection"
    }

    private val focusDirection = param.direction
    private var initiallyFocused: FocusRequester = FocusRequester()
    private var isLazyListFocused by mutableStateOf(false)
    private val isFocused = mutableMapOf<Int, Boolean>()
    private lateinit var lazyListState: LazyListState
    private lateinit var focusManager: FocusManager

    companion object {
        val ParamsToRun = buildList {
            for (reverseLayout in listOf(true, false)) {
                for (layoutDirection in listOf(Ltr, Rtl)) {
                    add(Param(reverseLayout, layoutDirection))
                }
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = buildList {
            for (direction in arrayOf(Previous, Next, Left, Right, Up, Down, Enter, Exit)) {
                add(FocusDirectionWrapper(direction))
            }
        }
    }

    private fun resetTestCase() {
        isLazyListFocused = false
        isFocused.clear()
        initiallyFocused = FocusRequester()
    }

    @Test
    fun moveFocusAmongVisibleItems() {
        // Arrange.
        rule.setTestContent {
            lazyList(50.dp, it, lazyListState) {
                item { FocusableBox(0) }
                item { FocusableBox(1, initiallyFocused) }
                item { FocusableBox(2) }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle { initiallyFocused.requestFocus() }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 2 else 0]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 0 else 2]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 0 else 2]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 2 else 0]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 2 else 0]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 0 else 2]).isTrue()
                        Previous -> assertThat(isFocused[0]).isTrue()
                        Next -> assertThat(isFocused[2]).isTrue()
                        Enter -> assertThat(isFocused[1]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Test
    fun moveFocusAmongVisibleItems_userScrollIsOff() {
        // Arrange.
        rule.setTestContent {
            lazyList(50.dp, it, lazyListState, userScrollEnabled = false) {
                item { FocusableBox(0) }
                item { FocusableBox(1, initiallyFocused) }
                item { FocusableBox(2) }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle { initiallyFocused.requestFocus() }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 2 else 0]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 0 else 2]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 0 else 2]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 2 else 0]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 2 else 0]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 0 else 2]).isTrue()
                        Previous -> assertThat(isFocused[0]).isTrue()
                        Next -> assertThat(isFocused[2]).isTrue()
                        Enter -> assertThat(isFocused[1]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
                rule.waitForIdle()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsJustBeyondBounds() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, it, lazyListState) {
                items(5) { FocusableBox(it) }
                item { FocusableBox(5, initiallyFocused) }
                items(5) { FocusableBox(it + 6) }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle {
                    // Scroll so that the focused item is in the middle.
                    runBlocking { lazyListState.scrollToItem(4) }

                    // Move focus to the last visible item.
                    initiallyFocused.requestFocus()
                    when (focusDirection) {
                        Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(
                            focusDirection
                        )

                        Enter, Exit -> {
                            // Do nothing
                        }

                        else -> unsupportedDirection()
                    }
                }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 7 else 3]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 3 else 7]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 3 else 7]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 7 else 3]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 7 else 3]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 3 else 7]).isTrue()
                        Previous -> assertThat(isFocused[3]).isTrue()
                        Next -> assertThat(isFocused[7]).isTrue()
                        Enter -> assertThat(isFocused[5]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsJustBeyondBounds_userScrollIsOff() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, it, lazyListState, userScrollEnabled = false) {
                items(5) { FocusableBox(it) }
                item { FocusableBox(5, initiallyFocused) }
                items(5) { FocusableBox(it + 6) }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) {
                runOnIdle {
                    // Scroll so that the focused item is in the middle.
                    runBlocking { lazyListState.scrollToItem(4) }

                    // Move focus to the last visible item.
                    initiallyFocused.requestFocus()
                    when (focusDirection) {
                        Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(
                            focusDirection
                        )

                        Enter, Exit -> {
                            // Do nothing
                        }

                        else -> unsupportedDirection()
                    }
                }
                val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
                // Act.
                runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert We Did Not Move
                runOnIdle {
                    assertThat(lazyListState.firstVisibleItemIndex).isEqualTo(firstVisibleItemIndex)
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsFarBeyondBounds() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, it, lazyListState) {
                items(5) { FocusableBox(it) }
                items(100) { Box(Modifier.size(10.dp)) }
                item { FocusableBox(105) }
                item { FocusableBox(106, initiallyFocused) }
                item { FocusableBox(107) }
                items(100) { Box(Modifier.size(10.dp)) }
                items(5) { FocusableBox(it + 208) }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle {
                    // Scroll so that the focused item is in the middle.
                    runBlocking { lazyListState.scrollToItem(105) }
                    initiallyFocused.requestFocus()

                    // Move focus to the last visible item.
                    when (focusDirection) {
                        Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(
                            focusDirection
                        )

                        Enter, Exit -> {
                            // Do nothing
                        }

                        else -> unsupportedDirection()
                    }
                }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr ->
                                assertThat(isFocused[if (param.reverseLayout) 208 else 4]).isTrue()

                            Rtl ->
                                assertThat(isFocused[if (param.reverseLayout) 4 else 208]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr ->
                                assertThat(isFocused[if (param.reverseLayout) 4 else 208]).isTrue()

                            Rtl ->
                                assertThat(isFocused[if (param.reverseLayout) 208 else 4]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 208 else 4]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 4 else 208]).isTrue()
                        Previous -> assertThat(isFocused[4]).isTrue()
                        Next -> assertThat(isFocused[208]).isTrue()
                        Enter -> assertThat(isFocused[106]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsBeyondBoundsAndInANestedLazyList() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, it, lazyListState) {
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 0) } }
                }
                item { FocusableBox(3) }
                item { FocusableBox(4, initiallyFocused) }
                item { FocusableBox(5) }
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 6) } }
                }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle {
                    // Scroll so that the focused item is in the middle.
                    runBlocking { lazyListState.scrollToItem(1) }
                    initiallyFocused.requestFocus()

                    // Move focus to the last visible item.
                    when (focusDirection) {
                        Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(
                            focusDirection
                        )

                        Enter, Exit -> {
                            // Do nothing
                        }

                        else -> unsupportedDirection()
                    }
                }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 8 else 0]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 2 else 6]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 2 else 6]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 8 else 0]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 8 else 0]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 2 else 6]).isTrue()
                        Previous -> assertThat(isFocused[2]).isTrue()
                        Next -> assertThat(isFocused[6]).isTrue()
                        Enter -> assertThat(isFocused[4]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsBeyondBoundsAndOutsideTheCurrentLazyList() {
        // TODO(b/250083104) Previous focus search is broken, this test just wasn't catching it and
        //  this CL makes it more obvious. Re-enable when focus search is fixed.
        assumeFalse(focusDirection == Previous)

        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, it, lazyListState) {
                item { FocusableBox(0) }
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 1) } }
                }
                item { FocusableBox(4, initiallyFocused) }
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 5) } }
                }
                item { FocusableBox(8) }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle {
                    // Scroll so that the focused item is in the middle.
                    runBlocking { lazyListState.scrollToItem(1, 10) }
                    initiallyFocused.requestFocus()

                    // Move focus to the last visible item.
                    when (focusDirection) {
                        Left, Right, Up, Down -> focusManager.moveFocus(focusDirection)
                        Previous, Next -> repeat(3) { focusManager.moveFocus(focusDirection) }
                        Enter, Exit -> {
                            // Do nothing
                        }

                        else -> unsupportedDirection()
                    }
                }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 8 else 0]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 0 else 8]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr -> assertThat(isFocused[if (param.reverseLayout) 0 else 8]).isTrue()
                            Rtl -> assertThat(isFocused[if (param.reverseLayout) 8 else 0]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 8 else 0]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 0 else 8]).isTrue()
                        Previous -> assertThat(isFocused[0]).isTrue()
                        Next -> assertThat(isFocused[8]).isTrue()
                        Enter -> assertThat(isFocused[4]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Test
    fun moveFocusAmongNestedLazyLists() {
        // TODO(b/250083104) Previous focus search is broken, this test just wasn't catching it and
        //  this CL makes it more obvious. Re-enable when focus search is fixed.
        assumeFalse(focusDirection == Previous)

        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, it, lazyListState) {
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 0) } }
                }
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 3) } }
                }
                item { FocusableBox(6, initiallyFocused) }
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 7) } }
                }
                item {
                    lazyListCrossAxis(
                        30.dp,
                        it
                    ) { items(3) { FocusableBox(it + 10) } }
                }
            }
        }
        with(rule) {
            forEachParameter(ParamsToRun) { param ->
                runOnIdle {
                    // Scroll so that the focused item is in the middle.
                    runBlocking { lazyListState.scrollToItem(2, 0) }
                    initiallyFocused.requestFocus()

                    // Move focus to the last visible item.
                    when (focusDirection) {
                        Left, Right, Up, Down -> focusManager.moveFocus(focusDirection)
                        Previous, Next -> repeat(3) { focusManager.moveFocus(focusDirection) }
                        Enter, Exit -> {
                            // Do nothing
                        }

                        else -> unsupportedDirection()
                    }
                }

                // Act.
                val success = runOnIdle {
                    focusManager.moveFocus(focusDirection)
                }

                // Assert.
                runOnIdle {
                    assertThat(success).apply {
                        if (focusDirection == Enter) isFalse() else isTrue()
                    }
                    when (focusDirection) {
                        Left -> when (param.layoutDirection) {
                            Ltr ->
                                assertThat(isFocused[if (param.reverseLayout) 12 else 0]).isTrue()

                            Rtl ->
                                assertThat(isFocused[if (param.reverseLayout) 2 else 10]).isTrue()
                        }

                        Right -> when (param.layoutDirection) {
                            Ltr ->
                                assertThat(isFocused[if (param.reverseLayout) 2 else 10]).isTrue()

                            Rtl ->
                                assertThat(isFocused[if (param.reverseLayout) 12 else 0]).isTrue()
                        }

                        Up -> assertThat(isFocused[if (param.reverseLayout) 12 else 0]).isTrue()
                        Down -> assertThat(isFocused[if (param.reverseLayout) 2 else 10]).isTrue()
                        Previous -> assertThat(isFocused[2]).isTrue()
                        Next -> assertThat(isFocused[10]).isTrue()
                        Enter -> assertThat(isFocused[6]).isTrue()
                        Exit -> assertThat(isLazyListFocused).isTrue()
                        else -> unsupportedDirection()
                    }
                }
                runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }
                resetTestCase()
            }
        }
    }

    @Composable
    private fun FocusableBox(index: Int, focusRequester: FocusRequester = FocusRequester()) {
        Box(
            Modifier
                .size(10.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused[index] = it.isFocused }
                .focusable()
        )
    }

    private fun ParameterizedComposeTestRule<Param>.setTestContent(
        composable: @Composable (param: Param) -> Unit
    ) {
        setContent {
            key(it) {
                CompositionLocalProvider(LocalLayoutDirection provides it.layoutDirection) {
                    focusManager = LocalFocusManager.current
                    lazyListState = rememberLazyListState()
                    composable(it)
                }
            }
        }
    }

    @Composable
    private fun lazyList(
        size: Dp,
        param: Param,
        state: LazyListState = rememberLazyListState(),
        userScrollEnabled: Boolean = true,
        content: LazyListScope.() -> Unit,
    ) {
        when (focusDirection) {
            Left, Right, Enter, Exit, Next, Previous -> LazyRow(
                modifier = Modifier
                    .size(size)
                    .onFocusChanged { isLazyListFocused = it.isFocused }
                    .focusable(),
                state = state,
                reverseLayout = param.reverseLayout,
                content = content,
                userScrollEnabled = userScrollEnabled
            )

            Up, Down -> LazyColumn(
                modifier = Modifier
                    .size(size)
                    .onFocusChanged { isLazyListFocused = it.isFocused }
                    .focusable(),
                state = state,
                reverseLayout = param.reverseLayout,
                content = content,
                userScrollEnabled = userScrollEnabled
            )

            else -> unsupportedDirection()
        }
    }

    @Composable
    private fun lazyListCrossAxis(
        size: Dp,
        param: Param,
        state: LazyListState = rememberLazyListState(),
        content: LazyListScope.() -> Unit
    ) {
        when (focusDirection) {
            Left, Right, Enter, Exit, Next, Previous -> LazyColumn(
                modifier = Modifier.size(size),
                state = state,
                reverseLayout = param.reverseLayout,
                content = content
            )

            Up, Down -> LazyRow(
                modifier = Modifier.size(size),
                state = state,
                reverseLayout = param.reverseLayout,
                content = content
            )

            else -> unsupportedDirection()
        }
    }

    private fun unsupportedDirection(): Nothing = error("Unsupported Focus Direction.")
}
