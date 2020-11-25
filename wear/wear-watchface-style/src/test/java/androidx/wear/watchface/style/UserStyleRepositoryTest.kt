/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(StyleTestRunner::class)
class UserStyleRepositoryTest {
    private val redStyleOption =
        ListUserStyleSetting.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleSetting.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleSetting.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = ListUserStyleSetting(
        "color_style_setting",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(Layer.BASE_LAYER)
    )

    private val classicStyleOption =
        ListUserStyleSetting.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleSetting.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleSetting.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = ListUserStyleSetting(
        "hand_style_setting",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(Layer.TOP_LAYER)
    )
    private val watchHandLengthStyleSetting =
        DoubleRangeUserStyleSetting(
            "watch_hand_length_style_setting",
            "Hand length",
            "Scale of watch hands",
            null,
            0.25,
            1.0,
            0.75,
            listOf(Layer.TOP_LAYER)
        )

    private val mockListener1 = Mockito.mock(UserStyleRepository.UserStyleListener::class.java)
    private val mockListener2 = Mockito.mock(UserStyleRepository.UserStyleListener::class.java)
    private val mockListener3 = Mockito.mock(UserStyleRepository.UserStyleListener::class.java)

    private val userStyleRepository =
        UserStyleRepository(
            UserStyleSchema(
                listOf(colorStyleSetting, watchHandStyleSetting, watchHandLengthStyleSetting)
            )
        )

    @Test
    fun addUserStyleListener_firesImmediately() {
        userStyleRepository.addUserStyleListener(mockListener1)
        Mockito.verify(mockListener1).onUserStyleChanged(userStyleRepository.userStyle)
    }

    @Test
    fun assigning_userStyle_firesListeners() {
        userStyleRepository.addUserStyleListener(mockListener1)
        userStyleRepository.addUserStyleListener(mockListener2)
        userStyleRepository.addUserStyleListener(mockListener3)

        Mockito.verify(mockListener1).onUserStyleChanged(userStyleRepository.userStyle)
        Mockito.verify(mockListener2).onUserStyleChanged(userStyleRepository.userStyle)
        Mockito.verify(mockListener3).onUserStyleChanged(userStyleRepository.userStyle)

        val newStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to greenStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )

        Mockito.reset(mockListener1)
        Mockito.reset(mockListener2)
        Mockito.reset(mockListener3)

        userStyleRepository.userStyle = newStyle

        Mockito.verify(mockListener1).onUserStyleChanged(userStyleRepository.userStyle)
        Mockito.verify(mockListener2).onUserStyleChanged(userStyleRepository.userStyle)
        Mockito.verify(mockListener3).onUserStyleChanged(userStyleRepository.userStyle)
    }

    @Test
    fun assigning_userStyle() {
        val newStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to greenStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )

        userStyleRepository.userStyle = newStyle

        assertThat(userStyleRepository.userStyle.selectedOptions[colorStyleSetting])
            .isEqualTo(greenStyleOption)
        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleSetting])
            .isEqualTo(gothicStyleOption)
    }

    @Test
    fun defaultValues() {
        val watchHandLengthOption =
            userStyleRepository.userStyle.selectedOptions[watchHandLengthStyleSetting]!! as
                DoubleRangeUserStyleSetting.DoubleRangeOption
        assertThat(watchHandLengthOption.value).isEqualTo(0.75)
    }

    @Test
    fun userStyle_mapConstructor() {
        val userStyle = UserStyle(
            mapOf(
                "color_style_setting" to "bluestyle",
                "hand_style_setting" to "gothic_style"
            ),
            userStyleRepository.schema
        )

        assertThat(userStyle.selectedOptions[colorStyleSetting]!!.id).isEqualTo("bluestyle")
        assertThat(userStyle.selectedOptions[watchHandStyleSetting]!!.id).isEqualTo("gothic_style")
    }

    @Test
    fun userStyle_mapConstructor_badColorStyle() {
        val userStyle = UserStyle(
            mapOf(
                "color_style_setting" to "I DO NOT EXIST",
                "hand_style_setting" to "gothic_style"
            ),
            userStyleRepository.schema
        )

        assertThat(userStyle.selectedOptions[colorStyleSetting]!!.id).isEqualTo("red_style")
        assertThat(userStyle.selectedOptions[watchHandStyleSetting]!!.id).isEqualTo("gothic_style")
    }

    @Test
    fun userStyle_mapConstructor_missingColorStyle() {
        val userStyle = UserStyle(
            mapOf(
                "hand_style_setting" to "gothic_style"
            ),
            userStyleRepository.schema
        )

        assertThat(userStyle.selectedOptions[colorStyleSetting]!!.id).isEqualTo("red_style")
        assertThat(userStyle.selectedOptions[watchHandStyleSetting]!!.id).isEqualTo("gothic_style")
    }
}
