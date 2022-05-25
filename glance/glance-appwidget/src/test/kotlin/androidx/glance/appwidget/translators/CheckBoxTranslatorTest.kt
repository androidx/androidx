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

package androidx.glance.appwidget.translators

import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckBoxColors
import androidx.glance.appwidget.ImageViewSubject.Companion.assertThat
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.configurationContext
import androidx.glance.appwidget.findViewByType
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.test.R
import androidx.glance.appwidget.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CheckBoxTranslatorTest {

    private lateinit var fakeCoroutineScope: TestScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    private val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resolved_unchecked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = false,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(checkedColor = Color.Red, uncheckedColor = Color.Blue)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Blue)
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resolved_checked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(checkedColor = Color.Red, uncheckedColor = Color.Blue)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Red)
    }

    @Config(sdk = [29])
    @Test
    fun canTranslateCheckBox_dayNight_unchecked_day() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate {
            CheckBox(
                checked = false,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Blue),
                    uncheckedColor = ColorProvider(day = Color.Yellow, night = Color.Green)
                )
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Yellow)
    }

    @Config(sdk = [29])
    @Test
    fun canTranslateCheckBox_dayNight_unchecked_night() = fakeCoroutineScope.runTest {
        val rv = darkContext.runAndTranslate {
            CheckBox(
                checked = false,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Blue),
                    uncheckedColor = ColorProvider(day = Color.Yellow, night = Color.Green)
                )
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Green)
    }

    @Config(sdk = [29])
    @Test
    fun canTranslateCheckBox_dayNight_checked_day() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Blue),
                    uncheckedColor = ColorProvider(day = Color.Yellow, night = Color.Green)
                )
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Red)
    }

    @Config(sdk = [29])
    @Test
    fun canTranslateCheckBox_dayNight_checked_night() = fakeCoroutineScope.runTest {
        val rv = darkContext.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Blue),
                    uncheckedColor = ColorProvider(day = Color.Yellow, night = Color.Green)
                )
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Blue)
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resource_checked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = null,
                text = "Check",
                colors = CheckBoxColors(R.color.my_checkbox_colors)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter("#FF0000")
    }

    @Config(sdk = [29])
    @Test
    fun canTranslateCheckBox_onCheckedChange_null() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = null,
                text = "CheckBox",
            )
        }

        val checkboxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(checkboxRoot.hasOnClickListeners()).isFalse()
    }

    @Config(sdk = [29])
    @Test
    fun canTranslateCheckBox_onCheckedChange_withAction() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = actionRunCallback<ActionCallback>(),
                text = "CheckBox",
            )
        }

        val checkboxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(checkboxRoot.hasOnClickListeners()).isTrue()
    }
}