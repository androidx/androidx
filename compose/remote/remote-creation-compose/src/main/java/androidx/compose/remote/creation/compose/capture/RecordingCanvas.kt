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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture

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
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.shaders.colorFilterModeToInt
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.getFloatIdForCreationState
import androidx.compose.remote.creation.compose.state.pack
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb

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
 *   compose perspective (are paint objects reused this way?)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap), RemoteStateScope {

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
    private var lastRemoteColorFilter: RemoteColorFilter? = null
    override lateinit var creationState: RemoteComposeCreationState
    override val document: RemoteComposeWriter
        get() = creationState.document

    override val remoteDensity: RemoteDensity
        get() = creationState.remoteDensity

    private var usingShaderMatrix: Boolean = false

    private var forceSendingPaint = false

    public val tempCanvas: Canvas = Canvas()
    public var saveCounter: Int = 0
    private var currentDrawToBitmapId = 0

    /**
     * Forces the next `usePaint` call to send all Paint attributes, regardless of changes. This is
     * useful for ensuring the remote side has the complete, up-to-date paint state.
     *
     * @param value If `true`, the next `usePaint` call will send all Paint attributes.
     */
    public fun forceSendingPaint(value: Boolean) {
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
    public fun setRemoteComposeCreationState(creationState: RemoteComposeCreationState) {
        this.creationState = creationState
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
    public fun usePaint(paint: Paint) {
        if (document.checkAndClearForceSendingNewPaint()) {
            forceSendingPaint = true
        }
        val paintBundle = PaintBundle()
        val tmpLastColorLong =
            if (paint is RemotePaint) {
                val remoteColor = paint.remoteColor
                if (remoteColor == null) {
                    paint.color.toLong() shl 32
                } else {
                    val constantValue = remoteColor.constantValueOrNull
                    if (constantValue == null) {
                        remoteColor.getIdForCreationState(creationState).toLong() shl
                            6 or
                            REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID
                    } else {
                        constantValue.pack()
                    }
                }
            } else {
                paint.color.toLong() shl 32
            }
        val tmpLastStrokeWidth = paint.strokeWidth
        val tmpLastTextSize = paint.textSize
        // Handle NPE in Robolectric
        val tmpLastStrokeCapOrdinal = paint.strokeCap?.ordinal ?: Paint.Cap.BUTT.ordinal
        val tmpLastStrokeJoinOrdinal = paint.strokeJoin?.ordinal ?: Paint.Join.MITER.ordinal
        val tmpLastStyleOrdinal = paint.style?.ordinal ?: Paint.Style.FILL.ordinal
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
        val tmpLastRemoteColorFilter =
            if (paint is RemotePaint) {
                paint.remoteColorFilter
            } else {
                null
            }
        var send = forceSendingPaint

        if (forceSendingPaint || lastColor != tmpLastColorLong) {
            val colorSpace = tmpLastColorLong and 0x3fL
            if (colorSpace == REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID) {
                paintBundle.setColorId((tmpLastColorLong shr 6).toInt())
            } else {
                // We don't handle long colors in PaintBundle.
                // TODO: add color long support / color space
                paintBundle.setColor((tmpLastColorLong shr 32).toInt())
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
            paintBundle.setStrokeCap(tmpLastStrokeCapOrdinal)
            lastStrokeCapOrdinal = tmpLastStrokeCapOrdinal
            send = true
        }
        if (forceSendingPaint || lastStrokeJoinOrdinal != tmpLastStrokeJoinOrdinal) {
            paintBundle.setStrokeJoin(tmpLastStrokeJoinOrdinal)
            lastStrokeJoinOrdinal = tmpLastStrokeJoinOrdinal
            send = true
        }
        if (forceSendingPaint || lastStyleOrdinal != tmpLastStyleOrdinal) {
            paintBundle.setStyle(tmpLastStyleOrdinal)
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
                lastColorFilterColor != tmpLastColorFilterColor ||
                lastRemoteColorFilter != tmpLastRemoteColorFilter
        ) {
            if (tmpLastRemoteColorFilter != null) {
                when (tmpLastRemoteColorFilter) {
                    is RemoteBlendModeColorFilter -> {
                        val constantColor = tmpLastRemoteColorFilter.color.constantValueOrNull

                        if (constantColor != null) {
                            // Where possible use a constant instead of an expression.
                            paintBundle.setColorFilter(
                                constantColor.toArgb(),
                                colorFilterModeToInt(tmpLastRemoteColorFilter.blendMode),
                            )
                        } else {
                            paintBundle.setColorFilterId(
                                tmpLastRemoteColorFilter.color.getIdForCreationState(creationState),
                                colorFilterModeToInt(tmpLastRemoteColorFilter.blendMode),
                            )
                        }
                    }
                }
                send = true
            } else if (tmpLastColorFilter is BlendModeColorFilter) {
                paintBundle.setColorFilter(
                    tmpLastColorFilter.color,
                    colorFilterModeToInt(tmpLastColorFilter.mode),
                )
                send = true
            }

            if (
                (tmpLastRemoteColorFilter == null && tmpLastColorFilter == null) &&
                    (lastRemoteColorFilter != null || lastColorFilter != null)
            ) {
                paintBundle.clearColorFilter()
                send = true
            }

            lastColorFilter = tmpLastColorFilter
            lastColorFilterColor = tmpLastColorFilterColor
            lastColorFilterMode = tmpLastColorFilterMode
            lastRemoteColorFilter = tmpLastRemoteColorFilter
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
                shader.apply(creationState, paintBundle)
                if (usingShaderMatrix || shader.remoteMatrix3x3 != null) {
                    usingShaderMatrix = true
                    val remoteMatrix3x3 = shader.remoteMatrix3x3
                    if (remoteMatrix3x3 != null) {
                        paintBundle.setShaderMatrix(
                            remoteMatrix3x3.getFloatIdForCreationState(creationState)
                        )
                    } else {
                        paintBundle.setShaderMatrix(0f)
                        usingShaderMatrix = false
                    }
                }
            } else {
                paintBundle.setShader(0)
            }
            lastRemoteShader = shader
            send = true
        }
        if (send) {
            document.buffer.addPaint(paintBundle)
        }
        forceSendingPaint = false
    }

    override fun drawColor(drawColor: Int) {
        drawRect(
            0f.rf,
            0f.rf,
            creationState.creationDisplayInfo.width.rf,
            creationState.creationDisplayInfo.height.rf,
            Paint().apply {
                color = drawColor
                style = Paint.Style.FILL
            },
        )
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        drawTextRun(text, 0, text.length, 0, text.length, x, y, false, paint)
    }

    public fun drawText(text: String, x: RemoteFloat, y: RemoteFloat, paint: Paint) {
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
    public fun drawText(
        text: RemoteString,
        length: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        paint: Paint,
    ) {
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
        usePaint(paint)
        document.drawRect(left, top, right, bottom)
    }

    public fun drawRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: Paint,
    ) {
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

    /** For V1 compatibility. */
    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        usePaint(paint)
        document.drawOval(left, top, right, bottom)
    }

    public fun drawOval(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawOval(
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            right.getFloatIdForCreationState(creationState),
            bottom.getFloatIdForCreationState(creationState),
        )
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawRoundRect(left, top, right, bottom, rx, ry)
    }

    public fun drawRoundRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        rx: RemoteFloat,
        ry: RemoteFloat,
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

    public fun drawTextOnCircle(
        text: RemoteString,
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        startAngle: RemoteFloat,
        warpRadiusOffset: RemoteFloat,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
        paint: RemotePaint,
    ) {
        usePaint(paint)
        document.drawTextOnCircle(
            text.getIdForCreationState(creationState),
            centerX.getFloatIdForCreationState(creationState),
            centerY.getFloatIdForCreationState(creationState),
            radius.getFloatIdForCreationState(creationState),
            startAngle.getFloatIdForCreationState(creationState),
            warpRadiusOffset.getFloatIdForCreationState(creationState),
            alignment,
            placement,
        )
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        usePaint(paint)
        document.drawLine(startX, startY, stopX, stopY)
    }

    public fun drawLine(
        startX: RemoteFloat,
        startY: RemoteFloat,
        stopX: RemoteFloat,
        stopY: RemoteFloat,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawLine(
            startX.getFloatIdForCreationState(creationState),
            startY.getFloatIdForCreationState(creationState),
            stopX.getFloatIdForCreationState(creationState),
            stopY.getFloatIdForCreationState(creationState),
        )
    }

    override fun translate(dx: Float, dy: Float) {
        if (dx != 0f || dy != 0f) {
            document.translate(dx, dy)
        }
    }

    public fun translate(dx: RemoteFloat, dy: RemoteFloat) {
        document.translate(
            dx.getFloatIdForCreationState(creationState),
            dy.getFloatIdForCreationState(creationState),
        )
    }

    override fun scale(sx: Float, sy: Float) {
        document.scale(sx, sy)
    }

    public fun scale(sx: RemoteFloat, sy: RemoteFloat) {
        document.scale(
            sx.getFloatIdForCreationState(creationState),
            sy.getFloatIdForCreationState(creationState),
        )
    }

    public fun scale(sx: RemoteFloat, sy: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
        document.scale(
            sx.getFloatIdForCreationState(creationState),
            sy.getFloatIdForCreationState(creationState),
            px.getFloatIdForCreationState(creationState),
            py.getFloatIdForCreationState(creationState),
        )
    }

    public fun drawBitmap(bitmap: ImageBitmap, left: Float, top: Float, paint: Paint?) {
        usePaint(paint!!)
        val androidBitmap = bitmap.asAndroidBitmap()
        document.drawBitmap(
            androidBitmap,
            left,
            top,
            left + androidBitmap.width.toFloat(),
            top + androidBitmap.height.toFloat(),
            "",
        )
    }

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        drawBitmap(bitmap.asImageBitmap(), left, top, paint)
    }

    public fun drawBitmap(
        bitmap: RemoteBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        paint: Paint?,
    ) {
        usePaint(paint!!)
        document.drawBitmap(
            bitmap.getIdForCreationState(creationState),
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            "",
        )
    }

    public fun drawBitmap(bitmap: ImageBitmap, src: Rect?, dst: Rect, paint: Paint?) {
        usePaint(paint!!)
        val androidBitmap = bitmap.asAndroidBitmap()
        document.drawBitmap(
            androidBitmap,
            dst.left.toFloat(),
            dst.top.toFloat(),
            dst.right.toFloat(),
            dst.bottom.toFloat(),
            "",
        )
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        drawBitmap(bitmap.asImageBitmap(), src, dst, paint)
    }

    public fun drawBitmap(bitmap: RemoteBitmap, src: Rect?, dst: Rect, paint: Paint?) {
        usePaint(paint!!)
        document.drawBitmap(
            bitmap.getIdForCreationState(creationState),
            dst.left.toFloat(),
            dst.top.toFloat(),
            dst.right.toFloat(),
            dst.bottom.toFloat(),
            "",
        )
    }

    public fun drawBitmap(bitmap: ImageBitmap, src: Rect?, dst: RectF, paint: Paint?) {
        usePaint(paint!!)
        val androidBitmap = bitmap.asAndroidBitmap()
        document.drawBitmap(androidBitmap, dst.left, dst.top, dst.right, dst.bottom, "")
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        drawBitmap(bitmap.asImageBitmap(), src, dst, paint)
    }

    /**
     * Draws a [RemotePath] onto the canvas using the specified [Paint].
     *
     * @param path The [RemotePath] to draw.
     * @param paint The [Paint] object to use for drawing the path.
     */
    public fun drawRPath(path: RemotePath, paint: Paint) {
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
        document.clipRect(left, top, right, bottom)
        tempCanvas.clipRect(left, top, right, bottom, op)
        return super.clipRect(left, top, right, bottom, op)
    }

    override fun clipRect(rect: Rect): Boolean =
        clipRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
        )

    override fun clipRect(rect: RectF): Boolean =
        clipRect(rect.left, rect.top, rect.right, rect.bottom)

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        document.clipRect(left, top, right, bottom)
        tempCanvas.clipRect(left, top, right, bottom)
        return super.clipRect(left, top, right, bottom)
    }

    public fun clipRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
    ): Boolean {
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
    public fun drawTextRun(
        text: RemoteString,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        isRtl: Boolean,
        paint: Paint,
    ) {
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
     * Draws a substring of [text] with [bitmapFont] at position [x], [y]
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
    public fun drawBitmapFontTextRun(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        start: Int,
        end: Int,
        x: RemoteFloat,
        y: RemoteFloat,
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
     * Draws a substring of [text] with [bitmapFont]
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
    public fun drawBitmapFontTextRunOnPath(
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
     * Draws a substring of [text] with [bitmapFont] centered position [x], [y] with additional
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
    public fun drawAnchoredBitmapFontTextRun(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        start: Int,
        end: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        panx: RemoteFloat,
        pany: RemoteFloat,
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

    public fun drawRPath(path: RemotePath, paint: RemotePaint) {
        usePaint(paint)
        val pathId = document.addPathData(path)
        document.drawPath(pathId)
    }

    override fun drawPath(path: Path, paint: Paint) {
        usePaint(paint)
        document.drawPath(path)
    }

    override fun rotate(degrees: Float) {
        document.rotate(degrees)
        super.rotate(degrees)
    }

    /**
     * Applies a rotation transformation to the canvas.
     *
     * @param degrees The angle of rotation in degrees.
     */
    public fun rotate(degrees: RemoteFloat) {
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
    public fun rotate(degrees: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
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
        //    if (paint != null) {
        //      //            usePaint(paint)
        //    }
        //    origami.saveLayer(left, top, left + right, top + bottom)
        //    //        tempCanvas.saveLayer(left, top, right, bottom, paint)
        return super.saveLayer(left, top, right, bottom, paint)
    }

    override fun saveLayer(bounds: RectF?, paint: Paint?): Int {
        tempCanvas.saveLayer(bounds, paint)
        return super.saveLayer(bounds, paint)
    }

    override fun getSaveCount(): Int {
        return saveCounter
    }

    override fun drawTextOnPath(
        text: String,
        path: Path,
        hOffset: Float,
        vOffset: Float,
        paint: Paint,
    ) {
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
    public fun drawTextOnPath(
        text: String,
        path: Path,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawTextOnPath(
            text,
            path,
            hOffset.getFloatIdForCreationState(creationState),
            vOffset.getFloatIdForCreationState(creationState),
        )
    }

    /**
     * Draws text along a given [RemotePath].
     *
     * @param text The text to draw.
     * @param path The [RemotePath] along which to draw the text.
     * @param hOffset The horizontal offset along the path.
     * @param vOffset The vertical offset from the path.
     * @param paint The [Paint] object for styling the text.
     */
    public fun drawTextOnPath(
        text: String,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: Paint,
    ) {
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
    public fun drawTextOnPath(
        text: RemoteString,
        path: Path,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawTextOnPath(
            text.getIdForCreationState(creationState),
            path,
            hOffset.getFloatIdForCreationState(creationState),
            vOffset.getFloatIdForCreationState(creationState),
        )
    }

    /**
     * Draws text from a [RemoteString] along a given [RemotePath].
     *
     * @param text The [RemoteString] to draw.
     * @param path The [RemotePath] along which to draw the text.
     * @param hOffset The horizontal offset along the path.
     * @param vOffset The vertical offset from the path.
     * @param paint The [Paint] object for styling the text.
     */
    public fun drawTextOnPath(
        text: RemoteString,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawTextOnPath(
            text.getIdForCreationState(creationState),
            path,
            hOffset.getFloatIdForCreationState(creationState),
            vOffset.getFloatIdForCreationState(creationState),
        )
    }

    public fun drawTextOnCircle(
        text: RemoteString,
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        startAngle: RemoteFloat,
        warpRadiusOffset: RemoteFloat,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
        paint: Paint,
    ) {
        usePaint(paint)
        document.drawTextOnCircle(
            text.getIdForCreationState(creationState),
            centerX.getFloatIdForCreationState(creationState),
            centerY.getFloatIdForCreationState(creationState),
            radius.getFloatIdForCreationState(creationState),
            startAngle.getFloatIdForCreationState(creationState),
            warpRadiusOffset.getFloatIdForCreationState(creationState),
            alignment,
            placement,
        )
    }

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
        usePaint(paint)
        if (useCenter) {
            document.drawSector(left, top, right, bottom, startAngle, sweepAngle)
        } else {
            document.drawArc(left, top, right, bottom, startAngle, sweepAngle)
        }
    }

    public fun drawArc(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
        useCenter: Boolean,
        paint: Paint,
    ) {
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

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        usePaint(paint)
        document.drawCircle(cx, cy, radius)
    }

    /**
     * Draws a circle at ([cx], [cy]) with the specified [radius] and [paint].
     *
     * @param cx The X-coordinate of the center of the circle.
     * @param cy The Y-coordinate of the center of the circle.
     * @param radius The radius of the circle.
     * @param paint The [Paint] object for styling.
     */
    public fun drawCircle(cx: RemoteFloat, cy: RemoteFloat, radius: RemoteFloat, paint: Paint) {
        usePaint(paint)
        document.drawCircle(
            cx.getFloatIdForCreationState(creationState),
            cy.getFloatIdForCreationState(creationState),
            radius.getFloatIdForCreationState(creationState),
        )
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
    public fun drawAnchoredText(
        text: String,
        anchorX: RemoteFloat,
        anchorY: RemoteFloat,
        panx: RemoteFloat,
        pany: RemoteFloat,
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
    public fun drawAnchoredText(
        text: RemoteString,
        anchorX: RemoteFloat,
        anchorY: RemoteFloat,
        panx: RemoteFloat,
        pany: RemoteFloat,
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
    public fun drawTweenPath(
        path1: androidx.compose.ui.graphics.Path,
        path2: androidx.compose.ui.graphics.Path,
        tween: RemoteFloat,
        start: RemoteFloat,
        stop: RemoteFloat,
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

    /**
     * Draws a path, interpolated using tween, between path1, & path2 [RemotePath] with the given
     * [Paint].
     *
     * @param path1 The starting [RemotePath].
     * @param path2 The ending [RemotePath].
     * @param tween The interpolation factor (0.0 for `path1`, 1.0 for `path2`).
     * @param start The start value for internal tween calculations (often 0.0).
     * @param stop The stop value for internal tween calculations (often 1.0).
     * @param paint The Compose UI [Paint] object for styling.
     */
    public fun drawTweenPath(
        path1: RemotePath,
        path2: RemotePath,
        tween: RemoteFloat,
        start: RemoteFloat,
        stop: RemoteFloat,
        paint: androidx.compose.ui.graphics.Paint,
    ) {
        usePaint(paint.asFrameworkPaint())
        document.drawTweenPath(
            path1,
            path2,
            tween.getFloatIdForCreationState(creationState),
            start.getFloatIdForCreationState(creationState),
            stop.getFloatIdForCreationState(creationState),
        )
    }

    public fun paint(canvas: Canvas) {
        canvas.restoreToCount(1)
    }

    /**
     * Draws a scaled portion of an [Bitmap] into a destination rectangle.
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
    public fun drawScaledBitmap(
        image: Bitmap,
        srcLeft: RemoteFloat,
        srcTop: RemoteFloat,
        srcRight: RemoteFloat,
        srcBottom: RemoteFloat,
        dstLeft: RemoteFloat,
        dstTop: RemoteFloat,
        dstRight: RemoteFloat,
        dstBottom: RemoteFloat,
        scaleType: Int,
        scaleFactor: RemoteFloat,
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
    public fun drawScaledBitmap(
        image: RemoteBitmap,
        srcLeft: RemoteFloat,
        srcTop: RemoteFloat,
        srcRight: RemoteFloat,
        srcBottom: RemoteFloat,
        dstLeft: RemoteFloat,
        dstTop: RemoteFloat,
        dstRight: RemoteFloat,
        dstBottom: RemoteFloat,
        scaleType: Int,
        scaleFactor: RemoteFloat,
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
    public fun loop(
        from: RemoteFloat,
        until: RemoteFloat,
        step: RemoteFloat,
        body: (index: RemoteFloat) -> Unit,
    ) {
        val loopVariableId = document.createFloatId()
        val loopVariable = MutableRemoteFloat(loopVariableId)
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
    public fun loop(from: Int, until: RemoteInt, body: (index: RemoteInt) -> Unit) {
        val loopVariableId = document.createFloatId()
        val loopVariable = MutableRemoteFloat(loopVariableId)
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
    public fun setMatrixFromPath(path: Path, fraction: RemoteFloat, tangentalOffset: RemoteFloat) {
        document.matrixFromPath(
            document.addPathData(path),
            fraction.getFloatIdForCreationState(creationState),
            tangentalOffset.getFloatIdForCreationState(creationState),
            3,
        )
    }

    /**
     * Instructs the player to conditionally execute [drawCommands] if [condition] evaluates to
     * true.
     *
     * @param condition The condition that controls whether or not the player executes
     *   [drawCommands].
     * @param drawCommands The commands the player will execute if [condition] evaluate to true.
     */
    public fun drawConditionally(condition: RemoteBoolean, drawCommands: () -> Unit) {
        document.conditionalOperations(
            ConditionalOperations.TYPE_NEQ,
            condition.toRemoteInt().toRemoteFloat().getFloatIdForCreationState(creationState),
            0f,
        )
        forceSendingPaint(true)
        drawCommands()
        forceSendingPaint(true)
        document.endConditionalOperations()
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap].
     *
     * @param bitmap The [RemoteBitmap] to draw to.
     * @param drawCommands The commands the player will execute in the offscreen buffer.
     */
    public fun drawToOffscreenBitmap(bitmap: RemoteBitmap, drawCommands: () -> Unit) {
        val bitmapId = bitmap.getIdForCreationState(creationState)
        document.drawOnBitmap(bitmapId, 1, 0)

        forceSendingPaint(true)
        val lastDrawToBitmapId = currentDrawToBitmapId
        currentDrawToBitmapId = bitmapId
        drawCommands()
        currentDrawToBitmapId = lastDrawToBitmapId
        forceSendingPaint(true)
        // Switch back to the previous canvas without clearing it.
        document.drawOnBitmap(lastDrawToBitmapId, 1, 0)
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap] which will be cleared with
     * [clearColor] before any [drawCommands] are processed.
     *
     * @param bitmap The [RemoteBitmap] to draw to.
     * @param clearColor The color the created offscreen bitmap will be cleared with.
     * @param drawCommands The commands the player will execute in the offscreen buffer.
     */
    public fun drawToOffscreenBitmap(
        bitmap: RemoteBitmap,
        @ColorInt clearColor: Int,
        drawCommands: () -> Unit,
    ) {
        val bitmapId = bitmap.getIdForCreationState(creationState)
        document.drawOnBitmap(bitmapId, 0, clearColor)

        forceSendingPaint(true)
        val lastDrawToBitmapId = currentDrawToBitmapId
        currentDrawToBitmapId = bitmapId
        drawCommands()
        currentDrawToBitmapId = lastDrawToBitmapId
        forceSendingPaint(true)
        // Switch back to the previous canvas without clearing it.
        document.drawOnBitmap(lastDrawToBitmapId, 1, 0)
    }

    public companion object {
        // TODO replace this with a dedicated color space for RemoteCompose.
        internal const val REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID = 5L
    }
}

private object Api29ColorLongHelper {
    fun getColorLong(paint: Paint, creationState: RemoteComposeCreationState) =
        if (paint is RemotePaint) {
            paint.getColorLong(creationState) ?: paint.colorLong
        } else {
            paint.colorLong
        }
}
