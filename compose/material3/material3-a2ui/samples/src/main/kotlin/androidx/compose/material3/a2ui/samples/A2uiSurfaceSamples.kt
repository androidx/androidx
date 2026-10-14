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

package androidx.compose.material3.a2ui.samples

import android.content.Intent
import android.net.Uri
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.annotation.Sampled
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Sampled
@Composable
fun A2uiSurfaceSample() {
    val context = LocalContext.current
    val catalog =
        remember(context) {
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
                video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
                functions = A2uiBasicCatalogV1.Functions(context),
            )
        }
    // Note: The message processor should typically be hosted in a ViewModel.
    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        // Note: Message collection should typically be run on a background thread.
        launch(Dispatchers.Default) { processor.collectMessages() }

        val surfaceId = "surface_1"
        // Simulate payloads received from an agent to create the surface and its root component
        processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, catalog.id))
        processor.processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId,
                listOf(
                    A2uiComponentPayload(
                        id = "root",
                        type = "Text",
                        properties = mapOf("text" to "Hello, A2UI with Material 3!"),
                    )
                ),
            )
        )
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    // Note: The UI is typically expected to render all active surfaces (e.g., in a list).
    val surfaceModel = surfaces.firstOrNull()

    if (surfaceModel != null) {
        A2uiSurface(surfaceModel = surfaceModel, modifier = Modifier.fillMaxSize())
    }
}

@Sampled
@Composable
fun A2uiSurfaceCustomLoadingAndErrorContentSample() {
    val context = LocalContext.current
    val catalog =
        remember(context) {
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
                video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
                functions = A2uiBasicCatalogV1.Functions(context),
            )
        }
    // Note: The message processor should typically be hosted in a ViewModel.
    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        // Note: Message collection should typically be run on a background thread.
        launch(Dispatchers.Default) { processor.collectMessages() }

        val surfaceId = "surface_1"
        // Simulate payloads received from an agent to create the surface
        processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, catalog.id))

        // Simulate network latency before components arrive so that the loading content is visible
        delay(2000.milliseconds)

        // Populate its root component
        processor.processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId,
                listOf(
                    A2uiComponentPayload(
                        id = "root",
                        type = "Text",
                        properties = mapOf("text" to "Hello, A2UI with Material 3!"),
                    )
                ),
            )
        )
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    // Note: The UI is typically expected to render all active surfaces (e.g., in a list).
    val surfaceModel = surfaces.firstOrNull()

    if (surfaceModel != null) {
        A2uiSurface(
            surfaceModel = surfaceModel,
            modifier = Modifier.fillMaxSize(),
            loadingContent = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
            errorContent = { exception ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Failed to load surface: ${exception.message}",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            },
        )
    }
}

@Sampled
@Composable
fun A2uiSurfaceCustomTransitionSpecSample() {
    val context = LocalContext.current
    val catalog =
        remember(context) {
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
                video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
                functions = A2uiBasicCatalogV1.Functions(context),
            )
        }
    // Note: The message processor should typically be hosted in a ViewModel.
    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        // Note: Message collection should typically be run on a background thread.
        launch(Dispatchers.Default) { processor.collectMessages() }

        val surfaceId = "surface_1"
        // Simulate payloads received from an agent to create the surface
        processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, catalog.id))

        // Simulate network latency before components arrive to demonstrate the transition animation
        delay(2000.milliseconds)

        // Populate its root component
        processor.processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId,
                listOf(
                    A2uiComponentPayload(
                        id = "root",
                        type = "Text",
                        properties = mapOf("text" to "Hello, A2UI with Material 3!"),
                    )
                ),
            )
        )
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    // Note: The UI is typically expected to render all active surfaces (e.g., in a list).
    val surfaceModel = surfaces.firstOrNull()

    if (surfaceModel != null) {
        A2uiSurface(
            surfaceModel = surfaceModel,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) togetherWith
                        fadeOut(animationSpec = tween(600)))
                    .using(SizeTransform(clip = false))
            },
        )
    }
}

