/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.text

import android.graphics.Paint.FontMetricsInt
import android.text.Layout.Directions
import android.text.Layout.TabStops

class TextLine() {
    var mText: CharSequence = ""
    var mLen = 0

    companion object {
        @JvmStatic
        fun obtain(): TextLine = TextLine()

        @JvmStatic
        fun recycle(line: TextLine) {
        }
    }

    fun metrics(fmi: FontMetricsInt): Float {
        println("TextLine.metrics")
        return mLen * 1f
    }

    fun set(
        paint: TextPaint,
        text: CharSequence,
        start: Int,
        limit: Int,
        dir: Int,
        directions: Directions,
        hasTabs: Boolean,
        tabStops: TabStops?,
        ellipsisStart: Int,
        ellipsisEnd: Int
    ) {
        println("TextLine.set")
        mText = text
        mLen = limit - start
    }
}