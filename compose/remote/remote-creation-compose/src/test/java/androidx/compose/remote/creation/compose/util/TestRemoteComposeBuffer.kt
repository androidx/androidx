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

package androidx.compose.remote.creation.compose.util

import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.profile.Profile
import java.util.ArrayList

/**
 * A test implementation of [RemoteComposeBuffer] used in tests to record calls made to the buffer
 * for verification. It stores formatted strings of calls in [calls] list.
 */
internal class TestRemoteComposeBuffer : RemoteComposeBuffer() {
    val calls = ArrayList<String>()

    private fun format(f: Float): String {
        return if (f.isNaN()) {
            "ID(${Utils.idFromNan(f)})"
        } else {
            f.toString()
        }
    }

    override fun addClipRect(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addClipRect(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)})"
        )
        super.addClipRect(left, top, right, bottom)
    }

    override fun addDrawRect(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addDrawRect(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)})"
        )
        super.addDrawRect(left, top, right, bottom)
    }

    override fun addConditionalOperations(type: Byte, a: Float, b: Float) {
        calls.add("addConditionalOperations($type, ${format(a)}, ${format(b)})")
        super.addConditionalOperations(type, a, b)
    }

    override fun endConditionalOperations() {
        calls.add("endConditionalOperations")
        super.endConditionalOperations()
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray) {
        val labels = arrayOfNulls<String>(value.size)
        for (i in 0 until value.size) {
            if (value[i].isNaN()) {
                labels[i] = "[" + Utils.idFromNan(value[i]) + "]"
            }
        }
        val exprStr = AnimatedFloatExpression.toString(value, labels)
        calls.add("addAnimatedFloat($id) = ($exprStr)")
        super.addAnimatedFloat(id, *value)
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray, animation: FloatArray?) {
        val labels = arrayOfNulls<String>(value.size)
        for (i in 0 until value.size) {
            if (value[i].isNaN()) {
                labels[i] = "[" + Utils.idFromNan(value[i]) + "]"
            }
        }
        val exprStr = AnimatedFloatExpression.toString(value, labels)
        calls.add("addAnimatedFloat($id) = ($exprStr)")
        super.addAnimatedFloat(id, value, animation)
    }

    override fun addIntegerExpression(id: Int, mask: Int, value: IntArray) {
        calls.add("addIntegerExpression($id, $mask, ${value.toList()})")
        super.addIntegerExpression(id, mask, value)
    }

    override fun addLoopStart(indexId: Int, from: Float, step: Float, until: Float) {
        calls.add("addLoopStart($indexId, ${format(from)}, ${format(step)}, ${format(until)})")
        super.addLoopStart(indexId, from, step, until)
    }

    override fun addLoopEnd() {
        calls.add("addLoopEnd")
        super.addLoopEnd()
    }

    override fun drawOnBitmap(bitmapId: Int, mode: Int, color: Int) {
        calls.add("drawOnBitmap($bitmapId, $mode, $color)")
        super.drawOnBitmap(bitmapId, mode, color)
    }

    override fun textLength(id: Int, textId: Int) {
        calls.add("textLength($id)")
        super.textLength(id, textId)
    }

    override fun textMerge(textId: Int, id1: Int, id2: Int): Int {
        calls.add("textMerge($textId, $id1, $id2)")
        return super.textMerge(textId, id1, id2)
    }

    override fun addText(id: Int, text: String) {
        calls.add("textData($id, \"$text\")")
        super.addText(id, text)
    }

    override fun addList(id: Int, value: IntArray) {
        calls.add("addList($id, ${value.toList()})")
        super.addList(id, value)
    }

    override fun createTextFromFloat(
        textId: Int,
        value: Float,
        before: Short,
        after: Short,
        flags: Int,
    ): Int {
        calls.add("createTextFromFloat($textId, ${format(value)}, $before, $after, $flags)")
        return super.createTextFromFloat(textId, value, before, after, flags)
    }

    override fun textLookup(textId: Int, stringListId: Float, indexId: Float) {
        calls.add("textLookup($textId, ${format(stringListId)}, ${format(indexId)})")
        super.textLookup(textId, stringListId, indexId)
    }

    override fun textLookup(textId: Int, stringListId: Float, indexId: Int) {
        calls.add("textLookup($textId, ${format(stringListId)}, $indexId)")
        super.textLookup(textId, stringListId, indexId)
    }

    override fun textSubtext(textId: Int, sourceTextId: Int, start: Float, length: Float) {
        calls.add("textSubtext($textId, $sourceTextId, ${format(start)}, ${format(length)})")
        super.textSubtext(textId, sourceTextId, start, length)
    }

    override fun textTransform(
        textId: Int,
        sourceTextId: Int,
        start: Float,
        length: Float,
        type: Int,
    ) {
        calls.add(
            "textTransform($textId, $sourceTextId, ${format(start)}, ${format(length)}, $type)"
        )
        super.textTransform(textId, sourceTextId, start, length, type)
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
        calls.add("addDrawTextRun($textId)")
        super.addDrawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl)
    }

    override fun addDrawBitmap(
        bitmapId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        descriptionId: Int,
    ) {
        calls.add("addDrawBitmap($bitmapId)")
        super.addDrawBitmap(bitmapId, left, top, right, bottom, descriptionId)
    }

    override fun addDrawBitmapFontTextRun(
        textId: Int,
        bitmapFontId: Int,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        glyphSpacing: Float,
    ) {
        calls.add("addDrawBitmapFontTextRun($textId, $bitmapFontId)")
        super.addDrawBitmapFontTextRun(textId, bitmapFontId, start, end, x, y, glyphSpacing)
    }

    override fun addPaint(paint: PaintBundle) {
        calls.add("addPaint")
        super.addPaint(paint)
    }

    override fun addMatrixSave() {
        calls.add("addMatrixSave")
        super.addMatrixSave()
    }

    override fun addMatrixRestore() {
        calls.add("addMatrixRestore")
        super.addMatrixRestore()
    }

    override fun addRootContentDescription(contentDescriptionId: Int) {
        calls.add("addRootContentDescription($contentDescriptionId)")
        super.addRootContentDescription(contentDescriptionId)
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
        calls.add("drawBitmap($imageId)")
        super.drawBitmap(
            imageId,
            imageWidth,
            imageHeight,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            contentDescriptionId,
        )
    }

    override fun mapLookup(id: Int, mapId: Int, strId: Int) {
        calls.add("mapLookup($id, $mapId, $strId)")
        super.mapLookup(id, mapId, strId)
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
        calls.add("addClickArea($id)")
        super.addClickArea(id, contentDescriptionId, left, top, right, bottom, metadataId)
    }

    override fun setRootContentBehavior(scroll: Int, alignment: Int, sizing: Int, mode: Int) {
        calls.add("setRootContentBehavior($scroll, $alignment, $sizing, $mode)")
        super.setRootContentBehavior(scroll, alignment, sizing, mode)
    }

    override fun addDrawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        calls.add(
            "addDrawArc(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)}, $startAngle, $sweepAngle)"
        )
        super.addDrawArc(left, top, right, bottom, startAngle, sweepAngle)
    }

    override fun addDrawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        calls.add(
            "addDrawSector(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)}, $startAngle, $sweepAngle)"
        )
        super.addDrawSector(left, top, right, bottom, startAngle, sweepAngle)
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
        calls.add("drawScaledBitmap($imageId)")
        super.drawScaledBitmap(
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
    }

    override fun addBitmapFont(
        id: Int,
        glyphs: Array<androidx.compose.remote.core.operations.BitmapFontData.Glyph>,
    ): Int {
        calls.add("addBitmapFont($id)")
        return super.addBitmapFont(id, glyphs)
    }

    override fun addBitmapFont(
        id: Int,
        glyphs: Array<androidx.compose.remote.core.operations.BitmapFontData.Glyph>,
        kerningTable: Map<String, Short>,
    ): Int {
        calls.add("addBitmapFont($id)")
        return super.addBitmapFont(id, glyphs, kerningTable)
    }

    override fun setBitmapName(id: Int, name: String) {
        calls.add("setBitmapName($id, \"$name\")")
        super.setBitmapName(id, name)
    }

    override fun addDrawCircle(centerX: Float, centerY: Float, radius: Float) {
        calls.add("addDrawCircle(${format(centerX)}, ${format(centerY)}, ${format(radius)})")
        super.addDrawCircle(centerX, centerY, radius)
    }

    override fun addDrawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        calls.add("addDrawLine(${format(x1)}, ${format(y1)}, ${format(x2)}, ${format(y2)})")
        super.addDrawLine(x1, y1, x2, y2)
    }

    override fun addDrawOval(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addDrawOval(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)})"
        )
        super.addDrawOval(left, top, right, bottom)
    }

    override fun pathTween(out: Int, pid1: Int, pid2: Int, tween: Float): Int {
        calls.add("pathTween($out, $pid1, $pid2, $tween)")
        return super.pathTween(out, pid1, pid2, tween)
    }

    override fun pathCreate(out: Int, x: Float, y: Float): Int {
        calls.add("pathCreate($out, ${format(x)}, ${format(y)})")
        return super.pathCreate(out, x, y)
    }

    override fun pathAppend(id: Int, vararg path: Float) {
        calls.add("pathAppend($id, ...)")
        super.pathAppend(id, *path)
    }

    override fun addDrawPath(pathId: Int) {
        calls.add("addDrawPath($pathId)")
        super.addDrawPath(pathId)
    }

    override fun addDrawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {
        calls.add(
            "addDrawRoundRect(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)}, ${format(radiusX)}, ${format(radiusY)})"
        )
        super.addDrawRoundRect(left, top, right, bottom, radiusX, radiusY)
    }

    override fun addDrawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {
        calls.add("addDrawTextOnPath($textId, $pathId, ${format(hOffset)}, ${format(vOffset)})")
        super.addDrawTextOnPath(textId, pathId, hOffset, vOffset)
    }

    override fun addDrawTextOnCircle(
        textId: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        warpRadiusOffset: Float,
        alignment: androidx.compose.remote.core.operations.DrawTextOnCircle.Alignment,
        placement: androidx.compose.remote.core.operations.DrawTextOnCircle.Placement,
    ) {
        calls.add("addDrawTextOnCircle($textId)")
        super.addDrawTextOnCircle(
            textId,
            centerX,
            centerY,
            radius,
            startAngle,
            warpRadiusOffset,
            alignment,
            placement,
        )
    }

    override fun addDrawBitmapFontTextRunOnPath(
        textId: Int,
        bitmapFontId: Int,
        pathId: Int,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float,
    ) {
        calls.add("addDrawBitmapFontTextRunOnPath($textId, $bitmapFontId, $pathId)")
        super.addDrawBitmapFontTextRunOnPath(
            textId,
            bitmapFontId,
            pathId,
            start,
            end,
            yAdj,
            glyphSpacing,
        )
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
        glyphSpacing: Float,
    ) {
        calls.add("drawBitmapTextAnchored($textId, $bitmapFontId)")
        super.drawBitmapTextAnchored(
            textId,
            bitmapFontId,
            start,
            end,
            x,
            y,
            panX,
            panY,
            glyphSpacing,
        )
    }

    override fun drawTextAnchored(
        textId: Int,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int,
    ) {
        calls.add(
            "drawTextAnchored($textId, ${format(x)}, ${format(y)}, ${format(panX)}, ${format(panY)}, $flags)"
        )
        super.drawTextAnchored(textId, x, y, panX, panY, flags)
    }

    override fun addDrawTweenPath(
        path1Id: Int,
        path2Id: Int,
        tween: Float,
        start: Float,
        stop: Float,
    ) {
        calls.add("addDrawTweenPath($path1Id, $path2Id, $tween, $start, $stop)")
        super.addDrawTweenPath(path1Id, path2Id, tween, start, stop)
    }

    override fun addPathData(id: Int, pathData: FloatArray): Int {
        calls.add("addPathData($id)")
        return super.addPathData(id, pathData)
    }

    override fun addPathData(id: Int, pathData: FloatArray, winding: Int): Int {
        calls.add("addPathData($id, $winding)")
        return super.addPathData(id, pathData, winding)
    }

    override fun definePattern(name: String, paramIds: IntArray): Int {
        calls.add("definePattern(\"$name\")")
        return super.definePattern(name, paramIds)
    }

    override fun definePattern(id: Int, paramIds: IntArray): Int {
        calls.add("definePattern($id)")
        return super.definePattern(id, paramIds)
    }

    override fun definePatternParameter(name: String): Int {
        calls.add("definePatternParameter(\"$name\")")
        return super.definePatternParameter(name)
    }

    override fun inflatePattern(id: Int, argIds: IntArray) {
        calls.add("inflatePattern($id)")
        super.inflatePattern(id, argIds)
    }

    override fun addPatternBlock(paramIndex: Int) {
        calls.add("addPatternBlock($paramIndex)")
        super.addPatternBlock(paramIndex)
    }

    override fun addPatternArgument(paramIndex: Int) {
        calls.add("addPatternArgument($paramIndex)")
        super.addPatternArgument(paramIndex)
    }

    override fun addPatternForEach(collectionId: Int, localItemId: Int) {
        calls.add("addPatternForEach($collectionId, $localItemId)")
        super.addPatternForEach(collectionId, localItemId)
    }

    override fun endPatternForEach() {
        calls.add("endPatternForEach")
        super.endPatternForEach()
    }

    override fun endPatternDefine() {
        calls.add("endPatternDefine")
        super.endPatternDefine()
    }

    override fun endPatternInflation() {
        calls.add("endPatternInflation")
        super.endPatternInflation()
    }

    override fun endPatternBlock() {
        calls.add("endPatternBlock")
        super.endPatternBlock()
    }

    override fun setTheme(theme: Int) {
        calls.add("setTheme($theme)")
        super.setTheme(theme)
    }

    override fun addMatrixSkew(skewX: Float, skewY: Float) {
        calls.add("addMatrixSkew(${format(skewX)}, ${format(skewY)})")
        super.addMatrixSkew(skewX, skewY)
    }

    override fun addMatrixRotate(angle: Float, centerX: Float, centerY: Float) {
        calls.add("addMatrixRotate($angle, ${format(centerX)}, ${format(centerY)})")
        super.addMatrixRotate(angle, centerX, centerY)
    }

    override fun addMatrixTranslate(dx: Float, dy: Float) {
        calls.add("addMatrixTranslate(${format(dx)}, ${format(dy)})")
        super.addMatrixTranslate(dx, dy)
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float) {
        calls.add("addMatrixScale(${format(scaleX)}, ${format(scaleY)})")
        super.addMatrixScale(scaleX, scaleY)
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        calls.add(
            "addMatrixScale(${format(scaleX)}, ${format(scaleY)}, ${format(centerX)}, ${format(centerY)})"
        )
        super.addMatrixScale(scaleX, scaleY, centerX, centerY)
    }

    override fun addClipPath(pathId: Int) {
        calls.add("addClipPath($pathId)")
        super.addClipPath(pathId)
    }

    override fun addFloat(id: Int, value: Float): Float {
        calls.add("addFloat($id, ${format(value)})")
        return super.addFloat(id, value)
    }

    override fun addInteger(id: Int, value: Int) {
        calls.add("addInteger($id, $value)")
        super.addInteger(id, value)
    }

    override fun addLong(id: Int, value: Long) {
        calls.add("addLong($id, $value)")
        super.addLong(id, value)
    }

    override fun addBoolean(id: Int, value: Boolean) {
        calls.add("addBoolean($id, $value)")
        super.addBoolean(id, value)
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
        calls.add("addTouchExpression($id)")
        super.addTouchExpression(
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
    }

    override fun textMeasure(id: Int, textId: Int, mode: Int) {
        calls.add("textMeasure($id, $textId, $mode)")
        super.textMeasure(id, textId, mode)
    }

    override fun addFloatArray(id: Int, values: FloatArray) {
        calls.add("addFloatArray($id, ${values.toList()})")
        super.addFloatArray(id, values)
    }

    override fun addDynamicFloatArray(id: Int, size: Float) {
        calls.add("addDynamicFloatArray($id, ${format(size)})")
        super.addDynamicFloatArray(id, size)
    }

    override fun setArrayValue(id: Int, index: Float, value: Float) {
        calls.add("setArrayValue($id, ${format(index)}, ${format(value)})")
        super.setArrayValue(id, index, value)
    }

    override fun addMap(id: Int, keys: Array<String>, types: ByteArray?, listId: IntArray) {
        calls.add("addMap($id)")
        super.addMap(id, keys, types, listId)
    }

    override fun idLookup(id: Int, dataSet: Float, index: Float) {
        calls.add("idLookup($id, ${format(dataSet)}, ${format(index)})")
        super.idLookup(id, dataSet, index)
    }

    override fun addColor(id: Int, color: Int) {
        calls.add("addColor($id, $color)")
        super.addColor(id, color)
    }

    override fun addColorExpression(id: Int, color1: Int, color2: Int, tween: Float) {
        calls.add("addColorExpression($id, $color1, $color2, ${format(tween)})")
        super.addColorExpression(id, color1, color2, tween)
    }

    override fun addColorExpression(id: Int, color1: Short, color2: Int, tween: Float) {
        calls.add("addColorExpression($id, $color1, $color2, ${format(tween)})")
        super.addColorExpression(id, color1, color2, tween)
    }

    override fun addColorExpression(id: Int, color1: Int, color2: Short, tween: Float) {
        calls.add("addColorExpression($id, $color1, $color2, ${format(tween)})")
        super.addColorExpression(id, color1, color2, tween)
    }

    override fun addColorExpression(id: Int, color1: Short, color2: Short, tween: Float) {
        calls.add("addColorExpression($id, $color1, $color2, ${format(tween)})")
        super.addColorExpression(id, color1, color2, tween)
    }

    override fun addColorExpression(id: Int, hue: Float, sat: Float, value: Float) {
        calls.add("addColorExpression($id, ${format(hue)}, ${format(sat)}, ${format(value)})")
        super.addColorExpression(id, hue, sat, value)
    }

    override fun addColorExpression(id: Int, alpha: Int, hue: Float, sat: Float, value: Float) {
        calls.add(
            "addColorExpression($id, $alpha, ${format(hue)}, ${format(sat)}, ${format(value)})"
        )
        super.addColorExpression(id, alpha, hue, sat, value)
    }

    override fun addColorExpression(id: Int, alpha: Float, red: Float, green: Float, blue: Float) {
        calls.add(
            "addColorExpression($id, ${format(alpha)}, ${format(red)}, ${format(green)}, ${format(blue)})"
        )
        super.addColorExpression(id, alpha, red, green, blue)
    }

    override fun setNamedVariable(id: Int, name: String, type: Int) {
        calls.add("setNamedVariable($id, \"$name\", $type)")
        super.setNamedVariable(id, name, type)
    }

    override fun addComponentStart(type: Int, id: Int) {
        calls.add("addComponentStart($type, $id)")
        super.addComponentStart(type, id)
    }

    override fun addComponentStart(type: Int) {
        calls.add("addComponentStart($type)")
        super.addComponentStart(type)
    }

    override fun addContainerEnd() {
        calls.add("addContainerEnd")
        super.addContainerEnd()
    }

    override fun addModifierScroll(direction: Int, max: Float) {
        calls.add("addModifierScroll($direction, ${format(max)})")
        super.addModifierScroll(direction, max)
    }

    override fun addModifierBackground(color: Int, shape: Int) {
        calls.add("addModifierBackground($color, $shape)")
        super.addModifierBackground(color, shape)
    }

    override fun addDynamicModifierBackground(colorId: Int, shape: Int) {
        calls.add("addDynamicModifierBackground($colorId, $shape)")
        super.addDynamicModifierBackground(colorId, shape)
    }

    override fun addModifierBackground(r: Float, g: Float, b: Float, a: Float, shape: Int) {
        calls.add(
            "addModifierBackground(${format(r)}, ${format(g)}, ${format(b)}, ${format(a)}, $shape)"
        )
        super.addModifierBackground(r, g, b, a, shape)
    }

    override fun addModifierAlignBy(line: Float) {
        calls.add("addModifierAlignBy(${format(line)})")
        super.addModifierAlignBy(line)
    }

    override fun addModifierBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        color: Int,
        shape: Int,
    ) {
        calls.add(
            "addModifierBorder(${format(borderWidth)}, ${format(borderRoundedCorner)}, $color, $shape)"
        )
        super.addModifierBorder(borderWidth, borderRoundedCorner, color, shape)
    }

    override fun addModifierDynamicBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        colorId: Int,
        shape: Int,
    ) {
        calls.add(
            "addModifierDynamicBorder(${format(borderWidth)}, ${format(borderRoundedCorner)}, $colorId, $shape)"
        )
        super.addModifierDynamicBorder(borderWidth, borderRoundedCorner, colorId, shape)
    }

    override fun addModifierPadding(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addModifierPadding(${format(left)}, ${format(top)}, ${format(right)}, ${format(bottom)})"
        )
        super.addModifierPadding(left, top, right, bottom)
    }

    override fun addModifierOffset(x: Float, y: Float) {
        calls.add("addModifierOffset(${format(x)}, ${format(y)})")
        super.addModifierOffset(x, y)
    }

    override fun addModifierZIndex(value: Float) {
        calls.add("addModifierZIndex(${format(value)})")
        super.addModifierZIndex(value)
    }

    override fun addModifierRipple() {
        calls.add("addModifierRipple")
        super.addModifierRipple()
    }

    override fun addModifierMarquee(
        iterations: Int,
        animationMode: Int,
        repeatDelayMillis: Float,
        initialDelayMillis: Float,
        spacing: Float,
        velocity: Float,
    ) {
        calls.add("addModifierMarquee($iterations)")
        super.addModifierMarquee(
            iterations,
            animationMode,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    }

    override fun addModifierGraphicsLayer(attributes: java.util.HashMap<Int, Any>) {
        calls.add("addModifierGraphicsLayer(...)")
        super.addModifierGraphicsLayer(attributes)
    }

    override fun addRoundClipRectModifier(
        topStart: Float,
        topEnd: Float,
        bottomStart: Float,
        bottomEnd: Float,
    ) {
        calls.add(
            "addRoundClipRectModifier(${format(topStart)}, ${format(topEnd)}, ${format(bottomStart)}, ${format(bottomEnd)})"
        )
        super.addRoundClipRectModifier(topStart, topEnd, bottomStart, bottomEnd)
    }

    override fun addClipRectModifier() {
        calls.add("addClipRectModifier")
        super.addClipRectModifier()
    }

    override fun addStateLayout(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        indexId: Int,
    ) {
        calls.add("addStateLayout($componentId, $animationId)")
        super.addStateLayout(componentId, animationId, horizontal, vertical, indexId)
    }

    override fun addBoxStart(componentId: Int, animationId: Int, horizontal: Int, vertical: Int) {
        calls.add("addBoxStart($componentId, $animationId)")
        super.addBoxStart(componentId, animationId, horizontal, vertical)
    }

    override fun addFitBoxStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
    ) {
        calls.add("addFitBoxStart($componentId, $animationId)")
        super.addFitBoxStart(componentId, animationId, horizontal, vertical)
    }

    override fun addImage(
        componentId: Int,
        animationId: Int,
        bitmapId: Int,
        scaleType: Int,
        alpha: Float,
    ) {
        calls.add("addImage($componentId, $bitmapId)")
        super.addImage(componentId, animationId, bitmapId, scaleType, alpha)
    }

    override fun addRowStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        calls.add("addRowStart($componentId, $animationId)")
        super.addRowStart(componentId, animationId, horizontal, vertical, spacedBy)
    }

    override fun addCollapsibleRowStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        calls.add("addCollapsibleRowStart($componentId, $animationId)")
        super.addCollapsibleRowStart(componentId, animationId, horizontal, vertical, spacedBy)
    }

    override fun addFlowStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
        maxItemsInEachRow: Int,
        maxLines: Int,
    ) {
        calls.add("addFlowStart($componentId, $animationId)")
        super.addFlowStart(
            componentId,
            animationId,
            horizontal,
            vertical,
            spacedBy,
            maxItemsInEachRow,
            maxLines,
        )
    }

    override fun addColumnStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        calls.add("addColumnStart($componentId, $animationId)")
        super.addColumnStart(componentId, animationId, horizontal, vertical, spacedBy)
    }

    override fun addCollapsibleColumnStart(
        componentId: Int,
        animationId: Int,
        horizontal: Int,
        vertical: Int,
        spacedBy: Float,
    ) {
        calls.add("addCollapsibleColumnStart($componentId, $animationId)")
        super.addCollapsibleColumnStart(componentId, animationId, horizontal, vertical, spacedBy)
    }

    override fun addCanvasStart(componentId: Int, animationId: Int) {
        calls.add("addCanvasStart($componentId, $animationId)")
        super.addCanvasStart(componentId, animationId)
    }

    override fun addCanvasContentStart(componentId: Int) {
        calls.add("addCanvasContentStart($componentId)")
        super.addCanvasContentStart(componentId)
    }

    override fun addRootStart() {
        calls.add("addRootStart")
        super.addRootStart()
    }

    override fun addContentStart() {
        calls.add("addContentStart")
        super.addContentStart()
    }

    override fun addCanvasOperationsStart() {
        calls.add("addCanvasOperationsStart")
        super.addCanvasOperationsStart()
    }

    override fun addRunActionsStart() {
        calls.add("addRunActionsStart")
        super.addRunActionsStart()
    }

    override fun addComponentValue(id: Int, type: Int) {
        calls.add("addComponentValue($id, $type)")
        super.addComponentValue(id, type)
    }

    override fun addComponentWidthValue(id: Int) {
        calls.add("addComponentWidthValue($id)")
        super.addComponentWidthValue(id)
    }

    override fun addComponentHeightValue(id: Int) {
        calls.add("addComponentHeightValue($id)")
        super.addComponentHeightValue(id)
    }

    override fun addComponentContentWidthValue(id: Int) {
        calls.add("addComponentContentWidthValue($id)")
        super.addComponentContentWidthValue(id)
    }

    override fun addComponentContentHeightValue(id: Int) {
        calls.add("addComponentContentHeightValue($id)")
        super.addComponentContentHeightValue(id)
    }

    override fun addComponentXValue(id: Int) {
        calls.add("addComponentXValue($id)")
        super.addComponentXValue(id)
    }

    override fun addComponentYValue(id: Int) {
        calls.add("addComponentYValue($id)")
        super.addComponentYValue(id)
    }

    override fun addComponentRootXValue(id: Int) {
        calls.add("addComponentRootXValue($id)")
        super.addComponentRootXValue(id)
    }

    override fun addComponentRootYValue(id: Int) {
        calls.add("addComponentRootYValue($id)")
        super.addComponentRootYValue(id)
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
        calls.add("addTextComponentStart($componentId, $textId)")
        super.addTextComponentStart(
            componentId,
            animationId,
            textId,
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamilyId,
            flags,
            textAlign,
            overflow,
            maxLines,
        )
    }

    override fun addTextComponentStart(
        componentId: Int,
        animationId: Int,
        textId: Int,
        textStyleId: Int,
        color: Int,
        colorId: Int,
        fontSize: Float,
        minFontSize: Float,
        maxFontSize: Float,
        fontStyle: Int,
        fontWeight: Float,
        fontFamilyId: Int,
        textAlign: Int,
        overflow: Int,
        maxLines: Int,
        letterSpacing: Float,
        lineHeightAdd: Float,
        lineHeightMultiplier: Float,
        lineBreakStrategy: Int,
        hyphenationFrequency: Int,
        justificationMode: Int,
        underline: Boolean,
        strikethrough: Boolean,
        fontAxis: IntArray?,
        fontAxisValues: FloatArray?,
        autosize: Boolean,
        flags: Int,
    ) {
        calls.add("addTextComponentStart($componentId, $textId)")
        super.addTextComponentStart(
            componentId,
            animationId,
            textId,
            textStyleId,
            color,
            colorId,
            fontSize,
            minFontSize,
            maxFontSize,
            fontStyle,
            fontWeight,
            fontFamilyId,
            textAlign,
            overflow,
            maxLines,
            letterSpacing,
            lineHeightAdd,
            lineHeightMultiplier,
            lineBreakStrategy,
            hyphenationFrequency,
            justificationMode,
            underline,
            strikethrough,
            fontAxis,
            fontAxisValues,
            autosize,
            flags,
        )
    }

    override fun addTextComponentStart(
        componentId: Int,
        animationId: Int,
        textId: Int,
        textStyleId: Int,
        flags: Int,
    ) {
        calls.add("addTextComponentStart($componentId, $textId, style=$textStyleId)")
        super.addTextComponentStart(componentId, animationId, textId, textStyleId, flags)
    }

    override fun addTextStyle(
        id: Int,
        color: Int?,
        colorId: Int?,
        fontSize: Float?,
        minFontSize: Float?,
        maxFontSize: Float?,
        fontStyle: Int?,
        fontWeight: Float?,
        fontFamilyId: Int?,
        textAlign: Int?,
        overflow: Int?,
        maxLines: Int?,
        letterSpacing: Float?,
        lineHeightAdd: Float?,
        lineHeightMultiplier: Float?,
        lineBreakStrategy: Int?,
        hyphenationFrequency: Int?,
        justificationMode: Int?,
        underline: Boolean?,
        strikethrough: Boolean?,
        fontAxis: IntArray?,
        fontAxisValues: FloatArray?,
        autosize: Boolean?,
        parentId: Int?,
    ) {
        calls.add("addTextStyle($id)")
        super.addTextStyle(
            id,
            color,
            colorId,
            fontSize,
            minFontSize,
            maxFontSize,
            fontStyle,
            fontWeight,
            fontFamilyId,
            textAlign,
            overflow,
            maxLines,
            letterSpacing,
            lineHeightAdd,
            lineHeightMultiplier,
            lineBreakStrategy,
            hyphenationFrequency,
            justificationMode,
            underline,
            strikethrough,
            fontAxis,
            fontAxisValues,
            autosize,
            parentId,
        )
    }

    override fun addImpulse(duration: Float, start: Float) {
        calls.add("addImpulse($duration, $start)")
        super.addImpulse(duration, start)
    }

    override fun addImpulseProcess() {
        calls.add("addImpulseProcess")
        super.addImpulseProcess()
    }

    override fun addImpulseEnd() {
        calls.add("addImpulseEnd")
        super.addImpulseEnd()
    }

    override fun addParticles(
        id: Int,
        varIds: IntArray,
        initialExpressions: Array<FloatArray>,
        particleCount: Int,
    ) {
        calls.add("addParticles($id, $particleCount)")
        super.addParticles(id, varIds, initialExpressions, particleCount)
    }

    override fun addParticlesLoop(id: Int, restart: FloatArray?, expressions: Array<FloatArray>) {
        calls.add("addParticlesLoop($id)")
        super.addParticlesLoop(id, restart, expressions)
    }

    override fun addParticlesComparison(
        id: Int,
        flags: Short,
        min: Float,
        max: Float,
        condition: FloatArray?,
        apply1: Array<FloatArray>?,
        apply2: Array<FloatArray>?,
    ) {
        calls.add("addParticlesComparison($id)")
        super.addParticlesComparison(id, flags, min, max, condition, apply1, apply2)
    }

    override fun addParticleLoopEnd() {
        calls.add("addParticleLoopEnd")
        super.addParticleLoopEnd()
    }

    override fun defineFloatFunction(fid: Int, args: IntArray) {
        calls.add("defineFloatFunction($fid)")
        super.defineFloatFunction(fid, args)
    }

    override fun addEndFloatFunctionDef() {
        calls.add("addEndFloatFunctionDef")
        super.addEndFloatFunctionDef()
    }

    override fun callFloatFunction(id: Int, args: FloatArray?) {
        calls.add("callFloatFunction($id)")
        super.callFloatFunction(id, args)
    }

    override fun bitmapAttribute(id: Int, bitmapId: Int, attribute: Short) {
        calls.add("bitmapAttribute($id, $bitmapId, $attribute)")
        super.bitmapAttribute(id, bitmapId, attribute)
    }

    override fun textAttribute(id: Int, textId: Int, attribute: Short) {
        calls.add("textAttribute($id, $textId, $attribute)")
        super.textAttribute(id, textId, attribute)
    }

    override fun timeAttribute(id: Int, timeId: Int, attribute: Short, vararg args: Int) {
        calls.add("timeAttribute($id, $timeId, $attribute)")
        super.timeAttribute(id, timeId, attribute, *args)
    }

    override fun drawComponentContent() {
        calls.add("drawComponentContent")
        super.drawComponentContent()
    }

    override fun storeBitmap(
        imageId: Int,
        imageWidth: Int,
        imageHeight: Int,
        data: ByteArray,
    ): Int {
        calls.add("storeBitmap($imageId)")
        return super.storeBitmap(imageId, imageWidth, imageHeight, data)
    }

    override fun createBitmap(imageId: Int, imageWidth: Short, imageHeight: Short): Int {
        calls.add("createBitmap($imageId)")
        return super.createBitmap(imageId, imageWidth, imageHeight)
    }

    override fun storeBitmapA8(
        imageId: Int,
        imageWidth: Int,
        imageHeight: Int,
        data: ByteArray,
    ): Int {
        calls.add("storeBitmapA8($imageId)")
        return super.storeBitmapA8(imageId, imageWidth, imageHeight, data)
    }

    override fun storeBitmapUrl(imageId: Int, url: String, width: Int, height: Int): Int {
        calls.add("storeBitmapUrl($imageId, \"$url\")")
        return super.storeBitmapUrl(imageId, url, width, height)
    }

    override fun pathCombine(id: Int, path1: Int, path2: Int, op: Byte) {
        calls.add("pathCombine($id, $path1, $path2, $op)")
        super.pathCombine(id, path1, path2, op)
    }

    override fun performHaptic(feedbackConstant: Int) {
        calls.add("performHaptic($feedbackConstant)")
        super.performHaptic(feedbackConstant)
    }

    override fun addDebugMessage(textId: Int, value: Float, flags: Int) {
        calls.add("addDebugMessage($textId, ${format(value)}, $flags)")
        super.addDebugMessage(textId, value, flags)
    }

    override fun getColorAttribute(id: Int, baseColor: Int, type: Short) {
        calls.add("getColorAttribute($id, $baseColor, $type)")
        super.getColorAttribute(id, baseColor, type)
    }

    override fun setMatrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {
        calls.add("setMatrixFromPath($pathId, ${format(fraction)}, ${format(vOffset)}, $flags)")
        super.setMatrixFromPath(pathId, fraction, vOffset, flags)
    }

    override fun bitmapTextMeasure(
        id: Int,
        textId: Int,
        bmFontId: Int,
        type: Int,
        glyphSpacing: Float,
    ) {
        calls.add("bitmapTextMeasure($id, $textId, $bmFontId)")
        super.bitmapTextMeasure(id, textId, bmFontId, type, glyphSpacing)
    }

    override fun rem(text: String) {
        calls.add("rem(\"$text\")")
        super.rem(text)
    }

    override fun beginSkip(type: Short, value: Int): Int {
        calls.add("beginSkip($type, $value)")
        return super.beginSkip(type, value)
    }

    override fun endSkip(offset: Int) {
        calls.add("endSkip($offset)")
        super.endSkip(offset)
    }

    override fun addMatrixConst(id: Int, values: FloatArray) {
        calls.add("addMatrixConst($id)")
        super.addMatrixConst(id, values)
    }

    override fun addMatrixExpression(id: Int, exp: FloatArray) {
        calls.add("addMatrixExpression($id)")
        super.addMatrixExpression(id, exp)
    }

    override fun addMatrixVectorMath(
        matrixId: Float,
        type: Short,
        from: FloatArray,
        outId: IntArray,
    ) {
        calls.add("addMatrixVectorMath(${format(matrixId)})")
        super.addMatrixVectorMath(matrixId, type, from, outId)
    }

    override fun addFont(id: Int, type: Int, data: ByteArray) {
        calls.add("addFont($id)")
        super.addFont(id, type, data)
    }

    override fun wakeIn(seconds: Float) {
        calls.add("wakeIn(${format(seconds)})")
        super.wakeIn(seconds)
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
        calls.add("addPathExpression($id)")
        super.addPathExpression(id, expressionX, expressionY, start, end, count, flags)
    }

    override fun addComponentVisibilityOperation(valueId: Int) {
        calls.add("addComponentVisibilityOperation($valueId)")
        super.addComponentVisibilityOperation(valueId)
    }

    override fun addWidthModifierOperation(type: Int, value: Float) {
        calls.add("addWidthModifierOperation($type, ${format(value)})")
        super.addWidthModifierOperation(type, value)
    }

    override fun addHeightModifierOperation(type: Int, value: Float) {
        calls.add("addHeightModifierOperation($type, ${format(value)})")
        super.addHeightModifierOperation(type, value)
    }

    override fun addHeightInModifierOperation(min: Float, max: Float) {
        calls.add("addHeightInModifierOperation(${format(min)}, ${format(max)})")
        super.addHeightInModifierOperation(min, max)
    }

    override fun addTouchDownModifierOperation() {
        calls.add("addTouchDownModifierOperation")
        super.addTouchDownModifierOperation()
    }

    override fun addTouchUpModifierOperation() {
        calls.add("addTouchUpModifierOperation")
        super.addTouchUpModifierOperation()
    }

    override fun addTouchCancelModifierOperation() {
        calls.add("addTouchCancelModifierOperation")
        super.addTouchCancelModifierOperation()
    }

    override fun addWidthInModifierOperation(min: Float, max: Float) {
        calls.add("addWidthInModifierOperation(${format(min)}, ${format(max)})")
        super.addWidthInModifierOperation(min, max)
    }

    override fun addDimensionConstraintsModifierOperation(type: Int, min: Float, max: Float) {
        calls.add("addDimensionConstraintsModifierOperation($type, ${format(min)}, ${format(max)})")
        super.addDimensionConstraintsModifierOperation(type, min, max)
    }

    override fun addDrawContentOperation() {
        calls.add("addDrawContentOperation")
        super.addDrawContentOperation()
    }

    override fun startLayoutCompute(type: Int, boundsId: Int, animateChanges: Boolean) {
        calls.add("startLayoutCompute($type, $boundsId)")
        super.startLayoutCompute(type, boundsId, animateChanges)
    }

    override fun endLayoutCompute() {
        calls.add("endLayoutCompute")
        super.endLayoutCompute()
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
        calls.add("addSemanticsModifier($contentDescriptionId)")
        super.addSemanticsModifier(
            contentDescriptionId,
            role,
            textId,
            stateDescriptionId,
            mode,
            enabled,
            clickable,
        )
    }

    override fun addClickModifierOperation(clickType: Int) {
        calls.add("addClickModifierOperation($clickType)")
        super.addClickModifierOperation(clickType)
    }

    override fun addClickModifierOperation() {
        calls.add("addClickModifierOperation")
        super.addClickModifierOperation()
    }

    override fun addCollapsiblePriorityModifier(orientation: Int, priority: Float) {
        calls.add("addCollapsiblePriorityModifier($orientation, ${format(priority)})")
        super.addCollapsiblePriorityModifier(orientation, priority)
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
        calls.add("addAnimationSpecModifier($animationId)")
        super.addAnimationSpecModifier(
            animationId,
            motionDuration,
            motionEasingType,
            visibilityDuration,
            visibilityEasingType,
            enterAnimation,
            exitAnimation,
        )
    }

    override fun addValueStringChangeActionOperation(destTextId: Int, srcTextId: Int) {
        calls.add("addValueStringChangeActionOperation($destTextId, $srcTextId)")
        super.addValueStringChangeActionOperation(destTextId, srcTextId)
    }

    override fun addValueIntegerExpressionChangeActionOperation(
        destIntegerId: Long,
        srcIntegerId: Long,
    ) {
        calls.add("addValueIntegerExpressionChangeActionOperation($destIntegerId, $srcIntegerId)")
        super.addValueIntegerExpressionChangeActionOperation(destIntegerId, srcIntegerId)
    }

    override fun addValueFloatChangeActionOperation(valueId: Int, value: Float) {
        calls.add("addValueFloatChangeActionOperation($valueId, ${format(value)})")
        super.addValueFloatChangeActionOperation(valueId, value)
    }

    override fun addValueIntegerChangeActionOperation(valueId: Int, value: Int) {
        calls.add("addValueIntegerChangeActionOperation($valueId, $value)")
        super.addValueIntegerChangeActionOperation(valueId, value)
    }

    override fun addValueFloatExpressionChangeActionOperation(mValueId: Int, mValue: Int) {
        calls.add("addValueFloatExpressionChangeActionOperation($mValueId, $mValue)")
        super.addValueFloatExpressionChangeActionOperation(mValueId, mValue)
    }

    override fun addReferencedOperations(id: Int) {
        calls.add("addReferencedOperations($id)")
        super.addReferencedOperations(id)
    }

    override fun addThemedColor(
        id: Int,
        groupId: Int,
        lightId: Short,
        darkId: Short,
        lightFallback: Int,
        darkFallback: Int,
    ) {
        calls.add("addThemedColor($id)")
        super.addThemedColor(id, groupId, lightId, darkId, lightFallback, darkFallback)
    }
}

internal class MyRemoteComposeWriterAndroid(
    profile: Profile,
    buffer: RemoteComposeBuffer,
    vararg tags: RemoteComposeWriter.HTag,
) : RemoteComposeWriterAndroid(profile, buffer, *tags)
