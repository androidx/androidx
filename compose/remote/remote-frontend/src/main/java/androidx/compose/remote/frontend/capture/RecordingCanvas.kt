/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.frontend.capture

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.frontend.capture.shaders.RemoteShader
import androidx.compose.remote.frontend.capture.shaders.colorFilterModeToInt
import androidx.compose.remote.frontend.state.MutableRemoteFloat
import androidx.compose.remote.frontend.state.RemoteBitmap
import androidx.compose.remote.frontend.state.RemoteBitmapFont
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteInt
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.remote.frontend.state.getFloatIdForCreationState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.asAndroidPath

/**
 * This provides a recording canvas implementation. This is the main way we intercept the output of
 * a compose function and serialize the result in an origami document.
 *
 * see also: Layout functions
 *
 * Beyond intercepting all the canvas commands, we also support additional origami-specific commands
 * that allow us to represent higher-level concepts:
 * - layout
 * - state machine
 * - animation
 *
 * Notes: this also keeps a local cache of bitmaps and associate them with Ids. Bitmap draw calls
 * are then split in two, with 1/ sending the bitmap 2/ referencing the bitmap via its id, such that
 * follow up calls are cheaper. Main caveat is that the current implementation will not resend an
 * updated bitmap if it's still the same instance (todo: check bitmap generation flag)
 *
 * Capturing the paint object is at the moment very crude; we need a much better mechanism:
 * - the Paint object contains a lot more stuff to capture (!)
 * - efficiently capturing and serializing paint calls is still a WIP; current thinking is to split
 *   up the paint object in several paint commands areas, to simplify both runtime checks at capture
 *   time and more efficient serialization (eg if only one of the area changed, like text-related
 *   attributes, only send this instead of the full paint object).
 * - the way paint instances are used in normal android views begs for a post-process pass in origmi
 *   to identify reuse (eg if cycling between 3-4 different paint objects, we should identify this
 *   instead of serializing the deltas). On the flip side, this might not be as critical/useful in a
 *   compose perspective (are paint objecs reused this way?)
 */
class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap) {

    private var lastStyleOrdinal: Int = -1
    private var typeface: Int = -1
    private var typefaceIsItalic: Boolean = false
    private var typefaceWeight: Int = -1
    private var typefaceStyle: Int = -1
    private var lastStrokeCapOrdinal: Int = -1
    private var lastStrokeJoinOrdinal: Int = -1
    private var lastTextSize: Float = -1F
    private var lastStrokeWidth: Float = -1F
    private var lastColor: Long = -1L
    private var lastColorFilter: ColorFilter? = null
    private var lastColorFilterColor: Int = -1
    private var lastColorFilterMode: Int = -1
    private var lastRemoteShader: RemoteShader? = null
    private var lastBlendMode: BlendMode? = null
    lateinit var document: RemoteComposeWriter
    lateinit var creationState: RemoteComposeCreationState

    private var usingShaderMatrix: Boolean = false

    private var forceSendingPaint = false

    val tempCanvas = Canvas()
    var saveCounter = 0

    /**
     * Forces the next `usePaint` call to send all Paint attributes, regardless of changes. This is
     * useful for ensuring the remote side has the complete, up-to-date paint state.
     *
     * @param value If `true`, the next `usePaint` call will send all Paint attributes.
     */
    fun forceSendingPaint(value: Boolean) {
        forceSendingPaint = value
    }

    /**
     * Sets the [RemoteComposeCreationState] and [RemoteComposeWriter] instances. These are critical
     * for the `RecordingCanvas` to interact with the remote document and capture context. This must
     * be called before any drawing operations.
     *
     * @param creationState The current [RemoteComposeCreationState] holding document and other
     *   context.
     */
    fun setRemoteComposeCreationState(creationState: RemoteComposeCreationState) {
        this.creationState = creationState
        this.document = creationState.document
    }

