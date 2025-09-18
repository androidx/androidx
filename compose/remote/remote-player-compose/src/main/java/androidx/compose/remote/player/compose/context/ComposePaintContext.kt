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
package androidx.compose.remote.player.compose.context

import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.Platform
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.utils.FloatsToPath
import androidx.compose.remote.player.compose.utils.copy
import androidx.compose.remote.player.view.platform.AndroidComputedTextLayout
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [PaintContext] implementation for [androidx.compose.remote.player.compose.RemoteComposePlayer].
 */
internal class ComposePaintContext(
    remoteContext: ComposeRemoteContext,
    private var canvas: Canvas,
) : PaintContext(remoteContext) {

    var paint = Paint()
    var paintList: MutableList<Paint> = mutableListOf()
    var tmpRect: Rect = Rect() // use in calculation of bounds
    var node: RenderNode? = null
    var mainCanvas: Canvas? = null
    var previousCanvas: Canvas? = null
    var canvasCache: MutableMap<Bitmap, Canvas> = mutableMapOf()
    private var cachedFontMetrics: android.graphics.Paint.FontMetrics? = null
    private val cachedPaintChanges =
        ComposePaintChanges(remoteContext = remoteContext, getPaint = { this.paint })

    override fun drawBitmap(
        imageId: Int,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        cdId: Int,
    ) {
        val androidContext = mContext as ComposeRemoteContext
        if (androidContext.mRemoteComposeState.containsId(imageId)) {
            val bitmap = androidContext.mRemoteComposeState.getFromId(imageId) as Bitmap?
            bitmap?.let {
                nativeCanvas()
                    .drawBitmap(
                        bitmap,
                        Rect(srcLeft, srcTop, srcRight, srcBottom),
                        Rect(dstLeft, dstTop, dstRight, dstBottom),
                        paint.asFrameworkPaint(),
                    )
            }
        }
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        canvas.scale(scaleX, scaleY)
    }

    override fun translate(translateX: Float, translateY: Float) {
        canvas.translate(translateX, translateY)
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, false, paint)
    }

    override fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, true, paint)
    }

    override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {
        val androidContext = mContext as ComposeRemoteContext
        if (androidContext.mRemoteComposeState.containsId(id)) {
            val bitmap = androidContext.mRemoteComposeState.getFromId(id) as Bitmap?
            val src = Rect(0, 0, bitmap!!.getWidth(), bitmap.getHeight())
            val dst = RectF(left, top, right, bottom)
            nativeCanvas().drawBitmap(bitmap, src, dst, paint.asFrameworkPaint())
        }
    }

    override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {
        canvas.drawCircle(Offset(centerX, centerY), radius, paint)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        canvas.drawLine(Offset(x1, y1), Offset(x2, y2), paint)
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawOval(left, top, right, bottom, paint)
    }

    override fun drawPath(id: Int, start: Float, end: Float) {
        canvas.drawPath(getPath(id, start, end), paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawRect(left, top, right, bottom, paint)
    }

    override fun savePaint() {
        paintList.add(paint.copy())
    }

    override fun restorePaint() {
        paint = paintList.removeAt(paintList.size - 1)
    }

    override fun replacePaint(paint: PaintBundle) {
        this.paint.asFrameworkPaint().reset()
        applyPaint(paint)
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {
        canvas.drawRoundRect(left, top, right, bottom, radiusX, radiusY, paint)
    }

    override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {
        nativeCanvas()
            .drawTextOnPath(
                getText(textId)!!,
                getNativePath(pathId, 0f, 1f),
                hOffset,
                vOffset,
                paint.asFrameworkPaint(),
            )
    }

    override fun getTextBounds(textId: Int, start: Int, end: Int, flags: Int, bounds: FloatArray) {
        val str = getText(textId)

        val endSanitized =
            if (end == -1 || end > str!!.length) {
                str!!.length
            } else end

        val paint = paint.asFrameworkPaint()
        if (cachedFontMetrics == null) {
            cachedFontMetrics = paint.getFontMetrics()
        }
        paint.getFontMetrics(cachedFontMetrics)
        paint.getTextBounds(str, start, endSanitized, tmpRect)
        if ((flags and TEXT_MEASURE_SPACES) != 0) {
            bounds[0] = 0f
            bounds[2] = paint.measureText(str, start, endSanitized)
        } else {
            bounds[0] = tmpRect.left.toFloat()
            if ((flags and TEXT_MEASURE_MONOSPACE_WIDTH) != 0) {
                bounds[2] = paint.measureText(str, start, endSanitized) - tmpRect.left
            } else {
                bounds[2] = tmpRect.right.toFloat()
            }
        }

        if ((flags and TEXT_MEASURE_FONT_HEIGHT) != 0) {
            bounds[1] = cachedFontMetrics!!.ascent.roundToInt().toFloat()
            bounds[3] = cachedFontMetrics!!.descent.roundToInt().toFloat()
        } else {
            bounds[1] = tmpRect.top.toFloat()
            bounds[3] = tmpRect.bottom.toFloat()
        }
    }

    override fun layoutComplexText(
        textId: Int,
        start: Int,
        end: Int,
        alignment: Int,
        overflow: Int,
        maxLines: Int,
        maxWidth: Float,
        flags: Int,
    ): Platform.ComputedTextLayout? {
        val str = getText(textId)
        if (str == null) {
            return null
        }

        val endSanitized =
            if (end == -1 || end > str.length) {
                str.length
            } else end

        val textPaint = TextPaint()
        textPaint.set(paint.asFrameworkPaint())
        val staticLayoutBuilder =
            StaticLayout.Builder.obtain(str, start, endSanitized, textPaint, maxWidth.toInt())
        when (alignment) {
            TextLayout.TEXT_ALIGN_RIGHT,
            TextLayout.TEXT_ALIGN_END ->
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            TextLayout.TEXT_ALIGN_CENTER ->
                staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_CENTER)
            else -> staticLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_NORMAL)
        }
        when (overflow) {
            TextLayout.OVERFLOW_ELLIPSIS ->
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.END)
            TextLayout.OVERFLOW_MIDDLE_ELLIPSIS ->
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.MIDDLE)
            TextLayout.OVERFLOW_START_ELLIPSIS ->
                staticLayoutBuilder.setEllipsize(TextUtils.TruncateAt.START)
            else -> {}
        }
        staticLayoutBuilder.setMaxLines(maxLines)
        staticLayoutBuilder.setIncludePad(false)

        val staticLayout = staticLayoutBuilder.build()
        return AndroidComputedTextLayout(
            staticLayout,
            staticLayout.width.toFloat(),
            staticLayout.height.toFloat(),
        )
    }

    override fun drawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        var textToPaint = getText(textId)
        if (textToPaint == null) {
            return
        }
        if (end == -1) {
            if (start != 0) {
                textToPaint = textToPaint.substring(start)
            }
        } else if (end > textToPaint.length) {
            textToPaint = textToPaint.substring(start)
        } else {
            textToPaint = textToPaint.substring(start, end)
        }

        nativeCanvas().drawText(textToPaint, x, y, paint.asFrameworkPaint())
    }

    override fun drawComplexText(computedTextLayout: Platform.ComputedTextLayout?) {
        if (computedTextLayout == null) {
            return
        }
        val staticLayout = (computedTextLayout as AndroidComputedTextLayout).get()
        staticLayout.draw(nativeCanvas())
    }

    override fun drawTweenPath(
        path1Id: Int,
        path2Id: Int,
        tween: Float,
        start: Float,
        stop: Float,
    ) {
        canvas.drawPath(getPath(path1Id, path2Id, tween, start, stop), paint)
    }

    override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {
        val p: FloatArray = getPathArray(path1, path2, tween)
        val androidContext = mContext as ComposeRemoteContext
        androidContext.mRemoteComposeState.putPathData(out, p)
    }

    override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {
        val p1 = getPath(path1, 0f, 1f)
        val p2 = getPath(path2, 0f, 1f)
        val op =
            arrayOf(
                PathOperation.Difference,
                PathOperation.Intersect,
                PathOperation.ReverseDifference,
                PathOperation.Union,
                PathOperation.Xor,
            )
        val p = Path.combine(op[operation.toInt()], p1, p2)

        val androidContext = mContext as ComposeRemoteContext
        androidContext.mRemoteComposeState.putPath(out, p)
    }

    override fun applyPaint(paintData: PaintBundle) {
        paintData.applyPaintChange(this, cachedPaintChanges)
    }

    override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        if (centerX.isNaN()) {
            canvas.scale(scaleX, scaleY)
        } else {
            nativeCanvas().scale(scaleX, scaleY, centerX, centerY)
        }
    }

    override fun matrixTranslate(translateX: Float, translateY: Float) {
        canvas.translate(translateX, translateY)
    }

    override fun matrixSkew(skewX: Float, skewY: Float) {
        canvas.skew(skewX, skewY)
    }

    override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {
        if (pivotX.isNaN()) {
            canvas.rotate(rotate)
        } else {
            nativeCanvas().rotate(rotate, pivotX, pivotY)
        }
    }

    override fun matrixSave() {
        canvas.save()
    }

    override fun matrixRestore() {
        canvas.restore()
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        canvas.clipRect(left, top, right, bottom)
    }

    override fun clipPath(pathId: Int, regionOp: Int) {
        val path = getPath(pathId, 0f, 1f)
        if (regionOp == ClipPath.DIFFERENCE) {
            canvas.clipPath(path, ClipOp.Difference)
        } else {
            canvas.clipPath(path, ClipOp.Intersect)
        }
    }

    override fun roundedClipRect(
        width: Float,
        height: Float,
        topStart: Float,
        topEnd: Float,
        bottomStart: Float,
        bottomEnd: Float,
    ) {
        val roundedPath = Path()
        val roundRect =
            RoundRect(
                left = 0f,
                top = 0f,
                right = width,
                bottom = height,
                topLeftCornerRadius = CornerRadius(topStart, topStart),
                topRightCornerRadius = CornerRadius(topEnd, topEnd),
                bottomRightCornerRadius = CornerRadius(bottomEnd, bottomEnd),
                bottomLeftCornerRadius = CornerRadius(bottomStart, bottomStart),
            )
        roundedPath.addRoundRect(roundRect)
        canvas.clipPath(roundedPath)
    }

    override fun reset() {
        with(paint.asFrameworkPaint()) {
            // With out calling setTypeface before or after paint is reset()
            // Variable type fonts corrupt memory resulting in a
            // segmentation violation
            setTypeface(Typeface.DEFAULT)
            reset()
        }
    }

    override fun startGraphicsLayer(w: Int, h: Int) {
        val newNode = RenderNode("layer")
        newNode.setPosition(0, 0, w, h)
        node = newNode
        previousCanvas = canvas
        canvas = Canvas(newNode.beginRecording())
    }

    override fun setGraphicsLayer(attributes: HashMap<Int?, in Any>) {
        node?.let {
            val node = it
            var hasBlurEffect = false
            var hasOutline = false
            for (key in attributes.keys) {
                val value = attributes.get(key)
                when (key) {
                    GraphicsLayerModifierOperation.SCALE_X -> node.scaleX = value as Float
                    GraphicsLayerModifierOperation.SCALE_Y -> node.scaleY = value as Float
                    GraphicsLayerModifierOperation.ROTATION_X -> node.rotationX = value as Float
                    GraphicsLayerModifierOperation.ROTATION_Y -> node.rotationY = value as Float
                    GraphicsLayerModifierOperation.ROTATION_Z -> node.rotationZ = value as Float
                    GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X ->
                        node.pivotX = value as Float * node.width
                    GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y ->
                        node.pivotY = value as Float * node.width
                    GraphicsLayerModifierOperation.TRANSLATION_X ->
                        node.translationX = value as Float
                    GraphicsLayerModifierOperation.TRANSLATION_Y ->
                        node.translationY = value as Float
                    GraphicsLayerModifierOperation.TRANSLATION_Z ->
                        node.translationZ = value as Float
                    GraphicsLayerModifierOperation.SHAPE -> hasOutline = true
                    GraphicsLayerModifierOperation.SHADOW_ELEVATION ->
                        node.elevation = value as Float
                    GraphicsLayerModifierOperation.ALPHA -> node.alpha = value as Float
                    GraphicsLayerModifierOperation.CAMERA_DISTANCE ->
                        node.setCameraDistance(value as Float)
                    GraphicsLayerModifierOperation.SPOT_SHADOW_COLOR ->
                        node.spotShadowColor = value as Int
                    GraphicsLayerModifierOperation.AMBIENT_SHADOW_COLOR ->
                        node.ambientShadowColor = value as Int
                    GraphicsLayerModifierOperation.HAS_BLUR -> hasBlurEffect = (value as Int?) != 0
                }
            }
            if (hasOutline) {
                val outline = Outline()
                outline.alpha = 1f
                val oShape = attributes.get(GraphicsLayerModifierOperation.SHAPE)
                if (oShape != null) {
                    val oShapeRadius = attributes.get(GraphicsLayerModifierOperation.SHAPE_RADIUS)
                    val type = oShape as Int
                    if (type == GraphicsLayerModifierOperation.SHAPE_RECT) {
                        outline.setRect(0, 0, node.width, node.height)
                    } else if (type == GraphicsLayerModifierOperation.SHAPE_ROUND_RECT) {
                        if (oShapeRadius != null) {
                            val radius = oShapeRadius as Float
                            outline.setRoundRect(Rect(0, 0, node.width, node.height), radius)
                        } else {
                            outline.setRect(0, 0, node.width, node.height)
                        }
                    } else if (type == GraphicsLayerModifierOperation.SHAPE_CIRCLE) {
                        val radius: Float = min(node.width, node.height) / 2f
                        outline.setRoundRect(Rect(0, 0, node.width, node.height), radius)
                    }
                }
                node.setOutline(outline)
            }
            if (hasBlurEffect) {
                val oBlurRadiusX = attributes.get(GraphicsLayerModifierOperation.BLUR_RADIUS_X)
                var blurRadiusX = 0f
                if (oBlurRadiusX != null) {
                    blurRadiusX = oBlurRadiusX as Float
                }
                val oBlurRadiusY = attributes.get(GraphicsLayerModifierOperation.BLUR_RADIUS_Y)
                var blurRadiusY = 0f
                if (oBlurRadiusY != null) {
                    blurRadiusY = oBlurRadiusY as Float
                }
                var blurTileMode = 0
                val oBlurTileMode = attributes.get(GraphicsLayerModifierOperation.BLUR_TILE_MODE)
                if (oBlurTileMode != null) {
                    blurTileMode = oBlurTileMode as Int
                }
                var tileMode = Shader.TileMode.CLAMP
                when (blurTileMode) {
                    GraphicsLayerModifierOperation.TILE_MODE_CLAMP ->
                        tileMode = Shader.TileMode.CLAMP
                    GraphicsLayerModifierOperation.TILE_MODE_DECAL ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
                            tileMode = Shader.TileMode.DECAL
                        } // REMOVE IN PLATFORM
                    GraphicsLayerModifierOperation.TILE_MODE_MIRROR ->
                        tileMode = Shader.TileMode.MIRROR
                    GraphicsLayerModifierOperation.TILE_MODE_REPEATED ->
                        tileMode = Shader.TileMode.REPEAT
                }
                if (
                    Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                    >= Build.VERSION_CODES.S
                ) { // REMOVE IN PLATFORM
                    val effect = RenderEffect.createBlurEffect(blurRadiusX, blurRadiusY, tileMode)
                    node.setRenderEffect(effect)
                } // REMOVE IN PLATFORM
            }
        }
    }

    override fun endGraphicsLayer() {
        node!!.endRecording()
        previousCanvas?.let { canvas = it }
        if (nativeCanvas().isHardwareAccelerated) {
            canvas.enableZ()
            nativeCanvas().drawRenderNode(node!!)
            canvas.disableZ()
        }

        // node.discardDisplayList();
        node = null
    }

    override fun getText(textId: Int): String? {
        return mContext.mRemoteComposeState.getFromId(textId) as String?
    }

    override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {
        val path = getPath(pathId, 0f, 1f)
        if (path.isEmpty) return

        val measure = PathMeasure()
        measure.setPath(path, false)

        val matrix = Matrix()

        measure.getMatrix(matrix, fraction, flags)
        canvas.concat(matrix)
    }

    override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {
        if (mainCanvas == null) {
            mainCanvas = canvas
        }
        if (bitmapId == 0) {
            canvas = mainCanvas!!
            return
        }
        val bitmap = mContext.mRemoteComposeState.getFromId(bitmapId)!! as Bitmap
        if (canvasCache.containsKey(bitmap)) {
            canvas = canvasCache[bitmap]!!

            if ((mode and 1) == 0) {
                bitmap.eraseColor(color)
            }
            return
        }
        canvas = Canvas(bitmap.asImageBitmap())
        if ((mode and 1) == 0) {
            bitmap.eraseColor(color)
        }
        canvasCache[bitmap] = canvas
    }

    private fun nativeCanvas() = canvas.nativeCanvas

    private fun getPath(path1Id: Int, path2Id: Int, tween: Float, start: Float, end: Float): Path {
        return getPath(getPathArray(path1Id, path2Id, tween), start, end)
    }

    private fun getPath(tmp: FloatArray, start: Float, end: Float): Path {
        val path = Path()
        FloatsToPath.genPath(path, tmp, start, end)
        return path
    }

    private fun getPath(id: Int, start: Float, end: Float): Path {
        val p: Path? = mContext.mRemoteComposeState.getPath(id) as Path?
        val w: Int = mContext.mRemoteComposeState.getPathWinding(id)
        if (p != null) {
            return p
        }
        val path = Path()
        val pathData: FloatArray? = mContext.mRemoteComposeState.getPathData(id)
        if (pathData != null) {
            FloatsToPath.genPath(path, pathData, start, end)
            if (w == 1) {
                path.fillType = PathFillType.EvenOdd
            }
            mContext.mRemoteComposeState.putPath(id, path)
        }

        return path
    }

    private fun getNativePath(id: Int, start: Float, end: Float): android.graphics.Path {
        val androidContext = mContext as ComposeRemoteContext
        val p = androidContext.mRemoteComposeState.getPath(id) as? android.graphics.Path
        if (p != null) {
            return p
        }
        val path = android.graphics.Path()
        val pathData = androidContext.mRemoteComposeState.getPathData(id)
        if (pathData != null) {
            androidx.compose.remote.player.view.platform.FloatsToPath.genPath(
                path,
                pathData,
                start,
                end,
            )
            androidContext.mRemoteComposeState.putPath(id, path)
        }

        return path
    }

    private fun getPathArray(path1Id: Int, path2Id: Int, tween: Float): FloatArray {
        val androidContext = mContext as ComposeRemoteContext
        if (tween == 0.0f) {
            return androidContext.mRemoteComposeState.getPathData(path1Id)!!
        }
        if (tween == 1.0f) {
            return androidContext.mRemoteComposeState.getPathData(path2Id)!!
        }

        val data1: FloatArray = androidContext.mRemoteComposeState.getPathData(path1Id)!!
        val data2: FloatArray = androidContext.mRemoteComposeState.getPathData(path2Id)!!
        val tmp = FloatArray(data2.size)
        for (i in tmp.indices) {
            if (java.lang.Float.isNaN(data1[i]) || java.lang.Float.isNaN(data2[i])) {
                tmp[i] = data1[i]
            } else {
                tmp[i] = (data2[i] - data1[i]) * tween + data1[i]
            }
        }
        return tmp
    }

    private fun PathMeasure.getMatrix(matrix: Matrix, fraction: Float, flags: Int) {
        val len = this.length
        if (len == 0f) return

        val distanceOnPath = (len * fraction) % len

        // Get position
        val position = this.getPosition(distanceOnPath) // Returns Offset(x, y)

        // Apply translation for the position
        matrix.translate(position.x, position.y)

        // Check if tangent/rotation is requested (similar to
        // android.graphics.PathMeasure.TANGENT_MATRIX_FLAG)
        // Android: PATH_MEASURE_TANGENT_MATRIX_FLAG = 2
        if ((flags and 2) != 0) { // If the tangent flag is set
            val tangent =
                this.getTangent(distanceOnPath) // Returns Offset representing vector dx, dy
            // Calculate rotation angle from the tangent vector
            val angleRadians = atan2(tangent.y, tangent.x)
            val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
            // Apply rotation around the (translated) origin.
            // Since we already translated, the rotation is effectively at the point on the path.
            matrix.rotateZ(angleDegrees) // Rotate around Z-axis for 2D graphics
        }
    }
}
