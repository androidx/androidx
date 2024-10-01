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

import android.content.Context
import android.graphics.Rect as AndroidRect
import android.view.View
import android.widget.EditText
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusViewInteropTest {

    @get:Rule val rule = createComposeRule()

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
    fun requestFocus_returnsFalseWhenCancelled() {
        // Arrange.
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.size(10.dp)
                    .focusProperties { enter = { FocusRequester.Cancel } }
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
                            onRelease = {}
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

    private fun View.getFocusedRect() =
        AndroidRect().run {
            rule.runOnIdle { getFocusedRect(this) }
            IntRect(left, top, right, bottom)
        }
}
