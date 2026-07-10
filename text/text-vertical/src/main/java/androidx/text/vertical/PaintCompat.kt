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

package androidx.text.vertical

import android.graphics.Paint
import android.os.Build

internal fun Paint.getRunCharacterAdvanceCompat(
    text: CharSequence,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    isRtl: Boolean,
    offset: Int,
    out: FloatArray,
    outOffset: Int,
): Float {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return getRunCharacterAdvance(
            text,
            start,
            end,
            contextStart,
            contextEnd,
            isRtl,
            offset,
            out,
            outOffset,
        )
    } else {
        TODO("Implement backport.")
    }
}

internal fun Paint.measureTextVertical(text: CharSequence): Float =
    measureTextVertical(text, 0, text.length)

internal fun Paint.measureTextVertical(text: CharSequence, start: Int, end: Int): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        withVerticalFlag { measureText(text, start, end) }
    } else {
        TODO("Implement backport")
    }

internal fun Paint.getRunCharacterAdvanceVertical(
    text: CharSequence,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    isRtl: Boolean,
    offset: Int,
    out: FloatArray,
    outOffset: Int,
): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        withVerticalFlag {
            getRunCharacterAdvance(
                text,
                start,
                end,
                contextStart,
                contextEnd,
                isRtl,
                offset,
                out,
                outOffset,
            )
        }
    } else {
        TODO("Implement backport")
    }
