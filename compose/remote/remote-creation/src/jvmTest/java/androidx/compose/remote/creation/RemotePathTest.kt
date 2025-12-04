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
package androidx.compose.remote.creation

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.Utils
import com.google.common.truth.Truth.assertThat
import java.awt.geom.AffineTransform
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemotePathTest {
    @Test
    fun testEmptyConstructor() {
        val path = RemotePath()
        assertThat(path.isEmpty()).isTrue()
    }

    @Test
    fun testMoveTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)

        assertThat(path.isEmpty()).isFalse()
        assertThat(path.currentX).isEqualTo(10f)
        assertThat(path.currentY).isEqualTo(20f)
    }

    @Test
    fun testLineTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)

        assertThat(path.currentX).isEqualTo(30f)
        assertThat(path.currentY).isEqualTo(40f)
    }

    @Test
    fun testQuadTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.quadTo(30f, 40f, 50f, 60f)

        assertThat(path.currentX).isEqualTo(50f)
        assertThat(path.currentY).isEqualTo(60f)
    }

    @Test
    fun testCubicTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.cubicTo(30f, 40f, 50f, 60f, 70f, 80f)

        assertThat(path.currentX).isEqualTo(70f)
        assertThat(path.currentY).isEqualTo(80f)
    }

    @Test
    fun testClose() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        path.close()

        assertThat(Utils.idFromNan(path.pathArray[path.size - 1])).isEqualTo(RemotePath.CLOSE)
    }

    @Test
    fun testReset() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        path.reset()
        assertThat(path.isEmpty()).isTrue()
    }

    @Test
    fun testRMoveTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.rMoveTo(5f, 15f)
        assertThat(path.currentX).isEqualTo(15f)
        assertThat(path.currentY).isEqualTo(35f)
    }

    @Test
    fun testRLineTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.rLineTo(5f, 15f)
        assertThat(path.currentX).isEqualTo(15f)
        assertThat(path.currentY).isEqualTo(35f)
    }

    @Test
    fun testRQuadTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.rQuadTo(5f, 15f, 10f, 30f)
        assertThat(path.currentX).isEqualTo(20f)
        assertThat(path.currentY).isEqualTo(50f)
    }

    @Test
    fun testRCubicTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.rCubicTo(5f, 15f, 10f, 30f, 15f, 45f)
        assertThat(path.currentX).isEqualTo(25f)
        assertThat(path.currentY).isEqualTo(65f)
    }

    @Test
    fun testConicTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.conicTo(30f, 40f, 50f, 60f, 0.5f)
        assertThat(path.currentX).isEqualTo(50f)
        assertThat(path.currentY).isEqualTo(60f)
    }

    @Test
    fun testRConicTo() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.rConicTo(5f, 15f, 10f, 30f, 0.5f)
        assertThat(path.currentX).isEqualTo(20f)
        assertThat(path.currentY).isEqualTo(50f)
    }

    @Test
    fun testRewind() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        path.rewind()
        assertThat(path.isEmpty()).isTrue()
    }

    @Test
    fun testTransform() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        path.quadTo(50f, 60f, 70f, 80f)
        path.conicTo(85f, 95f, 105f, 115f, 0.5f)
        path.cubicTo(90f, 100f, 110f, 120f, 130f, 140f)
        path.close()

        val matrix = AffineTransform()
        matrix.translate(100.0, 200.0)
        matrix.scale(2.0, 3.0)

        path.transform(matrix)

        // moveTo(10, 20) -> (10*2+100, 20*3+200) = (120, 260)
        assertThat(path.pathArray[1]).isEqualTo(120f)
        assertThat(path.pathArray[2]).isEqualTo(260f)

        // lineTo(30, 40) -> (30*2+100, 40*3+200) = (160, 320)
        assertThat(path.pathArray[6]).isEqualTo(160f)
        assertThat(path.pathArray[7]).isEqualTo(320f)

        // quadTo(50, 60, 70, 80) -> (200, 380), (240, 440)
        assertThat(path.pathArray[11]).isEqualTo(200f)
        assertThat(path.pathArray[12]).isEqualTo(380f)
        assertThat(path.pathArray[13]).isEqualTo(240f)
        assertThat(path.pathArray[14]).isEqualTo(440f)

        // conicTo(85, 95, 105, 115, 0.5) -> (270, 485), (310, 545)
        assertThat(path.pathArray[18]).isEqualTo(270f)
        assertThat(path.pathArray[19]).isEqualTo(485f)
        assertThat(path.pathArray[20]).isEqualTo(310f)
        assertThat(path.pathArray[21]).isEqualTo(545f)
        assertThat(path.pathArray[22]).isEqualTo(0.5f) // weight unchanged

        // cubicTo(90, 100, 110, 120, 130, 140) -> (280, 500), (320, 560), (360, 620)
        assertThat(path.pathArray[26]).isEqualTo(280f)
        assertThat(path.pathArray[27]).isEqualTo(500f)
        assertThat(path.pathArray[28]).isEqualTo(320f)
        assertThat(path.pathArray[29]).isEqualTo(560f)
        assertThat(path.pathArray[30]).isEqualTo(360f)
        assertThat(path.pathArray[31]).isEqualTo(620f)

        assertThat(Utils.idFromNan(path.pathArray[32])).isEqualTo(RemotePath.CLOSE)
    }

    @Test
    fun testCreateFloatArray() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        val floatArray = path.createFloatArray()
        assertThat(floatArray.size).isEqualTo(path.size)
        assertThat(floatArray).isEqualTo(path.pathArray.copyOf(path.size))
    }

    @Test
    fun testToString() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        path.close()
        val str = path.toString()
        assertThat(str).contains("moveTo(10.0, 20.0 )")
        assertThat(str).contains("lineTo(30.0, 40.0 )")
        assertThat(str).contains("close()")
    }

    @Test
    fun testStringConstructor() {
        val path = RemotePath("M10,20 L30,40 C50,60 70,80 90,100 Z")
        val str = path.toString()
        assertThat(str).contains("moveTo(10.0, 20.0 )")
        assertThat(str).contains("lineTo(30.0, 40.0 )")
        assertThat(str).contains("cubicTo( 50.0, 60.0, 70.0, 80.0, 90.0, 100.0 )")
        assertThat(str).contains("close()")
    }

    @Test
    fun testPathCreation() {
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        path.quadTo(50f, 60f, 70f, 80f)
        path.cubicTo(90f, 100f, 110f, 120f, 130f, 140f)
        path.close()

        val floatArray = path.createFloatArray()
        assertThat(floatArray).hasLength(25)

        // moveTo
        assertThat(floatArray[0].isNaN()).isTrue()
        assertThat(floatArray[1]).isEqualTo(10f)
        assertThat(floatArray[2]).isEqualTo(20f)

        // lineTo
        assertThat(floatArray[3].isNaN()).isTrue()
        assertThat(floatArray[4]).isEqualTo(0f)
        assertThat(floatArray[5]).isEqualTo(0f)
        assertThat(floatArray[6]).isEqualTo(30f)
        assertThat(floatArray[7]).isEqualTo(40f)

        // quadTo
        assertThat(floatArray[8].isNaN()).isTrue()
        assertThat(floatArray[9]).isEqualTo(0f)
        assertThat(floatArray[10]).isEqualTo(0f)
        assertThat(floatArray[11]).isEqualTo(50f)
        assertThat(floatArray[12]).isEqualTo(60f)
        assertThat(floatArray[13]).isEqualTo(70f)
        assertThat(floatArray[14]).isEqualTo(80f)

        // cubicTo
        assertThat(floatArray[15].isNaN()).isTrue()
        // Skipped cubic points for brevity

        // close
        assertThat(floatArray[24].isNaN()).isTrue()
    }

    @Test
    fun testDrawPath() {
        val writer = RemoteComposeWriter(JvmRcPlatformServices())
        writer.getRcPaint().setColor(0xFF000000.toInt())
        writer.getRcPaint().commit()
        val path = RemotePath()
        path.moveTo(10f, 20f)
        path.lineTo(30f, 40f)
        writer.drawPath(path)

        val operations = ArrayList<Operation>()
        writer.getBuffer().inflateFromBuffer(operations)

        val opStrings = operations.map { it.toString() }
        assertThat(opStrings)
            .containsExactly(
                "HEADER v1.1.0",
                "PaintData \"\n    Color(0xff000000),\n\"",
                "PathData[42] = \"M 10.0 20.0 L 0.0 0.0 30.0 40.0\"",
                "DrawPath [42], 0.0, 1.0",
            )
            .inOrder()
    }
}
