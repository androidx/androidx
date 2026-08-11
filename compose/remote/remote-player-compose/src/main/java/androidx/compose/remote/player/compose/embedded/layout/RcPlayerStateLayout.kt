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
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.player.compose.embedded.RcPlayerComponent
import androidx.compose.remote.player.compose.embedded.indexIdReflection
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun RcPlayerStateLayout(layout: StateLayout, modifier: Modifier) {
    val index by rememberRemoteIntAsState(layout.indexIdReflection)

    // StateLayout relies on remote-core's internal visibility toggle based on currentLayoutIndex
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val children = remember { ArrayList<Component>().apply { layout.getComponents(this) } }
        RcPlayerComponent(children[index])
    }
}
