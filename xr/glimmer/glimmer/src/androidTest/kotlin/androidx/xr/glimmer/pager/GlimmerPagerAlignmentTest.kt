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

package androidx.xr.glimmer.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.createGlimmerRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class GlimmerPagerAlignmentTest(private val config: GlimmerPagerParamConfig) :
    BaseParameterizedGlimmerPagerTest() {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun verticalAlignment_top() {
        val state = GlimmerPagerState { 5 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(
                config = config,
                state = state,
                modifier = Modifier.size(100.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.size(20.dp).background(Color.Red).testTag("page-0"))
            }
        }

        rule.onNodeWithTag("page-0").getBoundsInRoot().top.assertIsEqualTo(0.dp)
        rule.onNodeWithTag("page-0").getBoundsInRoot().bottom.assertIsEqualTo(20.dp)
    }

    @Test
    fun verticalAlignment_center() {
        val state = GlimmerPagerState { 5 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(
                config = config,
                state = state,
                modifier = Modifier.size(100.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).background(Color.Red).testTag("page-0"))
            }
        }

        rule.onNodeWithTag("page-0").getBoundsInRoot().top.assertIsEqualTo(40.dp)
        rule.onNodeWithTag("page-0").getBoundsInRoot().bottom.assertIsEqualTo(60.dp)
    }

    @Test
    fun verticalAlignment_bottom() {
        val state = GlimmerPagerState { 5 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(
                config = config,
                state = state,
                modifier = Modifier.size(100.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(Modifier.size(20.dp).background(Color.Red).testTag("page-0"))
            }
        }

        rule.onNodeWithTag("page-0").getBoundsInRoot().top.assertIsEqualTo(80.dp)
        rule.onNodeWithTag("page-0").getBoundsInRoot().bottom.assertIsEqualTo(100.dp)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllGlimmerPagerTestParams
    }
}
