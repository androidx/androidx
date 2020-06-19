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

package androidx.wear.watchfacestyle

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

// Without this we get test failures with an error:
// "failed to access class kotlin.jvm.internal.DefaultConstructorMarker".
class UserStyleManagerTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotInstrumentPackage("androidx.wear.watchfacestyle")
            .build()
}

@RunWith(UserStyleManagerTestRunner::class)
class UserStyleManagerTest {
    private val redStyleOption =
        ListViewUserStyleCategory.ListViewOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListViewUserStyleCategory.ListViewOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListViewUserStyleCategory.ListViewOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleCategory = ListViewUserStyleCategory(
        "color_style_category",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList
    )

    private val classicStyleOption =
        ListViewUserStyleCategory.ListViewOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListViewUserStyleCategory.ListViewOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListViewUserStyleCategory.ListViewOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleCategory = ListViewUserStyleCategory(
        "hand_style_category",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList
    )

    private val mockListener1 = Mockito.mock(UserStyleManager.UserStyleListener::class.java)
    private val mockListener2 = Mockito.mock(UserStyleManager.UserStyleListener::class.java)
    private val mockListener3 = Mockito.mock(UserStyleManager.UserStyleListener::class.java)

    private val styleManager = UserStyleManager(listOf(colorStyleCategory, watchHandStyleCategory))

    @Test
    fun addUserStyleListener_firesImmediately() {
        styleManager.addUserStyleListener(mockListener1)
        Mockito.verify(mockListener1).onUserStyleChanged(styleManager.userStyle)
    }

    @Test
    fun assigning_userStyle_firesListeners() {
        styleManager.addUserStyleListener(mockListener1)
        styleManager.addUserStyleListener(mockListener2)
        styleManager.addUserStyleListener(mockListener3)

        Mockito.verify(mockListener1).onUserStyleChanged(styleManager.userStyle)
        Mockito.verify(mockListener2).onUserStyleChanged(styleManager.userStyle)
        Mockito.verify(mockListener3).onUserStyleChanged(styleManager.userStyle)

        val newStyle: Map<UserStyleCategory, UserStyleCategory.Option> = mapOf(
            colorStyleCategory to greenStyleOption,
            watchHandStyleCategory to gothicStyleOption
        )

        Mockito.reset(mockListener1)
        Mockito.reset(mockListener2)
        Mockito.reset(mockListener3)

        styleManager.userStyle = newStyle

        Mockito.verify(mockListener1).onUserStyleChanged(styleManager.userStyle)
        Mockito.verify(mockListener2).onUserStyleChanged(styleManager.userStyle)
        Mockito.verify(mockListener3).onUserStyleChanged(styleManager.userStyle)
    }

    @Test
    fun assigning_userStyle() {
        val newStyle: Map<UserStyleCategory, UserStyleCategory.Option> = mapOf(
            colorStyleCategory to greenStyleOption,
            watchHandStyleCategory to gothicStyleOption
        )

        styleManager.userStyle = newStyle

        Truth.assertThat(styleManager.userStyle[colorStyleCategory]).isEqualTo(greenStyleOption)
        Truth.assertThat(styleManager.userStyle[watchHandStyleCategory])
            .isEqualTo(gothicStyleOption)
    }
}