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

package androidx.compose.remote.player.compose.embedded.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.player.compose.embedded.LocalRemoteActionHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach
import kotlin.math.roundToInt

/**
 * The document's click areas (`ClickArea` operations), each a clickable region labelled with its
 * content description. The embedded equivalent of the View player's `ClickAreaView`s: a click
 * reports the area's id and metadata to the player's action handler.
 */
@Composable
internal fun RcPlayerClickAreas(document: CoreDocument) {
    // Registered on the document while its operations are applied at setup.
    val clickAreas = remember(document) { document.clickAreas.toList() }
    if (clickAreas.isEmpty()) return
    val onAction = LocalRemoteActionHandler.current
    val density = LocalDensity.current
    clickAreas.fastForEach { area ->
        val label = area.contentDescription
        // Obscures click targets below this area, matching the View player's ClickAreaView.
        Box(
            modifier =
                Modifier.offset { IntOffset(area.left.roundToInt(), area.top.roundToInt()) }
                    .size(
                        with(density) { area.width().toDp() },
                        with(density) { area.height().toDp() },
                    )
                    .semantics { if (label != null) contentDescription = label }
                    .clickable { onAction(area.id, area.metadata) }
        )
    }
}
