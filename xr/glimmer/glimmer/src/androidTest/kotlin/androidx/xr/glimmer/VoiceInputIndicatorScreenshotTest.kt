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

package androidx.xr.glimmer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class VoiceInputIndicatorScreenshotTest() {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun voiceInputIndicator() {
        rule.setGlimmerThemeContent { VoiceInputIndicator(level = { 0f }) }
        rule.assertRootAgainstGolden("voiceInputIndicator", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_level25() {
        rule.setGlimmerThemeContent { VoiceInputIndicator(level = { 0.25f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_level25", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_level50() {
        rule.setGlimmerThemeContent { VoiceInputIndicator(level = { 0.5f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_level50", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_level75() {
        rule.setGlimmerThemeContent { VoiceInputIndicator(level = { 0.75f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_level75", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_maxLevel() {
        rule.setGlimmerThemeContent { VoiceInputIndicator(level = { 1f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_maxLevel", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_withContainer() {
        rule.setGlimmerThemeContent { ContainedVoiceInputIndicator(level = { 0f }) }
        rule.assertRootAgainstGolden("voiceInputIndicator_withContainer", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_withContainer_level25() {
        rule.setGlimmerThemeContent { ContainedVoiceInputIndicator(level = { 0.25f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_withContainer_level25", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_withContainer_level50() {
        rule.setGlimmerThemeContent { ContainedVoiceInputIndicator(level = { 0.5f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_withContainer_level50", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_withContainer_level75() {
        rule.setGlimmerThemeContent { ContainedVoiceInputIndicator(level = { 0.75f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_withContainer_level75", screenshotRule)
    }

    @Test
    fun voiceInputIndicator_withContainer_maxLevel() {
        rule.setGlimmerThemeContent { ContainedVoiceInputIndicator(level = { 1f }) }
        // Advance to peak
        rule.mainClock.advanceTimeBy(250)
        rule.assertRootAgainstGolden("voiceInputIndicator_withContainer_maxLevel", screenshotRule)
    }
}
