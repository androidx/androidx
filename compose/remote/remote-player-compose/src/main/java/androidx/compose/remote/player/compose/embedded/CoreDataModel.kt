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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.DrawTextOnCircle

internal data class ClipPathData(val id: Int, val regionOp: Int)

internal data class DrawTextOnPathData(val pathId: Int, val hOffset: Float, val vOffset: Float)

internal data class DrawTextAnchoredData(
    val textId: Int,
    val x: Float,
    val y: Float,
    val panX: Float,
    val panY: Float,
    val flags: Int,
)

internal data class DrawBitmapIntData(
    val imageId: Int,
    val srcLeft: Int,
    val srcTop: Int,
    val srcRight: Int,
    val srcBottom: Int,
    val dstLeft: Int,
    val dstTop: Int,
    val dstRight: Int,
    val dstBottom: Int,
)

internal data class DrawTextOnCircleData(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val startAngle: Float,
    val warpRadiusOffset: Float,
    val alignment: DrawTextOnCircle.Alignment,
    val placement: DrawTextOnCircle.Placement,
)

internal data class DrawBitmapFontTextData(
    val textId: Int,
    val fontId: Int,
    val start: Int,
    val end: Int,
    val x: Float,
    val y: Float,
    val glyphSpacing: Float,
)

internal data class DrawBitmapFontTextOnPathData(
    val textId: Int,
    val fontId: Int,
    val pathId: Int,
    val start: Int,
    val end: Int,
    val yAdj: Float,
    val glyphSpacing: Float,
)

internal data class DrawBitmapTextAnchoredData(
    val textId: Int,
    val fontId: Int,
    val start: Int,
    val end: Int,
    val x: Float,
    val y: Float,
    val panX: Float,
    val panY: Float,
    val glyphSpacing: Float,
)

internal data class DrawToBitmapData(val bitmapId: Int, val mode: Int, val color: Int)

internal data class DrawTweenPathData(
    val path1Id: Int,
    val path2Id: Int,
    val tween: Float,
    val start: Float,
    val stop: Float,
)

internal data class ConditionalOperationsData(
    val varAOut: Float,
    val varBOut: Float,
    val type: Byte,
)

internal data class LoopOperationData(
    val fromOut: Float,
    val untilOut: Float,
    val stepOut: Float,
    val indexVariableId: Int,
)

internal data class FloatFunctionCallData(val function: Any?, val outArgs: FloatArray?)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class CustomData(val config: String?, val configId: Int, val properties: Any?)

internal data class MarqueeModifierOperationData(
    val iterations: Int,
    val animationMode: Int,
    val repeatDelayMillis: Float,
    val initialDelayMillis: Float,
    val spacing: Float,
    val velocity: Float,
)

internal data class GraphicsLayerAttributeValueData(val name: String, val id: Int, val value: Float)

internal data class HostNamedActionOperationData(val textId: Int, val type: Int, val valueId: Int)

internal data class DrawBase2Data(
    val v1: Float,
    val v2: Float,
    val value1: Float,
    val value2: Float,
)

internal data class DrawBase3Data(
    val v1: Float,
    val v2: Float,
    val v3: Float,
    val value1: Float,
    val value2: Float,
    val value3: Float,
)

internal data class DrawBase4Data(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val x1Value: Float,
    val y1Value: Float,
    val x2Value: Float,
    val y2Value: Float,
)

internal data class DrawBase6Data(
    val v1: Float,
    val v2: Float,
    val v3: Float,
    val v4: Float,
    val v5: Float,
    val v6: Float,
    val value1: Float,
    val value2: Float,
    val value3: Float,
    val value4: Float,
    val value5: Float,
    val value6: Float,
)

internal data class DrawBitmapData(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val outputLeft: Float,
    val outputTop: Float,
    val outputRight: Float,
    val outputBottom: Float,
    val id: Int,
    val descriptionId: Int,
)

internal data class DrawBitmapScaledData(
    val imageId: Int,
    val srcLeft: Float,
    val outSrcLeft: Float,
    val srcTop: Float,
    val outSrcTop: Float,
    val srcRight: Float,
    val outSrcRight: Float,
    val srcBottom: Float,
    val outSrcBottom: Float,
    val dstLeft: Float,
    val outDstLeft: Float,
    val dstTop: Float,
    val outDstTop: Float,
    val dstRight: Float,
    val outDstRight: Float,
    val dstBottom: Float,
    val outDstBottom: Float,
    val contentDescId: Int,
    val scaleFactor: Float,
    val outScaleFactor: Float,
    val scaleType: Int,
)

internal data class DrawPathData(val id: Int, val start: Float, val end: Float)

internal data class BitmapDataData(
    val imageId: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val type: Short,
    val encoding: Short,
    val bitmap: ByteArray,
)

internal data class BorderModifierOperationData(
    val useColorId: Boolean,
    val colorId: Int,
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float,
    val borderWidth: Float,
    val roundedCorner: Float,
    val shapeType: Int,
)

internal data class BackgroundModifierOperationData(
    val useColorId: Boolean,
    val colorId: Int,
    val rId: Float,
    val gId: Float,
    val bId: Float,
    val aId: Float,
    val shapeType: Int,
)

internal data class CoreTextData(
    val colorValue: Int,
    val fontSizeValue: Float,
    val type: Int,
    val fontWeightValue: Float,
    val fontStyle: Int,
    val textAlignValue: Int,
    val overflow: Int,
    val maxLines: Int,
    val letterSpacing: Float,
    val lineHeightMultiplier: Float,
    val lineHeightAdd: Float,
    val underline: Boolean,
    val strikethrough: Boolean,
    val fontAxis: IntArray?,
    val fontAxisValues: FloatArray?,
)

internal data class TextLayoutData(
    val colorValue: Int,
    val fontSizeValue: Float,
    val type: Int,
    val fontWeight: Float,
    val textAlignValue: Int,
    val overflow: Int,
    val maxLines: Int,
)
