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

package androidx.compose.remote.testing.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.MSSIMMatcher
import java.io.File
import java.io.FileOutputStream

fun ImageBitmap.assertAgainstGolden(
    goldenIdentifier: String,
    extension: String = ".png",
    matcher: BitmapMatcher = MSSIMMatcher(),
) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedBitmap =
        context.assets.open(goldenIdentifier + extension).use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Golden image not found in assets: $goldenIdentifier")

    val expectedIntArray = IntArray(expectedBitmap.width * expectedBitmap.height)
    expectedBitmap.getPixels(
        expectedIntArray,
        0,
        expectedBitmap.width,
        0,
        0,
        expectedBitmap.width,
        expectedBitmap.height,
    )
    val givenIntArray = IntArray(this.width * this.height)
    this.asAndroidBitmap().getPixels(givenIntArray, 0, this.width, 0, 0, this.width, this.height)
    val result = matcher.compareBitmaps(expectedIntArray, givenIntArray, this.width, this.height)

    assert(result.matches) { result.comparisonStatistics }
}

fun captureToImage(composeTestRule: ComposeContentTestRule): ImageBitmap {
    val androidRule = composeTestRule as AndroidComposeTestRule<*, *>
    val view = androidRule.activity.window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap.asImageBitmap()
}

fun Bitmap.saveToFile(name: String, folder: String = "screenshots") {
    val dir = File(folder)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val file = File(dir, "$name.png")
    FileOutputStream(file).use { out -> this.compress(Bitmap.CompressFormat.PNG, 100, out) }
}
