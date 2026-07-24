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

package androidx.compose.foundation.text.input.internal.selection

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ConfigChangeAppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.contextmenu.test.assertShown
import androidx.compose.foundation.text.contextmenu.test.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(ContextMenuFlagFlipperRunner::class)
@ContextMenuFlagSuppress(suppressedFlagValue = false)
class TextFieldConfigChangeAppCompatActivityLocaleTest : FocusedWindowTest {

    @get:Rule val rule = createAndroidComposeRule<ConfigChangeAppCompatActivity>()

    private val TAG = "BasicTextField"
    private val fontSize = 10.sp
    private lateinit var defaultLocaleListCompat: LocaleListCompat

    @Before
    fun setup() {
        defaultLocaleListCompat = AppCompatDelegate.getApplicationLocales()
    }

    @After
    fun teardown() {
        rule.runOnUiThread { AppCompatDelegate.setApplicationLocales(defaultLocaleListCompat) }
    }

    @Test
    fun toolbar_showsLocalizedStrings_whenLocaleChanges() = runTest {
        val textFieldState = TextFieldState("Hello")
        val boxFocusRequester = FocusRequester()
        val spyTextActionModeCallback = SpyTextActionModeCallback()
        val clipboard = FakeClipboard()

        rule.setTextFieldTestContent {
            ProvidePlatformTextContextMenuToolbar(
                callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
            ) {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    Box(modifier = Modifier.focusRequester(boxFocusRequester).size(100.dp)) {
                        BasicTextField(
                            state = textFieldState,
                            modifier = Modifier.testTag(TAG),
                            textStyle =
                                TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize),
                        )
                    }
                }
            }
        }

        val tagInteraction = rule.onNodeWithTag(TAG)
        rule.runOnUiThread { boxFocusRequester.requestFocus() }
        rule.waitForIdle()

        // Select text to trigger context menu toolbar
        tagInteraction.performTextInputSelectionShowingToolbar(TextRange(0, 5))
        rule.waitForIdle()

        // Verify initial toolbar matches default locale (which should be "Copy" or similar in
        // English)
        spyTextActionModeCallback.assertShown(true)
        val itemsBefore = spyTextActionModeCallback.menu!!.items().map { it.title.toString() }
        assertThat(itemsBefore).contains("Copy")

        // Change locale directly on AppCompatActivity
        rule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es-MX"))
        }
        rule.waitForIdle()

        // Verify toolbar is updated with spanish translation
        val itemsAfter = spyTextActionModeCallback.menu!!.items().map { it.title.toString() }
        assertThat(itemsAfter).contains("Copiar")
    }
}
