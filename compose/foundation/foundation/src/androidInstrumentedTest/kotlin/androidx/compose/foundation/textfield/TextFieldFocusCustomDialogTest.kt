/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextFieldFocusCustomDialogTest {
    @get:Rule
    val rule = createAndroidComposeRule<FragmentActivity>()

    data class FocusTestData(val focusRequester: FocusRequester, var focused: Boolean = false)

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun keyboardShown_forFieldInAndroidDialog_whenFocusRequestedImmediately_fromLaunchedEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                LaunchedEffect(Unit) {
                    it()
                }
            },
            wrapContent = {
                CustomDialog(content = it)
            }
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun keyboardShown_forFieldInAndroidDialog_whenFocusRequestedImmediately_fromDisposableEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            },
            wrapContent = {
                CustomDialog(content = it)
            }
        )
    }

    private fun keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
        runEffect: @Composable (body: () -> Unit) -> Unit,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit = { it() }
    ) {
        val focusRequester = FocusRequester()
        val keyboardHelper = KeyboardHelper(rule)

        rule.setContent {
            wrapContent {
                keyboardHelper.initialize()

                runEffect {
                    assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
                    focusRequester.requestFocus()
                }

                BasicTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        }

        rule.waitForIdle()
        // It's not enough to focus the composable, we also have to make the dialog window focused.
        Espresso.onView(ViewMatchers.supportsInputMethods()).perform(ViewActions.click())

        keyboardHelper.waitForKeyboardVisibility(visible = true)

        // Ensure the keyboard doesn't leak in to the next test. Can't do this at the start of the
        // test since the KeyboardHelper won't be initialized until composition runs, and this test
        // is checking behavior that all happens on the first frame.
        keyboardHelper.hideKeyboard()
        keyboardHelper.waitForKeyboardVisibility(visible = false)
    }

    /**
     * This represents an Android dialog that doesn't use the Compose [Dialog] function, and thus
     * doesn't use DialogWindowProvider.
     */
    @Composable
    private fun CustomDialog(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val updatedContent by rememberUpdatedState(content)
        DisposableEffect(Unit) {
            val dialogFragment = CustomDialogFragment { updatedContent() }
            val fragmentManager = context.findActivity().supportFragmentManager
            dialogFragment.show(fragmentManager, null)
            onDispose {}
        }
    }

    private tailrec fun Context.findActivity(): FragmentActivity = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("No FragmentActivity found")
    }

    class CustomDialogFragment(private val content: @Composable () -> Unit) : DialogFragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = ComposeView(requireContext()).also {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
            it.setContent(content)
        }
    }
}
