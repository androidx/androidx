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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.material3.l10n.TranslationProviderByLocaleTag
import androidx.compose.material3.l10n.en
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
internal actual value class Strings(val value: Int) {
    actual companion object {
        actual val NavigationMenu = Strings(0)
        actual val CloseDrawer = Strings(1)
        actual val CloseSheet = Strings(2)
        actual val DefaultErrorMessage = Strings(3)
        actual val SliderRangeStart = Strings(4)
        actual val SliderRangeEnd = Strings(5)
        actual val Dialog = Strings(6)
        actual val MenuExpanded = Strings(7)
        actual val MenuCollapsed = Strings(8)
        actual val SnackbarDismiss = Strings(9)
        actual val SearchBarSearch = Strings(10)
        actual val SuggestionsAvailable = Strings(11)
        actual val DatePickerTitle = Strings(12)
        actual val DatePickerHeadline = Strings(13)
        actual val DatePickerYearPickerPaneTitle = Strings(14)
        actual val DatePickerSwitchToYearSelection = Strings(15)
        actual val DatePickerSwitchToDaySelection = Strings(16)
        actual val DatePickerSwitchToNextMonth = Strings(17)
        actual val DatePickerSwitchToPreviousMonth = Strings(18)
        actual val DatePickerNavigateToYearDescription = Strings(19)
        actual val DatePickerHeadlineDescription = Strings(20)
        actual val DatePickerNoSelectionDescription = Strings(21)
        actual val DatePickerTodayDescription = Strings(22)
        actual val DatePickerScrollToShowLaterYears = Strings(23)
        actual val DatePickerScrollToShowEarlierYears = Strings(24)
        actual val DateInputTitle = Strings(25)
        actual val DateInputHeadline = Strings(26)
        actual val DateInputLabel = Strings(27)
        actual val DateInputHeadlineDescription = Strings(28)
        actual val DateInputNoInputDescription = Strings(29)
        actual val DateInputInvalidNotAllowed = Strings(30)
        actual val DateInputInvalidForPattern = Strings(31)
        actual val DateInputInvalidYearRange = Strings(32)
        actual val DatePickerSwitchToCalendarMode = Strings(33)
        actual val DatePickerSwitchToInputMode = Strings(34)
        actual val DateRangePickerTitle = Strings(35)
        actual val DateRangePickerStartHeadline = Strings(36)
        actual val DateRangePickerEndHeadline = Strings(37)
        actual val DateRangePickerScrollToShowNextMonth = Strings(38)
        actual val DateRangePickerScrollToShowPreviousMonth = Strings(39)
        actual val DateRangePickerDayInRange = Strings(40)
        actual val DateRangeInputTitle = Strings(41)
        actual val DateRangeInputInvalidRangeInput = Strings(42)
        actual val BottomSheetPaneTitle = Strings(43)
        actual val BottomSheetDragHandleDescription = Strings(44)
        actual val BottomSheetPartialExpandDescription = Strings(45)
        actual val BottomSheetDismissDescription = Strings(46)
        actual val BottomSheetExpandDescription = Strings(47)
        actual val TooltipLongPressLabel = Strings(48)
        actual val TimePickerAM = Strings(49)
        actual val TimePickerPM = Strings(50)
        actual val TimePickerPeriodToggle = Strings(51)
        actual val TimePickerHourSelection = Strings(52)
        actual val TimePickerMinuteSelection = Strings(53)
        actual val TimePickerHourSuffix = Strings(54)
        actual val TimePicker24HourSuffix = Strings(55)
        actual val TimePickerMinuteSuffix = Strings(56)
        actual val TimePickerHour = Strings(57)
        actual val TimePickerMinute = Strings(58)
        actual val TimePickerHourTextField = Strings(59)
        actual val TimePickerMinuteTextField = Strings(60)
        actual val TooltipPaneDescription = Strings(61)
        actual val ExposedDropdownMenu = Strings(62)
    }
}

// TODO check if we should replace it by a more performant implementation
//  (without creating intermediate strings)
// TODO current implementation doesn't support sophisticated formatting like %.2f,
//  but currently we use it only for integers and strings
internal actual fun String.format(vararg formatArgs: Any?): String {
    var result = this
    formatArgs.forEachIndexed { index, arg ->
        result = result
            .replace("%${index+1}\$d", arg.toString())
            .replace("%${index+1}\$s", arg.toString())
    }
    return result
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings): String {
    val locale = Locale.current
    val tag = localeTag(language = locale.language, region = locale.region)
    val translation = translationByLocaleTag.getOrPut(tag) {
        findTranslation(locale)
    }
    return translation[string] ?: error("Missing translation for $string")
}

/**
 * A single translation; should contain all the [Strings].
 */
internal typealias Translation = Map<Strings, String>

/**
 * Translations we've already loaded, mapped by the locale tag (see [localeTag]).
 */
private val translationByLocaleTag = mutableMapOf<String, Translation>()

/**
 * Returns the tag for the given locale.
 *
 * Note that this is our internal format; this isn't the same as [Locale.toLanguageTag].
 */
private fun localeTag(language: String, region: String) = when {
    language == "" -> ""
    region == "" -> language
    else -> "${language}_$region"
}

/**
 * Returns a sequence of locale tags to use as keys to look up the translation for the given locale.
 *
 * Note that we don't need to check children (e.g. use `fr_FR` if `fr` is missing) because the
 * translations should never have a missing parent.
 */
private fun localeTagChain(locale: Locale) = sequence {
    if (locale.region != "") {
        yield(localeTag(language = locale.language, region = locale.region))
    }
    if (locale.language != "") {
        yield(localeTag(language = locale.language, region = ""))
    }
    yield(localeTag("", ""))
}

/**
 * Finds a [Translation] for the given locale.
 */
private fun findTranslation(locale: Locale): Translation {
    for (tag in localeTagChain(locale)) {
        // We don't need to merge translations because each one should contain all the strings.
        val translation = TranslationProviderByLocaleTag[tag]?.invoke()
        if (translation != null) {
            return translation
        }
    }
    error("Root translation must be present")
}

/**
 * This object is only needed to provide a namespace for the [Translation] provider functions
 * (e.g. [Translations.en]), to avoid polluting the global namespace.
 */
internal object Translations