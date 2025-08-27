/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.frontend.util

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ClickArea
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapScaled
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawContent
import androidx.compose.remote.core.operations.DrawLine
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawText
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FontData
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.PathAppend
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextLength
import androidx.compose.remote.core.operations.TextLookup
import androidx.compose.remote.core.operations.TextLookupInt
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.TextMerge
import androidx.compose.remote.core.operations.TextSubtext

/** Utility for getting summary information about a [CoreDocument]. */
class DocumentStats {
    var numBitmaps: Int = 0
        private set

    var uncompressedBitmapBytes: Int = 0
        private set

    var numBitmapFonts: Int = 0
        private set

    var numFloatExpressions: Int = 0
        private set

    var totalFloatExpressionSize: Int = 0
        private set

    var numIntegerExpressions: Int = 0
        private set

    var totalIntegerExpressionSize: Int = 0
        private set

    var numClickAreas: Int = 0
        private set

    var numClipPaths: Int = 0
        private set

    var numClipRects: Int = 0
        private set

    var numColorConstants: Int = 0
        private set

    var numColorExpressions: Int = 0
        private set

    var numConditionalOperations: Int = 0
        private set

    var numDrawOperations: Int = 0
        private set

    var numMatrixOperations: Int = 0
        private set

    var numStringExpressions: Int = 0
        private set

    var numStringConstants: Int = 0
        private set

    var totalConstantStringLength: Int = 0
        private set

    var numFonts: Int = 0
        private set

    var numPaintObjects: Int = 0
        private set

    var numPaths: Int = 0
        private set

    var numPathOperations: Int = 0
        private set

    var numShaders: Int = 0
        private set

    override fun toString(): String =
        "numBitmaps: $numBitmaps, " +
            "uncompressedBitmapBytes: $uncompressedBitmapBytes, " +
            "numBitmapFonts: $numBitmapFonts, " +
            "numFloatExpressions: $numFloatExpressions, " +
            "totalFloatExpressionSize: $totalFloatExpressionSize, " +
            "numIntegerExpressions: $numIntegerExpressions, " +
            "totalIntegerExpressionSize: $totalIntegerExpressionSize, " +
            "numClickAreas: $numClickAreas, " +
            "numClipPaths: $numClipPaths, " +
            "numClipRects: $numClipRects, " +
            "numColorConstants: $numColorConstants, " +
            "numColorExpressions: $numColorExpressions, " +
            "numConditionalOperations: $numConditionalOperations, " +
            "numDrawOperations: $numDrawOperations, " +
            "numMatrixOperations: $numMatrixOperations, " +
            "numStringExpressions: $numStringExpressions, " +
            "numStringConstants: $numStringConstants, " +
            "totalConstantStringLength: $totalConstantStringLength, " +
            "numFonts: $numFonts, " +
            "numPaintObjects: $numPaintObjects, " +
            "numPaths: $numPaths, " +
            "numPathOperations: $numPathOperations, " +
            "numShaders: $numShaders"

    private fun process(bitmapData: BitmapData) {
        numBitmaps++
        val bytesPerPixel =
            when (bitmapData.type.toShort()) {
                BitmapData.TYPE_PNG_ALPHA_8 -> 1
                BitmapData.TYPE_RAW8 -> 1
                else -> 4
            }
        uncompressedBitmapBytes += bitmapData.width * bitmapData.height * bytesPerPixel
    }

    private fun process(floatExpression: FloatExpression) {
        numFloatExpressions++
        totalFloatExpressionSize += floatExpression.mSrcValue.size
    }

    private fun process(integerExpression: IntegerExpression) {
        numIntegerExpressions++
        totalIntegerExpressionSize += integerExpression.mSrcValue.size
    }

    private fun process(conditionalOperations: ConditionalOperations) {
        numConditionalOperations++
        for (op in conditionalOperations.mList) {
            process(op)
        }
    }

    private fun process(textData: TextData) {
        numStringConstants++
        totalConstantStringLength += textData.mText.length
    }

    private fun process(operation: Operation) {
        when (operation) {
            is BitmapData -> process(operation)
            is BitmapFontData -> numBitmapFonts++
            is ClickArea -> numClickAreas++
            is ClipPath -> numClipPaths++
            is ClipRect -> numClipRects++
            is ColorConstant -> numColorConstants++
            is ColorExpression -> numColorExpressions++
            is ConditionalOperations -> process(operation)
            is DrawArc -> numDrawOperations++
            is DrawBitmap -> numDrawOperations++
            is DrawBitmapFontText -> numDrawOperations++
            is DrawBitmapInt -> numDrawOperations++
            is DrawBitmapScaled -> numDrawOperations++
            is DrawBitmapTextAnchored -> numDrawOperations++
            is DrawCircle -> numDrawOperations++
            is DrawContent -> numDrawOperations++
            is DrawLine -> numDrawOperations++
            is DrawOval -> numDrawOperations++
            is DrawPath -> numDrawOperations++
            is DrawRect -> numDrawOperations++
            is DrawRoundRect -> numDrawOperations++
            is DrawSector -> numDrawOperations++
            is DrawText -> numDrawOperations++
            is DrawTextAnchored -> numDrawOperations++
            is DrawTextOnPath -> numDrawOperations++
            is DrawTweenPath -> numDrawOperations++
            is FloatExpression -> process(operation)
            is FontData -> numFonts++
            is IntegerExpression -> process(operation)
            is MatrixFromPath -> numMatrixOperations++
            is MatrixRestore -> numMatrixOperations++
            is MatrixSave -> numMatrixOperations++
            is MatrixScale -> numMatrixOperations++
            is MatrixSkew -> numMatrixOperations++
            is MatrixTranslate -> numMatrixOperations++
            is PathAppend -> numPathOperations++
            is PathCombine -> numPathOperations++
            is PathCreate -> numPathOperations++
            is PathData -> numPaths++
            is PaintData -> numPaintObjects++
            is PathTween -> numPathOperations++
            is ShaderData -> numShaders++
            is TextData -> process(operation)
            is TextFromFloat -> numStringExpressions++
            is TextLength -> numStringExpressions++
            is TextLookup -> numStringExpressions++
            is TextLookupInt -> numStringExpressions++
            is TextMeasure -> numStringExpressions++
            is TextMerge -> numStringExpressions++
            is TextSubtext -> numStringExpressions++
        }
    }

    companion object {
        /** Examines the [document] and produces [DocumentStats]. */
        fun examineDocument(document: CoreDocument) =
            DocumentStats().apply {
                for (op in document.operations) {
                    process(op)
                }
            }
    }
}
