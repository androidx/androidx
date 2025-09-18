/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.compose.remote.creation.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Platform
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.creation.RemotePath
import androidx.graphics.path.PathSegment
import androidx.graphics.path.iterator
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat

/**
 * This support services needed by the RemoteCompose core that need to be provided by the platform.
 * e.g. PNG compression
 */
public open class AndroidxPlatformServices(private val logger: RCLogger = RCLogger.AndroidLog) :
    Platform {
    private fun convertAlpha8ToARGB8888(alphaBitmap: Bitmap): Bitmap {
        // Check if the bitmap is ALPHA_8, if not return the original
        if (alphaBitmap.config != Bitmap.Config.ALPHA_8) {
            return alphaBitmap
        }

        // Create a new bitmap with the same dimensions but in ARGB_8888 format
        val argbBitmap =
            Bitmap.createBitmap(alphaBitmap.width, alphaBitmap.height, Bitmap.Config.ARGB_8888)

        // Create a canvas to draw onto the new bitmap
        val canvas = Canvas(argbBitmap)

        // Create paint with anti-aliasing and filtering
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Since the original bitmap only contains alpha values, we set a color to paint with.
        // Here, we're using black (0xFF000000.toInt()) as the color, but you can choose any color.
        // The alpha values from the original bitmap will be combined with this color.
        paint.color = 0xFF000000.toInt() // You can change this color as needed

        // Draw the original bitmap onto the new one. The alpha values will be applied to the paint
        // color.
        canvas.drawBitmap(alphaBitmap, 0f, 0f, paint)

        // Return the new ARGB_8888 bitmap
        return argbBitmap
    }

    override fun imageToByteArray(image: Any): ByteArray? {
        if (image is Bitmap) {
            val treatedImage = convertAlpha8ToARGB8888(image)
            // let's create a bitmap
            val byteArrayBitmapStream = ByteArrayOutputStream()
            val successful =
                treatedImage.compress(Bitmap.CompressFormat.PNG, 90, byteArrayBitmapStream)
            assert(successful) { "Image could not be compressed" }
            return byteArrayBitmapStream.toByteArray()
        }
        return null
    }

    override fun getImageWidth(image: Any): Int {
        if (image is Bitmap) {
            return image.width
        }
        return 0
    }

    override fun getImageHeight(image: Any): Int {
        if (image is Bitmap) {
            return image.height
        }
        return 0
    }

    override fun isAlpha8Image(image: Any): Boolean {
        if (image is Bitmap) {
            return image.config == Bitmap.Config.ALPHA_8
        }
        return false
    }

    override fun pathToFloatArray(path: Any): FloatArray? {
        if (path is RemotePath) {
            return path.createFloatArray()
        }
        if (path is Path) {
            return androidPathToFloatArray(path)
        }
        return null
    }

    override fun parsePath(pathData: String): Any {
        val path = Path()
        val cords = FloatArray(6)

        val commands =
            pathData
                .split("(?=[MmZzLlHhVvCcSsQqTtAa])".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        for (command in commands) {
            val cmd = command.get(0)
            val values =
                command
                    .substring(1)
                    .trim { it <= ' ' }
                    .split("[,\\s]+".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            when (cmd) {
                'M' -> path.moveTo(values[0].toFloat(), values[1].toFloat())
                'L' -> {
                    var i = 0
                    while (i < values.size) {
                        path.lineTo(values[i].toFloat(), values[i + 1].toFloat())
                        i += 2
                    }
                }

                'H' ->
                    for (value in values) {
                        path.lineTo(value.toFloat(), cords[1])
                    }

                'C' -> {
                    var i = 0
                    while (i < values.size) {
                        path.cubicTo(
                            values[i].toFloat(),
                            values[i + 1].toFloat(),
                            values[i + 2].toFloat(),
                            values[i + 3].toFloat(),
                            values[i + 4].toFloat(),
                            values[i + 5].toFloat(),
                        )
                        i += 6
                    }
                }

                'S' -> {
                    var i = 0
                    while (i < values.size) {
                        path.cubicTo(
                            2 * cords[0] - cords[2],
                            2 * cords[1] - cords[3],
                            values[i].toFloat(),
                            values[i + 1].toFloat(),
                            values[i + 2].toFloat(),
                            values[i + 3].toFloat(),
                        )
                        i += 4
                    }
                }

                'Z' -> path.close()
                else -> throw IllegalArgumentException("Unsupported command: " + cmd)
            }
            if (cmd != 'Z' && cmd != 'H') {
                cords[0] = values[values.size - 2].toFloat()
                cords[1] = values[values.size - 1].toFloat()
                if (cmd == 'C' || cmd == 'S') {
                    cords[2] = values[values.size - 4].toFloat()
                    cords[3] = values[values.size - 3].toFloat()
                }
            }
        }

        return path
    }

    private fun androidPathToFloatArray(path: Path): FloatArray {
        val pathFloat = FloatArray(path.iterator().calculateSize() * 10)
        val iter = path.iterator()
        var count = 0
        for (seg in iter) {
            pathFloat[count++] =
                when (seg.type) {
                    PathSegment.Type.Move -> PathData.MOVE_NAN
                    PathSegment.Type.Line -> PathData.LINE_NAN
                    PathSegment.Type.Quadratic -> PathData.QUADRATIC_NAN
                    PathSegment.Type.Conic -> PathData.CONIC_NAN
                    PathSegment.Type.Cubic -> PathData.CUBIC_NAN
                    PathSegment.Type.Close -> PathData.CLOSE_NAN
                    PathSegment.Type.Done -> PathData.DONE_NAN
                }
            for (point in seg.points) {
                pathFloat[count++] = point.x
                pathFloat[count++] = point.y
            }
            if (seg.type == PathSegment.Type.Conic) {
                pathFloat[count++] = seg.weight
            }
        }
        return pathFloat.copyOf(count)
    }

    override fun log(category: Platform.LogCategory, message: String) {
        logger.log(category = category, message = message)
    }
}

/** This is for debugging paths */
private fun pathDump(path: Path) {
    val iter = path.iterator()
    val df = DecimalFormat("##0.00")
    var k = 0
    for (seg in iter) {
        print(" dump [$k]")
        k++
        when (seg.type) {
            androidx.graphics.path.PathSegment.Type.Move -> print(">Move")
            androidx.graphics.path.PathSegment.Type.Line -> print(">Line")
            androidx.graphics.path.PathSegment.Type.Quadratic -> print(">Quadratic")
            androidx.graphics.path.PathSegment.Type.Conic -> print(">Conic")
            androidx.graphics.path.PathSegment.Type.Cubic -> print(">Cubic")
            androidx.graphics.path.PathSegment.Type.Close -> print(">Close")
            androidx.graphics.path.PathSegment.Type.Done -> print(">Done")
        }

        for (point in seg.points) {
            print("  " + df.format(point.x) + ", " + df.format(point.y))
        }
        if (seg.type == androidx.graphics.path.PathSegment.Type.Conic) {
            println(" weight = ${seg.weight}")
        }
        println()
    }
}
