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

package androidx.ink.brush

import androidx.ink.nativeloader.cinterop.StockBrushesNative_createDashedLine
import androidx.ink.nativeloader.cinterop.StockBrushesNative_createEmojiHighlighter
import androidx.ink.nativeloader.cinterop.StockBrushesNative_createHighlighter
import androidx.ink.nativeloader.cinterop.StockBrushesNative_createMarker
import androidx.ink.nativeloader.cinterop.StockBrushesNative_createPredictionFadeOutBehavior
import androidx.ink.nativeloader.cinterop.StockBrushesNative_createPressurePen
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object StockBrushesNative {
    actual fun createMarker(version: Int): Long = StockBrushesNative_createMarker(version)

    actual fun createDashedLine(version: Int): Long = StockBrushesNative_createDashedLine(version)

    actual fun createPressurePen(version: Int): Long = StockBrushesNative_createPressurePen(version)

    actual fun createHighlighter(selfOverlap: Int, version: Int): Long =
        StockBrushesNative_createHighlighter(selfOverlap, version)

    actual fun createEmojiHighlighter(
        clientTextureId: String,
        showMiniEmojiTrail: Boolean,
        selfOverlap: Int,
        version: Int,
    ): Long =
        StockBrushesNative_createEmojiHighlighter(
            clientTextureId,
            showMiniEmojiTrail,
            selfOverlap,
            version,
        )

    actual fun createPredictionFadeOutBehavior(): Long =
        StockBrushesNative_createPredictionFadeOutBehavior()
}
