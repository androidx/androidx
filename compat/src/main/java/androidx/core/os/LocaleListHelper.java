/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.Size;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

/**
 * LocaleListHelper is an immutable list of Locales, typically used to keep an ordered list of user
 * preferences for locales.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
final class LocaleListHelper {
    private final Locale[] mList;
    // This is a comma-separated list of the locales in the LocaleListHelper created at construction
    // time, basically the result of running each locale's toLanguageTag() method and concatenating
    // them with commas in between.
    @NonNull
    private final String mStringRepresentation;

    private static final Locale[] sEmptyList = new Locale[0];
    private static final LocaleListHelper sEmptyLocaleList = new LocaleListHelper();

    /**
     * Retrieves the {@link Locale} at the specified index.
     *
     * @param index The position to retrieve.
     * @return The {@link Locale} in the given index.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Locale get(int index) {
        return (0 <= index && index < mList.length) ? mList[index] : null;
    }

    /**
     * Returns whether the {@link LocaleListHelper} contains no {@link Locale} items.
     *
     * @return {@code true} if this {@link LocaleListHelper} has no {@link Locale} items,
     *         {@code false} otherwise.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    boolean isEmpty() {
        return mList.length == 0;
    }

    /**
     * Returns the number of {@link Locale} items in this {@link LocaleListHelper}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntRange(from = 0)
    int size() {
        return mList.length;
    }

    /**
     * Searches this {@link LocaleListHelper} for the specified {@link Locale} and returns the index
     * of the first occurrence.
     *
     * @param locale The {@link Locale} to search for.
     * @return The index of the first occurrence of the {@link Locale} or {@code -1} if the item
     *     wasn't found.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntRange(from = -1)
    int indexOf(Locale locale) {
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
        if (!(other instanceof LocaleListHelper)) {
            return false;
        }
        final Locale[] otherList = ((LocaleListHelper) other).mList;
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
        for (int i = 0; i < mList.length; i++) {
            result = 31 * result + mList[i].hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
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

    /**
     * Retrieves a String representation of the language tags in this list.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    String toLanguageTags() {
        return mStringRepresentation;
    }

    /**
     * Creates a new {@link LocaleListHelper}.
     *
     * <p>For empty lists of {@link Locale} items it is better to use {@link #getEmptyLocaleList()},
     * which returns a pre-constructed empty list.</p>
     *
     * @throws NullPointerException if any of the input locales is <code>null</code>.
     * @throws IllegalArgumentException if any of the input locales repeat.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    LocaleListHelper(@NonNull Locale... list) {
        if (list.length == 0) {
            mList = sEmptyList;
            mStringRepresentation = "";
        } else {
            final Locale[] localeList = new Locale[list.length];
            final HashSet<Locale> seenLocales = new HashSet<Locale>();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.length; i++) {
                final Locale l = list[i];
                if (l == null) {
                    throw new NullPointerException("list[" + i + "] is null");
                } else if (seenLocales.contains(l)) {
                    throw new IllegalArgumentException("list[" + i + "] is a repetition");
                } else {
                    final Locale localeClone = (Locale) l.clone();
                    localeList[i] = localeClone;
                    sb.append(LocaleHelper.toLanguageTag(localeClone));
                    if (i < list.length - 1) {
                        sb.append(',');
                    }
                    seenLocales.add(localeClone);
                }
            }
            mList = localeList;
            mStringRepresentation = sb.toString();
        }
    }

    /**
     * Constructs a locale list, with the topLocale moved to the front if it already is
     * in otherLocales, or added to the front if it isn't.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    LocaleListHelper(@NonNull Locale topLocale, LocaleListHelper otherLocales) {
        if (topLocale == null) {
            throw new NullPointerException("topLocale is null");
        }

        final int inputLength = (otherLocales == null) ? 0 : otherLocales.mList.length;
        int topLocaleIndex = -1;
        for (int i = 0; i < inputLength; i++) {
            if (topLocale.equals(otherLocales.mList[i])) {
                topLocaleIndex = i;
                break;
            }
        }

        final int outputLength = inputLength + (topLocaleIndex == -1 ? 1 : 0);
        final Locale[] localeList = new Locale[outputLength];
        localeList[0] = (Locale) topLocale.clone();
        if (topLocaleIndex == -1) {
            // topLocale was not in otherLocales
            for (int i = 0; i < inputLength; i++) {
                localeList[i + 1] = (Locale) otherLocales.mList[i].clone();
            }
        } else {
            for (int i = 0; i < topLocaleIndex; i++) {
                localeList[i + 1] = (Locale) otherLocales.mList[i].clone();
            }
            for (int i = topLocaleIndex + 1; i < inputLength; i++) {
                localeList[i] = (Locale) otherLocales.mList[i].clone();
            }
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outputLength; i++) {
            sb.append(LocaleHelper.toLanguageTag(localeList[i]));

            if (i < outputLength - 1) {
                sb.append(',');
            }
        }

        mList = localeList;
        mStringRepresentation = sb.toString();
    }

    /**
     * Retrieve an empty instance of {@link LocaleListHelper}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    static LocaleListHelper getEmptyLocaleList() {
        return sEmptyLocaleList;
    }

    /**
     * Generates a new LocaleListHelper with the given language tags.
     *
     * @param list The language tags to be included as a single {@link String} separated by commas.
     * @return A new instance with the {@link Locale} items identified by the given tags.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    static LocaleListHelper forLanguageTags(@Nullable String list) {
        if (list == null || list.isEmpty()) {
            return getEmptyLocaleList();
        } else {
            final String[] tags = list.split(",", -1);
            final Locale[] localeArray = new Locale[tags.length];
            for (int i = 0; i < localeArray.length; i++) {
                localeArray[i] = LocaleHelper.forLanguageTag(tags[i]);
            }
            return new LocaleListHelper(localeArray);
        }
    }

    private static String getLikelyScript(Locale locale) {
        if (Build.VERSION.SDK_INT >= 21) {
            final String script = locale.getScript();
            if (!script.isEmpty()) {
                return script;
            } else {
                return "";
            }
        }
        return "";
    }

    private static final String STRING_EN_XA = "en-XA";
    private static final String STRING_AR_XB = "ar-XB";
    private static final Locale LOCALE_EN_XA = new Locale("en", "XA");
    private static final Locale LOCALE_AR_XB = new Locale("ar", "XB");
    private static final int NUM_PSEUDO_LOCALES = 2;

    private static boolean isPseudoLocale(String locale) {
        return STRING_EN_XA.equals(locale) || STRING_AR_XB.equals(locale);
    }

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

    private static final Locale EN_LATN = LocaleHelper.forLanguageTag("en-Latn");

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
            final Locale supportedLocale = LocaleHelper.forLanguageTag(languageTag);
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

    /**
     * Returns the first match in the locale list given an unordered array of supported locales
     * in BCP 47 format.
     *
     * @return The first {@link Locale} from this list that appears in the given array, or
     *     {@code null} if the {@link LocaleListHelper} is empty.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    Locale getFirstMatch(String[] supportedLocales) {
        return computeFirstMatch(Arrays.asList(supportedLocales),
                false /* assume English is not supported */);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    int getFirstMatchIndex(String[] supportedLocales) {
        return computeFirstMatchIndex(Arrays.asList(supportedLocales),
                false /* assume English is not supported */);
    }

    /**
     * Same as getFirstMatch(), but with English assumed to be supported, even if it's not.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    Locale getFirstMatchWithEnglishSupported(String[] supportedLocales) {
        return computeFirstMatch(Arrays.asList(supportedLocales),
                true /* assume English is supported */);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    int getFirstMatchIndexWithEnglishSupported(Collection<String> supportedLocales) {
        return computeFirstMatchIndex(supportedLocales, true /* assume English is supported */);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    int getFirstMatchIndexWithEnglishSupported(String[] supportedLocales) {
        return getFirstMatchIndexWithEnglishSupported(Arrays.asList(supportedLocales));
    }

    /**
     * Returns true if the collection of locale tags only contains empty locales and pseudolocales.
     * Assumes that there is no repetition in the input.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static boolean isPseudoLocalesOnly(@Nullable String[] supportedLocales) {
        if (supportedLocales == null) {
            return true;
        }

        if (supportedLocales.length > NUM_PSEUDO_LOCALES + 1) {
            // This is for optimization. Since there's no repetition in the input, if we have more
            // than the number of pseudo-locales plus one for the empty string, it's guaranteed
            // that we have some meaninful locale in the collection, so the list is not "practically
            // empty".
            return false;
        }
        for (String locale : supportedLocales) {
            if (!locale.isEmpty() && !isPseudoLocale(locale)) {
                return false;
            }
        }
        return true;
    }

    /** Lock for mutable static fields */
    private final static Object sLock = new Object();

    @GuardedBy("sLock")
    private static LocaleListHelper sLastExplicitlySetLocaleList = null;
    @GuardedBy("sLock")
    private static LocaleListHelper sDefaultLocaleList = null;
    @GuardedBy("sLock")
    private static LocaleListHelper sDefaultAdjustedLocaleList = null;
    @GuardedBy("sLock")
    private static Locale sLastDefaultLocale = null;

    /**
     * The result is guaranteed to include the default Locale returned by Locale.getDefault(), but
     * not necessarily at the top of the list. The default locale not being at the top of the list
     * is an indication that the system has set the default locale to one of the user's other
     * preferred locales, having concluded that the primary preference is not supported but a
     * secondary preference is.
     *
     * <p>Note that the default LocaleListHelper would change if Locale.setDefault() is called. This
     * method takes that into account by always checking the output of Locale.getDefault() and
     * recalculating the default LocaleListHelper if needed.</p>
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull @Size(min = 1)
    static LocaleListHelper getDefault() {
        final Locale defaultLocale = Locale.getDefault();
        synchronized (sLock) {
            if (!defaultLocale.equals(sLastDefaultLocale)) {
                sLastDefaultLocale = defaultLocale;
                // It's either the first time someone has asked for the default locale list, or
                // someone has called Locale.setDefault() since we last set or adjusted the default
                // locale list. So let's recalculate the locale list.
                if (sDefaultLocaleList != null
                        && defaultLocale.equals(sDefaultLocaleList.get(0))) {
                    // The default Locale has changed, but it happens to be the first locale in the
                    // default locale list, so we don't need to construct a new locale list.
                    return sDefaultLocaleList;
                }
                sDefaultLocaleList = new LocaleListHelper(
                        defaultLocale, sLastExplicitlySetLocaleList);
                sDefaultAdjustedLocaleList = sDefaultLocaleList;
            }
            // sDefaultLocaleList can't be null, since it can't be set to null by
            // LocaleListHelper.setDefault(), and if getDefault() is called before a call to
            // setDefault(), sLastDefaultLocale would be null and the check above would set
            // sDefaultLocaleList.
            return sDefaultLocaleList;
        }
    }

    /**
     * Returns the default locale list, adjusted by moving the default locale to its first
     * position.
     */
    @NonNull @Size(min = 1)
    static LocaleListHelper getAdjustedDefault() {
        getDefault(); // to recalculate the default locale list, if necessary
        synchronized (sLock) {
            return sDefaultAdjustedLocaleList;
        }
    }

    /**
     * Also sets the default locale by calling Locale.setDefault() with the first locale in the
     * list.
     *
     * @throws NullPointerException if the input is <code>null</code>.
     * @throws IllegalArgumentException if the input is empty.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static void setDefault(@NonNull @Size(min = 1) LocaleListHelper locales) {
        setDefault(locales, 0);
    }

    /**
     * This may be used directly by system processes to set the default locale list for apps. For
     * such uses, the default locale list would always come from the user preferences, but the
     * default locale may have been chosen to be a locale other than the first locale in the locale
     * list (based on the locales the app supports).
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static void setDefault(@NonNull @Size(min = 1) LocaleListHelper locales,
            int localeIndex) {
        if (locales == null) {
            throw new NullPointerException("locales is null");
        }
        if (locales.isEmpty()) {
            throw new IllegalArgumentException("locales is empty");
        }
        synchronized (sLock) {
            sLastDefaultLocale = locales.get(localeIndex);
            Locale.setDefault(sLastDefaultLocale);
            sLastExplicitlySetLocaleList = locales;
            sDefaultLocaleList = locales;
            if (localeIndex == 0) {
                sDefaultAdjustedLocaleList = sDefaultLocaleList;
            } else {
                sDefaultAdjustedLocaleList = new LocaleListHelper(
                        sLastDefaultLocale, sDefaultLocaleList);
            }
        }
    }
}
