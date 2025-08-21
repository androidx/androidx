/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation

import androidx.compose.remote.core.Platform
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile

/** Kotlin API to create a new RemoteCompose byte array */
public open class RemoteComposeContext {

    public constructor(writer: RemoteComposeWriter) {
        mRemoteWriter = writer
    }

    public constructor(width: Int, height: Int, contentDescription: String, profile: Profile) {
        mRemoteWriter = RemoteComposeWriter.obtain(width, height, contentDescription, profile)
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        profile: Profile,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter = RemoteComposeWriter.obtain(width, height, contentDescription, profile)
        content()
    }

    public constructor(width: Int, height: Int, contentDescription: String, platform: Platform) {
        mRemoteWriter = RemoteComposeWriter(width, height, contentDescription, platform)
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        platform: Platform,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter = RemoteComposeWriter(width, height, contentDescription, platform)
        content()
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        apiLevel: Int,
        profiles: Int,
        platform: Platform,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter =
            RemoteComposeWriter(width, height, contentDescription, apiLevel, profiles, platform)
        content()
    }

    public constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        apiLevel: Int,
        profiles: Int,
        platform: Platform,
    ) {
        mRemoteWriter =
            RemoteComposeWriter(width, height, contentDescription, apiLevel, profiles, platform)
    }

    public constructor(
        vararg tags: RemoteComposeWriter.HTag,
        platform: Platform,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter = RemoteComposeWriter(platform, *tags)
    }

    /** Create a new matrix expression. */
    public fun matrix(vararg exp: Float): Matrix {
        return Matrix(this, *exp)
    }

    public val buffer: RemoteComposeBuffer
        get() = mRemoteWriter.buffer

    public val writer: RemoteComposeWriter
        get() = mRemoteWriter

    public val TIME_IN_SEC: Float = RemoteContext.FLOAT_CONTINUOUS_SEC
    public val FONT_TYPE_DEFAULT: Int = PaintBundle.FONT_TYPE_DEFAULT
    public val FONT_TYPE_SANS_SERIF: Int = PaintBundle.FONT_TYPE_SANS_SERIF
    public val FONT_TYPE_SERIF: Int = PaintBundle.FONT_TYPE_SERIF
    public val FONT_TYPE_MONOSPACE: Int = PaintBundle.FONT_TYPE_MONOSPACE

    public fun column(
        modifier: RecordingModifier = Modifier,
        horizontal: Int = ColumnLayout.START,
        vertical: Int = ColumnLayout.TOP,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter.column(modifier, horizontal, vertical) { content() }
    }

    public fun row(
        modifier: RecordingModifier = Modifier,
        horizontal: Int = RowLayout.START,
        vertical: Int = RowLayout.TOP,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter.row(modifier, horizontal, vertical) { content() }
    }

    public fun box(
        modifier: RecordingModifier = Modifier,
        horizontal: Int = BoxLayout.START,
        vertical: Int = BoxLayout.TOP,
        content: RemoteComposeContext.() -> Unit,
    ) {
        mRemoteWriter.box(modifier, horizontal, vertical) { content() }
    }

    public val Modifier: RecordingModifier
        get() = RecordingModifier()

    public lateinit var mRemoteWriter: RemoteComposeWriter

    public fun areFloatExpressionOperationsValid(f: Float): Boolean {
        return mRemoteWriter.areFloatExpressionOperationsValid(f)
    }

    public fun validateOps(ops: FloatArray) {
        mRemoteWriter.validateOps(ops)
    }

    public fun reset() {
        mRemoteWriter.reset()
    }

    public fun header(
        width: Int,
        height: Int,
        contentDescription: String?,
        density: Float,
        capabilities: Long,
    ) {
        mRemoteWriter.header(width, height, contentDescription, density, capabilities)
    }

    public fun pathCombine(path1: Int, path2: Int, op: Byte): Int {
        return mRemoteWriter.pathCombine(path1, path2, op)
    }

    public fun performHaptic(feedbackConstant: Int) {
        mRemoteWriter.performHaptic(feedbackConstant)
    }

    public fun getColorAttribute(baseColor: Int, type: Short): Float {
        return mRemoteWriter.getColorAttribute(baseColor, type)
    }

    public fun addAction(vararg actions: Action?) {
        mRemoteWriter.addAction(*actions)
    }

    public fun textSubtext(txtId: Int, start: Float, len: Float): Int {
        return mRemoteWriter.textSubtext(txtId, start, len)
    }

    public fun bitmapTextMeasure(textId: Int, bmFontId: Int, measureWidth: Int): Float {
        return mRemoteWriter.bitmapTextMeasure(textId, bmFontId, measureWidth)
    }

    public fun MatrixMultiply(matrixId: Float, from: FloatArray?, out: FloatArray?) {
        mRemoteWriter.addMatrixMultiply(matrixId, from, out)
    }

    public fun MatrixMultiply(matrixId: Float, type: Short, from: FloatArray?, out: FloatArray) {
        mRemoteWriter.addMatrixMultiply(matrixId, type, from, out)
    }

    public fun checkAndClearForceSendingNewPaint(): Boolean {
        return mRemoteWriter.checkAndClearForceSendingNewPaint()
    }

    public fun buffer(): ByteArray {
        return mRemoteWriter.buffer()
    }

    public fun bufferSize(): Int {
        return mRemoteWriter.bufferSize()
    }

    public fun createShader(shaderString: String): RemoteComposeShader {
        return mRemoteWriter.createShader(shaderString)
    }

    public fun setTheme(theme: Int) {
        mRemoteWriter.setTheme(theme)
    }

    public fun drawBitmap(image: Object, width: Int, height: Int, contentDescription: String) {
        mRemoteWriter.drawBitmap(image, width, height, contentDescription)
    }

    public fun setRootContentBehavior(scroll: Int, alignment: Int, sizing: Int, mode: Int) {
        mRemoteWriter.setRootContentBehavior(scroll, alignment, sizing, mode)
    }

    public fun addClickArea(
        id: Int,
        contentDescription: String?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadata: String?,
    ) {
        mRemoteWriter.addClickArea(id, contentDescription, left, top, right, bottom, metadata)
    }

    public fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        mRemoteWriter.drawArc(left, top, right, bottom, startAngle, sweepAngle)
    }

    public fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        mRemoteWriter.drawSector(left, top, right, bottom, startAngle, sweepAngle)
    }

    public fun drawBitmap(
        image: Any,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        contentDescription: String?,
    ) {
        mRemoteWriter.drawBitmap(image, left, top, right, bottom, contentDescription)
    }

    public fun drawBitmap(
        imageId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        contentDescription: String?,
    ) {
        mRemoteWriter.drawBitmap(imageId, left, top, right, bottom, contentDescription)
    }

    public fun drawBitmap(imageId: Int, left: Float, top: Float, contentDescription: String?) {
        mRemoteWriter.drawBitmap(imageId, left, top, contentDescription)
    }

    public fun drawScaledBitmap(
        image: Object,
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
        contentDescription: String?,
    ) {
        mRemoteWriter.drawScaledBitmap(
            image,
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
            contentDescription,
        )
    }

    public fun drawScaledBitmap(
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
        contentDescription: String?,
    ) {
        mRemoteWriter.drawScaledBitmap(
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
            contentDescription,
        )
    }

    public fun drawCircle(centerX: Float, centerY: Float, radius: Float) {
        mRemoteWriter.drawCircle(centerX, centerY, radius)
    }

    public fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        mRemoteWriter.drawLine(x1, y1, x2, y2)
    }

    public fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {
        mRemoteWriter.drawOval(left, top, right, bottom)
    }

    public fun drawPath(path: Any) {
        mRemoteWriter.drawPath(path)
    }

    public fun drawPath(pathId: Int) {
        mRemoteWriter.drawPath(pathId)
    }

    public fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
        mRemoteWriter.drawRect(left, top, right, bottom)
    }

    public fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {
        mRemoteWriter.drawRoundRect(left, top, right, bottom, radiusX, radiusY)
    }

    public fun textCreateId(text: String): Int {
        return mRemoteWriter.textCreateId(text)
    }

    public fun textMerge(id1: Int, id2: Int): Int {
        return mRemoteWriter.textMerge(id1, id2)
    }

    public fun drawTextOnPath(text: String, path: Any, hOffset: Float, vOffset: Float) {
        mRemoteWriter.drawTextOnPath(text, path, hOffset, vOffset)
    }

    public fun drawTextOnPath(textId: Int, path: Any, hOffset: Float, vOffset: Float) {
        mRemoteWriter.drawTextOnPath(textId, path, hOffset, vOffset)
    }

    public fun drawTextRun(
        text: String,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        mRemoteWriter.drawTextRun(text, start, end, contextStart, contextEnd, x, y, rtl)
    }

    public fun drawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        mRemoteWriter.drawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl)
    }

    public fun drawBitmapFontTextRun(
        textId: Int,
        bitmapFontId: Int,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
    ) {
        mRemoteWriter.drawBitmapFontTextRun(textId, bitmapFontId, start, end, x, y)
    }

    public fun drawTextAnchored(
        str: String,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int,
    ) {
        mRemoteWriter.drawTextAnchored(str, x, y, panX, panY, flags)
    }

    public fun drawTextAnchored(
        strId: Int,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int,
    ) {
        mRemoteWriter.drawTextAnchored(strId, x, y, panX, panY, flags)
    }

    public fun drawTweenPath(path1: Any, path2: Any, tween: Float, start: Float, stop: Float) {
        mRemoteWriter.drawTweenPath(path1, path2, tween, start, stop)
    }

    public fun drawBitmapTextAnchored(
        text: String,
        bitmapFontId: Int,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
    ) {
        mRemoteWriter.drawBitmapTextAnchored(text, bitmapFontId, start, end, x, y, panX, panY)
    }

    public fun drawBitmapTextAnchored(
        textId: Int,
        bitmapFontId: Int,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
    ) {
        mRemoteWriter.drawBitmapTextAnchored(textId, bitmapFontId, start, end, x, y, panX, panY)
    }

    public fun drawTweenPath(path1Id: Int, path2Id: Int, tween: Float, start: Float, stop: Float) {
        mRemoteWriter.drawTweenPath(path1Id, path2Id, tween, start, stop)
    }

    public fun addPathData(path: Any): Int {
        return mRemoteWriter.addPathData(path)
    }

    public fun pathTween(pid1: Int, pid2: Int, tween: Float): Int {
        return mRemoteWriter.pathTween(pid1, pid2, tween)
    }

    public fun pathCreate(x: Float, y: Float): Int {
        return mRemoteWriter.pathCreate(x, y)
    }

    public fun pathAppend(pathId: Int, vararg path: Float) {
        mRemoteWriter.pathAppend(pathId, *path)
    }

    public fun pathAppendLineTo(pathId: Int, x: Float, y: Float) {
        mRemoteWriter.pathAppendLineTo(pathId, x, y)
    }

    public fun pathAppendQuadTo(pathId: Int, x1: Float, y1: Float, x2: Float, y2: Float) {
        mRemoteWriter.pathAppendQuadTo(pathId, x1, y1, x2, y2)
    }

    public fun pathAppendMoveTo(pathId: Int, x: Float, y: Float) {
        mRemoteWriter.pathAppendMoveTo(pathId, x, y)
    }

    public fun pathAppendClose(pathId: Int) {
        mRemoteWriter.pathAppendClose(pathId)
    }

    public fun pathAppendReset(pathId: Int) {
        mRemoteWriter.pathAppendReset(pathId)
    }

    public fun addPathString(path: String): Int {
        return mRemoteWriter.addPathString(path)
    }

    //    public fun parsePath(pathData: String): Path {
    //        return RemoteComposeWriter.parsePath(pathData)
    //    }

    public fun skew(skewX: Float, skewY: Float) {
        mRemoteWriter.skew(skewX, skewY)
    }

    public fun rotate(angle: Float, centerX: Float, centerY: Float) {
        mRemoteWriter.rotate(angle, centerX, centerY)
    }

    public fun rotate(angle: Float) {
        mRemoteWriter.rotate(angle)
    }

    public fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {
        mRemoteWriter.matrixFromPath(pathId, fraction, vOffset, flags)
    }

    public fun save() {
        mRemoteWriter.save()
    }

    public fun restore() {
        mRemoteWriter.restore()
    }

    public fun translate(dx: Float, dy: Float) {
        mRemoteWriter.translate(dx, dy)
    }

    public fun scale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        mRemoteWriter.scale(scaleX, scaleY, centerX, centerY)
    }

    public fun scale(scaleX: Float, scaleY: Float) {
        mRemoteWriter.scale(scaleX, scaleY)
    }

    public fun addClipPath(pathId: Int) {
        mRemoteWriter.addClipPath(pathId)
    }

    public fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        mRemoteWriter.clipRect(left, top, right, bottom)
    }

    public fun addFloatConstant(value: Float): Float {
        return mRemoteWriter.addFloatConstant(value)
    }

    public fun reserveFloatVariable(): Float {
        return mRemoteWriter.reserveFloatVariable()
    }

    public fun addComponentWidthValue(): Float {
        return mRemoteWriter.addComponentWidthValue()
    }

    public fun addComponentHeightValue(): Float {
        return mRemoteWriter.addComponentHeightValue()
    }

    public fun addColor(color: Int): Int {
        return mRemoteWriter.addColor(color)
    }

    public fun addNamedColor(name: String, color: Int): Int {
        return mRemoteWriter.addNamedColor(name, color)
    }

    public fun setColorName(id: Int, name: String) {
        mRemoteWriter.setColorName(id, name)
    }

    public fun setStringName(id: Int, name: String) {
        mRemoteWriter.setStringName(id, name)
    }

    public fun addNamedString(name: String, initialValue: String): Int {
        return mRemoteWriter.addNamedString(name, initialValue)
    }

    public fun addNamedInt(name: String, initialValue: Int): Long {
        return mRemoteWriter.addNamedInt(name, initialValue)
    }

    public fun addNamedFloat(name: String, initialValue: Float): Float {
        return mRemoteWriter.addNamedFloat(name, initialValue)
    }

    public fun addNamedBitmap(name: String, initialValue: Object): Int {
        return mRemoteWriter.addNamedBitmap(name, initialValue)
    }

    public fun addNamedLong(name: String, initialValue: Long): Int {
        return mRemoteWriter.addNamedLong(name, initialValue)
    }

    public fun addColorExpression(color1: Int, color2: Int, tween: Float): Short {
        return mRemoteWriter.addColorExpression(color1, color2, tween)
    }

    public fun addColorExpression(colorId1: Short, color2: Int, tween: Float): Short {
        return mRemoteWriter.addColorExpression(colorId1, color2, tween)
    }

    public fun addColorExpression(color1: Int, colorId2: Short, tween: Float): Short {
        return mRemoteWriter.addColorExpression(color1, colorId2, tween)
    }

    public fun addColorExpression(colorId1: Short, colorId2: Short, tween: Float): Short {
        return mRemoteWriter.addColorExpression(colorId1, colorId2, tween)
    }

    public fun addColorExpression(hue: Float, sat: Float, value: Float): Short {
        return mRemoteWriter.addColorExpression(hue, sat, value)
    }

    public fun addColorExpression(alpha: Int, hue: Float, sat: Float, value: Float): Short {
        return mRemoteWriter.addColorExpression(alpha, hue, sat, value)
    }

    public fun addColorExpression(alpha: Float, red: Float, green: Float, blue: Float): Short {
        return mRemoteWriter.addColorExpression(alpha, red, green, blue)
    }

    public fun floatExpression(vararg value: Float): Float {
        return mRemoteWriter.floatExpression(*value)
    }

    public fun floatExpression(value: FloatArray, animation: FloatArray): Float {
        return mRemoteWriter.floatExpression(value, animation)
    }

    public fun addInteger(value: Int): Long {
        return mRemoteWriter.addInteger(value)
    }

    public fun addLong(value: Long): Int {
        return mRemoteWriter.addLong(value)
    }

    public fun addBoolean(value: Boolean): Int {
        return mRemoteWriter.addBoolean(value)
    }

    public fun mapLookup(mapId: Int, str: String): Int {
        return mRemoteWriter.mapLookup(mapId, str)
    }

    public fun mapLookup(mapId: Int, strId: Int): Int {
        return mRemoteWriter.mapLookup(mapId, strId)
    }

    public fun addText(text: String): Int {
        return mRemoteWriter.addText(text)
    }

    public fun textMeasure(textId: Int, mode: Int): Float {
        return mRemoteWriter.textMeasure(textId, mode)
    }

    public fun textLength(textId: Int): Float {
        return mRemoteWriter.textLength(textId)
    }

    public fun easing(maxTime: Float, maxAcceleration: Float, maxVelocity: Float): FloatArray {
        return mRemoteWriter.easing(maxTime, maxAcceleration, maxVelocity)
    }

    public fun addTouch(
        defValue: Float,
        min: Float,
        max: Float,
        touchMode: Int,
        velocityId: Float,
        touchEffects: Int,
        touchSpec: FloatArray?,
        easingSpec: FloatArray?,
        vararg exp: Float,
    ): Float {
        return mRemoteWriter.addTouch(
            defValue,
            min,
            max,
            touchMode,
            velocityId,
            touchEffects,
            touchSpec,
            easingSpec,
            *exp,
        )
    }

    public fun spring(
        stiffness: Float,
        damping: Float,
        stopThreshold: Float,
        boundaryMode: Int,
    ): FloatArray {
        return mRemoteWriter.spring(stiffness, damping, stopThreshold, boundaryMode)
    }

    public fun bitmapAttribute(bitmapId: Int, attribute: Short): Float {
        return mRemoteWriter.bitmapAttribute(bitmapId, attribute)
    }

    public fun textAttribute(textId: Int, attribute: Short): Float {
        return mRemoteWriter.textAttribute(textId, attribute)
    }

    //    public fun map(name: String?, value: Float): DataMap {
    //        return RemoteComposeWriter.map(name, value)
    //    }
    //
    //    public fun map(name: String?, value: Int): DataMap {
    //        return RemoteComposeWriter.map(name, value)
    //    }
    //
    //    public fun map(name: String?, value: Long): DataMap {
    //        return RemoteComposeWriter.map(name, value)
    //    }
    //
    //    public fun map(name: String?, value: String?): DataMap {
    //        return RemoteComposeWriter.map(name, value)
    //    }
    //
    //    public fun map(name: String?, value: Boolean): DataMap {
    //        return RemoteComposeWriter.map(name, value)
    //    }

    public fun addDataMap(vararg data: RemoteComposeWriter.DataMap?): Int {
        return mRemoteWriter.addDataMap(*data)
    }

    public fun addDataMap(keys: Array<String>, ids: IntArray): Float {
        return mRemoteWriter.addDataMap(keys, ids)
    }

    public fun integerExpression(mask: Int, vararg value: Int): Long {
        return mRemoteWriter.integerExpression(mask, *value)
    }

    public fun integerExpression(vararg v: Long): Long {
        return mRemoteWriter.integerExpression(*v)
    }

    public fun asFloatId(id: Long): Float {
        return mRemoteWriter.asFloatId(id)
    }

    public fun timeAttribute(longID: Int, type: Short, vararg args: Int): Float {
        return mRemoteWriter.timeAttribute(longID, type, *args)
    }

    public fun exp(vararg value: Float): FloatArray {
        return mRemoteWriter.exp(*value)
    }

    public fun anim(
        duration: Float,
        type: Int,
        spec: FloatArray?,
        initialValue: Float,
        wrap: Float,
    ): FloatArray {
        return mRemoteWriter.anim(duration, type, spec, initialValue, wrap)
    }

    public fun anim(
        duration: Float,
        type: Int,
        spec: FloatArray?,
        initialValue: Float,
    ): FloatArray {
        return mRemoteWriter.anim(duration, type, spec, initialValue)
    }

    public fun anim(duration: Float, type: Int, spec: FloatArray?): FloatArray {
        return mRemoteWriter.anim(duration, type, spec)
    }

    public fun anim(duration: Float, type: Int): FloatArray {
        return mRemoteWriter.anim(duration, type)
    }

    public fun anim(duration: Float): FloatArray {
        return mRemoteWriter.anim(duration)
    }

    public fun textLookup(arrayId: Float, index: Float): Int {
        return mRemoteWriter.textLookup(arrayId, index)
    }

    public fun textLookup(arrayId: Float, indexId: Int): Int {
        return mRemoteWriter.textLookup(arrayId, indexId)
    }

    public fun createTextFromFloat(value: Float, before: Int, after: Int, flags: Int): Int {
        return mRemoteWriter.createTextFromFloat(value, before, after, flags)
    }

    public fun createID(type: Int): Int {
        return mRemoteWriter.createID(type)
    }

    public fun nextId(): Int {
        return mRemoteWriter.nextId()
    }

    public fun root(content: RemoteComposeWriterInterface) {
        mRemoteWriter.root(content)
    }

    public fun startLoop(indexId: Int, from: Float, step: Float, until: Float) {
        mRemoteWriter.startLoop(indexId, from, step, until)
    }

    public fun startLoopVar(from: Float, step: Float, until: Float): Float {
        return mRemoteWriter.startLoopVar(from, step, until)
    }

    public fun startLoop(count: Float): Float {
        return mRemoteWriter.startLoop(count)
    }

    public fun endLoop() {
        mRemoteWriter.endLoop()
    }

    public fun loop(
        indexId: Int,
        from: Float,
        step: Float,
        until: Float,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.loop(indexId, from, step, until, content)
    }

    public fun loop(
        indexId: Int,
        from: Int,
        step: Int,
        until: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.loop(indexId, from, step, until, content)
    }

    public fun conditionalOperations(
        type: Byte,
        a: Float,
        b: Float,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.conditionalOperations(type, a, b, content)
    }

    public fun conditionalOperations(type: Byte, a: Float, b: Float) {
        mRemoteWriter.conditionalOperations(type, a, b)
    }

    public fun endConditionalOperations() {
        mRemoteWriter.endConditionalOperations()
    }

    public fun column(
        modifier: RecordingModifier,
        horizontal: Int,
        vertical: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.column(modifier, horizontal, vertical, content)
    }

    public fun startColumn(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.startColumn(modifier, horizontal, vertical)
    }

    public fun endColumn() {
        mRemoteWriter.endColumn()
    }

    public fun collapsibleColumn(
        modifier: RecordingModifier,
        horizontal: Int,
        vertical: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.collapsibleColumn(modifier, horizontal, vertical, content)
    }

    public fun startCollapsibleColumn(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.startCollapsibleColumn(modifier, horizontal, vertical)
    }

    public fun endCollapsibleColumn() {
        mRemoteWriter.endCollapsibleColumn()
    }

    public fun row(
        modifier: RecordingModifier,
        horizontal: Int,
        vertical: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.row(modifier, horizontal, vertical, content)
    }

    public fun startRow(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.startRow(modifier, horizontal, vertical)
    }

    public fun endRow() {
        mRemoteWriter.endRow()
    }

    public fun collapsibleRow(
        modifier: RecordingModifier,
        horizontal: Int,
        vertical: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.collapsibleRow(modifier, horizontal, vertical, content)
    }

    public fun startCollapsibleRow(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.startCollapsibleRow(modifier, horizontal, vertical)
    }

    public fun endCollapsibleRow() {
        mRemoteWriter.endCollapsibleRow()
    }

    public fun canvas(modifier: RecordingModifier, content: RemoteComposeWriterInterface) {
        mRemoteWriter.canvas(modifier, content)
    }

    public fun drawComponentContent() {
        mRemoteWriter.drawComponentContent()
    }

    public fun startCanvas(modifier: RecordingModifier) {
        mRemoteWriter.startCanvas(modifier)
    }

    public fun endCanvas() {
        mRemoteWriter.endCanvas()
    }

    public fun startCanvasOperations() {
        mRemoteWriter.startCanvasOperations()
    }

    public fun endCanvasOperations() {
        mRemoteWriter.endCanvasOperations()
    }

    public fun startRunActions() {
        mRemoteWriter.startRunActions()
    }

    public fun endRunActions() {
        mRemoteWriter.endRunActions()
    }

    public fun box(
        modifier: RecordingModifier,
        horizontal: Int,
        vertical: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.box(modifier, horizontal, vertical, content)
    }

    public fun startBox(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.startBox(modifier, horizontal, vertical)
    }

    public fun startBox(modifier: RecordingModifier) {
        mRemoteWriter.startBox(modifier)
    }

    public fun endBox() {
        mRemoteWriter.endBox()
    }

    public fun startFitBox(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.startFitBox(modifier, horizontal, vertical)
    }

    public fun endFitBox() {
        mRemoteWriter.endFitBox()
    }

    public fun image(modifier: RecordingModifier, imageId: Int, scaleType: Int, alpha: Float) {
        mRemoteWriter.image(modifier, imageId, scaleType, alpha)
    }

    public fun stateLayout(
        modifier: RecordingModifier,
        indexId: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.stateLayout(modifier, indexId, content)
    }

    public fun startStateLayout(modifier: RecordingModifier, indexId: Int) {
        mRemoteWriter.startStateLayout(modifier, indexId)
    }

    public fun endStateLayout() {
        mRemoteWriter.endStateLayout()
    }

    public fun addModifierScroll(direction: Int, positionId: Float) {
        mRemoteWriter.addModifierScroll(direction, positionId)
    }

    public fun addModifierScroll(direction: Int, positionId: Float, notches: Int) {
        mRemoteWriter.addModifierScroll(direction, positionId, notches)
    }

    public fun addModifierScroll(direction: Int) {
        mRemoteWriter.addModifierScroll(direction)
    }

    public fun textComponent(
        modifier: RecordingModifier,
        textId: Int,
        color: Int,
        fontSize: Float,
        fontStyle: Int,
        fontWeight: Float,
        fontFamily: String?,
        textAlign: Int,
        overflow: Int,
        maxLines: Int,
        content: RemoteComposeWriterInterface,
    ) {
        mRemoteWriter.textComponent(
            modifier,
            textId,
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            textAlign,
            overflow,
            maxLines,
            content,
        )
    }

    public fun startTextComponent(
        modifier: RecordingModifier,
        textId: Int,
        color: Int,
        fontSize: Float,
        fontStyle: Int,
        fontWeight: Float,
        fontFamily: String?,
        textAlign: Int,
        overflow: Int,
        maxLines: Int,
    ) {
        mRemoteWriter.startTextComponent(
            modifier,
            textId,
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            textAlign,
            overflow,
            maxLines,
        )
    }

    public fun endTextComponent() {
        mRemoteWriter.endTextComponent()
    }

    public fun box(modifier: RecordingModifier, horizontal: Int, vertical: Int) {
        mRemoteWriter.box(modifier, horizontal, vertical)
    }

    public fun box(modifier: RecordingModifier) {
        mRemoteWriter.box(modifier)
    }

    public fun addStringList(vararg strs: String?): Float {
        return mRemoteWriter.addStringList(*strs)
    }

    public fun addStringList(vararg strIds: Int): Float {
        return mRemoteWriter.addStringList(*strIds)
    }

    public fun addList(listId: IntArray): Float {
        return mRemoteWriter.addList(listId)
    }

    public fun addFloatArray(values: FloatArray): Float {
        return mRemoteWriter.addFloatArray(values)
    }

    public fun addFloatList(values: FloatArray): Float {
        return mRemoteWriter.addFloatList(values)
    }

    public fun addFloatMap(keys: Array<String>, values: FloatArray): Float {
        return mRemoteWriter.addFloatMap(keys, values)
    }

    public fun storeBitmap(image: Any): Int {
        return mRemoteWriter.storeBitmap(image)
    }

    public fun addBitmap(image: Object): Int {
        return mRemoteWriter.addBitmap(image)
    }

    public fun addBitmap(image: Object, name: String): Int {
        return mRemoteWriter.addBitmap(image, name)
    }

    public fun addBitmapFont(glyphs: Array<BitmapFontData.Glyph>): Int {
        return mRemoteWriter.addBitmapFont(glyphs)
    }

    public fun nameBitmapId(id: Int, omicron: String) {
        mRemoteWriter.nameBitmapId(id, omicron)
    }

    public fun createFloatId(): Float {
        return mRemoteWriter.createFloatId()
    }

    public fun impulse(duration: Float, start: Float) {
        mRemoteWriter.impulse(duration, start)
    }

    public fun impulse(duration: Float, start: Float, run: Runnable) {
        mRemoteWriter.impulse(duration, start, run)
    }

    public fun impulseProcess(run: Runnable) {
        mRemoteWriter.impulseProcess(run)
    }

    public fun impulseProcess() {
        mRemoteWriter.impulseProcess()
    }

    public fun impulseEnd() {
        mRemoteWriter.impulseEnd()
    }

    public fun createParticles(
        variables: FloatArray,
        initialExpressions: Array<FloatArray>,
        particleCount: Int,
    ): Float {
        return mRemoteWriter.createParticles(variables, initialExpressions, particleCount)
    }

    public fun particlesLoop(
        id: Float,
        restart: FloatArray?,
        expressions: Array<FloatArray>,
        r: Runnable,
    ) {
        mRemoteWriter.particlesLoop(id, restart, expressions, r)
    }

    public fun createFloatFunction(args: FloatArray): Int {
        return mRemoteWriter.createFloatFunction(args)
    }

    public fun endFloatFunction() {
        mRemoteWriter.endFloatFunction()
    }

    public fun callFloatFunction(id: Int, vararg args: Float) {
        mRemoteWriter.callFloatFunction(id, *args)
    }

    public fun addTimeLong(time: Long): Int {
        return mRemoteWriter.addTimeLong(time)
    }

    public fun addDebugMessage(message: String) {
        mRemoteWriter.addDebugMessage(message)
    }

    public fun addDebugMessage(message: String, value: Float) {
        mRemoteWriter.addDebugMessage(message, value)
    }

    public fun addDebugMessage(message: String, value: Float, flag: Int) {
        mRemoteWriter.addDebugMessage(message, value, flag)
    }

    public fun addDebugMessage(textId: Int, value: Float, flag: Int) {
        mRemoteWriter.addDebugMessage(textId, value, flag)
    }

    public fun matrixExpression(vararg exp: Float): Float {
        return mRemoteWriter.matrixExpression(*exp)
    }

    public fun addFont(data: ByteArray): Int {
        return mRemoteWriter.addFont(data)
    }

    /** hours run from Midnight=0 quantized to Hours 0-23 */
    public fun Hour(): RFloat {
        return mRemoteWriter.Hour()
    }

    /** minutes run from Midnight=0 quantized to minutes 0..1439 */
    public fun Minutes(): RFloat {
        return mRemoteWriter.Minutes()
    }

    /** seconds run from Midnight=0 quantized to seconds hour 0..3599 */
    public fun Seconds(): RFloat {
        return mRemoteWriter.Seconds()
    }

    /** CONTINUOUS_SEC is seconds from midnight looping every hour 0-3600 */
    public fun ContinuousSec(): RFloat {
        return mRemoteWriter.ContinuousSec()
    }

    /** ID_OFFSET_TO_UTC is the offset from UTC in sec (typically / 3600f) */
    public fun UtcOffset(): RFloat {
        return mRemoteWriter.UtcOffset()
    }

    /** DAY OF THE WEEK 1-7. 1 = Monday */
    public fun DayOfWeek(): RFloat {
        return mRemoteWriter.DayOfWeek()
    }

    /** Moth of Year quantized to MONTHS 1-12. 1 = January */
    public fun Month(): RFloat {
        return mRemoteWriter.Month()
    }

    /** DAY OF THE MONTH 1-31 */
    public fun DayOfMonth(): RFloat {
        return mRemoteWriter.DayOfMonth()
    }

    /** Width */
    public fun ComponentWidth(): RFloat {
        return mRemoteWriter.ComponentWidth()
    }

    /** Height */
    public fun ComponentHeight(): RFloat {
        return mRemoteWriter.ComponentHeight()
    }

    /** generate random number */
    public fun rand(): RFloat {
        return mRemoteWriter.rand()
    }

    /** the index variable in the particle system */
    public fun index(): RFloat {
        return mRemoteWriter.index()
    }

    /** The time in seconds relative to animation 0 at start of running */
    public fun animationTime(): RFloat {
        return mRemoteWriter.animationTime()
    }

    /** The time in seconds relative to animation 0 at start of running */
    public fun deltTime(): RFloat {
        return mRemoteWriter.deltTime()
    }

    public fun rf(vararg elements: Float): RFloat {
        return mRemoteWriter.rf(*elements)
    }

    public fun rf(v: Number): RFloat {
        return mRemoteWriter.rf(v)
    }
}

