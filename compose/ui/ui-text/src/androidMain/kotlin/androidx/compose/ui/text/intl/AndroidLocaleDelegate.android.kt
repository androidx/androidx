/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.intl

import android.os.LocaleList as AndroidLocaleList
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.platform.createSynchronizedObject
import java.util.Locale as JavaLocale

private val TAG = "Locale"

/** An Android implementation of LocaleDelegate object for API 23 */
internal class AndroidLocaleDelegateAPI23 : PlatformLocaleDelegate {

    override val current: LocaleList
        get() = LocaleList(listOf(Locale(JavaLocale.getDefault())))

    override fun parseLanguageTag(languageTag: String): PlatformLocale {
        val platformLocale = JavaLocale.forLanguageTag(languageTag)
        if (platformLocale.toLanguageTag() == "und") {
            Log.e(
                TAG,
                "The language tag $languageTag is not well-formed. Locale is resolved " +
                    "to Undetermined. Note that underscore '_' is not a valid subtags delimiter and " +
                    "must be replaced with '-'."
            )
        }
        return platformLocale
    }
}

/** An Android implementation of LocaleDelegate object for API 24 and later */
@RequiresApi(api = 24)
internal class AndroidLocaleDelegateAPI24 : PlatformLocaleDelegate {
    private var lastPlatformLocaleList: AndroidLocaleList? = null
    private var lastLocaleList: LocaleList? = null
    private val lock = createSynchronizedObject()

    override val current: LocaleList
        get() {
            val platformLocaleList = AndroidLocaleList.getDefault()
            return synchronized(lock) {
                // try to avoid any more allocs
                lastLocaleList?.let { if (platformLocaleList === lastPlatformLocaleList) return it }
                // this is faster than adding to an empty mutableList
                val localeList =
                    LocaleList(
                        List(platformLocaleList.size()) { position ->
                            Locale(platformLocaleList[position])
                        }
                    )
                // cache the platform result and compose result
                lastPlatformLocaleList = platformLocaleList
                lastLocaleList = localeList
                localeList
            }
        }

    override fun parseLanguageTag(languageTag: String): PlatformLocale {
        val platformLocale = JavaLocale.forLanguageTag(languageTag)
        if (platformLocale.toLanguageTag() == "und") {
            Log.e(
                TAG,
                "The language tag $languageTag is not well-formed. Locale is resolved " +
                    "to Undetermined. Note that underscore '_' is not a valid subtag delimiter and " +
                    "must be replaced with '-'."
            )
        }
        return platformLocale
    }
}
