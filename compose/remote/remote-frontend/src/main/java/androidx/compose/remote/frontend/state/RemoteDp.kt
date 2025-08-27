/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.state

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.layout.RemoteFloatContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

/**
 * Represents a Density-independent pixel (Dp) value.
 *
 * @property value The [RemoteFloat] that holds the actual Dp value.
 */
class RemoteDp(var value: RemoteFloat)

/**
 * A Composable function to remember and provide a [RemoteDp] value.
 *
 * @param content A lambda that takes a [RemoteFloatContext] (providing access to the remote
 *   creation state for float-related operations) and returns a [Dp] value.
 * @return A [RemoteDp] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
fun rememberRemoteDpValue(content: RemoteFloatContext.() -> Dp): RemoteDp {
    val state = LocalRemoteComposeCreationState.current
    val context = RemoteFloatContext(state)
    val valueId = state.document.addFloatConstant(content(context).value)
    val value = RemoteFloat(valueId)
    return remember { value.asRemoteDp() }
}
