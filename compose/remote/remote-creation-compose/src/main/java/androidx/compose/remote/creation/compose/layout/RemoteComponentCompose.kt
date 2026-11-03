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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

internal val LocalRemoteComponentToken: ProvidableCompositionLocal<RemoteComponentToken?> =
    compositionLocalOf {
        null
    }

/**
 * Returns a [RemoteComponent] bound to the enclosing layout component.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteComponentSample
 *
 * * Captures the enclosing layout component's identity at composition time.
 * * Resolves [RemoteComponent] properties to that component's ID regardless of where the resulting
 *   [RemoteFloat] is consumed (such as in sibling modifiers, nested containers, or [RemoteCanvas]
 *   draw commands).
 *
 * @return [RemoteComponent] bound to the current layout component in composition
 */
@Composable
@RemoteComposable
public fun remoteComponent(): RemoteComponent {
    val token =
        checkNotNull(LocalRemoteComponentToken.current) {
            "remoteComponent() must be called inside a RemoteCompose layout component."
        }
    return remember(token) { RemoteComponent(token) }
}

/**
 * Returns a [RemoteComponent] bound to this [RemoteStateScope]'s active component.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteComponentDrawScopeSample
 *
 * * Resolves to the target layout component when called from a [RemoteModifier] or [RemoteAction]
 *   scope (such as [drawWithContent]).
 * * Resolves to the component currently being drawn when called from a [RemoteDrawScope] or
 *   [RemoteCanvas].
 *
 * @return [RemoteComponent] bound to the active component in this [RemoteStateScope]
 */
public fun RemoteStateScope.remoteComponent(): RemoteComponent {
    val token = resolveComponentToken()
    return if (token != null) RemoteComponent(token) else RemoteComponent(this)
}