@Sampled
@Composable
fun A2uiSurfaceCustomOpenUrlFunctionSample() {
    val context = LocalContext.current
    val catalog =
        remember(context) {
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
                video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
                functions =
                    object : A2uiBasicCatalogV1.Functions {
                        // Only opens HTTPS URLs. Throwing reports the error to the agent. All
                        // other functions use their default implementations.
                        override fun openUrl(url: String, executionContext: A2uiExecutionContext) {
                            if (!url.startsWith("https://", ignoreCase = true)) {
                                throw A2uiException.A2uiRuntimeException(
                                    "Only HTTPS URLs are allowed"
                                )
                            }
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                            )
                        }
                    },
            )
        }
    // Note: The message processor should typically be hosted in a ViewModel.
    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        // Note: Message collection should typically be run on a background thread.
        launch(Dispatchers.Default) { processor.collectMessages() }

        val surfaceId = "surface_1"
        // Simulate payloads received from an agent to create a button that opens a URL
        processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, catalog.id))
        processor.processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId,
                listOf(
                    A2uiComponentPayload(
                        id = "root",
                        type = "Button",
                        properties =
                            mapOf(
                                "child" to "label",
                                "action" to
                                    mapOf(
                                        "functionCall" to
                                            mapOf(
                                                "call" to "openUrl",
                                                "args" to
                                                    mapOf("url" to "https://developer.android.com"),
                                            )
                                    ),
                            ),
                    ),
                    A2uiComponentPayload(
                        id = "label",
                        type = "Text",
                        properties = mapOf("text" to "Open developer.android.com"),
                    ),
                ),
            )
        )
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    // Note: The UI is typically expected to render all active surfaces (e.g., in a list).
    val surfaceModel = surfaces.firstOrNull()

    if (surfaceModel != null) {
        A2uiSurface(surfaceModel = surfaceModel, modifier = Modifier.fillMaxSize())
    }
}

@Sampled
@Composable
fun A2uiSurfaceCustomEmailFunctionSample() {
    val context = LocalContext.current
    val catalog =
        remember(context) {
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
                video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
                // Delegates to the default implementations of all functions, including openUrl,
                // and only overrides email.
                functions =
                    object : A2uiBasicCatalogV1.Functions by A2uiBasicCatalogV1.Functions(context) {
                        // Only accepts example.com addresses, and calls the default implementation
                        // to check that the value is a valid email address.
                        override fun email(
                            value: String,
                            executionContext: A2uiExecutionContext,
                        ): Boolean =
                            value.endsWith("@example.com", ignoreCase = true) &&
                                super.email(value, executionContext)
                    },
            )
        }
    // Note: The message processor should typically be hosted in a ViewModel.
    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        // Note: Message collection should typically be run on a background thread.
        launch(Dispatchers.Default) { processor.collectMessages() }

        val surfaceId = "surface_1"
        // Simulate payloads received from an agent to create an email field with an email check
        processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, catalog.id))
        processor.processMessage(
            A2uiUpdateDataModelMessage(surfaceId, path = "/email", value = "user@example.org")
        )
        processor.processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId,
                listOf(
                    A2uiComponentPayload(
                        id = "root",
                        type = "TextField",
                        properties =
                            mapOf(
                                "label" to "Work email",
                                "value" to mapOf("path" to "/email"),
                                "checks" to
                                    listOf(
                                        mapOf(
                                            "condition" to
                                                mapOf(
                                                    "call" to "email",
                                                    "args" to
                                                        mapOf("value" to mapOf("path" to "/email")),
                                                ),
                                            "message" to "Enter an example.com email address",
                                        )
                                    ),
                            ),
                    )
                ),
            )
        )
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    // Note: The UI is typically expected to render all active surfaces (e.g., in a list).
    val surfaceModel = surfaces.firstOrNull()

    if (surfaceModel != null) {
        A2uiSurface(surfaceModel = surfaceModel, modifier = Modifier.fillMaxSize())
    }
}
