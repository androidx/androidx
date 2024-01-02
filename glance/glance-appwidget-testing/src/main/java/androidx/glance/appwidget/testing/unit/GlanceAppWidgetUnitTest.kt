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

package androidx.glance.appwidget.testing.unit

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.testing.GlanceNodeAssertionsProvider
import androidx.glance.testing.unit.GlanceMappedNode
import androidx.glance.testing.unit.MappedNode
import kotlin.time.Duration

/**
 * Sets up the test environment and runs the given unit [test block][block]. Use the methods on
 * [GlanceAppWidgetUnitTest] in the test to provide Glance composable content, find Glance elements
 * and make assertions on them.
 *
 * Test your individual Glance composable functions in isolation to verify that your logic outputs
 * right elements. For example: if input data is 'x', an image 'y' was
 * outputted. In sample below, the test class has a separate test for the header and the status
 * row.
 *
 * Tests can be run on JVM as these don't involve rendering the UI. If your logic depends on
 * [Context] or other android APIs, tests can be run on Android unit testing frameworks such as
 * [Robolectric](https://github.com/robolectric/robolectric).
 *
 * Note: Keeping a reference to the [GlanceAppWidgetUnitTest] outside of this function is an error.
 *
 * @sample androidx.glance.appwidget.testing.samples.isolatedGlanceComposableTestSamples
 *
 * @param timeout test time out; defaults to 10s
 * @param block The test block that involves calling methods in [GlanceAppWidgetUnitTest]
 */
// This and backing environment is based on pattern followed by
// "androidx.compose.ui.test.runComposeUiTest". Alternative of exposing testRule was explored, but
// it wasn't necessary for this case. If developers wish, they may use this function to create their
// own test rule.
fun runGlanceAppWidgetUnitTest(
    timeout: Duration = DEFAULT_TIMEOUT,
    block: GlanceAppWidgetUnitTest.() -> Unit
) = GlanceAppWidgetUnitTestEnvironment(timeout).runTest(block)

/**
 * Provides methods to enable you to test your logic of building Glance composable content in the
 * [runGlanceAppWidgetUnitTest] scope.
 *
 * @see [runGlanceAppWidgetUnitTest]
 */
sealed interface GlanceAppWidgetUnitTest :
    GlanceNodeAssertionsProvider<MappedNode, GlanceMappedNode> {
    /**
     * Sets the size of the appWidget to be assumed for the test. This corresponds to the
     * `LocalSize.current` composition local. If you are accessing the local size, you must
     * call this method to set the intended size for the test.
     *
     * Note: This should be called before calling [provideComposable].
     * Default is `349.dp, 455.dp` that of a 5x4 widget in Pixel 4 portrait mode. See
     * [GlanceAppWidgetUnitTestDefaults.size]
     *
     * 1. If your appWidget uses `sizeMode == Single`, you can set this to the `minWidth` and
     * `minHeight` set in your appwidget info xml.
     * 2. If your appWidget uses `sizeMode == Exact`, you can identify the sizes to test looking
     * at the documentation on
     * [Determine a size for your widget](https://developer.android.com/develop/ui/views/appwidgets/layouts#anatomy_determining_size).
     * and identifying landscape and portrait sizes that your widget may appear on.
     * 3. If your appWidget uses `sizeMode == Responsive`, you can set this to one of the sizes from
     * the list that you provide when specifying the sizeMode.
     */
    fun setAppWidgetSize(size: DpSize)

    /**
     * Sets the state to be used for the test if your composable under test accesses it via
     * `currentState<*>()` or `LocalState.current`.
     *
     * Default state is `null`. Note: This should be called before calling [provideComposable],
     * updates to the state after providing content has no effect. This matches the appWidget
     * behavior where you need to call `update` on the widget for state changes to take effect.
     *
     * @param state the state to be used for testing the composable.
     * @param T type of state used in your [GlanceStateDefinition] e.g. `Preferences` if your state
     *          definition is `GlanceStateDefinition<Preferences>`
     */
    fun <T> setState(state: T)

    /**
     * Sets the context to be used for the test.
     *
     * It is optional to call this method. However, you must set this if your composable needs
     * access to `LocalContext`. You may need to use a Android unit test framework such as
     * [Robolectric](https://github.com/robolectric/robolectric) to get the context.
     *
     * Note: This should be called before calling [provideComposable], updates to the state after
     * providing content has no effect
     */
    fun setContext(context: Context)

    /**
     * Sets the Glance composable function to be tested. Each unit test should test a composable in
     * isolation and assume specific state as input. Prefer keeping composables side-effects free.
     * Perform any state changes needed for the test before calling [provideComposable] or
     * [runGlanceAppWidgetUnitTest].
     *
     * @param composable the composable function under test
     */
    fun provideComposable(composable: @Composable () -> Unit)

    /**
     * Wait until all recompositions are calculated. For example if you have `LaunchedEffect` with
     * delays in your composable.
     */
    fun awaitIdle()
}

/**
 * Provides default values for various properties used in the Glance appWidget unit tests.
 */
object GlanceAppWidgetUnitTestDefaults {
    /**
     * [GlanceId] that can be assumed for state updates testing a Glance composable in isolation.
     */
    fun glanceId(): GlanceId = AppWidgetId(1)

    /**
     * Default size of the appWidget assumed in the unit tests. To override the size, use the
     * [GlanceAppWidgetUnitTest.setAppWidgetSize] function.
     *
     * The default `349.dp, 455.dp` is that of a 5x4 widget in Pixel 4 portrait mode.
     */
    fun size(): DpSize = DpSize(height = 349.dp, width = 455.dp)

    /**
     * Default category of the appWidget assumed in the unit tests.
     *
     * The default is `WIDGET_CATEGORY_HOME_SCREEN`
     */
    fun hostCategory(): Int = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
}
