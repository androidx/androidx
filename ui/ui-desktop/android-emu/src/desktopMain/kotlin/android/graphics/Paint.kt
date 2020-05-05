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

class Paint {
    enum class Style {
        FILL,
        FILL_AND_STROKE,
        STROKE
    }

    enum class Cap {
        BUTT,
        ROUND,
        SQUARE
    }

    enum class Join {
        BEVEL,
        MITER,
        ROUND
    }

    constructor(flags: Int) {}

    val skijaPaint = org.jetbrains.skija.Paint()

    var color: Int
        get() = skijaPaint.getColor().toInt()
        set(value) {
            skijaPaint.setColor(value.toLong())
        }
    var strokeWidth: Float = 0f
    var style: Style = Style.STROKE
    var stokeCap: Cap = Cap.BUTT
}
