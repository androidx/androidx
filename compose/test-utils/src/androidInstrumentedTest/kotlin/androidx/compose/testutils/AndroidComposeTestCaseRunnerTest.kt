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

package androidx.compose.testutils

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidComposeTestCaseRunnerTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    internal fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>
        .forGivenContent(composable: @Composable () -> Unit): ComposeTestCaseSetup {
        return forGivenTestCase(
            object : ComposeTestCase {
                @Composable
                override fun Content() {
                    composable()
                }
            }
        )
    }

    @Test
    fun foreverRecomposing_viaModel_shouldFail() {
        val count = mutableStateOf(0)
        composeTestRule
            .forGivenContent {
                Text("Hello ${count.value}")
                count.value++
            }
            .performTestWithEventsControl {
                // Force the first recompose as the changes during initial composition are not
                // considered to invalidate the composition.
                count.value++
                assertFailsWith<AssertionError>("Changes are still pending after '10' frames.") {
                    doFramesAssertAllHadChangesExceptLastOne(10)
                }
            }
    }

    // @Test //- TODO: Does not work, performs only 1 frame until stable
    fun foreverRecomposing_viaState_shouldFail() {
        composeTestRule
            .forGivenContent {
                val state = remember { mutableStateOf(0) }
                Text("Hello ${state.value}")
                state.value++
            }
            .performTestWithEventsControl {
                assertFailsWith<AssertionError>("Changes are still pending after '10' frames.") {
                    doFramesAssertAllHadChangesExceptLastOne(10)
                }
            }
    }

    // @Test //- TODO: Does not work, performs only 1 frame until stable
    fun foreverRecomposing_viaStatePreCommit_shouldFail() {
        composeTestRule
            .forGivenContent {
                val state = remember { mutableStateOf(0) }
                Text("Hello ${state.value}")
                SideEffect { state.value++ }
            }
            .performTestWithEventsControl {
                assertFailsWith<AssertionError>("Changes are still pending after '10' frames.") {
                    doFramesAssertAllHadChangesExceptLastOne(10)
                }
            }
    }

    @Test
    fun recomposeZeroTime() {
        composeTestRule
            .forGivenContent {
                // Just empty composable
            }
            .performTestWithEventsControl {
                doFrame()
                assertNoPendingChanges()
            }
    }

    @Test
    fun recomposeZeroTime2() {
        composeTestRule
            .forGivenContent { Text("Hello") }
            .performTestWithEventsControl {
                doFrame()
                assertNoPendingChanges()
            }
    }

    @Test
    fun recomposeOnce() {
        composeTestRule
            .forGivenContent {
                val state = remember { mutableStateOf(0) }
                if (state.value < 1) {
                    state.value++
                }
            }
            .performTestWithEventsControl {
                doFrame()
                assertNoPendingChanges()
            }
    }

    // @Test //- TODO: Does not work, performs only 1 frame until stable
    fun recomposeTwice() {
        composeTestRule
            .forGivenContent {
                val state = remember { mutableStateOf(0) }
                if (state.value < 2) {
                    state.value++
                }
            }
            .performTestWithEventsControl { doFramesAssertAllHadChangesExceptLastOne(2) }
    }

    @Test
    fun recomposeTwice2() {
        val count = mutableStateOf(0)
        composeTestRule
            .forGivenContent {
                Text("Hello ${count.value}")
                if (count.value < 3) {
                    count.value++
                }
            }
            .performTestWithEventsControl {
                // Force the first recompose as the changes during initial composition are not
                // considered to invalidate the composition.
                count.value++
                doFramesAssertAllHadChangesExceptLastOne(2)
            }
    }

    @Test
    fun measurePositiveOnEmptyShouldFail() {
        composeTestRule
            .forGivenContent {
                // Just empty composable
            }
            .performTestWithEventsControl {
                doFrame()
                assertFailsWith<AssertionError> { assertMeasureSizeIsPositive() }
            }
    }

    @Test
    fun measurePositive() {
        composeTestRule
            .forGivenContent { Box { Text("Hello") } }
            .performTestWithEventsControl {
                doFrame()
                assertMeasureSizeIsPositive()
            }
    }

    @Test
    fun layout_preservesActiveFocus() {
        lateinit var focusState: FocusState
        composeTestRule
            .forGivenContent {
                val focusRequester = FocusRequester()
                Box(
                    Modifier.fillMaxSize()
                        .onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
            .performTestWithEventsControl {
                doFrame()
                assertThat(focusState.isFocused).isTrue()
            }
    }

    @Test
    fun countLaunchedCoroutines_noContentLaunches() {
        composeTestRule
            .forGivenContent { Box { Text("Hello") } }
            .performTestWithEventsControl { assertCoroutinesCount(0) }
    }

    @Test
    fun countLaunchedCoroutines_modifierLaunches() {
        val node =
            object : Modifier.Node() {
                override fun onAttach() {
                    super.onAttach()
                    coroutineScope.launch {}
                }
            }
        val element =
            object : ModifierNodeElement<Modifier.Node>() {
                override fun create(): Modifier.Node = node

                override fun update(node: Modifier.Node) {
                    // no op
                }

                override fun hashCode(): Int = 0

                override fun equals(other: Any?): Boolean = false
            }
        composeTestRule
            .forGivenContent { Box(Modifier.then(element)) { Text("Hello") } }
            .performTestWithEventsControl { assertCoroutinesCount(1) }
    }

    @Test
    fun countLaunchedCoroutines_launchedEffect() {
        composeTestRule
            .forGivenContent { LaunchedEffect(Unit) { launch {} } }
            .performTestWithEventsControl { assertCoroutinesCount(2) }
    }

    @Test
    fun countLaunchedCoroutines_scopeLaunches_lazy() {
        composeTestRule
            .forGivenContent {
                val scope = rememberCoroutineScope()
                Box(Modifier.clickable { scope.launch {} }) { Text("Hello") }
            }
            .performTestWithEventsControl { assertCoroutinesCount(0) }
    }

    @Test
    fun countLaunchedCoroutines_suspend() {
        composeTestRule
            .forGivenContent {
                LaunchedEffect(Unit) { suspendCancellableCoroutine {} }

                LaunchedEffect(Unit) { suspendCoroutine {} }
            }
            .performTestWithEventsControl { assertCoroutinesCount(2) }
    }

    @Test
    fun countLaunchedCoroutines_delay() {
        composeTestRule
            .forGivenContent {
                LaunchedEffect(Unit) { delay(1_000L) }

                LaunchedEffect(Unit) { launch {} }
            }
            .performTestWithEventsControl { assertCoroutinesCount(3) }
    }

    @Test
    fun countLaunchedCoroutines_yield() {
        composeTestRule
            .forGivenContent {
                LaunchedEffect(Unit) { yield() }

                LaunchedEffect(Unit) { launch {} }
            }
            .performTestWithEventsControl { assertCoroutinesCount(3) }
    }

    private inline fun <reified T : Throwable> assertFailsWith(
        expectedErrorMessage: String? = null,
        block: () -> Any
    ) {
        try {
            block()
        } catch (e: Throwable) {
            if (e !is T) {
                throw AssertionError("Expected exception not thrown, received: $e")
            }
            if (expectedErrorMessage != null && e.localizedMessage != expectedErrorMessage) {
                throw AssertionError(
                    "Expected error message not found, received: '" + "${e.localizedMessage}'"
                )
            }
            return
        }

        throw AssertionError("Expected exception not thrown")
    }
}
