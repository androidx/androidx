/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalComposeUiApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class LocalSoftwareKeyboardControllerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun whenButtonClicked_performsHide_realisticAppTestCase() {
        // code under test
        @Composable
        fun TestComposable() {
            val softwareKeyboardController = LocalSoftwareKeyboardController.current
            // Box instead of Button in this file for module dependency reasons
            Box(Modifier.clickable { softwareKeyboardController?.hide() }) {
                BasicText("Click Me")
            }
        }

        // arrange
        val mockSoftwareKeyboardController: SoftwareKeyboardController = mock()
        rule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides mockSoftwareKeyboardController
            ) {
                TestComposable()
            }
        }

        // act
        rule.onNodeWithText("Click Me").performClick()

        // assert
        rule.runOnIdle {
            verify(mockSoftwareKeyboardController).hide()
        }
    }

    @Test
    fun localSoftwareKeyboardController_delegatesTo_textInputService() {
        val platformTextInputService = mock<PlatformTextInputService>()
        platformTextInputServiceInterceptor = { platformTextInputService }

        rule.setContent {
            val controller = LocalSoftwareKeyboardController.current
            SideEffect {
                controller?.hide()
            }
        }

        rule.runOnIdle {
            verify(platformTextInputService, times(1))
                .hideSoftwareKeyboard()
        }
    }

    @Test
    fun showHideSoftKeyboard_dontCrash_beforeSession() {
        var keyboardController: SoftwareKeyboardController? = null
        rule.setContent {
            keyboardController = LocalSoftwareKeyboardController.current
        }
        keyboardController!!.show()
        keyboardController!!.hide()
    }
}
