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

package androidx.compose.runtime.a2ui

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope

@Stable
internal class A2uiComponentScopeImpl(
    private val id: String,
    private val surface: A2uiCoreSurfaceModel,
    private val surfaceScope: CoroutineScope,
) : A2uiComponentScope {

    @Composable
    override fun observeA2uiComponentState(id: String, dataScopePath: String?): A2uiComponentState {
        TODO("Not implemented yet")
    }

    override fun dispatchAction(actionPayload: Map<String, Any?>) {
        TODO("Not implemented yet")
    }

    override fun reportError(exception: A2uiException) {
        TODO("Not implemented yet")
    }
}
