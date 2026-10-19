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

package androidx.a2ui.compose.ui.tooling.preview

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Creates and remembers an [A2uiSurfaceModel] within a `@Preview` composable.
 *
 * Provided by [A2uiPreviewParameterProvider] so that catalog and surface initialization occur
 * inside composition.
 */
public fun interface A2uiSurfaceModelFactory {
    /** Creates and remembers the [A2uiSurfaceModel] in the current composition. */
    @Composable public fun create(): A2uiSurfaceModel
}

/**
 * Converts raw A2UI JSON protocol messages into [A2uiSurfaceModelFactory] instances for Compose
 * `@Preview` functions.
 *
 * Each list of messages must include at least a `createSurface` message, which initializes the
 * surface, and an `updateComponents` message, which defines the components to render. If the
 * components bind to values in the surface's data model, the list may also include
 * `updateDataModel` messages to populate it.
 *
 * Call [A2uiSurfaceModelFactory.create] inside the `@Preview` composable and pass the resulting
 * [A2uiSurfaceModel] to an A2UI surface composable, such as `A2uiSurface` when using the Compose
 * Material 3 A2UI APIs:
 * ```kotlin
 * val OrderConfirmationMessages = listOf(
 *     """{"version": "v0.9", "createSurface": {"surfaceId": "preview_surface", "catalogId": "..."}}""",
 *     """{"version": "v0.9", "updateComponents": {"surfaceId": "preview_surface", "components": [...]}}""",
 * )
 *
 * class MyOrderPreviewProvider : A2uiPreviewParameterProvider(
 *     catalog = { rememberMyCatalog() },
 *     namedJsonMessages = mapOf("Order Confirmation" to OrderConfirmationMessages),
 * )
 *
 * @Preview
 * @Composable
 * fun OrderPreview(
 *     @PreviewParameter(MyOrderPreviewProvider::class) surfaceFactory: A2uiSurfaceModelFactory
 * ) {
 *     A2uiSurface(surfaceModel = surfaceFactory.create())
 * }
 * ```
 */
public open class A2uiPreviewParameterProvider
private constructor(
    private val payloads: List<Pair<String?, List<String>>>,
    private val catalogProvider: @Composable () -> A2uiCatalog,
) : PreviewParameterProvider<A2uiSurfaceModelFactory> {

    /**
     * Creates a provider for multiple named surfaces using a `@Composable` catalog provider.
     *
     * @param catalogProvider `@Composable` provider returning the [A2uiCatalog] for the surfaces
     * @param namedJsonMessages map of display names to lists of raw A2UI JSON protocol strings,
     *   where each list must include a `createSurface` message
     */
    public constructor(
        catalogProvider: @Composable () -> A2uiCatalog,
        namedJsonMessages: Map<String, List<String>>,
    ) : this(
        payloads = namedJsonMessages.toList(),
        catalogProvider = catalogProvider,
    )

    /**
     * Creates a provider for a single surface using a `@Composable` catalog provider.
     *
     * @param catalogProvider `@Composable` provider returning the [A2uiCatalog] for the surface
     * @param jsonMessages list of raw A2UI JSON protocol strings for the surface, which must
     *   include a `createSurface` message
     */
    public constructor(
        catalogProvider: @Composable () -> A2uiCatalog,
        jsonMessages: List<String>,
    ) : this(
        payloads = listOf(null to jsonMessages),
        catalogProvider = catalogProvider,
    )

    /**
     * Creates a provider for multiple named surfaces using a static [catalog] instance.
     *
     * @param catalog [A2uiCatalog] defining the components available to the surfaces
     * @param namedJsonMessages map of display names to lists of raw A2UI JSON protocol strings,
     *   where each list must include a `createSurface` message
     */
    public constructor(
        catalog: A2uiCatalog,
        namedJsonMessages: Map<String, List<String>>,
    ) : this(
        payloads = namedJsonMessages.toList(),
        catalogProvider = { catalog },
    )

    /**
     * Creates a provider for a single surface using a static [catalog] instance.
     *
     * @param catalog [A2uiCatalog] defining the components available to the surface
     * @param jsonMessages list of raw A2UI JSON protocol strings for the surface, which must
     *   include a `createSurface` message
     */
    public constructor(
        catalog: A2uiCatalog,
        jsonMessages: List<String>,
    ) : this(
        payloads = listOf(null to jsonMessages),
        catalogProvider = { catalog },
    )

    override val values: Sequence<A2uiSurfaceModelFactory> by
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Array(payloads.size) { index ->
                    val jsonMessages = payloads[index].second
                    A2uiSurfaceModelFactory {
                        rememberA2uiPreviewSurface(
                            catalog = catalogProvider(),
                            jsonMessages = jsonMessages,
                        )
                    }
                }
                .asSequence()
        }

    override fun getDisplayName(index: Int): String? = payloads.getOrNull(index)?.first
}

/**
 * Creates and remembers an [A2uiSurfaceModel] from raw A2UI JSON messages for Compose `@Preview`s.
 *
 * Creates a new [A2uiSurfaceModel] whenever [catalog] or [jsonMessages] changes and disposes of the
 * previous one. The remembered surface is also disposed when this function leaves the composition.
 *
 * @param catalog [A2uiCatalog] defining the components available to this surface
 * @param jsonMessages list of raw JSON protocol messages, which must include a `createSurface`
 *   message
 * @return remembered [A2uiSurfaceModel]
 */
@Composable
public fun rememberA2uiPreviewSurface(
    catalog: A2uiCatalog,
    jsonMessages: List<String>,
): A2uiSurfaceModel {
    val scope = remember(catalog, jsonMessages) { CoroutineScope(SupervisorJob()) }
    val surfaceModel =
        remember(scope) {
            createA2uiPreviewSurfaceModel(
                catalog = catalog,
                jsonMessages = jsonMessages,
                scope = scope,
            )
        }
    DisposableEffect(scope) { onDispose { scope.cancel() } }
    return surfaceModel
}

/**
 * Synchronously creates and initializes an [A2uiSurfaceModel] from raw A2UI JSON messages for
 * Compose `@Preview`s or tests.
 *
 * @param catalog [A2uiCatalog] defining the components available to this surface
 * @param jsonMessages list of raw JSON protocol messages, which must include a `createSurface`
 *   message
 * @param scope [CoroutineScope] that processes the messages of this surface. Cancelling it disposes
 *   the returned [A2uiSurfaceModel]
 * @return fully initialized [A2uiSurfaceModel]
 */
internal fun createA2uiPreviewSurfaceModel(
    catalog: A2uiCatalog,
    jsonMessages: List<String>,
    scope: CoroutineScope,
): A2uiSurfaceModel {
    val parser = A2uiMessageParser()
    val processor = A2uiMessageProcessor(catalogs = listOf(catalog))
    scope.launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        processor.collectMessages()
    }

    for (i in jsonMessages.indices) {
        processor.processMessage(parser.parse(jsonMessages[i]))
    }

    val surfaces = processor.activeSurfaces.value
    require(surfaces.isNotEmpty()) {
        "No active surface was created. Ensure jsonMessages includes a createSurface message."
    }
    return surfaces[0]
}
