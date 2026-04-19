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
package androidx.xr.scenecore

import androidx.annotation.IntRange

/**
 * Defines the attribute of a vertex.
 *
 * This class is used to define the semantic meaning of a vertex attribute in a
 * [VertexAttributeDescriptor]. See [VertexAttributeType] for the supported data types.
 */
@ExperimentalCustomMeshApi
public class VertexAttribute private constructor(private val name: String) {
    public companion object {
        /** The position of the vertex. Must be a [VertexAttributeType.FLOAT3]. */
        @JvmField public val POSITION: VertexAttribute = VertexAttribute("POSITION")
        /**
         * The normal of the vertex. Must be a [VertexAttributeType.FLOAT3] normal, or a
         * [VertexAttributeType.FLOAT4] quaternion representing the entire tangent frame (normal,
         * tangent, and bitangent vectors).
         */
        @JvmField public val NORMAL: VertexAttribute = VertexAttribute("NORMAL")
        /** The color of the vertex. Must be a [VertexAttributeType.UBYTE4_NORM]. */
        @JvmField public val COLOR: VertexAttribute = VertexAttribute("COLOR")
        /** The first set of texture coordinates. Must be a [VertexAttributeType.FLOAT2]. */
        @JvmField public val UV0: VertexAttribute = VertexAttribute("UV0")
        /** The second set of texture coordinates. Must be a [VertexAttributeType.FLOAT2]. */
        @JvmField public val UV1: VertexAttribute = VertexAttribute("UV1")
        /**
         * The indices of the bones that affect this vertex. Must be a [VertexAttributeType.UBYTE4].
         */
        @JvmField public val BONE_INDICES: VertexAttribute = VertexAttribute("BONE_INDICES")
        /**
         * The weights of the bones that affect this vertex. Must be a
         * [VertexAttributeType.UBYTE4_NORM].
         */
        @JvmField public val BONE_WEIGHTS: VertexAttribute = VertexAttribute("BONE_WEIGHTS")
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexAttribute) return false
        return name == other.name
    }

    public override fun hashCode(): Int = name.hashCode()

    public override fun toString(): String = name
}

/**
 * Defines the type of data for a vertex attribute.
 *
 * This specifies the data type and component count for an attribute in the vertex buffer.
 */
@ExperimentalCustomMeshApi
public class VertexAttributeType private constructor(private val name: String) {
    public companion object {
        /** A single 32-bit floating point value. */
        @JvmField public val FLOAT: VertexAttributeType = VertexAttributeType("FLOAT")
        /** Two 32-bit floating point values. */
        @JvmField public val FLOAT2: VertexAttributeType = VertexAttributeType("FLOAT2")
        /** Three 32-bit floating point values. */
        @JvmField public val FLOAT3: VertexAttributeType = VertexAttributeType("FLOAT3")
        /** Four 32-bit floating point values. */
        @JvmField public val FLOAT4: VertexAttributeType = VertexAttributeType("FLOAT4")
        /** Four unsigned 8-bit integers, normalized to [0, 1]. */
        @JvmField public val UBYTE4_NORM: VertexAttributeType = VertexAttributeType("UBYTE4_NORM")
        /** Four unsigned 8-bit integers. */
        @JvmField public val UBYTE4: VertexAttributeType = VertexAttributeType("UBYTE4")
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexAttributeType) return false
        return name == other.name
    }

    public override fun hashCode(): Int = name.hashCode()

    public override fun toString(): String = name
}

/**
 * Descriptor for a single vertex attribute.
 *
 * Defines how a specific vertex attribute is structured within a vertex buffer.
 *
 * @param attribute The [VertexAttribute] semantic being described.
 * @param type The [VertexAttributeType] data type of the attribute.
 * @param bufferIndex The index of the vertex buffer where this attribute is stored.
 * @throws IllegalArgumentException if the given [type] is incompatible with the given [attribute],
 *   or if [bufferIndex] is negative.
 */
@ExperimentalCustomMeshApi
public class VertexAttributeDescriptor(
    public val attribute: VertexAttribute,
    public val type: VertexAttributeType,
    @IntRange(from = 0) public val bufferIndex: Int = 0,
) {
    init {
        require(bufferIndex >= 0) { "bufferIndex must be non-negative." }
        val isValid =
            when (attribute) {
                VertexAttribute.POSITION -> type == VertexAttributeType.FLOAT3
                VertexAttribute.NORMAL ->
                    type == VertexAttributeType.FLOAT3 || type == VertexAttributeType.FLOAT4
                VertexAttribute.COLOR -> type == VertexAttributeType.UBYTE4_NORM
                VertexAttribute.UV0,
                VertexAttribute.UV1 -> type == VertexAttributeType.FLOAT2
                VertexAttribute.BONE_INDICES -> type == VertexAttributeType.UBYTE4
                VertexAttribute.BONE_WEIGHTS -> type == VertexAttributeType.UBYTE4_NORM
                else -> false // Handle unknown attributes safely by throwing
            }
        require(isValid) { "Incompatible type $type for attribute $attribute." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexAttributeDescriptor) return false
        if (attribute != other.attribute) return false
        if (type != other.type) return false
        if (bufferIndex != other.bufferIndex) return false
        return true
    }

    override fun hashCode(): Int {
        var result = attribute.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + bufferIndex
        return result
    }

    override fun toString(): String {
        return "VertexAttributeDescriptor(attribute=$attribute, type=$type, bufferIndex=$bufferIndex)"
    }
}

/**
 * Layout of a vertex, composed of multiple attribute descriptors.
 *
 * A `VertexLayout` describes the complete structure of vertices in a [MeshBuffer], which may span
 * across multiple vertex buffers.
 *
 * @param attributes List of [VertexAttributeDescriptor]s defining the vertex layout.
 * @throws IllegalArgumentException if [attributes] does not contain a [VertexAttribute.POSITION]
 *   attribute, or if it contains duplicate attributes.
 */
@ExperimentalCustomMeshApi
public class VertexLayout(public val attributes: List<VertexAttributeDescriptor>) {
    init {
        require(attributes.any { it.attribute == VertexAttribute.POSITION }) {
            "VertexLayout must contain a POSITION attribute."
        }
        val duplicateAttributes =
            attributes.groupBy { it.attribute }.filter { it.value.size > 1 }.keys.joinToString(", ")
        require(duplicateAttributes.isEmpty()) {
            "VertexLayout cannot contain duplicate attributes: $duplicateAttributes"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexLayout) return false
        if (attributes != other.attributes) return false
        return true
    }

    override fun hashCode(): Int {
        return attributes.hashCode()
    }

    override fun toString(): String {
        return "VertexLayout(attributes=$attributes)"
    }
}
