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
    static final LocaleListInterface IMPL;
    private static final LocaleListCompat sEmptyLocaleList = new LocaleListCompat();


    static class LocaleListCompatBaseImpl implements LocaleListInterface {
        private LocaleListHelper mLocaleList = new LocaleListHelper();

        @Override
        public void setLocaleList(@NonNull Locale... list) {
            mLocaleList = new LocaleListHelper(list);
        }

        @Override
        public Object getLocaleList() {
            return mLocaleList;
        }

        @Override
        public Locale get(int index) {
            return mLocaleList.get(index);
        }

        @Override
        public boolean isEmpty() {
            return mLocaleList.isEmpty();
        }

        @Override
        @IntRange(from = 0)
        public int size() {
            return mLocaleList.size();
        }

        @Override
        @IntRange(from = -1)
        public int indexOf(Locale locale) {
            return mLocaleList.indexOf(locale);
        }

        @Override
        public boolean equals(Object other) {
            return mLocaleList.equals(((LocaleListCompat) other).unwrap());
        }

        @Override
        public int hashCode() {
            return mLocaleList.hashCode();
        }

        @Override
        public String toString() {
            return mLocaleList.toString();
        }

        @Override
        public String toLanguageTags() {
            return mLocaleList.toLanguageTags();
        }

        @Nullable
        @Override
        public Locale getFirstMatch(String[] supportedLocales) {
            if (mLocaleList != null) {
                return mLocaleList.getFirstMatch(supportedLocales);
            }
            return null;
        }
    }

    @RequiresApi(24)
    static class LocaleListCompatApi24Impl implements LocaleListInterface {
        private LocaleList mLocaleList = new LocaleList();

        @Override
        public void setLocaleList(@NonNull Locale... list) {
            mLocaleList = new LocaleList(list);
        }

        @Override
        public Object getLocaleList() {
            return mLocaleList;
        }

        @Override
        public Locale get(int index) {
            return mLocaleList.get(index);
        }

        @Override
        public boolean isEmpty() {
            return mLocaleList.isEmpty();
        }

        @Override
        @IntRange(from = 0)
        public int size() {
            return mLocaleList.size();
        }

        @Override
        @IntRange(from = -1)
        public int indexOf(Locale locale) {
            return mLocaleList.indexOf(locale);
        }

        @Override
        public boolean equals(Object other) {
            return mLocaleList.equals(((LocaleListCompat) other).unwrap());
        }

        @Override
        public int hashCode() {
            return mLocaleList.hashCode();
        }

        @Override
        public String toString() {
            return mLocaleList.toString();
        }

        @Override
        public String toLanguageTags() {
            return mLocaleList.toLanguageTags();
        }

        @Nullable
        @Override
        public Locale getFirstMatch(String[] supportedLocales) {
            if (mLocaleList != null) {
                return mLocaleList.getFirstMatch(supportedLocales);
            }
            return null;
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 24) {
            IMPL = new LocaleListCompatApi24Impl();
        } else {
            IMPL = new LocaleListCompatBaseImpl();
        }
    }

    private LocaleListCompat() {}

    /**
     * Creates a new instance of {@link LocaleListCompat} from the Locale list.
     */
    @RequiresApi(24)
    public static LocaleListCompat wrap(Object object) {
        LocaleListCompat instance = new LocaleListCompat();
        if (object instanceof LocaleList) {
            instance.setLocaleList((LocaleList) object);

        }
        return instance;
    }

    /**
     * Gets the underlying framework object.
     *
     * @return an android.os.LocaleList object if API &gt;= 24 , or {@link Locale} if not.
     */
    @Nullable
    public Object unwrap() {
        return IMPL.getLocaleList();
    }

    /**
     * Creates a new instance of {@link LocaleListCompat} from the {@link Locale} array.
     */
    public static LocaleListCompat create(@NonNull Locale... localeList) {
        LocaleListCompat instance = new LocaleListCompat();
        instance.setLocaleListArray(localeList);
        return instance;
    }

    /**
     * Retrieves the {@link Locale} at the specified index.
     *
     * @param index The position to retrieve.
     * @return The {@link Locale} in the given index
     */
    public Locale get(int index) {
        return IMPL.get(index);
    }

    /**
     * Returns whether the {@link LocaleListCompat} contains no {@link Locale} items.
     *
     * @return {@code true} if this {@link LocaleListCompat} has no {@link Locale} items,
     *         {@code false} otherwise
     */
    public boolean isEmpty() {
        return IMPL.isEmpty();
    }

    /**
     * Returns the number of {@link Locale} items in this {@link LocaleListCompat}.
     */
    @IntRange(from = 0)
    public int size() {
        return IMPL.size();
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
    public int indexOf(Locale locale) {
        return IMPL.indexOf(locale);
    }

    /**
     * Retrieves a String representation of the language tags in this list.
     */
    @NonNull
    public String toLanguageTags() {
        return IMPL.toLanguageTags();
    }

    /**
     * Returns the first match in the locale list given an unordered array of supported locales
     * in BCP 47 format.
     *
     * @return The first {@link Locale} from this list that appears in the given array, or
     *         {@code null} if the {@link LocaleListCompat} is empty.
     */
    public Locale getFirstMatch(String[] supportedLocales) {
        return IMPL.getFirstMatch(supportedLocales);
    }

    /**
     * Retrieve an empty instance of {@link LocaleList}.
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
                        ? Locale.forLanguageTag(tags[i])
                        : LocaleHelper.forLanguageTag(tags[i]);
            }
            LocaleListCompat instance = new LocaleListCompat();
            instance.setLocaleListArray(localeArray);
            return instance;
        }
    }

    /**
     * Returns the default locale list, adjusted by moving the default locale to its first
     * position.
     */
    @NonNull @Size(min = 1)
    public static LocaleListCompat getAdjustedDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            return LocaleListCompat.wrap(LocaleList.getAdjustedDefault());
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
            return LocaleListCompat.wrap(LocaleList.getDefault());
        } else {
            return LocaleListCompat.create(Locale.getDefault());
        }
    }

    @Override
    public boolean equals(Object other) {
        return IMPL.equals(other);
    }

    @Override
    public int hashCode() {
        return IMPL.hashCode();
    }

    @Override
    public String toString() {
        return IMPL.toString();
    }

    @RequiresApi(24)
    private void setLocaleList(LocaleList localeList) {
        final int localeListSize = localeList.size();
        if (localeListSize > 0) {
            Locale[] localeArrayList = new Locale[localeListSize];
            for (int i = 0; i < localeListSize; i++) {
                localeArrayList[i] = localeList.get(i);
            }
            IMPL.setLocaleList(localeArrayList);
        }
    }

    private void setLocaleListArray(Locale... localeArrayList) {
        IMPL.setLocaleList(localeArrayList);
    }
}
