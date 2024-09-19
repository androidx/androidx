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
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusableComponent
import androidx.compose.ui.focus.FocusableView
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ViewParentFocusSearchTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var composeView: ViewGroup
    private lateinit var view: View
    private lateinit var view1: View
    private lateinit var view2: View
    private lateinit var wrapperState1: FocusState
    private lateinit var wrapperState2: FocusState
    private val composable = "composable"
    private val composable1 = "composable1"
    private val composable2 = "composable2"

    @Test
    fun canceledFocusSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                FocusableComponent(
                    composable1,
                    Modifier.focusProperties { down = FocusRequester.Cancel }
                )
                FocusableComponent(composable2)
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        rule.runOnIdle {
            assertThat(composeView.focusSearch(composeView, View.FOCUS_DOWN))
                .isSameInstanceAs(composeView)
        }
    }

    @Test
    fun noComposeViewSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                FocusableComponent(composable1)
                AndroidView({ FocusableView(it).apply { view1 = this } })
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        rule.runOnIdle {
            assertThat(composeView.focusSearch(composeView, View.FOCUS_DOWN))
                .isSameInstanceAs(view1)
        }
    }

    @Test
    fun onlyComposeViewSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                FocusableComponent(composable1)
                FocusableComponent(composable2)
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        rule.runOnIdle {
            assertThat(composeView.focusSearch(composeView, View.FOCUS_DOWN))
                .isSameInstanceAs(composeView)
        }
    }

    @Test
    fun nextComposeViewSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                AndroidView({ FocusableView(it).apply { view1 = this } })
                FocusableComponent(composable1)
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        rule.runOnIdle {
            assertThat(composeView.focusSearch(view1, View.FOCUS_DOWN))
                .isSameInstanceAs(composeView)
        }
    }

    @Test
    fun oneDimensionSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                FocusableComponent(composable1)
                AndroidView({ FocusableView(it).apply { view1 = this } })
            }
        }
        rule.onNodeWithTag(composable1).requestFocus()

        rule.runOnIdle {
            assertThat(composeView.focusSearch(composeView, View.FOCUS_FORWARD))
                .isSameInstanceAs(view1)
        }
    }

    @Test
    fun bestComposeViewSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                AndroidView({ FocusableView(it).apply { view1 = this } })
                FocusableComponent(composable1)
                AndroidView({ FocusableView(it).apply { view2 = this } })
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        rule.runOnIdle {
            assertThat(composeView.focusSearch(view1, View.FOCUS_DOWN))
                .isSameInstanceAs(composeView)
        }
    }

    @Test
    fun bestViewSearch() {
        rule.setContent {
            composeView = LocalView.current as ViewGroup
            Column {
                AndroidView({ FocusableView(it).apply { view1 = this } })
                AndroidView({ FocusableView(it).apply { view2 = this } })
                FocusableComponent(composable1)
            }
        }
        rule.runOnIdle { view1.requestFocus() }

        rule.runOnIdle {
            assertThat(composeView.focusSearch(view1, View.FOCUS_DOWN)).isSameInstanceAs(view2)
        }
    }
}
