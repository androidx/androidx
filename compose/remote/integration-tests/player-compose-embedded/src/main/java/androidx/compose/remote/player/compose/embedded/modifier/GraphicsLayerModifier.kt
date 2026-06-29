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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.getValuesReflection
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun Modifier.graphicsLayer(op: GraphicsLayerModifierOperation): Modifier {
    val document = LocalCoreDocument.current
    val values = op.getValuesReflection()
    val scaleX =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.SCALE_X].value).value
    val scaleY =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.SCALE_Y].value).value
    val alpha = rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.ALPHA].value).value
    val translationX =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.TRANSLATION_X].value).value
    val translationY =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.TRANSLATION_Y].value).value
    val shadowElevation =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.SHADOW_ELEVATION].value)
            .value
    val rotationX =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.ROTATION_X].value).value
    val rotationY =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.ROTATION_Y].value).value
    val rotationZ =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.ROTATION_Z].value).value
    val cameraDistance =
        rememberRemoteFloatAsState(values[GraphicsLayerModifierOperation.CAMERA_DISTANCE].value)
            .value

    return this.graphicsLayer {
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.alpha = alpha
        this.translationX = translationX
        this.translationY = translationY
        this.shadowElevation = shadowElevation
        this.rotationX = rotationX
        this.rotationY = rotationY
        this.rotationZ = rotationZ
        this.cameraDistance = cameraDistance
    }
}
