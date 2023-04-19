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
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.ImageViewSubject.Companion.assertThat
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.configurationContext
import androidx.glance.appwidget.findView
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.SwitchDefaults
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

@Config(minSdk = 23, maxSdk = 30)
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SwitchBackportTranslatorTest {

    private lateinit var fakeCoroutineScope: TestScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    private val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun canTranslateSwitch_fixed_unchecked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(Color.Blue),
                    uncheckedThumbColor = ColorProvider(Color.Red),
                    checkedTrackColor = ColorProvider(Color.Green),
                    uncheckedTrackColor = ColorProvider(Color.Yellow)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Red)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.Yellow)
    }

    @Test
    fun canTranslateSwitch_fixed_checked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(Color.Blue),
                    uncheckedThumbColor = ColorProvider(Color.Red),
                    checkedTrackColor = ColorProvider(Color.Green),
                    uncheckedTrackColor = ColorProvider(Color.Yellow)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Blue)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.Green)
    }

    @Test
    fun canTranslateSwitch_dayNight_unchecked_day() = fakeCoroutineScope.runTest {
        val thumbColor = Color.Blue
        val trackColor = Color.Cyan
        val otherColor = Color.White

        val rv = lightContext.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(otherColor),
                    uncheckedThumbColor = ColorProvider(day = thumbColor, night = otherColor),
                    checkedTrackColor = ColorProvider(otherColor),
                    uncheckedTrackColor = ColorProvider(day = trackColor, night = otherColor)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(thumbColor)
        assertThat(switchRoot.trackImageView).hasColorFilter(trackColor)
    }

    @Test
    fun canTranslateSwitch_dayNight_unchecked_night() = fakeCoroutineScope.runTest {
        val thumbColor = Color.Blue
        val trackColor = Color.Cyan
        val otherColor = Color.White

        val rv = darkContext.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(otherColor),
                    uncheckedThumbColor = ColorProvider(day = otherColor, night = thumbColor),
                    checkedTrackColor = ColorProvider(otherColor),
                    uncheckedTrackColor = ColorProvider(day = otherColor, night = trackColor)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(thumbColor)
        assertThat(switchRoot.trackImageView).hasColorFilter(trackColor)
    }

    @Test
    fun canTranslateSwitch_dayNight_checked_day() = fakeCoroutineScope.runTest {
        val thumbColor = Color.Blue
        val trackColor = Color.Cyan
        val otherColor = Color.White

        val rv = lightContext.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(day = thumbColor, night = otherColor),
                    uncheckedThumbColor = ColorProvider(day = otherColor, night = otherColor),
                    checkedTrackColor = ColorProvider(day = trackColor, night = otherColor),
                    uncheckedTrackColor = ColorProvider(day = otherColor, night = otherColor)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(thumbColor)
        assertThat(switchRoot.trackImageView).hasColorFilter(trackColor)
    }

    @Test
    fun canTranslateSwitch_dayNight_checked_night() = fakeCoroutineScope.runTest {
        val thumbColor = Color.Blue
        val trackColor = Color.Cyan
        val otherColor = Color.White

        val rv = darkContext.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(day = otherColor, night = thumbColor),
                    uncheckedThumbColor = ColorProvider(otherColor),
                    checkedTrackColor = ColorProvider(day = otherColor, night = trackColor),
                    uncheckedTrackColor = ColorProvider(otherColor)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(thumbColor)
        assertThat(switchRoot.trackImageView).hasColorFilter(trackColor)
    }

    @Test
    fun canTranslateSwitch_resource_unchecked() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(Color.Magenta),
                    uncheckedThumbColor = ColorProvider(Color.Red),
                    checkedTrackColor = ColorProvider(Color.Magenta),
                    uncheckedTrackColor = ColorProvider(Color.Blue)
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Red)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.Blue)
    }

    @Test
    fun canTranslateSwitch_resource_checked() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(Color.Red),
                    uncheckedThumbColor = ColorProvider(Color.Magenta),
                    checkedTrackColor = ColorProvider(Color.Blue),
                    uncheckedTrackColor = ColorProvider(Color.Magenta),
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Red)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.Blue)
    }

    @Test
    fun canTranslateSwitch_onCheckedChange_null() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
            )
        }

        val switchRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(switchRoot.hasOnClickListeners()).isFalse()
    }

    @Test
    fun canTranslateSwitch_onCheckedChange_withAction() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = actionRunCallback<ActionCallback>(),
                text = "Switch",
            )
        }

        val switchRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(switchRoot.hasOnClickListeners()).isTrue()
    }

    @Test
    fun canTranslateSwitchWithSemanticsModifier_contentDescription() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = actionRunCallback<ActionCallback>(),
                text = "Switch",
                modifier = GlanceModifier.semantics {
                    contentDescription = "Custom switch description"
                },
            )
        }

        val switchRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(switchRoot.contentDescription).isEqualTo("Custom switch description")
    }

    private val ViewGroup.thumbImageView: ImageView?
        get() = findView {
            shadowOf(it.drawable).createdFromResId ==
                androidx.glance.appwidget.R.drawable.glance_switch_thumb_animated
        }

    private val ViewGroup.trackImageView: ImageView?
        get() = findView {
            shadowOf(it.drawable).createdFromResId ==
                androidx.glance.appwidget.R.drawable.glance_switch_track
        }
}