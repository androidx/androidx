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

package androidx.compose.remote.creation.compose.state

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Environment
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RecordingRemoteComposeBuffer
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.test.R
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import java.io.File
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WIDTH = 800
private const val HEIGHT = 800

/** Emulator-based screenshot test of [RecordingCanvas]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemoteBitmapFontTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private val recordingBuffer = RecordingRemoteComposeBuffer()
    private val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriter(
                profile,
                recordingBuffer,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        }

    private val creationState =
        RemoteComposeCreationState(Size(WIDTH.toFloat(), HEIGHT.toFloat()), profile)

    private val recordingCanvas =
        RecordingCanvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val font = ResourcesCompat.getFont(context, R.font.karla_regular)!!

    private val remoteContext = AndroidRemoteContext()
    private val timeZone = ZoneId.of("America/New_York")
    private val clock =
        Clock.fixed(
            ZonedDateTime.of(LocalDateTime.of(2025, 11, 20, 10, 30, 25), timeZone).toInstant(),
            timeZone,
        )

    @Before
    fun setUp() {
        recordingCanvas.setRemoteComposeCreationState(creationState)
    }

    @Test
    fun drawBitmapFontTextRun_glyphSpacingTest() {
        val text = RemoteString("Lorem Ipsum")
        val bitmapFont =
            createBitmapFont(text.computeRequiredCodePointSet(creationState)!!, font, 80.0f)
        val paint = Paint()

        for (i in -4 until 5) {
            recordingCanvas.drawBitmapFontTextRun(
                text,
                bitmapFont,
                start = 0,
                end = -1,
                x = RemoteFloat(10f),
                y = RemoteFloat(420f + 90f * i.toFloat()),
                glyphSpacing = RemoteFloat(i.toFloat() * 5f),
                paint,
            )
        }

        val document = constructDocument()
        saveDocument(document.buffer, "drawBitmapFontTextRun.rc")
        assertScreenshot(document, "drawBitmapFontTextRun_glyphSpacingTest")
    }

    @Test
    fun drawBitmapFontTextRunOnPath_glyphSpacingTest() {
        val text = RemoteString("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
        val bitmapFont =
            createBitmapFont(text.computeRequiredCodePointSet(creationState)!!, font, 40.0f)
        val paint = Paint()

        recordingCanvas.drawBitmapFontTextRunOnPath(
            text,
            bitmapFont,
            createSpiralPath(200f, 200f, 160f, 20f, 3f),
            start = 0,
            end = -1,
            yAdj = 0f,
            glyphSpacing = -10f,
            paint,
        )

        recordingCanvas.drawBitmapFontTextRunOnPath(
            text,
            bitmapFont,
            createSpiralPath(600f, 200f, 160f, 20f, 3f),
            start = 0,
            end = -1,
            yAdj = 0f,
            glyphSpacing = -7f,
            paint,
        )

        recordingCanvas.drawBitmapFontTextRunOnPath(
            text,
            bitmapFont,
            createSpiralPath(200f, 600f, 180f, 20f, 3f),
            start = 0,
            end = -1,
            yAdj = 0f,
            glyphSpacing = -3f,
            paint,
        )

        recordingCanvas.drawBitmapFontTextRunOnPath(
            text,
            bitmapFont,
            createSpiralPath(600f, 600f, 180f, 20f, 3f),
            start = 0,
            end = -1,
            yAdj = 0f,
            glyphSpacing = 0f,
            paint,
        )

        val document = constructDocument()
        saveDocument(document.buffer, "drawBitmapFontTextRunOnPath.rc")
        assertScreenshot(document, "drawBitmapFontTextRunOnPath_glyphSpacingTest")
    }

    private fun constructDocument() =
        CoreDocument(clock).apply {
            recordingBuffer.writeToBuffer()
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
        }

    private fun assertScreenshot(document: CoreDocument, filename: String) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        remoteContext.useCanvas(canvas)
        document.paint(remoteContext, 0)
        bitmap.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$filename")
    }

    fun saveDocument(buffer: RemoteComposeBuffer, name: String) {
        val filePath =
            context.getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)[0].getAbsolutePath()
        val myFile = File(filePath, name)
        buffer.write(buffer, myFile)
        System.err.println("<<< Saved RC doc " + myFile.getAbsolutePath())
    }
}

internal fun createBitmapFont(
    codePoints: Set<String>,
    typeface: Typeface,
    textSize: Float,
): RemoteBitmapFont {
    val paint = Paint()
    paint.typeface = typeface
    paint.textSize = textSize
    val glyphs = codePoints.map { renderGlyph(paint, it, textSize) }
    val kerningTable = HashMap<String, Short>()
    for (a in glyphs) {
        for (b in glyphs) {
            val glyphPair = a.chars + b.chars
            val sizeAB = paint.measureText(glyphPair, 0, glyphPair.length).toInt()
            if (a.bitmap == null || b.bitmap == null) {
                continue
            }
            val kerningAdjustment = sizeAB - a.bitmap!!.width - b.bitmap!!.width
            if (kerningAdjustment != 0) {
                kerningTable.put(glyphPair, kerningAdjustment.toShort())
            }
        }
    }
    return RemoteBitmapFont(glyphs, kerningTable)
}

private fun renderGlyph(paint: Paint, text: String, textSize: Float): RemoteBitmapFont.Glyph {
    if (text == " ") {
        return RemoteBitmapFont.Glyph(text, null, textSize.toInt().toShort(), 0, 0, 0)
    }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val width = ceil(bounds.width().toFloat()).toInt()
    val height = ceil(bounds.height().toFloat()).toInt()
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawText(text, /* x= */ -bounds.left.toFloat(), /* y= */ -bounds.top.toFloat(), paint)
    return RemoteBitmapFont.Glyph(
        text,
        resultBitmap,
        bounds.left.toShort(),
        bounds.top.toShort(),
        0,
        0,
    )
}

/**
 * Creates a Path object representing a spiral.
 *
 * @param centerX The x-coordinate of the spiral's center.
 * @param centerY The y-coordinate of the spiral's center.
 * @param startRadius The distance from the center where the spiral begins.
 * @param endRadius The distance from the center where the spiral ends.
 * @param revolutions The total number of full 360-degree turns.
 * @param segmentsPerRevolution The number of line segments used to draw each full turn. More
 *   segments result in a smoother curve.
 * @return A Path object representing the calculated spiral.
 */
private fun createSpiralPath(
    centerX: Float,
    centerY: Float,
    startRadius: Float,
    endRadius: Float,
    revolutions: Float,
    segmentsPerRevolution: Int = 50,
): Path {
    val path = Path()
    val totalAngle = revolutions * 2 * Math.PI.toFloat()
    val totalSegments = (segmentsPerRevolution * revolutions).toInt()
    val startX = centerX + startRadius
    path.moveTo(startX, centerY)
    for (i in 1..totalSegments) {
        val progress = i.toFloat() / totalSegments
        val currentAngle = progress * totalAngle
        val currentRadius = startRadius + (endRadius - startRadius) * progress
        val x = centerX + currentRadius * cos(currentAngle)
        val y = centerY + currentRadius * sin(currentAngle)
        path.lineTo(x, y)
    }
    return path
}
