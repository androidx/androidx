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

package androidx.compose.ui.test

import androidx.compose.ui.unit.Density
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Variant of [runComposeUiTest] that allows you to specify the size of the surface.
 *
 * @param width the desired width of the surface
 * @param height the desired height of the surface
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 * `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context.
 */
@Deprecated(
    message =
        "Use `androidx.compose.ui.test.v2.runDesktopComposeUiTest` instead. The v2 APIs use " +
            "`StandardTestDispatcher` by default to better simulate production behavior where " +
            "coroutines are queued rather than executed immediately.",
    level = DeprecationLevel.WARNING,
)
@ExperimentalTestApi
fun runDesktopComposeUiTest(
    width: Int = 1024,
    height: Int = 768,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = Duration.INFINITE,
    block: suspend DesktopComposeUiTest.() -> Unit
) {
    kotlinx.coroutines.test.runTest {
        with(
            DesktopComposeUiTest(
                width = width,
                height = height,
                effectContext = effectContext,
                runTestContext = runTestContext,
                testTimeout = testTimeout,
                useStandardTestDispatcherForComposition = false
            )
        ) {
            runTest { block() }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, InternalTestApi::class)
@ExperimentalTestApi
class DesktopComposeUiTest(
    width: Int = 1024,
    height: Int = 768,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = Duration.INFINITE,
    density: Density = Density(1f),
    useStandardTestDispatcherForComposition: Boolean,
) : SkikoComposeUiTest(
    width = width,
    height = height,
    effectContext = effectContext,
    runTestContext = runTestContext,
    testTimeout = testTimeout,
    density = density,
    semanticsOwnerListener = null,
    windowInsets = null,
    useStandardTestDispatcherForComposition = useStandardTestDispatcherForComposition
) {
    private val idlingResources = mutableSetOf<IdlingResource>()

    override fun areAllResourcesIdle(): Boolean {
        return synchronized(idlingResources) {
            idlingResources.all { it.isIdleNow }
        }
    }

    fun registerIdlingResource(idlingResource: IdlingResource) {
        synchronized(idlingResources) {
            idlingResources.add(idlingResource)
        }
    }

    fun unregisterIdlingResource(idlingResource: IdlingResource) {
        synchronized(idlingResources) {
            idlingResources.remove(idlingResource)
        }
    }
}
