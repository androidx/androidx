/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModelReadsTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private var actionExecuted = false

    @Before
    fun setup() {
        actionExecuted = false
    }

    @Test
    fun useTheSameModelInDrawAndPosition() {
        val offset = mutableStateOf(5)
        var drawExecuted = false
        var positionExecuted = false
        rule.setContent {
            Layout(
                {},
                modifier =
                    Modifier.drawBehind {
                        // read from the model
                        offset.value
                        drawExecuted = true
                    },
            ) { _, _ ->
                layout(10, 10) {
                    // read from the model
                    offset.value
                    positionExecuted = true
                }
            }
        }
        rule.waitForIdle()
        assertTrue(drawExecuted)
        assertTrue(positionExecuted)

        drawExecuted = false
        positionExecuted = false
        rule.runOnIdle { offset.value = 7 }

        rule.waitForIdle()
        assertTrue(drawExecuted)
        assertTrue(positionExecuted)

        drawExecuted = false
        positionExecuted = false
        rule.runOnIdle { offset.value = 10 }

        rule.waitForIdle()
        assertTrue(drawExecuted)
        assertTrue(positionExecuted)
    }

    @Test
    @MediumTest
    fun useDifferentModelsInDrawAndPosition() {
        val drawModel = mutableStateOf(5)
        val positionModel = mutableStateOf(5)
        var drawExecuted = false
        var positionExecuted = false
        rule.setContent {
            Layout(
                {},
                modifier =
                    Modifier.drawBehind {
                        // read from the model
                        drawModel.value
                        drawExecuted = true
                    },
            ) { _, _ ->
                layout(10, 10) {
                    // read from the model
                    positionModel.value
                    positionExecuted = true
                }
            }
        }
        rule.waitForIdle()
        assertTrue(drawExecuted)
        assertTrue(positionExecuted)

        drawExecuted = false
        positionExecuted = false
        rule.runOnIdle { drawModel.value = 7 }

        rule.waitForIdle()
        assertTrue(drawExecuted)
        assertFalse(positionExecuted)

        drawExecuted = false
        positionExecuted = false
        rule.runOnIdle { positionModel.value = 10 }

        rule.waitForIdle()
        assertTrue(positionExecuted)
        assertFalse(drawExecuted)
    }

    @Test
    fun useTheSameModelInMeasureAndDraw() {
        val offset = mutableStateOf(5)
        var measureExecuted = false
        var drawExecuted = false
        rule.setContent {
            Layout(
                {},
                modifier =
                    Modifier.drawBehind {
                        // read from the model
                        offset.value
                        drawExecuted = true
                    },
            ) { _, _ ->
                measureExecuted = true
                // read from the model
                layout(offset.value, 10) {}
            }
        }
        rule.waitForIdle()
        assertTrue(measureExecuted)
        assertTrue(drawExecuted)

        measureExecuted = false
        drawExecuted = false
        rule.runOnIdle { offset.value = 10 }

        rule.waitForIdle()
        assertTrue(measureExecuted)
        assertTrue(drawExecuted)

        measureExecuted = false
        drawExecuted = false
        rule.runOnIdle { offset.value = 15 }

        rule.waitForIdle()
        assertTrue(measureExecuted)
        assertTrue(drawExecuted)
    }

    @Test
    fun useDifferentModelsInMeasureAndPosition() {
        val measureModel = mutableStateOf(5)
        val positionModel = mutableStateOf(5)
        var measureExecuted = false
        var positionExecuted = false
        rule.setContent {
            Layout({}) { _, _ ->
                measureExecuted = true
                // read from the model
                layout(measureModel.value, 10) {
                    // read from the model
                    positionModel.value
                    positionExecuted = true
                }
            }
        }
        rule.waitForIdle()
        assertTrue(measureExecuted)
        assertTrue(positionExecuted)

        measureExecuted = false
        positionExecuted = false
        rule.runOnIdle { measureModel.value = 10 }

        rule.waitForIdle()
        assertTrue(measureExecuted)
        // remeasuring automatically triggers relayout
        assertTrue(positionExecuted)

        measureExecuted = false
        positionExecuted = false
        rule.runOnIdle { positionModel.value = 15 }

        rule.waitForIdle()
        assertFalse(measureExecuted)
        assertTrue(positionExecuted)
    }

    @Test
    fun drawReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            AtLeastSize(
                10,
                modifier =
                    Modifier.drawBehind {
                        if (enabled.value) {
                            // read the model
                            model.value
                        }
                        actionExecuted = true
                    },
            ) {}
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)
    }

    @Test
    fun measureReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            Layout({}) { _, _ ->
                if (enabled.value) {
                    // read the model
                    model.value
                }
                actionExecuted = true
                layout(10, 10) {}
            }
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model)
    }

    @Test
    fun layoutReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            Layout({}) { _, _ ->
                layout(10, 10) {
                    if (enabled.value) {
                        // read the model
                        model.value
                    }
                    actionExecuted = true
                }
            }
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model)
    }

    @Test
    @MediumTest
    fun drawStopsReactingOnModelsAfterDetaching() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            val modifier =
                if (enabled.value) {
                    Modifier.drawBehind {
                        // read the model
                        model.value
                        actionExecuted = true
                    }
                } else Modifier
            AtLeastSize(10, modifier = modifier) {}
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model, false)
    }

    @Test
    @MediumTest
    fun measureStopsReactingOnModelsAfterDetaching() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            if (enabled.value) {
                Layout({}) { _, _ ->
                    // read the model
                    model.value
                    actionExecuted = true
                    layout(10, 10) {}
                }
            }
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model, false)
    }

    @Test
    @MediumTest
    fun layoutStopsReactingOnModelsAfterDetaching() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            if (enabled.value) {
                Layout({}) { _, _ ->
                    layout(10, 10) {
                        // read the model
                        model.value
                        actionExecuted = true
                    }
                }
            }
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model, false)
    }

    @Test
    fun remeasureRequestForTheNodeBeingMeasured() {
        var measured = false
        val model = mutableStateOf(0)
        rule.setContent {
            Layout({}) { _, _ ->
                if (model.value == 1) {
                    // this will trigger remeasure request for this node we currently measure
                    model.value = 2
                    Snapshot.sendApplyNotifications()
                }
                measured = true
                layout(100, 100) {}
            }
        }

        rule.waitForIdle()
        assertTrue(measured)

        measured = false

        rule.runOnIdle { model.value = 1 }

        rule.waitForIdle()
        assertTrue(measured)
    }

    @Test
    fun remeasureRequestForTheNodeBeingLaidOut() {
        var remeasured = false
        var relayouted = false
        val remeasureModel = mutableStateOf(0)
        val relayoutModel = mutableStateOf(0)
        var valueReadDuringMeasure = -1
        var modelAlreadyChanged = false
        rule.setContent {
            Layout({}) { _, _ ->
                valueReadDuringMeasure = remeasureModel.value
                remeasured = true
                layout(100, 100) {
                    if (relayoutModel.value != 0) {
                        if (!modelAlreadyChanged) {
                            // this will trigger remeasure request for this node we layout
                            remeasureModel.value = 1
                            Snapshot.sendApplyNotifications()
                            // the remeasure will also include another relayout and we don't
                            // want to loop and request remeasure again
                            modelAlreadyChanged = true
                        }
                    }
                    relayouted = true
                }
            }
        }

        rule.waitForIdle()
        assertTrue(remeasured)
        assertTrue(relayouted)

        remeasured = false
        relayouted = false

        rule.runOnIdle { relayoutModel.value = 1 }

        rule.waitForIdle()
        assertTrue(remeasured)
        assertTrue(relayouted)
        assertEquals(1, valueReadDuringMeasure)
    }

    @Test
    fun relayoutRequestForTheNodeBeingMeasured() {
        var remeasured = false
        var relayouted = false
        val remeasureModel = mutableStateOf(0)
        val relayoutModel = mutableStateOf(0)
        rule.setContent {
            Layout({}) { _, _ ->
                if (remeasureModel.value != 0) {
                    // this will trigger relayout request for this node we currently measure
                    relayoutModel.value = 1
                    Snapshot.sendApplyNotifications()
                }
                remeasured = true
                layout(100, 100) {
                    relayoutModel.value // just register the read
                    relayouted = true
                }
            }
        }

        rule.waitForIdle()
        assertTrue(remeasured)
        assertTrue(relayouted)

        remeasured = false
        relayouted = false

        rule.runOnIdle { remeasureModel.value = 1 }

        rule.waitForIdle()
        assertTrue(remeasured)
        assertTrue(relayouted)
    }

    @Test
    fun relayoutRequestForTheNodeBeingLaidOut() {
        var relayouted = false
        val model = mutableStateOf(0)
        rule.setContent {
            Layout({}) { _, _ ->
                layout(100, 100) {
                    if (model.value == 1) {
                        // this will trigger relayout request for this node we currently layout
                        model.value = 2
                        Snapshot.sendApplyNotifications()
                    }
                    relayouted = true
                }
            }
        }

        rule.waitForIdle()
        assertTrue(relayouted)

        relayouted = false

        rule.runOnIdle { model.value = 1 }

        rule.waitForIdle()
        assertTrue(relayouted)
    }

    @Test
    fun measureModifierReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            Layout(
                {},
                Modifier.layout(
                    onMeasure = {
                        if (enabled.value) {
                            // read the model
                            model.value
                        }
                        actionExecuted = true
                    }
                ),
            ) { _, _ ->
                layout(10, 10) {}
            }
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model)
    }

    @Test
    fun layoutModifierReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.setContent {
            Layout(
                {},
                Modifier.layout(
                    onLayout = {
                        if (enabled.value) {
                            // read the model
                            model.value
                        }
                        actionExecuted = true
                    }
                ),
            ) { _, _ ->
                layout(10, 10) {}
            }
        }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        assertActionExecutedOnlyWhileEnabled(enabled, model)
    }

    @Test
    fun parentIsNotRemeasuredOrRelaidOutWhenChildMeasureModifierUsesState() {
        val model = mutableStateOf(0)
        var parentMeasureCount = 0
        var parentLayoutsCount = 0
        var childMeasured = false
        rule.setContent {
            Layout({
                Layout(
                    {},
                    Modifier.layout(
                        onMeasure = {
                            // read the model
                            model.value
                            childMeasured = true
                        }
                    ),
                ) { _, _ ->
                    layout(10, 10) {}
                }
            }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                parentMeasureCount++
                layout(placeable.width, placeable.height) {
                    parentLayoutsCount++
                    placeable.place(0, 0)
                }
            }
        }
        rule.waitForIdle()
        assertTrue(childMeasured)

        childMeasured = false
        rule.runOnIdle {
            assertEquals(1, parentMeasureCount)
            assertEquals(1, parentLayoutsCount)
            model.value++
        }

        rule.waitForIdle()
        assertTrue(childMeasured)

        rule.runOnIdle {
            assertEquals(1, parentMeasureCount)
            assertEquals(1, parentLayoutsCount)
        }
    }

    @Test
    fun parentIsNotRelaidOutWhenChildLayoutModifierUsesState() {
        val model = mutableStateOf(0)
        var parentLayoutsCount = 0
        var childLayouted = false
        rule.setContent {
            Layout({
                Layout(
                    {},
                    Modifier.layout(
                        onLayout = {
                            // read the model
                            model.value
                            childLayouted = true
                        }
                    ),
                ) { _, _ ->
                    layout(10, 10) {}
                }
            }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    parentLayoutsCount++
                    placeable.place(0, 0)
                }
            }
        }
        rule.waitForIdle()
        assertTrue(childLayouted)

        childLayouted = false
        rule.runOnIdle {
            assertEquals(1, parentLayoutsCount)
            model.value++
        }

        rule.waitForIdle()
        assertTrue(childLayouted)

        rule.runOnIdle { assertEquals(1, parentLayoutsCount) }
    }

    @Test
    fun stateReadForTheIntroducedLaterMeasureModifierIsObserved() {
        val model = mutableStateOf(0)
        var measured = false
        var modifier by mutableStateOf(Modifier.layout(onMeasure = { measured = true }))
        rule.setContent { Layout({}, modifier) { _, _ -> layout(10, 10) {} } }
        rule.waitForIdle()
        assertTrue(measured)

        measured = false
        rule.runOnIdle {
            modifier =
                Modifier.layout(
                    onMeasure = {
                        // read the model
                        model.value
                        measured = true
                    }
                )
        }
        rule.waitForIdle()
        assertTrue(measured)

        measured = false
        rule.runOnIdle { model.value++ }

        rule.waitForIdle()
        assertTrue(measured)
    }

    @Test
    fun stateReadForTheIntroducedLaterLayoutModifierIsObserved() {
        val model = mutableStateOf(0)
        var layouted = false
        var modifier by mutableStateOf(Modifier.layout(onLayout = { layouted = true }))
        rule.setContent { Layout({}, modifier) { _, _ -> layout(10, 10) {} } }
        rule.waitForIdle()
        assertTrue(layouted)

        layouted = false
        rule.runOnIdle {
            modifier =
                Modifier.layout(
                    onLayout = {
                        // read the model
                        model.value
                        layouted = true
                    }
                )
        }
        rule.waitForIdle()
        assertTrue(layouted)

        layouted = false
        rule.runOnIdle { model.value++ }

        rule.waitForIdle()
        assertTrue(layouted)
    }

    @Test
    fun stateChangeTriggersUpdateWhenDerivedStateIsUsedRightAfter() {
        val state = mutableStateOf(0)
        val derivedState = derivedStateOf { 0 }
        var layoutCount = 0
        rule.setContent {
            Layout({}) { _, _ ->
                state.value
                derivedState.value
                layoutCount++
                layout(10, 10) {}
            }
        }
        rule.runOnIdle {
            layoutCount = 0
            state.value++
        }
        rule.runOnIdle { assertEquals(1, layoutCount) }
    }

    private fun Modifier.layout(onMeasure: () -> Unit = {}, onLayout: () -> Unit = {}) =
        layout { measurable, constraints ->
            onMeasure()
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                onLayout()
                placeable.place(0, 0)
            }
        }

    fun assertActionExecutedOnlyWhileEnabled(
        enableModel: MutableState<Boolean>,
        valueModel: MutableState<Int>,
        triggeredByEnableSwitch: Boolean = true,
    ) {
        actionExecuted = false
        rule.runOnIdle { valueModel.value++ }
        rule.waitForIdle()
        assertTrue(actionExecuted)

        actionExecuted = false
        rule.runOnIdle { enableModel.value = false }
        rule.waitForIdle()
        if (triggeredByEnableSwitch) {
            assertTrue(actionExecuted)
        } else {
            assertFalse(actionExecuted)
        }

        actionExecuted = false
        rule.runOnIdle { valueModel.value++ }
        rule.waitForIdle()
        assertFalse(actionExecuted)
    }
}