    /**
     * Processes a [Paint] object, determining which of its attributes have changed since the last
     * `usePaint` call and serializing only those changes (or all if forced) to the remote document
     * via a [PaintBundle].
     *
     * This is a crucial optimization to reduce the amount of data sent over the wire, as `Paint`
     * objects can be complex and frequently modified.
     *
     * @param paint The [Paint] object whose attributes need to be synchronized with the remote
     *   side.
     */
    fun usePaint(paint: Paint) {
        if (document.checkAndClearForceSendingNewPaint()) {
            forceSendingPaint = true
        }
        val paintBundle = PaintBundle()
        val tmpLastColorLong = paint.colorLong
        val tmpLastStrokeWidth = paint.strokeWidth
        val tmpLastTextSize = paint.textSize
        val tmpLastStrokeCapOrdinal = paint.strokeCap.ordinal
        val tmpLastStrokeJoinOrdinal = paint.strokeJoin.ordinal
        val tmpLastStyleOrdinal = paint.style.ordinal
        val paintTypeface = paint.typeface
        val tmpTypeface =
            when (paintTypeface) {
                null -> PaintBundle.FONT_TYPE_DEFAULT
                Typeface.DEFAULT -> PaintBundle.FONT_TYPE_DEFAULT
                Typeface.DEFAULT_BOLD -> PaintBundle.FONT_TYPE_DEFAULT
                Typeface.SERIF -> PaintBundle.FONT_TYPE_SERIF
                Typeface.SANS_SERIF -> PaintBundle.FONT_TYPE_SANS_SERIF
                Typeface.MONOSPACE -> PaintBundle.FONT_TYPE_MONOSPACE
                else -> {
                    if ( // REMOVE IN PLATFORM
                        Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                        >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ) { // REMOVE IN PLATFORM
                        when (paintTypeface.systemFontFamilyName) {
                            "serif" -> PaintBundle.FONT_TYPE_SERIF
                            "sans-serif" -> PaintBundle.FONT_TYPE_SANS_SERIF
                            "monospace" -> PaintBundle.FONT_TYPE_MONOSPACE
                            else -> PaintBundle.FONT_TYPE_DEFAULT
                        }
                    } else { // REMOVE IN PLATFORM
                        PaintBundle.FONT_TYPE_DEFAULT // REMOVE IN PLATFORM
                    } // REMOVE IN PLATFORM
                }
            }
        val tmpTypefaceStyle = paintTypeface?.style ?: 0
        val tmpTypefaceWeight = paintTypeface?.weight ?: 0
        val tmpTypefaceIsItalic = paintTypeface?.isItalic ?: false
        val tmpLastColorFilter = paint.colorFilter
        val tmpLastColorFilterColor =
            if (tmpLastColorFilter is BlendModeColorFilter) {
                tmpLastColorFilter.color
            } else {
                -1
            }
        val tmpLastColorFilterMode =
            if (tmpLastColorFilter is BlendModeColorFilter) {
                colorFilterModeToInt(tmpLastColorFilter.mode)
            } else {
                -1
            }
        var send = forceSendingPaint

        if (forceSendingPaint || lastColor != tmpLastColorLong) {
            val colorSpace = tmpLastColorLong and 0x3fL
            if (colorSpace == REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID) {
                paintBundle.setColorId((tmpLastColorLong shr 6).toInt())
            } else {
                // We don't handle long colors in PaintBundle.
                // TODO: add color long support / color space
                paintBundle.setColor(paint.color)
            }
            lastColor = tmpLastColorLong
            send = true
        }
        if (forceSendingPaint || lastStrokeWidth != tmpLastStrokeWidth) {
            paintBundle.setStrokeWidth(paint.strokeWidth)
            lastStrokeWidth = tmpLastStrokeWidth
            send = true
        }
        if (forceSendingPaint || lastTextSize != tmpLastTextSize) {
            paintBundle.setTextSize(paint.textSize)
            lastTextSize = tmpLastTextSize
            send = true
        }
        if (forceSendingPaint || lastStrokeCapOrdinal != tmpLastStrokeCapOrdinal) {
            paintBundle.setStrokeCap(paint.strokeCap.ordinal)
            lastStrokeCapOrdinal = tmpLastStrokeCapOrdinal
            send = true
        }
        if (forceSendingPaint || lastStrokeJoinOrdinal != tmpLastStrokeJoinOrdinal) {
            paintBundle.setStrokeJoin(paint.strokeJoin.ordinal)
            lastStrokeJoinOrdinal = tmpLastStrokeJoinOrdinal
            send = true
        }
        if (forceSendingPaint || lastStyleOrdinal != tmpLastStyleOrdinal) {
            paintBundle.setStyle(paint.style.ordinal)
            lastStyleOrdinal = tmpLastStyleOrdinal
            send = true
        }
        if (
            forceSendingPaint ||
                typeface != tmpTypeface ||
                typefaceStyle != tmpTypefaceStyle ||
                typefaceWeight != tmpTypefaceWeight ||
                typefaceIsItalic != tmpTypefaceIsItalic
        ) {
            typeface = tmpTypeface
            typefaceStyle = tmpTypefaceStyle
            typefaceWeight = tmpTypefaceWeight
            typefaceIsItalic = tmpTypefaceIsItalic
            paintBundle.setTextStyle(typeface, typefaceWeight, typefaceIsItalic)
            send = true
        }

        if (
            forceSendingPaint ||
                lastColorFilter != tmpLastColorFilter ||
                lastColorFilterMode != tmpLastColorFilterMode ||
                lastColorFilterColor != tmpLastColorFilterColor
        ) {
            if (tmpLastColorFilter is BlendModeColorFilter) {
                lastColorFilter = tmpLastColorFilter
                lastColorFilterColor = tmpLastColorFilterColor
                lastColorFilterMode = tmpLastColorFilterMode
                paintBundle.setColorFilter(
                    tmpLastColorFilter.color,
                    colorFilterModeToInt(tmpLastColorFilter.mode),
                )
                send = true
            } else if (tmpLastColorFilter == null && lastColorFilter != null) {
                lastColorFilter = null
                paintBundle.clearColorFilter()
                send = true
            } else {
                lastColorFilter = null
            }
        }

        val paintBlendMode = paint.blendMode
        if (forceSendingPaint || lastBlendMode != paintBlendMode) {
            lastBlendMode = paintBlendMode
            if (paintBlendMode != null) {
                paintBundle.setBlendMode(colorFilterModeToInt(paintBlendMode))
            } else {
                paintBundle.setBlendMode(PaintBundle.BLEND_MODE_SRC_OVER)
            }
            send = true
        }

        val shader = paint.shader as? RemoteShader
        if (forceSendingPaint || shader != lastRemoteShader) {
            if (shader != null) {
                shader.apply(paintBundle)
                if (usingShaderMatrix || shader.remoteMatrix3x3 != null) {
                    usingShaderMatrix = true
                    paintBundle.setShaderMatrix(
                        shader.remoteMatrix3x3?.getFloatIdForCreationState(creationState) ?: 0f
                    )
                }
            } else {
                paintBundle.setShader(0)
            }
            lastRemoteShader = shader
            send = true
        }
        if (send) {
            PaintData.apply(document.buffer.buffer, paintBundle)
        }
        forceSendingPaint = false
    }

