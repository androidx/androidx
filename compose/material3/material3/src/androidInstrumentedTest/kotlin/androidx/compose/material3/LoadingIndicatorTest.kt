/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class LoadingIndicatorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun nonMaterialSetContent_loadingIndicator() {
        val progress = mutableFloatStateOf(0f)

        rule.setContent {
            LoadingIndicator(
                modifier = Modifier.testTag(TestTag),
                progress = { progress.value },
            )
        }

        rule.onNodeWithTag(TestTag).assertIsDisplayed()
    }

    @Test
    fun nonMaterialSetContent_containedLoadingIndicator() {
        val progress = mutableFloatStateOf(0f)

        rule.setContent {
            ContainedLoadingIndicator(
                modifier = Modifier.testTag(TestTag),
                progress = { progress.value },
            )
        }

        rule.onNodeWithTag(TestTag).assertIsDisplayed()
    }

    @Test
    fun determinateLoadingIndicator_Progress() {
        val progress = mutableFloatStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            LoadingIndicator(modifier = Modifier.testTag(TestTag), progress = { progress.value })
        }

        rule
            .onNodeWithTag(TestTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 0.5f }

        rule
            .onNodeWithTag(TestTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateContainedLoadingIndicator_Progress() {
        val progress = mutableFloatStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            ContainedLoadingIndicator(
                modifier = Modifier.testTag(TestTag),
                progress = { progress.value }
            )
        }

        rule
            .onNodeWithTag(TestTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 0.5f }

        rule
            .onNodeWithTag(TestTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateLoadingIndicator_ProgressIsCoercedInBounds() {
        val progress = mutableStateOf(-1f)

        rule.setMaterialContent(lightColorScheme()) {
            LoadingIndicator(modifier = Modifier.testTag(TestTag), progress = { progress.value })
        }

        rule
            .onNodeWithTag(TestTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 1.5f }

        rule
            .onNodeWithTag(TestTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun determinateLoadingIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions { LoadingIndicator(progress = { 0f }) }
            .assertWidthIsEqualTo(LoadingIndicatorDefaults.ContainerWidth)
            .assertHeightIsEqualTo(LoadingIndicatorDefaults.ContainerHeight)
    }

    @Test
    fun determinateContainedLoadingIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions { ContainedLoadingIndicator(progress = { 0f }) }
            .assertWidthIsEqualTo(LoadingIndicatorDefaults.ContainerWidth)
            .assertHeightIsEqualTo(LoadingIndicatorDefaults.ContainerHeight)
    }

    @Test(expected = IllegalArgumentException::class)
    fun determinateLoadingIndicator_MinPolygons() {
        rule.setMaterialContent(lightColorScheme()) {
            LoadingIndicator(progress = { 0f }, polygons = listOf(MaterialShapes.PuffyDiamond))
        }
    }

    @Test
    fun indeterminateLoadingIndicator_Progress() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            LoadingIndicator(modifier = Modifier.testTag(TestTag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.onNodeWithTag(TestTag).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateContainedLoadingIndicator_Progress() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            ContainedLoadingIndicator(modifier = Modifier.testTag(TestTag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.onNodeWithTag(TestTag).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateLoadingIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest = rule.setMaterialContentForSizeAssertions { LoadingIndicator() }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertWidthIsEqualTo(LoadingIndicatorDefaults.ContainerWidth)
            .assertHeightIsEqualTo(LoadingIndicatorDefaults.ContainerHeight)
    }

    @Test
    fun indeterminateContainedLoadingIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest = rule.setMaterialContentForSizeAssertions { ContainedLoadingIndicator() }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertWidthIsEqualTo(LoadingIndicatorDefaults.ContainerWidth)
            .assertHeightIsEqualTo(LoadingIndicatorDefaults.ContainerHeight)
    }

    @Test(expected = IllegalArgumentException::class)
    fun indeterminateLoadingIndicator_MinPolygons() {
        rule.setMaterialContent(lightColorScheme()) {
            LoadingIndicator(polygons = listOf(MaterialShapes.PuffyDiamond))
        }
    }

    private val TestTag = "indicator"
}
