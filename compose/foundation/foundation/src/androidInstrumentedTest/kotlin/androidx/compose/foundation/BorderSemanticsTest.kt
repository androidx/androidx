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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BorderSemanticsTest {

    @get:Rule val rule = createComposeRule()

    val testTag = "BorderTag"

    @Test
    fun rectangleShape_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .border(BorderStroke(1.dp, Color.Red), RectangleShape)
                    .testTag(testTag)
            ) {}
        }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    fun roundedCornerShape_setsShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .border(BorderStroke(1.dp, Color.Red), RoundedCornerShape(1.dp))
                    .testTag(testTag)
            ) {}
        }

        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
    }

    @Test
    fun genericShape_setsShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .border(BorderStroke(1.dp, Color.Red), CutCornerShape(2.dp))
                    .testTag(testTag)
            ) {}
        }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(2.dp)))
    }

    @Test
    fun shapeChange_fromRectangle_invalidatesSemanticsProperty() {
        var shape by mutableStateOf(RectangleShape)
        rule.setContent {
            Box(
                Modifier.size(10.dp).border(BorderStroke(1.dp, Color.Red), shape).testTag(testTag)
            ) {}
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))

        rule.runOnIdle { shape = CircleShape }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CircleShape))
    }

    @Test
    fun shapeChange_toRectangle_invalidatesSemanticsProperty() {
        var shape: Shape by mutableStateOf(CircleShape)
        rule.setContent {
            Box(
                Modifier.size(10.dp).border(BorderStroke(1.dp, Color.Red), shape).testTag(testTag)
            ) {}
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CircleShape))

        rule.runOnIdle { shape = RectangleShape }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    fun shapeChange_betweenNonRectangles_invalidatesSemanticsProperty() {
        var shape: Shape by mutableStateOf(CircleShape)
        rule.setContent {
            Box(
                Modifier.size(10.dp).border(BorderStroke(1.dp, Color.Red), shape).testTag(testTag)
            ) {}
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CircleShape))

        rule.runOnIdle { shape = CutCornerShape(1.dp) }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(1.dp)))
    }
}
