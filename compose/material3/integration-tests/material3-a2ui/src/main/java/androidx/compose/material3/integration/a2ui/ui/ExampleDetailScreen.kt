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

import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.integration.a2ui.icons.ArrowBackIcon
import androidx.compose.material3.integration.a2ui.icons.CodeIcon
import androidx.compose.material3.integration.a2ui.model.CanonicalExample
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleDetailScreen(
    example: CanonicalExample,
    surfaceModel: A2uiSurfaceModel?,
    snackbarMessages: Flow<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    var showJsonSheet by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessages, snackbarHostState) {
        snackbarMessages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.semantics { heading() },
                title = { Text(example.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = ArrowBackIcon, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showJsonSheet = true }) {
                        Icon(imageVector = CodeIcon, contentDescription = "View A2UI JSON")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (example.description.isNotEmpty()) {
                Text(
                    text = example.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(),
                elevation = CardDefaults.elevatedCardElevation(),
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .drawDottedPattern(
                                dotColor =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                spacing = 16.dp,
                                dotRadius = 1.25.dp,
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (surfaceModel != null) {
                        A2uiSurface(
                            surfaceModel = surfaceModel,
                            modifier = Modifier.fillMaxWidth().heightIn(max = MaxSurfaceHeight),
                        )
                    }
                }
            }
        }
    }

    if (showJsonSheet) {
        JsonBottomSheet(
            sheetState = sheetState,
            rawJson = example.rawJson,
            onDismissRequest = { showJsonSheet = false },
        )
    }
}

private val MaxSurfaceHeight = 2000.dp
