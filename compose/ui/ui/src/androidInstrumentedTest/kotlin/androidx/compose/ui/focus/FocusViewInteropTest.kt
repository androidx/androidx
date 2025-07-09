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

package androidx.compose.ui.focus

import android.app.Instrumentation
import android.content.Context
import android.graphics.Rect as AndroidRect
import android.os.Build.VERSION.SDK_INT
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.META_SHIFT_ON as Shift
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FocusViewInteropTest {

    @get:Rule val rule = createComposeRule()
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before fun enterNonTouchMode() = instrumentation.setInTouchMode(false)

    @After fun resetTouchMode() = instrumentation.resetInTouchModeCompat()

    @Test
    fun getFocusedRect_reportsFocusBounds_whenFocused() {
        val focusRequester = FocusRequester()
        var hasFocus = false
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier.size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .onFocusChanged {
                            if (it.isFocused) {
                                hasFocus = true
                            }
                        }
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.waitUntil { hasFocus }

        assertThat(view.getFocusedRect()).isEqualTo(IntRect(30, 40, 40, 60))
    }

    @Test
    fun getFocusedRect_reportsEntireView_whenNoFocus() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier.size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .focusable()
                )
            }
        }

        assertThat(view.getFocusedRect()).isEqualTo(IntRect(0, 0, 90, 100))
    }

    @Test
    fun getFocusedRect_reportsEmptyRect_whenNothingFocusable() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier.size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                )
            }
        }

        rule.waitForIdle()
        val expectedRect = IntRect(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        assertThat(view.getFocusedRect()).isEqualTo(expectedRect)
    }

    @Test
    fun getFocusedRect_reportsSizeOfFocusTarget_whenCanFocusIsTrue() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier.size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .focusProperties { canFocus = true }
                        .focusable()
                )
            }
        }

        assertThat(view.getFocusedRect()).isEqualTo(IntRect(0, 0, 90, 100))
    }

    @Test
    fun getFocusedRect_reportsEmptyRect_whenCanFocusIsFalse() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier.size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .focusProperties { canFocus = false }
                        .focusable()
                )
            }
        }

        val expectedRect = IntRect(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        assertThat(view.getFocusedRect()).isEqualTo(expectedRect)
    }

    @Test
    fun getFocusedRect_reportsSizeOfFocusTarget_whenEnterIsCanceled() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier.size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .focusProperties { onEnter = { cancelFocusChange() } }
                        .focusable()
                )
            }
        }

        assertThat(view.getFocusedRect()).isEqualTo(IntRect(0, 0, 90, 100))
    }

    @Test
    fun requestFocus_returnsFalseWhenCancelled() {
        // Arrange.
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.size(10.dp)
                    .focusProperties { onEnter = { cancelFocusChange() } }
                    .focusGroup()
            ) {
                Box(Modifier.size(10.dp).focusable())
            }
        }

        // Act.
        val success = rule.runOnIdle { view.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(success).isFalse() }
    }

    @Test
    fun focusGainOnRemovedView() {
        val lazyListState = LazyListState(0, 0)
        var thirdEditText: EditText? by mutableStateOf(null)
        var thirdFocused = false
        var touchSlop = 0f

        // This looks a little complex, but it is slightly simplified from the b/367238588
        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            val keyboardToolbarVisible = remember { MutableStateFlow(false) }
            val showKeyboardToolbar by keyboardToolbarVisible.collectAsState()
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                LazyColumn(modifier = Modifier.weight(1f).testTag("list"), state = lazyListState) {
                    items(100) { index ->
                        val focusChangeListener = remember {
                            View.OnFocusChangeListener { v, hasFocus ->
                                keyboardToolbarVisible.tryEmit(hasFocus)
                                if (v == thirdEditText) {
                                    thirdFocused = hasFocus
                                }
                            }
                        }
                        AndroidView(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(with(LocalDensity.current) { 200.toDp() }),
                            factory = { context: Context ->
                                EditText(context).apply {
                                    onFocusChangeListener = focusChangeListener
                                }
                            },
                            onReset = {},
                            onRelease = {},
                        ) { et ->
                            et.setText("$index")
                            if (index == 2) {
                                thirdEditText = et
                            }
                        }
                    }
                }
                if (showKeyboardToolbar) {
                    Box(modifier = Modifier.height(1.dp).focusable().fillMaxWidth())
                }
            }
        }

        rule.runOnIdle { lazyListState.requestScrollToItem(5) }

        // Scroll it the other way until the first 3 are just hidden
        rule.runOnIdle { lazyListState.requestScrollToItem(3) }

        // Scroll down with touch
        rule.onNodeWithTag("list").performTouchInput {
            down(Offset(width / 2f, 1f))
            // drag touch slop amount
            moveBy(Offset(0f, touchSlop))
            // move it 10 pixels into the edit text
            moveBy(delta = Offset(0f, 10f))
            up()
        }

        // click just inside the list, on the first item
        rule.onNodeWithTag("list").performTouchInput {
            down(Offset(width / 2f, 1f))
            up()
        }

        rule.waitForIdle()
        assertThat(thirdFocused).isTrue()
    }

    @Test
    fun nonFocusableComposeViewDoesNotCrashOnFocusMove() {
        lateinit var topEditText: EditText
        lateinit var composeView: ComposeView
        lateinit var bottomEditText: EditText

        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LinearLayout(context).also { linearLayout ->
                        linearLayout.orientation = VERTICAL
                        EditText(context).also {
                            linearLayout.addView(it, LinearLayout.LayoutParams(100, 100))
                            topEditText = it
                            it.imeOptions = EditorInfo.IME_ACTION_NEXT
                        }
                        ComposeView(context).also {
                            it.setContent { Box(Modifier.size(10.dp)) }
                            linearLayout.addView(it, LinearLayout.LayoutParams(100, 100))
                            composeView = it
                        }
                        EditText(context).also {
                            linearLayout.addView(it, LinearLayout.LayoutParams(100, 100))
                            bottomEditText = it
                        }
                    }
                },
            )
        }

        rule.runOnIdle { topEditText.requestFocus() }

        rule.runOnIdle {
            BaseInputConnection(topEditText, true).performEditorAction(EditorInfo.IME_ACTION_NEXT)
        }

        rule.runOnIdle {
            assertThat(topEditText.isFocused).isFalse()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomEditText.isFocused).isTrue()
        }
    }

    @Test
    fun composeViewDoesNotCrashWithCanFocusFalseOnFocusMove() {
        lateinit var topEditText: EditText
        lateinit var composeView: ComposeView
        lateinit var bottomEditText: EditText

        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LinearLayout(context).also { linearLayout ->
                        linearLayout.orientation = VERTICAL
                        EditText(context).also {
                            linearLayout.addView(it)
                            topEditText = it
                            it.imeOptions = EditorInfo.IME_ACTION_NEXT
                        }
                        ComposeView(context).also {
                            it.setContent {
                                Box(
                                    Modifier.size(10.dp)
                                        .focusProperties { canFocus = false }
                                        .focusable()
                                )
                            }
                            linearLayout.addView(it)
                            composeView = it
                        }
                        EditText(context).also {
                            linearLayout.addView(it)
                            bottomEditText = it
                        }
                    }
                },
            )
        }

        rule.runOnIdle { topEditText.requestFocus() }

        rule.runOnIdle {
            BaseInputConnection(topEditText, true).performEditorAction(EditorInfo.IME_ACTION_NEXT)
        }

        rule.runOnIdle {
            assertThat(topEditText.isFocused).isFalse()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomEditText.isFocused).isTrue()
        }
    }

    @Test
    fun composeViewDoesNotCrashWithCanceledFocusOnFocusMove() {
        lateinit var topEditText: EditText
        lateinit var composeView: ComposeView
        lateinit var bottomEditText: EditText

        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LinearLayout(context).also { linearLayout ->
                        linearLayout.orientation = VERTICAL
                        EditText(context).also {
                            linearLayout.addView(it)
                            topEditText = it
                            it.imeOptions = EditorInfo.IME_ACTION_NEXT
                        }
                        ComposeView(context).also {
                            it.setContent {
                                Box(
                                    Modifier.focusProperties { onEnter = { cancelFocusChange() } }
                                        .focusGroup()
                                ) {
                                    Box(Modifier.size(10.dp).focusable())
                                }
                            }
                            linearLayout.addView(it)
                            composeView = it
                        }
                        EditText(context).also {
                            linearLayout.addView(it)
                            bottomEditText = it
                        }
                    }
                },
            )
        }

        rule.runOnIdle { topEditText.requestFocus() }

        rule.runOnIdle {
            BaseInputConnection(topEditText, true).performEditorAction(EditorInfo.IME_ACTION_NEXT)
        }

        rule.runOnIdle {
            assertThat(topEditText.isFocused).isFalse()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomEditText.isFocused).isTrue()
        }
    }

    @Test
    fun moveFocusThroughUnFocusableComposeViewPrevious() {
        @OptIn(ExperimentalComposeUiApi::class)
        assumeTrue(
            ComposeUiFlags.isBypassUnfocusableComposeViewEnabled ||
                ComposeUiFlags.isViewFocusFixEnabled
        )

        // Arrange.
        lateinit var topButton: Button
        lateinit var composeView: ComposeView
        lateinit var bottomButton: Button
        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Top Button"
                                topButton = this
                            }
                        )
                        addView(
                            ComposeView(it).apply {
                                setContent { Box(Modifier.size(10.dp)) }
                                composeView = this
                            }
                        )
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Bottom Button"
                                bottomButton = this
                            }
                        )
                    }
                },
            )
        }
        rule.runOnIdle { bottomButton.requestFocus() }

        // Act.
        instrumentation.sendKeySync(
            AndroidKeyEvent(0, 0, ACTION_DOWN, Key.Tab.nativeKeyCode, 0, Shift)
        )

        // Assert.
        rule.runOnIdle {
            assertThat(topButton.isFocused).isTrue()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomButton.isFocused).isFalse()
        }
    }

    @Test
    fun moveFocusThroughUnFocusableComposeViewNext() {
        @OptIn(ExperimentalComposeUiApi::class)
        assumeTrue(
            ComposeUiFlags.isBypassUnfocusableComposeViewEnabled ||
                ComposeUiFlags.isViewFocusFixEnabled
        )

        // Arrange.
        lateinit var topButton: Button
        lateinit var composeView: ComposeView
        lateinit var bottomButton: Button
        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Top Button"
                                topButton = this
                            }
                        )
                        addView(
                            ComposeView(it).apply {
                                setContent { Box(Modifier.size(10.dp)) }
                                composeView = this
                            }
                        )
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Bottom Button"
                                bottomButton = this
                            }
                        )
                    }
                },
            )
        }
        rule.runOnIdle { topButton.requestFocus() }

        // Act.
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.Tab.nativeKeyCode))

        // Assert.
        rule.runOnIdle {
            assertThat(topButton.isFocused).isFalse()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomButton.isFocused).isTrue()
        }
    }

    @Test
    fun moveFocusThroughUnFocusableComposeViewDown() {
        // Arrange.
        lateinit var topButton: Button
        lateinit var composeView: ComposeView
        lateinit var bottomButton: Button
        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Top Button"
                                topButton = this
                            }
                        )
                        addView(
                            ComposeView(it).apply {
                                setContent { Box(Modifier.size(10.dp)) }
                                composeView = this
                            }
                        )
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Bottom Button"
                                bottomButton = this
                            }
                        )
                    }
                },
            )
        }
        rule.runOnIdle { topButton.requestFocus() }

        // Act.
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.DirectionDown.nativeKeyCode))

        // Assert.
        rule.runOnIdle {
            assertThat(topButton.isFocused).isFalse()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomButton.isFocused).isTrue()
        }
    }

    @Test
    fun moveFocusThroughUnFocusableComposeViewUp() {
        // Arrange.
        lateinit var topButton: Button
        lateinit var composeView: ComposeView
        lateinit var bottomButton: Button
        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    LinearLayout(it).apply {
                        orientation = VERTICAL
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Top Button"
                                topButton = this
                            }
                        )
                        addView(
                            ComposeView(it).apply {
                                setContent { Box(Modifier.size(10.dp)) }
                                composeView = this
                            }
                        )
                        addView(
                            Button(it).apply {
                                isFocusableInTouchMode = true
                                text = "Bottom Button"
                                bottomButton = this
                            }
                        )
                    }
                },
            )
        }
        rule.runOnIdle { bottomButton.requestFocus() }

        // Act.
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.DirectionUp.nativeKeyCode))

        // Assert.
        rule.runOnIdle {
            assertThat(topButton.isFocused).isTrue()
            assertThat(composeView.isFocused).isFalse()
            assertThat(bottomButton.isFocused).isFalse()
        }
    }

    @Test
    fun focusBetweenComposeViews_NextPrevious() {
        @OptIn(ExperimentalComposeUiApi::class)
        assumeTrue(
            ComposeUiFlags.isBypassUnfocusableComposeViewEnabled ||
                ComposeUiFlags.isViewFocusFixEnabled
        )

        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LinearLayout(context).apply {
                        orientation = VERTICAL
                        addView(
                            ComposeView(context).apply {
                                setContent {
                                    Box(
                                        Modifier.size(10.dp)
                                            .focusProperties { canFocus = true }
                                            .focusable()
                                            .testTag("button1")
                                    )
                                }
                            }
                        )
                        addView(
                            ComposeView(context).apply {
                                setContent {
                                    Box(
                                        Modifier.size(10.dp)
                                            .focusProperties { canFocus = true }
                                            .focusable()
                                            .testTag("button2")
                                    )
                                }
                            }
                        )
                        addView(
                            ComposeView(context).apply {
                                setContent {
                                    Box(
                                        Modifier.size(10.dp)
                                            .focusProperties { canFocus = true }
                                            .focusable()
                                            .testTag("button3")
                                    )
                                }
                            }
                        )
                    }
                },
            )
        }
        rule.onNodeWithTag("button1").requestFocus()
        rule.onNodeWithTag("button1").assertIsFocused()
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.Tab.nativeKeyCode))
        rule.onNodeWithTag("button2").assertIsFocused()
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.Tab.nativeKeyCode))
        rule.onNodeWithTag("button3").assertIsFocused()
        instrumentation.sendKeySync(
            AndroidKeyEvent(0L, 0L, ACTION_DOWN, Key.Tab.nativeKeyCode, 0, Shift)
        )
        rule.onNodeWithTag("button2").assertIsFocused()
        instrumentation.sendKeySync(
            AndroidKeyEvent(0L, 0L, ACTION_DOWN, Key.Tab.nativeKeyCode, 0, Shift)
        )
        rule.onNodeWithTag("button1").assertIsFocused()
    }

    @Test
    fun focusBetweenComposeViews_DownUp() {
        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LinearLayout(context).also { linearLayout ->
                        linearLayout.orientation = VERTICAL
                        ComposeView(context).also {
                            it.setContent {
                                Box(
                                    Modifier.size(10.dp)
                                        .focusProperties { canFocus = true }
                                        .focusable()
                                        .testTag("button1")
                                )
                            }
                            linearLayout.addView(it)
                        }
                        ComposeView(context).also {
                            it.setContent {
                                Box(
                                    Modifier.size(10.dp)
                                        .focusProperties { canFocus = true }
                                        .focusable()
                                        .testTag("button2")
                                )
                            }
                            linearLayout.addView(it)
                        }
                        ComposeView(context).also {
                            it.setContent {
                                Box(
                                    Modifier.size(10.dp)
                                        .focusProperties { canFocus = true }
                                        .focusable()
                                        .testTag("button3")
                                )
                            }
                            linearLayout.addView(it)
                        }
                    }
                },
            )
        }
        rule.onNodeWithTag("button1").requestFocus()
        rule.onNodeWithTag("button1").assertIsFocused()
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.DirectionDown.nativeKeyCode))
        rule.onNodeWithTag("button2").assertIsFocused()
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.DirectionDown.nativeKeyCode))
        rule.onNodeWithTag("button3").assertIsFocused()
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.DirectionUp.nativeKeyCode))
        rule.onNodeWithTag("button2").assertIsFocused()
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.DirectionUp.nativeKeyCode))
        rule.onNodeWithTag("button1").assertIsFocused()
    }

    @Test
    fun requestFocusFromViewMovesToComposeView() {
        lateinit var androidButton1: Button
        lateinit var composeView: View
        val composeButton = FocusRequester()
        rule.setContent {
            composeView = LocalView.current
            Column(Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    Modifier.testTag("button")
                        .focusProperties { canFocus = true }
                        .focusRequester(composeButton),
                ) {
                    Text("Compose Button")
                }
                AndroidView(
                    factory = { context ->
                        LinearLayout(context).apply {
                            orientation = VERTICAL
                            addView(
                                Button(context).apply {
                                    text = "Android Button 1"
                                    isFocusableInTouchMode = true
                                    androidButton1 = this
                                }
                            )
                            addView(
                                Button(context).apply {
                                    text = "Android Button 2"
                                    isFocusableInTouchMode = true
                                }
                            )
                        }
                    }
                )
            }
        }

        for (direction in arrayOf(Left, Up, Right, Down, Next, Previous)) {
            rule.runOnIdle { androidButton1.requestFocus() }

            rule.runOnIdle {
                assertThat(androidButton1.isFocused).isTrue()
                composeButton.requestFocus(direction)
            }

            rule.onNodeWithTag("button").assertIsFocused()

            rule.runOnIdle {
                assertThat(composeView.isFocused).isTrue()
                assertThat(androidButton1.isFocused).isFalse()
            }
        }
    }

    @Test
    fun removeFocusedView() {
        // Arrange.
        lateinit var buttonView0: Button
        lateinit var buttonView2: Button
        lateinit var lazyListState: LazyListState
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = 2)
            with(rule.density) {
                LazyColumn(Modifier.size(10f.toDp()), lazyListState) {
                    items(3) { index ->
                        AndroidView(
                            modifier = Modifier.size(10f.toDp()),
                            factory = { context ->
                                Button(context).apply {
                                    text = "Android Button"
                                    isFocusableInTouchMode = true
                                    when (index) {
                                        0 -> buttonView0 = this
                                        2 -> buttonView2 = this
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        rule.runOnIdle { buttonView2.requestFocus() }

        // Act.
        rule.runOnIdle { lazyListState.requestScrollToItem(0) }

        // Assert.
        rule.runOnIdle {
            assertThat(buttonView2.isFocused).isFalse()
            // We don't reassign focus in touch mode.
            // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
            if (inputModeManager.inputMode == Touch && SDK_INT >= 28) {
                assertThat(buttonView0.isFocused).isFalse()
            } else {
                assertThat(buttonView0.isFocused).isTrue()
            }
        }
    }

    @Test
    fun moveBetweenEmbeddedCousins() {
        // Arrange.
        lateinit var button2: Button
        lateinit var button3: Button
        rule.setContent {
            AndroidView({
                LinearLayout(it).apply {
                    orientation = VERTICAL
                    addView(
                        ComposeView(it).also { composeView ->
                            composeView.setContent {
                                AndroidView({
                                    LinearLayout(it).apply {
                                        orientation = VERTICAL
                                        addView(Button(context).apply { text = "Button 1" })
                                        addView(
                                            Button(context).apply {
                                                text = "Button 2"
                                                isFocusableInTouchMode = true
                                                button2 = this
                                            }
                                        )
                                    }
                                })
                            }
                        }
                    )
                    addView(
                        ComposeView(it).also { composeView ->
                            composeView.setContent {
                                AndroidView({
                                    LinearLayout(it).apply {
                                        orientation = VERTICAL
                                        addView(
                                            Button(context).apply {
                                                text = "Button 3"
                                                isFocusableInTouchMode = true
                                                button3 = this
                                            }
                                        )
                                        addView(
                                            Button(context).apply {
                                                text = "Button 4"
                                                isFocusableInTouchMode = true
                                            }
                                        )
                                    }
                                })
                            }
                        }
                    )
                }
            })
        }
        rule.runOnIdle { button2.requestFocus() }

        // Act.
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.Tab.nativeKeyCode))

        // Assert.
        rule.runOnIdle { assertThat(button3.isFocused).isTrue() }
    }

    private fun View.getFocusedRect() =
        AndroidRect().run {
            rule.runOnIdle { getFocusedRect(this) }
            IntRect(left, top, right, bottom)
        }
}
