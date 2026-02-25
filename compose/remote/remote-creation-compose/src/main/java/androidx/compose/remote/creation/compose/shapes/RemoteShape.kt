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

package androidx.compose.remote.creation.compose.shapes

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection

/** Defines a generic remote shape. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteShape {
    /**
     * Creates [RemoteOutline] of this shape for the given [size].
     *
     * @param size the size of the shape boundary.
     * @param layoutDirection the current layout direction.
     * @return [Outline] of this shape for the given [size].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createOutline(
        size: RemoteSize,
        density: RemoteDensity,
        layoutDirection: LayoutDirection,
    ): RemoteOutline
}
