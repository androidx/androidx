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

package androidx.compose.ui

import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.InspectorInfo

/**
 * A modifier that keeps the device screen on as long as it is part of the composition on supported
 * platforms.
 *
 * This is useful for scenarios where the user might not be interacting with the screen frequently
 * but the content needs to remain visible, such as during video playback.
 */
fun Modifier.keepScreenOn(): Modifier = this then KeepScreenOnElement

private data object KeepScreenOnElement : ModifierNodeElement<KeepScreenOnNode>() {
    override fun create(): KeepScreenOnNode = KeepScreenOnNode()

    override fun update(node: KeepScreenOnNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "keepScreenOn"
    }
}

private class KeepScreenOnNode : Modifier.Node() {
    override fun onAttach() {
        requireOwner().incrementKeepScreenOnCount()
    }

    override fun onDetach() {
        requireOwner().decrementKeepScreenOnCount()
    }
}
