/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test

/**
 * Regression test for https://youtrack.jetbrains.com/issue/CMP-7353:
 * Crash on Web when using Arrangement.spacedBy(Dp.Unspecified).
 * Dp.Unspecified (= Dp(Float.NaN)) should be treated as 0.dp spacing.
 */
@OptIn(ExperimentalTestApi::class)
class ArrangementSpacedByUnspecifiedTest {

    @Test
    fun columnWithSpacedByDpUnspecifiedDoesNotCrash() = runComposeUiTest {
        val itemSize = 50.dp
        setContent {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dp.Unspecified),
                modifier = Modifier.size(200.dp)
            ) {
                Box(Modifier.testTag("item1").size(itemSize).background(Color.Red))
                Box(Modifier.testTag("item2").size(itemSize).background(Color.Blue))
            }
        }

        // Dp.Unspecified should be treated as 0.dp spacing
        onNodeWithTag("item1").assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("item2").assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun rowWithSpacedByDpUnspecifiedDoesNotCrash() = runComposeUiTest {
        val itemSize = 50.dp
        setContent {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dp.Unspecified),
                modifier = Modifier.size(200.dp)
            ) {
                Box(Modifier.testTag("item1").size(itemSize).background(Color.Red))
                Box(Modifier.testTag("item2").size(itemSize).background(Color.Blue))
            }
        }

        // Dp.Unspecified should be treated as 0.dp spacing
        onNodeWithTag("item1").assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("item2").assertLeftPositionInRootIsEqualTo(itemSize)
    }
}
