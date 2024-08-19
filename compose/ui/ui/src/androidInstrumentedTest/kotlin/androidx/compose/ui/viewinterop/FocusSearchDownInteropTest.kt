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

package androidx.compose.ui.viewinterop

import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusableComponent
import androidx.compose.ui.focus.FocusableView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class FocusSearchDownInteropTest(private val moveFocusProgrammatically: Boolean) {
    @get:Rule val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private lateinit var view: View
    private lateinit var view1: View
    private lateinit var view2: View
    private lateinit var wrapperState1: FocusState
    private lateinit var wrapperState2: FocusState
    private val composable = "composable"
    private val composable1 = "composable1"
    private val composable2 = "composable2"

    @Test
    fun singleFocusableComposable() {
        // Arrange.
        setContent { FocusableComponent(composable) }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable).assertIsFocused()
    }

    @Test
    fun singleFocusableView() {
        // Arrange.
        setContent { AndroidView({ FocusableView(it).apply { view = this } }) }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
    }

    @Test
    fun singleViewInLinearLayout() {
        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(FocusableView(it).apply { view = this })
                }
            })
        }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
    }

    @Test
    fun viewViewInLinearLayout() {
        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(FocusableView(it).apply { view1 = this })
                    addView(FocusableView(it).apply { view2 = this })
                }
            })
        }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isTrue()
            assertThat(view2.isFocused).isFalse()
        }
    }

    @Test
    fun focusedViewViewInLinearLayout() {
        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(FocusableView(it).apply { view1 = this })
                    addView(FocusableView(it).apply { view2 = this })
                }
            })
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(view2.isFocused).isTrue()
        }
    }

    @Test
    fun focusedComposableViewInLinearLayout() {
        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(ComposeView(it).apply { setContent { FocusableComponent(composable) } })
                    addView(FocusableView(it).apply { view = this })
                }
            })
        }
        rule.onNodeWithTag(composable).requestFocus()

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable).assertIsNotFocused()
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
    }

    @Test
    fun focusedComposableWithFocusableView_view_inLinearLayout() {

        // TODO(b/354025981) This test is flaky when moving focus programmatically.
        //  Note: Moving focus programmatically among views is a stretch goal,
        //  as the view system does not have a moveFocus() API.
        if (!moveFocusProgrammatically) return

        // Arrange.
        var isComposableFocused = false
        setContent {
            AndroidView({ context ->
                LinearLayout(context).apply {
                    orientation = VERTICAL
                    addView(
                        ComposeView(context).apply {
                            setContent {
                                Column(
                                    Modifier.testTag(composable)
                                        .onFocusChanged { isComposableFocused = it.isFocused }
                                        .focusable()
                                ) {
                                    AndroidView({ FocusableView(it).apply { view1 = this } })
                                }
                            }
                        }
                    )
                    addView(FocusableView(context).apply { view2 = this })
                }
            })
        }
        rule.onNodeWithTag(composable).requestFocus()
        rule.waitUntil { isComposableFocused }

        // Act.
        rule.focusSearchDown(waitForIdle = false)

        // Assert.
        rule.waitUntil { !isComposableFocused && view2.isFocused }
    }

    @Test
    fun viewViewNoRolloverInLinearLayout() {
        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(FocusableView(it).apply { view1 = this })
                    addView(FocusableView(it).apply { view2 = this })
                }
            })
        }
        rule.runOnIdle { view2.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(view2.isFocused).isTrue()
        }
    }

    @Test
    fun viewComposableViewInLinearLayout() {
        // TODO(b/261190892): Investigate why FocusManager.moveFocus(Right)
        //  does not navigate to an embedded Composable.
        //  Note: Moving focus programmatically among views is a stretch goal,
        //  as the view system does not have a moveFocus() API.
        if (moveFocusProgrammatically) return

        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(FocusableView(it).apply { view1 = this })
                    addView(ComposeView(it).apply { setContent { FocusableComponent(composable) } })
                    addView(FocusableView(it).apply { view2 = this })
                }
            })
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view1.isFocused).isFalse() }
        rule.onNodeWithTag(composable).assertIsFocused()
        rule.runOnIdle { assertThat(view2.isFocused).isFalse() }
    }

    @Test
    fun viewViewComposableInLinearLayout() {
        // Arrange.
        setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(FocusableView(it).apply { view1 = this })
                    addView(FocusableView(it).apply { view2 = this })
                    addView(ComposeView(it).apply { setContent { FocusableComponent(composable) } })
                }
            })
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(view2.isFocused).isTrue()
        }
        rule.onNodeWithTag(composable).assertIsNotFocused()
    }

    @Test
    fun movingAcrossLinearLayouts() {
        // Arrange.
        setContent {
            Column {
                AndroidView({
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(FocusableView(it))
                        addView(FocusableView(it).apply { view1 = this })
                    }
                })
                AndroidView({
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(FocusableView(it).apply { view2 = this })
                        addView(FocusableView(it))
                    }
                })
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(view2.isFocused).isTrue()
        }
    }

    @Test
    fun composableToLinearLayout() {
        // Arrange.
        setContent {
            Column {
                FocusableComponent(composable1)
                AndroidView({
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(FocusableView(it).apply { view = this })
                        addView(FocusableView(it))
                    }
                })
                FocusableComponent(composable2)
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
        rule.onNodeWithTag(composable2).assertIsNotFocused()
    }

    @Test
    fun linearLayoutToComposable() {
        // Arrange.
        setContent {
            Column {
                AndroidView({
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(FocusableView(it))
                        addView(FocusableView(it).apply { view1 = this })
                    }
                })
                FocusableComponent(composable)
                AndroidView({ FocusableView(it).apply { view2 = this } })
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable).assertIsFocused()
        rule.runOnIdle { assertThat(view2.isFocused).isFalse() }
    }

    @Test
    fun composableViewInColumn() {
        // Arrange.
        setContent {
            Column {
                FocusableComponent(composable)
                AndroidView({ FocusableView(it).apply { view = this } })
            }
        }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable).assertIsFocused()
        rule.runOnIdle { assertThat(view.isFocused).isFalse() }
    }

    @Test
    fun focusedComposableViewInColumn() {
        // Arrange.
        setContent {
            Column {
                FocusableComponent(composable)
                AndroidView({ FocusableView(it).apply { view = this } })
            }
        }
        rule.onNodeWithTag(composable).requestFocus()

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable).assertIsNotFocused()
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
    }

    @Test
    fun composableViewNoRolloverInColumn() {
        // Arrange.
        setContent {
            Column {
                FocusableComponent(composable)
                AndroidView({ FocusableView(it).apply { view = this } })
            }
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
        rule.onNodeWithTag(composable).assertIsNotFocused()
    }

    @Test
    fun viewComposableInColumn() {
        // Arrange.
        setContent {
            Column {
                AndroidView({ FocusableView(it).apply { view = this } })
                FocusableComponent(composable)
            }
        }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
        rule.onNodeWithTag(composable).assertIsNotFocused()
    }

    @Test
    fun focusedViewComposableInColumn() {
        // Arrange.
        setContent {
            Column {
                AndroidView({ FocusableView(it).apply { view = this } })
                FocusableComponent(composable)
            }
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view.isFocused).isFalse() }
        rule.onNodeWithTag(composable).assertIsFocused()
    }

    @Test
    fun viewComposableNoRolloverInColumn() {
        // Arrange.
        setContent {
            Column {
                AndroidView({ FocusableView(it).apply { view = this } })
                FocusableComponent(composable)
            }
        }
        rule.onNodeWithTag(composable).requestFocus()

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable).assertIsFocused()
        rule.runOnIdle { assertThat(view.isFocused).isFalse() }
    }

    @Test
    fun viewViewInColumn() {
        // Arrange.
        setContent {
            Column {
                AndroidView(
                    factory = { FocusableView(it).apply { view1 = this } },
                    modifier = Modifier.onFocusChanged { wrapperState1 = it }
                )
                AndroidView(
                    factory = { FocusableView(it).apply { view2 = this } },
                    modifier = Modifier.onFocusChanged { wrapperState2 = it }
                )
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(wrapperState1.hasFocus).isFalse()
            assertThat(wrapperState1.isFocused).isFalse()

            assertThat(view2.isFocused).isTrue()
            assertThat(wrapperState2.hasFocus).isTrue()
            assertThat(wrapperState2.isFocused).isFalse()
        }
    }

    @Test
    fun viewViewNoRolloverInColumn() {
        // Arrange.
        setContent {
            Column {
                AndroidView(
                    factory = { FocusableView(it).apply { view1 = this } },
                    modifier = Modifier.onFocusChanged { wrapperState1 = it }
                )
                AndroidView(
                    factory = { FocusableView(it).apply { view2 = this } },
                    modifier = Modifier.onFocusChanged { wrapperState2 = it }
                )
            }
        }
        rule.runOnIdle { view2.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(wrapperState1.hasFocus).isFalse()
            assertThat(wrapperState1.isFocused).isFalse()

            assertThat(view2.isFocused).isTrue()
            assertThat(wrapperState2.hasFocus).isTrue()
            assertThat(wrapperState2.isFocused).isFalse()
        }
    }

    @Test
    fun composableViewComposableInColumn() {
        // Arrange.
        setContent {
            Column {
                FocusableComponent(composable1)
                AndroidView({ FocusableView(it).apply { view = this } })
                FocusableComponent(composable2)
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable1).assertIsNotFocused()
        rule.runOnIdle { assertThat(view.isFocused).isTrue() }
        rule.onNodeWithTag(composable2).assertIsNotFocused()
    }

    @Test
    fun composableComposableViewInColumn() {
        // Arrange.
        setContent {
            Column {
                FocusableComponent(composable1)
                FocusableComponent(composable2)
                AndroidView({ FocusableView(it).apply { view = this } })
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.onNodeWithTag(composable1).assertIsNotFocused()
        rule.onNodeWithTag(composable2).assertIsFocused()
        rule.runOnIdle { assertThat(view.isFocused).isFalse() }
    }

    @Test
    fun viewComposableViewInColumn() {
        // Arrange.
        setContent {
            Column {
                AndroidView({ FocusableView(it).apply { view1 = this } })
                FocusableComponent(composable)
                AndroidView({ FocusableView(it).apply { view2 = this } })
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle { assertThat(view1.isFocused).isFalse() }
        rule.onNodeWithTag(composable).assertIsFocused()
        rule.runOnIdle { assertThat(view2.isFocused).isFalse() }
    }

    @Test
    fun viewViewComposableInColumn() {
        // Arrange.
        setContent {
            Column {
                Box {
                    AndroidView({ FocusableView(it).apply { view1 = this } }, Modifier.size(50.dp))
                }
                Box {
                    AndroidView({ FocusableView(it).apply { view2 = this } }, Modifier.size(50.dp))
                }
                FocusableComponent(composable)
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        // Act.
        rule.focusSearchDown()

        // Assert.
        rule.runOnIdle {
            assertThat(view1.isFocused).isFalse()
            assertThat(view2.isFocused).isTrue()
        }
        rule.onNodeWithTag(composable).assertIsNotFocused()
    }

    private fun ComposeContentTestRule.focusSearchDown(waitForIdle: Boolean = true) {
        if (waitForIdle) waitForIdle()
        if (moveFocusProgrammatically) {
            runOnUiThread { focusManager.moveFocus(FocusDirection.Down) }
        } else {
            InstrumentationRegistry.getInstrumentation()
                .sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, Key.DirectionDown.nativeKeyCode))
        }
    }

    private fun setContent(composable: @Composable () -> Unit) {
        rule.setContent {
            focusManager = LocalFocusManager.current
            composable.invoke()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "moveFocusProgrammatically = {0}")
        fun initParameters(): Array<Boolean> = arrayOf(false, true)
    }
}
