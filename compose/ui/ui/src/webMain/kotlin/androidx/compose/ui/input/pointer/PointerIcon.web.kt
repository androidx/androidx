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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi

object DummyPointerIcon : PointerIcon

internal data class BrowserCursor(val id: String): PointerIcon

/**
 * Creates [PointerIcon] from provided cursor keyword.
 * @param keyword one of the values representing the cursor appearance in a browser.
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/cursor">https://developer.mozilla.org/en-US/docs/Web/CSS/cursor</a>
 */
@ExperimentalComposeUiApi
fun PointerIcon.Companion.fromKeyword(keyword: String): PointerIcon {
    return BrowserCursor(keyword)
}

internal actual val pointerIconDefault: PointerIcon = BrowserCursor("default")
internal actual val pointerIconCrosshair: PointerIcon = BrowserCursor("crosshair")
internal actual val pointerIconText: PointerIcon = BrowserCursor("text")
internal actual val pointerIconHand: PointerIcon = BrowserCursor("pointer")