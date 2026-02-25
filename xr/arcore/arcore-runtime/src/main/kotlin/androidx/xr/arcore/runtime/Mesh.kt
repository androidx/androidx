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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Represents a polygon mesh describing a piece of geometry as perceived by the perception system.
 *
 * @property triangleIndices a [ShortBuffer] of triangles' indices in consecutive triplets
 * @property vertices a [FloatBuffer] of 3D vertices in (x, y, z) packing
 * @property normals a [FloatBuffer] of 3D normals in (x, y, z) packing
 * @property textureCoordinates a [FloatBuffer] of UV texture coordinates in (u, v) packing
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Mesh(
    public val triangleIndices: ShortBuffer?,
    public val vertices: FloatBuffer?,
    public val normals: FloatBuffer?,
    public val textureCoordinates: FloatBuffer?,
)