public fun RemoteComposeWriter.particlesLoops(
    id: Float,
    restart: RFloat?,
    rexpressions: Array<Number>,
    r: Runnable,
) {
    val expressions: Array<FloatArray> =
        Array(rexpressions.size) {
            val v = rexpressions[it]
            if (v is RFloat) v.array else floatArrayOf(v.toFloat())
        }
    this.particlesLoop(id, restart?.array, expressions, r)
}

public fun RemoteComposeWriter.createParticles(
    rvariables: Array<RFloat>,
    initialize: Array<Number>,
    particleCount: Int,
): Float {
    val initializeArray: Array<FloatArray> =
        Array(initialize.size) {
            val v = initialize[it]
            if (v is RFloat) v.array else floatArrayOf(v.toFloat())
        }
    val variables: FloatArray = FloatArray(rvariables.size)
    val ret = this.createParticles(variables, initializeArray, particleCount)
    for (i in rvariables.indices) {
        rvariables[i] = RFloat(this, variables[i])
    }
    return ret
}

operator public fun <RFloat> Array<RFloat>.component6() = this[5]

operator public fun <RFloat> Array<RFloat>.component7() = this[6]

/*
// This is useful to create an approximate circle using remote float
public fun RemoteComposeContext.createCirclePath(cx: Float, cy: Float, radius: Float): RemotePath {
    return createCirclePath(
        RFloat(mRemoteWriter, cx),
        RFloat(mRemoteWriter, cy),
        RFloat(mRemoteWriter, radius),
    )
}

public fun RemoteComposeContext.createCirclePath(
    cx: RFloat,
    cy: RFloat,
    radius: RFloat,
): RemotePath {
    val C = 0.5522847498f
    val c = radius * C
    val path = RemotePath()
    path.moveTo(cx + radius, cy)
    path.cubicTo(cx + radius, cy - c, cx + c, cy - radius, cx, cy - radius)
    path.cubicTo(cx - c, cy - radius, cx - radius, cy - c, cx - radius, cy)
    path.cubicTo(cx - radius, cy + c, cx - c, cy + radius, cx, cy + radius)
    path.cubicTo(cx + c, cy + radius, cx + radius, cy + c, cx + radius, cy)
    path.close()
    return path
}
*/
