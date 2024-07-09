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

package androidx.compose.foundation.text.input.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.intl.PlatformLocale
import androidx.compose.ui.text.style.TextDirection

internal actual fun resolveTextDirectionForKeyboardTypePhone(
    locale: PlatformLocale
): TextDirection {
    val digitDirection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DigitDirectionalityApi28.resolve(locale)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            DigitDirectionalityApi24.resolve(locale)
        } else {
            DigitDirectionalityApi21.resolve(locale)
        }
    return if (
        digitDirection == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            digitDirection == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
    ) {
        TextDirection.Rtl
    } else {
        TextDirection.Ltr
    }
}

private object DigitDirectionalityApi21 {
    fun resolve(locale: PlatformLocale): Byte {
        val symbols = java.text.DecimalFormatSymbols.getInstance(locale)
        val zero = symbols.zeroDigit
        return Character.getDirectionality(zero)
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private object DigitDirectionalityApi24 {
    fun resolve(locale: PlatformLocale): Byte {
        val symbols = android.icu.text.DecimalFormatSymbols.getInstance(locale)
        val zero = symbols.zeroDigit
        return Character.getDirectionality(zero)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object DigitDirectionalityApi28 {
    fun resolve(locale: PlatformLocale): Byte {
        val symbols = android.icu.text.DecimalFormatSymbols.getInstance(locale)
        val zero = symbols.digitStrings[0].codePointAt(0)
        return Character.getDirectionality(zero)
    }
}
