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

package androidx.compose.remote.integration.demos.main

import androidx.compose.material3.Text
import androidx.compose.remote.integration.demos.layout.RemoteBoxAlignmentsDemo
import androidx.compose.remote.integration.demos.layout.RemoteFlowRowDemo
import androidx.compose.remote.integration.demos.layout.RemoteStateLayoutSimpleDemo
import androidx.compose.remote.integration.demos.modifier.AlphaDemo
import androidx.compose.remote.integration.demos.modifier.ClickableDemo
import androidx.compose.remote.integration.demos.modifier.CombinedClickableDemo
import androidx.compose.remote.integration.demos.modifier.PaddingDemo
import androidx.compose.remote.integration.demos.modifier.TouchActionDemo
import androidx.compose.remote.integration.demos.player.BitmapLoaderDemo
import androidx.compose.remote.integration.demos.settings.SettingsScreen
import androidx.compose.runtime.Composable

private object ScreenKeys {
    const val REMOTE_BOX_ALIGNMENT = "REMOTE_BOX_ALIGNMENT"
    const val REMOTE_FLOW_ROW = "REMOTE_FLOW_ROW"
    const val REMOTE_STATE_LAYOUT = "REMOTE_STATE_LAYOUT"
    const val CLICKABLE = "CLICKABLE"
    const val COMBINED_CLICKABLE = "COMBINED_CLICKABLE"
    const val PADDING = "PADDING"
    const val ALPHA = "ALPHA"
    const val TOUCH_ACTION = "TOUCH_ACTION"
    const val BITMAP_LOADER = "BITMAP_LOADER"
    const val SETTINGS = "SETTINGS"
}

@Composable
fun ComposableScreenNavigation(key: String, onNavigateUp: () -> Unit) {
    when (key) {
        ScreenKeys.REMOTE_BOX_ALIGNMENT -> RemoteBoxAlignmentsDemo()
        ScreenKeys.REMOTE_FLOW_ROW -> RemoteFlowRowDemo()
        ScreenKeys.REMOTE_STATE_LAYOUT -> RemoteStateLayoutSimpleDemo()
        ScreenKeys.PADDING -> PaddingDemo()
        ScreenKeys.ALPHA -> AlphaDemo()
        ScreenKeys.CLICKABLE -> ClickableDemo()
        ScreenKeys.COMBINED_CLICKABLE -> CombinedClickableDemo()
        ScreenKeys.TOUCH_ACTION -> TouchActionDemo()
        ScreenKeys.BITMAP_LOADER -> BitmapLoaderDemo()
        ScreenKeys.SETTINGS -> SettingsScreen()
        else -> Text("Unknown screen: $key")
    }
}

val Screens =
    Category(
        key = "root",
        title = "Remote Compose Demos",
        screens =
            listOf(
                Category(
                    key = "layout_category",
                    title = "Layout",
                    screens =
                        listOf(
                            ComposableScreen(
                                key = ScreenKeys.REMOTE_BOX_ALIGNMENT,
                                title = "RemoteBox alignment",
                            ),
                            ComposableScreen(
                                key = ScreenKeys.REMOTE_FLOW_ROW,
                                title = "RemoteFlowRow",
                            ),
                            ComposableScreen(
                                key = ScreenKeys.REMOTE_STATE_LAYOUT,
                                title = "RemoteStateLayout",
                            ),
                        ),
                ),
                Category(
                    key = "action_category",
                    title = "Modifier",
                    screens =
                        listOf(
                            ComposableScreen(key = ScreenKeys.PADDING, title = "Padding"),
                            ComposableScreen(key = ScreenKeys.ALPHA, title = "Alpha"),
                            ComposableScreen(key = ScreenKeys.CLICKABLE, title = "Clickable"),
                            ComposableScreen(
                                key = ScreenKeys.COMBINED_CLICKABLE,
                                title = "CombinedClickable",
                            ),
                            ComposableScreen(key = ScreenKeys.TOUCH_ACTION, title = "TouchAction"),
                        ),
                ),
                Category(
                    key = "player_category",
                    title = "Player",
                    screens =
                        listOf(
                            ComposableScreen(key = ScreenKeys.BITMAP_LOADER, title = "BitmapLoader")
                        ),
                ),
            ),
    )

val SettingsScreen = ComposableScreen(key = ScreenKeys.SETTINGS, title = "Settings")
