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
                null
            ),
            UserStyleSetting.ListUserStyleSetting.ListOption(
                UserStyleSetting.Option.Id("green_style"),
                context.resources,
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
}