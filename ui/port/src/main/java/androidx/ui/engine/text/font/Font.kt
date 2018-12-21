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

package androidx.ui.engine.text.font

import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight

// TODO(Migration/siyamed): might need nullable defaults for FontWeight and FontStyle which
// would mean read the weight and style from the font.
data class Font(
    val name: String,
    val weight: FontWeight = FontWeight.normal,
    val style: FontStyle = FontStyle.normal,
    // TODO(Migration/siyamed): add test
    val ttcIndex: Int = 0,
    // TODO(Migration/siyamed): add test
    val fontVariationSettings: String = ""
) {
    init {
        assert(name.isNotEmpty()) { "Font name cannot be empty" }
    }
}

fun Font.asFontFamily(): FontFamily {
    return FontFamily(this)
}