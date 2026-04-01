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

package androidx.xr.compose.subspace.node

import androidx.xr.compose.subspace.layout.DelegatableSubspaceNode
import androidx.xr.compose.unit.IntVolumeSize

/**
 * A [androidx.xr.compose.subspace.layout.SubspaceModifier.Node] which receives callbacks when the
 * layout it is attached to is remeasured.
 *
 * This is the [androidx.xr.compose.subspace.layout.SubspaceModifier.Node] equivalent of
 * [androidx.xr.compose.subspace.layout.onSizeChanged].
 */
public interface SubspaceMeasuredSizeAwareModifierNode : DelegatableSubspaceNode {
    /**
     * This method is called when the layout content is remeasured. The most common usage is
     * [androidx.xr.compose.subspace.layout.onSizeChanged].
     */
    public fun onRemeasured(size: IntVolumeSize)
}
