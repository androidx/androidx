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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.LocalA2uiReadinessEvaluator
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.asReadinessEvaluator
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Displays an A2UI surface styled with Material Design 3.
 *
 * This composable acts as the visual root for an A2UI surface. It observes the reactive state of
 * the root component within the provided [surfaceModel] and automatically handles transitions
 * between loading, error, and success states. It applies Material 3 design patterns for its default
 * loading indicator, error fallback, and animated transitions between layout changes.
 *
 * Note that data-only updates to the underlying [A2uiSurfaceModel] (e.g., text or binding changes
 * that do not alter the component hierarchy) do not trigger structural transition animations,
 * ensuring high-performance reactive updates.
 *
 * ### Setup and Initialization
 * To obtain a valid [surfaceModel] instance:
 * 1. Create an [A2uiCatalog] using the [androidx.a2ui.compose.ui.A2uiCatalog] or
 *    [androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1] factory functions.
 * 2. Create an [androidx.a2ui.model.processor.A2uiMessageProcessor] using the
 *    [androidx.a2ui.compose.ui.A2uiMessageProcessor] factory function with the catalog(s),
 *    typically hosted in a `ViewModel`.
 * 3. Run [androidx.a2ui.model.processor.A2uiMessageProcessor.collectMessages] on a background
 *    coroutine dispatcher to process agent messages.
 * 4. Collect [androidx.a2ui.model.processor.A2uiMessageProcessor.activeSurfaces] and pass the
 *    emitted [A2uiSurfaceModel] to this composable.
 *
 * For basic [A2uiSurface] usage:
 *
 * @sample androidx.compose.material3.a2ui.samples.A2uiSurfaceSample
 *
 * To customize the loading indicator and error fallback content:
 *
 * @sample androidx.compose.material3.a2ui.samples.A2uiSurfaceCustomLoadingAndErrorContentSample
 *
 * To customize transition animations between visual states:
 *
 * @sample androidx.compose.material3.a2ui.samples.A2uiSurfaceCustomTransitionSpecSample
 * @param surfaceModel the [A2uiSurfaceModel] containing the data, components, and catalog for this
 *   UI, typically obtained from [androidx.a2ui.model.processor.A2uiMessageProcessor.activeSurfaces]
 * @param modifier the [Modifier] to be applied to the surface layout
 * @param loadingContent the composable to display while the root component is loading or resolving
 *   its dynamic data bindings. By default, this uses [A2uiSurfaceDefaults.LoadingIndicator]
 * @param errorContent the composable to display if the root component fails to evaluate or render
 *   due to a validation or runtime error. By default, this uses [A2uiSurfaceDefaults.ErrorFallback]
 * @param transitionSpec the [ContentTransform] animation used when the root transitions between
 *   loading, error, and success states. By default, uses [A2uiSurfaceDefaults.transitionSpec]. Set
 *   to `null` to disable animations
 * @throws IllegalArgumentException if [surfaceModel] does not implement `A2uiCoreSurfaceModel`
 *   (e.g., if the message processor was not created using the
 *   [androidx.a2ui.compose.ui.A2uiMessageProcessor] factory function), or if its catalog does not
 *   implement [A2uiCatalog] (e.g., if the catalog was not created using the
 *   [androidx.a2ui.compose.ui.A2uiCatalog] factory function)
 */
@Composable
public fun A2uiSurface(
    surfaceModel: A2uiSurfaceModel,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { A2uiSurfaceDefaults.LoadingIndicator() },
    errorContent: @Composable (A2uiException) -> Unit = { A2uiSurfaceDefaults.ErrorFallback(it) },
    transitionSpec: (AnimatedContentTransitionScope<A2uiComponentState>.() -> ContentTransform)? =
        A2uiSurfaceDefaults.transitionSpec,
) {
    require(surfaceModel is A2uiCoreSurfaceModel) {
        "A2uiSurface requires an A2uiCoreSurfaceModel."
    }
    val composeCatalog =
        requireNotNull(surfaceModel.catalog as? A2uiCatalog) {
            "A2uiSurface requires an A2uiCatalog."
        }
    val readinessEvaluator = remember(composeCatalog) { composeCatalog.asReadinessEvaluator() }

    CompositionLocalProvider(LocalA2uiReadinessEvaluator provides readinessEvaluator) {
        val rootState = observeA2uiComponentState(surface = surfaceModel)
        if (transitionSpec != null) {
            AnimatedContent(
                targetState = rootState,
                contentKey = { state ->
                    when (state) {
                        is A2uiComponentState.Loading -> "loading"
                        is A2uiComponentState.Error -> "error"
                        is A2uiComponentState.Success ->
                            Pair(state.component.surface.id, state.component.type)
                    }
                },
                transitionSpec = transitionSpec,
                label = "A2uiSurfaceTransition",
                modifier = modifier,
            ) { targetState ->
                when (targetState) {
                    is A2uiComponentState.Loading -> loadingContent()
                    is A2uiComponentState.Error -> errorContent(targetState.exception)
                    is A2uiComponentState.Success ->
                        A2uiComponent(component = targetState.component)
                }
            }
        } else {
            Box(modifier = modifier) {
                when (rootState) {
                    is A2uiComponentState.Loading -> loadingContent()
                    is A2uiComponentState.Error -> errorContent(rootState.exception)
                    is A2uiComponentState.Success -> A2uiComponent(component = rootState.component)
                }
            }
        }
    }
}

/** Contains the defaults for [A2uiSurface] visual states and transitions. */
public object A2uiSurfaceDefaults {

    /**
     * The default loading indicator for use by [A2uiSurface] when the root component is in an
     * [A2uiComponentState.Loading] state.
     */
    @Composable
    public fun LoadingIndicator() {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    /**
     * The default error fallback displaying the error message for use by [A2uiSurface] when the
     * root component transitions to an [A2uiComponentState.Error] state.
     *
     * @param exception the [A2uiException] detailing the error that caused the render failure
     */
    @Composable
    public fun ErrorFallback(exception: A2uiException) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text = exception.message.orEmpty(), modifier = Modifier.padding(16.dp))
        }
    }

    /**
     * The default structural transition animation spec used by [A2uiSurface] when transitioning
     * between root component loading, success, and error states.
     */
    public val transitionSpec:
        AnimatedContentTransitionScope<A2uiComponentState>.() -> ContentTransform
        @Composable
        get() {
            // Effects specs are used for non-spatial opacity/color changes
            val defaultEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

            // Spatial specs are used for bounds/size/position changes
            val defaultSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()

            return remember(defaultEffectsSpec, fastEffectsSpec, defaultSpatialSpec) {
                {
                    (fadeIn(animationSpec = defaultEffectsSpec) togetherWith
                            fadeOut(animationSpec = fastEffectsSpec))
                        .using(
                            SizeTransform(
                                clip = false,
                                sizeAnimationSpec = { _, _ -> defaultSpatialSpec },
                            )
                        )
                }
            }
        }
}
