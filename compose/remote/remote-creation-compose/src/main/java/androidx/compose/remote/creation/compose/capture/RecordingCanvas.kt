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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCustomPropertiesScope
import androidx.compose.remote.creation.compose.layout.RemotePattern0
import androidx.compose.remote.creation.compose.layout.RemotePattern1
import androidx.compose.remote.creation.compose.layout.RemotePattern2
import androidx.compose.remote.creation.compose.layout.RemotePattern3
import androidx.compose.remote.creation.compose.layout.RemotePattern4
import androidx.compose.remote.creation.compose.layout.RemotePattern5
import androidx.compose.remote.creation.compose.layout.RemotePattern6
import androidx.compose.remote.creation.compose.layout.drawArc
import androidx.compose.remote.creation.compose.layout.drawBitmap
import androidx.compose.remote.creation.compose.layout.drawBitmapFontTextRun
import androidx.compose.remote.creation.compose.layout.drawBitmapFontTextRunOnPath
import androidx.compose.remote.creation.compose.layout.drawCircle
import androidx.compose.remote.creation.compose.layout.drawColor
import androidx.compose.remote.creation.compose.layout.drawLine
import androidx.compose.remote.creation.compose.layout.drawOval
import androidx.compose.remote.creation.compose.layout.drawPath
import androidx.compose.remote.creation.compose.layout.drawRPath
import androidx.compose.remote.creation.compose.layout.drawRect
import androidx.compose.remote.creation.compose.layout.drawRoundRect
import androidx.compose.remote.creation.compose.layout.drawRoundedPolygon
import androidx.compose.remote.creation.compose.layout.drawRoundedPolygonMorph
import androidx.compose.remote.creation.compose.layout.drawScaledBitmap
import androidx.compose.remote.creation.compose.layout.drawText
import androidx.compose.remote.creation.compose.layout.drawTextOnPath
import androidx.compose.remote.creation.compose.layout.drawTextRun
import androidx.compose.remote.creation.compose.layout.drawTweenPath
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.BaseRemoteState
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.graphics.shapes.RoundedPolygon

