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

import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;

import java.util.Locale;

/**
 * Helper for accessing features in {@link LocaleList}.
 */
public final class LocaleListCompat {
    private static final LocaleListCompat sEmptyLocaleList = create();

    private final LocaleListInterface mImpl;

    private LocaleListCompat(LocaleListInterface impl) {
        mImpl = impl;
    }

    /** @deprecated Use {@link #wrap(LocaleList)} */
    @Deprecated
    @RequiresApi(24)
    public static LocaleListCompat wrap(Object localeList) {
        return wrap((LocaleList) localeList);
    }

    /**
     * Creates a new instance of {@link LocaleListCompat} from the Locale list.
     */
    @RequiresApi(24)
    @NonNull
    public static LocaleListCompat wrap(@NonNull LocaleList localeList) {
        return new LocaleListCompat(new LocaleListPlatformWrapper(localeList));
    }

    /**
     * Gets the underlying framework object.
     *
     * @return an android.os.LocaleList object if API &gt;= 24 , or {@code null} if not.
     */
    @Nullable
    public Object unwrap() {
        return mImpl.getLocaleList();
    }

    /**
     * Creates a new instance of {@link LocaleListCompat} from the {@link Locale} array.
     */
    @NonNull
    public static LocaleListCompat create(@NonNull Locale... localeList) {
        if (Build.VERSION.SDK_INT >= 24) {
            return wrap(Api24Impl.createLocaleList(localeList));
        }
        return new LocaleListCompat(new LocaleListCompatWrapper(localeList));
    }

    /**
     * Retrieves the {@link Locale} at the specified index.
     *
     * @param index The position to retrieve.
     * @return The {@link Locale} in the given index
     */
    @Nullable
    public Locale get(int index) {
        return mImpl.get(index);
    }

    /**
     * Returns whether the {@link LocaleListCompat} contains no {@link Locale} items.
     *
     * @return {@code true} if this {@link LocaleListCompat} has no {@link Locale} items,
     *         {@code false} otherwise
     */
    public boolean isEmpty() {
        return mImpl.isEmpty();
    }

    /**
     * Returns the number of {@link Locale} items in this {@link LocaleListCompat}.
     */
    @IntRange(from = 0)
    public int size() {
        return mImpl.size();
    }

    /**
     * Searches this {@link LocaleListCompat} for the specified {@link Locale} and returns the
     * index of the first occurrence.
     *
     * @param locale The {@link Locale} to search for.
     * @return The index of the first occurrence of the {@link Locale} or {@code -1} if the item
     *         wasn't found
     */
    @IntRange(from = -1)
    public int indexOf(@Nullable Locale locale) {
        return mImpl.indexOf(locale);
    }

    /**
     * Retrieves a String representation of the language tags in this list.
     */
    @NonNull
    public String toLanguageTags() {
        return mImpl.toLanguageTags();
    }

    /**
     * Returns the first match in the locale list given an unordered array of supported locales
     * in BCP 47 format.
     *
     * @return The first {@link Locale} from this list that appears in the given array, or
     *         {@code null} if the {@link LocaleListCompat} is empty.
     */
    @Nullable
    public Locale getFirstMatch(@NonNull String[] supportedLocales) {
        return mImpl.getFirstMatch(supportedLocales);
    }

    /**
     * Retrieve an empty instance of {@link LocaleListCompat}.
     */
    @NonNull
    public static LocaleListCompat getEmptyLocaleList() {
        return sEmptyLocaleList;
    }

    /**
     * Generates a new LocaleList with the given language tags.
     *
     * <p>Note that for API < 24 only the first language tag will be used.</>
     *
     * @param list The language tags to be included as a single {@link String} separated by commas.
     * @return A new instance with the {@link Locale} items identified by the given tags.
     */
    @NonNull
    public static LocaleListCompat forLanguageTags(@Nullable String list) {
        if (list == null || list.isEmpty()) {
            return getEmptyLocaleList();
        } else {
            final String[] tags = list.split(",", -1);
            final Locale[] localeArray = new Locale[tags.length];
            for (int i = 0; i < localeArray.length; i++) {
                localeArray[i] = Build.VERSION.SDK_INT >= 21
                        ? Api21Impl.forLanguageTag(tags[i])
                        : forLanguageTagCompat(tags[i]);
            }
            return create(localeArray);
        }
    }

    // Simpleton implementation for Locale.forLanguageTag(...)
    static Locale forLanguageTagCompat(String str) {
        if (str.contains("-")) {
            String[] args = str.split("-", -1);
            if (args.length > 2) {
                return new Locale(args[0], args[1], args[2]);
            } else if (args.length > 1) {
                return new Locale(args[0], args[1]);
            } else if (args.length == 1) {
                return new Locale(args[0]);
            }
        } else if (str.contains("_")) {
            String[] args = str.split("_", -1);
            if (args.length > 2) {
                return new Locale(args[0], args[1], args[2]);
            } else if (args.length > 1) {
                return new Locale(args[0], args[1]);
            } else if (args.length == 1) {
                return new Locale(args[0]);
            }
        } else {
            return new Locale(str);
        }

        throw new IllegalArgumentException("Can not parse language tag: [" + str + "]");
    }

    /**
     * Returns the default locale list, adjusted by moving the default locale to its first
     * position.
     */
    @NonNull @Size(min = 1)
    public static LocaleListCompat getAdjustedDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            return LocaleListCompat.wrap(Api24Impl.getAdjustedDefault());
        } else {
            return LocaleListCompat.create(Locale.getDefault());
        }
    }

    /**
     * The result is guaranteed to include the default Locale returned by Locale.getDefault(), but
     * not necessarily at the top of the list. The default locale not being at the top of the list
     * is an indication that the system has set the default locale to one of the user's other
     * preferred locales, having concluded that the primary preference is not supported but a
     * secondary preference is.
     *
     * <p>Note that for API &gt;= 24 the default LocaleList would change if Locale.setDefault() is
     * called. This method takes that into account by always checking the output of
     * Locale.getDefault() and recalculating the default LocaleList if needed.</p>
     */
    @NonNull @Size(min = 1)
    public static LocaleListCompat getDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            return LocaleListCompat.wrap(Api24Impl.getDefault());
        } else {
            return LocaleListCompat.create(Locale.getDefault());
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LocaleListCompat && mImpl.equals(((LocaleListCompat) other).mImpl);
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return mImpl.toString();
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static LocaleList createLocaleList(Locale... list) {
            return new LocaleList(list);
        }

        @DoNotInline
        static LocaleList getAdjustedDefault() {
            return LocaleList.getAdjustedDefault();
        }

        @DoNotInline
        static LocaleList getDefault() {
            return LocaleList.getDefault();
        }
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Locale forLanguageTag(String languageTag) {
            return Locale.forLanguageTag(languageTag);
        }
    }
}
