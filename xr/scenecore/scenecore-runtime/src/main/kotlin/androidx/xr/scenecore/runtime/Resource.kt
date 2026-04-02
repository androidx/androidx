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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo

/** Interface for a resource. A resource represents a loadable resource. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface Resource {}

/**
 * Interface for an EXR resource. These HDR images can be used for image based lighting and
 * skyboxes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface ExrImageResource : Resource {}

/** Interface for a glTF resource. This can be used for creating glTF entities. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface GltfModelResource : Resource {}

/** Interface for a texture resource. This can be used alongside materials. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface TextureResource : Resource {}

/** Interface for a material resource. This can be used to override materials on meshes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface MaterialResource : Resource {}

/** Interface for a MeshBuffer resource. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MeshBufferResource : Resource {
    /** Specifies the attribute of a vertex. */
    public annotation class VertexAttribute {
        public companion object {
            public const val POSITION: Int = 0
            public const val NORMAL: Int = 1
            public const val COLOR: Int = 2
            public const val UV0: Int = 3
            public const val UV1: Int = 4
            public const val BONE_INDICES: Int = 5
            public const val BONE_WEIGHTS: Int = 6
        }
    }

    /** Specifies the type of data for a vertex attribute. */
    public annotation class VertexAttributeType {
        public companion object {
            public const val FLOAT: Int = 0
            public const val FLOAT2: Int = 1
            public const val FLOAT3: Int = 2
            public const val FLOAT4: Int = 3
            public const val UBYTE4_NORM: Int = 4
            public const val UBYTE4: Int = 5
        }
    }
}

/** Interface for a CustomMesh resource. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CustomMeshResource : Resource {
    /** Specifies the topology of the indices in a MeshSubset. */
    public annotation class Topology {
        public companion object {
            public const val TRIANGLES: Int = 0
            public const val TRIANGLE_STRIP: Int = 1
        }
    }
}
