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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(StyleTestRunner::class)
class UserStyleRepositoryTest {
    private val redStyleOption =
        ListUserStyleCategory.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleCategory.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleCategory.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleCategory = ListUserStyleCategory(
        "color_style_category",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        UserStyleCategory.LAYER_FLAG_WATCH_FACE_BASE
    )

    private val classicStyleOption =
        ListUserStyleCategory.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleCategory.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleCategory.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleCategory = ListUserStyleCategory(
        "hand_style_category",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        UserStyleCategory.LAYER_FLAG_WATCH_FACE_UPPER
    )
    private val watchHandLengthStyleCategory =
        DoubleRangeUserStyleCategory(
            "watch_hand_length_style_category",
            "Hand length",
            "Scale of watch hands",
            null,
            0.25,
            1.0,
            0.75,
            UserStyleCategory.LAYER_FLAG_WATCH_FACE_UPPER
        )

    private val mockListener1 = Mockito.mock(UserStyleRepository.UserStyleListener::class.java)
    private val mockListener2 = Mockito.mock(UserStyleRepository.UserStyleListener::class.java)
    private val mockListener3 = Mockito.mock(UserStyleRepository.UserStyleListener::class.java)

    private val userStyleRepository =
        UserStyleRepository(
            listOf(colorStyleCategory, watchHandStyleCategory, watchHandLengthStyleCategory)
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

        val newStyle = UserStyle(hashMapOf(
            colorStyleCategory to greenStyleOption,
            watchHandStyleCategory to gothicStyleOption
        ))

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
        val newStyle = UserStyle(hashMapOf(
            colorStyleCategory to greenStyleOption,
            watchHandStyleCategory to gothicStyleOption
        ))

        userStyleRepository.userStyle = newStyle

        assertThat(userStyleRepository.userStyle.options[colorStyleCategory])
            .isEqualTo(greenStyleOption)
        assertThat(userStyleRepository.userStyle.options[watchHandStyleCategory])
            .isEqualTo(gothicStyleOption)
    }

    @Test
    fun defaultValues() {
        val watchHandLengthOption =
            userStyleRepository.userStyle.options[watchHandLengthStyleCategory]!! as
                    DoubleRangeUserStyleCategory.DoubleRangeOption
        assertThat(watchHandLengthOption.value).isEqualTo(0.75)
    }
}
