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

import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusableComponent
import androidx.compose.ui.focus.FocusableView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusWrapperTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun hostViewIsNotFocused_whenViewIsFocused() {
        // Arrange.
        lateinit var hostView: View
        lateinit var view: View
        lateinit var wrapperState: FocusState
        rule.setContent {
            hostView = LocalView.current
            AndroidView(
                factory = { FocusableView(it).apply { view = this } },
                modifier = Modifier.onFocusChanged { wrapperState = it }
            )
        }

        // Act.
        rule.runOnIdle { view.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(view.isFocused).isTrue()
            assertThat(wrapperState.hasFocus).isTrue()
            assertThat(wrapperState.isFocused).isFalse()
            assertThat(hostView.hasFocus()).isTrue()
            assertThat(hostView.isFocused).isFalse()
        }
    }

    @Test
    fun hostViewIsFocused_whenComposableIsFocused() {
        // Arrange.
        lateinit var hostView: View
        val composable = "composable"
        rule.setContent {
            hostView = LocalView.current
            FocusableComponent(composable)
        }

        // Act.
        rule.onNodeWithTag(composable).requestFocus()

        // Assert.
        rule.onNodeWithTag(composable).assertIsFocused()
        rule.runOnIdle {
            assertThat(hostView.hasFocus()).isTrue()
            assertThat(hostView.isFocused).isTrue()
        }
    }
}
