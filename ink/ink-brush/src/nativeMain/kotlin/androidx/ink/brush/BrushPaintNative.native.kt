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

import androidx.ink.nativeloader.cinterop.BrushPaintNative_create
import androidx.ink.nativeloader.cinterop.BrushPaintNative_free
import androidx.ink.nativeloader.cinterop.BrushPaintNative_getColorFunctionCount
import androidx.ink.nativeloader.cinterop.BrushPaintNative_getColorFunctionParametersTypeInt
import androidx.ink.nativeloader.cinterop.BrushPaintNative_getSelfOverlapInt
import androidx.ink.nativeloader.cinterop.BrushPaintNative_getTextureLayerCount
import androidx.ink.nativeloader.cinterop.BrushPaintNative_getTextureLayerMappingInt
import androidx.ink.nativeloader.cinterop.BrushPaintNative_isCompatibleWithMeshFormat
import androidx.ink.nativeloader.cinterop.BrushPaintNative_newCopyOfColorFunction
import androidx.ink.nativeloader.cinterop.BrushPaintNative_newCopyOfTextureLayer
import androidx.ink.nativeloader.cinterop.ColorFunctionNative_computeReplaceColorLong
import androidx.ink.nativeloader.cinterop.ColorFunctionNative_createOpacityMultiplier
import androidx.ink.nativeloader.cinterop.ColorFunctionNative_createReplaceColor
import androidx.ink.nativeloader.cinterop.ColorFunctionNative_free
import androidx.ink.nativeloader.cinterop.ColorFunctionNative_getOpacityMultiplier
import androidx.ink.nativeloader.cinterop.StampingTextureNative_create
import androidx.ink.nativeloader.cinterop.StampingTextureNative_getAnimationColumns
import androidx.ink.nativeloader.cinterop.StampingTextureNative_getAnimationDurationMillis
import androidx.ink.nativeloader.cinterop.StampingTextureNative_getAnimationFrames
import androidx.ink.nativeloader.cinterop.StampingTextureNative_getAnimationRows
import androidx.ink.nativeloader.cinterop.StampingTextureNative_getClientTextureId
import androidx.ink.nativeloader.cinterop.TextureLayerNative_free
import androidx.ink.nativeloader.cinterop.TextureLayerNative_getBlendModeInt
import androidx.ink.nativeloader.cinterop.TextureLayerNative_getMappingInt
import androidx.ink.nativeloader.cinterop.TilingTextureNative_create
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getClientTextureId
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getOffsetX
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getOffsetY
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getOriginInt
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getRotationDegrees
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getSizeUnitInt
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getSizeX
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getSizeY
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getWrapXInt
import androidx.ink.nativeloader.cinterop.TilingTextureNative_getWrapYInt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object BrushPaintNative {
    actual fun create(
        textureLayerNativePointers: LongArray,
        colorFunctionNativePointers: LongArray,
        selfOverlapInt: Int,
    ): Long =
        textureLayerNativePointers.usePinned { pinnedTextureLayers ->
            colorFunctionNativePointers.usePinned { pinnedColorFunctions ->
                BrushPaintNative_create(
                    jni_env_pass_through = null,
                    if (textureLayerNativePointers.isEmpty()) null
                    else pinnedTextureLayers.addressOf(0),
                    textureLayerNativePointers.size,
                    if (colorFunctionNativePointers.isEmpty()) null
                    else pinnedColorFunctions.addressOf(0),
                    colorFunctionNativePointers.size,
                    selfOverlapInt,
                    throwForNonOkStatusCallback,
                )
            }
        }

    actual fun free(nativePointer: Long) = BrushPaintNative_free(nativePointer)

    actual fun getTextureLayerCount(nativePointer: Long): Int =
        BrushPaintNative_getTextureLayerCount(nativePointer)

    actual fun getTextureLayerMappingInt(nativePointer: Long, index: Int): Int =
        BrushPaintNative_getTextureLayerMappingInt(nativePointer, index)

    actual fun newCopyOfTextureLayer(nativePointer: Long, index: Int): Long =
        BrushPaintNative_newCopyOfTextureLayer(nativePointer, index)

    actual fun getColorFunctionCount(nativePointer: Long): Int =
        BrushPaintNative_getColorFunctionCount(nativePointer)

    actual fun getColorFunctionParametersTypeInt(nativePointer: Long, index: Int): Int =
        BrushPaintNative_getColorFunctionParametersTypeInt(nativePointer, index)

    actual fun newCopyOfColorFunction(nativePointer: Long, index: Int): Long =
        BrushPaintNative_newCopyOfColorFunction(nativePointer, index)

    actual fun getSelfOverlapInt(nativePointer: Long): Int =
        BrushPaintNative_getSelfOverlapInt(nativePointer)

    actual fun isCompatibleWithMeshFormat(
        nativePointer: Long,
        meshFormatNativePointer: Long,
    ): Boolean = BrushPaintNative_isCompatibleWithMeshFormat(nativePointer, meshFormatNativePointer)
}

