/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

@Deprecated(
    message = "Renamed to PlatformLayersComposeScene",
    replaceWith = ReplaceWith(
        expression = "PlatformLayersComposeScene(" +
            "density, layoutDirection, size, coroutineContext, composeSceneContext, invalidate" +
            ")",
        imports = arrayOf(
            "androidx.compose.ui.scene.PlatformLayersComposeScenePlatformLayersComposeScene"
        )
    ),
    level = DeprecationLevel.WARNING
)
@InternalComposeUiApi
fun SingleLayerComposeScene(
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    size: IntSize? = null,
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    composeSceneContext: ComposeSceneContext = ComposeSceneContext.Empty,
    invalidate: () -> Unit = {},
): ComposeScene = PlatformLayersComposeScene(
    density = density,
    layoutDirection = layoutDirection,
    size = size,
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
)
