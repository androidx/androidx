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

import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Locale;

@RequiresApi(24)
final class LocaleListPlatformWrapper implements LocaleListInterface {
    private final LocaleList mLocaleList;

    LocaleListPlatformWrapper(Object localeList) {
        mLocaleList = (LocaleList) localeList;
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
    public int size() {
        return mLocaleList.size();
    }

    @Override
    public int indexOf(Locale locale) {
        return mLocaleList.indexOf(locale);
    }

    @Override
    public boolean equals(Object other) {
        return mLocaleList.equals(((LocaleListInterface) other).getLocaleList());
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
    public Locale getFirstMatch(@NonNull String[] supportedLocales) {
        return mLocaleList.getFirstMatch(supportedLocales);
    }
}
