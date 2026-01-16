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
            TextureFormat.Undefined,
            TextureFormat.R8Unorm,
            TextureFormat.R8Snorm,
            TextureFormat.R8Uint,
            TextureFormat.R8Sint,
            TextureFormat.R16Unorm,
            TextureFormat.R16Snorm,
            TextureFormat.R16Uint,
            TextureFormat.R16Sint,
            TextureFormat.R16Float,
            TextureFormat.RG8Unorm,
            TextureFormat.RG8Snorm,
            TextureFormat.RG8Uint,
            TextureFormat.RG8Sint,
            TextureFormat.R32Float,
            TextureFormat.R32Uint,
            TextureFormat.R32Sint,
            TextureFormat.RG16Unorm,
            TextureFormat.RG16Snorm,
            TextureFormat.RG16Uint,
            TextureFormat.RG16Sint,
            TextureFormat.RG16Float,
            TextureFormat.RGBA8Unorm,
            TextureFormat.RGBA8UnormSrgb,
            TextureFormat.RGBA8Snorm,
            TextureFormat.RGBA8Uint,
            TextureFormat.RGBA8Sint,
            TextureFormat.BGRA8Unorm,
            TextureFormat.BGRA8UnormSrgb,
            TextureFormat.RGB10A2Uint,
            TextureFormat.RGB10A2Unorm,
            TextureFormat.RG11B10Ufloat,
            TextureFormat.RGB9E5Ufloat,
            TextureFormat.RG32Float,
            TextureFormat.RG32Uint,
            TextureFormat.RG32Sint,
            TextureFormat.RGBA16Unorm,
            TextureFormat.RGBA16Snorm,
            TextureFormat.RGBA16Uint,
            TextureFormat.RGBA16Sint,
            TextureFormat.RGBA16Float,
            TextureFormat.RGBA32Float,
            TextureFormat.RGBA32Uint,
            TextureFormat.RGBA32Sint,
            TextureFormat.Stencil8,
            TextureFormat.Depth16Unorm,
            TextureFormat.Depth24Plus,
            TextureFormat.Depth24PlusStencil8,
            TextureFormat.Depth32Float,
            TextureFormat.Depth32FloatStencil8,
            TextureFormat.BC1RGBAUnorm,
            TextureFormat.BC1RGBAUnormSrgb,
            TextureFormat.BC2RGBAUnorm,
            TextureFormat.BC2RGBAUnormSrgb,
            TextureFormat.BC3RGBAUnorm,
            TextureFormat.BC3RGBAUnormSrgb,
            TextureFormat.BC4RUnorm,
            TextureFormat.BC4RSnorm,
            TextureFormat.BC5RGUnorm,
            TextureFormat.BC5RGSnorm,
            TextureFormat.BC6HRGBUfloat,
            TextureFormat.BC6HRGBFloat,
            TextureFormat.BC7RGBAUnorm,
            TextureFormat.BC7RGBAUnormSrgb,
            TextureFormat.ETC2RGB8Unorm,
            TextureFormat.ETC2RGB8UnormSrgb,
            TextureFormat.ETC2RGB8A1Unorm,
            TextureFormat.ETC2RGB8A1UnormSrgb,
            TextureFormat.ETC2RGBA8Unorm,
            TextureFormat.ETC2RGBA8UnormSrgb,
            TextureFormat.EACR11Unorm,
            TextureFormat.EACR11Snorm,
            TextureFormat.EACRG11Unorm,
            TextureFormat.EACRG11Snorm,
            TextureFormat.ASTC4x4Unorm,
            TextureFormat.ASTC4x4UnormSrgb,
            TextureFormat.ASTC5x4Unorm,
            TextureFormat.ASTC5x4UnormSrgb,
            TextureFormat.ASTC5x5Unorm,
            TextureFormat.ASTC5x5UnormSrgb,
            TextureFormat.ASTC6x5Unorm,
            TextureFormat.ASTC6x5UnormSrgb,
            TextureFormat.ASTC6x6Unorm,
            TextureFormat.ASTC6x6UnormSrgb,
            TextureFormat.ASTC8x5Unorm,
            TextureFormat.ASTC8x5UnormSrgb,
            TextureFormat.ASTC8x6Unorm,
            TextureFormat.ASTC8x6UnormSrgb,
            TextureFormat.ASTC8x8Unorm,
            TextureFormat.ASTC8x8UnormSrgb,
            TextureFormat.ASTC10x5Unorm,
            TextureFormat.ASTC10x5UnormSrgb,
            TextureFormat.ASTC10x6Unorm,
            TextureFormat.ASTC10x6UnormSrgb,
            TextureFormat.ASTC10x8Unorm,
            TextureFormat.ASTC10x8UnormSrgb,
            TextureFormat.ASTC10x10Unorm,
            TextureFormat.ASTC10x10UnormSrgb,
            TextureFormat.ASTC12x10Unorm,
            TextureFormat.ASTC12x10UnormSrgb,
            TextureFormat.ASTC12x12Unorm,
            TextureFormat.ASTC12x12UnormSrgb,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines the internal storage format of a texture. */
