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

package androidx.compose.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.UiMediaScope.KeyboardKind
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.UiMediaScope.Posture
import androidx.compose.ui.UiMediaScope.ViewingDistance
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMediaQueryApi::class, ExperimentalTestApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class MediaQueryTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())
    private val scope = TestUiMediaScope()

    @Test
    fun derivedMediaQuery_returnsTrue_whenConditionMet() {
        scope.windowWidth = 100.dp
        var result = true

        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                val state by derivedMediaQuery { windowWidth > 50.dp }
                result = state
            }
        }

        assertThat(result).isTrue()
    }

    @Test
    fun derivedMediaQuery_returnsFalse_whenConditionNotMet() {
        scope.windowWidth = 100.dp
        var result = true

        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                val state by derivedMediaQuery { windowWidth > 200.dp }
                result = state
            }
        }

        assertThat(result).isFalse()
    }

    @Test
    fun derivedMediaQuery_updates_whenScopeChanges() {
        scope.windowWidth = 100.dp

        var result = false
        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                val state by derivedMediaQuery { windowWidth > 150.dp }
                result = state
            }
        }

        assertThat(result).isFalse()

        scope.windowWidth = 200.dp
        rule.waitForIdle()

        assertThat(result).isTrue()
    }

    @Test
    fun derivedMediaQuery_updates_whenCapturedValueChanges() {
        scope.windowWidth = 100.dp
        var threshold by mutableStateOf(50.dp)
        var queryResult = false

        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                // We delegate to a composable to ensure the lambda captures
                // a value parameter, not a state object.
                TestComponent(threshold = threshold, onResult = { queryResult = it })
            }
        }

        // 100.dp > 50.dp -> Should be true
        rule.runOnIdle { assertThat(queryResult).isTrue() }

        threshold = 150.dp
        rule.waitForIdle()

        // 100.dp > 150.dp -> Should be false
        assertThat(queryResult).isFalse()
    }

    @Test
    fun derivedMediaQuery_recomposesOnlyWhenResultChanges() {
        scope.windowWidth = 100.dp
        var recompositionCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                val result by derivedMediaQuery { windowWidth > 50.dp }

                SideEffect { recompositionCount++ }

                // Ensure we read result
                if (result) {
                    // no-op
                }
            }
        }

        rule.runOnIdle { assertThat(recompositionCount).isEqualTo(1) }

        // Change property, but query result remains true (100 -> 150, 150 > 50 is true)
        scope.windowWidth = 150.dp
        rule.waitForIdle()

        // Should not have recomposed
        assertThat(recompositionCount).isEqualTo(1)

        // Change property so query result changes (150 -> 40, 40 > 50 is false)
        scope.windowWidth = 40.dp
        rule.waitForIdle()

        // Should have recomposed
        assertThat(recompositionCount).isEqualTo(2)
    }

    @Test
    fun mediaQuery_updates_whenScopeChanges() {
        scope.windowWidth = 100.dp
        var result = false

        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                val state = mediaQuery { windowWidth > 150.dp }
                result = state
            }
        }

        assertThat(result).isFalse()

        scope.windowWidth = 200.dp
        rule.waitForIdle()

        assertThat(result).isTrue()
    }

    @Test
    fun mediaQuery_recomposes_whenScopeChanges_evenIfResultSame() {
        scope.windowWidth = 100.dp
        var recompositionCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides scope) {
                val result = mediaQuery { windowWidth > 50.dp }

                SideEffect { recompositionCount++ }

                if (result) {
                    // no-op
                }
            }
        }

        rule.runOnIdle { assertThat(recompositionCount).isEqualTo(1) }

        // Change property, but query result remains same (100 -> 150, 150 > 50 is true)
        // Should cause recomposition when `windowWidth` is read directly using basic `mediaQuery`
        scope.windowWidth = 150.dp
        rule.waitForIdle()

        assertThat(recompositionCount).isEqualTo(2)
    }

    @Test
    fun derivedMediaQuery_stateUpdates_withConfigurationChanges() {
        scope.windowWidth = 100.dp
        var result = false

        rule.setContent {
            val context = LocalContext.current
            val view = LocalView.current
            val windowInfo = LocalWindowInfo.current

            val mediaScope = obtainUiMediaScope(context, view, windowInfo)

            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowSize(DpSize(300.dp, 300.dp))
                ) {
                    val state by derivedMediaQuery { windowWidth > 200.dp }
                    result = state
                }
            }
        }

        rule.runOnIdle { assertThat(result).isTrue() }
    }

    @Composable
    private fun TestComponent(threshold: Dp, onResult: (Boolean) -> Unit) {
        val value by derivedMediaQuery { windowWidth > threshold }

        SideEffect { onResult(value) }
    }

    private class TestUiMediaScope : UiMediaScope {
        override var windowWidth: Dp by mutableStateOf(0.dp)
        override var windowHeight: Dp by mutableStateOf(0.dp)
        override var windowPosture: Posture by mutableStateOf(Posture.Flat)
        override var pointerPrecision: PointerPrecision by mutableStateOf(PointerPrecision.None)
        override var keyboardKind: KeyboardKind by mutableStateOf(KeyboardKind.None)
        override var hasMicrophone: Boolean by mutableStateOf(false)
        override var hasCamera: Boolean by mutableStateOf(false)
        override var viewingDistance: ViewingDistance by mutableStateOf(ViewingDistance.Near)
    }
}
