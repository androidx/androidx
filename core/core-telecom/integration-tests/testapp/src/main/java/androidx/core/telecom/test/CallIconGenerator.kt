/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.test

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * `CallIconGenerator` is a utility class responsible for generating bitmaps that serve as icons.
 * Each generated icon displays a sequentially incrementing number on a colored background.
 */
class CallIconGenerator {

    companion object {
        const val ICON_WIDTH = 200
        const val ICON_HEIGHT = 200
        /**
         * Keeps track of the next number to be used for icon generation. It is incremented each
         * time `generateNextBitmap()` is called.
         */
        private var mNextCallIcon: Int = 1

        /**
         * Converts a given number into a bitmap.
         *
         * The bitmap will have a solid background color and the number rendered in the center.
         *
         * @param number The number to be drawn on the bitmap.
         * @return A bitmap representing the number.
         */
        fun numberToBitmap(number: Int): Bitmap {
            // Create a Bitmap
            val bitmap = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Draw the background
            canvas.drawColor(Color.parseColor("#A4C639")) // Android Green
            // Draw the number on the canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.WHITE // Choose your desired color
            paint.textSize = 150f // Adjust font size as needed
            paint.textAlign = Paint.Align.CENTER
            val text = number.toString()
            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val x = canvas.width / 2f
            val y = canvas.height / 2f + textBounds.height() / 2f - textBounds.bottom
            canvas.drawText(text, x, y, paint)
            return bitmap
        }

        /**
         * Generates a bitmap with the next sequential number.
         *
         * Each call to this method produces a bitmap with a number one greater than the previous
         * call.
         *
         * @return A bitmap with the next number.
         */
        fun generateNextBitmap(): Bitmap {
            val bitmap = numberToBitmap(mNextCallIcon++)
            return bitmap
        }
    }
}
