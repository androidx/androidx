/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(JUnit4::class)
class NavigationSuiteTest {

    private val layoutTypeProvider = NavigationSuiteDefaults.layoutTypeProvider

    @Test
    fun navigationLayoutTypeTest_compactWidth_compactHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 400.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_compactWidth_mediumHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_compactWidth_expandedHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 1000.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_mediumWidth_compactHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 400.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_mediumWidth_mediumHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 800.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_mediumWidth_expandedHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 1000.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_expandedWidth_compactHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 400.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_expandedWidth_mediumHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 800.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationRail)
    }

    @Test
    fun navigationLayoutTypeTest_expandedWidth_expandedHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 1000.dp))
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationRail)
    }

    @Test
    fun navigationLayoutTypeTest_tableTop() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 400.dp)),
                isTableTop = true
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_tableTop_expandedWidth() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 1000.dp)),
                isTableTop = true
            )

        assertThat(layoutTypeProvider.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationLayoutType.NavigationBar)
    }

    private fun createMockAdaptiveInfo(
        windowSizeClass: WindowSizeClass,
        isTableTop: Boolean = false
    ): WindowAdaptiveInfo {
        return WindowAdaptiveInfo(
            windowSizeClass,
            Posture(isTabletop = isTableTop)
        )
    }
}
