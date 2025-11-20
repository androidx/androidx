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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.collection.MutableIntObjectMap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.ClickArea
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DataDynamicListFloat
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.DataListIds
import androidx.compose.remote.core.operations.DataMapIds
import androidx.compose.remote.core.operations.DataMapLookup
import androidx.compose.remote.core.operations.DebugMessage
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
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
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnCircle.Placement
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.FontData
import androidx.compose.remote.core.operations.HapticFeedback
import androidx.compose.remote.core.operations.IdLookup
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathAppend
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathExpression
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.Rem
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.RootContentDescription
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextLength
import androidx.compose.remote.core.operations.TextLookup
import androidx.compose.remote.core.operations.TextLookupInt
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.TextMerge
import androidx.compose.remote.core.operations.TextSubtext
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.UpdateDynamicFloatList
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.WakeIn
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.ComponentStart
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.ImpulseProcess
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.LayoutComputeOperation
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RunActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.core.types.BooleanConstant
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.core.types.LongConstant
import java.lang.UnsupportedOperationException
import java.nio.charset.StandardCharsets

/**
 * Override of [RemoteComposeBuffer] that supports global optimizations. As the document is written,
 * RecordingRemoteComposeBuffer keeps track of which conditional container each operation is written
 * in (if any) and the operations it depends on, if any. After all operations have been written,
 * [writeToBuffer] must be called and operations are reordered such that they're in the tightest
 * scope that allows their side effects to be visible everywhere they're referenced.
 *
 * This class makes several assumptions:
 * 1) Operations are always written after any dependencies
 * 2) All operations are needed and written lazily as used, I.e. we don't need to hoist operations
 *    into a tighter scope. NB if we ever wanted to drop this assumption, we can make idealSpan
 *    nullable, allowing us to move operations to a tighter scope and elide unused operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecordingRemoteComposeBuffer
@JvmOverloads
constructor(apiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL) : RemoteComposeBuffer(apiLevel) {
    private var spanTreeRoot = Span(null, null, 0, 0)
    private var insertPoint = spanTreeRoot
    private val operationMap = MutableIntObjectMap<SpanOp>()
    private val tinyBuffer = WireBuffer(5 * 1024)
    private val dependencyExtractingRemoteContext = DependencyExtractingRemoteContext()

    private class SpanOp(val op: Operation, var idealSpan: Span) {
        val deps = ArrayList<SpanOp>()

        /**
         * Records that this [SpanOp] is used by [span], and it updates the [idealSpan] to the
         * common ancestor span. This is done recursively for any dependencies.
         */
        fun recordUsageBySpan(span: Span) {
            // Find the common ancestor of idealSpan & span
            val newIdealSpan = findCommonAncestor(idealSpan, span)

            if (idealSpan != newIdealSpan) {
                idealSpan = newIdealSpan
                // Ensure all dependants are also in idealSpan too
                for (i in 0 until deps.size) {
                    deps[i].recordUsageBySpan(idealSpan)
                }
                idealSpan.operations.add(this)
            }
        }

        fun collectDependencies(
            dependencyExtractingRemoteContext: DependencyExtractingRemoteContext
        ) {
            if (op is VariableSupport) {
                dependencyExtractingRemoteContext.spanOp = this
                op.registerListening(dependencyExtractingRemoteContext)
            }
        }
    }

    private class Span(val parent: Span?, val prev: Span?, val depth: Int, val siblingRank: Int) {
        val operations = ArrayList<SpanOp>()
        var child: Span? = null
        var next: Span? = null

        fun record(buffer: WireBuffer) {
            for (i in 0 until operations.size) {
                val op = operations[i]
                if (op.idealSpan == this) {
                    op.op.write(buffer)
                }
            }
            child?.record(buffer)
            next?.record(buffer)
        }
    }

    internal companion object {
        private fun findCommonAncestor(a: Span, b: Span): Span {
            var a = a
            var b = b
            while (a != b) {
                // Try to equalize depth & siblingRank
                if (a.depth > b.depth) {
                    a = a.parent!!
                } else if (b.depth > a.depth) {
                    b = b.parent!!
                } else if (a.siblingRank > b.siblingRank) {
                    a = a.prev!!
                } else if (b.siblingRank > a.siblingRank) {
                    b = b.prev!!
                } else {
                    a = a.parent!!
                    b = b.parent!!
                }
            }
            return a
        }
    }

    private fun addOperation(op: Operation, id: Int) {
        val spanOp = SpanOp(op, insertPoint)
        insertPoint.operations.add(spanOp)
        operationMap[id] = spanOp
        spanOp.collectDependencies(dependencyExtractingRemoteContext)
    }

    private fun addOperation(op: Operation) {
        val spanOp = SpanOp(op, insertPoint)
        insertPoint.operations.add(spanOp)
        spanOp.collectDependencies(dependencyExtractingRemoteContext)
    }

    private fun addOperation(id: Int, block: (WireBuffer, ArrayList<Operation>) -> Unit) {
        val tempOpList = ArrayList<Operation>()
        tinyBuffer.reset(0)
        block(tinyBuffer, tempOpList)
        addOperation(tempOpList.last(), id)
    }

    private fun addOperation(block: (WireBuffer, ArrayList<Operation>) -> Unit) {
        val tempOpList = ArrayList<Operation>()
        tinyBuffer.reset(0)
        block(tinyBuffer, tempOpList)
        addOperation(tempOpList.last())
    }

    /** Writes all operations to the buffer in the optimal order. */
    public fun writeToBuffer() {
        spanTreeRoot.record(buffer)
    }

    override fun reset(expectedSize: Int) {
        spanTreeRoot = Span(null, null, 0, 0)
    }

    override fun addRootContentDescription(contentDescriptionId: Int) {
        if (contentDescriptionId != 0) {
            addOperation(RootContentDescription(contentDescriptionId))
        }
    }

    override fun drawBitmap(
        imageId: Int,
        imageWidth: Int,
        imageHeight: Int,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        contentDescriptionId: Int,
    ) {
        addOperation(
            DrawBitmapInt(
                imageId,
                srcLeft,
                srcTop,
                srcRight,
                srcBottom,
                dstLeft,
                dstTop,
                dstRight,
                dstBottom,
                contentDescriptionId,
            ),
            imageId,
        )
    }

    override fun mapLookup(id: Int, mapId: Int, strId: Int) {
        addOperation(DataMapLookup(id, mapId, strId), id)
    }

    override fun addText(id: Int, text: String) {
        addOperation(TextData(id, text), id)
    }

    override fun addClickArea(
        id: Int,
        contentDescriptionId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadataId: Int,
    ) {
        addOperation(ClickArea(id, contentDescriptionId, left, top, right, bottom, metadataId), id)
    }

    override fun setRootContentBehavior(scroll: Int, alignment: Int, sizing: Int, mode: Int) {
        addOperation(RootContentBehavior(scroll, alignment, sizing, mode))
    }

    override fun addDrawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        addOperation(DrawArc(left, top, right, bottom, startAngle, sweepAngle))
    }

    override fun addDrawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        addOperation(DrawSector(left, top, right, bottom, startAngle, sweepAngle))
    }

    override fun addDrawBitmap(
        imageId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        contentDescriptionId: Int,
    ) {
        addOperation(DrawBitmap(imageId, left, top, right, bottom, contentDescriptionId))
    }

    override fun drawScaledBitmap(
        imageId: Int,
        srcLeft: Float,
        srcTop: Float,
        srcRight: Float,
        srcBottom: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        scaleType: Int,
        scaleFactor: Float,
        contentDescriptionId: Int,
    ) {
        addOperation(
            DrawBitmapScaled(
                imageId,
                srcLeft,
                srcTop,
                srcRight,
                srcBottom,
                dstLeft,
                dstTop,
                dstRight,
                dstBottom,
                scaleType,
                scaleFactor,
                contentDescriptionId,
            )
        )
    }

    override fun addBitmapFont(id: Int, glyphs: Array<BitmapFontData.Glyph>): Int {
        addOperation(BitmapFontData(id, glyphs), id)
        return id
    }

    override fun addBitmapFont(
        id: Int,
        glyphs: Array<BitmapFontData.Glyph>,
        kerningTable: MutableMap<String, Short>,
    ): Int {
        addOperation(BitmapFontData(id, glyphs, BitmapFontData.VERSION_2, kerningTable), id)
        return id
    }

    override fun setBitmapName(id: Int, name: String) {
        addOperation(NamedVariable(id, NamedVariable.IMAGE_TYPE, name))
    }

    override fun addDrawCircle(centerX: Float, centerY: Float, radius: Float) {
        addOperation(DrawCircle(centerX, centerY, radius))
    }

    override fun addDrawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        addOperation(DrawLine(x1, y1, x2, y2))
    }

    override fun addDrawOval(left: Float, top: Float, right: Float, bottom: Float) {
        addOperation(DrawOval(left, top, right, bottom))
    }

    override fun pathTween(out: Int, pid1: Int, pid2: Int, tween: Float): Int {
        addOperation(PathTween(out, pid1, pid2, tween))
        return out
    }

    override fun pathCreate(out: Int, x: Float, y: Float): Int {
        addOperation(PathCreate(out, x, y), out)
        return out
    }

    override fun pathAppend(id: Int, vararg path: Float) {
        addOperation(PathAppend(id, path))
    }

    override fun addDrawPath(pathId: Int) {
        addOperation(DrawPath(pathId))
    }

    override fun addDrawRect(left: Float, top: Float, right: Float, bottom: Float) {
        addOperation(DrawRect(left, top, right, bottom))
    }

    override fun addDrawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {
        addOperation(DrawRoundRect(left, top, right, bottom, radiusX, radiusY))
    }

    override fun addDrawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {
        addOperation(DrawTextOnPath(textId, pathId, hOffset, vOffset))
    }

    override fun addDrawTextOnCircle(
        textId: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        warpRadiusOffset: Float,
        alignment: DrawTextOnCircle.Alignment,
        placement: Placement,
    ) {
        addOperation(
            DrawTextOnCircle(
                textId,
                centerX,
                centerY,
                radius,
                startAngle,
                warpRadiusOffset,
                alignment,
                placement,
            )
        )
    }

    override fun addDrawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        addOperation(DrawText(textId, start, end, contextStart, contextEnd, x, y, rtl))
    }

    override fun addDrawBitmapFontTextRun(
        textId: Int,
        bitmapFontId: Int,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
    ) {
        addOperation(DrawBitmapFontText(textId, bitmapFontId, start, end, x, y))
    }

    override fun addDrawBitmapFontTextRunOnPath(
        textId: Int,
        bitmapFontId: Int,
        pathId: Int,
        start: Int,
        end: Int,
        yAdj: Float,
    ) {
        addOperation(DrawBitmapFontTextOnPath(textId, bitmapFontId, pathId, start, end, yAdj))
    }

    override fun drawBitmapTextAnchored(
        textId: Int,
        bitmapFontId: Int,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
    ) {
        addOperation(DrawBitmapTextAnchored(textId, bitmapFontId, start, end, x, y, panX, panY))
    }

    override fun textMerge(textId: Int, id1: Int, id2: Int): Int {
        addOperation(TextMerge(textId, id1, id2), textId)
        return textId
    }

    override fun createTextFromFloat(
        id: Int,
        value: Float,
        digitsBefore: Short,
        digitsAfter: Short,
        flags: Int,
    ): Int {
        addOperation(TextFromFloat(id, value, digitsBefore, digitsAfter, flags), id)
        return id
    }

    override fun drawTextAnchored(
        textId: Int,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int,
    ) {
        addOperation(DrawTextAnchored(textId, x, y, panX, panY, flags))
    }

    override fun addDrawTweenPath(
        path1Id: Int,
        path2Id: Int,
        tween: Float,
        start: Float,
        stop: Float,
    ) {
        addOperation(DrawTweenPath(path1Id, path2Id, tween, start, stop))
    }

    override fun addPathData(id: Int, pathData: FloatArray): Int {
        val winding: Int = id shr 24
        addOperation(PathData(id, pathData, winding), id)
        return id
    }

    override fun addPathData(id: Int, pathData: FloatArray, winding: Int): Int {
        addOperation(PathData(id, pathData, winding), id)
        return id
    }

    override fun addPaint(paint: PaintBundle) {
        addOperation(PaintData(paint))
    }

    override fun setTheme(theme: Int) {
        addOperation(Theme(theme))
    }

    override fun addMatrixSkew(skewX: Float, skewY: Float) {
        addOperation(MatrixSkew(skewX, skewY))
    }

    override fun addMatrixRestore() {
        addOperation(MatrixRestore())
    }

    override fun addMatrixSave() {
        addOperation(MatrixSave())
    }

    override fun addMatrixRotate(angle: Float, centerX: Float, centerY: Float) {
        addOperation(MatrixRotate(angle, centerX, centerY))
    }

    override fun addMatrixTranslate(dx: Float, dy: Float) {
        addOperation(MatrixTranslate(dx, dy))
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float) {
        addOperation(MatrixScale(scaleX, scaleY, Float.NaN, Float.NaN))
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        addOperation(MatrixScale(scaleX, scaleY, centerX, centerY))
    }

    override fun addClipPath(pathId: Int) {
        val id = pathId and 0xFFFFF
        val regionOp = pathId shr 24
        addOperation(ClipPath(id, regionOp))
    }

    override fun addClipRect(left: Float, top: Float, right: Float, bottom: Float) {
        addOperation(ClipRect(left, top, right, bottom))
    }

    override fun addFloat(id: Int, value: Float): Float {
        addOperation(FloatConstant(id, value), id)
        return Utils.asNan(id)
    }

    override fun addInteger(id: Int, value: Int) {
        addOperation(IntegerConstant(id, value), id)
    }

    override fun addLong(id: Int, value: Long) {
        addOperation(LongConstant(id, value), id)
    }

    override fun addBoolean(id: Int, value: Boolean) {
        addOperation(BooleanConstant(id, value), id)
    }

    override fun addAnimatedFloat(id: Int, vararg value: Float) {
        addOperation(FloatExpression(id, value, null), id)
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray, animation: FloatArray?) {
        addOperation(FloatExpression(id, value, animation), id)
    }

    override fun addTouchExpression(
        id: Int,
        value: Float,
        min: Float,
        max: Float,
        velocityId: Float,
        touchEffects: Int,
        exp: FloatArray,
        touchMode: Int,
        touchSpec: FloatArray?,
        easingSpec: FloatArray?,
    ) {
        // TODO we should refactor to avoid this round trip
        addOperation(id) { buffer, operations ->
            TouchExpression.apply(
                buffer,
                id,
                value,
                min,
                max,
                velocityId,
                touchEffects,
                exp,
                touchMode,
                touchSpec,
                easingSpec,
            )
            buffer.index = 0
            TouchExpression.read(buffer, operations)
        }
    }

    override fun textMeasure(id: Int, textId: Int, mode: Int) {
        addOperation(TextMeasure(id, textId, mode), id)
    }

    override fun textLength(id: Int, textId: Int) {
        addOperation(TextLength(id, textId), id)
    }

    override fun addFloatArray(id: Int, values: FloatArray) {
        addOperation(DataListFloat(id, values), id)
    }

    override fun addDynamicFloatArray(id: Int, size: Float) {
        addOperation(DataDynamicListFloat(id, size), id)
    }

    override fun setArrayValue(id: Int, index: Float, value: Float) {
        addOperation(UpdateDynamicFloatList(id, index, value), id)
    }

    override fun addList(id: Int, listId: IntArray) {
        addOperation(DataListIds(id, listId), id)
    }

    override fun addMap(id: Int, keys: Array<String>, types: ByteArray?, listId: IntArray) {
        addOperation(DataMapIds(id, keys, types!!, listId), id)
    }

    override fun textLookup(id: Int, dataSet: Float, index: Float) {
        addOperation(TextLookup(id, Utils.idFromNan(dataSet), index), id)
    }

    override fun idLookup(id: Int, dataSet: Float, index: Float) {
        addOperation(IdLookup(id, Utils.idFromNan(dataSet), index), id)
    }

    override fun textLookup(id: Int, dataSet: Float, index: Int) {
        addOperation(TextLookupInt(id, Utils.idFromNan(dataSet), index), id)
    }

    override fun addIntegerExpression(id: Int, mask: Int, value: IntArray) {
        addOperation(IntegerExpression(id, mask, value), id)
    }

    override fun addColor(id: Int, color: Int) {
        addOperation(ColorConstant(id, color), id)
    }

    override fun addColorExpression(id: Int, color1: Int, color2: Int, tween: Float) {
        addOperation(ColorExpression(id, 0, color1, color2, tween), id)
    }

    override fun addColorExpression(id: Int, color1: Short, color2: Int, tween: Float) {
        addOperation(ColorExpression(id, 1, color1.toInt(), color2, tween), id)
    }

    override fun addColorExpression(id: Int, color1: Int, color2: Short, tween: Float) {
        addOperation(ColorExpression(id, 2, color1, color2.toInt(), tween), id)
    }

    override fun addColorExpression(id: Int, color1: Short, color2: Short, tween: Float) {
        addOperation(ColorExpression(id, 3, color1.toInt(), color2.toInt(), tween), id)
    }

    override fun addColorExpression(id: Int, hue: Float, sat: Float, value: Float) {
        addOperation(ColorExpression(id, hue, sat, value), id)
    }

    override fun addColorExpression(id: Int, alpha: Int, hue: Float, sat: Float, value: Float) {
        addOperation(ColorExpression(id, ColorExpression.HSV_MODE, alpha, hue, sat, value), id)
    }

    override fun addColorExpression(id: Int, alpha: Float, red: Float, green: Float, blue: Float) {
        addOperation(ColorExpression(id, ColorExpression.ARGB_MODE, alpha, red, green, blue), id)
    }

    override fun setNamedVariable(id: Int, name: String, type: Int) {
        addOperation(NamedVariable(id, type, name), id)
    }

    override fun addComponentStart(type: Int, id: Int) {
        mLastComponentId = getComponentId(id)
        addOperation(ComponentStart(type, mLastComponentId, 0f, 0f))
    }

    override fun addContainerEnd() {
        addOperation(ContainerEnd())
    }

    override fun addModifierScroll(direction: Int, max: Float) {
        addOperation(ScrollModifierOperation(direction, 0f, max, 0f))
        addOperation(ContainerEnd())
    }

    override fun addModifierBackground(color: Int, shape: Int) {
        val r = (color shr 16 and 0xff) / 255.0f
        val g = (color shr 8 and 0xff) / 255.0f
        val b = (color and 0xff) / 255.0f
        val a = (color shr 24 and 0xff) / 255.0f
        addOperation(BackgroundModifierOperation(0f, 0f, 0f, 0f, r, g, b, a, shape))
    }

    override fun addModifierBackground(r: Float, g: Float, b: Float, a: Float, shape: Int) {
        addOperation(BackgroundModifierOperation(0f, 0f, 0f, 0f, r, g, b, a, shape))
    }

    override fun addModifierAlignBy(line: Float) {
        addOperation(AlignByModifierOperation(line, 0))
    }

    override fun addModifierBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        color: Int,
        shape: Int,
    ) {
        val r = (color shr 16 and 0xff) / 255.0f
        val g = (color shr 8 and 0xff) / 255.0f
        val b = (color and 0xff) / 255.0f
        val a = (color shr 24 and 0xff) / 255.0f
        addOperation(
            BorderModifierOperation(
                0f,
                0f,
                0f,
                0f,
                borderWidth,
                borderRoundedCorner,
                r,
                g,
                b,
                a,
                shape,
            )
        )
    }

    override fun addModifierPadding(left: Float, top: Float, right: Float, bottom: Float) {
        addOperation(PaddingModifierOperation(left, top, right, bottom))
    }

    override fun addModifierOffset(x: Float, y: Float) {
        addOperation(OffsetModifierOperation(x, y))
    }

    override fun addModifierZIndex(value: Float) {
        addOperation(ZIndexModifierOperation(value))
    }

    override fun addModifierRipple() {
        addOperation(RippleModifierOperation())
    }

    override fun addModifierMarquee(
        iterations: Int,
        animationMode: Int,
        repeatDelayMillis: Float,
        initialDelayMillis: Float,
        spacing: Float,
        velocity: Float,
    ) {
        addOperation(
            MarqueeModifierOperation(
                iterations,
                animationMode,
                repeatDelayMillis,
                initialDelayMillis,
                spacing,
                velocity,
            )
        )
    }

    override fun addModifierGraphicsLayer(attributes: HashMap<Int?, Any?>) {
        // TODO we should add a constructor to GraphicsLayerModifierOperation to avoid this round
        // trip.
        addOperation { buffer, operations ->
            GraphicsLayerModifierOperation.apply(buffer, attributes)
            buffer.index = 0
            GraphicsLayerModifierOperation.read(buffer, operations)
        }
    }

    override fun addRoundClipRectModifier(
        topStart: Float,
        topEnd: Float,
        bottomStart: Float,
        bottomEnd: Float,
    ) {
        addOperation(RoundedClipRectModifierOperation(topStart, topEnd, bottomStart, bottomEnd))
    }

    override fun addClipRectModifier() {
        addOperation(ClipRectModifierOperation())
    }

    override fun addLoopStart(indexId: Int, from: Float, step: Float, until: Float) {
        addOperation(LoopOperation(indexId, from, step, until))
    }

    override fun addLoopEnd() {
        addOperation(ContainerEnd())
    }

    override fun addStateLayout(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        indexId: Int,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(StateLayout(mLastComponentId, animationId, horizontal, vertical, indexId))
    }

    override fun addBoxStart(componentId: Int, animationId: Int, horizontal: Int, vertical: Int) {
        mLastComponentId = getComponentId(componentId)
        addOperation(BoxLayout(null, mLastComponentId, animationId, horizontal, vertical))
    }

    override fun addFitBoxStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(FitBoxLayout(null, mLastComponentId, animationId, horizontal, vertical))
    }

    override fun addImage(
        componentId: Int,
        animationId: Int,
        bitmapId: Int,
        scaleType: Int,
        alpha: Float,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(ImageLayout(null, componentId, animationId, bitmapId, scaleType, alpha))
    }

    override fun addRowStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(RowLayout(null, mLastComponentId, animationId, horizontal, vertical, spacedBy))
    }

    override fun addCollapsibleRowStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(
            CollapsibleRowLayout(
                null,
                mLastComponentId,
                animationId,
                horizontal,
                vertical,
                spacedBy,
            )
        )
    }

    override fun addColumnStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(
            ColumnLayout(null, mLastComponentId, animationId, horizontal, vertical, spacedBy)
        )
    }

    override fun addCollapsibleColumnStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        mLastComponentId = getComponentId(componentId)
        addOperation(
            CollapsibleColumnLayout(
                null,
                mLastComponentId,
                animationId,
                horizontal,
                vertical,
                spacedBy,
            )
        )
    }

    override fun addCanvasStart(componentId: Int, animationId: Int) {
        mLastComponentId = getComponentId(componentId)
        addOperation(CanvasLayout(null, mLastComponentId, animationId))
    }

    override fun addCanvasContentStart(componentId: Int) {
        mLastComponentId = getComponentId(componentId)
        addOperation(CanvasContent(mLastComponentId))
    }

    override fun addRootStart() {
        mLastComponentId = getComponentId(-1)
        addOperation(RootLayoutComponent(mLastComponentId))
    }

    override fun addContentStart() {
        mLastComponentId = getComponentId(-1)
        addOperation(LayoutComponentContent(mLastComponentId))
    }

    override fun addCanvasOperationsStart() {
        addOperation(CanvasOperations())
    }

    override fun addRunActionsStart() {
        addOperation(RunActionOperation())
    }

    override fun addComponentWidthValue(id: Int) {
        addOperation(ComponentValue(ComponentValue.WIDTH, mLastComponentId, id))
    }

    override fun addComponentHeightValue(id: Int) {
        addOperation(ComponentValue(ComponentValue.HEIGHT, mLastComponentId, id))
    }

    override fun addTextComponentStart(
        componentId: Int,
        animationId: Int,
        textId: Int,
        color: Int,
        fontSize: Float,
        fontStyle: Int,
        fontWeight: Float,
        fontFamilyId: Int,
        flags: Short,
        textAlign: Short,
        overflow: Int,
        maxLines: Int,
    ) {
        mLastComponentId = getComponentId(componentId)
        val flagsAndTextAlign = (flags.toInt() shl 16) or (textAlign.toInt() and 0xFFFF)

        addOperation(
            TextLayout(
                null,
                mLastComponentId,
                animationId,
                textId,
                color,
                fontSize,
                fontStyle,
                fontWeight,
                fontFamilyId,
                flagsAndTextAlign,
                overflow,
                maxLines,
            )
        )
    }

    override fun addImpulse(duration: Float, start: Float) {
        addOperation(ImpulseOperation(duration, start))
    }

    override fun addImpulseProcess() {
        addOperation(ImpulseProcess())
    }

    override fun addImpulseEnd() {
        addOperation(ContainerEnd())
    }

    override fun addParticles(
        id: Int,
        varIds: IntArray,
        initialExpressions: Array<FloatArray?>,
        particleCount: Int,
    ) {
        addOperation(ParticlesCreate(id, varIds, initialExpressions, particleCount))
    }

    override fun addParticlesLoop(id: Int, restart: FloatArray?, expressions: Array<FloatArray?>) {
        addOperation(ParticlesLoop(id, restart, expressions))
    }

    override fun addParticlesComparison(
        id: Int,
        flags: Short,
        min: Float,
        max: Float,
        condition: FloatArray?,
        apply1: Array<FloatArray?>?,
        apply2: Array<FloatArray?>?,
    ) {
        addOperation(ParticlesCompare(id, flags, min, max, condition, apply1, apply2))
    }

    override fun addParticleLoopEnd() {
        addOperation(ContainerEnd())
    }

    override fun defineFloatFunction(fid: Int, args: IntArray) {
        addOperation(FloatFunctionDefine(fid, args), fid)
    }

    override fun addEndFloatFunctionDef() {
        addOperation(ContainerEnd())
    }

    override fun callFloatFunction(id: Int, args: FloatArray?) {
        addOperation(FloatFunctionCall(id, args), id)
    }

    override fun bitmapAttribute(id: Int, bitmapId: Int, attribute: Short) {
        addOperation(ImageAttribute(id, bitmapId, attribute, null), id)
    }

    override fun textAttribute(id: Int, textId: Int, attribute: Short) {
        addOperation(TextAttribute(id, textId, attribute), id)
    }

    override fun timeAttribute(id: Int, timeId: Int, attribute: Short, vararg args: Int) {
        addOperation(TimeAttribute(id, timeId, attribute, args), id)
    }

    override fun drawComponentContent() {
        addOperation(DrawContent())
    }

    override fun storeBitmap(
        imageId: Int,
        imageWidth: Int,
        imageHeight: Int,
        data: ByteArray,
    ): Int {
        addOperation(BitmapData(imageId, imageWidth, imageHeight, data))
        return imageId
    }

    override fun createBitmap(imageId: Int, imageWidth: Short, imageHeight: Short): Int {
        addOperation(
            BitmapData(
                imageId,
                BitmapData.TYPE_RAW8888,
                imageWidth,
                BitmapData.ENCODING_EMPTY,
                imageHeight,
                ByteArray(0),
            )
        )
        return imageId
    }

    override fun drawOnBitmap(imageId: Int, mode: Int, color: Int) {
        addOperation(DrawToBitmap(imageId, mode, color))
    }

    override fun storeBitmapA8(
        imageId: Int,
        imageWidth: Int,
        imageHeight: Int,
        data: ByteArray,
    ): Int {
        addOperation(
            BitmapData(
                imageId,
                BitmapData.TYPE_PNG_ALPHA_8,
                imageWidth.toShort(),
                BitmapData.ENCODING_INLINE,
                imageHeight.toShort(),
                data,
            )
        )
        return imageId
    }

    override fun storeBitmapUrl(imageId: Int, url: String): Int {
        addOperation(
            BitmapData(
                imageId,
                BitmapData.TYPE_PNG,
                1.toShort(),
                BitmapData.ENCODING_URL,
                1.toShort(),
                url.toByteArray(StandardCharsets.UTF_8),
            )
        )
        return imageId
    }

    override fun pathCombine(id: Int, path1: Int, path2: Int, op: Byte) {
        // TODO is this right?
        addOperation(PathCombine(id, path1, path2, op), id)
    }

    override fun performHaptic(feedbackConstant: Int) {
        addOperation(HapticFeedback(feedbackConstant))
    }

    override fun addConditionalOperations(type: Byte, a: Float, b: Float) {
        val child =
            Span(parent = insertPoint, prev = null, depth = insertPoint.depth + 1, siblingRank = 0)
        insertPoint.child = child
        insertPoint = child
        addOperation(ConditionalOperations(type, a, b))
    }

    override fun endConditionalOperations() {
        addOperation(ContainerEnd())
        insertPoint = insertPoint.parent!!
        val next =
            Span(
                parent = insertPoint.parent,
                prev = insertPoint,
                depth = insertPoint.depth,
                siblingRank = insertPoint.siblingRank + 1,
            )
        insertPoint.next = next
        insertPoint = next
    }

    override fun addDebugMessage(textId: Int, value: Float, flags: Int) {
        addOperation(DebugMessage(textId, value, flags))
    }

    override fun getColorAttribute(id: Int, baseColor: Int, type: Short) {
        addOperation(ColorAttribute(id, baseColor, type), id)
    }

    override fun setMatrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {
        addOperation(MatrixFromPath(pathId, fraction, vOffset, flags))
    }

    override fun textSubtext(id: Int, txtId: Int, start: Float, len: Float) {
        addOperation(TextSubtext(id, txtId, start, len), id)
    }

    override fun bitmapTextMeasure(id: Int, textId: Int, bmFontId: Int, type: Int) {
        addOperation(BitmapTextMeasure(id, textId, bmFontId, type), id)
    }

    override fun rem(text: String) {
        addOperation(Rem(text))
    }

    override fun setVersion(documentApiLevel: Int, profiles: Int) {
        throw UnsupportedOperationException("setVersion is not supported")
    }

    override fun setVersion(documentApiLevel: Int, supportedOperations: MutableSet<Int?>) {
        throw UnsupportedOperationException("setVersion is not supported")
    }

    override fun addMatrixConst(id: Int, values: FloatArray) {
        addOperation(MatrixConstant(id, 0, values), id)
    }

    override fun addMatrixExpression(id: Int, exp: FloatArray) {
        addOperation(MatrixExpression(id, 0, exp), id)
    }

    override fun addMatrixVectorMath(
        matrixId: Float,
        type: Short,
        from: FloatArray,
        outId: IntArray,
    ) {
        // TODO should we specify the id?
        addOperation(MatrixVectorMath(type, outId, Utils.idFromNan(matrixId), from))
    }

    override fun addFont(id: Int, type: Int, data: ByteArray) {
        addOperation(FontData(id, type, data))
    }

    override fun wakeIn(seconds: Float) {
        addOperation(WakeIn(seconds))
    }

    override fun addPathExpression(
        id: Int,
        expressionX: FloatArray,
        expressionY: FloatArray?,
        start: Float,
        end: Float,
        count: Float,
        flags: Int,
    ) {
        addOperation(PathExpression(id, expressionX, expressionY, start, end, count, flags), id)
    }

    override fun addComponentVisibilityOperation(valueId: Int) {
        addOperation(ComponentVisibilityOperation(valueId))
    }

    override fun addWidthModifierOperation(type: Int, value: Float) {
        addOperation(WidthModifierOperation(DimensionModifierOperation.Type.fromInt(type), value))
    }

    override fun addHeightModifierOperation(type: Int, value: Float) {
        addOperation(HeightModifierOperation(DimensionModifierOperation.Type.fromInt(type), value))
    }

    override fun addHeightInModifierOperation(min: Float, max: Float) {
        addOperation(HeightInModifierOperation(min, max))
    }

    override fun addTouchDownModifierOperation() {
        addOperation(TouchDownModifierOperation())
    }

    override fun addTouchUpModifierOperation() {
        addOperation(TouchUpModifierOperation())
    }

    override fun addTouchCancelModifierOperation() {
        addOperation(TouchCancelModifierOperation())
    }

    override fun addWidthInModifierOperation(min: Float, max: Float) {
        addOperation(WidthInModifierOperation(min, max))
    }

    override fun addDrawContentOperation() {
        addOperation(DrawContentOperation())
    }

    override fun startLayoutCompute(type: Int, boundsId: Int, animateChanges: Boolean) {
        addOperation(LayoutComputeOperation(type, boundsId, animateChanges))
    }

    override fun endLayoutCompute() {
        addOperation(ContainerEnd())
    }

    override fun addSemanticsModifier(
        contentDescriptionId: Int,
        role: Byte,
        textId: Int,
        stateDescriptionId: Int,
        mode: Int,
        enabled: Boolean,
        clickable: Boolean,
    ) {
        addOperation(
            CoreSemantics(
                contentDescriptionId,
                role,
                textId,
                stateDescriptionId,
                mode,
                enabled,
                clickable,
            )
        )
    }

    override fun addClickModifierOperation() {
        addOperation(ClickModifierOperation())
    }

    override fun addCollapsiblePriorityModifier(orientation: Int, priority: Float) {
        addOperation(CollapsiblePriorityModifierOperation(orientation, priority))
    }

    override fun addAnimationSpecModifier(
        animationId: Int,
        motionDuration: Float,
        motionEasingType: Int,
        visibilityDuration: Float,
        visibilityEasingType: Int,
        enterAnimation: Int,
        exitAnimation: Int,
    ) {
        addOperation(
            AnimationSpec(
                animationId,
                motionDuration,
                motionEasingType,
                visibilityDuration,
                visibilityEasingType,
                AnimationSpec.intToAnimation(enterAnimation),
                AnimationSpec.intToAnimation(exitAnimation),
            )
        )
    }

    override fun addValueStringChangeActionOperation(destTextId: Int, srcTextId: Int) {
        addOperation(ValueStringChangeActionOperation(destTextId, srcTextId))
    }

    override fun addValueIntegerExpressionChangeActionOperation(
        destIntegerId: Long,
        srcIntegerId: Long,
    ) {
        addOperation(ValueIntegerExpressionChangeActionOperation(destIntegerId, srcIntegerId))
    }

    override fun addValueFloatChangeActionOperation(valueId: Int, value: Float) {
        addOperation(ValueFloatChangeActionOperation(valueId, value))
    }

    override fun addValueIntegerChangeActionOperation(valueId: Int, value: Int) {
        addOperation(ValueIntegerChangeActionOperation(valueId, value))
    }

    override fun addValueFloatExpressionChangeActionOperation(mValueId: Int, mValue: Int) {
        addOperation(ValueFloatExpressionChangeActionOperation(mValueId, mValue))
    }

    /** Used to extract the dependencies from [listensTo]. */
    private inner class DependencyExtractingRemoteContext : RemoteContext() {
        var spanOp: SpanOp? = null

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
            TODO("Not yet implemented")
        }

        override fun getPathData(instanceId: Int): FloatArray? {
            TODO("Not yet implemented")
        }

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {
            TODO("Not yet implemented")
        }

        override fun loadColor(id: Int, color: Int) {
            TODO("Not yet implemented")
        }

        override fun setNamedColorOverride(colorName: String, color: Int) {
            TODO("Not yet implemented")
        }

        override fun setNamedStringOverride(stringName: String, value: String) {
            TODO("Not yet implemented")
        }

        override fun clearNamedStringOverride(stringName: String) {
            TODO("Not yet implemented")
        }

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
            TODO("Not yet implemented")
        }

        override fun clearNamedBooleanOverride(booleanName: String) {
            TODO("Not yet implemented")
        }

        override fun setNamedIntegerOverride(integerName: String, value: Int) {
            TODO("Not yet implemented")
        }

        override fun clearNamedIntegerOverride(integerName: String) {
            TODO("Not yet implemented")
        }

        override fun setNamedFloatOverride(floatName: String, value: Float) {
            TODO("Not yet implemented")
        }

        override fun clearNamedFloatOverride(floatName: String) {
            TODO("Not yet implemented")
        }

        override fun setNamedLong(name: String, value: Long) {
            TODO("Not yet implemented")
        }

        override fun setNamedDataOverride(dataName: String, value: Any) {
            TODO("Not yet implemented")
        }

        override fun clearNamedDataOverride(dataName: String) {
            TODO("Not yet implemented")
        }

        override fun addCollection(id: Int, collection: ArrayAccess) {
            TODO("Not yet implemented")
        }

        override fun putDataMap(id: Int, map: DataMap) {
            TODO("Not yet implemented")
        }

        override fun getDataMap(id: Int): DataMap? {
            TODO("Not yet implemented")
        }

        override fun runAction(id: Int, metadata: String) {
            TODO("Not yet implemented")
        }

        override fun runNamedAction(id: Int, value: Any?) {
            TODO("Not yet implemented")
        }

        override fun putObject(id: Int, value: Any) {
            TODO("Not yet implemented")
        }

        override fun getObject(id: Int): Any? {
            TODO("Not yet implemented")
        }

        override fun hapticEffect(type: Int) {
            TODO("Not yet implemented")
        }

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {
            TODO("Not yet implemented")
        }

        override fun loadText(id: Int, text: String) {
            TODO("Not yet implemented")
        }

        override fun getText(id: Int): String? {
            TODO("Not yet implemented")
        }

        override fun loadFloat(id: Int, value: Float) {
            TODO("Not yet implemented")
        }

        override fun overrideFloat(id: Int, value: Float) {
            TODO("Not yet implemented")
        }

        override fun loadInteger(id: Int, value: Int) {
            TODO("Not yet implemented")
        }

        override fun overrideInteger(id: Int, value: Int) {
            TODO("Not yet implemented")
        }

        override fun overrideText(id: Int, valueId: Int) {
            TODO("Not yet implemented")
        }

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
            TODO("Not yet implemented")
        }

        override fun loadShader(id: Int, value: ShaderData) {
            TODO("Not yet implemented")
        }

        override fun getFloat(id: Int): Float {
            TODO("Not yet implemented")
        }

        override fun getInteger(id: Int): Int {
            TODO("Not yet implemented")
        }

        override fun getLong(id: Int): Long {
            TODO("Not yet implemented")
        }

        override fun getColor(id: Int): Int {
            TODO("Not yet implemented")
        }

        override fun listensTo(id: Int, variableSupport: VariableSupport) {
            // We can expect this lookup to fail for time related IDs.
            val dep = operationMap[id] ?: return
            spanOp!!.deps.add(dep)
            dep.recordUsageBySpan(insertPoint)
        }

        override fun updateOps(): Int {
            TODO("Not yet implemented")
        }

        override fun getShader(id: Int): ShaderData? {
            TODO("Not yet implemented")
        }

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {
            TODO("Not yet implemented")
        }
    }
}
