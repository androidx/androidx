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
class Path {
    enum class Op {
        DIFFERENCE, INTERSECT, UNION, XOR, REVERSE_DIFFERENCE
    }

    enum class FillType {
        WINDING, EVEN_ODD, INVERSE_WINDING, INVERSE_EVEN_ODD
    }

    enum class Direction(val skija: org.jetbrains.skija.PathDirection) {
        @JvmStatic
        CW(org.jetbrains.skija.PathDirection.CLOCKWISE),
        @JvmStatic
        CCW(org.jetbrains.skija.PathDirection.COUNTER_CLOCKWISE)
    }

    val skijaPath = org.jetbrains.skija.Path()

    var fillType: FillType = FillType.WINDING
        set(value) {
            field = value
            skijaPath.fillMode = value.toSkia()
        }

    fun moveTo(x: Float, y: Float) {
        skijaPath.moveTo(x, y)
    }

    fun rMoveTo(dx: Float, dy: Float) {
        skijaPath.rMoveTo(dx, dy)
    }

    fun lineTo(x: Float, y: Float) {
        skijaPath.lineTo(x, y)
    }

    fun rLineTo(dx: Float, dy: Float) {
        skijaPath.rLineTo(dx, dy)
    }

    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        skijaPath.quadTo(x1, y1, x2, y2)
    }

    fun rQuadTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        skijaPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        skijaPath.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    fun rCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
        skijaPath.rCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    fun arcTo(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean
    ) {
        skijaPath.arcTo(oval.toSkia(), startAngle, sweepAngle, forceMoveTo)
    }

    fun arcTo(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float
    ) {
        arcTo(oval, startAngle, sweepAngle, forceMoveTo = false)
    }

    fun arcTo(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean
    ) {
        arcTo(RectF(left, top, right, bottom), startAngle, sweepAngle, forceMoveTo)
    }

    fun addRect(rect: RectF, dir: Direction) {
        skijaPath.addRect(rect.toSkia(), dir.skija)
    }

    fun addRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        dir: Direction
    ) {
        addRect(RectF(left, top, right, bottom), dir)
    }

    fun addOval(oval: RectF, dir: Direction) {
        skijaPath.addOval(oval.toSkia(), dir.skija)
    }

    fun addOval(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        dir: Direction
    ) {
        addOval(RectF(left, top, right, bottom), dir)
    }

    fun addArc(oval: RectF, startAngle: Float, sweepAngle: Float) {
        skijaPath.addArc(oval.toSkia(), startAngle, sweepAngle)
    }

    fun addArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        addArc(RectF(left, top, right, bottom), startAngle, sweepAngle)
    }

    fun addRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        dir: Direction
    ) {
        skijaPath.addRRect(org.jetbrains.skija.RRect.makeLTRB(
            left, top, right, bottom, rx, ry), dir.skija)
    }

    fun addRoundRect(
        rect: RectF,
        radii: FloatArray,
        dir: Direction
    ) {
        skijaPath.addRRect(org.jetbrains.skija.RRect.makeComplexLTRB(
            rect.left, rect.top, rect.right, rect.bottom, radii), dir.skija)
    }

    fun addPath(src: Path, dx: Float, dy: Float) {
        skijaPath.addPath(src.skijaPath, dx, dy)
    }

    fun addPath(src: Path) {
        skijaPath.addPath(src.skijaPath)
    }

    fun addPath(src: Path, matrix: Matrix) {
        skijaPath.addPath(src.skijaPath, matrix.skija)
    }

    fun close() {
        skijaPath.closePath()
    }

    fun reset() {
        skijaPath.reset()
    }

    fun transform(matrix: Matrix, dst: Path?) {
        skijaPath.transform(matrix.skija, dst?.skijaPath)
    }

    fun transform(matrix: Matrix) {
        skijaPath.transform(matrix.skija)
    }

    @Suppress("UNUSED_PARAMETER")
    fun computeBounds(bounds: RectF, exact: Boolean) {
        val skijaBounds = skijaPath.bounds
        bounds.left = skijaBounds.left
        bounds.top = skijaBounds.top
        bounds.right = skijaBounds.right
        bounds.bottom = skijaBounds.bottom
    }

    fun op(path: Path, op: Op): Boolean {
        println("Canvas.op not implemented yet")
        return false
    }

    fun op(path1: Path, path2: Path, op: Op): Boolean {
        println("Canvas.op not implemented yet")
        return false
    }

    fun isConvex(): Boolean = skijaPath.isConvex

    fun isEmpty(): Boolean = skijaPath.isEmpty
}

internal fun Path.FillType.toSkia() = when (this) {
    Path.FillType.WINDING -> org.jetbrains.skija.PathFillMode.WINDING
    Path.FillType.EVEN_ODD -> org.jetbrains.skija.PathFillMode.EVEN_ODD
    Path.FillType.INVERSE_WINDING -> org.jetbrains.skija.PathFillMode.INVERSE_WINDING
    Path.FillType.INVERSE_EVEN_ODD -> org.jetbrains.skija.PathFillMode.INVERSE_EVEN_ODD
}
