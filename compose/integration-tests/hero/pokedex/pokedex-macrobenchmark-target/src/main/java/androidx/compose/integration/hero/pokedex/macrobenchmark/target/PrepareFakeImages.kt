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

package androidx.compose.integration.hero.pokedex.macrobenchmark.target

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.util.trace
import com.skydoves.pokedex.compose.core.model.AllPokemonNames
import java.io.File
import kotlin.math.sqrt

/**
 * Creates and stores gradient bitmaps for each element in [pokemonNames], under
 * '[directory]/${pokemonName}.png'.
 *
 * @param pokemonNames The names to generate bitmaps for. Used as a seed for the gradient.
 * @param width The width of the bitmap
 * @param height The height of the bitmap
 * @param directory The output directory for the generated bitmaps
 */
fun createAndStoreGradientImages(
    pokemonNames: List<String> = AllPokemonNames.take(150),
    directory: File,
    width: Int = 300,
    height: Int = 300,
) {
    trace("createAndStoreGradientImages") {
        pokemonNames.forEach { pokemonName ->
            val image =
                trace("Create Bitmap for $pokemonName") {
                    GradientBitmap(
                        width = width,
                        height = height,
                        colors =
                            GradientColorsFor(
                                pokemonName.hashCode() * 100 /* introduce greater variance */
                            ),
                    )
                }
            trace("Store Bitmap for $pokemonName") {
                val outFile = File(directory, "$pokemonName.png")
                val outputStream = outFile.outputStream()
                image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    }
}

/**
 * Generate a [Bitmap] with a radial gradient. This is useful for creating images that are unique
 * and can help avoiding system caches.
 *
 * @param width The width of the bitmap.
 * @param height The height of the bitmap.
 * @param colors The gradient colors.
 */
private fun GradientBitmap(width: Int, height: Int, colors: IntArray): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f
    val cy = height / 2f
    val r = sqrt(cx * cx + cy * cy)
    val grad = RadialGradient(cx, cy, r, colors, null, Shader.TileMode.CLAMP)
    val paint = Paint()
    paint.isDither = true
    paint.shader = grad
    canvas.drawCircle(cx, cy, r, paint)
    return bitmap
}

/** Generate a set of gradient colors for the given key */
private fun GradientColorsFor(key: Int): IntArray {
    var hash = key
    val cs = if (hash and 0b1 != 0) firstGradientPalette else secondGradientPalette
    hash = hash shr 1
    val c1 = cs[hash.mod(5)]
    hash = hash shr 8
    val c2 = cs[hash.mod(5)]
    return intArrayOf(c1, c2)
}

private val firstGradientPalette =
    intArrayOf(
        0xFFE2EFDE.toInt(),
        0xFFAFD0BF.toInt(),
        0xFF808F87.toInt(),
        0xFF9B7E46.toInt(),
        0xFFF4B266.toInt(),
        0xFFA50E0E.toInt(),
        0xFF1E8E3E.toInt(),
        0xFFE8710A.toInt(),
        0xFFE52592.toInt(),
    )

private val secondGradientPalette =
    intArrayOf(
        0xFFDCE0D9.toInt(),
        0xFF31081F.toInt(),
        0xFF6B0F1A.toInt(),
        0xFF595959.toInt(),
        0xFF808F85.toInt(),
        0xFF9334E6.toInt(),
        0xFF12B5CB.toInt(),
        0xFFFCC934.toInt(),
        0xFF4285F4.toInt(),
    )
