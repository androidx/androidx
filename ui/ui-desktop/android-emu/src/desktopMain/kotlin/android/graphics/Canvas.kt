/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

@Suppress("unused", "MemberVisibilityCanBePrivate")
public class Canvas(private val _skijaCanvas: org.jetbrains.skija.Canvas? = null) {
    enum class VertexMode {
        TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN
    }

    val skijaCanvas: org.jetbrains.skija.Canvas get() = requireNotNull(_skijaCanvas)

    var skijaFont = org.jetbrains.skija.Font(Typeface.DEFAULT.skijaTypeface, 30f)

    fun translate(x: Float, y: Float) {
        skijaCanvas.translate(x, y)
    }

    fun scale(sx: Float, sy: Float) {
        skijaCanvas.scale(sx, sy)
    }

    fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        translate(px, py)
        skijaCanvas.scale(sx, sy)
        translate(-px, -py)
    }

    fun rotate(degrees: Float) {
        skijaCanvas.rotate(degrees)
    }

    fun rotate(degrees: Float, px: Float, py: Float) {
        translate(px, py)
        skijaCanvas.rotate(degrees)
        translate(-px, -py)
    }

    fun skew(sx: Float, sy: Float) {
        skijaCanvas.skew(sx, sy)
    }

    fun concat(matrix: Matrix) {
        skijaCanvas.concat(matrix.skija)
    }

    fun drawLine(
        startX: Float,
        startY: Float,
        stopX: Float,
        stopY: Float,
        paint: Paint
    ) {
        skijaCanvas.drawLine(
            startX,
            startY,
            stopX,
            stopY,
            paint.skijaPaint
        )
    }

    fun drawBitmap(
        bitmap: Bitmap,
        left: Float,
        top: Float,
        paint: Paint
    ) {
        skijaCanvas.drawImage(bitmap.skiaImage, left, top, paint.skijaPaint)
    }

    fun drawBitmap(
        bitmap: Bitmap,
        src: Rect?,
        dst: RectF,
        paint: Paint
    ) {
        val skijaDst = dst.toSkia()
        val skijaSrc = src?.toSkia()
        skijaCanvas.drawImageRect(bitmap.skiaImage, skijaSrc, skijaDst, paint.skijaPaint)
    }

    fun drawBitmap(
        bitmap: Bitmap,
        src: Rect?,
        dst: Rect,
        paint: Paint
    ) {
        val skijaDst = dst.toSkia()
        val skijaSrc = src?.toSkia()
        skijaCanvas.drawImageRect(bitmap.skiaImage, skijaSrc, skijaDst, paint.skijaPaint)
    }

    fun drawBitmap(
        bitmap: Bitmap,
        matrix: Matrix,
        paint: Paint
    ) {
        save()
        concat(matrix)
        skijaCanvas.drawImage(bitmap.skiaImage, 0f, 0f, paint.skijaPaint)
        restore()
    }

    fun drawRect(rect: RectF, paint: Paint) {
        val skijaRect = rect.toSkia()
        skijaCanvas.drawRect(skijaRect, paint.skijaPaint)
    }

    fun drawRect(r: Rect, paint: Paint) {
        drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), paint)
    }

    fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ) {
        val skijaRect = org.jetbrains.skija.Rect.makeLTRB(left, top, right, bottom)
        skijaCanvas.drawRect(skijaRect, paint.skijaPaint)
    }

    fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        skijaCanvas.drawArc(
            left,
            top,
            right,
            bottom,
            startAngle,
            sweepAngle,
            useCenter,
            paint.skijaPaint
        )
    }

    fun drawArc(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        skijaCanvas.drawArc(
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            startAngle,
            sweepAngle,
            useCenter,
            paint.skijaPaint
        )
    }

    fun drawText(
        text: CharArray,
        index: Int,
        count: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawText(String(text, index, count), x, y, paint)
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawText(text, 0, text.length, x, y, paint)
    }

    fun drawText(
        text: String,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawText(text, start, end, x, y, paint)
    }

    fun drawText(
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        val typeface = paint.getTypeface() ?: Typeface.DEFAULT
        val skijaFont = org.jetbrains.skija.Font(typeface.skijaTypeface, paint.textSize)
        skijaCanvas.drawString(text.substring(start, end), x, y, skijaFont, paint.skijaPaint)
    }

    fun drawRoundRect(
        rect: RectF,
        rx: Float,
        ry: Float,
        paint: Paint
    ) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint)
    }

    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        paint: Paint
    ) {
        val skijaRoundedRect = org.jetbrains.skija.RRect.makeLTRB(left, top, right, bottom, rx, ry)
        skijaCanvas.drawRRect(skijaRoundedRect, paint.skijaPaint)
    }

    fun drawPath(path: Path, paint: Paint) {
        skijaCanvas.drawPath(path.skijaPath, paint.skijaPaint)
    }

    fun drawCircle(
        cx: Float,
        cy: Float,
        radius: Float,
        paint: Paint
    ) {
        skijaCanvas.drawCircle(cx, cy, radius, paint.skijaPaint)
    }

    fun clipRect(rect: Rect, op: Region.Op): Boolean {
        val skijaRect = rect.toSkia()
        skijaCanvas.clipRect(skijaRect, op.skija)
        return true
    }

    fun clipRect(rect: RectF): Boolean {
        val skijaRect = rect.toSkia()
        skijaCanvas.clipRect(skijaRect)
        return true
    }

    fun clipRect(rect: Rect): Boolean {
        val skijaRect = rect.toSkia()
        skijaCanvas.clipRect(skijaRect)
        return true
    }

    fun clipRect(left: Float, top: Float, right: Float, bottom: Float, op: Region.Op): Boolean {
        val skijaRect = org.jetbrains.skija.Rect.makeLTRB(left, top, right, bottom)
        skijaCanvas.clipRect(skijaRect, op.skija)
        return true
    }

    fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Boolean {
        val skijaRect = org.jetbrains.skija.Rect.makeLTRB(left, top, right, bottom)
        skijaCanvas.clipRect(skijaRect)
        return true
    }

    fun clipRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Boolean {
        return clipRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    fun clipPath(path: Path, op: Region.Op): Boolean {
        skijaCanvas.clipPath(path.skijaPath, op.skija)
        return true
    }

    fun clipPath(path: Path) {
        skijaCanvas.clipPath(path.skijaPath)
    }

    fun drawColor(color: Int) {
        skijaCanvas.drawPaint(org.jetbrains.skija.Paint().apply {
            setColor(color)
        })
    }

    fun drawOval(oval: RectF, paint: Paint) {
        drawOval(oval.left, oval.top, oval.right, oval.bottom, paint)
    }

    fun drawOval(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ) {
        skijaCanvas.drawOval(
            org.jetbrains.skija.Rect.makeLTRB(left, top, right, bottom),
            paint.skijaPaint
        )
    }

    fun drawPaint(paint: Paint) {
        skijaCanvas.drawPaint(paint.skijaPaint)
    }

    fun drawPoint(
        x: Float,
        y: Float,
        paint: Paint
    ) {
        skijaCanvas.drawPoint(x, y, paint.skijaPaint)
    }

    fun drawVertices(
        mode: VertexMode,
        vertexCount: Int,
        verts: FloatArray,
        vertOffset: Int,
        texs: FloatArray,
        texOffset: Int,
        colors: IntArray,
        colorOffset: Int,
        indices: ShortArray,
        indexOffset: Int,
        indexCount: Int,
        paint: Paint
    ) {
        println("Canvas.drawVertices not implemented yet")
    }

    fun save(): Int {
        return skijaCanvas.save()
    }

    fun restore() {
        skijaCanvas.restore()
    }

    @Suppress("UNUSED_PARAMETER")
    fun saveLayer(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
        saveFlags: Int
    ): Int {
        return skijaCanvas.saveLayer(left, top, right, bottom, paint.skijaPaint)
    }

    fun saveLayer(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ): Int {
        return skijaCanvas.saveLayer(left, top, right, bottom, paint.skijaPaint)
    }
}
