/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotStateObserverTestsCommon {

    @Test
    fun stateChangeTriggersCallback() {
        val data = ValueWrapper("Hello World")
        var changes = 0

        val state = mutableIntStateOf(0)
        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            val onChangeListener: (ValueWrapper) -> Unit = { affected ->
                assertEquals(data, affected)
                assertEquals(0, changes)
                changes++
            }

            stateObserver.observeReads(data, onChangeListener) {
                // read the value
                state.intValue
            }

            Snapshot.notifyObjectsInitialized()
            state.intValue++
            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun multipleStagesWorksTogether() {
        val strStage1 = ValueWrapper("Stage1")
        val strStage2 = ValueWrapper("Stage2")
        val strStage3 = ValueWrapper("Stage3")
        var stage1Changes = 0
        var stage2Changes = 0
        var stage3Changes = 0
        val stage1Model = mutableIntStateOf(0)
        val stage2Model = mutableIntStateOf(0)
        val stage3Model = mutableIntStateOf(0)

        val onChangeStage1: (ValueWrapper) -> Unit = { affectedData ->
            assertEquals(strStage1, affectedData)
            assertEquals(0, stage1Changes)
            stage1Changes++
        }
        val onChangeStage2: (ValueWrapper) -> Unit = { affectedData ->
            assertEquals(strStage2, affectedData)
            assertEquals(0, stage2Changes)
            stage2Changes++
        }
        val onChangeStage3: (ValueWrapper) -> Unit = { affectedData ->
            assertEquals(strStage3, affectedData)
            assertEquals(0, stage3Changes)
            stage3Changes++
        }
        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(strStage1, onChangeStage1) { stage1Model.intValue }

            stateObserver.observeReads(strStage2, onChangeStage2) { stage2Model.intValue }

            stateObserver.observeReads(strStage3, onChangeStage3) { stage3Model.intValue }

            Snapshot.notifyObjectsInitialized()

            stage1Model.intValue++
            stage2Model.intValue++
            stage3Model.intValue++

            Snapshot.sendApplyNotifications()

            assertEquals(1, stage1Changes)
            assertEquals(1, stage2Changes)
            assertEquals(1, stage3Changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun enclosedStagesCorrectlyObserveChanges() {
        val stage1Info = ValueWrapper("stage 1")
        val stage2Info1 = ValueWrapper("stage 1 - value 1")
        val stage2Info2 = ValueWrapper("stage 2 - value 2")
        var stage1Changes = 0
        var stage2Changes1 = 0
        var stage2Changes2 = 0
        val stage1Data = mutableIntStateOf(0)
        val stage2Data1 = mutableIntStateOf(0)
        val stage2Data2 = mutableIntStateOf(0)

        val onChangeStage1Listener: (ValueWrapper) -> Unit = { affected ->
            assertEquals(affected, stage1Info)
            assertEquals(stage1Changes, 0)
            stage1Changes++
        }
        val onChangeState2Listener: (ValueWrapper) -> Unit = { affected ->
            when (affected) {
                stage2Info1 -> {
                    assertEquals(0, stage2Changes1)
                    stage2Changes1++
                }
                stage2Info2 -> {
                    assertEquals(0, stage2Changes2)
                    stage2Changes2++
                }
                stage1Info -> {
                    error("stage 1 called in stage 2")
                }
            }
        }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(stage2Info1, onChangeState2Listener) {
                stage2Data1.intValue
                stateObserver.observeReads(stage2Info2, onChangeState2Listener) {
                    stage2Data2.intValue
                    stateObserver.observeReads(stage1Info, onChangeStage1Listener) {
                        stage1Data.intValue
                    }
                }
            }

            Snapshot.notifyObjectsInitialized()

            stage2Data1.intValue++
            stage2Data2.intValue++
            stage1Data.intValue++

            Snapshot.sendApplyNotifications()

            assertEquals(1, stage1Changes)
            assertEquals(1, stage2Changes1)
            assertEquals(1, stage2Changes2)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun stateReadTriggersCallbackAfterSwitchingAdvancingGlobalWithinObserveReads() {
        val info = ValueWrapper("Hello")
        var changes = 0

        val state = mutableIntStateOf(0)
        val onChangeListener: (ValueWrapper) -> Unit = { _ ->
            assertEquals(0, changes)
            changes++
        }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(info, onChangeListener) {
                // Create a sub-snapshot
                // this will be done by subcomposition, for example.
                val snapshot = Snapshot.takeMutableSnapshot()
                try {
                    // read the value
                    snapshot.enter { state.intValue }
                    snapshot.apply().check()
                } finally {
                    snapshot.dispose()
                }
            }

            state.intValue++

            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun pauseStopsObserving() {
        val data = ValueWrapper("data")
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                stateObserver.withNoObservations { state.value }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun withoutReadObservationStopsObserving() {
        val data = ValueWrapper("data")
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                Snapshot.withoutReadObservation { state.value }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun changeAfterWithoutReadObservationIsObserving() {
        val data = ValueWrapper("data")
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                Snapshot.withoutReadObservation { state.value }
                state.value
            }
        }

        assertEquals(1, changes)
    }

    @Suppress("DEPRECATION")
    @Test
    fun nestedPauseStopsObserving() {
        val data = ValueWrapper("data")
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                stateObserver.withNoObservations {
                    stateObserver.withNoObservations { state.value }
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun nestedWithoutReadObservation() {
        val data = ValueWrapper("data")
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                Snapshot.withoutReadObservation {
                    Snapshot.withoutReadObservation { state.value }
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun simpleObserving() {
        val data = ValueWrapper("data")
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) { state.value }
        }

        assertEquals(1, changes)
    }

    @Suppress("DEPRECATION")
    @Test
    fun observeWithinPause() {
        val data = ValueWrapper("data")
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes1++ }) {
                stateObserver.withNoObservations {
                    stateObserver.observeReads(data, { _ -> changes2++ }) { state.value }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(1, changes2)
    }

    @Test
    fun observeWithinWithoutReadObservation() {
        val data = ValueWrapper("data")
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes1++ }) {
                Snapshot.withoutReadObservation {
                    stateObserver.observeReads(data, { changes2++ }) { state.value }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(1, changes2)
    }

    @Test
    fun withoutReadsPausesNestedObservation() {
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(ValueWrapper("scope1"), { changes1++ }) {
                stateObserver.observeReads(ValueWrapper("scope2"), { changes2++ }) {
                    Snapshot.withoutReadObservation { state.value }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(0, changes2)
    }

    @Test
    fun withoutReadsPausesNestedObservationWhenNewMutableSnapshotIsEnteredWithin() {
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(ValueWrapper("scope1"), { changes1++ }) {
                stateObserver.observeReads(ValueWrapper("scope2"), { changes2++ }) {
                    Snapshot.withoutReadObservation {
                        val newSnapshot = Snapshot.takeMutableSnapshot()
                        newSnapshot.enter { state.value }
                        newSnapshot.apply().check()
                        newSnapshot.dispose()
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(0, changes2)
    }

    @Test
    fun withoutReadsPausesNestedObservationWhenNewSnapshotIsEnteredWithin() {
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(ValueWrapper("scope1"), { changes1++ }) {
                stateObserver.observeReads(ValueWrapper("scope2"), { changes2++ }) {
                    Snapshot.withoutReadObservation {
                        val newSnapshot = Snapshot.takeSnapshot()
                        newSnapshot.enter { state.value }
                        newSnapshot.dispose()
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(0, changes2)
    }

    @Test
    fun withoutReadsInReadOnlySnapshot() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                val newSnapshot = Snapshot.takeSnapshot()
                newSnapshot.enter { Snapshot.withoutReadObservation { state.value } }
                newSnapshot.dispose()
            }
        }
        assertEquals(0, changes)
    }

    @Test
    fun derivedStateOfInvalidatesObserver() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value }

            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                // read
                derivedState.value
            }
        }
        assertEquals(1, changes)
    }

    @Suppress("MutableCollectionMutableState") // The point of this test
    @Test
    fun derivedStateOfReferentialChangeDoesNotInvalidateObserver() {
        var changes = 0

        runSimpleTest { stateObserver, _ ->
            val state = mutableStateOf(mutableListOf(42), referentialEqualityPolicy())
            val derivedState = derivedStateOf { state.value }

            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                // read
                derivedState.value
            }

            state.value = mutableListOf(42)
        }
        assertEquals(0, changes)
    }

    @Test
    fun nestedDerivedStateOfInvalidatesObserver() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value }
            val derivedState2 = derivedStateOf { derivedState.value }

            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                // read
                derivedState2.value
            }
        }
        assertEquals(1, changes)
    }

    @Suppress("MutableCollectionMutableState") // The point of this test
    @Test
    fun derivedStateOfWithReferentialMutationPolicy() {
        var changes = 0

        runSimpleTest { stateObserver, _ ->
            val state = mutableStateOf(mutableListOf(1), referentialEqualityPolicy())
            val derivedState = derivedStateOf(referentialEqualityPolicy()) { state.value }

            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                // read
                derivedState.value
            }

            state.value = mutableListOf(1)
        }
        assertEquals(1, changes)
    }

    @Suppress("MutableCollectionMutableState") // The point of this test
    @Test
    fun derivedStateOfWithStructuralMutationPolicy() {
        var changes = 0

        runSimpleTest { stateObserver, _ ->
            val state = mutableStateOf(mutableListOf(1), referentialEqualityPolicy())
            val derivedState = derivedStateOf(structuralEqualityPolicy()) { state.value }

            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                // read
                derivedState.value
            }

            state.value = mutableListOf(1)
        }
        assertEquals(0, changes)
    }

    @Test
    fun readingDerivedStateAndDependencyInvalidates() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value >= 0 }

            stateObserver.observeReads(ValueWrapper("scope"), { changes++ }) {
                // read derived state
                derivedState.value
                // read dependency
                state.value
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun readingDerivedStateWithDependencyChangeInvalidates() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val state2 = mutableStateOf(false)
            val derivedState = derivedStateOf {
                if (state2.value) {
                    state.value
                } else {
                    null
                }
            }
            val onChange: (ValueWrapper) -> Unit = { changes++ }

            val scope = ValueWrapper("scope")
            stateObserver.observeReads(scope, onChange) {
                // read derived state
                derivedState.value
            }

            state2.value = true
            // advance snapshot
            Snapshot.sendApplyNotifications()
            Snapshot.notifyObjectsInitialized()

            stateObserver.observeReads(scope, onChange) {
                // read derived state
                derivedState.value
            }
        }
        assertEquals(2, changes)
    }

    @Test
    fun readingDerivedStateConditionallyInvalidatesBothScopes() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value }

            val onChange: (ValueWrapper) -> Unit = { changes++ }
            stateObserver.observeReads(ValueWrapper("scope"), onChange) {
                // read derived state
                derivedState.value
            }

            val scope2 = ValueWrapper("other scope")
            // read the same state in other scope
            stateObserver.observeReads(scope2, onChange) { derivedState.value }

            // advance snapshot to invalidate reads
            Snapshot.notifyObjectsInitialized()

            // stop observing state in other scope
            stateObserver.observeReads(scope2, onChange) {
                /* no-op */
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun testRecursiveApplyChanges_SingleRecursive() {
        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableIntStateOf(0)
        val state2 = mutableIntStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            val onChange: (ValueWrapper) -> Unit = { scope ->
                if (scope.s == "scope" && state1.intValue < 2) {
                    state1.intValue++
                    Snapshot.sendApplyNotifications()
                }
            }

            stateObserver.observeReads(ValueWrapper("scope"), onChange) {
                state1.intValue
                state2.intValue
            }

            repeat(10) {
                stateObserver.observeReads(ValueWrapper("scope $it"), onChange) {
                    state1.intValue
                    state2.intValue
                }
            }

            state1.intValue++
            state2.intValue++

            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun testRecursiveApplyChanges_MultiRecursive() {
        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableIntStateOf(0)
        val state2 = mutableIntStateOf(0)
        val state3 = mutableIntStateOf(0)
        val state4 = mutableIntStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            val onChange: (ValueWrapper) -> Unit = { scope ->
                if (scope.s == "scope" && state1.intValue < 2) {
                    state1.intValue++
                    Snapshot.sendApplyNotifications()
                    state2.intValue++
                    Snapshot.sendApplyNotifications()
                    state3.intValue++
                    Snapshot.sendApplyNotifications()
                    state4.intValue++
                    Snapshot.sendApplyNotifications()
                }
            }

            stateObserver.observeReads(ValueWrapper("scope"), onChange) {
                state1.intValue
                state2.intValue
                state3.intValue
                state4.intValue
            }

            repeat(10) {
                stateObserver.observeReads(ValueWrapper("scope $it"), onChange) {
                    state1.intValue
                    state2.intValue
                    state3.intValue
                    state4.intValue
                }
            }

            state1.intValue++
            state2.intValue++
            state3.intValue++
            state4.intValue++

            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun readingValueAfterClearInvalidates() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val changeBlock: (Any) -> Unit = { changes++ }
            // record observation
            val s = ValueWrapper("scope")
            stateObserver.observeReads(s, changeBlock) {
                // read state
                state.value
            }

            // clear scope
            stateObserver.clear(s)

            // record again
            stateObserver.observeReads(s, changeBlock) {
                // read state
                state.value
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun readingDerivedState_invalidatesWhenValueNotChanged() {
        var changes = 0
        val changeBlock: (Any) -> Unit = { changes++ }

        runSimpleTest { stateObserver, state ->
            var condition by mutableStateOf(false)
            val derivedState = derivedStateOf {
                // the same initial value for both branches
                if (condition) state.value else 0
            }

            // record observation
            stateObserver.observeReads("scope", changeBlock) {
                // read state
                derivedState.value
            }

            condition = true
            Snapshot.sendApplyNotifications()
        }
        assertEquals(1, changes)
    }

    @Test
    fun readingDerivedState_invalidatesIfReadBeforeSnapshotAdvance() {
        var changes = 0
        val changeBlock: (Any) -> Unit = {
            if (it == "draw_1") {
                changes++
            }
        }

        runSimpleTest { stateObserver, layoutState ->
            val derivedState = derivedStateOf { layoutState.value }

            // record observation for a draw scope
            stateObserver.observeReads("draw", changeBlock) { derivedState.value }

            // record observation for a different draw scope
            stateObserver.observeReads("draw_1", changeBlock) { derivedState.value }

            Snapshot.sendApplyNotifications()

            // record
            layoutState.value += 1

            // record observation for the first draw scope
            stateObserver.observeReads("draw", changeBlock) {
                // read state
                derivedState.value
            }

            // second block should be invalidated after we read the value
            assertEquals(1, changes)

            // record observation for the second draw scope
            stateObserver.observeReads("draw_1", changeBlock) {
                // read state
                derivedState.value
            }
        }
        assertEquals(2, changes)
    }

    private fun runSimpleTest(
        block: (modelObserver: SnapshotStateObserver, data: MutableState<Int>) -> Unit
    ) {
        val stateObserver = SnapshotStateObserver { it() }
        val state = mutableIntStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()
            block(stateObserver, state)
            state.intValue++
            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }
}

// In k/js string is a primitive type and it doesn't have identityHashCode
private class ValueWrapper(val s: String)
