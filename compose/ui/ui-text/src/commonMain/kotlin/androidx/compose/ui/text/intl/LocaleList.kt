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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.util.fastMap

/**
 * Defines a list of [Locale] objects.
 *
 * @see TextStyle
 * @see SpanStyle
 */
@Immutable
public class LocaleList(public val localeList: List<Locale>) : Collection<Locale> {
    public companion object {

        /**
         * An empty instance of [LocaleList]. Usually used to reference a lack of explicit [Locale]
         * configuration.
         */
        public val Empty: LocaleList = LocaleList(listOf())

        /** Returns Locale object which represents current locale */
        @Deprecated(
            "LocaleList.current is not backed by snapshot state and does not notify readers on updates. " +
                "In @Composable functions, use LocalLocaleList.current instead. Outside composables, " +
                "pass the current locale list explicitly from an observable state source.",
            replaceWith =
                ReplaceWith(
                    "LocalLocaleList.current",
                    "androidx.compose.ui.platform.LocalLocaleList",
                ),
        )
        @Suppress("DEPRECATION")
        public val current: LocaleList
            get() = platformLocaleDelegate.current
    }

    /**
     * Create a [LocaleList] object from comma separated language tags.
     *
     * @param languageTags A comma separated [IETF BCP47](https://tools.ietf.org/html/bcp47)
     *   compliant language tag.
     */
    public constructor(
        languageTags: String
    ) : this(languageTags.split(",").fastMap { it.trim() }.fastMap { Locale(it) })

    /** Creates a [LocaleList] object from a list of [Locale]s. */
    public constructor(vararg locales: Locale) : this(locales.toList())

    public operator fun get(i: Int): Locale = localeList[i]

    // Collection overrides for easy iterations.
    public override val size: Int = localeList.size

    public override operator fun contains(element: Locale): Boolean = localeList.contains(element)

    public override fun containsAll(elements: Collection<Locale>): Boolean =
        localeList.containsAll(elements)

    public override fun isEmpty(): Boolean = localeList.isEmpty()

    public override fun iterator(): Iterator<Locale> = localeList.iterator()

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocaleList) return false
        if (localeList != other.localeList) return false
        return true
    }

    public override fun hashCode(): Int {
        return localeList.hashCode()
    }

    public override fun toString(): String {
        return "LocaleList(localeList=$localeList)"
    }
}
