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

package androidx.webgpu

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@IntDef(
    value =
        [
            VertexFormat.Uint8,
            VertexFormat.Uint8x2,
            VertexFormat.Uint8x4,
            VertexFormat.Sint8,
            VertexFormat.Sint8x2,
            VertexFormat.Sint8x4,
            VertexFormat.Unorm8,
            VertexFormat.Unorm8x2,
            VertexFormat.Unorm8x4,
            VertexFormat.Snorm8,
            VertexFormat.Snorm8x2,
            VertexFormat.Snorm8x4,
            VertexFormat.Uint16,
            VertexFormat.Uint16x2,
            VertexFormat.Uint16x4,
            VertexFormat.Sint16,
            VertexFormat.Sint16x2,
            VertexFormat.Sint16x4,
            VertexFormat.Unorm16,
            VertexFormat.Unorm16x2,
            VertexFormat.Unorm16x4,
            VertexFormat.Snorm16,
            VertexFormat.Snorm16x2,
            VertexFormat.Snorm16x4,
            VertexFormat.Float16,
            VertexFormat.Float16x2,
            VertexFormat.Float16x4,
            VertexFormat.Float32,
            VertexFormat.Float32x2,
            VertexFormat.Float32x3,
            VertexFormat.Float32x4,
            VertexFormat.Uint32,
            VertexFormat.Uint32x2,
            VertexFormat.Uint32x3,
            VertexFormat.Uint32x4,
            VertexFormat.Sint32,
            VertexFormat.Sint32x2,
            VertexFormat.Sint32x3,
            VertexFormat.Sint32x4,
            VertexFormat.Unorm10_10_10_2,
            VertexFormat.Unorm8x4BGRA,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines the format and number of components for a single vertex attribute. */
public annotation class VertexFormat {
    public companion object {

        /** Unsigned 8-bit integer. */
        public const val Uint8: Int = 0x00000001

        /** Two unsigned 8-bit integer components. */
        public const val Uint8x2: Int = 0x00000002

        /** Four unsigned 8-bit integer components. */
        public const val Uint8x4: Int = 0x00000003

        /** Signed 8-bit integer. */
        public const val Sint8: Int = 0x00000004

        /** Two signed 8-bit integer components. */
        public const val Sint8x2: Int = 0x00000005

        /** Four signed 8-bit integer components. */
        public const val Sint8x4: Int = 0x00000006

        /** Unsigned 8-bit normalized value. */
        public const val Unorm8: Int = 0x00000007

        /** Two unsigned 8-bit normalized components. */
        public const val Unorm8x2: Int = 0x00000008

        /** Four unsigned 8-bit normalized components. */
        public const val Unorm8x4: Int = 0x00000009

        /** Signed 8-bit normalized value. */
        public const val Snorm8: Int = 0x0000000a

        /** Two signed 8-bit normalized components. */
        public const val Snorm8x2: Int = 0x0000000b

        /** Four signed 8-bit normalized components. */
        public const val Snorm8x4: Int = 0x0000000c

        /** Unsigned 16-bit integer. */
        public const val Uint16: Int = 0x0000000d

        /** Two unsigned 16-bit integer components. */
        public const val Uint16x2: Int = 0x0000000e

        /** Four unsigned 16-bit integer components. */
        public const val Uint16x4: Int = 0x0000000f

        /** Signed 16-bit integer. */
        public const val Sint16: Int = 0x00000010

        /** Two signed 16-bit integer components. */
        public const val Sint16x2: Int = 0x00000011

        /** Four signed 16-bit integer components. */
        public const val Sint16x4: Int = 0x00000012

        /** Unsigned 16-bit normalized value. */
        public const val Unorm16: Int = 0x00000013

        /** Two unsigned 16-bit normalized components. */
        public const val Unorm16x2: Int = 0x00000014

        /** Four unsigned 16-bit normalized components. */
        public const val Unorm16x4: Int = 0x00000015

        /** Signed 16-bit normalized value. */
        public const val Snorm16: Int = 0x00000016

        /** Two signed 16-bit normalized components. */
        public const val Snorm16x2: Int = 0x00000017

        /** Four signed 16-bit normalized components. */
        public const val Snorm16x4: Int = 0x00000018

        /** Half-precision 16-bit floating point. */
        public const val Float16: Int = 0x00000019

        /** Two half-precision 16-bit floating point components. */
        public const val Float16x2: Int = 0x0000001a

        /** Four half-precision 16-bit floating point components. */
        public const val Float16x4: Int = 0x0000001b

        /** Single-precision 32-bit floating point. */
        public const val Float32: Int = 0x0000001c

        /** Two single-precision 32-bit floating point components. */
        public const val Float32x2: Int = 0x0000001d

        /** Three single-precision 32-bit floating point components. */
        public const val Float32x3: Int = 0x0000001e

        /** Four single-precision 32-bit floating point components. */
        public const val Float32x4: Int = 0x0000001f

        /** Unsigned 32-bit integer. */
        public const val Uint32: Int = 0x00000020

        /** Two unsigned 32-bit integer components. */
        public const val Uint32x2: Int = 0x00000021

        /** Three unsigned 32-bit integer components. */
        public const val Uint32x3: Int = 0x00000022

        /** Four unsigned 32-bit integer components. */
        public const val Uint32x4: Int = 0x00000023

        /** Signed 32-bit integer. */
        public const val Sint32: Int = 0x00000024

        /** Two signed 32-bit integer components. */
        public const val Sint32x2: Int = 0x00000025

        /** Three signed 32-bit integer components. */
        public const val Sint32x3: Int = 0x00000026

        /** Four signed 32-bit integer components. */
        public const val Sint32x4: Int = 0x00000027
        public const val Unorm10_10_10_2: Int = 0x00000028
        public const val Unorm8x4BGRA: Int = 0x00000029
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "Uint8",
                0x00000002 to "Uint8x2",
                0x00000003 to "Uint8x4",
                0x00000004 to "Sint8",
                0x00000005 to "Sint8x2",
                0x00000006 to "Sint8x4",
                0x00000007 to "Unorm8",
                0x00000008 to "Unorm8x2",
                0x00000009 to "Unorm8x4",
                0x0000000a to "Snorm8",
                0x0000000b to "Snorm8x2",
                0x0000000c to "Snorm8x4",
                0x0000000d to "Uint16",
                0x0000000e to "Uint16x2",
                0x0000000f to "Uint16x4",
                0x00000010 to "Sint16",
                0x00000011 to "Sint16x2",
                0x00000012 to "Sint16x4",
                0x00000013 to "Unorm16",
                0x00000014 to "Unorm16x2",
                0x00000015 to "Unorm16x4",
                0x00000016 to "Snorm16",
                0x00000017 to "Snorm16x2",
                0x00000018 to "Snorm16x4",
                0x00000019 to "Float16",
                0x0000001a to "Float16x2",
                0x0000001b to "Float16x4",
                0x0000001c to "Float32",
                0x0000001d to "Float32x2",
                0x0000001e to "Float32x3",
                0x0000001f to "Float32x4",
                0x00000020 to "Uint32",
                0x00000021 to "Uint32x2",
                0x00000022 to "Uint32x3",
                0x00000023 to "Uint32x4",
                0x00000024 to "Sint32",
                0x00000025 to "Sint32x2",
                0x00000026 to "Sint32x3",
                0x00000027 to "Sint32x4",
                0x00000028 to "Unorm10_10_10_2",
                0x00000029 to "Unorm8x4BGRA",
            )

        public fun toString(@VertexFormat value: Int): String = names[value] ?: value.toString()
    }
}
