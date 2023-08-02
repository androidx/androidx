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

package androidx.core.google.shortcuts.converters;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;

import com.google.android.gms.appindex.Indexable;

/**
 * Interface for a converter that will convert an AppSearch Document into {@link Indexable}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public interface AppSearchDocumentConverter {
    /** Converts a @{@link GenericDocument} into a {@link Indexable.Builder}. */
    @NonNull
    Indexable.Builder convertGenericDocument(@NonNull Context context,
            @NonNull GenericDocument genericDocument);
}
