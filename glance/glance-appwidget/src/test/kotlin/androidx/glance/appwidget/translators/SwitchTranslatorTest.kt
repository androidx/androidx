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
import androidx.glance.appwidget.ImageViewSubject.Companion.assertThat
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.SwitchColors
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.configurationContext
import androidx.glance.appwidget.findView
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.test.R
import androidx.glance.appwidget.unit.ColorProvider
import androidx.glance.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertIs

@Config(sdk = [29])
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SwitchTranslatorTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    private val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun canTranslateSwitch_fixed_unchecked() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
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
    fun canTranslateSwitch_fixed_checked() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
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
    fun canTranslateSwitch_dayNight_unchecked_day() = fakeCoroutineScope.runBlockingTest {
        val rv = lightContext.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
                    checkedThumbColor = ColorProvider(day = Color.Blue, night = Color.Red),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Yellow),
                    checkedTrackColor = ColorProvider(day = Color.White, night = Color.Black),
                    uncheckedTrackColor = ColorProvider(
                        day = Color.DarkGray,
                        night = Color.LightGray
                    )
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Green)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.DarkGray)
    }

    @Test
    fun canTranslateSwitch_dayNight_unchecked_night() = fakeCoroutineScope.runBlockingTest {
        val rv = darkContext.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
                    checkedThumbColor = ColorProvider(day = Color.Blue, night = Color.Red),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Yellow),
                    checkedTrackColor = ColorProvider(day = Color.White, night = Color.Black),
                    uncheckedTrackColor = ColorProvider(
                        day = Color.DarkGray,
                        night = Color.LightGray
                    )
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Yellow)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.LightGray)
    }

    @Test
    fun canTranslateSwitch_dayNight_checked_day() = fakeCoroutineScope.runBlockingTest {
        val rv = lightContext.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
                    checkedThumbColor = ColorProvider(day = Color.Blue, night = Color.Red),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Yellow),
                    checkedTrackColor = ColorProvider(day = Color.White, night = Color.Black),
                    uncheckedTrackColor = ColorProvider(
                        day = Color.DarkGray,
                        night = Color.LightGray
                    )
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Blue)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.White)
    }

    @Test
    fun canTranslateSwitch_dayNight_checked_night() = fakeCoroutineScope.runBlockingTest {
        val rv = darkContext.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
                    checkedThumbColor = ColorProvider(day = Color.Blue, night = Color.Red),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Yellow),
                    checkedTrackColor = ColorProvider(day = Color.White, night = Color.Black),
                    uncheckedTrackColor = ColorProvider(
                        day = Color.DarkGray,
                        night = Color.LightGray
                    )
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(darkContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter(Color.Red)
        assertThat(switchRoot.trackImageView).hasColorFilter(Color.Black)
    }

    @Test
    fun canTranslateSwitch_resource_unchecked() = fakeCoroutineScope.runBlockingTest {
        val rv = lightContext.runAndTranslate {
            Switch(
                checked = false,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
                    thumbColor = R.color.my_switch_thumb_colors,
                    trackColor = R.color.my_switch_track_colors
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter("#110FF0FF")
        assertThat(switchRoot.trackImageView).hasColorFilter("#220FF0FF")
    }

    @Test
    fun canTranslateSwitch_resource_checked() = fakeCoroutineScope.runBlockingTest {
        val rv = lightContext.runAndTranslate {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Switch",
                colors = SwitchColors(
                    thumbColor = R.color.my_switch_thumb_colors,
                    trackColor = R.color.my_switch_track_colors
                )
            )
        }

        val switchRoot = assertIs<ViewGroup>(lightContext.applyRemoteViews(rv))
        assertThat(switchRoot.thumbImageView).hasColorFilter("#11040040")
        assertThat(switchRoot.trackImageView).hasColorFilter("#22040040")
    }

    @Test
    fun canTranslateSwitch_onCheckedChange_null() = fakeCoroutineScope.runBlockingTest {
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
    fun canTranslateSwitch_onCheckedChange_withAction() = fakeCoroutineScope.runBlockingTest {
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

    private val ViewGroup.thumbImageView: ImageView?
        get() = findView {
            shadowOf(it.drawable).createdFromResId ==
                androidx.glance.appwidget.R.drawable.switch_thumb_animated
        }

    private val ViewGroup.trackImageView: ImageView?
        get() = findView {
            shadowOf(it.drawable).createdFromResId ==
                androidx.glance.appwidget.R.drawable.switch_track
        }
}