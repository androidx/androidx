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

package androidx.a2ui.compose.ui.testing

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.LocalA2uiReadinessEvaluator
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.asReadinessEvaluator
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A test utility composable that mounts the root component of the specified [A2uiSurfaceModel] to
 * allow testing individual components or entire surfaces in isolation.
 *
 * This composable observes the reactive state of the root component within the [surface]. It
 * handles transitions between loading, error, and success states using the provided [onLoading] and
 * [onError] slots, and automatically provides the catalog's readiness evaluator to the composition.
 *
 * By default, if the root component fails to evaluate (for example, due to a schema validation
 * error or an unknown component type), this composable throws an [AssertionError]. This ensures
 * that tests fail predictably and explicitly expose the underlying [A2uiException].
 *
 * @sample androidx.a2ui.compose.ui.testing.samples.A2uiTestSurfaceSample
 * @param surface The [A2uiSurfaceModel] to render, typically returned by [A2uiTestController.start]
 *   or obtained from [A2uiTestController.surface].
 * @param modifier The [Modifier] to be applied to the root component's layout.
 * @param onLoading A composable to display while the component is resolving its payload or waiting
 *   for dynamic data bindings to become ready. By default, nothing is displayed during loading.
 * @param onError A composable to display if the component encounters a schema validation or runtime
 *   error. By default, this throws an [AssertionError] to fail the active test.
 */
@Composable
public fun A2uiTestSurface(
    surface: A2uiSurfaceModel,
    modifier: Modifier = Modifier,
    onLoading: @Composable (Modifier) -> Unit = {},
    onError: @Composable (A2uiException, Modifier) -> Unit = { exception, _ ->
        throw AssertionError("A2UI test surface failed to render: ${exception.message}", exception)
    },
) {
    surface as? A2uiCoreSurfaceModel
        ?: throw IllegalArgumentException("A2uiTestSurface requires an A2uiCoreSurfaceModel.")

    val composeCatalog =
        surface.catalog as? A2uiCatalog
            ?: throw IllegalArgumentException("Catalog must implement A2uiCatalog.")

    val readinessEvaluator = remember(composeCatalog) { composeCatalog.asReadinessEvaluator() }

    CompositionLocalProvider(LocalA2uiReadinessEvaluator provides readinessEvaluator) {
        when (val rootState = observeA2uiComponentState(surface = surface)) {
            is A2uiComponentState.Success -> {
                A2uiComponent(component = rootState.component, modifier = modifier)
            }
            is A2uiComponentState.Error -> {
                onError(rootState.exception, modifier)
            }
            is A2uiComponentState.Loading -> {
                onLoading(modifier)
            }
        }
    }
}
