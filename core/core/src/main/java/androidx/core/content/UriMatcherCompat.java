/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.content;

import android.content.UriMatcher;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Predicate;

/**
 * Helper for accessing {@link UriMatcher} to create Uri Predicate.
 */
public class UriMatcherCompat {
    private UriMatcherCompat(){
        /* Hide constructor */
    }

    /**
     * Creates a Uri predicate from a {@link UriMatcher}.
     * @param matcher the uriMatcher.
     * @return the predicate created from the uriMatcher.
     */
    @NonNull
    public static Predicate<Uri> asPredicate(@NonNull UriMatcher matcher) {
        return v -> matcher.match(v) != UriMatcher.NO_MATCH;
    }
}
