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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.ui.test.junit4.v2.createComposeRule
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Value-producing paint operations (`TextAttribute`, `TimeAttribute`, `BitmapTextMeasure`) in the
 * embedded player. Each test draws a bar whose width is the produced value, so a value that is
 * never computed (0) draws nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerAttributesTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun textAttributeLengthIsComputed() {
        val bitmap = rule.renderDocument {
            val text = writer.textCreateId("hello")
            val length = writer.textAttribute(text, TextAttribute.TEXT_LENGTH)
            bar(writer.floatExpression(length, 10f, MUL))
        }

        assertBarWidth(bitmap, 50)
    }

    @Test
    fun timeAttributeOfALongIsComputed() {
        val june =
            LocalDateTime.of(2020, 6, 15, 12, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val bitmap = rule.renderDocument {
            val time = writer.addLong(june)
            val month = writer.timeAttribute(time, TimeAttribute.TIME_MONTH_VALUE)
            bar(writer.floatExpression(month, 10f, MUL))
        }

        // TIME_MONTH_VALUE is zero based, so June is 5.
        assertBarWidth(bitmap, 50)
    }

    @Test
    fun bitmapTextMeasureWidthIsComputed() {
        val bitmap = rule.renderDocument {
            val font =
                writer.addBitmapFont(arrayOf(BitmapFontData.Glyph("a", 0, 0, 0, 0, 0, 10, 10)))
            val text = writer.textCreateId("aaaa")
            bar(writer.bitmapTextMeasure(text, font, BitmapTextMeasure.MEASURE_WIDTH, 0f))
        }

        assertBarWidth(bitmap, 40)
    }

    /** Draws a red bar from x = 0 to [width], across the middle of the player. */
    private fun RemoteComposeContextAndroid.bar(width: Float) {
        painter.setColor(AndroidColor.RED).commit()
        writer.drawRect(0f, 40f, width, 60f)
    }

    private fun assertBarWidth(bitmap: Bitmap, width: Int) {
        assertRed(bitmap, width - 5, 50)
        assertNotRed(bitmap, width + 5, 50)
    }
}
