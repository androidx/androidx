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

package androidx.compose.ui.layout

import android.view.ViewGroup.LayoutParams
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeViewLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun largeWidth() {
        var width by mutableStateOf(0)
        var height by mutableStateOf(0)
        rule.setContent {
            AndroidView(
                factory = { context ->
                    HorizontalScrollView(context).also { scrollView ->
                        scrollView.addView(
                            ComposeView(context).also { view ->
                                view.setContent {
                                    with(LocalDensity.current) {
                                        Box(
                                            Modifier.fillMaxHeight().width(2000.toDp()).onPlaced {
                                                coordinates ->
                                                width = coordinates.size.width
                                                height = coordinates.size.height
                                            }
                                        )
                                    }
                                }
                                view.layoutParams = LayoutParams(1 shl 28, 2000)
                            }
                        )
                    }
                }
            )
        }

        rule.runOnIdle {
            assertThat(width).isEqualTo(2000)
            assertThat(height).isEqualTo(2000)
        }
    }

    @Test
    fun largeHeight() {
        var width by mutableStateOf(0)
        var height by mutableStateOf(0)
        rule.setContent {
            AndroidView(
                factory = { context ->
                    ScrollView(context).also { scrollView ->
                        scrollView.addView(
                            ComposeView(context).also { view ->
                                view.setContent {
                                    with(LocalDensity.current) {
                                        Box(
                                            Modifier.fillMaxWidth().height(2000.toDp()).onPlaced {
                                                coordinates ->
                                                width = coordinates.size.width
                                                height = coordinates.size.height
                                            }
                                        )
                                    }
                                }
                                view.layoutParams = LayoutParams(2000, 1 shl 28)
                            }
                        )
                    }
                }
            )
        }

        rule.runOnIdle {
            assertThat(width).isEqualTo(2000)
            assertThat(height).isEqualTo(2000)
        }
    }

    @Test
    fun rootViewConfiguration() {
        val tag = "myLayout"
        rule.setContent { Box(Modifier.size(10.dp).testTag(tag)) }
        assertThat(rule.onRoot().fetchSemanticsNode().layoutInfo.viewConfiguration)
            .isSameInstanceAs(
                rule.onNodeWithTag(tag).fetchSemanticsNode().layoutInfo.viewConfiguration
            )
    }
}
