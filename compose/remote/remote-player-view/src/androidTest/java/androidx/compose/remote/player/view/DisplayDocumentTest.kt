/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.util.Log
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Test of RemoteComposeDocument class verifying serialization/deserialization */
@RunWith(AndroidJUnit4::class)
class DisplayDocumentTest {

    fun diff(a: String, b: String) {
        val `as` = a.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val bs = b.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (`as`.size != bs.size) println("diff " + `as`.size + " lines vs. " + bs.size + " lines")
        else {
            println("diff ---------" + `as`.size + " lines")
        }
        val max = Math.max(`as`.size, bs.size)
        val c = CharArray(50) { '-' }
        val mark = c.toString()
        for (i in 0 until max) {
            if (i >= `as`.size) {
                println(
                    i.toString() +
                        ": \"" +
                        mark.substring(0, bs[i].length) +
                        "\"!=\"" +
                        bs[i] +
                        "\""
                )
                continue
            }
            if (i >= bs.size) {
                println(
                    i.toString() +
                        ": \"" +
                        `as`[i] +
                        "\"!=\"" +
                        mark.substring(0, `as`[i].length) +
                        "\""
                )
                continue
            }
            if (bs[i] != `as`[i]) {
                println(i.toString() + ": \"" + `as`[i] + "\"!=\"" + bs[i] + "\"")
                break
            }
            println(i.toString() + ": " + `as`[i])
        }
    }

    fun createDocument(context: RemoteContext): RemoteComposeDocument {
        val tw = 600
        val th = 600

        val lightImage = createImage(tw, th, false)
        val darkImage = createImage(tw, th, true)

        val doc =
            RemoteComposeContextAndroid(tw, th, "Demo", AndroidxPlatformServices()) {
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

    @Test
    fun testDisplayDocumentInLightMode() {
        val debugContext = DebugPlayerContext()

        val doc = createDocument(debugContext)
        doc.paint(debugContext, Theme.LIGHT)

        val result = removeTime(debugContext.getTestResults())
        val expectedResult =
            """header(1, 1, 0) 600 x 600, 0
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
setTheme(-3)
drawBitmap <43>
setTheme(-2)
setTheme(-1)

"""
        if (TestUtils.diff(expectedResult, result)) {
            Log.v("TEST", result)
            TestUtils.dumpDifference(expectedResult, result)
        }

        assertEquals("light mode <$result>", expectedResult, result)
    }

    @Test
    fun testDisplayDocumentUnknownMode() {
        val debugContext = DebugPlayerContext()

        val doc = createDocument(debugContext)
        doc.paint(debugContext, Theme.UNSPECIFIED)

        val result = removeTime(debugContext.getTestResults())
        val expectedResult =
            "header(1, 1, 0) 600 x 600, 0\n" +
                "loadText(42)\n" +
                "setTheme(-3)\n" +
                "loadImage(43)\n" +
                "loadText(44)\n" +
                "setTheme(-2)\n" +
                "loadImage(45)\n" +
                "loadText(46)\n" +
                "setTheme(-1)\n" +
                "loadText(47)\n" +
                "loadText(48)\n" +
                "clickArea(1, 0.0, 0.0, 300.0, 300.0, 48)\n" +
                "loadText(49)\n" +
                "loadText(50)\n" +
                "clickArea(2, 300.0, 0.0, 600.0, 300.0, 50)\n" +
                "loadText(51)\n" +
                "loadText(52)\n" +
                "clickArea(3, 0.0, 300.0, 300.0, 600.0, 52)\n" +
                "loadText(53)\n" +
                "loadText(54)\n" +
                "clickArea(4, 300.0, 300.0, 600.0, 600.0, 54)\n" +
                "setTheme(-1)\n" +
                "setTheme(-3)\n" +
                "drawBitmap <43>\n" +
                "setTheme(-2)\n" +
                "drawBitmap <45>\n" +
                "setTheme(-1)\n\n"

        Log.v("TEST", result)
        if (TestUtils.diff(expectedResult, result)) {
            Log.v("TEST", result)
            TestUtils.dumpDifference(expectedResult, result)
        }
        assertEquals("light mode <$result>", expectedResult, result)
    }

    @Test
    fun testDisplayDocumentInDarkMode() {
        val debugContext = DebugPlayerContext()

        val doc = createDocument(debugContext)
        // debugContext.clearResults()
        doc.paint(debugContext, Theme.DARK)

        var result = removeTime(debugContext.getTestResults())
        val expectedResult =
            """header(1, 1, 0) 600 x 600, 0
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
setTheme(-3)
setTheme(-2)
drawBitmap <45>
setTheme(-1)

"""

        println("=========================")

        if (TestUtils.diff(expectedResult, result)) {
            Log.v("TEST", result)
            TestUtils.dumpDifference(expectedResult, result)
        }
        println("=========================")
        assertEquals("dark mode <$result>", expectedResult, result)
    }

    private fun removeTime(text: String): String {
        val sb = StringBuilder()
        for (line in text.split("\n")) {
            if (!line.contains(Regex("loadFloat\\[1*[0-9]\\]"))) {
                sb.append(line)
                sb.append("\n")
                println("add $line")
            }
        }
        return sb.toString()
    }
}