    //  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    //  @SuppressLint("BlockedPrivateApi")
    //  fun usePaint(paint: Paint) {
    //    val newPaintValues = PaintValues()
    //    newPaintValues.color = paint.color
    //    newPaintValues.textSize = paint.textSize
    //    newPaintValues.strokeWidth = paint.strokeWidth
    //    newPaintValues.cap = paint.strokeCap.ordinal
    //    newPaintValues.style = paint.style.ordinal
    //    val cf = paint.colorFilter
    //    if (cf is BlendModeColorFilter) {
    //      newPaintValues.paintBundle.setColorFilter(cf.color, colorFilterModeToInt(cf.mode))
    //    }
    //    if (paint.typeface != null) {
    //      newPaintValues.fontWeight = paint.typeface.weight
    //      newPaintValues.textStyle = paint.typeface.style
    //    } else {
    //      newPaintValues.fontWeight = 400
    //      newPaintValues.textStyle = PaintValues.NORMAL
    //    }
    //    val shader = paint.shader
    //    newPaintValues.shader = shader != null
    //    if (shader != null) {
    //      if (shader is OrigamiSweepShader) {
    //        newPaintValues.paintBundle.setSweepGradient(
    //          shader.colors,
    //          stops = shader.positions,
    //          shader.centerX,
    //          shader.centerY,
    //        )
    //      }
    //      if (shader is OrigamiRadialShader) {
    //        newPaintValues.paintBundle.setRadialGradient(
    //          shader.colors,
    //          stops = shader.positions,
    //          shader.centerX,
    //          shader.centerY,
    //          shader.radius,
    //          shader.tileMode.ordinal,
    //        )
    //      } else if (shader is OrigamiShader) {
    //        newPaintValues.paintBundle.setLinearGradient(
    //          shader.colors,
    //          stops = shader.positions,
    //          shader.x0,
    //          shader.y0,
    //          shader.x1,
    //          shader.y1,
    //          tileMode = 0,
    //        )
    //      } else if (shader is OrigamiRuntimeShader) {
    //        println("RUNTIME SHADER ${shader.description()}")
    //        val runtimeShader = androidx.teleport.origami.operations.RuntimeShader(shader.shader)
    //        for (value in shader.mapValues) {
    //          runtimeShader.setOrigamiUniform(value.key, value.value.first, value.value.second)
    //        }
    //        for (value in shader.map1Values) {
    //          runtimeShader.setUniform(value.key, value.value)
    //        }
    //        for (value in shader.map2Values) {
    //          runtimeShader.setUniform(value.key, value.value.first, value.value.second)
    //        }
    //        for (value in shader.mapBitmaps) {
    //          val bitmapId = getImageId(value.value.bitmap)
    //          val tileX = getTileMode(value.value.tileX)
    //          val tileY = getTileMode(value.value.tileY)
    //          runtimeShader.setBitmapShader(value.key, bitmapId, tileX, tileY)
    //        }
    //        origami.useRuntimeShader(runtimeShader)
    //      }
    //    }
    //    if (true || newPaintValues != paintValues) {
    //      origami.usePaintValues(newPaintValues)
    //      paintValues = newPaintValues
    //    }
    //  }

    //  private fun getTileMode(tileMode: TileMode): Int {
    //    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    //      when (tileMode) {
    //        TileMode.CLAMP -> 0
    //        TileMode.REPEAT -> 1
    //        TileMode.MIRROR -> 2
    //        TileMode.DECAL -> 3
    //      }
    //    } else {
    //      when (tileMode) {
    //        TileMode.CLAMP -> 0
    //        TileMode.REPEAT -> 1
    //        TileMode.MIRROR -> 2
    //        else -> 0
    //      }
    //    }
    //  }

    override fun drawColor(drawColor: Int) {
        drawRect(
            0f,
            0f,
            creationState.size.width,
            creationState.size.height,
            Paint().apply {
                color = drawColor
                style = Paint.Style.FILL
            },
        )
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        // println("NRO drawText $text")
        drawTextRun(text, 0, text.length, 0, text.length, x, y, false, paint)
    }

    fun drawText(text: String, x: Number, y: Number, paint: Paint) {
        // println("NRO drawText $text")
        drawTextRun(
            text,
            0,
            text.length,
            0,
            text.length,
            x.getFloatIdForCreationState(creationState),
            y.getFloatIdForCreationState(creationState),
            false,
            paint,
        )
    }

    /**
     * Draws text from a [RemoteString] at the specified position.
     *
     * @param text The [RemoteString] to draw.
     * @param length The number of characters to draw from the [RemoteString].
     * @param x The X coordinate of the text's origin.
     * @param y The Y coordinate of the text's origin.
     * @param paint The [Paint] object used for styling the text.
     */
    fun drawText(text: RemoteString, length: Int, x: Number, y: Number, paint: Paint) {
        // println("NRO drawText $text")
        usePaint(paint)
        document.drawTextRun(
            text.getIdForCreationState(creationState),
            0,
            length,
            0,
            length,
            x.getFloatIdForCreationState(creationState),
            y.getFloatIdForCreationState(creationState),
            false,
        )
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        // println("NRO drawRect $left $top $right $bottom")
        usePaint(paint)
        document.drawRect(left, top, right, bottom)
    }