public annotation class TextureFormat {
    public companion object {

        /** An undefined texture format. */
        public const val Undefined: Int = 0x00000000

        /** 8-bit red unsigned normalized. */
        public const val R8Unorm: Int = 0x00000001

        /** 8-bit red signed normalized. */
        public const val R8Snorm: Int = 0x00000002

        /** 8-bit red unsigned integer. */
        public const val R8Uint: Int = 0x00000003

        /** 8-bit red signed integer. */
        public const val R8Sint: Int = 0x00000004

        /** 16-bit red unsigned normalized. */
        public const val R16Unorm: Int = 0x00000005

        /** 16-bit red signed normalized. */
        public const val R16Snorm: Int = 0x00000006

        /** 16-bit red unsigned integer. */
        public const val R16Uint: Int = 0x00000007

        /** 16-bit red signed integer. */
        public const val R16Sint: Int = 0x00000008

        /** 16-bit red floating point. */
        public const val R16Float: Int = 0x00000009

        /** 8-bit red, 8-bit green unsigned normalized. */
        public const val RG8Unorm: Int = 0x0000000a

        /** 8-bit red, 8-bit green signed normalized. */
        public const val RG8Snorm: Int = 0x0000000b

        /** 8-bit red, 8-bit green unsigned integer. */
        public const val RG8Uint: Int = 0x0000000c

        /** 8-bit red, 8-bit green signed integer. */
        public const val RG8Sint: Int = 0x0000000d

        /** 32-bit red floating point. */
        public const val R32Float: Int = 0x0000000e

        /** 32-bit red unsigned integer. */
        public const val R32Uint: Int = 0x0000000f

        /** 32-bit red signed integer. */
        public const val R32Sint: Int = 0x00000010

        /** 16-bit red, 16-bit green unsigned normalized. */
        public const val RG16Unorm: Int = 0x00000011

        /** 16-bit red, 16-bit green signed normalized. */
        public const val RG16Snorm: Int = 0x00000012

        /** 16-bit red, 16-bit green unsigned integer. */
        public const val RG16Uint: Int = 0x00000013

        /** 16-bit red, 16-bit green signed integer. */
        public const val RG16Sint: Int = 0x00000014

        /** 16-bit red, 16-bit green floating point. */
        public const val RG16Float: Int = 0x00000015

        /** 8-bit RGBA unsigned normalized. */
        public const val RGBA8Unorm: Int = 0x00000016

        /** 8-bit RGBA unsigned normalized sRGB. */
        public const val RGBA8UnormSrgb: Int = 0x00000017

        /** 8-bit RGBA signed normalized. */
        public const val RGBA8Snorm: Int = 0x00000018

        /** 8-bit RGBA unsigned integer. */
        public const val RGBA8Uint: Int = 0x00000019

        /** 8-bit RGBA signed integer. */
        public const val RGBA8Sint: Int = 0x0000001a

        /** 8-bit BGRA unsigned normalized. */
        public const val BGRA8Unorm: Int = 0x0000001b

        /** 8-bit BGRA unsigned normalized sRGB. */
        public const val BGRA8UnormSrgb: Int = 0x0000001c

        /** 10-bit RGB, 2-bit A unsigned integer. */
        public const val RGB10A2Uint: Int = 0x0000001d

        /** 10-bit RGB, 2-bit A unsigned normalized. */
        public const val RGB10A2Unorm: Int = 0x0000001e

        /** 11-bit R, 11-bit G, 10-bit B unsigned floating point. */
        public const val RG11B10Ufloat: Int = 0x0000001f

        /** 9-bit RGB, 5-bit exponent unsigned floating point. */
        public const val RGB9E5Ufloat: Int = 0x00000020

        /** 32-bit red, 32-bit green floating point. */
        public const val RG32Float: Int = 0x00000021

        /** 32-bit red, 32-bit green unsigned integer. */
        public const val RG32Uint: Int = 0x00000022

        /** 32-bit red, 32-bit green signed integer. */
        public const val RG32Sint: Int = 0x00000023

        /** 16-bit RGBA unsigned normalized. */
        public const val RGBA16Unorm: Int = 0x00000024

        /** 16-bit RGBA signed normalized. */
        public const val RGBA16Snorm: Int = 0x00000025

        /** 16-bit RGBA unsigned integer. */
        public const val RGBA16Uint: Int = 0x00000026

        /** 16-bit RGBA signed integer. */
        public const val RGBA16Sint: Int = 0x00000027

        /** 16-bit RGBA floating point. */
        public const val RGBA16Float: Int = 0x00000028

        /** 32-bit RGBA floating point. */
        public const val RGBA32Float: Int = 0x00000029

        /** 32-bit RGBA unsigned integer. */
        public const val RGBA32Uint: Int = 0x0000002a

        /** 32-bit RGBA signed integer. */
        public const val RGBA32Sint: Int = 0x0000002b

        /** 8-bit stencil component. */
        public const val Stencil8: Int = 0x0000002c

        /** 16-bit depth unsigned normalized. */
        public const val Depth16Unorm: Int = 0x0000002d

        /** At least 24-bit depth component. */
        public const val Depth24Plus: Int = 0x0000002e

        /** At least 24-bit depth and 8-bit stencil component. */
        public const val Depth24PlusStencil8: Int = 0x0000002f

        /** 32-bit depth floating point. */
        public const val Depth32Float: Int = 0x00000030

        /** 32-bit depth floating point and 8-bit stencil. */
        public const val Depth32FloatStencil8: Int = 0x00000031

        /** Block Compression 1 (DXT1) for RGBA, unsigned normalized. */
        public const val BC1RGBAUnorm: Int = 0x00000032

        /** Block Compression 1 (DXT1) for RGBA, unsigned normalized sRGB. */
        public const val BC1RGBAUnormSrgb: Int = 0x00000033

        /** Block Compression 2 (DXT3) for RGBA, unsigned normalized. */
        public const val BC2RGBAUnorm: Int = 0x00000034

        /** Block Compression 2 (DXT3) for RGBA, unsigned normalized sRGB. */
        public const val BC2RGBAUnormSrgb: Int = 0x00000035

        /** Block Compression 3 (DXT5) for RGBA, unsigned normalized. */
        public const val BC3RGBAUnorm: Int = 0x00000036

        /** Block Compression 3 (DXT5) for RGBA, unsigned normalized sRGB. */
        public const val BC3RGBAUnormSrgb: Int = 0x00000037

        /** Block Compression 4 (3Dc/ATI1) for single channel R, unsigned normalized. */
        public const val BC4RUnorm: Int = 0x00000038

        /** Block Compression 4 (3Dc/ATI1) for single channel R, signed normalized. */
        public const val BC4RSnorm: Int = 0x00000039

        /** Block Compression 5 (3Dc/ATI2) for two channels RG, unsigned normalized. */
        public const val BC5RGUnorm: Int = 0x0000003a

        /** Block Compression 5 (3Dc/ATI2) for two channels RG, signed normalized. */
        public const val BC5RGSnorm: Int = 0x0000003b

        /** Block Compression 6H for RGB, unsigned floating point. */
        public const val BC6HRGBUfloat: Int = 0x0000003c

        /** Block Compression 6H for RGB, half-precision floating point (signed). */
        public const val BC6HRGBFloat: Int = 0x0000003d

        /** Block Compression 7 for RGBA, unsigned normalized. */
        public const val BC7RGBAUnorm: Int = 0x0000003e

        /** Block Compression 7 for RGBA, unsigned normalized sRGB. */
        public const val BC7RGBAUnormSrgb: Int = 0x0000003f

        /** ETC2 RGB8 unsigned normalized. */
        public const val ETC2RGB8Unorm: Int = 0x00000040

        /** ETC2 RGB8 unsigned normalized sRGB. */
        public const val ETC2RGB8UnormSrgb: Int = 0x00000041

        /** ETC2 RGB8 plus 1-bit alpha unsigned normalized. */
        public const val ETC2RGB8A1Unorm: Int = 0x00000042

        /** ETC2 RGB8 plus 1-bit alpha unsigned normalized sRGB. */
        public const val ETC2RGB8A1UnormSrgb: Int = 0x00000043

        /** ETC2 RGBA8 unsigned normalized. */
        public const val ETC2RGBA8Unorm: Int = 0x00000044

        /** ETC2 RGBA8 unsigned normalized sRGB. */
        public const val ETC2RGBA8UnormSrgb: Int = 0x00000045

        /** EAC single channel R 11-bit unsigned normalized. */
        public const val EACR11Unorm: Int = 0x00000046

        /** EAC single channel R 11-bit signed normalized. */
        public const val EACR11Snorm: Int = 0x00000047

        /** EAC dual channel RG 11-bit unsigned normalized. */
        public const val EACRG11Unorm: Int = 0x00000048

        /** EAC dual channel RG 11-bit signed normalized. */
        public const val EACRG11Snorm: Int = 0x00000049

        /** ASTC 4x4 block size unsigned normalized. */
        public const val ASTC4x4Unorm: Int = 0x0000004a

        /** ASTC 4x4 block size unsigned normalized sRGB. */
        public const val ASTC4x4UnormSrgb: Int = 0x0000004b

        /** ASTC 5x4 block size unsigned normalized. */
        public const val ASTC5x4Unorm: Int = 0x0000004c

        /** ASTC 5x4 block size unsigned normalized sRGB. */
        public const val ASTC5x4UnormSrgb: Int = 0x0000004d

        /** ASTC 5x5 block size unsigned normalized. */
        public const val ASTC5x5Unorm: Int = 0x0000004e

        /** ASTC 5x5 block size unsigned normalized sRGB. */
        public const val ASTC5x5UnormSrgb: Int = 0x0000004f

        /** ASTC 6x5 block size unsigned normalized. */
        public const val ASTC6x5Unorm: Int = 0x00000050

        /** ASTC 6x5 block size unsigned normalized sRGB. */
        public const val ASTC6x5UnormSrgb: Int = 0x00000051

        /** ASTC 6x6 block size unsigned normalized. */
        public const val ASTC6x6Unorm: Int = 0x00000052

        /** ASTC 6x6 block size unsigned normalized sRGB. */
        public const val ASTC6x6UnormSrgb: Int = 0x00000053

        /** ASTC 8x5 block size unsigned normalized. */
        public const val ASTC8x5Unorm: Int = 0x00000054

        /** ASTC 8x5 block size unsigned normalized sRGB. */
        public const val ASTC8x5UnormSrgb: Int = 0x00000055

        /** ASTC 8x6 block size unsigned normalized. */
        public const val ASTC8x6Unorm: Int = 0x00000056

        /** ASTC 8x6 block size unsigned normalized sRGB. */
        public const val ASTC8x6UnormSrgb: Int = 0x00000057

        /** ASTC 8x8 block size unsigned normalized. */
        public const val ASTC8x8Unorm: Int = 0x00000058

        /** ASTC 8x8 block size unsigned normalized sRGB. */
        public const val ASTC8x8UnormSrgb: Int = 0x00000059

        /** ASTC 10x5 block size unsigned normalized. */
        public const val ASTC10x5Unorm: Int = 0x0000005a

        /** ASTC 10x5 block size unsigned normalized sRGB. */
        public const val ASTC10x5UnormSrgb: Int = 0x0000005b

        /** ASTC 10x6 block size unsigned normalized. */
        public const val ASTC10x6Unorm: Int = 0x0000005c

        /** ASTC 10x6 block size unsigned normalized sRGB. */
        public const val ASTC10x6UnormSrgb: Int = 0x0000005d

        /** ASTC 10x8 block size unsigned normalized. */
        public const val ASTC10x8Unorm: Int = 0x0000005e

        /** ASTC 10x8 block size unsigned normalized sRGB. */
        public const val ASTC10x8UnormSrgb: Int = 0x0000005f

        /** ASTC 10x10 block size unsigned normalized. */
        public const val ASTC10x10Unorm: Int = 0x00000060

        /** ASTC 10x10 block size unsigned normalized sRGB. */
        public const val ASTC10x10UnormSrgb: Int = 0x00000061

        /** ASTC 12x10 block size unsigned normalized. */
        public const val ASTC12x10Unorm: Int = 0x00000062

        /** ASTC 12x10 block size unsigned normalized sRGB. */
        public const val ASTC12x10UnormSrgb: Int = 0x00000063

        /** ASTC 12x12 block size unsigned normalized. */
        public const val ASTC12x12Unorm: Int = 0x00000064

        /** ASTC 12x12 block size unsigned normalized sRGB. */
        public const val ASTC12x12UnormSrgb: Int = 0x00000065
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "R8Unorm",
                0x00000002 to "R8Snorm",
                0x00000003 to "R8Uint",
                0x00000004 to "R8Sint",
                0x00000005 to "R16Unorm",
                0x00000006 to "R16Snorm",
                0x00000007 to "R16Uint",
                0x00000008 to "R16Sint",
                0x00000009 to "R16Float",
                0x0000000a to "RG8Unorm",
                0x0000000b to "RG8Snorm",
                0x0000000c to "RG8Uint",
                0x0000000d to "RG8Sint",
                0x0000000e to "R32Float",
                0x0000000f to "R32Uint",
                0x00000010 to "R32Sint",
                0x00000011 to "RG16Unorm",
                0x00000012 to "RG16Snorm",
                0x00000013 to "RG16Uint",
                0x00000014 to "RG16Sint",
                0x00000015 to "RG16Float",
                0x00000016 to "RGBA8Unorm",
                0x00000017 to "RGBA8UnormSrgb",
                0x00000018 to "RGBA8Snorm",
                0x00000019 to "RGBA8Uint",
                0x0000001a to "RGBA8Sint",
                0x0000001b to "BGRA8Unorm",
                0x0000001c to "BGRA8UnormSrgb",
                0x0000001d to "RGB10A2Uint",
                0x0000001e to "RGB10A2Unorm",
                0x0000001f to "RG11B10Ufloat",
                0x00000020 to "RGB9E5Ufloat",
                0x00000021 to "RG32Float",
                0x00000022 to "RG32Uint",
                0x00000023 to "RG32Sint",
                0x00000024 to "RGBA16Unorm",
                0x00000025 to "RGBA16Snorm",
                0x00000026 to "RGBA16Uint",
                0x00000027 to "RGBA16Sint",
                0x00000028 to "RGBA16Float",
                0x00000029 to "RGBA32Float",
                0x0000002a to "RGBA32Uint",
                0x0000002b to "RGBA32Sint",
                0x0000002c to "Stencil8",
                0x0000002d to "Depth16Unorm",
                0x0000002e to "Depth24Plus",
                0x0000002f to "Depth24PlusStencil8",
                0x00000030 to "Depth32Float",
                0x00000031 to "Depth32FloatStencil8",
                0x00000032 to "BC1RGBAUnorm",
                0x00000033 to "BC1RGBAUnormSrgb",
                0x00000034 to "BC2RGBAUnorm",
                0x00000035 to "BC2RGBAUnormSrgb",
                0x00000036 to "BC3RGBAUnorm",
                0x00000037 to "BC3RGBAUnormSrgb",
                0x00000038 to "BC4RUnorm",
                0x00000039 to "BC4RSnorm",
                0x0000003a to "BC5RGUnorm",
                0x0000003b to "BC5RGSnorm",
                0x0000003c to "BC6HRGBUfloat",
                0x0000003d to "BC6HRGBFloat",
                0x0000003e to "BC7RGBAUnorm",
                0x0000003f to "BC7RGBAUnormSrgb",
                0x00000040 to "ETC2RGB8Unorm",
                0x00000041 to "ETC2RGB8UnormSrgb",
                0x00000042 to "ETC2RGB8A1Unorm",
                0x00000043 to "ETC2RGB8A1UnormSrgb",
                0x00000044 to "ETC2RGBA8Unorm",
                0x00000045 to "ETC2RGBA8UnormSrgb",
                0x00000046 to "EACR11Unorm",
                0x00000047 to "EACR11Snorm",
                0x00000048 to "EACRG11Unorm",
                0x00000049 to "EACRG11Snorm",
                0x0000004a to "ASTC4x4Unorm",
                0x0000004b to "ASTC4x4UnormSrgb",
                0x0000004c to "ASTC5x4Unorm",
                0x0000004d to "ASTC5x4UnormSrgb",
                0x0000004e to "ASTC5x5Unorm",
                0x0000004f to "ASTC5x5UnormSrgb",
                0x00000050 to "ASTC6x5Unorm",
                0x00000051 to "ASTC6x5UnormSrgb",
                0x00000052 to "ASTC6x6Unorm",
                0x00000053 to "ASTC6x6UnormSrgb",
                0x00000054 to "ASTC8x5Unorm",
                0x00000055 to "ASTC8x5UnormSrgb",
                0x00000056 to "ASTC8x6Unorm",
                0x00000057 to "ASTC8x6UnormSrgb",
                0x00000058 to "ASTC8x8Unorm",
                0x00000059 to "ASTC8x8UnormSrgb",
                0x0000005a to "ASTC10x5Unorm",
                0x0000005b to "ASTC10x5UnormSrgb",
                0x0000005c to "ASTC10x6Unorm",
                0x0000005d to "ASTC10x6UnormSrgb",
                0x0000005e to "ASTC10x8Unorm",
                0x0000005f to "ASTC10x8UnormSrgb",
                0x00000060 to "ASTC10x10Unorm",
                0x00000061 to "ASTC10x10UnormSrgb",
                0x00000062 to "ASTC12x10Unorm",
                0x00000063 to "ASTC12x10UnormSrgb",
                0x00000064 to "ASTC12x12Unorm",
                0x00000065 to "ASTC12x12UnormSrgb",
            )

        public fun toString(@TextureFormat value: Int): String = names[value] ?: value.toString()
    }
}
