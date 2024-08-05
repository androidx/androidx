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

package androidx.compose.material3.adaptive.navigationsuite

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class NavigationSuiteScaffoldTest {

    @Test
    fun navigationLayoutTypeTest_compactWidth_compactHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(400f, 400f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_compactWidth_mediumHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(400f, 800f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_compactWidth_expandedHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(400f, 1000f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_mediumWidth_compactHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(800f, 400f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_mediumWidth_mediumHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(800f, 800f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationRail)
    }

    @Test
    fun navigationLayoutTypeTest_mediumWidth_expandedHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(800f, 1000f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationRail)
    }

    @Test
    fun navigationLayoutTypeTest_expandedWidth_compactHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(1000f, 400f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_expandedWidth_mediumHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(1000f, 800f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationRail)
    }

    @Test
    fun navigationLayoutTypeTest_expandedWidth_expandedHeight() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(1000f, 1000f)
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationRail)
    }

    @Test
    fun navigationLayoutTypeTest_tableTop() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(400f, 400f),
                isTableTop = true
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun navigationLayoutTypeTest_tableTop_expandedWidth() {
        val mockAdaptiveInfo =
            createMockAdaptiveInfo(
                windowSizeClass = WindowSizeClass.compute(1000f, 1000f),
                isTableTop = true
            )

        assertThat(NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(mockAdaptiveInfo))
            .isEqualTo(NavigationSuiteType.NavigationBar)
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
