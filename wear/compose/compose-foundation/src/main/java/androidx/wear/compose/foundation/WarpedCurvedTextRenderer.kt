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

package androidx.wear.compose.foundation

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import android.text.GraphemeClusterSegmentFinder
import android.text.Spannable
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastForEach
import androidx.core.text.TextDirectionHeuristicsCompat
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import androidx.graphics.path.PathIterator
import androidx.graphics.path.PathSegment
import androidx.graphics.path.iterator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Implementation of CurvedTextRenderer using warping. This creates a path from the text using
 * Android's "Paint.getTextPath", then iterates that path and warps each segment. The warping maps
 * from the rectangle each glyph occupies to an annulus segment, so adjacent characters share a
 * line, making this much better for cursive fonts. Note that "getTextOnPath" doesn't work with
 * emojis, so they have to be detected and processed independently. The warpRadiusOffset parameter
 * determines how far away from the baseline is the warping line, which is the horizontal (before
 * warping) line of the text that will maintain its size.
 */
@RequiresApi(29)
internal class WarpedCurvedTextRenderer() : CurvedTextRenderer {
    override fun preRender(
        center: Offset,
        radius: Float,
        clockwise: Boolean,
        sweepDegree: Float,
        startAngleRadians: Float,
        text: String,
        paint: TextPaint,
        warpRadiusOffset: Float,
    ) {
        // Get text direction
        val hasRtl = TextDirectionHeuristicsCompat.ANYRTL_LTR.isRtl(text, 0, text.length)
        val textDir = if (hasRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR

        // Build array of starting positions
        val startingPositions = FloatArray(text.length + 1) // + total advance at end
        paint.getTextRunAdvances(
            text.toCharArray(),
            0,
            text.length,
            0,
            text.length,
            textDir == TextDirectionHeuristics.RTL,
            startingPositions,
            1,
        )
        var sum = 0f
        for (i in startingPositions.indices) {
            sum += startingPositions[i]
            startingPositions[i] = sum
        }
        val totalAdvance = startingPositions[text.length]

        // Ideally, PositionedGlyphs would indicate if a glyph can be represented as a path. However
        // that info is not available to us, so instead we identify which sections of the strings
        // are emojis, and assume that any glyphs that aren't emojis can be represented by a path.
        emojis.clear()
        textRuns.clear()
        EmojiRunProcessorManager.buildRuns(
            text,
            startingPositions,
            totalAdvance,
            paint,
            textRuns,
            emojis,
        )

        // Convert positions for RTL text
        if (textDir == TextDirectionHeuristics.RTL) {
            emojis.fastForEach { emoji ->
                emoji.centeredPosition = totalAdvance - emoji.centeredPosition
            }
            textRuns.fastForEach { run ->
                run.startPosition = totalAdvance - run.startPosition - run.advance
            }
        }

        warpRadius = if (clockwise) radius + warpRadiusOffset else radius - warpRadiusOffset
        this.clockwise = clockwise
        this.center = center
        baselineRadius = radius
        this.startAngle = startAngleRadians + if (clockwise) 0f else sweepDegree.toRadians()
        this.warpRadiusOffset = warpRadiusOffset
    }

    private var textRuns: MutableList<TextRunInfo> = mutableListOf()
    private var emojis: MutableList<EmojiInfo> = mutableListOf()
    private var clockwise: Boolean = true
    private var warpRadius: Float = 0f
    private var center: Offset = Offset.Zero
    private var baselineRadius: Float = 0f
    private var startAngle: Float = 0f
    private var warpRadiusOffset: Float = 0f

    private var textPath = Path()

    override fun render(canvas: Canvas, text: String, paint: TextPaint) {
        // Render warped paths
        val d = if (clockwise) 1f else -1f
        textRuns.fastForEach { run ->
            val tt = text.substring(run.start, run.end)
            val angleAdj = run.startPosition / warpRadius * d

            textPath.reset()
            paint.getTextPath(tt, 0, tt.length, 0.0f, 0.0f, textPath)

            val warpedPath =
                warpPath(
                    textPath.iterator(),
                    run.advance,
                    center.x,
                    center.y,
                    baselineRadius,
                    startAngle + angleAdj,
                    clockwise,
                    warpRadiusOffset,
                )

            canvas.drawPath(warpedPath, paint)
        }

        if (emojis.isNotEmpty()) {
            // Draw emojis
            val prevAlign = paint.textAlign
            paint.textAlign = Paint.Align.CENTER
            if (!clockwise)
                startAngle = 2f * PI.toFloat() - startAngle // account for inside path being CCW
            val dir = if (clockwise) Path.Direction.CW else Path.Direction.CCW
            val circle = Path()
            circle.addCircle(center.x, center.y, baselineRadius, dir)
            val pm = PathMeasure(circle, false)
            emojis.fastForEach { emoji ->
                val warpRadiusDist = warpRadius * startAngle + emoji.centeredPosition
                val baselineDist = (warpRadiusDist * baselineRadius / warpRadius).mod(pm.length)

                val pos = FloatArray(2)
                val tan = FloatArray(2)
                pm.getPosTan(baselineDist, pos, tan)

                canvas.save()
                canvas.rotate(atan2(tan[1], tan[0]) * 180f / PI.toFloat(), pos[0], pos[1])
                canvas.drawText(emoji.sequence.toString(), pos[0], pos[1], paint)
                canvas.restore()
            }
            paint.textAlign = prevAlign
        }
    }
}

private class EmojiRunProcessorManager {
    companion object {
        val processor: EmojiRunProcessor by lazy {
            if (Build.VERSION.SDK_INT >= 34) {
                EmojiRunProcessorGCSF()
            } else {
                EmojiRunProcessorEC()
            }
        }

        fun buildRuns(
            text: String,
            startingPositions: FloatArray,
            totalAdvance: Float,
            paint: TextPaint,
            textRuns: MutableList<TextRunInfo>,
            emojis: MutableList<EmojiInfo>,
        ) = processor.buildRuns(text, startingPositions, totalAdvance, paint, textRuns, emojis)
    }

