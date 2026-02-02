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

package androidx.glance.appwidget.translators

import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.ImageViewSubject.Companion.assertThat
import androidx.glance.appwidget.RadioButton
import androidx.glance.appwidget.RadioButtonDefaults
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.configurationContext
import androidx.glance.appwidget.findView
import androidx.glance.appwidget.findViewByType
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.color.ColorProvider
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(maxSdk = 30)
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RadioButtonBackportTranslatorTest {

    private lateinit var fakeCoroutineScope: TestScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    private val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun canTranslateRadioButton_fixed_unchecked() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(
                        checked = false,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(Color.Blue),
                                uncheckedColor = ColorProvider(Color.Red),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Red)
        }

    @Test
    fun canTranslateRadioButton_fixed_checked() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(Color.Blue),
                                uncheckedColor = ColorProvider(Color.Red),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Blue)
        }

    @Test
    fun canTranslateRadioButton_dayNight_unchecked_day() =
        fakeCoroutineScope.runTest {
            val rv =
                lightContext.runAndTranslate {
                    RadioButton(
                        checked = false,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(day = Color.Blue, night = Color.Red),
                                uncheckedColor =
                                    ColorProvider(day = Color.Green, night = Color.Yellow),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Green)
        }

    @Test
    fun canTranslateRadioButton_dayNight_unchecked_night() =
        fakeCoroutineScope.runTest {
            val rv =
                darkContext.runAndTranslate {
                    RadioButton(
                        checked = false,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(day = Color.Blue, night = Color.Red),
                                uncheckedColor =
                                    ColorProvider(day = Color.Green, night = Color.Yellow),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Yellow)
        }

    @Test
    fun canTranslateRadioButton_dayNight_checked_day() =
        fakeCoroutineScope.runTest {
            val rv =
                lightContext.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(day = Color.Blue, night = Color.Red),
                                uncheckedColor =
                                    ColorProvider(day = Color.Green, night = Color.Yellow),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Blue)
        }

    @Test
    fun canTranslateRadioButton_dayNight_checked_night() =
        fakeCoroutineScope.runTest {
            val rv =
                darkContext.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(day = Color.Blue, night = Color.Red),
                                uncheckedColor =
                                    ColorProvider(day = Color.Green, night = Color.Yellow),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Red)
        }

    @Test
    fun canTranslateRadioButton_resource_unchecked() =
        fakeCoroutineScope.runTest {
            val rv =
                lightContext.runAndTranslate {
                    RadioButton(
                        checked = false,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(day = Color.Blue, night = Color.Red),
                                uncheckedColor =
                                    ColorProvider(day = Color.Green, night = Color.Yellow),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Green)
        }

    @Test
    fun canTranslateRadioButton_resource_checked() =
        fakeCoroutineScope.runTest {
            val rv =
                lightContext.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = null,
                        text = "RadioButton",
                        colors =
                            RadioButtonDefaults.colors(
                                checkedColor = ColorProvider(day = Color.Blue, night = Color.Red),
                                uncheckedColor =
                                    ColorProvider(day = Color.Green, night = Color.Yellow),
                            ),
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
            assertThat(radioButtonRoot.radioImageView).hasColorFilter(Color.Blue)
        }

    @Test
    fun canTranslateRadioButton_onClick_null() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(checked = true, onClick = null, text = "RadioButton")
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            assertThat(radioButtonRoot.hasOnClickListeners()).isFalse()
        }

    @Test
    fun canTranslateRadioButton_onClick_withAction() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = actionRunCallback<ActionCallback>(),
                        text = "RadioButton",
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            assertThat(radioButtonRoot.hasOnClickListeners()).isTrue()
        }

    @Test
    fun canTranslateRadioButton_text() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = actionRunCallback<ActionCallback>(),
                        text = "RadioButton",
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            val text = radioButtonRoot.findViewByType<TextView>()
            assertThat(text).isNotNull()
            assertThat(text?.text).isEqualTo("RadioButton")
        }

    @Test
    fun canTranslateRadioButton_disabled() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = actionRunCallback<ActionCallback>(),
                        enabled = false,
                        text = "RadioButton",
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            assertThat(radioButtonRoot.hasOnClickListeners()).isFalse()
        }

    @Test
    fun canTranslateRadioButtonWithSemanticsModifier_contentDescription() =
        fakeCoroutineScope.runTest {
            val rv =
                context.runAndTranslate {
                    RadioButton(
                        checked = true,
                        onClick = actionRunCallback<ActionCallback>(),
                        text = "RadioButton",
                        modifier =
                            GlanceModifier.semantics {
                                contentDescription = "Custom radio button description"
                            },
                    )
                }

            val radioButtonRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
            assertThat(radioButtonRoot.contentDescription)
                .isEqualTo("Custom radio button description")
        }

    private val ViewGroup.radioImageView: ImageView?
        get() = findView {
            shadowOf(it.drawable).createdFromResId ==
                androidx.glance.appwidget.R.drawable.glance_btn_radio_material_anim
        }
}
