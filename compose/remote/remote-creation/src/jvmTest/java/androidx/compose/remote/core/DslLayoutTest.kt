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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.ResizeDocument
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.TestParameters
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcContentScale
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.componentId
import androidx.compose.remote.creation.dsl.fillMaxHeight
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.horizontalCollapsiblePriority
import androidx.compose.remote.creation.dsl.horizontalWeight
import androidx.compose.remote.creation.dsl.offset
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.requiredHeightIn
import androidx.compose.remote.creation.dsl.requiredWidthIn
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.dsl.widthIn
import org.junit.Test

class DslLayoutTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testDslBoxLayout() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 400f, 400f), // Box
                validateBounds(
                    11,
                    0f,
                    0f,
                    140f,
                    140f,
                ), // Child Box outer bounds (100 + 2*20 padding)
            )

        checkDslLayout(400, 400, ops = ops) {
            Box(modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFF0000.toInt())) {
                Box(
                    modifier =
                        Modifier.componentId(11)
                            .padding(20f)
                            .size(100f)
                            .background(0xFF00FF00.toInt())
                )
            }
        }
    }

    @Test
    fun testDslRowColumnWithWeights() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(1, 0f, 0f, 600f, 200f), // Row
                validateBounds(2, 0f, 0f, 100f, 200f), // Box A: weight 1 -> 100px
                validateBounds(3, 100f, 0f, 300f, 200f), // Box B: weight 3 -> 300px
                validateBounds(4, 400f, 0f, 200f, 200f), // Box C: fixed width -> 200px
            )

        checkDslLayout(600, 200, ops = ops) {
            Row(modifier = Modifier.componentId(1).fillMaxSize().background(0xFFFFFFFF.toInt())) {
                Box(
                    modifier =
                        Modifier.componentId(2)
                            .horizontalWeight(1.0f)
                            .fillMaxHeight()
                            .background(0xFF00FF00.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(3)
                            .horizontalWeight(3.0f)
                            .fillMaxHeight()
                            .background(0xFF0000FF.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(4)
                            .width(200f)
                            .fillMaxHeight()
                            .background(0xFFFF0000.toInt())
                )
            }
        }
    }

    @Test
    fun testDslConstraintsPropagation() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(1, 0f, 0f, 400f, 400f), // Column
                // Sibling A: wrap-content width but constrained by widthIn(150f, 250f) -> since
                // text is short, it clamps to min 150f
                validateBounds(2, 0f, 0f, 150f, 50f),
            )

        checkDslLayout(400, 400, ops = ops) {
            Column(
                modifier = Modifier.componentId(1).fillMaxSize().background(0xFFFFFFFF.toInt())
            ) {
                Box(
                    modifier =
                        Modifier.componentId(2)
                            .widthIn(min = 150f, max = 250f)
                            .height(50f)
                            .background(0xFF00FF00.toInt())
                )
            }
        }
    }

    @Test
    fun testDslOffsetAndBorder() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(1, 0f, 0f, 400f, 400f), // Row
                // Sibling A: normal box at 0, 0
                validateBounds(2, 0f, 0f, 100f, 100f),
                // Sibling B: offset layout position is still 100, 0 (shift is paint-only)
                validateBounds(3, 100f, 0f, 100f, 100f),
                // Sibling C: normal box placed at 200, 0
                validateBounds(4, 200f, 0f, 100f, 100f),
            )

        checkDslLayout(400, 400, ops = ops) {
            Row(modifier = Modifier.componentId(1).fillMaxSize().background(0xFFFFFFFF.toInt())) {
                Box(modifier = Modifier.componentId(2).size(100f).background(0xFF00FF00.toInt()))
                Box(
                    modifier =
                        Modifier.componentId(3)
                            .offset(x = 50f, y = 50f)
                            .size(100f)
                            .background(0xFFFF0000.toInt())
                )
                Box(modifier = Modifier.componentId(4).size(100f).background(0xFF0000FF.toInt()))
            }
        }
    }

    @Test
    fun testDslRowArrangements() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 600f, 200f), // Row
                validateBounds(11, 0f, 0f, 100f, 200f), // Child A: start
                validateBounds(12, 250f, 0f, 100f, 200f), // Child B: center (0 + 100 + 150 spacing)
                validateBounds(13, 500f, 0f, 100f, 200f), // Child C: end (250 + 100 + 150 spacing)
            )

        checkDslLayout(600, 200, ops = ops) {
            Row(
                modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt()),
                horizontal = RcRowHorizontalPositioning.SpaceBetween,
            ) {
                Box(
                    modifier =
                        Modifier.componentId(11).size(100f, 200f).background(0xFF00FF00.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(12).size(100f, 200f).background(0xFF0000FF.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(13).size(100f, 200f).background(0xFFFF0000.toInt())
                )
            }
        }
    }

    @Test
    fun testDslCollapsibleRow() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                // Initial layout (width = 400): sum is 450.
                // Under the codebase rules, larger number = higher priority.
                // So priority 1 (Child 11) collapses first.
                // Priority 2 (Child 12) and Priority 3 (Child 13) remain visible.
                validateBounds(10, 0f, 0f, 400f, 200f), // CollapsibleRow
                validateBounds(
                    12,
                    0f,
                    0f,
                    150f,
                    200f,
                ), // Child 12: priority 2 -> visible at x=0 (Child 11 is GONE)
                validateBounds(
                    13,
                    150f,
                    0f,
                    150f,
                    200f,
                ), // Child 13: priority 3 -> visible at x=150
                validateChildCount(10, 2), // Exactly 2 children visible (Child 11 collapsed)

                // Resize parent to 250px: both priority 1 (Child 11) and priority 2 (Child 12) must
                // collapse
                ResizeDocument(250, 200),
                CaptureComponentTree(),
                validateBounds(13, 0f, 0f, 150f, 200f), // Child 13: priority 3 -> visible at x=0
                validateChildCount(10, 1), // Only 1 child visible (Child 11 and 12 collapsed)
            )

        checkDslLayout(400, 200, ops = ops) {
            CollapsibleRow(
                modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt())
            ) {
                Box(
                    modifier =
                        Modifier.componentId(11)
                            .size(150f, 200f)
                            .horizontalCollapsiblePriority(1.0f)
                            .background(0xFF00FF00.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(12)
                            .size(150f, 200f)
                            .horizontalCollapsiblePriority(2.0f)
                            .background(0xFF0000FF.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(13)
                            .size(150f, 200f)
                            .horizontalCollapsiblePriority(3.0f)
                            .background(0xFFFF0000.toInt())
                )
            }
        }
    }

    @Test
    fun testDslFitBox() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 300f, 300f), // FitBox parent
                // Child A (400x400) too large: marked GONE, but layout coordinates still computed
                // at (0, -50, 400, 400)
                validateBounds(11, 0f, -50f, 400f, 400f),
                // Child B (200x200) fits: marked VISIBLE, centered vertically -> y = 50px
                validateBounds(12, 0f, 50f, 200f, 200f),
                // Child C (100x100) fits but bypassed (first fitting was selected): marked GONE
                validateBounds(13, 0f, 100f, 100f, 100f),
                validateChildCount(10, 1), // Only exactly 1 child is visible (Child 12)
            )

        checkDslLayout(300, 300, ops = ops) {
            // FitBox responsive selection test
            FitBox(
                modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt()),
                vertical = RcVerticalPositioning.Center,
            ) {
                // Child A: 400x400 (too large, should be GONE)
                Box(modifier = Modifier.componentId(11).size(400f).background(0xFFFF0000.toInt()))
                // Child B: 200x200 (fits, should be VISIBLE)
                Box(modifier = Modifier.componentId(12).size(200f).background(0xFF00FF00.toInt()))
                // Child C: 100x100 (fits, but since Child B already fit and is first, Child C
                // should be GONE)
                Box(modifier = Modifier.componentId(13).size(100f).background(0xFF0000FF.toInt()))
            }
        }
    }

    @Test
    fun testDslFlowLayout() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 400f, 200f), // Flow parent
                validateBounds(11, 0f, 0f, 100f, 50f), // Child 1
                validateBounds(12, 100f, 0f, 100f, 50f), // Child 2
                validateBounds(13, 200f, 0f, 100f, 50f), // Child 3
                validateBounds(14, 300f, 0f, 100f, 50f), // Child 4 (fits exactly on row 1)
                validateBounds(15, 0f, 50f, 100f, 50f), // Child 5 (wrapped to row 2 at y=50)
            )

        checkDslLayout(400, 200, ops = ops) {
            Flow(modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt())) {
                Box(
                    modifier =
                        Modifier.componentId(11).size(100f, 50f).background(0xFF00FF00.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(12).size(100f, 50f).background(0xFF0000FF.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(13).size(100f, 50f).background(0xFFFF0000.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(14).size(100f, 50f).background(0xFFFFFF00.toInt())
                )
                Box(
                    modifier =
                        Modifier.componentId(15).size(100f, 50f).background(0xFF00FFFF.toInt())
                )
            }
        }
    }

    @Test
    fun testDslStateLayout() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                // Initial State (state = 0): Child 11 (Index 0) is VISIBLE, Child 12 (Index 1) is
                // GONE (size 0).
                validateBounds(10, 0f, 0f, 400f, 400f), // StateLayout parent
                validateBounds(11, 0f, 0f, 400f, 400f), // Child 11: visible and fills parent
                validateBounds(12, 0f, 0f, 0f, 0f), // Child 12: GONE (size 0)
                validateChildCount(10, 1), // Only 1 child is visible (Child 11)

                // Switch state to 1: Child 11 collapses (GONE), Child 12 becomes VISIBLE.
                SetNamedIntegerVariable("layout_state", 1),
                CaptureComponentTree(),
                // Child 11 is now GONE, but since its layout pass was skipped, its width/height
                // fields are stale at 400x400
                validateBounds(11, 0f, 0f, 400f, 400f),
                // Child 12 is now VISIBLE and fills parent
                validateBounds(12, 0f, 0f, 400f, 400f),
                validateChildCount(10, 1), // Only 1 child is visible (Child 12)
            )

        checkDslLayout(400, 400, ops = ops) {
            val stateIndex = remoteNamedInteger("layout_state", 0)
            StateLayout(
                stateIndex = stateIndex,
                modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt()),
            ) {
                Box(
                    modifier = Modifier.componentId(11).fillMaxSize().background(0xFF00FF00.toInt())
                )
                Box(
                    modifier = Modifier.componentId(12).fillMaxSize().background(0xFF0000FF.toInt())
                )
            }
        }
    }

    @Test
    fun testDslImageWithScale() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 900f, 300f), // Row parent
                validateBounds(11, 0f, 0f, 300f, 300f), // Image 1 (Fit)
                validateBounds(12, 300f, 0f, 300f, 300f), // Image 2 (Crop)
                validateBounds(13, 600f, 0f, 300f, 300f), // Image 3 (FillBounds)
            )

        checkDslLayout(900, 300, ops = ops) {
            val img = createBitmap(200, 100) // 2:1 aspect ratio
            Row(modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt())) {
                Image(
                    image = img,
                    modifier = Modifier.componentId(11).size(300f),
                    contentScale = RcContentScale.Fit,
                )
                Image(
                    image = img,
                    modifier = Modifier.componentId(12).size(300f),
                    contentScale = RcContentScale.Crop,
                )
                Image(
                    image = img,
                    modifier = Modifier.componentId(13).size(300f),
                    contentScale = RcContentScale.FillBounds,
                )
            }
        }
    }

    @Test
    fun testDslTextAndSpacer() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 300f, 100f), // Row parent
                validateBounds(11, 0f, 0f, 100f, 100f), // Sibling A (Box)
                validateBounds(12, 100f, 0f, 150f, 100f), // Spacer (Sibling B)
                validateBounds(13, 250f, 0f, 50f, 100f), // Sibling C (Box)
            )

        checkDslLayout(300, 100, ops = ops) {
            Row(modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt())) {
                Box(modifier = Modifier.componentId(11).size(100f).background(0xFF00FF00.toInt()))
                Spacer(modifier = Modifier.componentId(12).horizontalWeight(1f).fillMaxHeight())
                Box(
                    modifier =
                        Modifier.componentId(13).size(50f, 100f).background(0xFF0000FF.toInt())
                )
            }
        }
    }

    @Test
    fun testDslRequiredConstraints() {
        val ops =
            arrayListOf<TestOperation>(
                CaptureComponentTree(),
                validateBounds(10, 0f, 0f, 100f, 100f), // Parent Box (100x100)
                validateBounds(
                    11,
                    0f,
                    0f,
                    100f,
                    100f,
                ), // Child 1: normal width(200f) -> clamped to parent max width (100f)
                validateBounds(
                    12,
                    0f,
                    0f,
                    200f,
                    200f,
                ), // Child 2: requiredWidthIn(200f) & requiredHeightIn(200f) -> bypasses parent and
                // is exactly 200x200!
            )

        checkDslLayout(100, 100, ops = ops) {
            Box(modifier = Modifier.componentId(10).fillMaxSize().background(0xFFFFFFFF.toInt())) {
                // Child 1: standard width/height (should clamp)
                Box(
                    modifier =
                        Modifier.componentId(11)
                            .width(200f)
                            .height(200f)
                            .background(0xFF00FF00.toInt())
                )
                // Child 2: required width/height (should bypass clamp)
                Box(
                    modifier =
                        Modifier.componentId(12)
                            .requiredWidthIn(min = 200f, max = 200f)
                            .requiredHeightIn(min = 200f, max = 200f)
                            .background(0xFF0000FF.toInt())
                )
            }
        }
    }
}

class SetNamedIntegerVariable(val name: String, val newValue: Int) : TestOperation() {
    override fun apply(
        context: RemoteContext,
        document: CoreDocument,
        testParameters: TestParameters,
        commands: MutableList<Map<String, Any>>?,
    ): Boolean {
        val mockContext = context as androidx.compose.remote.core.layout.MockRemoteContext
        val id = mockContext.varNamesMap[name] ?: throw AssertionError("Variable $name not found")
        context.loadInteger(id, newValue)
        context.mRemoteComposeState.overrideInteger(id, newValue)

        // Pass 1: Paint to let StateLayout detect the state variable change and invalidate measure
        document.paint(context, 0)

        // Pass 2: Paint again. Since measure was invalidated, this will automatically trigger
        // layout & measure, and paint the new state
        document.paint(context, 0)

        document.needsMeasure()
        return true
    }
}
