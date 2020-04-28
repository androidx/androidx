/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Rect

public class Canvas(private val canvas: org.jetbrains.skija.Canvas) {

    fun translate(x: Float, y: Float) {
        canvas.translate(x, y)
    }

    fun drawRect(rect: android.graphics.RectF, paint: android.graphics.Paint) {
        val skijaRect = Rect.makeLTRB(rect.left, rect.top, rect.right, rect.bottom)
        canvas.drawRect(skijaRect, paint.skijaPaint)
    }
}
