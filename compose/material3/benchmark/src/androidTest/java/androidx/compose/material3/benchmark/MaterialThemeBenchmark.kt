/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MaterialThemeBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val themeTestCaseFactory = { MaterialThemeTestCase() }
    private val themeWithButtonTestCaseFactory = { MaterialThemeWithButtonTestCase() }
    private val themeWithButtonAndDynamicColorsTestCaseFactory = {
        MaterialThemeWithButtonAndDynamicColorsTestCase()
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(themeTestCaseFactory)
    }

    @Test
    fun firstPixel_withButton() {
        benchmarkRule.benchmarkToFirstPixel(themeWithButtonTestCaseFactory)
    }

    @Test
    fun firstPixel_withButtonAndDynamicColors() {
        benchmarkRule.benchmarkToFirstPixel(themeWithButtonAndDynamicColorsTestCaseFactory)
    }

    @Test
    fun changeColors() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(caseFactory = themeTestCaseFactory)
    }

    @Test
    fun changeColors_withButton() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = themeWithButtonTestCaseFactory
        )
    }

    @Test
    fun changeColors_withButtonAndDynamicColors() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = themeWithButtonAndDynamicColorsTestCaseFactory
        )
    }
}

internal class MaterialThemeTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    val lightColorScheme = lightColorScheme()
    val darkColorScheme = darkColorScheme()
    private var colorScheme: ColorScheme by mutableStateOf(lightColorScheme)

    @Composable
    override fun MeasuredContent() {
        MaterialTheme(colorScheme = colorScheme) { Box(Modifier.fillMaxSize()) }
    }

    override fun toggleState() {
        colorScheme =
            if (colorScheme == lightColorScheme) {
                darkColorScheme
            } else {
                lightColorScheme
            }
    }
}

internal class MaterialThemeWithButtonTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    val lightColorScheme = lightColorScheme()
    val darkColorScheme = darkColorScheme()
    private var colorScheme: ColorScheme by mutableStateOf(lightColorScheme)

    @Composable
    override fun MeasuredContent() {
        MaterialTheme(colorScheme = colorScheme) { Button(onClick = {}) { Text("Button") } }
    }

    override fun toggleState() {
        colorScheme =
            if (colorScheme == lightColorScheme) {
                darkColorScheme
            } else {
                lightColorScheme
            }
    }
}

internal class MaterialThemeWithButtonAndDynamicColorsTestCase :
    LayeredComposeTestCase(), ToggleableTestCase {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val lightColorScheme =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            lightColorScheme()
        }

    private val darkColorScheme =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            darkColorScheme()
        }

    private var colorScheme: ColorScheme by mutableStateOf(lightColorScheme)

    @Composable
    override fun MeasuredContent() {
        MaterialTheme(colorScheme = colorScheme) { Button(onClick = {}) { Text("Button") } }
    }

    override fun toggleState() {
        colorScheme =
            if (colorScheme == lightColorScheme) {
                darkColorScheme
            } else {
                lightColorScheme
            }
    }
}