/**
 * A bridge for code calling standard [android.graphics.Canvas] to record drawing commands into a
 * remote document.
 *
 * This implementation intercepts standard [Canvas] methods and delegates recording operations to an
 * underlying [RemoteCanvas]. It allows legacy or framework-dependent code that uses standard
 * platform types to be recorded.
 *
 * For most remote-compose development,
 * [androidx.compose.remote.creation.compose.layout.RemoteCanvas] is the main way to interact with
 * the recording system, as it provides a remote-first API with overloads for [RemoteFloat],
 * [androidx.compose.remote.creation.compose.state.RemoteColor], etc.
 *
 * Note [flush] MUST be called to commit commands to the underlying document.
 *
 * @param bitmap The backing [Bitmap] for the Android [Canvas].
 * @param creationState Optional [RemoteComposeCreationState] to initialize [creationState].
 * @param enableOptimizations Whether to enable save/restore elision and transform fusing
 *   optimizations. Defaults to `false`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RecordingCanvas(
    bitmap: Bitmap,
    internal val creationState: RemoteComposeCreationState,
    public val enableOptimizations: Boolean = false,
) : Canvas(bitmap), RemoteStateScope by creationState {

    internal val remoteCanvas: RemoteCanvas =
        RemoteCanvas(
            creationState = creationState,
            enableOptimizations = enableOptimizations,
        )

    internal val buffer: CanvasOperationBuffer = remoteCanvas.buffer

    public val creationDisplayInfo: RemoteCreationDisplayInfo
        get() = remoteCanvas.creationDisplayInfo

    /**
     * Flushes all buffered operations to the document, applying optimizations like common
     * subexpression elimination and operation hoisting.
     */
    public fun flush() {
        remoteCanvas.flush()
    }

    @VisibleForTesting
    public fun usePaint(paint: Paint?) {
        remoteCanvas.usePaint(paint?.asRemotePaint())
    }

    @VisibleForTesting
    public fun usePaint(paint: RemotePaint?) {
        remoteCanvas.usePaint(paint)
    }

    override fun drawColor(drawColor: Int) {
        remoteCanvas.drawColor(drawColor)
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        remoteCanvas.drawText(text, x, y, paint)
    }

    public fun drawText(text: String, x: RemoteFloat, y: RemoteFloat, paint: Paint) {
        remoteCanvas.drawText(text, x, y, paint)
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
        remoteCanvas.drawText(text, length, x, y, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        remoteCanvas.drawRect(left, top, right, bottom, paint)
    }

    public fun drawRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: Paint,
    ) {
        remoteCanvas.drawRect(left, top, right, bottom, paint)
    }

    override fun drawRect(rect: Rect, paint: Paint) {
        remoteCanvas.drawRect(rect, paint)
    }

    override fun drawRect(rect: RectF, paint: Paint) {
        remoteCanvas.drawRect(rect, paint)
    }

    /** For V1 compatibility. */
    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        remoteCanvas.drawOval(left, top, right, bottom, paint)
    }

    public fun drawOval(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: Paint,
    ) {
        remoteCanvas.drawOval(left, top, right, bottom, paint)
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
        remoteCanvas.drawRoundRect(left, top, right, bottom, rx, ry, paint)
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
        remoteCanvas.drawRoundRect(left, top, right, bottom, rx, ry, paint)
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        remoteCanvas.drawLine(startX, startY, stopX, stopY, paint)
    }

    public fun drawLine(
        startX: RemoteFloat,
        startY: RemoteFloat,
        stopX: RemoteFloat,
        stopY: RemoteFloat,
        paint: Paint,
    ) {
        remoteCanvas.drawLine(startX, startY, stopX, stopY, paint)
    }

    override fun translate(dx: Float, dy: Float) {
        if (dx != 0f || dy != 0f) {
            remoteCanvas.translate(dx.rf, dy.rf)
        }
    }

    public fun translate(dx: RemoteFloat, dy: RemoteFloat) {
        remoteCanvas.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        remoteCanvas.scale(sx.rf, sy.rf)
    }

    public fun scale(sx: RemoteFloat, sy: RemoteFloat) {
        remoteCanvas.scale(sx, sy)
    }

    public fun scale(sx: RemoteFloat, sy: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
        remoteCanvas.scale(sx, sy, px, py)
    }

    override fun skew(sx: Float, sy: Float) {
        skew(sx.rf, sy.rf)
    }

    public fun skew(sx: RemoteFloat, sy: RemoteFloat) {
        remoteCanvas.skew(sx, sy)
    }

    public fun drawBitmap(bitmap: ImageBitmap, left: Float, top: Float, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, left, top, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, left, top, paint)
    }

    public fun drawBitmap(
        bitmap: RemoteImageBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        paint: Paint?,
    ) {
        remoteCanvas.drawBitmap(bitmap, left, top, paint)
    }

    public fun drawBitmap(bitmap: ImageBitmap, src: Rect?, dst: Rect, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    public fun drawBitmap(bitmap: RemoteImageBitmap, src: Rect?, dst: Rect, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    public fun drawBitmap(bitmap: ImageBitmap, src: Rect?, dst: RectF, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    public fun drawBitmap(
        bitmap: Bitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: Paint?,
    ) {
        remoteCanvas.drawBitmap(bitmap, left, top, right, bottom, paint)
    }

    /**
     * Draws a [RemotePath] onto the canvas using the specified [Paint].
     *
     * @param path The [RemotePath] to draw.
     * @param paint The [Paint] object to use for drawing the path.
     */
    public fun drawRPath(path: RemotePath, paint: Paint) {
        remoteCanvas.drawRPath(path, paint)
    }

    /**
     * Draws a [RoundedPolygon] onto the canvas using the specified [Paint].
     *
     * @param roundedPolygon The [RoundedPolygon] to draw.
     * @param paint The [Paint] object to use for drawing the polygon.
     */
    public fun drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
        remoteCanvas.drawRoundedPolygon(roundedPolygon, paint)
    }

    /**
     * Draws a morph between two [RoundedPolygon]s onto the canvas using the specified [Paint].
     *
     * @param from The starting [RoundedPolygon].
     * @param to The ending [RoundedPolygon].
     * @param progress The morph progress [0..1].
     * @param paint The [Paint] object to use for drawing the morph.
     */
    public fun drawRoundedPolygonMorph(
        from: RoundedPolygon,
        to: RoundedPolygon,
        progress: RemoteFloat,
        paint: RemotePaint?,
    ) {
        remoteCanvas.drawRoundedPolygonMorph(from, to, progress, paint)
    }

    override fun save(): Int = remoteCanvas.save()

    override fun restore(): Unit = remoteCanvas.restore()

    override fun restoreToCount(saveCount: Int): Unit = remoteCanvas.restoreToCount(saveCount)

    override fun getClipBounds(bounds: Rect): Boolean {
        bounds.set(0, 0, 2048, 2048)
        return true
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
        remoteCanvas.clipRect(left.rf, top.rf, right.rf, bottom.rf)
        return true
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
        remoteCanvas.clipRect(left.rf, top.rf, right.rf, bottom.rf)
        return true
    }

    public fun clipRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
    ): Boolean {
        remoteCanvas.clipRect(left, top, right, bottom)
        return true
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
        remoteCanvas.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint)
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
        remoteCanvas.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint)
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
     * @param y The top coordinate to start rendering from
     * @param glyphSpacing Horizontal adjustment in pixels between glyphs
     * @param paint The [Paint] to render with
     */
    public fun drawBitmapFontTextRun(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        start: Int,
        end: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        glyphSpacing: RemoteFloat,
        paint: Paint,
    ) {
        remoteCanvas.drawBitmapFontTextRun(text, bitmapFont, start, end, x, y, glyphSpacing, paint)
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
     * @param glyphSpacing Horizontal adjustment in pixels between glyphs
     * @param paint The [Paint] to render with
     */
    public fun drawBitmapFontTextRunOnPath(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        path: Path,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float,
        paint: Paint,
    ) {
        remoteCanvas.drawBitmapFontTextRunOnPath(
            text,
            bitmapFont,
            path,
            start,
            end,
            yAdj,
            glyphSpacing,
            paint,
        )
    }

    override fun drawPath(path: Path, paint: Paint) {
        remoteCanvas.drawPath(path, paint)
    }

    override fun rotate(degrees: Float) {
        remoteCanvas.rotate(degrees.rf)
    }

    /**
     * Applies a rotation transformation to the canvas.
     *
     * @param degrees The angle of rotation in degrees.
     */
    public fun rotate(degrees: RemoteFloat) {
        remoteCanvas.rotate(degrees)
    }

    /**
     * Applies a rotation transformation to the canvas around a specified pivot point.
     *
     * @param degrees The angle of rotation in degrees.
     * @param px The X-coordinate of the pivot point.
     * @param py The Y-coordinate of the pivot point.
     */
    public fun rotate(degrees: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
        remoteCanvas.rotate(degrees, px, py)
    }

    override fun getSaveCount(): Int = remoteCanvas.saveCount

    override fun drawTextOnPath(
        text: String,
        path: Path,
        hOffset: Float,
        vOffset: Float,
        paint: Paint,
    ) {
        remoteCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
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
        remoteCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
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
        remoteCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
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
        remoteCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
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
        remoteCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
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
        remoteCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint)
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
        remoteCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        remoteCanvas.drawCircle(cx, cy, radius, paint)
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
        remoteCanvas.drawCircle(cx, cy, radius, paint)
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
        remoteCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
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
        remoteCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
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
        remoteCanvas.drawScaledBitmap(
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

    /**
     * Draws a scaled portion of a [RemoteImageBitmap] into a destination rectangle.
     *
     * @param image The [RemoteImageBitmap] to draw.
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
        image: RemoteImageBitmap,
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
        remoteCanvas.drawScaledBitmap(
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
        remoteCanvas.loop(from, until, step, body)
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
        remoteCanvas.loop(from, until, body)
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
        remoteCanvas.drawConditionally(condition, drawCommands)
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap].
     *
     * @param bitmap The [RemoteImageBitmap] to draw to.
     * @param drawCommands The commands the player will execute in the offscreen buffer. Profiler
     *   hooks will be executed.
     */
    public fun drawToOffscreenBitmap(bitmap: RemoteImageBitmap, drawCommands: () -> Unit) {
        remoteCanvas.drawToOffscreenBitmap(bitmap, drawCommands)
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap] which will be cleared with
     * [clearColor] before any [drawCommands] are processed.
     *
     * @param bitmap The [RemoteImageBitmap] to draw to.
     * @param clearColor The color the created offscreen bitmap will be cleared with.
     * @param drawCommands The commands the player will execute in the offscreen buffer.
     */
    public fun drawToOffscreenBitmap(
        bitmap: RemoteImageBitmap,
        @ColorInt clearColor: Int,
        drawCommands: () -> Unit,
    ) {
        remoteCanvas.drawToOffscreenBitmap(bitmap, clearColor, drawCommands)
    }

    /**
     * Defines a reusable drawing pattern with no parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template.
     *
     * @param action drawing commands to record into the pattern template
     * @return a [RemotePattern0] that inflates the recorded pattern
     */
    public fun definePattern(action: () -> Unit): RemotePattern0 =
        remoteCanvas.definePattern(action)

    /**
     * Defines a reusable drawing pattern with 1 parameter.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided argument.
     *
     * @param action drawing commands parameterized by [T1]
     * @return a [RemotePattern1] that inflates the recorded pattern
     */
    public fun <T1 : BaseRemoteState<*>> definePattern(action: (T1) -> Unit): RemotePattern1<T1> =
        remoteCanvas.definePattern(action)

    /**
     * Defines a reusable drawing pattern with 2 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1] and [T2]
     * @return a [RemotePattern2] that inflates the recorded pattern
     */
    public fun <T1 : BaseRemoteState<*>, T2 : BaseRemoteState<*>> definePattern(
        action: (T1, T2) -> Unit
    ): RemotePattern2<T1, T2> = remoteCanvas.definePattern(action)

    /**
     * Defines a reusable drawing pattern with 3 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1], [T2], and [T3]
     * @return a [RemotePattern3] that inflates the recorded pattern
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
    > definePattern(action: (T1, T2, T3) -> Unit): RemotePattern3<T1, T2, T3> =
        remoteCanvas.definePattern(action)

    /**
     * Defines a reusable drawing pattern with 4 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1], [T2], [T3], and [T4]
     * @return a [RemotePattern4] that inflates the recorded pattern
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
        T4 : BaseRemoteState<*>,
    > definePattern(action: (T1, T2, T3, T4) -> Unit): RemotePattern4<T1, T2, T3, T4> =
        remoteCanvas.definePattern(action)

    /**
     * Defines a reusable drawing pattern with 5 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1], [T2], [T3], [T4], and [T5]
     * @return a [RemotePattern5] that inflates the recorded pattern
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
        T4 : BaseRemoteState<*>,
        T5 : BaseRemoteState<*>,
    > definePattern(action: (T1, T2, T3, T4, T5) -> Unit): RemotePattern5<T1, T2, T3, T4, T5> =
        remoteCanvas.definePattern(action)

    /**
     * Defines a reusable drawing pattern with 6 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1] through [T6]
     * @return a [RemotePattern6] that inflates the recorded pattern
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
        T4 : BaseRemoteState<*>,
        T5 : BaseRemoteState<*>,
        T6 : BaseRemoteState<*>,
    > definePattern(
        action: (T1, T2, T3, T4, T5, T6) -> Unit
    ): RemotePattern6<T1, T2, T3, T4, T5, T6> = remoteCanvas.definePattern(action)

    /**
     * Iterates over [items] and records [action] for each element.
     *
     * @param items list of items to iterate over
     * @param action drawing commands to record for each element
     */
    public fun <T : BaseRemoteState<*>> forEach(items: List<T>, action: (T) -> Unit) {
        remoteCanvas.forEach(items, action)
    }

    /**
     * Iterates over [array] and records [action] for each float element.
     *
     * @param array remote float array to iterate over
     * @param action drawing commands to record for each element
     */
    public fun forEach(array: RemoteFloatArray, action: (RemoteFloat) -> Unit) {
        remoteCanvas.forEach(array, action)
    }

    /**
     * Iterates over [array] and records [action] for each string element.
     *
     * @param array remote string array to iterate over
     * @param action drawing commands to record for each element
     */
    public fun forEach(array: RemoteStringArray, action: (RemoteString) -> Unit) {
        remoteCanvas.forEach(array, action)
    }

    /**
     * Iterates over [array] and records [action] for each integer element.
     *
     * @param array remote integer array to iterate over
     * @param action drawing commands to record for each element
     */
    public fun forEach(array: RemoteIntArray, action: (RemoteInt) -> Unit) {
        remoteCanvas.forEach(array, action)
    }

    /**
     * Iterates over [items1] and [items2] pairwise and records [action] for each pair.
     *
     * Iteration stops at the length of the shorter list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param action drawing commands to record for each pair
     */
    public fun <T1 : BaseRemoteState<*>, T2 : BaseRemoteState<*>> forEach(
        items1: List<T1>,
        items2: List<T2>,
        action: (T1, T2) -> Unit,
    ) {
        remoteCanvas.forEach(items1, items2, action)
    }

    /**
     * Iterates over [items1], [items2], and [items3] and records [action] for each triple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param action drawing commands to record for each triple
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        action: (T1, T2, T3) -> Unit,
    ) {
        remoteCanvas.forEach(items1, items2, items3, action)
    }

    /**
     * Iterates over [items1], [items2], [items3], and [items4] and records [action] for each
     * 4-tuple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param items4 fourth list of items to iterate over
     * @param action drawing commands to record for each 4-tuple
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
        T4 : BaseRemoteState<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        items4: List<T4>,
        action: (T1, T2, T3, T4) -> Unit,
    ) {
        remoteCanvas.forEach(items1, items2, items3, items4, action)
    }

    /**
     * Iterates over [items1], [items2], [items3], [items4], and [items5] and records [action] for
     * each 5-tuple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param items4 fourth list of items to iterate over
     * @param items5 fifth list of items to iterate over
     * @param action drawing commands to record for each 5-tuple
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
        T4 : BaseRemoteState<*>,
        T5 : BaseRemoteState<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        items4: List<T4>,
        items5: List<T5>,
        action: (T1, T2, T3, T4, T5) -> Unit,
    ) {
        remoteCanvas.forEach(items1, items2, items3, items4, items5, action)
    }

    /**
     * Iterates over [items1] through [items6] and records [action] for each 6-tuple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param items4 fourth list of items to iterate over
     * @param items5 fifth list of items to iterate over
     * @param items6 sixth list of items to iterate over
     * @param action drawing commands to record for each 6-tuple
     */
    public fun <
        T1 : BaseRemoteState<*>,
        T2 : BaseRemoteState<*>,
        T3 : BaseRemoteState<*>,
        T4 : BaseRemoteState<*>,
        T5 : BaseRemoteState<*>,
        T6 : BaseRemoteState<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        items4: List<T4>,
        items5: List<T5>,
        items6: List<T6>,
        action: (T1, T2, T3, T4, T5, T6) -> Unit,
    ) {
        remoteCanvas.forEach(items1, items2, items3, items4, items5, items6, action)
    }

    /**
     * Records a custom component layout operation.
     *
     * @param config The configuration string for the custom component.
     * @param modifier The [RemoteModifier] applied to this component.
     * @param content Optional drawing commands to record inside the custom component layout.
     * @param properties Scope for configuring custom component properties and return bindings.
     */
    public fun custom(
        config: String,
        modifier: RemoteModifier = RemoteModifier,
        content: (() -> Unit)? = null,
        properties: RemoteCustomPropertiesScope.() -> Unit = {},
    ) {
        remoteCanvas.custom(config, modifier, content, properties)
    }
}
