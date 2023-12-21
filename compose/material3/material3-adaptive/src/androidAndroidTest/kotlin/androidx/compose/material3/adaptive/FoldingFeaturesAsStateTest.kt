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

import android.graphics.Rect
import androidx.compose.runtime.State
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class FoldingFeaturesAsStateTest {
    private val composeRule = createComposeRule()
    private val publisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule
    val testRule: TestRule
    init {
        testRule = RuleChain.outerRule(publisherRule).around(composeRule)
    }

    @Test
    fun test_foldingFeatureAsState_returnEmptyListInitially() {
        lateinit var actualFoldingFeatures: State<List<FoldingFeature>>

        composeRule.setContent {
            actualFoldingFeatures = foldingFeaturesAsState()
        }

        composeRule.runOnIdle {
            assertThat(actualFoldingFeatures.value).isEmpty()
        }
    }

    @Test
    fun test_foldingFeatureAsState_returnCurrentFoldingFeatures() {
        lateinit var actualFoldingFeatures: State<List<FoldingFeature>>

        composeRule.setContent {
            actualFoldingFeatures = foldingFeaturesAsState()
        }

        publisherRule.overrideWindowLayoutInfo(
            WindowLayoutInfo(
                listOf(MockFoldingFeature1, MockFoldingFeature2, MockDisplayFeature)
            )
        )

        composeRule.runOnIdle {
            assertThat(actualFoldingFeatures.value.size).isEqualTo(2)
            assertThat(MockFoldingFeature1 in actualFoldingFeatures.value).isTrue()
            assertThat(MockFoldingFeature2 in actualFoldingFeatures.value).isTrue()
        }
    }

    companion object {
        val MockFoldingFeature1 = FoldingFeature(
            windowBounds = Rect(0, 0, 1024, 800),
            size = 1
        )
        val MockFoldingFeature2 = FoldingFeature(
            windowBounds = Rect(0, 0, 1024, 800),
            size = 0
        )
        val MockDisplayFeature = object : DisplayFeature {
            override val bounds = Rect(10, 10, 12, 12)
        }
    }
}