    internal interface EmojiRunProcessor {
        fun buildRuns(
            text: String,
            startingPositions: FloatArray,
            totalAdvance: Float,
            paint: TextPaint,
            textRuns: MutableList<TextRunInfo>,
            emojis: MutableList<EmojiInfo>,
        )
    }

    // Fallback for when everything else fails. Mark all text as non-emoji.
    internal class EmojiRunProcessorBase() : EmojiRunProcessor {
        override fun buildRuns(
            text: String,
            startingPositions: FloatArray,
            totalAdvance: Float,
            paint: TextPaint,
            textRuns: MutableList<TextRunInfo>,
            emojis: MutableList<EmojiInfo>,
        ) {}
    }

    // Builds text and emoji runs using EmojiCompat (requires Android API 19)
    internal class EmojiRunProcessorEC() : EmojiRunProcessor {
        override fun buildRuns(
            text: String,
            startingPositions: FloatArray,
            totalAdvance: Float,
            paint: TextPaint,
            textRuns: MutableList<TextRunInfo>,
            emojis: MutableList<EmojiInfo>,
        ) {
            val ec = EmojiCompat.get()
            if (ec.loadState != EmojiCompat.LOAD_STATE_SUCCEEDED) {
                textRuns.add(
                    TextRunInfo(
                        start = 0,
                        end = text.length,
                        startPosition = 0f,
                        advance = totalAdvance,
                    )
                )
                return
            }
            val processed = ec.process(text)
            var lastIdx = 0
            if (processed != null && processed is Spannable) {
                val spans = processed.getSpans(0, processed.length, EmojiSpan::class.java)
                for (span in spans) {
                    val spanStart: Int = processed.getSpanStart(span)
                    val spanEnd: Int = processed.getSpanEnd(span)
                    val emojiSubStr = processed.subSequence(spanStart, spanEnd)

                    val emojiStart = startingPositions[spanStart]
                    val emojiEnd = startingPositions[spanEnd]

                    // Create text run from the last emoji seen to this one
                    textRuns.add(
                        TextRunInfo(
                            lastIdx,
                            spanStart,
                            startingPositions[lastIdx],
                            emojiStart - startingPositions[lastIdx],
                        )
                    )

                    // Create emoji info
                    emojis.add(EmojiInfo(emojiSubStr, emojiStart + (emojiEnd - emojiStart) / 2f))

                    lastIdx = spanEnd
                }
            }

            // Add the final text run
            textRuns.add(
                TextRunInfo(
                    lastIdx,
                    text.length,
                    startingPositions[lastIdx],
                    totalAdvance - startingPositions[lastIdx],
                )
            )
        }
    }

