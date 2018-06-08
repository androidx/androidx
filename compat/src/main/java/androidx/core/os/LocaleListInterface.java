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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Locale;

/**
 * Interface describing backwards-compatible LocaleList APIs.
 *
 * @hide Internal use only
 */
@RestrictTo(LIBRARY_GROUP)
interface LocaleListInterface {
    void setLocaleList(@NonNull Locale... list);

    Object getLocaleList();

    Locale get(int index);

    boolean isEmpty();

    @IntRange(from = 0)
    int size();

    @IntRange(from = -1)
    int indexOf(Locale locale);

    @Override
    boolean equals(Object other);

    @Override
    int hashCode();

    @Override
    String toString();

    String toLanguageTags();

    @Nullable
    Locale getFirstMatch(String[] supportedLocales);
}
