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

package androidx.compose.remote.player.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.Ignore
import kotlin.test.Test
import org.junit.runner.RunWith

/** Test writing a document, verifying serialization/deserialization */
@RunWith(AndroidJUnit4::class)
class WriteDocumentTest {

    fun createDocument(context: RemoteContext): RemoteComposeDocument {
        val tw = 600
        val th = 600

        val lightImage = createImage(tw, th, false)
        val darkImage = createImage(tw, th, true)

        val doc =
            RemoteComposeContextAndroid(tw, th, "Demo") {
                setTheme(Theme.LIGHT)
                drawBitmap(lightImage, "Light Mode")
                setTheme(Theme.DARK)
                drawBitmap(darkImage, "Dark Mode")
                setTheme(Theme.UNSPECIFIED)
                addClickArea(1, "Area A", 0f, 0f, 300f, 300f, "A")
                addClickArea(2, "Area B", 300f, 0f, 600f, 300f, "B")
                addClickArea(3, "Area C", 0f, 300f, 300f, 600f, "C")
                addClickArea(4, "Area D", 300f, 300f, 600f, 600f, "D")
            }

        val buffer = doc.buffer()
        val bufferSize = doc.bufferSize()

        val recreatedDocument = RemoteComposeDocument(ByteArrayInputStream(buffer, 0, bufferSize))
        recreatedDocument.initializeContext(context)
        return recreatedDocument
    }

    private fun createImage(tw: Int, th: Int, darkTheme: Boolean): Bitmap {
        val image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        val paint = Paint()

        val canvas = Canvas(image)
        paint.color = android.graphics.Color.RED
        paint.strokeWidth = 3f
        if (darkTheme) {
            paint.color = android.graphics.Color.BLACK
        } else {
            paint.color = android.graphics.Color.WHITE
        }
        canvas.drawRect(0f, 0f, tw.toFloat(), th.toFloat(), paint)
        paint.color = android.graphics.Color.RED
        canvas.drawLine(0f, 0f, tw.toFloat(), th.toFloat(), paint)
        canvas.drawLine(0f, th.toFloat(), tw.toFloat(), 0f, paint)
        paint.color = android.graphics.Color.BLUE
        canvas.drawLine(300f, 0f, 300f, 600f, paint)
        canvas.drawLine(0f, 300f, 600f, 300f, paint)
        return image
    }

    @Ignore("b/370676179 failing for now")
    @Test
    fun testWriteDocument() {
        val debugContext = DebugPlayerContext()
        val doc = createDocument(debugContext)
        doc.paint(debugContext, Theme.UNSPECIFIED)

        val result = debugContext.getTestResults()
        val expectedResult =
            """loadFloat[10]=3600.0
loadFloat[12]=10.0
loadFloat[11]=2.0
header(0, 1, 0) 600 x 600, 0
loadText(42)
setTheme(-3)
loadImage(43)
loadText(44)
setTheme(-2)
loadImage(45)
loadText(46)
setTheme(-1)
loadText(47)
loadText(48)
clickArea(1, 0.0, 0.0, 300.0, 300.0, 48)
loadText(49)
loadText(50)
clickArea(2, 300.0, 0.0, 600.0, 300.0, 50)
loadText(51)
loadText(52)
clickArea(3, 0.0, 300.0, 300.0, 600.0, 52)
loadText(53)
loadText(54)
clickArea(4, 300.0, 300.0, 600.0, 600.0, 54)
setTheme(-1)
loadFloat[10]=3600.0
loadFloat[12]=10.0
loadFloat[11]=2.0
header(0, 1, 0) 600 x 600, 0
loadText(42)
setTheme(-3)
loadImage(43)
loadText(44)
drawBitmap <43>
setTheme(-2)
loadImage(45)
loadText(46)
drawBitmap <45>
setTheme(-1)
loadText(47)
loadText(48)
loadText(49)
loadText(50)
loadText(51)
loadText(52)
loadText(53)
loadText(54)

"""

        val result2 = removeTime(result)
        println("================")
        dumpDifference(expectedResult, result2)
        println("================")

        assertEquals("write doc ", expectedResult, result2)
    }

    private fun removeTime(text: String): String {
        val sb = StringBuilder()
        for (line in text.split("\n")) {
            if (!line.contains(Regex("loadFloat\\[[0-9]\\]"))) {
                sb.append(line)
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    fun dumpDifference(expected: String, result: String) {
        val e = expected.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val r = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var len = 0
        println("chars  " + expected.length + " " + result.length)
        println("lines  " + e.size + "  " + r.size)
        if (abs(expected.length - result.length) < 10) {
            var count = 0
            for (i in 0 until min(expected.length, result.length)) {
                if (expected[i] != result[i]) {
                    count++
                }
            }
            if (count < 10) {
                for (i in 0 until min(expected.length, result.length)) {
                    if (expected[i] != result[i]) {
                        println("$i " + expected[i] + " - " + result[i])
                    }
                }
            } else {
                println(" errors all over $count")
            }
        }
        for (i in r.indices) {
            len = max(r[i].length.toDouble(), len.toDouble()).toInt()
        }
        for (i in e.indices) {
            len = max(e[i].length.toDouble(), len.toDouble()).toInt()
        }

        println("------------------------------------------------")
        for (i in 0 until max(e.size, r.size)) {
            val split = if ((e.size > i && r.size > i && e[i] == r[i])) "=" else "*"
            if (e.size > i) {
                val gap = String(CharArray(len - e[i].length)).replace('\u0000', ' ') + split
                print(gap + e[i] + gap)
            }
            if (r.size > i) {
                print(r[i])
            }

            println(" $i")
        }
        println("------------------------------------------------")
    }
}
