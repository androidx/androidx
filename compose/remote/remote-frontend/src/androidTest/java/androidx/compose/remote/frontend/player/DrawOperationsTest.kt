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

package androidx.compose.remote.frontend.player

import android.graphics.Path
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations.PROFILE_WIDGETS
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.frontend.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.frontend.test.util.createBitmap
import androidx.compose.remote.frontend.test.util.getCoreDocument
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.remote.test.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class DrawOperationsTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.999),
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun drawOperationsInGrid() {
        val documents: Array<CoreDocument> =
            arrayOf(
                drawBitmap(),
                drawBitmapWithBounds(),
                drawBitmapWithId(),
                drawBitmapWithIdAndPosition(),
                drawScaledBitmap(),
                drawScaledBitmap_partially_samePosition(),
                drawScaledBitmap_partially_differentPosition(),
                drawScaledBitmapWithId(),
                drawArc(),
                drawSector(),
                drawCircle(),
                drawLine(),
                drawOval(),
                drawPath(),
                drawPathWithPath(),
                drawRect(),
                drawRoundRect(),
                drawTextOnPath(),
                drawTextOnPathWithTextId(),
                drawTextRun(),
                drawTextRunWithTextId(),
                drawBitmapFontTextRun(),
                drawTextAnchored(),
                drawTextAnchoredWithStrId(),
                drawBitmapTextAnchored(),
                drawBitmapTextAnchoredWithTextId(),
                drawTweenPath(),
                drawTweenPathWithPath(),
            )

        val columns = 5

        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.White) {
            val density = LocalDensity.current.density
            val itemWidth = (100f / density).toInt()
            val itemHeight = (100f / density).toInt()

            Column(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until documents.size step columns) {
                    Row {
                        for (j in 0 until columns) {
                            val index = i + j
                            if (index >= documents.size) break

                            RemoteDocumentPlayer(
                                documents[index],
                                itemWidth,
                                itemHeight,
                                modifier = Modifier.padding(10.dp).testTag("index=$index"),
                                debugMode = 1,
                            )
                        }
                    }
                }
            }
        }
    }

    fun drawBitmap() = getCoreDocument {
        drawBitmap(
            image = createBitmap() as Object,
            width = 100,
            height = 100,
            contentDescription = "contentDescription",
        )
    }

    fun drawBitmapWithBounds() = getCoreDocument {
        drawBitmap(
            image = createBitmap(),
            left = 0f,
            top = 0f,
            right = 100f,
            bottom = 100f,
            contentDescription = "contentDescription",
        )
    }

    fun drawBitmapWithId() = getCoreDocument {
        val imageId = storeBitmap(createBitmap())
        drawBitmap(
            imageId = imageId,
            left = 0f,
            top = 0f,
            right = 100f,
            bottom = 100f,
            contentDescription = "contentDescription",
        )
    }

    fun drawBitmapWithIdAndPosition() = getCoreDocument {
        @Suppress("ConstantConditionIf") if (true) return@getCoreDocument // "b/440385172"
        val imageId = storeBitmap(createBitmap())
        drawBitmap(
            imageId = imageId,
            left = 10f,
            top = 10f,
            contentDescription = "contentDescription",
        )
    }

    fun drawScaledBitmap() = getCoreDocument {
        drawScaledBitmap(
            image = createBitmap() as Object,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = 100f,
            srcBottom = 100f,
            dstLeft = 0f,
            dstTop = 0f,
            dstRight = 100f,
            dstBottom = 100f,
            scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
            scaleFactor = 0f,
            contentDescription = "contentDescription",
        )
    }

    fun drawScaledBitmap_partially_samePosition() = getCoreDocument {
        // TODO(b/441044854): this operation breaks subsequent non bitmap operations.
        // TODO(b/294531403): clipping not working with multiple players on the same screen.
        @Suppress("ConstantConditionIf") if (true) return@getCoreDocument
        drawScaledBitmap(
            image = createBitmap() as Object,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = 50f,
            srcBottom = 50f,
            dstLeft = 0f,
            dstTop = 0f,
            dstRight = 50f,
            dstBottom = 50f,
            scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
            scaleFactor = 0f,
            contentDescription = "contentDescription",
        )
    }

    fun drawScaledBitmap_partially_differentPosition() = getCoreDocument {
        // TODO(b/441044854): this operation breaks subsequent non bitmap operations.
        // TODO(b/294531403): clipping not working with multiple players on the same screen.
        @Suppress("ConstantConditionIf") if (true) return@getCoreDocument
        drawScaledBitmap(
            image = createBitmap() as Object,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = 50f,
            srcBottom = 50f,
            dstLeft = 50f,
            dstTop = 50f,
            dstRight = 100f,
            dstBottom = 100f,
            scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
            scaleFactor = 0f,
            contentDescription = "contentDescription",
        )
    }

    fun drawScaledBitmapWithId() = getCoreDocument {
        val imageId = storeBitmap(createBitmap())
        drawScaledBitmap(
            imageId = imageId,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = 100f,
            srcBottom = 100f,
            dstLeft = 0f,
            dstTop = 0f,
            dstRight = 100f,
            dstBottom = 100f,
            scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
            scaleFactor = 0f,
            contentDescription = "contentDescription",
        )
    }

    fun drawArc() = getCoreDocument { drawArc(0f, 0f, 100f, 100f, 45f, 135f) }

    fun drawSector() = getCoreDocument { drawSector(0f, 0f, 100f, 100f, 45f, 135f) }

    fun drawCircle() = getCoreDocument { drawCircle(50f, 50f, 40f) }

    fun drawLine() = getCoreDocument { drawLine(10f, 10f, 90f, 90f) }

    fun drawOval() = getCoreDocument { drawOval(10f, 20f, 90f, 80f) }

    fun drawPath() = getCoreDocument {
        val pathId = pathCreate(10f, 10f)
        pathAppendLineTo(pathId, 90f, 90f)
        pathAppendLineTo(pathId, 10f, 90f)
        pathAppendClose(pathId)
        drawPath(pathId)
    }

    fun drawPathWithPath() = getCoreDocument {
        val path = Path()
        path.moveTo(10f, 10f)
        path.lineTo(90f, 90f)
        path.lineTo(10f, 90f)
        path.close()
        drawPath(path)
    }

    fun drawRect() = getCoreDocument { drawRect(10f, 20f, 90f, 80f) }

    fun drawRoundRect() = getCoreDocument { drawRoundRect(10f, 20f, 90f, 80f, 15f, 15f) }

    fun drawTextOnPath() = getCoreDocument {
        @Suppress("ConstantConditionIf") if (true) return@getCoreDocument // "b/440318500"
        val path = Path()
        path.moveTo(10f, 50f)
        path.lineTo(90f, 50f)
        drawTextOnPath("Text on path", path, 0f, 0f)
    }

    fun drawTextOnPathWithTextId() = getCoreDocument {
        @Suppress("ConstantConditionIf") if (true) return@getCoreDocument // "b/440318500"
        val textId = textCreateId("Text on path")
        val path = Path()
        path.moveTo(10f, 50f)
        path.lineTo(90f, 50f)
        drawTextOnPath(textId, path, 0f, 0f)
    }

    fun drawTextRun() = getCoreDocument {
        drawTextRun("Hello World", 0, 11, 0, 11, 10f, 50f, false)
    }

    fun drawTextRunWithTextId() = getCoreDocument {
        val textId = textCreateId("Hello World")
        drawTextRun(textId, 0, 11, 0, 11, 10f, 50f, false)
    }

    fun drawBitmapFontTextRun() = getCoreDocument {
        val textId = textCreateId("AB")
        val bitmapId = storeBitmap(createBitmap())
        val glyphs =
            arrayOf(
                BitmapFontData.Glyph("A", bitmapId, 0, 0, 0, 0, 10, 10),
                BitmapFontData.Glyph("B", bitmapId, 10, 0, 0, 0, 10, 10),
            )
        val bitmapFontId = addBitmapFont(glyphs)
        drawBitmapFontTextRun(textId, bitmapFontId, 0, 2, 10f, 50f)
    }

    fun drawTextAnchored() = getCoreDocument {
        drawTextAnchored("Anchored", 50f, 50f, 0.5f, 0.5f, 0)
    }

    fun drawTextAnchoredWithStrId() = getCoreDocument {
        val strId = textCreateId("Anchored")
        drawTextAnchored(strId, 50f, 50f, 0.5f, 0.5f, 0)
    }

    fun drawBitmapTextAnchored() =
        getCoreDocument(
            extraTags = arrayOf(RemoteComposeWriter.HTag(Header.DOC_PROFILES, PROFILE_WIDGETS))
        ) {
            val bitmapId = storeBitmap(createBitmap())
            val glyphs =
                arrayOf(
                    BitmapFontData.Glyph("A", bitmapId, 0, 0, 0, 0, 10, 10),
                    BitmapFontData.Glyph("B", bitmapId, 10, 0, 0, 0, 10, 10),
                )
            val bitmapFontId = addBitmapFont(glyphs)
            drawBitmapTextAnchored("AB", bitmapFontId, 0f, 2f, 50f, 50f, 0.5f, 0.5f)
        }

    fun drawBitmapTextAnchoredWithTextId() =
        getCoreDocument(
            extraTags = arrayOf(RemoteComposeWriter.HTag(Header.DOC_PROFILES, PROFILE_WIDGETS))
        ) {
            val textId = textCreateId("AB")
            val bitmapId = storeBitmap(createBitmap())
            val glyphs =
                arrayOf(
                    BitmapFontData.Glyph("A", bitmapId, 0, 0, 0, 0, 10, 10),
                    BitmapFontData.Glyph("B", bitmapId, 10, 0, 0, 0, 10, 10),
                )
            val bitmapFontId = addBitmapFont(glyphs)
            drawBitmapTextAnchored(textId, bitmapFontId, 0f, 2f, 50f, 50f, 0.5f, 0.5f)
        }

    fun drawTweenPath() = getCoreDocument {
        val path1Id = pathCreate(10f, 10f)
        pathAppendLineTo(path1Id, 90f, 10f)
        pathAppendLineTo(path1Id, 90f, 90f)
        pathAppendLineTo(path1Id, 10f, 90f)
        pathAppendClose(path1Id)

        val path2Id = pathCreate(50f, 50f)
        pathAppendLineTo(path2Id, 90f, 10f)
        pathAppendLineTo(path2Id, 90f, 90f)
        pathAppendLineTo(path2Id, 10f, 90f)
        pathAppendClose(path2Id)
        drawTweenPath(path1Id, path2Id, 0.5f, 0f, 1f)
    }

    fun drawTweenPathWithPath() = getCoreDocument {
        val path1 = Path()
        path1.moveTo(10f, 10f)
        path1.lineTo(90f, 10f)
        path1.lineTo(90f, 90f)
        path1.lineTo(10f, 90f)
        path1.close()

        val path2 = Path()
        path2.moveTo(50f, 50f)
        path2.lineTo(90f, 10f)
        path2.lineTo(90f, 90f)
        path2.lineTo(10f, 90f)
        path2.close()

        drawTweenPath(path1, path2, 0.5f, 0f, 1f)
    }
}
