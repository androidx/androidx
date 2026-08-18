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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import java.util.HashMap

/**
 * A dummy, draw-nothing [PaintContext] for evaluating value-producing
 * [androidx.compose.remote.core.PaintOperation]s in [GraphContext].
 */
internal class GraphPaintContext(context: RemoteContext) : PaintContext(context) {
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
    ) {}

    override fun scale(scaleX: Float, scaleY: Float) {}

    override fun translate(translateX: Float, translateY: Float) {}

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {}

    override fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {}

    override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {}

    override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {}

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {}

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {}

    override fun drawPath(id: Int, start: Float, end: Float) {}

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {}

    override fun savePaint() {}

    override fun restorePaint() {}

    override fun replacePaint(paintBundle: PaintBundle) {}

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {}

    override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {}

    override fun getTextBounds(textId: Int, start: Int, end: Int, flags: Int, bounds: FloatArray) {}

    override fun layoutComplexText(
        textId: Int,
        start: Int,
        end: Int,
        alignment: Int,
        overflow: Int,
        maxLines: Int,
        maxWidth: Float,
        maxHeight: Float,
        letterSpacing: Float,
        lineHeightAdd: Float,
        lineHeightMultiplier: Float,
        lineBreakStrategy: Int,
        hyphenationFrequency: Int,
        justificationMode: Int,
        useUnderline: Boolean,
        strikethrough: Boolean,
        flags: Int,
    ): RcPlatformServices.ComputedTextLayout? = null

    override fun drawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {}

    override fun drawComplexText(computedTextLayout: RcPlatformServices.ComputedTextLayout?) {}

    override fun drawTweenPath(
        path1Id: Int,
        path2Id: Int,
        tween: Float,
        start: Float,
        end: Float,
    ) {}

    override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}

    override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}

    override fun applyPaint(mPaintData: PaintBundle) {}

    override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}

    override fun matrixTranslate(translateX: Float, translateY: Float) {}

    override fun matrixSkew(skewX: Float, skewY: Float) {}

    override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}

    override fun matrixSave() {}

    override fun matrixRestore() {}

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {}

    override fun clipPath(pathId: Int, regionOp: Int) {}

    override fun roundedClipRect(
        width: Float,
        height: Float,
        topStart: Float,
        topEnd: Float,
        bottomStart: Float,
        bottomEnd: Float,
    ) {}

    override fun reset() {}

    override fun startGraphicsLayer(w: Int, h: Int) {}

    override fun setGraphicsLayer(attributes: HashMap<Int, Any>) {}

    override fun endGraphicsLayer() {}

    override fun getText(id: Int): String? = mContext.getText(id)

    override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

    override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
}
