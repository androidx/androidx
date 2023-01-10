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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.style.test.R
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@MediumTest
public class UserStyleSettingWithStringResourcesTest {

    private val context =
        ApplicationProvider.getApplicationContext<Context>().createConfigurationContext(
            ApplicationProvider.getApplicationContext<Context>().resources.configuration.apply {
                setLocale(Locale.ENGLISH)
            }
        )

    private val colorStyleSetting = UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id("color_style_setting"),
        context.resources,
        R.string.colors_style_setting,
        R.string.colors_style_setting_description,
        icon = null,
        options = listOf(
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
        Truth.assertThat(colorStyleSetting.displayName).isEqualTo("Colors")
        Truth.assertThat(colorStyleSetting.description).isEqualTo("Watchface colorization")

        Truth.assertThat(
            (
                colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("red_style")) as
                    UserStyleSetting.ListUserStyleSetting.ListOption
                ).displayName
        ).isEqualTo("Red Style")

        Truth.assertThat(
            (
                colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("green_style")) as
                    UserStyleSetting.ListUserStyleSetting.ListOption
                ).displayName
        ).isEqualTo("Green Style")
    }

    // We do want to call updateConfiguration here to test what happens when the locale changes.
    @Suppress("deprecation")
    @Test
    public fun stringResources_it() {
        context.resources.updateConfiguration(
            context.resources.configuration.apply {
                setLocale(Locale.ITALIAN)
            },
            context.resources.displayMetrics
        )
        Truth.assertThat(colorStyleSetting.displayName).isEqualTo("Colori")
        Truth.assertThat(colorStyleSetting.description).isEqualTo("Colorazione del quadrante")

        Truth.assertThat(
            (
                colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("red_style")) as
                    UserStyleSetting.ListUserStyleSetting.ListOption
                ).displayName
        ).isEqualTo("Stile rosso")

        Truth.assertThat(
            (
                colorStyleSetting.getOptionForId(UserStyleSetting.Option.Id("green_style")) as
                    UserStyleSetting.ListUserStyleSetting.ListOption
                ).displayName
        ).isEqualTo("Stile verde")
    }

    @Test
    public fun listOptionsWithIndices() {
        val listUserStyleSetting = ListUserStyleSetting(
            UserStyleSetting.Id("list"),
            context.resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options = listOf(
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
        Truth.assertThat(option0.displayName).isEqualTo("1st option")
        Truth.assertThat(option0.screenReaderName).isEqualTo("1st list option")

        val option1 = listUserStyleSetting.options[1] as ListOption
        Truth.assertThat(option1.displayName).isEqualTo("2nd option")
        Truth.assertThat(option1.screenReaderName).isEqualTo("2nd list option")

        val option2 = listUserStyleSetting.options[2] as ListOption
        Truth.assertThat(option2.displayName).isEqualTo("3rd option")
        Truth.assertThat(option2.screenReaderName).isEqualTo("3rd list option")
    }

    @Test
    @Suppress("deprecation")
    public fun listUserStyleSettingWireFormatRoundTrip_noScreenReaderName() {
        val listUserStyleSetting = ListUserStyleSetting(
            UserStyleSetting.Id("list"),
            context.resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options = listOf(
                ListOption(
                    UserStyleSetting.Option.Id("one"),
                    context.resources,
                    R.string.ith_option,
                    icon = null
                )
            ),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )

        val listUserStyleSettingAfterRoundTrip = ListUserStyleSetting(
            listUserStyleSetting.toWireFormat()
        )

        val option0 = listUserStyleSettingAfterRoundTrip.options[0] as ListOption
        Truth.assertThat(option0.displayName).isEqualTo("1st option")
        // We expect screenReaderName to be back filled by the displayName.
        Truth.assertThat(option0.screenReaderName).isEqualTo("1st option")
    }

    @Test
    public fun complicationSlotsOptionsWithIndices() {
        val complicationSetting = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting1"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(
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
        Truth.assertThat(option0.displayName).isEqualTo("1st option")
        Truth.assertThat(option0.screenReaderName).isEqualTo("1st list option")

        val option1 = complicationSetting.options[1] as ComplicationSlotsOption
        Truth.assertThat(option1.displayName).isEqualTo("2nd option")
        Truth.assertThat(option1.screenReaderName).isEqualTo("2nd list option")

        val option2 = complicationSetting.options[2] as ComplicationSlotsOption
        Truth.assertThat(option2.displayName).isEqualTo("3rd option")
        Truth.assertThat(option2.screenReaderName).isEqualTo("3rd list option")
    }

    @Test
    @Suppress("deprecation")
    public fun
    complicationsUserStyleSettingWireFormatRoundTrip_noScreenReaderName_filledByDisplayName() {
        val complicationSetting = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting1"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(
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

        val complicationSettingAfterRoundTrip = ComplicationSlotsUserStyleSetting(
            complicationSetting.toWireFormat()
        )

        val option0 = complicationSettingAfterRoundTrip.options[0] as ComplicationSlotsOption
        Truth.assertThat(option0.displayName).isEqualTo("1st option")
        // We expect screenReaderName to be back filled by the displayName.
        Truth.assertThat(option0.screenReaderName).isEqualTo("1st option")
    }
}