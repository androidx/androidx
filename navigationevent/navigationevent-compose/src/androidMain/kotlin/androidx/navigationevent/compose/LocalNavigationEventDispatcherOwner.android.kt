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

package androidx.navigationevent.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.ViewTreeHostDefaultKey
import androidx.compose.ui.platform.LocalContext
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.R

public actual val NavigationEventDispatcherOwnerHostDefaultKey:
    HostDefaultKey<NavigationEventDispatcherOwner?> =
    object : ViewTreeHostDefaultKey<NavigationEventDispatcherOwner?> {
        override val tagKey: Int
            get() = R.id.view_tree_navigation_event_dispatcher_owner
    }

@Composable
internal actual fun fallbackNavigationEventDispatcherOwner(): NavigationEventDispatcherOwner? =
    findOwner<NavigationEventDispatcherOwner>(LocalContext.current)

private inline fun <reified T> findOwner(context: Context): T? {
    var innerContext = context
    while (innerContext is ContextWrapper) {
        if (innerContext is T) {
            return innerContext
        }
        innerContext = innerContext.baseContext
    }
    return null
}
