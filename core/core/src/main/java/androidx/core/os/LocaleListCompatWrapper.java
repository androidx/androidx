/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.os;

import androidx.annotation.IntRange;
import androidx.annotation.VisibleForTesting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

final class LocaleListCompatWrapper implements LocaleListInterface {
    private final Locale[] mList;
    // This is a comma-separated list of the locales in the LocaleListHelper created at construction
    // time, basically the result of running each locale's toLanguageTag() method and concatenating
    // them with commas in between.
    private final @NonNull String mStringRepresentation;

    private static final Locale[] sEmptyList = new Locale[0];

    @Override
    public @Nullable Object getLocaleList() {
        return null;
    }

    @Override
    public Locale get(int index) {
        return (0 <= index && index < mList.length) ? mList[index] : null;
    }

    @Override
    public boolean isEmpty() {
        return mList.length == 0;
    }

    @Override
    public int size() {
        return mList.length;
    }

    @Override
    public int indexOf(Locale locale) {
        for (int i = 0; i < mList.length; i++) {
            if (mList[i].equals(locale)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LocaleListCompatWrapper)) {
            return false;
        }
        final Locale[] otherList = ((LocaleListCompatWrapper) other).mList;
        if (mList.length != otherList.length) {
            return false;
        }
        for (int i = 0; i < mList.length; i++) {
            if (!mList[i].equals(otherList[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (Locale locale : mList) {
            result = 31 * result + locale.hashCode();
        }
        return result;
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < mList.length; i++) {
            sb.append(mList[i]);
            if (i < mList.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toLanguageTags() {
        return mStringRepresentation;
    }

    LocaleListCompatWrapper(Locale @NonNull ... list) {
        if (list.length == 0) {
            mList = sEmptyList;
            mStringRepresentation = "";
        } else {
            final List<Locale> localeList = new ArrayList<>();
            final HashSet<Locale> seenLocales = new HashSet<>();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.length; i++) {
                final Locale l = list[i];
                if (l == null) {
                    throw new NullPointerException("list[" + i + "] is null");
                } else if (!seenLocales.contains(l)) {
                    final Locale localeClone = (Locale) l.clone();
                    localeList.add(localeClone);
                    toLanguageTag(sb, localeClone);
                    if (i < list.length - 1) {
                        sb.append(',');
                    }
                    seenLocales.add(localeClone);
                }
            }
            mList = localeList.toArray(new Locale[0]);
            mStringRepresentation = sb.toString();
        }
    }

    @VisibleForTesting
    static void toLanguageTag(StringBuilder builder, Locale locale) {
        builder.append(locale.getLanguage());
        final String country = locale.getCountry();
        // No guarantee that getCountry() was non-null in earlier SDKs.
        //noinspection ConstantConditions
        if (country != null && !country.isEmpty()) {
            builder.append('-');
            builder.append(locale.getCountry());
        }
    }

    private static String getLikelyScript(Locale locale) {
        final String script = locale.getScript();
        if (!script.isEmpty()) {
            return script;
        } else {
            return "";
        }
    }

    private static final Locale LOCALE_EN_XA = new Locale("en", "XA");
    private static final Locale LOCALE_AR_XB = new Locale("ar", "XB");

    private static boolean isPseudoLocale(Locale locale) {
        return LOCALE_EN_XA.equals(locale) || LOCALE_AR_XB.equals(locale);
    }

    @IntRange(from = 0, to = 1)
    private static int matchScore(Locale supported, Locale desired) {
        if (supported.equals(desired)) {
            return 1;  // return early so we don't do unnecessary computation
        }
        if (!supported.getLanguage().equals(desired.getLanguage())) {
            return 0;
        }
        if (isPseudoLocale(supported) || isPseudoLocale(desired)) {
            // The locales are not the same, but the languages are the same, and one of the locales
            // is a pseudo-locale. So this is not a match.
            return 0;
        }
        final String supportedScr = getLikelyScript(supported);
        if (supportedScr.isEmpty()) {
            // If we can't guess a script, we don't know enough about the locales' language to find
            // if the locales match. So we fall back to old behavior of matching, which considered
            // locales with different regions different.
            final String supportedRegion = supported.getCountry();
            return (supportedRegion.isEmpty() || supportedRegion.equals(desired.getCountry()))
                    ? 1
                    : 0;
        }
        final String desiredScr = getLikelyScript(desired);
        // There is no match if the two locales use different scripts. This will most imporantly
        // take care of traditional vs simplified Chinese.
        return supportedScr.equals(desiredScr) ? 1 : 0;
    }

    private int findFirstMatchIndex(Locale supportedLocale) {
        for (int idx = 0; idx < mList.length; idx++) {
            final int score = matchScore(supportedLocale, mList[idx]);
            if (score > 0) {
                return idx;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static final Locale EN_LATN = LocaleListCompat.forLanguageTagCompat("en-Latn");

    private int computeFirstMatchIndex(Collection<String> supportedLocales,
            boolean assumeEnglishIsSupported) {
        if (mList.length == 1) {  // just one locale, perhaps the most common scenario
            return 0;
        }
        if (mList.length == 0) {  // empty locale list
            return -1;
        }

        int bestIndex = Integer.MAX_VALUE;
        // Try English first, so we can return early if it's in the LocaleListHelper
        if (assumeEnglishIsSupported) {
            final int idx = findFirstMatchIndex(EN_LATN);
            if (idx == 0) { // We have a match on the first locale, which is good enough
                return 0;
            } else if (idx < bestIndex) {
                bestIndex = idx;
            }
        }
        for (String languageTag : supportedLocales) {
            final Locale supportedLocale = LocaleListCompat.forLanguageTagCompat(languageTag);
            // We expect the average length of locale lists used for locale resolution to be
            // smaller than three, so it's OK to do this as an O(mn) algorithm.
            final int idx = findFirstMatchIndex(supportedLocale);
            if (idx == 0) { // We have a match on the first locale, which is good enough
                return 0;
            } else if (idx < bestIndex) {
                bestIndex = idx;
            }
        }
        if (bestIndex == Integer.MAX_VALUE) {
            // no match was found, so we fall back to the first locale in the locale list
            return 0;
        } else {
            return bestIndex;
        }
    }

    private Locale computeFirstMatch(Collection<String> supportedLocales,
            boolean assumeEnglishIsSupported) {
        int bestIndex = computeFirstMatchIndex(supportedLocales, assumeEnglishIsSupported);
        return bestIndex == -1 ? null : mList[bestIndex];
    }

    @Override
    public Locale getFirstMatch(String @NonNull [] supportedLocales) {
        return computeFirstMatch(Arrays.asList(supportedLocales),
                false /* assume English is not supported */);
    }
}
