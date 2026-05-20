/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/** Singleton wrapper around BrushPaint native JNI calls. */
@UsedByNative
actual internal object BrushPaintNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun create(
        textureLayerNativePointers: LongArray,
        colorFunctionNativePointers: LongArray,
        selfOverlapInt: Int,
    ): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getTextureLayerCount(nativePointer: Long): Int

    @UsedByNative
    actual external fun getTextureLayerMappingInt(nativePointer: Long, index: Int): Int

    @UsedByNative actual external fun newCopyOfTextureLayer(nativePointer: Long, index: Int): Long

    @UsedByNative actual external fun getColorFunctionCount(nativePointer: Long): Int

    @UsedByNative
    actual external fun getColorFunctionParametersTypeInt(nativePointer: Long, index: Int): Int

    @UsedByNative actual external fun newCopyOfColorFunction(nativePointer: Long, index: Int): Long

    @UsedByNative actual external fun getSelfOverlapInt(nativePointer: Long): Int

    @UsedByNative
    actual external fun isCompatibleWithMeshFormat(
        nativePointer: Long,
        meshFormatNativePointer: Long,
    ): Boolean
}

@UsedByNative
actual internal object TextureLayerNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getMappingInt(nativePointer: Long): Int

    @UsedByNative actual external fun getBlendModeInt(nativePointer: Long): Int
}

@UsedByNative
actual internal object TilingTextureNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun create(
        clientTextureId: String,
        sizeX: Float,
        sizeY: Float,
        offsetX: Float,
        offsetY: Float,
        rotationDegrees: Float,
        sizeUnit: Int,
        origin: Int,
        wrapX: Int,
        wrapY: Int,
        blendMode: Int,
    ): Long

    @UsedByNative actual external fun getClientTextureId(nativePointer: Long): String

    @UsedByNative actual external fun getSizeX(nativePointer: Long): Float

    @UsedByNative actual external fun getSizeY(nativePointer: Long): Float

    @UsedByNative actual external fun getOffsetX(nativePointer: Long): Float

    @UsedByNative actual external fun getOffsetY(nativePointer: Long): Float

    @UsedByNative actual external fun getRotationDegrees(nativePointer: Long): Float

    @UsedByNative actual external fun getSizeUnitInt(nativePointer: Long): Int

    @UsedByNative actual external fun getOriginInt(nativePointer: Long): Int

    @UsedByNative actual external fun getWrapXInt(nativePointer: Long): Int

    @UsedByNative actual external fun getWrapYInt(nativePointer: Long): Int
}

@UsedByNative
actual internal object StampingTextureNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun create(
        clientTextureId: String,
        animationFrames: Int,
        animationRows: Int,
        animationColumns: Int,
        animationDurationMillis: Long,
        blendMode: Int,
    ): Long

    @UsedByNative actual external fun getClientTextureId(nativePointer: Long): String

    @UsedByNative actual external fun getAnimationFrames(nativePointer: Long): Int

    @UsedByNative actual external fun getAnimationRows(nativePointer: Long): Int

    @UsedByNative actual external fun getAnimationColumns(nativePointer: Long): Int

    @UsedByNative actual external fun getAnimationDurationMillis(nativePointer: Long): Long
}

@UsedByNative
actual internal object ColorFunctionNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun createOpacityMultiplier(multiplier: Float): Long

    @UsedByNative
    actual external fun createReplaceColor(
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
    ): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getOpacityMultiplier(nativePointer: Long): Float

    @UsedByNative actual external fun computeReplaceColorLong(nativePointer: Long): Long
}