    /**
     * Builds text and emoji runs using GraphemeClusterSegmentFinder (requires Android API 34).
     * Prefer this over EmojiCompat, since that requires some initialization time.
     */
    @RequiresApi(34)
    internal class EmojiRunProcessorGCSF : EmojiRunProcessor {
        override fun buildRuns(
            text: String,
            startingPositions: FloatArray,
            totalAdvance: Float,
            paint: TextPaint,
            textRuns: MutableList<TextRunInfo>,
            emojis: MutableList<EmojiInfo>,
        ) {
            val gcsf = GraphemeClusterSegmentFinder(text, paint)
            var start = 0
            var textStart: Int? = null
            while (start < text.length) {
                val end = gcsf.nextEndBoundary(start)

                // check first codepoint to see if this is emoji
                val emojiProp =
                    UCharacter.getIntPropertyValue(
                        UCharacter.codePointAt(text.toCharArray(), start, end),
                        UProperty.EMOJI_PRESENTATION,
                    )

                if (emojiProp == 1) { // we've reached an emoji
                    if (textStart != null) { // add the preceding text run
                        textRuns.add(
                            TextRunInfo(
                                textStart,
                                start,
                                startingPositions[textStart],
                                startingPositions[start] - startingPositions[textStart],
                            )
                        )
                        textStart = null
                    }

                    // add the emoji
                    val emojiStart = startingPositions[start]
                    val emojiEnd = startingPositions[end]
                    emojis.add(
                        EmojiInfo(
                            text.substring(start, end),
                            emojiStart + (emojiEnd - emojiStart) / 2f,
                        )
                    )
                } else {
                    textStart = textStart ?: start
                }

                start = end
            }

            // add the final text run, if necessary
            if (textStart != null) { // add the preceding text run
                textRuns.add(
                    TextRunInfo(
                        textStart,
                        text.length,
                        startingPositions[textStart],
                        totalAdvance - startingPositions[textStart],
                    )
                )
            }
        }
    }
}

private data class EmojiInfo(
    val sequence: CharSequence, // The emoji substring
    var centeredPosition: Float, // The centered position of the emoji (within the main string)
)

private data class TextRunInfo(
    val start: Int,
    val end: Int,
    var startPosition: Float,
    var advance: Float,
)

@RequiresApi(29)
internal fun warpPath(
    path: PathIterator,
    totalAdvance: Float,
    cx: Float,
    cy: Float,
    radius: Float,
    alignmentAngle: Float,
    clockwise: Boolean,
    warpRadiusOffset: Float,
): Path {
    val warpRadius = if (clockwise) radius + warpRadiusOffset else radius - warpRadiusOffset
    val warper = CircleWarper(Offset(cx, cy), warpRadius, alignmentAngle, clockwise)

    val retPath = Path()

    val cubic = FloatArray(8)
    val points = FloatArray(8)
    while (path.hasNext()) {
        val next = path.next(points)
        // Move y according to the warpRadiusOffset
        repeat(4) { points[it * 2 + 1] += warpRadiusOffset }

        var doCubic = false
        when (next) {
            PathSegment.Type.Move -> {
                warper.warpPoint(points)
                retPath.moveTo(points[0], points[1])
            }
            PathSegment.Type.Line -> {
                lerpPoint(cubic, 0, points, 0, 2, 0f)
                lerpPoint(cubic, 2, points, 0, 2, 1f / 3)
                lerpPoint(cubic, 4, points, 0, 2, 2f / 3)
                lerpPoint(cubic, 6, points, 0, 2, 1f)
                doCubic = true
            }
            PathSegment.Type.Quadratic -> {
                lerpPoint(cubic, 0, points, 0, 2, 0f)
                lerpPoint(cubic, 2, points, 0, 2, 2f / 3)
                lerpPoint(cubic, 4, points, 2, 4, 1f / 3)
                lerpPoint(cubic, 6, points, 2, 4, 1f)
                doCubic = true
            }
            PathSegment.Type.Cubic -> {
                points.copyInto(cubic)
                doCubic = true
            }
            PathSegment.Type.Conic -> throw UnsupportedOperationException()
            PathSegment.Type.Close -> retPath.close()
            PathSegment.Type.Done -> return retPath
        }
        if (doCubic) {
            warper.warpCubic(cubic)
            retPath.cubicTo(cubic[2], cubic[3], cubic[4], cubic[5], cubic[6], cubic[7])
        }
    }
    return retPath
}

private fun lerpPoint(
    target: FloatArray,
    targetIx: Int,
    src: FloatArray,
    srcIx0: Int,
    srcIx1: Int,
    t: Float,
) {
    target[targetIx] = src[srcIx0] * (1 - t) + src[srcIx1] * t
    target[targetIx + 1] = src[srcIx0 + 1] * (1 - t) + src[srcIx1 + 1] * t
}

internal class CircleWarper(
    val center: Offset,
    val r: Float,
    val angle: Float,
    val clockwise: Boolean,
) {
    fun warpPoint(p: Offset): Offset {
        val angleVector = angleVector(p)
        val r0 = r - p.y * counterClockwiseSign

        return angleVector * r0 + center
    }

    fun warpPoint(arr: FloatArray, i: Int = 0) {
        val w = warpPoint(Offset(arr[i], arr[i + 1]))
        arr[i] = w.x
        arr[i + 1] = w.y
    }

    fun warpCubic(cubic: FloatArray) {
        if (
            (pointEqualish(cubic, 2, 4) &&
                (pointEqualish(cubic, 0, 2) || pointEqualish(cubic, 4, 6)))
        ) {
            repeat(4) { warpPoint(cubic, it * 2) }
        } else {
            warpPoints(cubic, 0, 2)
            warpPoints(cubic, 6, 4)
        }
    }

    private fun angleVector(point: Offset): Offset {
        val a0 = point.x * counterClockwiseSign / r + angle
        return Offset(cos(a0), sin(a0))
    }

    private fun pointEqualish(arr: FloatArray, i: Int, j: Int) =
        abs(arr[i] - arr[j]) < DistanceEpsilon && abs(arr[i + 1] - arr[j + 1]) < DistanceEpsilon

    private fun warpPoints(arr: FloatArray, i: Int, j: Int) {
        val point = Offset(arr[i], arr[i + 1])
        val target = Offset(arr[j], arr[j + 1])

        val angleVector = angleVector(point)
        val r0 = r - point.y * counterClockwiseSign

        val retPoint = angleVector * r0 + center
        arr[i] = retPoint.x
        arr[i + 1] = retPoint.y

        val scale = r0 / r
        val tan = (target - point) * counterClockwiseSign
        val retTarget = retPoint + angleVector.rotate90() * scale * tan.x - angleVector * tan.y

        arr[j] = retTarget.x
        arr[j + 1] = retTarget.y
    }

    private val counterClockwiseSign = if (clockwise) 1f else -1f

    private fun Offset.rotate90() = Offset(-y, x)
}

private const val DistanceEpsilon = 1e-4f
