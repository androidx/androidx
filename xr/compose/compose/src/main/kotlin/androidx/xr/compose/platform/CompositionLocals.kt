/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.xr.compose.subspace.SubspaceComposable

@OptIn(InternalSubspaceApi::class)
@Composable
internal fun ProvideCompositionLocals(
    owner: AndroidComposeSpatialElement,
    content: @Composable @SubspaceComposable () -> Unit,
) {
    val subspace =
        checkNotNull(owner.spatialComposeScene) { "Owner element must be attached to a subspace." }

    CompositionLocalProvider(
        LocalContext provides subspace.context,
        LocalLifecycleOwner provides subspace.lifecycleOwner,
        LocalSession provides subspace.jxrSession,
        content = content,
    )
}
