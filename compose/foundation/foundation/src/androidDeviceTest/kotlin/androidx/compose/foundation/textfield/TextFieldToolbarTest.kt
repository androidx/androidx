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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.test.FakeTextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.contextmenu.test.assertNotNull
import androidx.compose.foundation.text.contextmenu.test.assertShown
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextFieldToolbarTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    val TAG = "TestBasicTextField"

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun setUp() {
        assumeTrue(ComposeFoundationFlags.isNewContextMenuEnabled)
    }

    @Test
    fun btf1_decorationBoxNotCallInnerTextField_longPress_doNotCrash() {
        rule.setContent {
            @Suppress("DEPRECATION") // b/552879150
            BasicTextField(
                modifier = Modifier.testTag(TAG),
                value = TextFieldValue(),
                onValueChange = {},
                decorationBox = {
                    Box(modifier = Modifier.size(50.dp, 50.dp).background(Color.Red))
                },
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        // Long click won't crash, no assertion needed.
    }

    @Test
    fun btf2_decorationBoxNotCallInnerTextField_longPress_doNotCrash() {
        rule.setContent {
            val state = rememberTextFieldState()
            BasicTextField(
                modifier = Modifier.testTag(TAG),
                state = state,
                decorator = { Box(modifier = Modifier.size(50.dp, 50.dp).background(Color.Red)) },
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        // Long click won't crash, no assertion needed.
    }

    // Regression test for b/516844145
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun btf1_autofillItemClick_afterTextFieldRemoved_doesNotCrash() {
        val fontSize = 10.sp
        val fontSizePx = with(rule.density) { fontSize.toPx() }
        var value by mutableStateOf(TextFieldValue("Hello"))
        var showTextField by mutableStateOf(true)
        val spyCallback = SpyTextActionModeCallback()
        var latestDataProvider: TextContextMenuDataProvider? = null

        rule.setTextFieldTestContent {
            ProvidePlatformTextContextMenuToolbar(
                callbackInjector = { spyCallback.apply { delegate = it } }
            ) {
                val platformToolbarProvider =
                    checkNotNull(LocalTextContextMenuToolbarProvider.current)
                val recordingToolbarProvider =
                    remember(platformToolbarProvider) {
                        FakeTextContextMenuProvider { dataProvider ->
                            latestDataProvider = dataProvider
                            platformToolbarProvider.showTextContextMenu(dataProvider)
                        }
                    }
                CompositionLocalProvider(
                    LocalTextContextMenuToolbarProvider provides recordingToolbarProvider
                ) {
                    if (showTextField) {
                        @Suppress("DEPRECATION") // b/552879150
                        BasicTextField(
                            value = value,
                            onValueChange = { value = it },
                            modifier = Modifier.width(100.dp).testTag(TAG),
                            textStyle =
                                TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize),
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.waitForIdle()
        spyCallback.assertShown(true)
        val menu = assertNotNull(spyCallback.menu)
        assertNotNull(menu.findItem(android.R.id.autofill))
        val autofillItem = rule.runOnIdle {
            assertNotNull(latestDataProvider)
                .data()
                .components
                .filterIsInstance<TextContextMenuItem>()
                .single { it.key == TextContextMenuKeys.AutofillKey }
        }

        showTextField = false
        rule.waitForIdle()
        spyCallback.assertShown(false)

        var sessionClosed = false
        val session =
            object : TextContextMenuSession {
                override fun close() {
                    sessionClosed = true
                }
            }
        // Invoke the pre-detach item directly (matching a queued FloatingToolbar button click)
        // because onPrepareActionMode may have already cleared MenuBuilder by id.
        rule.runOnUiThread { autofillItem.onClick(session) }
        assertThat(sessionClosed).isTrue()
    }
}
