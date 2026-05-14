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
         * If this attribute is present in a [VertexLayout], [BONE_WEIGHTS] must also be present.
         */
        @JvmField public val BONE_INDICES: VertexAttribute = VertexAttribute("BONE_INDICES")
        /**
         * The weights of the bones that affect this vertex. Must be a
         * [VertexAttributeType.UBYTE4_NORM]. If this attribute is present in a [VertexLayout],
         * [BONE_INDICES] must also be present.
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
 * This class is used to define the data type and component count for a vertex attribute in a
 * [VertexAttributeDescriptor].
 */
@ExperimentalCustomMeshApi
public class VertexAttributeType private constructor(private val value: Int) {
    public companion object {
        /** A single 32-bit floating point value. */
        @JvmField public val FLOAT: VertexAttributeType = VertexAttributeType(1)
        /** Two 32-bit floating point values. */
        @JvmField public val FLOAT2: VertexAttributeType = VertexAttributeType(2)
        /** Three 32-bit floating point values. */
        @JvmField public val FLOAT3: VertexAttributeType = VertexAttributeType(3)
        /** Four 32-bit floating point values. */
        @JvmField public val FLOAT4: VertexAttributeType = VertexAttributeType(4)
        /** Four unsigned 8-bit integers, normalized to [0, 1]. */
        @JvmField public val UBYTE4_NORM: VertexAttributeType = VertexAttributeType(5)
        /** Four unsigned 8-bit integers. */
        @JvmField public val UBYTE4: VertexAttributeType = VertexAttributeType(6)
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexAttributeType) return false
        return value == other.value
    }

    public override fun hashCode(): Int = value.hashCode()

    public override fun toString(): String =
        when (this) {
            FLOAT -> "FLOAT"
            FLOAT2 -> "FLOAT2"
            FLOAT3 -> "FLOAT3"
            FLOAT4 -> "FLOAT4"
            UBYTE4_NORM -> "UBYTE4_NORM"
            UBYTE4 -> "UBYTE4"
            else -> value.toString()
        }

    internal fun getByteSize(): Int {
        return when (this) {
            FLOAT -> 4
            FLOAT2 -> 8
            FLOAT3 -> 12
            FLOAT4 -> 16
            UBYTE4_NORM -> 4
            UBYTE4 -> 4
            else -> throw IllegalStateException("Unknown VertexAttributeType")
        }
    }
}

/**
 * Descriptor for a single vertex attribute.
 *
 * Defines how a specific vertex attribute is structured within a vertex buffer. If the [offset] is
 * set to [AUTO_OFFSET], the attribute will be placed immediately after the previous attribute in
 * the same buffer, or at the beginning of the vertex data if this is the first attribute. The
 * offset cannot exceed [MAX_OFFSET].
 *
 * @param attribute The [VertexAttribute] semantic of the attribute.
 * @param type The [VertexAttributeType] data type of the attribute.
 * @param offset The byte offset of the attribute from the start of the vertex data.
 * @throws IllegalArgumentException if the given [type] is incompatible with the given [attribute],
 *   or if [offset] is not [AUTO_OFFSET] and is not between 0 and [MAX_OFFSET].
 */
@ExperimentalCustomMeshApi
public class VertexAttributeDescriptor
@JvmOverloads
constructor(
    public val attribute: VertexAttribute,
    public val type: VertexAttributeType,
    @IntRange(from = -1, to = 32767) public val offset: Int = AUTO_OFFSET,
) {
    public companion object {
        /**
         * Indicates that the attribute offset should be computed automatically by placing it
         * immediately after the previous attribute in the same buffer, or at the beginning of the
         * vertex data if this is the first attribute.
         */
        public const val AUTO_OFFSET: Int = -1

        /** The maximum allowed byte offset for a vertex attribute. */
        public const val MAX_OFFSET: Int = 32767
    }

    init {
        require(offset == AUTO_OFFSET || offset in 0..MAX_OFFSET) {
            "offset must be AUTO_OFFSET or between 0 and $MAX_OFFSET."
        }
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
        if (offset != other.offset) return false
        return true
    }

    override fun hashCode(): Int {
        var result = attribute.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + offset
        return result
    }

    override fun toString(): String {
        return "VertexAttributeDescriptor(attribute=$attribute, type=$type, offset=$offset)"
    }
}

/**
 * Layout of a vertex buffer, composed of multiple attribute descriptors.
 *
 * If the stride is set to [AUTO_STRIDE], the stride will be computed automatically to be the
 * minimum byte stride required to encompass all attributes in the buffer. The stride must be
 * [AUTO_STRIDE] or a positive value up to [MAX_STRIDE].
 *
 * @param attributes List of [VertexAttributeDescriptor]s defining the layout within this buffer.
 * @param stride The byte stride of the buffer.
 * @throws IllegalArgumentException if [attributes] is empty, if [stride] is not [AUTO_STRIDE] and
 *   not between 1 and [MAX_STRIDE], if any attributes overlap, or if [stride] is not [AUTO_STRIDE]
 *   and is smaller than the minimum byte stride required to encompass all attributes.
 */
