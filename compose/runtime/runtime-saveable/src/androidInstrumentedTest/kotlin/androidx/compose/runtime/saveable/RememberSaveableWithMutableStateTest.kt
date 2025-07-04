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

package androidx.compose.runtime.saveable

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RememberSaveableWithMutableStateTest {

    @get:Rule val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun simpleRestore() {
        var state: MutableState<Int>? = null
        restorationTester.setContent { state = rememberSaveable { mutableStateOf(0) } }

        rule.runOnUiThread {
            assertThat(state!!.value).isEqualTo(0)

            state!!.value = 1
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(state!!.value).isEqualTo(1) }
    }

    @Test
    fun simpleRestoreList() {
        var state: SnapshotStateList<Int>? = null
        restorationTester.setContent { state = rememberSaveable { mutableStateListOf(0) } }

        rule.runOnUiThread {
            assertThat(state!![0]).isEqualTo(0)

            state!![0] = 1
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(state!![0]).isEqualTo(1) }
    }

    @Test
    fun simpleRestoreSet() {
        var state: SnapshotStateSet<Int>? = null
        restorationTester.setContent { state = rememberSaveable { mutableStateSetOf(0) } }

        rule.runOnUiThread {
            assertThat(state!!.contains(0)).isTrue()

            state!!.remove(0)
            state!!.add(1)
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(state!!.contains(1)).isTrue() }
    }

    @Test
    fun simpleSerializable() {
        var state: MutableState<SavedState>? = null
        restorationTester.setContent {
            state = rememberSerializable { mutableStateOf(savedState()) }
        }

        assertThat(state!!.value.read { contentDeepEquals(savedState()) }).isTrue()

        rule.runOnUiThread {
            state!!.value.write { putInt("key", 1) }
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        val expected = savedState { putInt("key", 1) }
        assertThat(state!!.value.read { contentDeepEquals(expected) }).isTrue()
    }

    @Test
    fun restoreWithSaver() {
        var state: MutableState<Holder>? = null
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = HolderSaver) { mutableStateOf(Holder(0)) }
        }

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(Holder(0))

            state!!.value.value = 1
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle { assertThat(state!!.value).isEqualTo(Holder(1)) }
    }

    @Test
    fun restoreWithSerializer() {
        var state: MutableState<Holder>? = null
        restorationTester.setContent {
            state =
                rememberSerializable(stateSerializer = HolderSerializer) {
                    mutableStateOf(Holder(0))
                }
        }

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(Holder(0))

            state!!.value.value = 1
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle { assertThat(state!!.value).isEqualTo(Holder(1)) }
    }

    @Test
    fun nullableStateRestoresNonNullValue() {
        var state: MutableState<String?>? = null
        restorationTester.setContent { state = rememberSaveable { mutableStateOf(null) } }

        rule.runOnUiThread {
            assertThat(state!!.value).isNull()

            state!!.value = "value"
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(state!!.value).isEqualTo("value") }
    }

    @Test
    fun nullableStateRestoresNullValue() {
        var state: MutableState<String?>? = null
        restorationTester.setContent { state = rememberSaveable { mutableStateOf("initial") } }

        rule.runOnUiThread {
            assertThat(state!!.value).isEqualTo("initial")

            state!!.value = null
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(state!!.value).isNull() }
    }

    @Test
    fun stateSaverReturnsNull() {
        var state: MutableState<String>? = null
        val saver = Saver<String, String>(save = { null }, restore = { it })
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = saver) { mutableStateOf("value") }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(state!!.value).isEqualTo("value") }
    }
}
