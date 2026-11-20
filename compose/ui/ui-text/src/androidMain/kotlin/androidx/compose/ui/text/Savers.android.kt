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
import kotlin.jvm.JvmField

/**
 * Holds the Android style [Saver] objects.
 *
 * Keeping these savers on a `private object` with [@JvmField] fields instead of top-level fields on
 * `Savers_androidKt` leaves `Savers_androidKt` free of a `<clinit>` class init method. Each
 * `Companion.Saver` getter reads its field from [AndroidStyleSavers]. R8 can strip unused savers,
 * and the app only builds them when saving state.
 */
private object AndroidStyleSavers {
    @JvmField
    internal val EmojiSupportMatchSaverInstance: Saver<EmojiSupportMatch, Any> =
        Saver<EmojiSupportMatch, Any>(
            save = { it.value },
            restore = { EmojiSupportMatch(it as Int) },
        )

    @JvmField
    internal val PlatformParagraphStyleSaverInstance: Saver<PlatformParagraphStyle, Any> =
        Saver<PlatformParagraphStyle, Any>(
            save = {
                arrayListOf(
                    save(it.includeFontPadding),
                    save(it.emojiSupportMatch, EmojiSupportMatchSaverInstance, this),
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                PlatformParagraphStyle(
                    includeFontPadding = restore(list[0])!!,
                    emojiSupportMatch = restore(list[1], EmojiSupportMatchSaverInstance)!!,
                )
            },
        )

    @JvmField
    internal val LineBreakSaverInstance: Saver<LineBreak, Any> =
        Saver<LineBreak, Any>(save = { it.mask }, restore = { LineBreak(it as Int) })

    @JvmField
    internal val TextMotionLinearitySaverInstance: Saver<TextMotion.Linearity, Any> =
        Saver<TextMotion.Linearity, Any>(
            save = { it.value },
            restore = { TextMotion.Linearity(it as Int) },
        )

    @JvmField
    internal val TextMotionSaverInstance: Saver<TextMotion, Any> =
        Saver<TextMotion, Any>(
            save = {
                arrayListOf(
                    save(it.linearity, TextMotionLinearitySaverInstance, this),
                    save(it.subpixelTextPositioning),
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                TextMotion(
                    linearity = restore(list[0], TextMotionLinearitySaverInstance)!!,
                    subpixelTextPositioning = restore(list[1])!!,
                )
            },
        )
}

internal actual val PlatformParagraphStyle.Companion.Saver: Saver<PlatformParagraphStyle, Any>
    get() = AndroidStyleSavers.PlatformParagraphStyleSaverInstance

internal val EmojiSupportMatch.Companion.Saver: Saver<EmojiSupportMatch, Any>
    get() = AndroidStyleSavers.EmojiSupportMatchSaverInstance

internal actual val LineBreak.Companion.Saver: Saver<LineBreak, Any>
    get() = AndroidStyleSavers.LineBreakSaverInstance

internal actual val TextMotion.Companion.Saver: Saver<TextMotion, Any>
    get() = AndroidStyleSavers.TextMotionSaverInstance
