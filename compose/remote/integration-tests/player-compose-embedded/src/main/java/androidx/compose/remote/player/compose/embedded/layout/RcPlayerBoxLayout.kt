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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.executeOperations
import androidx.compose.remote.player.compose.embedded.getDrawContentOperationsListReflection
import androidx.compose.remote.player.compose.embedded.horizontalPositioningReflection
import androidx.compose.remote.player.compose.embedded.verticalPositioningReflection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

@Composable
internal fun RcPlayerBox(layout: BoxLayout, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val drawOpsList = layout.getDrawContentOperationsListReflection()
    val drawModifier =
        if (drawOpsList != null) {
            Modifier.drawWithContent {
                executeOperations(
                    operations = drawOpsList,
                    remoteContext = remoteContext,
                    onDrawContent = { drawContent() },
                    graph = graph,
                )
            }
        } else Modifier

    Box(
        modifier = modifier.then(drawModifier),
        contentAlignment =
            boxContentAlignment(
                layout.horizontalPositioningReflection,
                layout.verticalPositioningReflection,
            ),
    ) {
        RcPlayerChildren(layout)
    }
}
