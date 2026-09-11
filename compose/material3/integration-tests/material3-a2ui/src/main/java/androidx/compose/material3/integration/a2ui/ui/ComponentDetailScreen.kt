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

package androidx.compose.material3.integration.a2ui.ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.material3.integration.a2ui.icons.ArrowBackIcon
import androidx.compose.material3.integration.a2ui.icons.CodeIcon
import androidx.compose.material3.integration.a2ui.model.UiComponent
import androidx.compose.material3.integration.a2ui.ui.samples.AudioPlayerRenderer
import androidx.compose.material3.integration.a2ui.ui.samples.AudioPlayerSample
import androidx.compose.material3.integration.a2ui.ui.samples.ButtonSample
import androidx.compose.material3.integration.a2ui.ui.samples.CardSample
import androidx.compose.material3.integration.a2ui.ui.samples.CheckBoxSample
import androidx.compose.material3.integration.a2ui.ui.samples.ColumnSample
import androidx.compose.material3.integration.a2ui.ui.samples.ComingSoonSample
import androidx.compose.material3.integration.a2ui.ui.samples.DateTimeInputSample
import androidx.compose.material3.integration.a2ui.ui.samples.DividerSample
import androidx.compose.material3.integration.a2ui.ui.samples.IconSample
import androidx.compose.material3.integration.a2ui.ui.samples.ImageRenderer
import androidx.compose.material3.integration.a2ui.ui.samples.ImageSample
import androidx.compose.material3.integration.a2ui.ui.samples.ListSample
import androidx.compose.material3.integration.a2ui.ui.samples.ModalSample
import androidx.compose.material3.integration.a2ui.ui.samples.RowSample
import androidx.compose.material3.integration.a2ui.ui.samples.SliderSample
import androidx.compose.material3.integration.a2ui.ui.samples.TabsSample
import androidx.compose.material3.integration.a2ui.ui.samples.TextFieldSample
import androidx.compose.material3.integration.a2ui.ui.samples.TextSample
import androidx.compose.material3.integration.a2ui.ui.samples.VideoRenderer
import androidx.compose.material3.integration.a2ui.ui.samples.VideoSample
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMediaQueryApi::class)
@Composable
fun ComponentDetailScreen(
    component: UiComponent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    var showJsonSheet by remember { mutableStateOf(false) }

    val surfaceId = remember(component) { "demo_${component.name.lowercase()}" }
    val catalog = rememberDemoCatalog()
    val processor =
        rememberDemoMessageProcessor(catalog = catalog, snackbarHostState = snackbarHostState)

    val surfaces by processor.activeSurfaces.collectAsState()
    val surfaceModel = surfaces.firstOrNull { it.id == surfaceId }

    var currentComponents by remember { mutableStateOf<List<A2uiComponentPayload>>(emptyList()) }
    var currentDataModel by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    val showSideBySide by derivedMediaQuery {
        windowWidth >= 600.dp || (windowWidth > windowHeight && windowWidth >= 480.dp)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ComponentDetailTopBar(
                component = component,
                onBack = onBack,
                onShowJson = { showJsonSheet = true },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        ComponentDetailContent(
            component = component,
            surfaceModel = surfaceModel,
            showSideBySide = showSideBySide,
            innerPadding = innerPadding,
            onPayloadUpdated = { updatedComponentsPayload, updatedDataModel ->
                currentComponents = updatedComponentsPayload
                currentDataModel = updatedDataModel
                processor.updateSurface(
                    surfaceId = surfaceId,
                    catalogId = catalog.id,
                    components = updatedComponentsPayload,
                    dataModel = updatedDataModel,
                )
            },
        )
    }

    if (showJsonSheet) {
        JsonBottomSheet(
            sheetState = sheetState,
            surfaceId = surfaceId,
            components = currentComponents,
            dataModel = currentDataModel,
            onDismissRequest = { showJsonSheet = false },
        )
    }
}

@Composable
private fun rememberDemoMessageProcessor(
    catalog: A2uiCatalog,
    snackbarHostState: SnackbarHostState,
): A2uiMessageProcessor {
    val coroutineScope = rememberCoroutineScope()
    val actionInterceptor =
        remember(coroutineScope, snackbarHostState) {
            A2uiActionInterceptor { action ->
                coroutineScope.launch {
                    val actionName =
                        when (action) {
                            is A2uiEventAction -> action.eventName
                            is A2uiFunctionCallAction -> action.functionName
                        }
                    snackbarHostState.showSnackbar("Action dispatched: $actionName")
                }
                action
            }
        }

    val processor =
        remember(catalog, actionInterceptor) {
            A2uiMessageProcessor(
                catalogs = listOf(catalog),
                interceptors = listOf(actionInterceptor),
            )
        }

    LaunchedEffect(processor) { launch(Dispatchers.Default) { processor.collectMessages() } }

    return processor
}

private fun A2uiMessageProcessor.updateSurface(
    surfaceId: String,
    catalogId: String,
    components: List<A2uiComponentPayload>,
    dataModel: Map<String, Any?>,
) {
    if (components.isNotEmpty()) {
        processMessage(
            A2uiCreateSurfaceMessage(
                surfaceId = surfaceId,
                catalogId = catalogId,
            )
        )
        processMessage(
            A2uiUpdateDataModelMessage(
                surfaceId = surfaceId,
                path = "/",
                value = dataModel,
            )
        )
        processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId = surfaceId,
                components = components,
            )
        )
    }
}

