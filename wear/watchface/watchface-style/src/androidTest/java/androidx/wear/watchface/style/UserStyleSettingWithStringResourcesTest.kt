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

package androidx.wear.watchface.style

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.test.R
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
public class UserStyleSettingWithStringResourcesTest {

    private val context =
        ApplicationProvider.getApplicationContext<Context>()
            .createConfigurationContext(
                ApplicationProvider.getApplicationContext<Context>().resources.configuration.apply {
                    setLocale(Locale.ENGLISH)
                }
            )

    private val icon_10x10 =
        Icon.createWithBitmap(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))

    private val colorStyleSetting =
        ListUserStyleSetting.Builder(
                UserStyleSetting.Id("color_style_setting"),
                options =
                    listOf(
                        ListOption.Builder(
                                UserStyleSetting.Option.Id("red_style"),
                                context.resources,
                                R.string.red_style_name,
                                R.string.red_style_name
                            )
                            .build(),
                        ListOption.Builder(
                                UserStyleSetting.Option.Id("green_style"),
                                context.resources,
                                R.string.green_style_name,
                                R.string.green_style_name
                            )
                            .build()
                    ),
                listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY),
                context.resources,
                R.string.colors_style_setting,
                R.string.colors_style_setting_description
            )
            .build()

    @Test
    public fun stringResources_en() {
        assertThat(colorStyleSetting.displayName).isEqualTo("Colors")
        assertThat(colorStyleSetting.description).isEqualTo("Watchface colorization")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("red_style"))
                        as ListOption)
                    .displayName
            )
            .isEqualTo("Red Style")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("green_style"))
                        as ListOption)
                    .displayName
            )
            .isEqualTo("Green Style")
    }

    // We do want to call updateConfiguration here to test what happens when the locale changes.
    @Suppress("deprecation")
    @Test
    public fun stringResources_it() {
        context.resources.updateConfiguration(
            context.resources.configuration.apply { setLocale(Locale.ITALIAN) },
            context.resources.displayMetrics
        )
        assertThat(colorStyleSetting.displayName).isEqualTo("Colori")
        assertThat(colorStyleSetting.description).isEqualTo("Colorazione del quadrante")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("red_style"))
                        as ListOption)
                    .displayName
            )
            .isEqualTo("Stile rosso")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("green_style"))
                        as ListOption)
                    .displayName
            )
            .isEqualTo("Stile verde")
    }

    @Test
    public fun listOptionsWithIndices() {
        val listUserStyleSetting =
            ListUserStyleSetting.Builder(
                    UserStyleSetting.Id("list"),
                    listOf(
                        ListOption.Builder(
                                UserStyleSetting.Option.Id("one"),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name,
                            )
                            .build(),
                        ListOption.Builder(
                                UserStyleSetting.Option.Id("two"),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name
                            )
                            .build(),
                        ListOption.Builder(
                                UserStyleSetting.Option.Id("three"),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name
                            )
                            .build()
                    ),
                    listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY),
                    context.resources,
                    R.string.colors_style_setting,
                    R.string.colors_style_setting_description
                )
                .build()

        val option0 = listUserStyleSetting.options[0] as ListOption
        assertThat(option0.displayName).isEqualTo("1st option")
        assertThat(option0.screenReaderName).isEqualTo("1st list option")

        val option1 = listUserStyleSetting.options[1] as ListOption
        assertThat(option1.displayName).isEqualTo("2nd option")
        assertThat(option1.screenReaderName).isEqualTo("2nd list option")

        val option2 = listUserStyleSetting.options[2] as ListOption
        assertThat(option2.displayName).isEqualTo("3rd option")
        assertThat(option2.screenReaderName).isEqualTo("3rd list option")
    }

    @Test
    @Suppress("deprecation")
    public fun listUserStyleSettingWireFormatRoundTrip_noScreenReaderName() {
        val listUserStyleSetting =
            ListUserStyleSetting.Builder(
                    UserStyleSetting.Id("list"),
                    listOf(
                        ListOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            R.string.ith_option,
                            icon = null
                        )
                    ),
                    listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY),
                    context.resources,
                    R.string.colors_style_setting,
                    R.string.colors_style_setting_description,
                )
                .build()

        val listUserStyleSettingAfterRoundTrip =
            ListUserStyleSetting(listUserStyleSetting.toWireFormat())

        val option0 = listUserStyleSettingAfterRoundTrip.options[0] as ListOption
        assertThat(option0.displayName).isEqualTo("1st option")
        // We expect screenReaderName to be back filled by the displayName.
        assertThat(option0.screenReaderName).isEqualTo("1st option")
    }

    @Test
    public fun complicationSlotsOptionsWithIndices() {
        val complicationSetting =
            ComplicationSlotsUserStyleSetting.Builder(
                    UserStyleSetting.Id("complications_style_setting1"),
                    listOf(
                        ComplicationSlotsOption.Builder(
                                UserStyleSetting.Option.Id("one"),
                                emptyList(),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name
                            )
                            .build(),
                        ComplicationSlotsOption.Builder(
                                UserStyleSetting.Option.Id("two"),
                                emptyList(),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name
                            )
                            .build(),
                        ComplicationSlotsOption.Builder(
                                UserStyleSetting.Option.Id("three"),
                                emptyList(),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name,
                            )
                            .build()
                    ),
                    listOf(WatchFaceLayer.COMPLICATIONS),
                    "Complications",
                    "Number and position"
                )
                .build()

        val option0 = complicationSetting.options[0] as ComplicationSlotsOption
        assertThat(option0.displayName).isEqualTo("1st option")
        assertThat(option0.screenReaderName).isEqualTo("1st list option")

        val option1 = complicationSetting.options[1] as ComplicationSlotsOption
        assertThat(option1.displayName).isEqualTo("2nd option")
        assertThat(option1.screenReaderName).isEqualTo("2nd list option")

        val option2 = complicationSetting.options[2] as ComplicationSlotsOption
        assertThat(option2.displayName).isEqualTo("3rd option")
        assertThat(option2.screenReaderName).isEqualTo("3rd list option")
    }

    @Test
    public fun complicationSettingsWithIndices() {
        val one = UserStyleSetting.Id("one")
        val two = UserStyleSetting.Id("two")
        val schema =
            UserStyleSchema(
                listOf(
                    ListUserStyleSetting.Builder(
                            one,
                            listOf(
                                ListOption.Builder(
                                        UserStyleSetting.Option.Id("one"),
                                        context.resources,
                                        R.string.ith_option,
                                        R.string.ith_option_screen_reader_name,
                                    )
                                    .build()
                            ),
                            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY),
                            context.resources,
                            R.string.ith_style,
                            R.string.ith_style_screen_reader_name,
                        )
                        .build(),
                    ComplicationSlotsUserStyleSetting.Builder(
                            two,
                            listOf(
                                ComplicationSlotsOption.Builder(
                                        UserStyleSetting.Option.Id("one"),
                                        emptyList(),
                                        context.resources,
                                        R.string.ith_option,
                                        R.string.ith_option_screen_reader_name
                                    )
                                    .build()
                            ),
                            listOf(WatchFaceLayer.COMPLICATIONS),
                            context.resources,
                            R.string.ith_style,
                            R.string.ith_style_screen_reader_name
                        )
                        .build()
                )
            )

        assertThat(schema[one]!!.displayName).isEqualTo("1st style")
        assertThat(schema[one]!!.description).isEqualTo("1st style setting")
        assertThat(schema[two]!!.displayName).isEqualTo("2nd style")
        assertThat(schema[two]!!.description).isEqualTo("2nd style setting")
    }

    @Test
    public fun complicationsUserStyleSettingWireFormatRoundTrip_noScreenReaderName_filledByDisplayName() {
        val complicationSetting =
            ComplicationSlotsUserStyleSetting.Builder(
                    UserStyleSetting.Id("complications_style_setting1"),
                    listOf(
                        @Suppress("deprecation")
                        ComplicationSlotsOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            displayNameResourceId = R.string.ith_option,
                            icon = null,
                            emptyList()
                        )
                    ),
                    listOf(WatchFaceLayer.COMPLICATIONS),
                    "Complications",
                    "Number and position"
                )
                .build()

        val complicationSettingAfterRoundTrip =
            ComplicationSlotsUserStyleSetting(complicationSetting.toWireFormat())

        val option0 = complicationSettingAfterRoundTrip.options[0] as ComplicationSlotsOption
        assertThat(option0.displayName).isEqualTo("1st option")
        // We expect screenReaderName to be back filled by the displayName.
        assertThat(option0.screenReaderName).isEqualTo("1st option")
    }

    @Test
    public fun booleanUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            UserStyleSetting.BooleanUserStyleSetting.Builder(
                    UserStyleSetting.Id("setting"),
                    affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS),
                    defaultValue = true,
                    context.resources,
                    10,
                    11
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun complicationSlotsUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            ComplicationSlotsUserStyleSetting.Builder(
                    UserStyleSetting.Id("complications_style_setting1"),
                    listOf(
                        ComplicationSlotsOption.Builder(
                                UserStyleSetting.Option.Id("one"),
                                emptyList(),
                                context.resources,
                                R.string.ith_option,
                                R.string.ith_option_screen_reader_name
                            )
                            .setIcon { icon_10x10 }
                            .build()
                    ),
                    listOf(WatchFaceLayer.COMPLICATIONS),
                    context.resources,
                    10,
                    11
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun complicationSlotsOption_lazyIcon() {
        val userStyleOption =
            ComplicationSlotsOption.Builder(
                    UserStyleSetting.Option.Id("one"),
                    emptyList(),
                    context.resources,
                    R.string.ith_option,
                    R.string.ith_option_screen_reader_name
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleOption.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun doubleRangeUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            UserStyleSetting.DoubleRangeUserStyleSetting.Builder(
                    UserStyleSetting.Id("setting"),
                    0.0,
                    1.0,
                    defaultValue = 0.75,
                    listOf(WatchFaceLayer.BASE),
                    context.resources,
                    10,
                    11
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun longRangeUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            UserStyleSetting.LongRangeUserStyleSetting.Builder(
                    UserStyleSetting.Id("setting"),
                    0,
                    100,
                    defaultValue = 75,
                    listOf(WatchFaceLayer.BASE),
                    context.resources,
                    10,
                    11
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun listUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            ListUserStyleSetting.Builder(
                    UserStyleSetting.Id("setting"),
                    listOf(
                        ListOption.Builder(
                                UserStyleSetting.Option.Id("red_style"),
                                context.resources,
                                R.string.red_style_name,
                                R.string.red_style_name
                            )
                            .setIcon { icon_10x10 }
                            .build()
                    ),
                    listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY),
                    context.resources,
                    10,
                    11
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun listOption_lazyIcon() {
        val userStyleOption =
            ListOption.Builder(
                    UserStyleSetting.Option.Id("red_style"),
                    context.resources,
                    R.string.red_style_name,
                    R.string.red_style_name
                )
                .setIcon { icon_10x10 }
                .build()

        assertThat(userStyleOption.icon).isEqualTo(icon_10x10)
    }
}
