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

package androidx.compose.ui.text.platform.extensions

import android.text.style.LocaleSpan
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.AndroidTextPaint

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(24)
internal object LocaleListHelperMethods {
    @RequiresApi(24)
    fun localeSpan(localeList: LocaleList): Any =
        LocaleSpan(android.os.LocaleList(*Array(localeList.size) { localeList[it].platformLocale }))

    @RequiresApi(24)
    fun setTextLocales(textPaint: AndroidTextPaint, localeList: LocaleList) {
        val currentLocales = textPaint.textLocales
        // Paint.textLocales setter internally checks for equality and avoids native calls if the
        // locales match, but only after receiving an android.os.LocaleList instance.
        // Checking equality here against compose's LocaleList avoids allocating the Array<Locale>
        // and android.os.LocaleList when the locales have not changed.
        if (currentLocales.size() == localeList.size) {
            var matches = true
            for (i in 0 until currentLocales.size()) {
                if (currentLocales[i] != localeList[i].platformLocale) {
                    matches = false
                    break
                }
            }
            if (matches) return
        }
        textPaint.textLocales =
            android.os.LocaleList(*Array(localeList.size) { localeList[it].platformLocale })
    }
}
