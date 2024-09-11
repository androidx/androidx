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
        UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            context.resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options =
                listOf(
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        UserStyleSetting.Option.Id("red_style"),
                        context.resources,
                        R.string.red_style_name,
                        R.string.red_style_name,
                        null
                    ),
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        UserStyleSetting.Option.Id("green_style"),
                        context.resources,
                        R.string.green_style_name,
                        R.string.green_style_name,
                        null
                    )
                ),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )

    @Test
    public fun stringResources_en() {
        assertThat(colorStyleSetting.displayName).isEqualTo("Colors")
        assertThat(colorStyleSetting.description).isEqualTo("Watchface colorization")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("red_style"))
                        as UserStyleSetting.ListUserStyleSetting.ListOption)
                    .displayName
            )
            .isEqualTo("Red Style")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("green_style"))
                        as UserStyleSetting.ListUserStyleSetting.ListOption)
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
                        as UserStyleSetting.ListUserStyleSetting.ListOption)
                    .displayName
            )
            .isEqualTo("Stile rosso")

        assertThat(
                (colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("green_style"))
                        as UserStyleSetting.ListUserStyleSetting.ListOption)
                    .displayName
            )
            .isEqualTo("Stile verde")
    }

    @Test
    public fun listOptionsWithIndices() {
        val listUserStyleSetting =
            ListUserStyleSetting(
                UserStyleSetting.Id("list"),
                context.resources,
                R.string.colors_style_setting,
                R.string.colors_style_setting_description,
                icon = null,
                options =
                    listOf(
                        ListOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            R.string.ith_option,
                            R.string.ith_option_screen_reader_name,
                            icon = null
                        ),
                        ListOption(
                            UserStyleSetting.Option.Id("two"),
                            context.resources,
                            R.string.ith_option,
                            R.string.ith_option_screen_reader_name,
                            icon = null
                        ),
                        ListOption(
                            UserStyleSetting.Option.Id("three"),
                            context.resources,
                            R.string.ith_option,
                            R.string.ith_option_screen_reader_name,
                            icon = null
                        )
                    ),
                listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
            )

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
            ListUserStyleSetting(
                UserStyleSetting.Id("list"),
                context.resources,
                R.string.colors_style_setting,
                R.string.colors_style_setting_description,
                icon = null,
                options =
                    listOf(
                        ListOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            R.string.ith_option,
                            icon = null
                        )
                    ),
                listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
            )

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
            ComplicationSlotsUserStyleSetting(
                UserStyleSetting.Id("complications_style_setting1"),
                displayName = "Complications",
                description = "Number and position",
                icon = null,
                complicationConfig =
                    listOf(
                        ComplicationSlotsOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            R.string.ith_option,
                            R.string.ith_option_screen_reader_name,
                            icon = null,
                            emptyList()
                        ),
                        ComplicationSlotsOption(
                            UserStyleSetting.Option.Id("two"),
                            context.resources,
                            R.string.ith_option,
                            R.string.ith_option_screen_reader_name,
                            icon = null,
                            emptyList()
                        ),
                        ComplicationSlotsOption(
                            UserStyleSetting.Option.Id("three"),
                            context.resources,
                            R.string.ith_option,
                            R.string.ith_option_screen_reader_name,
                            icon = null,
                            emptyList()
                        )
                    ),
                listOf(WatchFaceLayer.COMPLICATIONS)
            )

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
                    ListUserStyleSetting(
                        one,
                        context.resources,
                        R.string.ith_style,
                        R.string.ith_style_screen_reader_name,
                        icon = null,
                        options =
                            listOf(
                                ListOption(
                                    UserStyleSetting.Option.Id("one"),
                                    context.resources,
                                    R.string.ith_option,
                                    R.string.ith_option_screen_reader_name,
                                    icon = null
                                )
                            ),
                        listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
                    ),
                    ComplicationSlotsUserStyleSetting(
                        two,
                        context.resources,
                        R.string.ith_style,
                        R.string.ith_style_screen_reader_name,
                        icon = null,
                        complicationConfig =
                            listOf(
                                ComplicationSlotsOption(
                                    UserStyleSetting.Option.Id("one"),
                                    context.resources,
                                    R.string.ith_option,
                                    R.string.ith_option_screen_reader_name,
                                    icon = null,
                                    emptyList()
                                )
                            ),
                        listOf(WatchFaceLayer.COMPLICATIONS)
                    )
                )
            )

        assertThat(schema[one]!!.displayName).isEqualTo("1st style")
        assertThat(schema[one]!!.description).isEqualTo("1st style setting")
        assertThat(schema[two]!!.displayName).isEqualTo("2nd style")
        assertThat(schema[two]!!.description).isEqualTo("2nd style setting")
    }

    @Test
    @Suppress("deprecation")
    public fun complicationsUserStyleSettingWireFormatRoundTrip_noScreenReaderName_filledByDisplayName() {
        val complicationSetting =
            ComplicationSlotsUserStyleSetting(
                UserStyleSetting.Id("complications_style_setting1"),
                displayName = "Complications",
                description = "Number and position",
                icon = null,
                complicationConfig =
                    listOf(
                        ComplicationSlotsOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            displayNameResourceId = R.string.ith_option,
                            icon = null,
                            emptyList()
                        )
                    ),
                listOf(WatchFaceLayer.COMPLICATIONS)
            )

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
            UserStyleSetting.BooleanUserStyleSetting(
                UserStyleSetting.Id("setting"),
                context.resources,
                displayNameResourceId = 10,
                descriptionResourceId = 11,
                iconProvider = { icon_10x10 },
                affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS),
                defaultValue = true
            )

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun complicationSlotsUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            ComplicationSlotsUserStyleSetting(
                UserStyleSetting.Id("complications_style_setting1"),
                context.resources,
                displayNameResourceId = 10,
                descriptionResourceId = 11,
                iconProvider = { icon_10x10 },
                complicationConfig =
                    listOf(
                        ComplicationSlotsOption(
                            UserStyleSetting.Option.Id("one"),
                            context.resources,
                            displayNameResourceId = R.string.ith_option,
                            screenReaderNameResourceId = R.string.ith_option_screen_reader_name,
                            iconProvider = { icon_10x10 },
                            emptyList()
                        )
                    ),
                listOf(WatchFaceLayer.COMPLICATIONS)
            )

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun complicationSlotsOption_lazyIcon() {
        val userStyleOption =
            ComplicationSlotsOption(
                UserStyleSetting.Option.Id("one"),
                context.resources,
                displayNameResourceId = R.string.ith_option,
                screenReaderNameResourceId = R.string.ith_option_screen_reader_name,
                iconProvider = { icon_10x10 },
                emptyList()
            )

        assertThat(userStyleOption.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun doubleRangeUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            UserStyleSetting.DoubleRangeUserStyleSetting(
                UserStyleSetting.Id("setting"),
                context.resources,
                displayNameResourceId = 10,
                descriptionResourceId = 11,
                iconProvider = { icon_10x10 },
                0.0,
                1.0,
                listOf(WatchFaceLayer.BASE),
                defaultValue = 0.75
            )

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun longRangeUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            UserStyleSetting.LongRangeUserStyleSetting(
                UserStyleSetting.Id("setting"),
                context.resources,
                displayNameResourceId = 10,
                descriptionResourceId = 11,
                iconProvider = { icon_10x10 },
                0,
                100,
                listOf(WatchFaceLayer.BASE),
                defaultValue = 75
            )

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun listUserStyleSetting_lazyIcon() {
        val userStyleSetting =
            UserStyleSetting.ListUserStyleSetting(
                UserStyleSetting.Id("setting"),
                context.resources,
                displayNameResourceId = 10,
                descriptionResourceId = 11,
                iconProvider = { icon_10x10 },
                options =
                    listOf(
                        ListOption(
                            UserStyleSetting.Option.Id("red_style"),
                            context.resources,
                            displayNameResourceId = R.string.red_style_name,
                            screenReaderNameResourceId = R.string.red_style_name,
                            iconProvider = { null }
                        )
                    ),
                listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
            )

        assertThat(userStyleSetting.icon).isEqualTo(icon_10x10)
    }

    @Test
    public fun listOption_lazyIcon() {
        val userStyleOption =
            ListOption(
                UserStyleSetting.Option.Id("red_style"),
                context.resources,
                displayNameResourceId = R.string.red_style_name,
                screenReaderNameResourceId = R.string.red_style_name,
                iconProvider = { icon_10x10 }
            )

        assertThat(userStyleOption.icon).isEqualTo(icon_10x10)
    }
}
