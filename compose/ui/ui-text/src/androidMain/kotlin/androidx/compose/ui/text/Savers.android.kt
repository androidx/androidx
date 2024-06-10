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

package androidx.compose.ui.text

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextMotion

internal actual val PlatformParagraphStyle.Companion.Saver: Saver<PlatformParagraphStyle, Any>
    get() = PlatformParagraphStyleSaver

private val PlatformParagraphStyleSaver =
    Saver<PlatformParagraphStyle, Any>(
        save = { arrayListOf(save(it.includeFontPadding), save(it.emojiSupportMatch)) },
        restore = {
            @Suppress("UNCHECKED_CAST") val list = it as List<Any>
            PlatformParagraphStyle(
                includeFontPadding = restore(list[0])!!,
                emojiSupportMatch = restore(list[1])!!
            )
        }
    )

internal actual val LineBreak.Companion.Saver: Saver<LineBreak, Any>
    get() = LineBreakSaver

private val LineBreakSaver =
    Saver<LineBreak, Any>(save = { it.mask }, restore = { LineBreak(it as Int) })

internal actual val TextMotion.Companion.Saver: Saver<TextMotion, Any>
    get() = TextMotionSaver

private val TextMotionSaver =
    Saver<TextMotion, Any>(
        save = { arrayListOf(save(it.linearity), save(it.subpixelTextPositioning)) },
        restore = {
            @Suppress("UNCHECKED_CAST") val list = it as List<Any>
            TextMotion(linearity = restore(list[0])!!, subpixelTextPositioning = restore(list[1])!!)
        }
    )