@Composable
private fun ComponentDetailContent(
    component: UiComponent,
    surfaceModel: A2uiSurfaceModel?,
    showSideBySide: Boolean,
    innerPadding: PaddingValues,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    Grid(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
        config = {
            if (showSideBySide) {
                column(minmax(0.dp, 1.fr))
                column(minmax(0.dp, 1.fr))
                row(minmax(0.dp, 1.fr))
                columnGap(16.dp)
            } else {
                column(minmax(0.dp, 1.fr))
                row(GridTrackSize.Auto)
                row(minmax(0.dp, 1.fr))
            }
        },
    ) {
        PreviewPane(
            component = component,
            surfaceModel = surfaceModel,
            showSideBySide = showSideBySide,
            bottomPadding = innerPadding.calculateBottomPadding(),
        )

        ControlsPane(
            component = component,
            showSideBySide = showSideBySide,
            bottomPadding = innerPadding.calculateBottomPadding(),
            onPayloadUpdated = onPayloadUpdated,
        )
    }
}

@Composable
private fun PreviewPane(
    component: UiComponent,
    surfaceModel: A2uiSurfaceModel?,
    showSideBySide: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    ComponentPreviewCard(
        component = component,
        surfaceModel = surfaceModel,
        modifier =
            modifier.then(
                if (showSideBySide) {
                    Modifier.fillMaxSize()
                        .padding(
                            start = 16.dp,
                            top = 16.dp,
                            bottom = bottomPadding + 16.dp,
                        )
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(240.dp)
                }
            ),
    )
}

@Composable
private fun ControlsPane(
    component: UiComponent,
    showSideBySide: Boolean,
    bottomPadding: Dp,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controlsScrollState = rememberScrollState()
    Column(
        modifier =
            modifier
                .then(
                    if (showSideBySide) {
                        Modifier.fillMaxSize().padding(end = 16.dp)
                    } else {
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    }
                )
                .verticalFadingEdge(
                    scrollState = controlsScrollState,
                    fadeHeight = 32.dp,
                    bottomOffset = bottomPadding,
                )
                .verticalScroll(controlsScrollState)
                .padding(
                    top = if (showSideBySide) 16.dp else 8.dp,
                    bottom = bottomPadding + 16.dp,
                )
    ) {
        ComponentControlsSection(
            component = component,
            onPayloadUpdated = onPayloadUpdated,
        )
    }
}

@Composable
private fun rememberDemoCatalog() = remember {
    materialA2uiBasicCatalogV1(
        image =
            MaterialA2uiBasicCatalogV1Defaults.image {
                url,
                contentDescription,
                contentScale,
                modifier,
                _ ->
                ImageRenderer(
                    url = url,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier,
                )
            },
        video =
            MaterialA2uiBasicCatalogV1Defaults.video { url, modifier, onError ->
                VideoRenderer(
                    url = url,
                    modifier = modifier,
                    onError = onError,
                )
            },
        audioPlayer =
            MaterialA2uiBasicCatalogV1Defaults.audioPlayer {
                url,
                contentDescription,
                modifier,
                onError ->
                AudioPlayerRenderer(
                    url = url,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    onError = onError,
                )
            },
        urlOpener = {},
        messageFormatter = { pattern, _, _ -> pattern },
        localeProvider = A2uiLocaleProvider.Default,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentDetailTopBar(
    component: UiComponent,
    onBack: () -> Unit,
    onShowJson: () -> Unit,
) {
    TopAppBar(
        modifier = Modifier.semantics { heading() },
        title = { Text(component.displayName) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = ArrowBackIcon, contentDescription = "Back")
            }
        },
        actions = {
            if (component.isSupported) {
                IconButton(onClick = onShowJson) {
                    Icon(imageVector = CodeIcon, contentDescription = "View A2UI JSON")
                }
            }
        },
    )
}

@Composable
private fun ComponentPreviewCard(
    component: UiComponent,
    surfaceModel: A2uiSurfaceModel?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .drawDottedPattern(
                        dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                        spacing = 16.dp,
                        dotRadius = 1.25.dp,
                    )
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (component.isSupported) {
                if (surfaceModel != null) {
                    val surfaceModifier =
                        when (component) {
                            UiComponent.ROW ->
                                Modifier.fillMaxWidth()
                                    .wrapContentHeight(Alignment.CenterVertically)
                            UiComponent.COLUMN,
                            UiComponent.LIST,
                            UiComponent.DIVIDER -> Modifier.fillMaxSize()
                            UiComponent.TABS ->
                                Modifier.fillMaxWidth()
                                    .wrapContentHeight(Alignment.CenterVertically)
                            UiComponent.SLIDER,
                            UiComponent.DATE_TIME_INPUT ->
                                Modifier.fillMaxWidth()
                                    .wrapContentHeight(Alignment.CenterVertically)
                            else -> Modifier.wrapContentSize(Alignment.Center)
                        }
                    A2uiSurface(surfaceModel = surfaceModel, modifier = surfaceModifier)
                }
            } else {
                Text(
                    text = "Preview coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ComponentControlsSection(
    component: UiComponent,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    when (component) {
        UiComponent.ROW -> RowSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.COLUMN -> ColumnSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.BUTTON -> ButtonSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.TEXT -> TextSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.CARD -> CardSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.ICON -> IconSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.IMAGE -> ImageSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.DIVIDER -> DividerSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.LIST -> ListSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.TABS -> TabsSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.SLIDER -> SliderSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.CHECK_BOX -> CheckBoxSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.DATE_TIME_INPUT -> DateTimeInputSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.VIDEO -> VideoSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.AUDIO_PLAYER -> AudioPlayerSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.TEXT_FIELD -> TextFieldSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.MODAL -> ModalSample(onPayloadUpdated = onPayloadUpdated)
        UiComponent.CHOICE_PICKER -> ComingSoonSample(component = component)
    }
}