@ExperimentalCustomMeshApi
public class VertexBufferLayout
@JvmOverloads
constructor(
    public val attributes: List<VertexAttributeDescriptor>,
    @IntRange(from = -1, to = 32767) public val stride: Int = AUTO_STRIDE,
) {
    public companion object {
        /**
         * Indicates that the buffer stride should be computed automatically to be the minimum byte
         * stride required to encompass all attributes in the buffer.
         */
        public const val AUTO_STRIDE: Int = -1

        /** The maximum allowed byte stride for a vertex buffer (32767). */
        public const val MAX_STRIDE: Int = 32767

        private class AttributeSpan(val descriptor: VertexAttributeDescriptor, val offset: Int) {
            val size: Int = descriptor.type.getByteSize()
            val end: Int
                get() = offset + size

            fun overlapsWith(other: AttributeSpan): Boolean {
                return this.offset < other.end && other.offset < this.end
            }
        }
    }

    init {
        require(attributes.isNotEmpty()) {
            "VertexBufferLayout must contain at least one attribute."
        }
        require(stride == AUTO_STRIDE || stride in 1..MAX_STRIDE) {
            "stride must be AUTO_STRIDE or between 1 and $MAX_STRIDE."
        }

        var minRequiredStride = 0
        val spans = mutableListOf<AttributeSpan>()
        for (descriptor in attributes) {
            val effectiveOffset =
                if (descriptor.offset == VertexAttributeDescriptor.AUTO_OFFSET) {
                    if (spans.isEmpty()) 0 else spans.last().end
                } else {
                    descriptor.offset
                }
            val newSpan = AttributeSpan(descriptor, effectiveOffset)

            for (existingSpan in spans) {
                require(!newSpan.overlapsWith(existingSpan)) {
                    "Attribute ${descriptor.attribute} overlaps with existing attribute " +
                        "${existingSpan.descriptor.attribute} in the same buffer."
                }
            }
            spans.add(newSpan)
            minRequiredStride = maxOf(minRequiredStride, newSpan.end)
        }

        require(stride == AUTO_STRIDE || stride >= minRequiredStride) {
            "stride ($stride) must be at least the minimum byte stride required to " +
                "encompass all attributes in the buffer ($minRequiredStride)."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexBufferLayout) return false
        if (stride != other.stride) return false
        if (attributes != other.attributes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = stride
        result = 31 * result + attributes.hashCode()
        return result
    }

    override fun toString(): String {
        return "VertexBufferLayout(attributes=$attributes, stride=$stride)"
    }
}

/**
 * Layout of a vertex, composed of multiple buffer layouts.
 *
 * A `VertexLayout` describes the complete structure of vertices in a [MeshBuffer], which may span
 * across multiple vertex buffers.
 *
 * @param buffers List of [VertexBufferLayout]s defining the vertex layout.
 * @throws IllegalArgumentException if [buffers] is empty, if the layout does not contain a
 *   [VertexAttribute.POSITION] attribute, if it contains duplicate attributes, or if only one of
 *   [VertexAttribute.BONE_INDICES] or [VertexAttribute.BONE_WEIGHTS] is present.
 */
@ExperimentalCustomMeshApi
public class VertexLayout private constructor(public val buffers: List<VertexBufferLayout>) {

    /**
     * Builder for [VertexLayout].
     *
     * This builder is capable of building a layout where vertex attributes are spread across
     * multiple vertex buffers. Attributes added with [addAttribute] belong to the current buffer.
     * To start adding attributes to a new buffer, call [startNextBuffer].
     *
     * An [IllegalArgumentException] will be thrown by [addAttribute] if duplicate attributes are
     * added, and by [build] if the layout does not contain a [VertexAttribute.POSITION] attribute,
     * or if only one of [VertexAttribute.BONE_INDICES] or [VertexAttribute.BONE_WEIGHTS] is
     * present.
     *
     * Basic example of creating a layout with a single vertex buffer:
     * ```
     * val layout = VertexLayout.Builder()
     *     .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
     *     .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
     *     .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
     *     .build()
     * ```
     *
     * Example of creating a layout with multiple vertex buffers, explicit offsets, and strides:
     * ```
     * val layout = VertexLayout.Builder()
     *     // First buffer layout (e.g. position and normal interleaved)
     *     .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3, offset = 0)
     *     .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3, offset = 12)
     *     .setStride(24)
     *     .startNextBuffer()
     *     // Second buffer layout (e.g. UV and color interleaved)
     *     .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2, offset = 0)
     *     .addAttribute(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM, offset = 8)
     *     .setStride(12)
     *     .build()
     * ```
     */
    public class Builder {
        private val buffers = mutableListOf<VertexBufferLayout>()
        private var currentAttributes = mutableListOf<VertexAttributeDescriptor>()
        private var currentStride: Int = VertexBufferLayout.AUTO_STRIDE
        private val addedAttributes = mutableSetOf<VertexAttribute>()

        /**
         * Adds a vertex attribute to the current buffer.
         *
         * @param attribute The [VertexAttribute] semantic of the attribute.
         * @param type The [VertexAttributeType] data type of the attribute.
         * @param offset The byte offset of the attribute from the start of the vertex data. If set
         *   to [VertexAttributeDescriptor.AUTO_OFFSET], the attribute will be placed immediately
         *   after the previous attribute in the same buffer, or at the beginning of the vertex data
         *   if this is the first attribute.
         * @throws IllegalArgumentException if the attribute has already been added to the layout.
         */
        @Suppress("MissingGetterMatchingBuilder")
        @JvmOverloads
        public fun addAttribute(
            attribute: VertexAttribute,
            type: VertexAttributeType,
            @IntRange(from = -1, to = 32767) offset: Int = VertexAttributeDescriptor.AUTO_OFFSET,
        ): Builder {
            return addAttribute(VertexAttributeDescriptor(attribute, type, offset))
        }

        /**
         * Adds a vertex attribute to the current buffer.
         *
         * @param descriptor The [VertexAttributeDescriptor] defining the attribute to add.
         * @throws IllegalArgumentException if the attribute has already been added to the layout.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun addAttribute(descriptor: VertexAttributeDescriptor): Builder {
            require(addedAttributes.add(descriptor.attribute)) {
                "VertexLayout cannot contain duplicate attributes: ${descriptor.attribute}"
            }
            currentAttributes.add(descriptor)
            return this
        }

        /**
         * Sets the stride for the current buffer.
         *
         * @param stride The byte stride of the buffer. If set to [VertexBufferLayout.AUTO_STRIDE],
         *   the stride will be computed automatically to be the minimum byte stride required to
         *   encompass all attributes in the buffer. Otherwise, it must be a positive value up to
         *   [VertexBufferLayout.MAX_STRIDE].
         * @throws IllegalArgumentException if [stride] is not [VertexBufferLayout.AUTO_STRIDE] and
         *   not between 1 and [VertexBufferLayout.MAX_STRIDE].
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setStride(@IntRange(from = -1, to = 32767) stride: Int): Builder {
            require(
                stride == VertexBufferLayout.AUTO_STRIDE ||
                    stride in 1..VertexBufferLayout.MAX_STRIDE
            ) {
                "stride must be AUTO_STRIDE or between 1 and ${VertexBufferLayout.MAX_STRIDE}."
            }
            currentStride = stride
            return this
        }

        /**
         * Commits the current buffer and prepares for the next buffer.
         *
         * This method will add the previously set attributes and stride to the layout as a new
         * [VertexBufferLayout], and then reset the buffer layout state. The stride will be reset to
         * [VertexBufferLayout.AUTO_STRIDE]. Subsequent calls to [addAttribute] and [setStride] will
         * apply to a new buffer.
         *
         * @throws IllegalStateException if no attributes were added to the current buffer.
         * @throws IllegalArgumentException if any attributes in the current buffer overlap, or if
         *   an explicitly provided stride is smaller than the minimum byte stride required to
         *   encompass all attributes in the buffer.
         */
        @Suppress("BuilderSetStyle")
        public fun startNextBuffer(): Builder {
            check(currentAttributes.isNotEmpty()) {
                "Cannot call startNextBuffer() with no attributes added to the current buffer."
            }
            buffers.add(VertexBufferLayout(currentAttributes.toList(), currentStride))
            currentAttributes.clear()
            currentStride = VertexBufferLayout.AUTO_STRIDE
            return this
        }

        /**
         * Builds the [VertexLayout].
         *
         * @throws IllegalArgumentException if no attributes were added, if the layout does not
         *   contain a [VertexAttribute.POSITION] attribute, if only one of
         *   [VertexAttribute.BONE_INDICES] or [VertexAttribute.BONE_WEIGHTS] is present, if any
         *   attributes in the current buffer overlap, or if an explicitly provided stride is
         *   smaller than the minimum byte stride required to encompass all attributes.
         */
        public fun build(): VertexLayout {
            if (currentAttributes.isNotEmpty()) {
                startNextBuffer()
            }

            require(buffers.isNotEmpty()) { "VertexLayout must contain at least one buffer." }
            require(addedAttributes.contains(VertexAttribute.POSITION)) {
                "VertexLayout must contain a POSITION attribute."
            }
            val hasBoneIndices = addedAttributes.contains(VertexAttribute.BONE_INDICES)
            val hasBoneWeights = addedAttributes.contains(VertexAttribute.BONE_WEIGHTS)
            require(hasBoneIndices == hasBoneWeights) {
                "VertexLayout must contain both BONE_INDICES and BONE_WEIGHTS, or neither."
            }

            return VertexLayout(buffers.toList())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexLayout) return false
        if (buffers != other.buffers) return false
        return true
    }

    override fun hashCode(): Int {
        return buffers.hashCode()
    }

    override fun toString(): String {
        return "VertexLayout(buffers=$buffers)"
    }
}
