/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.integration.demos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun MainScreen(
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    navigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transformationSpec = rememberTransformationSpec()
    val columnState = rememberTransformingLazyColumnState()
    val context = LocalContext.current
    val colorScheme =
        if (useDynamicColor) {
            dynamicColorScheme(context)
        } else {
            null
        } ?: MaterialTheme.colorScheme

    MaterialTheme(colorScheme = colorScheme) {
        ScreenScaffold(scrollState = columnState, modifier = modifier) { contentPadding ->
            TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
                item {
                    ListHeader(
                        modifier =
                            Modifier.fillMaxWidth()
                                .transformedHeight(
                                    scope = this,
                                    transformationSpec = transformationSpec,
                                ),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text(
                            "Remote Compose Wear Material3 Demos",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item {
                    SwitchButton(
                        modifier = Modifier.fillMaxWidth(),
                        checked = useDynamicColor,
                        onCheckedChange = onUseDynamicColorChange,
                    ) {
                        Text("Dynamic Color")
                    }
                }
                item {
                    MenuButton(
                        "RemoteButton",
                        onClick = { navigateToRoute(Screen.RemoteButtonDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteCompactButton",
                        onClick = { navigateToRoute(Screen.RemoteCompactButtonDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteIconButton",
                        onClick = { navigateToRoute(Screen.RemoteIconButtonDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteTextButton",
                        onClick = { navigateToRoute(Screen.RemoteTextButtonDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteButtonGroup",
                        onClick = { navigateToRoute(Screen.RemoteButtonGroupDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteIcon",
                        onClick = { navigateToRoute(Screen.RemoteIconDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteCircularProgressIndicator",
                        onClick = {
                            navigateToRoute(Screen.RemoteCircularProgressIndicatorDemosScreen.route)
                        },
                    )
                }
                item {
                    MenuButton(
                        "RemoteAppCard",
                        onClick = { navigateToRoute(Screen.RemoteAppCardDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteCard",
                        onClick = { navigateToRoute(Screen.RemoteCardDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteTitleCard",
                        onClick = { navigateToRoute(Screen.RemoteTitleCardDemosScreen.route) },
                    )
                }
                item {
                    MenuButton(
                        "RemoteText",
                        onClick = { navigateToRoute(Screen.RemoteTextDemosScreen.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@WearPreviewDevices
@Composable
private fun MainScreenPreview() {
    MainScreen(useDynamicColor = true, onUseDynamicColorChange = {}, navigateToRoute = {})
}
