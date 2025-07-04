/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.emoji2.text.EmojiCompat
import java.text.BreakIterator

internal actual fun String.findPrecedingBreak(index: Int): Int {
    val emojiBreak =
        getEmojiCompatIfLoaded()?.getEmojiStart(this, maxOf(0, index - 1))?.takeUnless { it == -1 }
    if (emojiBreak != null) return emojiBreak

    val it = BreakIterator.getCharacterInstance()
    it.setText(this)
    return it.preceding(index)
}

internal actual fun String.findFollowingBreak(index: Int): Int {
    val emojiBreak = getEmojiCompatIfLoaded()?.getEmojiEnd(this, index)?.takeUnless { it == -1 }
    if (emojiBreak != null) return emojiBreak

    val it = BreakIterator.getCharacterInstance()
    it.setText(this)
    return it.following(index)
}

/**
 * @return the code point before the given [index], or [ifNotFound] if there is no code point before
 *   [index].
 */
private fun CharSequence.findCodePointBefore(index: Int, ifNotFound: Int): Int =
    if (index <= 0) ifNotFound
    else java.lang.Character.offsetByCodePoints(this, index, /* codePointOffset= */ -1)

internal actual fun String.findCodePointOrEmojiStartBefore(index: Int, ifNotFound: Int): Int {
    if (index <= 0) return ifNotFound

    val emojiCompat = getEmojiCompatIfLoaded()
    if (emojiCompat == null) return findCodePointBefore(index, ifNotFound)

    val emojiStart = emojiCompat.getEmojiStart(this, index - 1)
    if (emojiStart < 0) return findCodePointBefore(index, ifNotFound)

    return emojiStart
}

private fun getEmojiCompatIfLoaded(): EmojiCompat? =
    if (EmojiCompat.isConfigured())
        EmojiCompat.get().takeIf { it.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED }
    else null
