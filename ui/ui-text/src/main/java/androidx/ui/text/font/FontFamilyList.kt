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

package androidx.ui.text.font

data class FontFamilyList(val fontFamilies: List<FontFamily>) : List<FontFamily> by fontFamilies {
    constructor(fontFamily: FontFamily) : this(listOf(fontFamily))
    constructor(vararg fontFamily: FontFamily) : this(fontFamily.asList())

    init {
        assert(fontFamilies.size > 0) { "At least one FontFamily required in FontFamilyList" }
    }
}