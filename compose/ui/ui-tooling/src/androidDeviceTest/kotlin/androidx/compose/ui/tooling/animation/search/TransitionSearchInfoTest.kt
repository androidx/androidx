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

package androidx.compose.ui.tooling.animation.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.NoopClockInfo
import androidx.compose.ui.tooling.animation.Utils.addAnimations
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TransitionSearchInfoTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun searchInfoFound() {
        val search = AnimationSearch.TransitionSearch {}
        rule.addAnimations(search) { updateTransition(10.dp) }
        assertEquals(1, search.animations.size)

        search.animations.first().let { searchInfo ->
            val animation = searchInfo.createAnimation()
            assertNotNull(animation)
            animation!!
            val clock = searchInfo.createClock(animation, NoopClockInfo)
            assertNotNull(clock)
        }
    }

    @Test
    fun customLabel() {
        val search = AnimationSearch.TransitionSearch {}
        rule.addAnimations(search) { updateTransition(10.dp, label = "customLabel") }

        assertEquals("customLabel", search.animations.first().label)
    }

    @Test
    fun typeLabel() {
        val search = AnimationSearch.TransitionSearch {}
        rule.addAnimations(search) { updateTransition(10.dp) }

        assertEquals("Dp", search.animations.first().label)
    }

    @Test
    fun defaultLabel() {
        val search = AnimationSearch.TransitionSearch {}
        rule.addAnimations(search) { updateTransition(null) }

        assertEquals("updateTransition", search.animations.first().label)
    }

    @Test
    fun findInitialAndTargetDpStates() {
        val search = AnimationSearch.TransitionSearch {}
        val state = mutableStateOf(0)
        rule.addAnimations(search) {
            updateTransition(
                targetState =
                    when (state.value) {
                        0 -> 0.dp
                        1 -> 1.dp
                        else -> null
                    }
            )
        }
        val searchInfo = search.animations.first()
        searchInfo.setInitialStateToCurrentAnimationValue()
        assertEquals(0.dp, searchInfo.initialState)

        // Change target state.
        state.value = 1
        Snapshot.sendApplyNotifications()
        rule.waitForIdle()
        searchInfo.setTargetStateToCurrentAnimationValue()
        assertEquals(1.dp, searchInfo.targetState)

        // Change target state.
        state.value = 10
        Snapshot.sendApplyNotifications()
        rule.waitForIdle()
        searchInfo.setTargetStateToCurrentAnimationValue()
        assertNull(searchInfo.targetState)
    }

    @Test
    fun findInitialAndTargetDataClassStates() {
        val search = AnimationSearch.TransitionSearch {}
        val state = mutableStateOf(0)

        data class Data(val value: Int)

        rule.addAnimations(search) {
            updateTransition(
                targetState =
                    when (state.value) {
                        0 -> Data(0)
                        else -> Data(1)
                    }
            )
        }
        val searchInfo = search.animations.first()
        searchInfo.setInitialStateToCurrentAnimationValue()
        assertEquals(Data(0), searchInfo.initialState)

        // Change target state.
        state.value = 1
        Snapshot.sendApplyNotifications()
        rule.waitForIdle()
        searchInfo.setTargetStateToCurrentAnimationValue()
        assertEquals(Data(1), searchInfo.targetState)
    }

    @Test
    fun findInitialAndTargetNullableStates() {
        val search = AnimationSearch.TransitionSearch {}
        val state = mutableStateOf(0)

        rule.addAnimations(search) {
            updateTransition(
                targetState =
                    when (state.value) {
                        0 -> 0
                        else -> null
                    }
            )
        }
        val searchInfo = search.animations.first()
        searchInfo.setInitialStateToCurrentAnimationValue()
        assertEquals(0, searchInfo.initialState)

        // Change target state.
        state.value = 10
        Snapshot.sendApplyNotifications()
        rule.waitForIdle()
        searchInfo.setTargetStateToCurrentAnimationValue()
        assertNull(searchInfo.targetState)
    }
}
