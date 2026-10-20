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
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCustomPropertiesScope
import androidx.compose.remote.creation.compose.layout.drawArc
import androidx.compose.remote.creation.compose.layout.drawBitmap
import androidx.compose.remote.creation.compose.layout.drawBitmapFontTextRun
import androidx.compose.remote.creation.compose.layout.drawBitmapFontTextRunOnPath
import androidx.compose.remote.creation.compose.layout.drawCircle
import androidx.compose.remote.creation.compose.layout.drawColor
import androidx.compose.remote.creation.compose.layout.drawLine
import androidx.compose.remote.creation.compose.layout.drawOval
import androidx.compose.remote.creation.compose.layout.drawPath
import androidx.compose.remote.creation.compose.layout.drawRect
import androidx.compose.remote.creation.compose.layout.drawRoundRect
import androidx.compose.remote.creation.compose.layout.drawText
import androidx.compose.remote.creation.compose.layout.drawTextOnPath
import androidx.compose.remote.creation.compose.layout.drawTextRun
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rf

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

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, left, top, paint)
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
        remoteCanvas.drawBitmap(bitmap, src, dst, paint)
    }

    public fun drawBitmap(bitmap: RemoteImageBitmap, src: Rect?, dst: Rect, paint: Paint?) {
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
        text: RemoteString,
        path: Path,
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
