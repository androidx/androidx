/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.foundation.text

import androidx.compose.ui.text.input.AndroidImeOptions
import androidx.tv.foundation.ExperimentalTvFoundationApi

/**
 *  Additional IME configuration options supported for TV.
 *
 *  It is not guaranteed if IME will comply with the options provided here.
 *
 *  @param horizontalAlignment defines the horizontal alignment [TvKeyboardAlignment] option for
 *  keyboard.
 */
@ExperimentalTvFoundationApi
fun AndroidImeOptions(
    horizontalAlignment: TvKeyboardAlignment
) = AndroidImeOptions(horizontalAlignment.option)

/**
 *  Adds the keyboard alignment option to the private IME configuration options.
 *
 *  It is not guaranteed if IME will comply with the options provided here.
 *
 *  @param horizontalAlignment defines the horizontal alignment [TvKeyboardAlignment] option for
 *  keyboard.
 */
@ExperimentalTvFoundationApi
fun AndroidImeOptions.keyboardAlignment(
    horizontalAlignment: TvKeyboardAlignment
): AndroidImeOptions {
    val privateImeOptions =
        if (!privateImeOptions.isNullOrBlank()) this.privateImeOptions + "," else ""
    return AndroidImeOptions(privateImeOptions + horizontalAlignment.option)
}

/**
 * Represents the TV keyboard alignment options available for TextField(s).
 *
 * It is not guaranteed if IME will comply with the options provided here.
 */
@ExperimentalTvFoundationApi
enum class TvKeyboardAlignment(val option: String? = null) {
    Left("horizontalAlignment=left"),
    Right("horizontalAlignment=right"),
    Center("horizontalAlignment=center"),
    Fullscreen("fullWidthKeyboard")
}
