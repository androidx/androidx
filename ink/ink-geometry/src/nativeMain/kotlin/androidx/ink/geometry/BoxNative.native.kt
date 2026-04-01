/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.ink.nativeloader.cinterop.BoxNative_containsBox
import androidx.ink.nativeloader.cinterop.BoxNative_containsPoint
import androidx.ink.nativeloader.cinterop.BoxNative_createCenter
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

@OptIn(ExperimentalForeignApi::class)
actual internal object BoxNative {

    actual fun createCenter(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
    ): ImmutableVec {
        BoxNative_createCenter(rectXMin, rectYMin, rectXMax, rectYMax).useContents {
            return ImmutableVec(x, y)
        }
    }

    actual fun populateCenter(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        out: MutableVec,
    ) {
        BoxNative_createCenter(rectXMin, rectYMin, rectXMax, rectYMax).useContents {
            out.x = x
            out.y = y
        }
    }

    actual fun containsPoint(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean = BoxNative_containsPoint(rectXMin, rectYMin, rectXMax, rectYMax, pointX, pointY)

    actual fun containsBox(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        otherXMin: Float,
        otherYMin: Float,
        otherXMax: Float,
        otherYMax: Float,
    ): Boolean =
        BoxNative_containsBox(
            rectXMin,
            rectYMin,
            rectXMax,
            rectYMax,
            otherXMin,
            otherYMin,
            otherXMax,
            otherYMax,
        )
}