    fun drawRect(left: Number, top: Number, right: Number, bottom: Number, paint: Paint) {
        // println("NRO drawRect $left $top $right $bottom")
        usePaint(paint)
        document.drawRect(
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            right.getFloatIdForCreationState(creationState),
            bottom.getFloatIdForCreationState(creationState),
        )
    }

    override fun drawRect(rect: Rect, paint: Paint) {
        usePaint(paint)
        document.drawRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
        )
    }

    override fun drawRect(rect: RectF, paint: Paint) {
        usePaint(paint)
        document.drawRect(rect.left, rect.top, rect.right, rect.bottom)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        usePaint(paint)
        document.drawOval(left, top, right, bottom)
    }

    fun drawOval(left: Number, top: Number, right: Number, bottom: Number, paint: Paint) {
        usePaint(paint)
        document.drawOval(
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            right.getFloatIdForCreationState(creationState),
            bottom.getFloatIdForCreationState(creationState),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        paint: Paint,
    ) {
        // println("NRO drawRoundRect $left $top $right $bottom $rx $ry")
        usePaint(paint)
        document.drawRoundRect(left, top, right, bottom, rx, ry)
    }

    fun drawRoundRect(
        left: Number,
        top: Number,
        right: Number,
        bottom: Number,
        rx: Number,
        ry: Number,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawRoundRect(
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            right.getFloatIdForCreationState(creationState),
            bottom.getFloatIdForCreationState(creationState),
            rx.getFloatIdForCreationState(creationState),
            ry.getFloatIdForCreationState(creationState),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        //        println("NRO drawLine")
        usePaint(paint)
        document.drawLine(startX, startY, stopX, stopY)
    }

    fun drawLine(startX: Number, startY: Number, stopX: Number, stopY: Number, paint: Paint) {
        //        println("NRO drawLine")
        usePaint(paint)
        document.drawLine(
            startX.getFloatIdForCreationState(creationState),
            startY.getFloatIdForCreationState(creationState),
            stopX.getFloatIdForCreationState(creationState),
            stopY.getFloatIdForCreationState(creationState),
        )
    }

    override fun translate(dx: Float, dy: Float) {
        // println("NRO translate $dx $dy")
        if (dx != 0f || dy != 0f) {
            document.translate(dx, dy)
        }
    }

    fun translate(dx: Number, dy: Number) {
        // println("NRO translate $dx $dy")
        document.translate(
            dx.getFloatIdForCreationState(creationState),
            dy.getFloatIdForCreationState(creationState),
        )
    }

    override fun scale(sx: Float, sy: Float) {
        // super.scale(sx, sy)
        //    println(loc() + "NRO scale  " + sx.oString() + " , " + sy.oString())
        //    //        paintContext.add(Scale(sx, sy))
        document.scale(sx, sy)
    }

    fun scale(sx: Number, sy: Number) {
        document.scale(
            sx.getFloatIdForCreationState(creationState),
            sy.getFloatIdForCreationState(creationState),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        // println("NRO drawBitmap 2")
        usePaint(paint!!)
        document.drawBitmap(
            bitmap,
            left,
            top,
            left + bitmap.width.toFloat(),
            top + bitmap.height.toFloat(),
            "",
        )
    }

    fun drawBitmap(bitmap: RemoteBitmap, left: Number, top: Number, paint: Paint?) {
        // println("NRO drawBitmap 2")
        usePaint(paint!!)
        document.drawBitmap(
            bitmap.id,
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            "",
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        // println("NRO drawBitmap 3 ")
        usePaint(paint!!)
        document.drawBitmap(
            bitmap,
            dst.left.toFloat(),
            dst.top.toFloat(),
            dst.right.toFloat(),
            dst.bottom.toFloat(),
            "",
        )
    }

    fun drawBitmap(bitmap: RemoteBitmap, src: Rect?, dst: Rect, paint: Paint?) {
        // println("NRO drawBitmap 3 ")
        usePaint(paint!!)
        document.drawBitmap(
            bitmap.id,
            dst.left.toFloat(),
            dst.top.toFloat(),
            dst.right.toFloat(),
            dst.bottom.toFloat(),
            "",
        )
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        usePaint(paint!!)
        document.drawBitmap(bitmap, dst.left, dst.top, dst.right, dst.bottom, "")
    }

    /**
     * Draws a [RemotePath] onto the canvas using the specified [Paint].
     *
     * @param path The [RemotePath] to draw.
     * @param paint The [Paint] object to use for drawing the path.
     */
    fun drawRPath(path: RemotePath, paint: Paint) {
        usePaint(paint)
        document.drawPath(path)
    }

    override fun save(): Int {
        document.save()
        saveCounter++
        val temp = tempCanvas.save()
        return saveCounter
    }

    override fun restore() {
        document.restore()
        saveCounter--
        /// println("NRO STACK : restore $counter/ ${tempCanvas.saveCount}")
        if (tempCanvas.saveCount > 1) {
            tempCanvas.restore()
        }
    }

    override fun restoreToCount(saveCount: Int) {
        // TOOD: fix ?
        document.restore()
        saveCounter = saveCount
        // println("NRO STACK restoreToCount $saveCount => pre temp canvas is
        // ${tempCanvas.saveCount}")
    }

    override fun getClipBounds(bounds: Rect): Boolean {
        // temp fix, returns the full canvas
        bounds?.set(0, 0, 2048, 2048)
        return true
    }

    override fun clipPath(path: Path): Boolean {
        // println("NRO clipPath")
        tempCanvas.clipPath(path)
        return super.clipPath(path)
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        op: Region.Op,
    ): Boolean {
        // println("NRO clipRect 7")
        document.clipRect(left, top, right, bottom)
        tempCanvas.clipRect(left, top, right, bottom, op)
        return super.clipRect(left, top, right, bottom, op)
    }

    override fun clipRect(rect: Rect) =
        clipRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
        )

    override fun clipRect(rect: RectF) = clipRect(rect.left, rect.top, rect.right, rect.bottom)

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        document.clipRect(left, top, right, bottom)
        tempCanvas.clipRect(left, top, right, bottom)
        return super.clipRect(left, top, right, bottom)
    }

    fun clipRect(left: Number, top: Number, right: Number, bottom: Number): Boolean {
        val l = left.getFloatIdForCreationState(creationState)
        val t = top.getFloatIdForCreationState(creationState)
        val r = right.getFloatIdForCreationState(creationState)
        val b = bottom.getFloatIdForCreationState(creationState)
        document.clipRect(l, t, r, b)
        tempCanvas.clipRect(l, t, r, b)
        return super.clipRect(l, t, r, b)
    }

    override fun drawTextRun(
        text: CharSequence,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint,
    ) {
        // println("NRO drawTextRun 3 - $text $x $y")
        usePaint(paint)
        document.drawTextRun(text.toString(), start, end, contextStart, contextEnd, x, y, isRtl)
    }

    /**
     * Draws a run of text from a [RemoteString] at a specified position.
     *
     * @param text The [RemoteString] to draw.
     * @param start The index of the first character to draw.
     * @param end The index after the last character to draw.
     * @param contextStart The index of the first character of the context for glyph shaping.
     * @param contextEnd The index after the last character of the context for glyph shaping.
     * @param x The X coordinate of the text's origin.
     * @param y The Y coordinate of the text's origin.
     * @param isRtl `true` if the text is right-to-left.
     * @param paint The [Paint] object used for styling.
     */
    fun drawTextRun(
        text: RemoteString,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Number,
        y: Number,
        isRtl: Boolean,
        paint: Paint,
    ) {
        // println("NRO drawTextRun 3 - $text $x $y")
        usePaint(paint)
        document.drawTextRun(
            text.getIdForCreationState(creationState),
            start,
            end,
            contextStart,
            contextEnd,
            x.getFloatIdForCreationState(creationState),
            y.getFloatIdForCreationState(creationState),
            isRtl,
        )
    }

    /**
     * Draws a substring of [text] with [bitmapFontId] at position [x], [y]
     *
     * @param text The [RemoteString] to draw
     * @param bitmapFont The [RemoteBitmapFont] to draw [text] with
     * @param start The character to start drawing from
     * @param end The character to stop drawing at. Note if this is -1 then all characters from
     *   [start] until the last character of [text] are drawn
     * @param x The left x-coordinate to start rendering from
     * @param y The top y-coordinate to start rendering from
     * @param paint The [Paint] to render with
     */
    fun drawBitmapFontTextRun(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        start: Int,
        end: Int,
        x: Number,
        y: Number,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawBitmapFontTextRun(
            text.getIdForCreationState(creationState),
            bitmapFont.getIdForCreationState(creationState),
            start,
            end,
            x.getFloatIdForCreationState(creationState),
            y.getFloatIdForCreationState(creationState),
        )
    }

    /**
     * Draws a substring of [text] with [bitmapFontId] at position [x], [y]
     *
     * @param text The [RemoteString] to draw
     * @param bitmapFont The [RemoteBitmapFont] to draw [text] with
     * @param path The [Path] to draw along
     * @param start The character to start drawing from
     * @param end The character to stop drawing at. Note if this is -1 then all characters from
     *   [start] until the last character of [text] are drawn
     * @param yAdj Adjustment away from the path along the normal at that point
     * @param paint The [Paint] to render with
     */
    fun drawBitmapFontTextRunOnPath(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        path: Path,
        start: Int,
        end: Int,
        yAdj: Float,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawBitmapFontTextRunOnPath(
            text.getIdForCreationState(creationState),
            bitmapFont.getIdForCreationState(creationState),
            path,
            start,
            end,
            yAdj,
        )
    }

    /**
     * Draws a substring of [text] with [bitmapFontId] centered position [x], [y] with additional
     * translation from [panx] & [pany]
     *
     * @param text The [RemoteString] to draw
     * @param bitmapFont The [RemoteBitmapFont] to draw [text] with
     * @param start The character to start drawing from
     * @param end The character to stop drawing at. Note if this is -1 then all characters from
     *   [start] until the last character of [text] are drawn
     * @param x The left x-coordinate to start rendering from
     * @param y The top y-coordinate to start rendering from
     * @param panx A horizontal translation applied to the text. A value of -1 = left aligned, 0 =
     *   centered horizontally, 1 = right aligned.
     * @param pany A vertical translation applied to the text. A value of -1 = top aligned, 0 =
     *   centered vertically, 1 = bottom aligned.
     * @param paint The [Paint] to render with
     */
    fun drawAnchoredBitmapFontTextRun(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        start: Int,
        end: Int,
        x: Number,
        y: Number,
        panx: Number,
        pany: Number,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawBitmapTextAnchored(
            text.getIdForCreationState(creationState),
            bitmapFont.getIdForCreationState(creationState),
            start.toFloat(),
            end.toFloat(),
            x.getFloatIdForCreationState(creationState),
            y.getFloatIdForCreationState(creationState),
            panx.getFloatIdForCreationState(creationState),
            pany.getFloatIdForCreationState(creationState),
        )
    }

    override fun drawPath(path: Path, paint: Paint) {
        //    println("NRO drawPath")
        usePaint(paint)
        document.drawPath(path)
    }

    override fun rotate(degrees: Float) {
        //        println("NRO rotate $degrees")
        document.rotate(degrees)
        super.rotate(degrees)
    }

    /**
     * Applies a rotation transformation to the canvas.
     *
     * @param degrees The angle of rotation in degrees.
     */
    fun rotate(degrees: Number) {
        val id = degrees.getFloatIdForCreationState(creationState)
        document.rotate(id)
        super.rotate(id)
    }

    /**
     * Applies a rotation transformation to the canvas around a specified pivot point.
     *
     * @param degrees The angle of rotation in degrees.
     * @param px The X-coordinate of the pivot point.
     * @param py The Y-coordinate of the pivot point.
     */
    fun rotate(degrees: Number, px: Number, py: Number) {
        document.rotate(
            degrees.getFloatIdForCreationState(creationState),
            px.getFloatIdForCreationState(creationState),
            py.getFloatIdForCreationState(creationState),
        )
    }

    override fun saveLayer(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint?,
    ): Int {
        //    println("NRO saveLayer 1")
        //    if (paint != null) {
        //      //            usePaint(paint)
        //    }
        //    origami.saveLayer(left, top, left + right, top + bottom)
        //    //        tempCanvas.saveLayer(left, top, right, bottom, paint)
        return super.saveLayer(left, top, right, bottom, paint)
    }

    override fun saveLayer(bounds: RectF?, paint: Paint?): Int {
        //    println("NRO saveLayer 2")
        tempCanvas.saveLayer(bounds, paint)
        return super.saveLayer(bounds, paint)
    }

    override fun getSaveCount(): Int {
        //        println("NRO getSaveCount: ${paintContext.counter} / ${tempCanvas.saveCount}")
        return saveCounter
    }

    override fun drawTextOnPath(
        text: String,
        path: Path,
        hOffset: Float,
        vOffset: Float,
        paint: Paint,
    ) {
        // println("NRO drawTextOnPath 1")
        usePaint(paint)
        document.drawTextOnPath(text, path, hOffset, vOffset)
    }

    /**
     * Draws text from a [RemoteString] along a given [Path].
     *
     * @param text The [RemoteString] to draw.
     * @param path The [Path] along which to draw the text.
     * @param hOffset The horizontal offset along the path.
     * @param vOffset The vertical offset from the path.
     * @param paint The [Paint] object for styling the text.
     */
    fun drawTextOnPath(text: String, path: Path, hOffset: Number, vOffset: Number, paint: Paint) {
        // println("NRO drawTextOnPath 1")
        usePaint(paint)
        document.drawTextOnPath(
            text,
            path,
            hOffset.getFloatIdForCreationState(creationState),
            vOffset.getFloatIdForCreationState(creationState),
        )
    }

    /**
     * Draws text from a [RemoteString] along a given [Path].
     *
     * @param text The [RemoteString] to draw.
     * @param path The [Path] along which to draw the text.
     * @param hOffset The horizontal offset along the path.
     * @param vOffset The vertical offset from the path.
     * @param paint The [Paint] object for styling the text.
     */
    fun drawTextOnPath(
        text: RemoteString,
        path: Path,
        hOffset: Number,
        vOffset: Number,
        paint: Paint,
    ) {
        // println("NRO drawTextOnPath 1")
        usePaint(paint)
        document.drawTextOnPath(
            text.getIdForCreationState(creationState),
            path,
            hOffset.getFloatIdForCreationState(creationState),
            vOffset.getFloatIdForCreationState(creationState),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint,
    ) {
        // println("NRO drawArc")
        usePaint(paint)
        if (useCenter) {
            document.drawSector(left, top, right, bottom, startAngle, sweepAngle)
        } else {
            document.drawArc(left, top, right, bottom, startAngle, sweepAngle)
        }
    }

    fun drawArc(
        left: Number,
        top: Number,
        right: Number,
        bottom: Number,
        startAngle: Number,
        sweepAngle: Number,
        useCenter: Boolean,
        paint: Paint,
    ) {
        // println("NRO drawArc")
        usePaint(paint)
        if (useCenter) {
            document.drawSector(
                left.getFloatIdForCreationState(creationState),
                top.getFloatIdForCreationState(creationState),
                right.getFloatIdForCreationState(creationState),
                bottom.getFloatIdForCreationState(creationState),
                startAngle.getFloatIdForCreationState(creationState),
                sweepAngle.getFloatIdForCreationState(creationState),
            )
        } else {
            document.drawArc(
                left.getFloatIdForCreationState(creationState),
                top.getFloatIdForCreationState(creationState),
                right.getFloatIdForCreationState(creationState),
                bottom.getFloatIdForCreationState(creationState),
                startAngle.getFloatIdForCreationState(creationState),
                sweepAngle.getFloatIdForCreationState(creationState),
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        //        println("NRO drawCircle ($cx, $cy)")
        usePaint(paint)
        document.drawCircle(cx, cy, radius)
    }

    /**
     * Draws text with an anchor point and translation factors, allowing for flexible positioning
     * relative to a given anchor.
     *
     * @param text The [String] of text to draw.
     * @param anchorX The X-coordinate of the anchor point.
     * @param anchorY The Y-coordinate of the anchor point.
     * @param panx A horizontal translation factor (-1 = left, 0 = center, 1 = right).
     * @param pany A vertical translation factor (-1 = top, 0 = center, 1 = bottom).
     * @param flags Additional flags for text anchoring/alignment.
     * @param paint The [Paint] object for styling.
     */
    fun drawAnchoredText(
        text: String,
        anchorX: Number,
        anchorY: Number,
        panx: Number,
        pany: Number,
        flags: Int,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawTextAnchored(
            text,
            anchorX.getFloatIdForCreationState(creationState),
            anchorY.getFloatIdForCreationState(creationState),
            panx.getFloatIdForCreationState(creationState),
            pany.getFloatIdForCreationState(creationState),
            flags,
        )
    }

    /**
     * Draws text from a [RemoteString] with an anchor point and translation factors.
     *
     * @param text The [RemoteString] to draw.
     * @param anchorX The X-coordinate of the anchor point.
     * @param anchorY The Y-coordinate of the anchor point.
     * @param panx A horizontal translation factor (-1 = left, 0 = center, 1 = right).
     * @param pany A vertical translation factor (-1 = left, 0 = center, 1 = right).
     * @param flags Additional flags for text anchoring/alignment.
     * @param paint The [Paint] object for styling.
     */
    fun drawAnchoredText(
        text: RemoteString,
        anchorX: Number,
        anchorY: Number,
        panx: Number,
        pany: Number,
        flags: Int,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawTextAnchored(
            text.getIdForCreationState(creationState),
            anchorX.getFloatIdForCreationState(creationState),
            anchorY.getFloatIdForCreationState(creationState),
            panx.getFloatIdForCreationState(creationState),
            pany.getFloatIdForCreationState(creationState),
            flags,
        )
    }

    /**
     * Draws a path that is an interpolation (tween) between two Compose UI [Path] objects.
     *
     * This allows for smooth animation between different path shapes. If the input paths are
     * [RemoteComposePath] instances, it optimizes by directly using their remote representations.
     * Otherwise, it converts them to Android `Path` objects.
     *
     * @param path1 The starting Compose UI [Path].
     * @param path2 The ending Compose UI [Path].
     * @param tween The interpolation factor (0.0 for `path1`, 1.0 for `path2`).
     * @param start The start value for internal tween calculations (often 0.0).
     * @param stop The stop value for internal tween calculations (often 1.0).
     * @param paint The Compose UI [Paint] object for styling.
     */
    fun drawTweenPath(
        path1: androidx.compose.ui.graphics.Path,
        path2: androidx.compose.ui.graphics.Path,
        tween: Number,
        start: Number,
        stop: Number,
        paint: androidx.compose.ui.graphics.Paint,
    ) {
        usePaint(paint.asFrameworkPaint())
        if (path1 is RemoteComposePath && path2 is RemoteComposePath) {
            document.drawTweenPath(
                path1.remote,
                path2.remote,
                tween.getFloatIdForCreationState(creationState),
                start.getFloatIdForCreationState(creationState),
                stop.getFloatIdForCreationState(creationState),
            )
            return
        }

        document.drawTweenPath(
            path1.asAndroidPath(),
            path2.asAndroidPath(),
            tween.getFloatIdForCreationState(creationState),
            start.getFloatIdForCreationState(creationState),
            stop.getFloatIdForCreationState(creationState),
        )
    }

    fun paint(canvas: Canvas) {
        canvas.restoreToCount(1)
    }

    /**
     * Draws a scaled portion of an [Image] into a destination rectangle.
     *
     * @param image The [Bitmap] image to draw.
     * @param srcLeft The left coordinate of the source rectangle in the image.
     * @param srcTop The top coordinate of the source rectangle in the image.
     * @param srcRight The right coordinate of the source rectangle in the image.
     * @param srcBottom The bottom coordinate of the source rectangle in the image.
     * @param dstLeft The left coordinate of the destination rectangle on the canvas.
     * @param dstTop The top coordinate of the destination rectangle on the canvas.
     * @param dstRight The right coordinate of the destination rectangle on the canvas.
     * @param dstBottom The bottom coordinate of the destination rectangle on the canvas.
     * @param scaleType An integer representing the scale type (e.g., FIT_XY, CENTER_CROP).
     * @param scaleFactor A general scale factor.
     * @param contentDescription An optional content description for accessibility.
     */
    fun drawScaledBitmap(
        image: Bitmap,
        srcLeft: Number,
        srcTop: Number,
        srcRight: Number,
        srcBottom: Number,
        dstLeft: Number,
        dstTop: Number,
        dstRight: Number,
        dstBottom: Number,
        scaleType: Int,
        scaleFactor: Number,
        contentDescription: String?,
    ) {
        document.drawScaledBitmap(
            image,
            srcLeft.getFloatIdForCreationState(creationState),
            srcTop.getFloatIdForCreationState(creationState),
            srcRight.getFloatIdForCreationState(creationState),
            srcBottom.getFloatIdForCreationState(creationState),
            dstLeft.getFloatIdForCreationState(creationState),
            dstTop.getFloatIdForCreationState(creationState),
            dstRight.getFloatIdForCreationState(creationState),
            dstBottom.getFloatIdForCreationState(creationState),
            scaleType,
            scaleFactor.getFloatIdForCreationState(creationState),
            contentDescription,
        )
    }

    /**
     * Draws a scaled portion of a [RemoteBitmap] into a destination rectangle.
     *
     * @param image The [RemoteBitmap] to draw.
     * @param srcLeft The left coordinate of the source rectangle in the image.
     * @param srcTop The top coordinate of the source rectangle in the image.
     * @param srcRight The right coordinate of the source rectangle in the image.
     * @param srcBottom The bottom coordinate of the source rectangle in the image.
     * @param dstLeft The left coordinate of the destination rectangle on the canvas.
     * @param dstTop The top coordinate of the destination rectangle on the canvas.
     * @param dstRight The right coordinate of the destination rectangle on the canvas.
     * @param dstBottom The bottom coordinate of the destination rectangle on the canvas.
     * @param scaleType An integer representing the scale type.
     * @param scaleFactor A general scale factor.
     * @param contentDescription An optional content description for accessibility.
     */
    fun drawScaledBitmap(
        image: RemoteBitmap,
        srcLeft: Number,
        srcTop: Number,
        srcRight: Number,
        srcBottom: Number,
        dstLeft: Number,
        dstTop: Number,
        dstRight: Number,
        dstBottom: Number,
        scaleType: Int,
        scaleFactor: Number,
        contentDescription: String?,
    ) {
        document.drawScaledBitmap(
            image.getIdForCreationState(creationState),
            srcLeft.getFloatIdForCreationState(creationState),
            srcTop.getFloatIdForCreationState(creationState),
            srcRight.getFloatIdForCreationState(creationState),
            srcBottom.getFloatIdForCreationState(creationState),
            dstLeft.getFloatIdForCreationState(creationState),
            dstTop.getFloatIdForCreationState(creationState),
            dstRight.getFloatIdForCreationState(creationState),
            dstBottom.getFloatIdForCreationState(creationState),
            scaleType,
            scaleFactor.getFloatIdForCreationState(creationState),
            contentDescription,
        )
    }

    /**
     * Executes [body] commands in a loop, with the index in the range
     * [from .. until) with a stride of [step].
     *
     * Note there is a maximum number of operations that will be executed, which may be less than
     * the total number of iterations needed to fully realize this loop.
     *
     * @param from The initial value of index
     * @param until The loop will be executed until the index is >= this value
     * @param step The amount to increment each time
     * @param body Code that generates draw calls to run in a loop.
     */
    fun loop(from: Number, until: Number, step: Number, body: (index: RemoteFloat) -> Unit) {
        val loopVariableId = document.createFloatId()
        val loopVariable = MutableRemoteFloat(mutableFloatStateOf(0f), loopVariableId)
        document.loop(
            Utils.idFromNan(loopVariableId),
            from.getFloatIdForCreationState(creationState),
            step.getFloatIdForCreationState(creationState),
            until.getFloatIdForCreationState(creationState),
        ) {
            body(loopVariable)
        }
    }

    /**
     * Executes [body] commands in a loop, with the index in the range [from .. until).
     *
     * Note there is a maximum number of operations that will be executed, which may be less than
     * the total number of iterations needed to fully realize this loop.
     *
     * @param from The initial value of index
     * @param until The loop will be executed until the index is >= this value
     * @param body Code that generates draw calls to run in a loop.
     */
    fun loop(from: Int, until: RemoteInt, body: (index: RemoteInt) -> Unit) {
        val loopVariableId = document.createFloatId()
        val loopVariable = MutableRemoteFloat(mutableFloatStateOf(0f), loopVariableId)
        document.loop(
            Utils.idFromNan(loopVariableId),
            from.toFloat(),
            1f,
            until.getFloatIdForCreationState(creationState),
        ) {
            body(loopVariable.toRemoteInt())
        }
    }

    /**
     * Sets the position to align with a [fraction]al point along the given [path] and the rotation
     * to align with the path's tangent at that point.
     *
     * @param path The [Path]
     * @param fraction The fraction along the path. Note a whole number such as 1 wraps around to
     *   the beginning.
     * @param tangentalOffset An offset in pixels from from the path along the tangent.
     */
    fun setMatrixFromPath(path: Path, fraction: Number, tangentalOffset: Number) {
        document.matrixFromPath(
            document.addPathData(path),
            fraction.getFloatIdForCreationState(creationState),
            tangentalOffset.getFloatIdForCreationState(creationState),
            3,
        )
    }

    companion object {
        // TODO replace this with a dedicated color space for RemoteCompose.
        internal const val REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID = 5L
    }
}
