/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.android

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.DrawFilter
import android.graphics.Matrix
import android.graphics.NinePatch
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.RenderNode
import android.graphics.fonts.Font
import android.graphics.text.MeasuredText
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * This is a wrapper around Android Canvas that we get from
 * androidx.compose.ui.graphics.Canvas#nativeCanvas. This implementation delegates all methods to
 * the original nativeCanvas apart from `getClipBounds(Rect)`
 */
@SuppressLint("ClassVerificationFailure")
@Suppress("DEPRECATION")
internal class TextAndroidCanvas : Canvas() {
    /**
     * Original canvas object to which this class delegates its method calls
     */
    private lateinit var nativeCanvas: Canvas

    fun setCanvas(canvas: Canvas) {
        nativeCanvas = canvas
    }

    /**
     * By overriding this methods we allow android.text.Layout to draw all lines that would
     * otherwise be cut due to Layout's internal drawing logic.
     */
    override fun getClipBounds(bounds: Rect): Boolean {
        val result = nativeCanvas.getClipBounds(bounds)
        if (result) {
            val currentWidth = bounds.width()
            bounds.set(0, 0, currentWidth, Int.MAX_VALUE)
        }
        return result
    }

    override fun setBitmap(bitmap: Bitmap?) {
        nativeCanvas.setBitmap(bitmap)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun enableZ() {
        nativeCanvas.enableZ()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun disableZ() {
        nativeCanvas.disableZ()
    }

    override fun isOpaque(): Boolean {
        return nativeCanvas.isOpaque
    }

    override fun getWidth(): Int {
        return nativeCanvas.width
    }

    override fun getHeight(): Int {
        return nativeCanvas.getHeight()
    }

    override fun getDensity(): Int {
        return nativeCanvas.density
    }
    override fun setDensity(density: Int) {
        nativeCanvas.density = density
    }

    override fun getMaximumBitmapWidth(): Int {
        return nativeCanvas.maximumBitmapWidth
    }

    override fun getMaximumBitmapHeight(): Int {
        return nativeCanvas.maximumBitmapHeight
    }

    override fun save(): Int {
        return nativeCanvas.save()
    }

    @Deprecated("Deprecated in Java")
    override fun saveLayer(bounds: RectF?, paint: Paint?, saveFlags: Int): Int {
        return nativeCanvas.saveLayer(bounds, paint, saveFlags)
    }

    override fun saveLayer(bounds: RectF?, paint: Paint?): Int {
        return nativeCanvas.saveLayer(bounds, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun saveLayer(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint?,
        saveFlags: Int
    ): Int {
        return nativeCanvas.saveLayer(left, top, right, bottom, paint, saveFlags)
    }

    override fun saveLayer(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint?
    ): Int {
        return nativeCanvas.saveLayer(left, top, right, bottom, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun saveLayerAlpha(bounds: RectF?, alpha: Int, saveFlags: Int): Int {
        return nativeCanvas.saveLayerAlpha(bounds, alpha, saveFlags)
    }

    override fun saveLayerAlpha(bounds: RectF?, alpha: Int): Int {
        return nativeCanvas.saveLayerAlpha(bounds, alpha)
    }

    @Deprecated("Deprecated in Java")
    override fun saveLayerAlpha(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        alpha: Int,
        saveFlags: Int
    ): Int {
        return nativeCanvas.saveLayerAlpha(left, top, right, bottom, alpha, saveFlags)
    }

    override fun saveLayerAlpha(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        alpha: Int
    ): Int {
        return nativeCanvas.saveLayerAlpha(left, top, right, bottom, alpha)
    }

    override fun restore() {
        nativeCanvas.restore()
    }

    override fun getSaveCount(): Int {
        return nativeCanvas.saveCount
    }

    override fun restoreToCount(saveCount: Int) {
        nativeCanvas.restoreToCount(saveCount)
    }

    override fun translate(dx: Float, dy: Float) {
        nativeCanvas.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        nativeCanvas.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        nativeCanvas.rotate(degrees)
    }

    override fun skew(sx: Float, sy: Float) {
        nativeCanvas.skew(sx, sy)
    }

    override fun concat(matrix: Matrix?) {
        nativeCanvas.concat(matrix)
    }

    override fun setMatrix(matrix: Matrix?) {
        nativeCanvas.setMatrix(matrix)
    }

    @Deprecated("Deprecated in Java")
    override fun getMatrix(ctm: Matrix) {
        nativeCanvas.getMatrix(ctm)
    }

    @Deprecated("Deprecated in Java")
    override fun clipRect(rect: RectF, op: Region.Op): Boolean {
        return nativeCanvas.clipRect(rect, op)
    }

    @Deprecated("Deprecated in Java")
    override fun clipRect(rect: Rect, op: Region.Op): Boolean {
        return nativeCanvas.clipRect(rect, op)
    }

    override fun clipRect(rect: RectF): Boolean {
        return nativeCanvas.clipRect(rect)
    }

    override fun clipRect(rect: Rect): Boolean {
        return nativeCanvas.clipRect(rect)
    }

    @Deprecated("Deprecated in Java")
    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        op: Region.Op
    ): Boolean {
        return nativeCanvas.clipRect(left, top, right, bottom, op)
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        return nativeCanvas.clipRect(left, top, right, bottom)
    }

    override fun clipRect(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return nativeCanvas.clipRect(left, top, right, bottom)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun clipOutRect(rect: RectF): Boolean {
        return nativeCanvas.clipOutRect(rect)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun clipOutRect(rect: Rect): Boolean {
        return nativeCanvas.clipOutRect(rect)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun clipOutRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        return nativeCanvas.clipOutRect(left, top, right, bottom)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun clipOutRect(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return nativeCanvas.clipOutRect(left, top, right, bottom)
    }

    @Deprecated("Deprecated in Java")
    override fun clipPath(path: Path, op: Region.Op): Boolean {
        return nativeCanvas.clipPath(path, op)
    }

    override fun clipPath(path: Path): Boolean {
        return nativeCanvas.clipPath(path)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun clipOutPath(path: Path): Boolean {
        return nativeCanvas.clipOutPath(path)
    }

    override fun getDrawFilter() = nativeCanvas.drawFilter

    override fun setDrawFilter(filter: DrawFilter?) {
        nativeCanvas.drawFilter = filter
    }

    @Deprecated("Deprecated in Java")
    override fun quickReject(rect: RectF, type: EdgeType): Boolean {
        return nativeCanvas.quickReject(rect, type)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun quickReject(rect: RectF): Boolean {
        return nativeCanvas.quickReject(rect)
    }

    @Deprecated("Deprecated in Java")
    override fun quickReject(path: Path, type: EdgeType): Boolean {
        return nativeCanvas.quickReject(path, type)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun quickReject(path: Path): Boolean {
        return nativeCanvas.quickReject(path)
    }

    @Deprecated("Deprecated in Java")
    override fun quickReject(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        type: EdgeType
    ): Boolean {
        return nativeCanvas.quickReject(left, top, right, bottom, type)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun quickReject(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        return nativeCanvas.quickReject(left, top, right, bottom)
    }

    override fun drawPicture(picture: Picture) {
        nativeCanvas.drawPicture(picture)
    }

    override fun drawPicture(picture: Picture, dst: RectF) {
        nativeCanvas.drawPicture(picture, dst)
    }

    override fun drawPicture(picture: Picture, dst: Rect) {
        nativeCanvas.drawPicture(picture, dst)
    }

    override fun drawArc(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        nativeCanvas.drawArc(oval, startAngle, sweepAngle, useCenter, paint)
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        nativeCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint)
    }

    override fun drawARGB(a: Int, r: Int, g: Int, b: Int) {
        nativeCanvas.drawARGB(a, r, g, b)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        nativeCanvas.drawBitmap(bitmap, left, top, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        nativeCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        nativeCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun drawBitmap(
        colors: IntArray,
        offset: Int,
        stride: Int,
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        hasAlpha: Boolean,
        paint: Paint?
    ) {
        nativeCanvas.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun drawBitmap(
        colors: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        hasAlpha: Boolean,
        paint: Paint?
    ) {
        nativeCanvas.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, matrix: Matrix, paint: Paint?) {
        nativeCanvas.drawBitmap(bitmap, matrix, paint)
    }

    override fun drawBitmapMesh(
        bitmap: Bitmap,
        meshWidth: Int,
        meshHeight: Int,
        verts: FloatArray,
        vertOffset: Int,
        colors: IntArray?,
        colorOffset: Int,
        paint: Paint?
    ) {
        nativeCanvas.drawBitmapMesh(
            bitmap,
            meshWidth,
            meshHeight,
            verts,
            vertOffset,
            colors,
            colorOffset,
            paint
        )
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        nativeCanvas.drawCircle(cx, cy, radius, paint)
    }

    override fun drawColor(color: Int) {
        nativeCanvas.drawColor(color)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawColor(color: Long) {
        nativeCanvas.drawColor(color)
    }

    override fun drawColor(color: Int, mode: PorterDuff.Mode) {
        nativeCanvas.drawColor(color, mode)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawColor(color: Int, mode: BlendMode) {
        nativeCanvas.drawColor(color, mode)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawColor(color: Long, mode: BlendMode) {
        nativeCanvas.drawColor(color, mode)
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        nativeCanvas.drawLine(startX, startY, stopX, stopY, paint)
    }

    override fun drawLines(pts: FloatArray, offset: Int, count: Int, paint: Paint) {
        nativeCanvas.drawLines(pts, offset, count, paint)
    }

    override fun drawLines(pts: FloatArray, paint: Paint) {
        nativeCanvas.drawLines(pts, paint)
    }

    override fun drawOval(oval: RectF, paint: Paint) {
        nativeCanvas.drawOval(oval, paint)
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        nativeCanvas.drawOval(left, top, right, bottom, paint)
    }

    override fun drawPaint(paint: Paint) {
        nativeCanvas.drawPaint(paint)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun drawPatch(patch: NinePatch, dst: Rect, paint: Paint?) {
        nativeCanvas.drawPatch(patch, dst, paint)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun drawPatch(patch: NinePatch, dst: RectF, paint: Paint?) {
        nativeCanvas.drawPatch(patch, dst, paint)
    }

    override fun drawPath(path: Path, paint: Paint) {
        nativeCanvas.drawPath(path, paint)
    }

    override fun drawPoint(x: Float, y: Float, paint: Paint) {
        nativeCanvas.drawPoint(x, y, paint)
    }

    override fun drawPoints(pts: FloatArray?, offset: Int, count: Int, paint: Paint) {
        nativeCanvas.drawPoints(pts, offset, count, paint)
    }

    override fun drawPoints(pts: FloatArray, paint: Paint) {
        nativeCanvas.drawPoints(pts, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun drawPosText(
        text: CharArray,
        index: Int,
        count: Int,
        pos: FloatArray,
        paint: Paint
    ) {
        nativeCanvas.drawPosText(text, index, count, pos, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun drawPosText(text: String, pos: FloatArray, paint: Paint) {
        nativeCanvas.drawPosText(text, pos, paint)
    }

    override fun drawRect(rect: RectF, paint: Paint) {
        nativeCanvas.drawRect(rect, paint)
    }

    override fun drawRect(r: Rect, paint: Paint) {
        nativeCanvas.drawRect(r, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        nativeCanvas.drawRect(left, top, right, bottom, paint)
    }

    override fun drawRGB(r: Int, g: Int, b: Int) {
        nativeCanvas.drawRGB(r, g, b)
    }

    override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
        nativeCanvas.drawRoundRect(rect, rx, ry, paint)
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        paint: Paint
    ) {
        nativeCanvas.drawRoundRect(left, top, right, bottom, rx, ry, paint)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawDoubleRoundRect(
        outer: RectF,
        outerRx: Float,
        outerRy: Float,
        inner: RectF,
        innerRx: Float,
        innerRy: Float,
        paint: Paint
    ) {
        nativeCanvas.drawDoubleRoundRect(outer, outerRx, outerRy, inner, innerRx, innerRy, paint)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawDoubleRoundRect(
        outer: RectF,
        outerRadii: FloatArray,
        inner: RectF,
        innerRadii: FloatArray,
        paint: Paint
    ) {
        nativeCanvas.drawDoubleRoundRect(outer, outerRadii, inner, innerRadii, paint)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun drawGlyphs(
        glyphIds: IntArray,
        glyphIdOffset: Int,
        positions: FloatArray,
        positionOffset: Int,
        glyphCount: Int,
        font: Font,
        paint: Paint
    ) {
        nativeCanvas.drawGlyphs(
            glyphIds,
            glyphIdOffset,
            positions,
            positionOffset,
            glyphCount,
            font,
            paint
        )
    }

    override fun drawText(
        text: CharArray,
        index: Int,
        count: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        nativeCanvas.drawText(text, index, count, x, y, paint)
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        nativeCanvas.drawText(text, x, y, paint)
    }

    override fun drawText(text: String, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
        nativeCanvas.drawText(text, start, end, x, y, paint)
    }

    override fun drawText(
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        nativeCanvas.drawText(text, start, end, x, y, paint)
    }

    override fun drawTextOnPath(
        text: CharArray,
        index: Int,
        count: Int,
        path: Path,
        hOffset: Float,
        vOffset: Float,
        paint: Paint
    ) {
        nativeCanvas.drawTextOnPath(text, index, count, path, hOffset, vOffset, paint)
    }

    override fun drawTextOnPath(
        text: String,
        path: Path,
        hOffset: Float,
        vOffset: Float,
        paint: Paint
    ) {
        nativeCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun drawTextRun(
        text: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint
    ) {
        nativeCanvas.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun drawTextRun(
        text: CharSequence,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint
    ) {
        nativeCanvas.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawTextRun(
        text: MeasuredText,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint
    ) {
        nativeCanvas.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint)
    }

    override fun drawVertices(
        mode: VertexMode,
        vertexCount: Int,
        verts: FloatArray,
        vertOffset: Int,
        texs: FloatArray?,
        texOffset: Int,
        colors: IntArray?,
        colorOffset: Int,
        indices: ShortArray?,
        indexOffset: Int,
        indexCount: Int,
        paint: Paint
    ) {
        nativeCanvas.drawVertices(
            mode,
            vertexCount,
            verts,
            vertOffset,
            texs,
            texOffset,
            colors,
            colorOffset,
            indices,
            indexOffset,
            indexCount,
            paint
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun drawRenderNode(renderNode: RenderNode) {
        nativeCanvas.drawRenderNode(renderNode)
    }
}
