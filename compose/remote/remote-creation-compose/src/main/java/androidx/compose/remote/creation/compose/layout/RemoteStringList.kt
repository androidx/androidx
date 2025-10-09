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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.LogTodo
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntReference
import androidx.compose.remote.creation.compose.state.rememberRemoteIntValue
import androidx.compose.runtime.Composable

@Composable
public fun rememberRemoteStringList(vararg items: String): RemoteStringList {
    val state = LocalRemoteComposeCreationState.current
    return RemoteStringList(state.document.addStringList(*items))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteStringList(public var listId: Float) {

    @Composable
    public operator fun get(value: RemoteInt): RemoteIntReference {
        val state = LocalRemoteComposeCreationState.current

        if (state is NoRemoteCompose) {
            LogTodo("Fix preview mode for RemoteStringList")
        }

        val valueId = value.id.toInt()
        return RemoteIntReference(state.document.textLookup(listId, valueId))
    }

    @Composable
    public operator fun get(value: Int): RemoteIntReference {
        val state = LocalRemoteComposeCreationState.current

        if (state is NoRemoteCompose) {
            LogTodo("Fix preview mode for RemoteStringList")
        }

        val index = rememberRemoteIntValue { value }.id.toInt()
        return RemoteIntReference(state.document.textLookup(listId, index))
    }
}
