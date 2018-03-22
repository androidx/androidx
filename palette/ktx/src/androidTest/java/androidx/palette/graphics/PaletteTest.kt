/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.palette.graphics

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Color.RED
import androidx.palette.graphics.Target.VIBRANT
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class PaletteTest {
    @Test fun bitmapBuild() {
        val bitmap = Bitmap.createBitmap(10, 10, ARGB_8888)
        // There's no easy way to test that the palette was created from our Bitmap.
        assertNotNull(bitmap.buildPalette())
    }

    @Test fun operatorGet() {
        val bitmap = Bitmap.createBitmap(10, 10, ARGB_8888).apply {
            Canvas(this).drawColor(RED)
        }
        val palette = Palette.from(bitmap).generate()
        assertSame(palette.getSwatchForTarget(VIBRANT), palette[VIBRANT])
    }
}