@OptIn(ExperimentalForeignApi::class)
actual internal object TextureLayerNative {
    actual fun free(nativePointer: Long) = TextureLayerNative_free(nativePointer)

    actual fun getMappingInt(nativePointer: Long): Int =
        TextureLayerNative_getMappingInt(nativePointer)

    actual fun getBlendModeInt(nativePointer: Long): Int =
        TextureLayerNative_getBlendModeInt(nativePointer)
}

@OptIn(ExperimentalForeignApi::class)
actual internal object TilingTextureNative {
    actual fun create(
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
    ): Long =
        TilingTextureNative_create(
            jni_env_pass_through = null,
            clientTextureId,
            sizeX,
            sizeY,
            offsetX,
            offsetY,
            rotationDegrees,
            sizeUnit,
            origin,
            wrapX,
            wrapY,
            blendMode,
            throwForNonOkStatusCallback,
        )

    actual fun getClientTextureId(nativePointer: Long): String =
        TilingTextureNative_getClientTextureId(nativePointer)!!.toKString()

    actual fun getSizeX(nativePointer: Long): Float = TilingTextureNative_getSizeX(nativePointer)

    actual fun getSizeY(nativePointer: Long): Float = TilingTextureNative_getSizeY(nativePointer)

    actual fun getOffsetX(nativePointer: Long): Float =
        TilingTextureNative_getOffsetX(nativePointer)

    actual fun getOffsetY(nativePointer: Long): Float =
        TilingTextureNative_getOffsetY(nativePointer)

    actual fun getRotationDegrees(nativePointer: Long): Float =
        TilingTextureNative_getRotationDegrees(nativePointer)

    actual fun getSizeUnitInt(nativePointer: Long): Int =
        TilingTextureNative_getSizeUnitInt(nativePointer)

    actual fun getOriginInt(nativePointer: Long): Int =
        TilingTextureNative_getOriginInt(nativePointer)

    actual fun getWrapXInt(nativePointer: Long): Int =
        TilingTextureNative_getWrapXInt(nativePointer)

    actual fun getWrapYInt(nativePointer: Long): Int =
        TilingTextureNative_getWrapYInt(nativePointer)
}

@OptIn(ExperimentalForeignApi::class)
actual internal object StampingTextureNative {
    actual fun create(
        clientTextureId: String,
        animationFrames: Int,
        animationRows: Int,
        animationColumns: Int,
        animationDurationMillis: Long,
        blendMode: Int,
    ): Long =
        StampingTextureNative_create(
            jni_env_pass_through = null,
            clientTextureId,
            animationFrames,
            animationRows,
            animationColumns,
            animationDurationMillis,
            blendMode,
            throwForNonOkStatusCallback,
        )

    actual fun getClientTextureId(nativePointer: Long): String =
        StampingTextureNative_getClientTextureId(nativePointer)!!.toKString()

    actual fun getAnimationFrames(nativePointer: Long): Int =
        StampingTextureNative_getAnimationFrames(nativePointer)

    actual fun getAnimationRows(nativePointer: Long): Int =
        StampingTextureNative_getAnimationRows(nativePointer)

    actual fun getAnimationColumns(nativePointer: Long): Int =
        StampingTextureNative_getAnimationColumns(nativePointer)

    actual fun getAnimationDurationMillis(nativePointer: Long): Long =
        StampingTextureNative_getAnimationDurationMillis(nativePointer)
}

@OptIn(ExperimentalForeignApi::class)
actual internal object ColorFunctionNative {
    actual fun createOpacityMultiplier(multiplier: Float): Long =
        ColorFunctionNative_createOpacityMultiplier(
            jni_env_pass_through = null,
            multiplier,
            throwForNonOkStatusCallback,
        )

    actual fun createReplaceColor(
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
    ): Long =
        ColorFunctionNative_createReplaceColor(
            jni_env_pass_through = null,
            colorRed,
            colorGreen,
            colorBlue,
            colorAlpha,
            colorSpace,
            throwForNonOkStatusCallback,
        )

    actual fun free(nativePointer: Long) = ColorFunctionNative_free(nativePointer)

    actual fun getOpacityMultiplier(nativePointer: Long): Float =
        ColorFunctionNative_getOpacityMultiplier(nativePointer)

    actual fun computeReplaceColorLong(nativePointer: Long): Long =
        ColorFunctionNative_computeReplaceColorLong(
            jni_env_pass_through = null,
            nativePointer,
            composeColorLongFromComponentsCallback,
        )
}
