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

package androidx.ui.tooling.preview

import androidx.test.rule.ActivityTestRule
import androidx.ui.material.ColorPalette
import androidx.ui.material.darkColorPalette
import androidx.ui.material.lightColorPalette
import androidx.ui.tooling.ComposeViewAdapterTest
import androidx.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.ui.tooling.preview.datasource.LoremIpsum
import androidx.ui.tooling.test.R
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PreviewParameterTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<ComposeViewAdapterTest.Companion.TestActivity>(
        ComposeViewAdapterTest.Companion.TestActivity::class.java
    )

    private lateinit var composeViewAdapter: ComposeViewAdapter

    @Before
    fun setup() {
        composeViewAdapter =
            activityTestRule.activity.findViewById(R.id.compose_view_adapter)
    }

    @Test
    fun loremIpsumProvider() {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.ui.tooling.preview.ParameterProviderComposableKt",
                "OneStringParameter",
                parameterProvider = LoremIpsum::class,
                debugViewInfos = true
            )
        }

        activityTestRule.runOnUiThread {
            Assert.assertTrue(composeViewAdapter.viewInfos.isNotEmpty())
        }
    }

    private class MyListProvider : CollectionPreviewParameterProvider<Int>(listOf(1, 2, 3))

    @Test
    fun checkIntParameterProvider() {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.ui.tooling.preview.ParameterProviderComposableKt",
                "OneIntParameter",
                parameterProvider = MyListProvider::class,
                debugViewInfos = true
            )
        }
    }

    private class MyColorPaletteProvider : CollectionPreviewParameterProvider<ColorPalette>(
        listOf(lightColorPalette(), darkColorPalette())
    )

    @Test
    fun checkColorPaletteProvider() {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.ui.tooling.preview.ParameterProviderComposableKt",
                "ColorPaletteParameter",
                parameterProvider = MyColorPaletteProvider::class,
                debugViewInfos = true
            )
        }
    }
}