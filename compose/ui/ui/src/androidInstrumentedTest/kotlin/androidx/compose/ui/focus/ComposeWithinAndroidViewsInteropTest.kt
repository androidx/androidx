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

package androidx.compose.ui.focus

import android.app.Instrumentation
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.META_SHIFT_ON
import android.widget.Button
import android.widget.LinearLayout
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeWithinAndroidViewsInteropTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun moveBetweenEmbeddedCousinsNext() {
        // Arrange.
        lateinit var button2: Button
        lateinit var button3: Button
        rule.activityRule.withActivity {
            setContentView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        ComposeView(context).also { composeView ->
                            composeView.setContent {
                                AndroidView({
                                    LinearLayout(it).apply {
                                        orientation = LinearLayout.VERTICAL
                                        addView(
                                            Button(context).apply {
                                                setText("Button 1")
                                                isFocusableInTouchMode = true
                                            }
                                        )
                                        addView(
                                            Button(context).apply {
                                                setText("Button 2")
                                                isFocusable = true
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
                        ComposeView(context).also { composeView ->
                            composeView.setContent {
                                AndroidView({
                                    LinearLayout(it).apply {
                                        orientation = LinearLayout.VERTICAL
                                        addView(
                                            Button(context).apply {
                                                setText("Button 3")
                                                isFocusableInTouchMode = true
                                                button3 = this
                                            }
                                        )
                                        addView(
                                            Button(context).apply {
                                                setText("Button 4")
                                                isFocusableInTouchMode = true
                                            }
                                        )
                                    }
                                })
                            }
                        }
                    )
                }
            )
        }
        rule.runOnIdle { button2.requestFocus() }

        // Act.
        instrumentation.sendKeySync(AndroidKeyEvent(ACTION_DOWN, Key.Tab.nativeKeyCode))

        // Assert.
        rule.runOnIdle {
            assertThat(button2.isFocused).isFalse()
            assertThat(button3.isFocused).isTrue()
        }
    }

    @Test
    fun moveBetweenEmbeddedCousinsPrevious() {
        // Arrange.
        lateinit var button2: Button
        lateinit var button3: Button
        rule.activityRule.withActivity {
            setContentView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        ComposeView(context).also { composeView ->
                            composeView.setContent {
                                AndroidView({
                                    LinearLayout(it).apply {
                                        orientation = LinearLayout.VERTICAL
                                        addView(
                                            Button(context).apply {
                                                setText("Button 1")
                                                isFocusableInTouchMode = true
                                            }
                                        )
                                        addView(
                                            Button(context).apply {
                                                setText("Button 2")
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
                        ComposeView(context).also { composeView ->
                            composeView.setContent {
                                AndroidView({
                                    LinearLayout(it).apply {
                                        orientation = LinearLayout.VERTICAL
                                        addView(
                                            Button(context).apply {
                                                setText("Button 3")
                                                isFocusableInTouchMode = true
                                                button3 = this
                                            }
                                        )
                                        addView(
                                            Button(context).apply {
                                                setText("Button 4")
                                                isFocusableInTouchMode = true
                                            }
                                        )
                                    }
                                })
                            }
                        }
                    )
                }
            )
        }
        rule.runOnIdle { button3.requestFocus() }

        // Act.
        instrumentation.sendKeySync(
            AndroidKeyEvent(0, 0, ACTION_DOWN, Key.Tab.nativeKeyCode, 0, META_SHIFT_ON)
        )

        // Assert.
        rule.runOnIdle {
            assertThat(button3.isFocused).isFalse()
            assertThat(button2.isFocused).isTrue()
        }
    }
}
